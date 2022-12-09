package com.nibbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibbs.OAuth2Token;
import com.nibbs.dao.ScheduleDAO;
import com.nibbs.domain.ScheduleRequest;
import com.nibbs.domain.ScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
public class NibbsService {

    private static Logger log = LoggerFactory.getLogger(NibbsService.class);

    private static final String JSON_CONVERSION_FAILED = "JSON_RESPONSE_CONVERSION_FAILED";

    private static final String CREATE_SCHEDULE_EP="nibsspayplus/v2/Schedules/create";

    @Value("${nibbs.url}")
    private String NIBBS_URL;

    @Value("${client_id}")
    private String CLIENT_ID;

    @Value("${client_secret}")
    private String CLIENT_SECRET;

    @Value("${scope}")
    private String SCOPE;

    @Value("${grant_type}")
    private String GRANT_TYPE;

    @Value("${authority}")
    private String AUTHORITY;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ScheduleDAO scheduleDAO;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SUCCESS = "SUCCESS";

    private static String token = "";

    @Async("asyncExecutor")
    public void createSchedules() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Schedules...");

        log.info("Nibbs Url: " + NIBBS_URL);

        List<ScheduleRequest> scheduleRequests = scheduleDAO.findAllSchedules("");
        log.info("Schedule Requests: " + scheduleRequests);
        String endpointUrl = NIBBS_URL + CREATE_SCHEDULE_EP;
        log.info("Creating Schedules against Endpoint URL : {} ", endpointUrl);

        // query schedules from erp staging table and create in nibbs
        for (ScheduleRequest scheduleRequest : scheduleRequests) {
            HttpHeaders httpHeaders = createHeaders();
            // requestEntity = Body + Header
            HttpEntity<ScheduleRequest> httpRequest = new HttpEntity<>(scheduleRequest, httpHeaders);
            try {
                log.info("Sending Create Schedule Request : {}", objToJson(scheduleRequest));
                ResponseEntity<ScheduleResponse> httpResponse = restTemplate.exchange(endpointUrl, HttpMethod.POST,
                        httpRequest, ScheduleResponse.class);
                log.info("Create Schedule Response is:\n {}", objToJson(httpResponse.getBody()));
                // process each response from access bank back to erp staging table.
                log.info("Processing response from nibbs to erp staging table for schedule request: {}", scheduleRequest.getDebitAccountNumber());
                scheduleDAO.updateScheduleRecord(scheduleRequest, httpResponse.getBody());
                log.info("Successfully processed response from nibbs to erp staging table for the BatchRefId: {}", scheduleRequest.getDebitAccountNumber());
            } catch (HttpClientErrorException hcee) {
                log.info("HttpClientErrorException. Error Creating Schedule: {}. HTTP Status code is: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), hcee.getRawStatusCode(), hcee);
            } catch (HttpServerErrorException hsee) {
                log.info("HttpServerErrorException. Error Creating Schedule: {}: {}. HTTP Status code is: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), hsee.getRawStatusCode(), hsee);
            } catch (Exception ex) {
                log.info("HttpClientErrorException. Error Creating Schedule: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), ex);
            }
        }

        log.info("All the Schedules got created. There may be some failures. Please check the ERP database and server logs to take corrective actions if necessary.");
        log.info("#################################END################################\n");

    }

    private String objToJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.debug("failed conversion: object to Json", e);
            return JSON_CONVERSION_FAILED;
        }
    }

	private JsonNode stringToJosnNode(String jsonString) {
		try {
			return objectMapper.readTree(jsonString);
		} catch (IOException e) {
			log.debug("failed conversion: string to JsonNode", e);
			return null;
		}
	}

    private HttpHeaders createHeaders() throws IOException {
        return new HttpHeaders() {
            private static final long serialVersionUID = 1L;
            {
                /*if ("".equals(token)) {
					String tokenResponse = new OAuth2Token().generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
                    token = stringToJosnNode(tokenResponse).get("access_token").textValue();
                    System.out.println("Token: " + token);
                }*/
                String tokenResponse = new OAuth2Token().generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
                token = stringToJosnNode(tokenResponse).get("access_token").textValue();
                System.out.println("Token: " + token);
                String authHeader = "Bearer " + token;
                set("Authorization", authHeader);
            }
        };
    }

}