import streamlit as st
import pandas as pd
import pages as pg
import altair as alt
import plotly.graph_objs as go
from sqlalchemy import create_engine
from streamlit_option_menu import option_menu
from streamlit_navigation_bar import st_navbar

st.set_page_config(layout="wide", initial_sidebar_state="auto")


# Define database connection using SQLAlchemy
@st.cache_data
def get_data(query):
    engine = create_engine("postgresql://SidarAdmin0:0nimdAradiS@sidar-dev-db.postgres.database.azure.com:5432/sidar")
    with engine.connect() as conn:
        data = pd.read_sql(query, conn)
    return data

pages = ['Home', 'Add Premise', 'Organizations', 'Add Personnel']
# parent_dir = os.path.dirname(os.path.abspath(__file__))
# logo_path = os.path.join(parent_dir, "cubes.svg")
urls = {
    "Add Premise" : "http://localhost:5500/company/buildings",
    "Organizations": "http://localhost:5500/company/buildings/organizations",
    "Add Personnel": "http://localhost:5500/company/buildings/personnel"
}

nav_bar_styles = {
    "nav": {
        "background-color": "royalblue",
        "justify-content": "right",
    },
    "img": {
        "padding-right": "14px",
    },
    "span": {
        "color": "white",
        "padding": "14px",
    },
    "active": {
        # "background-color": "white",
        "color": "Orange",
        "font-weight": "bold",
        "padding": "14px",
    }
}

options = {
    "show_menu": False,
    "show_sidebar": False,
}

page = st_navbar (
    pages,
    urls = urls,
    styles = nav_bar_styles,
    options = options
)
# Fetch the company name for the title
def get_company_name(company_id):
    query = f"""
    SELECT company_name 
    FROM Company 
    WHERE company_id = '{company_id}';
    """
    result = get_data(query)
    return result.iloc[0]['company_name'] if not result.empty else "Company"

# Fetch list of buildings for filter
def get_buildings(company_id):
    query = f"""
    SELECT building_name 
    FROM Building 
    WHERE company_id = '{company_id}';
    """
    return get_data(query)['building_name'].tolist()

# Fetch list of security personnel for filter
def get_security_personnel(company_id):
    query = f"""
    SELECT full_name 
    FROM SecurityPersonnel 
    WHERE building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}');
    """
    return get_data(query)['full_name'].tolist()

# Fetch total visitors per day for a specific building
def get_visitors_per_day(building_name):
    query = f"""
    SELECT 
        DATE(v.signin_time) AS visit_date,
        COUNT(v.id) AS total_visitors
    FROM 
        Visitors v
    LEFT JOIN 
        Building b ON v.building_id = b.building_id
    WHERE 
        b.building_name = '{building_name}'
    GROUP BY 
        DATE(v.signin_time)
    ORDER BY 
        visit_date;
    """
    return get_data(query)

# Average Visitor Duration - Metric Display
def convert_to_hours_and_minutes(total_seconds):
    minutes = total_seconds // 60
    hours = minutes // 60
    minutes %= 60
    return f"{hours}h {minutes}m"

# Summary Metrics - Total Buildings, Visitors, and Organizations
def summary_metrics(company_id):
    query = f"""
    SELECT 
        COUNT(DISTINCT b.building_id) AS total_buildings,
        COUNT(DISTINCT v.id) AS total_visitors,
        COUNT(DISTINCT o.organization_id) AS total_organizations,
        COUNT(DISTINCT sp.national_id_number) AS total_personnel
    FROM 
        Company c
    LEFT JOIN 
        Building b ON c.company_id = b.company_id
    LEFT JOIN 
        Visitors v ON b.building_id = v.building_id
    LEFT JOIN 
        Organizations o ON b.building_id = o.building_id
    LEFT JOIN
        SecurityPersonnel sp ON b.building_id = sp.building_id
    WHERE 
        c.company_id = '{company_id}';
    """
    return get_data(query)

# Company Overview - Total Buildings and Organizations
def company_overview(company_id):
    query = f"""
    SELECT 
        b.building_name,
        COUNT(o.organization_name) AS total_organizations
    FROM 
        Company c
    LEFT JOIN 
        Building b ON c.company_id = b.company_id
    LEFT JOIN 
        Organizations o ON b.building_id = o.building_id
    WHERE 
        c.company_id = '{company_id}'
    GROUP BY 
        b.building_name;
    """
    return get_data(query)

# Visitor Analytics - Total Visitors Per Building
def visitor_analytics_building(company_id):
    query = f"""
    SELECT 
        b.building_name,
        COUNT(v.id) AS total_visitors
    FROM 
        Building b
    LEFT JOIN 
        Visitors v ON b.building_id = v.building_id
    WHERE 
        b.company_id = '{company_id}'
    GROUP BY 
        b.building_name;
    """
    return get_data(query)

# Fetch top 3 buildings with the highest traffic on any given day
def top_3_buildings_high_traffic(company_id):
    query = f"""
    SELECT 
        subquery.building_name,
        MAX(subquery.daily_visitors) AS max_daily_visitors
    FROM (
        SELECT 
            b.building_name,
            COUNT(v.id) AS daily_visitors,
            v.signin_time::DATE AS visit_date
        FROM 
            Visitors v
        LEFT JOIN 
            Building b ON v.building_id = b.building_id
        WHERE 
            b.company_id = '{company_id}'
        GROUP BY 
            b.building_name, v.signin_time::DATE
    ) AS subquery
    GROUP BY 
        subquery.building_name
    ORDER BY 
        max_daily_visitors DESC
    LIMIT 3;
    """
    return get_data(query)


# Visitor Analytics - Visitors Signed In by Security Personnel
def visitor_analytics_personnel(company_id):
    query = f"""
    SELECT 
        sp.full_name AS security_personnel,
        COUNT(v.id) AS visitors_signed_in
    FROM 
        SecurityPersonnel sp
    LEFT JOIN 
        Visitors v ON sp.work_id_number = v.signed_in_by
    WHERE 
        sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}')
    GROUP BY 
        sp.full_name;
    """
    return get_data(query)

# Performance Metrics - Average Visitor Duration
def avg_visitor_duration(company_id):
    query = f"""
    SELECT 
        AVG(EXTRACT(EPOCH FROM (v.signout_time - v.signin_time))) AS avg_visit_duration
    FROM 
        Visitors v
    LEFT JOIN 
        Building b ON v.building_id = b.building_id
    WHERE 
        b.company_id = '{company_id}';
    """
    return get_data(query)

# Fetch total visitors to the company for the last 7 days
def visitors_last_7_days_company(company_id):
    query = f"""
    SELECT 
        DATE(v.signin_time) AS visit_date,
        COUNT(v.id) AS total_visitors
    FROM 
        Visitors v
    LEFT JOIN 
        Building b ON v.building_id = b.building_id
    WHERE 
        b.company_id = '{company_id}' AND v.signin_time >= NOW() - INTERVAL '7 days'
    GROUP BY 
        visit_date
    ORDER BY 
        visit_date;
    """
    return get_data(query)

# Function to create a gauge chart for top 3 buildings in traffic
def create_gauge_chart(building_name, value, max_value):
    return go.Figure(go.Indicator(
        mode="gauge+number",
        value=value,
        title={'text': building_name},
        gauge={
            'axis': {'range': [0, max_value]},
            'bar': {'color': "darkblue"},
            'steps': [
                {'range': [0, max_value*0.5], 'color': "lightgray"},
                {'range': [max_value*0.5, max_value], 'color': "gray"}],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': value}}))

# Function to fetch the total personnel in a company
def get_total_personnel(company_id):
    query = f"""
    SELECT COUNT(DISTINCT sp.work_id_number) AS total_personnel
    FROM SecurityPersonnel sp
    LEFT JOIN Building b ON sp.building_id = b.building_id
    WHERE b.company_id = '{company_id}';
    """
    result = get_data(query)
    return result.iloc[0]['total_personnel'] if not result.empty else 0


# Sample Company ID - Replace with dynamic input
company_id = 'CMP111'

# Fetch the company name for the title
company_name = get_company_name(company_id)

# Set up the title and sidebar
st.title(":blue[{} Analytics]".format(company_name))

# Sidebar dropdowns
with st.sidebar:
    selected_item = option_menu(
            menu_title = "Menu",
            options = ["Home","Buildings","Personnel"]
        )

if selected_item == 'Home':

    # Display Summary Cards
    st.subheader(":gray[Summary Metrics]")
    summary_data = summary_metrics(company_id)
    avg_duration = avg_visitor_duration(company_id)

    col1, col2, col3, col4, col5 = st.columns(5)
    col1.metric(label="Total Buildings", value=int(summary_data.iloc[0]['total_buildings']))
    col2.metric(label="Total Visitors", value=int(summary_data.iloc[0]['total_visitors']))
    col3.metric(label="Total Organizations", value=int(summary_data.iloc[0]['total_organizations']))
    col4.metric(label="Total Personnel", value=int(summary_data.iloc[0]['total_personnel']))
    # col5.metric(label="Avg Visitor Duration", value=convert_to_hours_and_minutes(int(avg_duration.iloc[0]['avg_visit_duration'])))

    st.markdown("---")
    # Company Overview and Visitor Analytics (Horizontal Layout)
    st.subheader(':gray[Company Overview & Visitor Analytics]')
    col1, col2 = st.columns(2)

    # Total Organizations Chart (Horizontal Bar)
    overview_data = company_overview(company_id)
    with col1:
        st.write("Total Organizations by Building")
        org_chart = alt.Chart(overview_data).mark_bar(color="#2196F3").encode(
            x=alt.X('total_organizations:Q', title='Total Organizations'),
            y=alt.Y('building_name:N', sort='-x', title='Building Name'),
            tooltip=['building_name', 'total_organizations']
        ).properties(
            height=300,
            width=400
        )
        st.altair_chart(org_chart, use_container_width=False)

    # st.markdown("---")

    # Total Visitors Pie Chart
    visitors_building = visitor_analytics_building(company_id)
    with col2:
        st.write("Total Visitors by Building (Percentage)")
        visitors_building['percentage'] = (visitors_building['total_visitors'] / visitors_building['total_visitors'].sum()) * 100
        pie_chart = alt.Chart(visitors_building).mark_arc().encode(
            theta=alt.Theta(field="percentage", type="quantitative"),
            color=alt.Color(field="building_name", type="nominal"),
            tooltip=['building_name', 'total_visitors', 'percentage']
        ).properties(
            height=300,
            width=400
        )
        st.altair_chart(pie_chart, use_container_width=False)
    

    col1, col2 = st.columns(2)
    with col1: 
        # Fetch data
        visitors_last_7_days_data = visitors_last_7_days_company(company_id)

        # Display area chart with line for the last 7 days
        st.subheader("Total Visitors in the Last 7 Days")
        company_area_chart = alt.Chart(visitors_last_7_days_data).mark_area(
            color="#2196F3", 
            # background="white",
            line={'color': '#2196F3'},
            opacity=0.3
        ).encode(
            x=alt.X('visit_date:T', title='Date', axis=alt.Axis(format='%a, %b %d', labelAngle=-45)),
            y=alt.Y('total_visitors:Q', title='Total Visitors'),
            tooltip=['visit_date:T', 'total_visitors:Q']
        ).properties(
            height=400,
            width=500
        )

        # Add points on the line
        points = alt.Chart(visitors_last_7_days_data).mark_point(
            color='red',
            size=80
        ).encode(
            x='visit_date:T',
            y='total_visitors:Q',
            tooltip=['visit_date:T', 'total_visitors:Q']
        )

        st.altair_chart(company_area_chart+points, use_container_width=False)
    
    with col2:
        
        # Fetch the top 3 buildings with the highest traffic on any given day
        top_traffic_data = top_3_buildings_high_traffic(company_id)

        # Display the chart
        st.subheader(":orange[Top 3 Buildings with Highest Traffic]")

        traffic_chart = alt.Chart(top_traffic_data).mark_bar(color="#FF7043", size=40).encode(
            x=alt.X('building_name:N', title='Building Name'),
            y=alt.Y('max_daily_visitors:Q', title='Max Daily Visitors'),
            tooltip=['building_name:N', 'max_daily_visitors:Q']
        ).properties(
            height=400,
            width=500
        )

        st.altair_chart(traffic_chart, use_container_width=False)
    
    # Visitor Analytics - Visitors Signed In by Security Personnel
    st.subheader('Personnel Analytics')
    visitors_personnel = visitor_analytics_personnel(company_id)
    st.bar_chart(visitors_personnel.set_index('security_personnel'), use_container_width=True)


    # # Fetch data
    # top_buildings_data = top_3_buildings_high_traffic(company_id)

    # # Display bar chart for top 3 buildings with average high traffic
    # st.subheader(":gray[Top 3 Buildings with Highest Average Daily Traffic]")
    # # Get the maximum average traffic to set the gauge's upper limit
    # max_avg_traffic = top_buildings_data['avg_daily_visitors'].max()

    # # Display each gauge chart
    # for _, row in top_buildings_data.iterrows():
    #     gauge_chart = create_gauge_chart(row['building_name'], row['avg_daily_visitors'], max_avg_traffic)
    #     st.plotly_chart(gauge_chart, use_container_width=True)
        
elif selected_item == 'Buildings':
    building_filter = st.sidebar.selectbox('Select Building', get_buildings(company_id))
    if building_filter:
        st.subheader(f"Analytics for :green[{building_filter}]")
        visitors_per_day = get_visitors_per_day(building_filter)
        st.line_chart(visitors_per_day.set_index('visit_date'), use_container_width=True)

elif selected_item == 'Personnel':
    personnel_filter = st.sidebar.selectbox('Select Personnel', get_security_personnel(company_id))
    if personnel_filter:
        st.subheader(f"Analytics for :orange[{personnel_filter}]")

        st.write("Total visitors Registered: ")

        # Query to get total visitors registered today by the selected personnel
        def get_visitors_today(personnel_name, company_id):
            query = f"""
            SELECT COUNT(v.id) AS total_visitors_today
            FROM Visitors v
            LEFT JOIN SecurityPersonnel sp ON v.signed_in_by = sp.work_id_number
            WHERE sp.full_name = '{personnel_name}'
            AND v.signin_time::date = CURRENT_DATE
            AND sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}');
            """
            return get_data(query).iloc[0]['total_visitors_today']

        # Query to get total visitors registered this week by the selected personnel
        def get_visitors_this_week(personnel_name, company_id):
            query = f"""
            SELECT COUNT(v.id) AS total_visitors_week
            FROM Visitors v
            LEFT JOIN SecurityPersonnel sp ON v.signed_in_by = sp.work_id_number
            WHERE sp.full_name = '{personnel_name}'
            AND EXTRACT(WEEK FROM v.signin_time) = EXTRACT(WEEK FROM CURRENT_DATE)
            AND sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}');
            """
            return get_data(query).iloc[0]['total_visitors_week']

        # Query to get total visitors registered this month by the selected personnel
        def get_visitors_this_month(personnel_name, company_id):
            query = f"""
            SELECT COUNT(v.id) AS total_visitors_month
            FROM Visitors v
            LEFT JOIN SecurityPersonnel sp ON v.signed_in_by = sp.work_id_number
            WHERE sp.full_name = '{personnel_name}'
            AND EXTRACT(MONTH FROM v.signin_time) = EXTRACT(MONTH FROM CURRENT_DATE)
            AND sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}');
            """
            return get_data(query).iloc[0]['total_visitors_month']

        # Query to get total visitors registered by the selected personnel
        def get_total_visitors(personnel_name, company_id):
            query = f"""
            SELECT COUNT(v.id) AS total_visitors
            FROM Visitors v
            LEFT JOIN SecurityPersonnel sp ON v.signed_in_by = sp.work_id_number
            WHERE sp.full_name = '{personnel_name}'
            AND sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}');
            """
            return get_data(query).iloc[0]['total_visitors']
        
        #Query to get the total visitors over the last 7 days
        def visitors_by_personnel_last_7_days(personnel_name, company_id):
            query = f"""
            SELECT
                v.signin_time::date AS visit_date,
                COUNT(v.id) AS total_visitors
            FROM 
                visitors v
            LEFT JOIN
                SecurityPersonnel sp ON v.signed_in_by = sp.work_id_number
            WHERE
                sp.full_name = '{personnel_name}'
                AND v.signin_time::date >= CURRENT_DATE - INTERVAL '7 days'
                AND sp.building_id IN (SELECT building_id FROM Building WHERE company_id = '{company_id}')
            GROUP BY
                v.signin_time::date
            ORDER BY 
                visit_date;
            """
            return get_data(query)

        # Fetch data for the selected personnel
        visitors_today = get_visitors_today(personnel_filter, company_id)
        visitors_week = get_visitors_this_week(personnel_filter, company_id)
        visitors_month = get_visitors_this_month(personnel_filter, company_id)
        total_visitors = get_total_visitors(personnel_filter, company_id)
        visitors_last_7_days = visitors_by_personnel_last_7_days(personnel_filter, company_id)

        # Display cards for the selected personnel
        col1, col2, col3, col4 = st.columns(4)
        col1.metric(label="Today", value=int(visitors_today))
        col2.metric(label="This Week", value=int(visitors_week))
        col3.metric(label="This Month", value=int(visitors_month))
        col4.metric(label="All Time", value=int(total_visitors))

        # Bar chart for total visitors for the last 7 days
        st.subheader("Total visitors Registered over the last 7 days")
        bar_chart = alt.Chart(visitors_last_7_days).mark_bar(color="steelblue", size = 40).encode(
            x=alt.X('visit_date:T', title='Date', axis=alt.Axis(format='%a, %b %d', labelAngle=-45)),
            y=alt.Y('total_visitors:Q', title='Total Visitors'),
            tooltip=['visit_date:T', 'total_visitors:Q']
        )
        # Line chart
        line_chart = alt.Chart(visitors_last_7_days).mark_line(color="orange", size=2).encode(
            x=alt.X('visit_date:T'),
            y=alt.Y('total_visitors:Q')
        )

        # Combine the bar and line charts
        combined_chart = alt.layer(bar_chart, line_chart).properties(
            width=500,
            height=400
        )

        st.altair_chart(combined_chart, use_container_width=False)
    

st.write("End of Dashboard.")
