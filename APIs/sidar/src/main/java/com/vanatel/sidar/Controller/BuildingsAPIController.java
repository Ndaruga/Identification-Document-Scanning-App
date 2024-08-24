package com.vanatel.sidar.Controller;

import ch.qos.logback.classic.LoggerContext;
import com.vanatel.sidar.Model.BuildingDetails;
import com.vanatel.sidar.Service.BuildingService;
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
public class BuildingsAPIController {

    public static final Logger logger = LoggerFactory.getLogger(BuildingsAPIController.class);

    @Autowired
    BuildingService buildingService;

    @PostMapping("/buildings/register")
    public ResponseEntity<Map<String, String>> registerBuilding(@Validated @RequestBody BuildingDetails buildingDetails) {

        if (buildingDetails.getCompanyID() == null || buildingDetails.getCompanyID().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Company ID is null or empty"), HttpStatus.BAD_REQUEST);
        }

        BuildingDetails registeredBuilding = buildingService.registerBuilding(buildingDetails);
        logger.info("Building {} for company ID {} registered successfully", registeredBuilding.getBuildingName(), registeredBuilding.getCompanyID());
        return new ResponseEntity<>(Map.of("message", "Building registered Successfully", "buildingId", registeredBuilding.getBuildingId()), HttpStatus.OK);
    }

    @GetMapping("/buildings/list")
    public ResponseEntity<List<BuildingDetails>> getBuildingsByCompanyID(@RequestParam("companyID") String companyID) {
        List<BuildingDetails> buildings = buildingService.getBuildingsByCompanyId(companyID);
        return new ResponseEntity<>(buildings, HttpStatus.OK);
    }

    @GetMapping("/Building&Company-Name")
    public ResponseEntity<Map<String, Object>> getCompanyAndBuildingNames(@RequestParam String buildingId) {
        return buildingService.getCompanyAndBuildingNames(buildingId)
                .map(details -> new ResponseEntity<>(details, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
