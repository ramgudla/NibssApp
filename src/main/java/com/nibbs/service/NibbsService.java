package com.nibbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        String tokenResponse = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
        String token = stringToJosnNode(tokenResponse).get("access_token").textValue();
        String authHeader = "Bearer " + token;

        // query schedules from erp staging table and create in nibbs
        for (ScheduleRequest scheduleRequest : scheduleRequests) {
            try {
                log.info("Sending Create Schedule Request : {}\n", objToJson(scheduleRequest));
                String response = send(endpointUrl, objToJson(scheduleRequest), authHeader);
                log.info("Create Schedule Response is:\n {}", response);
                ScheduleResponse scheduleResponse = objectMapper.readValue(response, ScheduleResponse.class);
                // process each response from access bank back to erp staging table.
                log.info("Processing response from nibbs to erp staging table for schedule request: {}", scheduleRequest.getDebitAccountNumber());
                scheduleDAO.updateScheduleRecord(scheduleRequest, scheduleResponse);
                log.info("Successfully processed response from nibbs to erp staging table for the account acct number: {}", scheduleRequest.getDebitAccountNumber());
            } catch (Exception ex) {
                log.info("HttpClientErrorException. Error Creating Schedule: {}. The error is: ", scheduleRequest.getDebitAccountNumber(), ex);
            }
        }

        log.info("All the Schedules got created. There may be some failures. Please check the ERP database and server logs to take corrective actions if necessary.");
        log.info("#################################END################################\n");
    }

    @Async("asyncExecutor")
    public void processPayments() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Payments...");

        log.info("Nibbs Url: " + NIBBS_URL);

        String endpointUrl = NIBBS_URL + CREATE_SCHEDULE_EP;
        log.info("Processing Payments against Endpoint URL : {} ", endpointUrl);

        String tokenResponse = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
        String token = stringToJosnNode(tokenResponse).get("access_token").textValue();
        String authHeader = "Bearer " + token;

        Path csvFile = scheduleDAO.findAllPayments("");
        String response = upload(endpointUrl, csvFile.toFile(), authHeader);
        csvFile.toFile().delete();

        log.info("All the Payments got processed. There may be some failures. Please check the ERP database and server logs to take corrective actions if necessary.");
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

    private static String generateToken(
            String SCOPE,
            String CLIENT_ID,
            String CLIENT_SECRET,
            String AUTHORITY)
            throws IOException {

        String parameters = "scope=" + URLEncoder.encode(SCOPE, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&grant_type=client_credentials";

        URL url;
        HttpURLConnection connection = null;
        url = new URL(AUTHORITY);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(parameters.getBytes().length));
        connection.setDoOutput(true);
        connection.connect();

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        out.write(parameters);
        out.close();

        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuffer response = new StringBuffer();
        while((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        return response.toString();
    }

    private String send(String urlStr, String request, String authHeader) throws Exception {
        URL url;
        HttpURLConnection connection = null;
        url = new URL(urlStr);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", authHeader);
        connection.setRequestProperty("Content-Length", "" + Integer.toString(request.getBytes().length));
        connection.setDoOutput(true);
        connection.connect();

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        out.write(request);
        out.close();

        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuffer response = new StringBuffer();
        while((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        return response.toString();
    }

    private String upload(String urlStr, File file, String authHeader) throws IOException {
        URLConnection urlconnection = null;
        try {
            URL url = new URL(urlStr);
            urlconnection = url.openConnection();
            urlconnection.setDoOutput(true);
            urlconnection.setDoInput(true);

            if (urlconnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlconnection).setRequestMethod("POST");
                ((HttpURLConnection) urlconnection).setRequestProperty("Authorization", authHeader);
                ((HttpURLConnection) urlconnection).setRequestProperty("Content-type", "text/plain");
                ((HttpURLConnection) urlconnection).connect();
            }

            BufferedOutputStream bos = new BufferedOutputStream(urlconnection.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int i;
            // read byte by byte until end of stream
            while ((i = bis.read()) > 0) {
                bos.write(i);
            }
            bis.close();
            bos.close();
            System.out.println(((HttpURLConnection) urlconnection).getResponseMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            InputStream inputStream;
            int responseCode = ((HttpURLConnection) urlconnection).getResponseCode();
            if ((responseCode >= 200) && (responseCode <= 202)) {
                inputStream = ((HttpURLConnection) urlconnection).getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
            } else {
                inputStream = ((HttpURLConnection) urlconnection).getErrorStream();
            }
            ((HttpURLConnection) urlconnection).disconnect();

        } catch (IOException e) {
            throw e;
        }
        return "Failed Upload.";
    }

    public void upload2(String urlStr, File file, String authHeader) throws Exception{
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("Authorization", authHeader);

        String boundary = UUID.randomUUID().toString();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(connection.getOutputStream());

        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
        request.writeBytes("fileDescription" + "\r\n");

        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n\r\n");
        request.write(Files.readAllBytes(file.toPath()));
        request.writeBytes("\r\n");

        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();
        int respCode = connection.getResponseCode();

        switch(respCode) {
            case 200:
                //all went ok - read response
                break;
            case 301:
            case 302:
            case 307:
                //handle redirect - for example, re-post to the new location
                break;
            default:
                //do something sensible
        }
    }

    @Async("asyncExecutor")
    public void createSchedules_old() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Schedules...");

        log.info("Nibbs Url: " + NIBBS_URL);

        List<ScheduleRequest> scheduleRequests = scheduleDAO.findAllSchedules_dummy("");
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

}