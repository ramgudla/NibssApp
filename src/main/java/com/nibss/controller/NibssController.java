package com.nibss.controller;

import com.nibss.service.NibssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NibssController {
	
	 private static Logger log = LoggerFactory.getLogger(NibssController.class);
	
	@Autowired
    private NibssService nibssService;
	
	@GetMapping(path = "/create/schedules")
	public ResponseEntity<Integer> createSchedules() throws Exception {
		log.info("Create Schedule processing initiated...");
		nibssService.createSchedules();
		log.info("Finished execution : createSchedule() method");
		return ResponseEntity.accepted().build();
	}

	@GetMapping(path = "/process/payments")
	public ResponseEntity<Integer> processPayments() throws Exception {
		log.info("Payments processing initiated...");
		nibssService.processPayments();
		log.info("Finished execution : processPayments() method");
		return ResponseEntity.accepted().build();
	}
	
}
