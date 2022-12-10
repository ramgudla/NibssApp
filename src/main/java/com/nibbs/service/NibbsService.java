package com.nibbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibbs.dao.ScheduleDAO;
import com.nibbs.domain.ScheduleRequest;
import com.nibbs.domain.ScheduleResponse;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
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
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class NibbsService {

    private static Logger log = LoggerFactory.getLogger(NibbsService.class);

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

    private static final String JSON_CONVERSION_FAILED = "JSON_RESPONSE_CONVERSION_FAILED";

    private static final String CREATE_SCHEDULE_EP = "nibsspayplus/v2/Schedules/create";

    private static String token = "";

    @Async("asyncExecutor")
    public void createSchedules() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Schedules...");

        log.info("Nibbs Url: " + NIBBS_URL);

        List<ScheduleRequest> scheduleRequests = scheduleDAO.findAllSchedules("");
        List<ScheduleRequest> failedRequests = new ArrayList<ScheduleRequest>();
        log.info("Schedule Requests: " + scheduleRequests);
        String endpointUrl = NIBBS_URL + CREATE_SCHEDULE_EP;
        log.info("Creating Schedules against Endpoint URL : {} ", endpointUrl);

        // query schedules from erp staging table and create in nibbs
        for (ScheduleRequest scheduleRequest : scheduleRequests) {
            HttpHeaders httpHeaders = createHeaders();
            // requestEntity = Body + Header
            HttpEntity<ScheduleRequest> httpRequest = new HttpEntity<>(scheduleRequest, httpHeaders);
            try {
                log.info("Sending Create Schedule Request : {}\n", objToJson(scheduleRequest));
                ResponseEntity<ScheduleResponse> httpResponse = restTemplate.exchange(endpointUrl, HttpMethod.POST,
                        httpRequest, ScheduleResponse.class);
                log.info("Create Schedule Response is:\n {}", objToJson(httpResponse.getBody()));
                // process each response from access bank back to erp staging table.
                log.info("Processing response from nibbs to erp staging table for schedule request: {}", scheduleRequest.getDebitAccountNumber());
                scheduleDAO.updateScheduleRecord(scheduleRequest, httpResponse.getBody());
                log.info("Successfully processed response from nibbs to erp staging table for the account acct number: {}", scheduleRequest.getDebitAccountNumber());
            } catch (HttpClientErrorException hcee) {
                log.info("HttpClientErrorException. Error Creating Schedule: {}. HTTP Status code is: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), hcee.getRawStatusCode(), hcee);
                log.info("Fault response: {}\n", hcee.getResponseBodyAsString());
            } catch (HttpServerErrorException hsee) {
                log.info("HttpServerErrorException. Error Creating Schedule: {}: {}. HTTP Status code is: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), hsee.getRawStatusCode(), hsee);
                log.info("Fault response: {}\n", hsee.getResponseBodyAsString());
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

    private HttpHeaders createHeaders() throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return new HttpHeaders() {
            private static final long serialVersionUID = 1L;
            {
                /*if ("".equals(token)) {
					String tokenResponse = new OAuth2Token().generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
                    token = stringToJosnNode(tokenResponse).get("access_token").textValue();
                    System.out.println("Token: " + token);
                }*/
                String tokenResponse = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
                String token = stringToJosnNode(tokenResponse).get("access_token").textValue();
                String authHeader = "Bearer " + token;
                set("Authorization", authHeader);
            }
        };
    }

    public static String generateToken(
            String SCOPE,
            String CLIENT_ID,
            String CLIENT_SECRET,
            String AUTHORITY)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        HttpClient httpClient = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        HttpPost httppost = new HttpPost(AUTHORITY);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("scope", SCOPE));
        nameValuePairs.add(new BasicNameValuePair("grant_type", "client_credentials"));
        nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
        nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));

        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpClient.execute(httppost);
        return EntityUtils.toString(response.getEntity());
    }

}