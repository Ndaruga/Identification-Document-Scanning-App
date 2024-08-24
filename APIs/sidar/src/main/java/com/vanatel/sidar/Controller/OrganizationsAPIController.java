package com.vanatel.sidar.Controller;

import com.vanatel.sidar.Model.OrganizationDetails;
import com.vanatel.sidar.Service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/company")
public class OrganizationsAPIController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationsAPIController.class);

    @Autowired
    OrganizationService organizationService;

    @PostMapping("/buildings/organizations/register")
    public ResponseEntity<Map<String, String>> addOrganization(@Validated @RequestBody OrganizationDetails organizationDetails) {
        logger.info("Received organization details: {}", organizationDetails);

        if (organizationDetails.getBuildingId() == null || organizationDetails.getBuildingId().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Building ID is null or empty"), HttpStatus.BAD_REQUEST);
        }
        organizationService.addOrganization(organizationDetails);
        logger.info("Building ID {} for organization {} registered successfully", organizationDetails.getBuildingId(), organizationDetails.getOrganizationName());
        return new ResponseEntity<>(Map.of("message", "Organization registered successfully"), HttpStatus.OK);
    }

    @GetMapping("/building/organizations/list")
    public ResponseEntity<List<OrganizationDetails>> getOrganizationsByBuildingID(@RequestParam("BuildingID") String buildingID) {
        List<OrganizationDetails> organizations = organizationService.getOrganizationsByBuildingId(buildingID);
        return new ResponseEntity<>(organizations, HttpStatus.OK);
    }

    @GetMapping("/building/organizations/byFloor")
    public ResponseEntity<List<OrganizationDetails>> getOrganizationsByFloor(@RequestParam("BuildingID") String buildingID, @RequestParam("floorNumber") int floorNumber) {

        List<OrganizationDetails> organizationsByFloors = organizationService.findByBuildingIdAndFloorNumber(buildingID, floorNumber);
        return new ResponseEntity<>(organizationsByFloors, HttpStatus.OK);
    }
}
