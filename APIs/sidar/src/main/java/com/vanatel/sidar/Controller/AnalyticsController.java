//package com.vanatel.sidar.Controller;
//
//import com.vanatel.sidar.Service.AnalyticsService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/company")
//
//public class AnalyticsController {
//
//    public static final Logger logger = LoggerFactory.getLogger(CompanyAPIController.class);
//
//    @Autowired
//    AnalyticsService analyticsService;
//
//    @GetMapping("/name")
//    public ResponseEntity<String> getCompanyName(@PathVariable String companyId) {
//        logger.info("getCompanyName called");
//        return ResponseEntity.ok(analyticsService.getCompanyName(companyId));
//    }
//
//}
