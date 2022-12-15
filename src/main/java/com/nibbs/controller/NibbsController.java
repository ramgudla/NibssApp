package com.nibbs.controller;

import com.nibbs.domain.ScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.nibbs.service.NibbsService;

@RestController
public class NibbsController {
	
	 private static Logger log = LoggerFactory.getLogger(NibbsController.class);
	
	@Autowired
    private NibbsService nibbsService;
	
	@GetMapping(path = "/create/schedules")
	public ResponseEntity<Integer> createSchedules() throws Exception {
		log.info("Create Schedule processing initiated...");
		nibbsService.createSchedules();
		log.info("Finished execution : createSchedule() method");
		return ResponseEntity.accepted().build();
	}

	@GetMapping(path = "/process/payments")
	public ResponseEntity<Integer> processPayments() throws Exception {
		log.info("Payments processing initiated...");
		nibbsService.processPayments();
		log.info("Finished execution : processPayments() method");
		return ResponseEntity.accepted().build();
	}
	
}
