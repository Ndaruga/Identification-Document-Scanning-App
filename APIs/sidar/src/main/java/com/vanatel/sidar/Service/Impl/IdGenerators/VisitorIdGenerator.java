package com.vanatel.sidar.Service.Impl.IdGenerators;

import com.vanatel.sidar.DataBaseRepository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static com.vanatel.sidar.Controller.CompanyAPIController.logger;

@Component
@Service
public class VisitorIdGenerator {

    @Autowired
    VisitorRepository visitorRepository;

    public Integer generateVisitorId() {
        Integer lastVisitorId = visitorRepository.findLastVisitorID();
        if (lastVisitorId != null){
            int newVisitorIdNumber = lastVisitorId + 1;
            logger.info("Generated Visitor id: {}", newVisitorIdNumber);
            return newVisitorIdNumber;
        } else {
            return 1;
        }
    }

}
