package com.nibss.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibss.dao.ScheduleDAO;
import com.nibss.domain.CloseScheduleRequest;
import com.nibss.domain.PaymentsResponse;
import com.nibss.domain.ScheduleRequest;
import com.nibss.domain.ScheduleResponse;
import com.nibss.exception.NibssException;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
import java.net.URLEncoder;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NibssService {

    private static Logger log = LoggerFactory.getLogger(NibssService.class);

    @Value("${nibbs.url}")
    private String NIBSS_URL;

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

    private static final String CLOSE_SCHEDULE_EP = "nibsspayplus/v2/Schedules/Close";

    private static final String UPLOAD_PAYMENTS_EP = "nibsspayplus/v2/Schedules";

    private static String token = "";

    @Async("asyncExecutor")
    public void createSchedules() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Schedules...");

        log.info("Nibbs Url: " + NIBSS_URL);

        List<ScheduleRequest> scheduleRequests = scheduleDAO.findAllSchedules("");
        List<ScheduleRequest> failedRequests = new ArrayList<ScheduleRequest>();
        log.info("Schedule Requests: " + scheduleRequests);
        String endpointUrl = NIBSS_URL + CREATE_SCHEDULE_EP;
        log.info("Creating Schedules against Endpoint URL : {} ", endpointUrl);

        String tokenResponse = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
        String token = stringToJosnNode(tokenResponse).get("access_token").textValue();
        String authHeader = "Bearer " + token;

        // query schedules from erp staging table and create in nibbs
        for (ScheduleRequest scheduleRequest : scheduleRequests) {
            try {
                log.info("Sending Create Schedule Request : {}\n", objToJson(scheduleRequest));
                Map<Integer, String> responseMap = send(endpointUrl, objToJson(scheduleRequest), authHeader);
                int status = (int) responseMap.keySet().toArray()[0];
                String response = responseMap.get(status);
                log.info("Create Schedule Response for the reference number {} :\n {}", scheduleRequest.getReferenceNo(), response);

                if (status == HttpURLConnection.HTTP_OK) {
                    ScheduleResponse scheduleResponse = objectMapper.readValue(response, ScheduleResponse.class);
                    // process each response from access bank back to erp staging table.
                    log.info("Processing response from nibbs to erp staging table for schedule request reference number : {} with scheduleId : {}", scheduleRequest.getReferenceNo(), scheduleResponse.getData().scheduleId);
                    scheduleDAO.updateScheduleRecord(scheduleRequest, scheduleResponse);
                    log.info("Successfully processed response from nibbs to erp staging table for the reference number: {} with scheduleId : {}", scheduleRequest.getReferenceNo(), scheduleResponse.getData().scheduleId);
                }
                else {
                    scheduleDAO.updateFailedScheduleRecord(scheduleRequest, response);
                    log.info("Create Schedule failed for the reference number {}. Nibss erp staging table was updated with the failure message.", scheduleRequest.getReferenceNo());
                }

            } catch (Exception ex) {
                log.info("HttpClientErrorException. Error Creating Schedule for reference: {}. The error is: ", scheduleRequest.getReferenceNo(), ex);
            }
        }

        log.info("All the Schedules got created. There may be some failures. Please check the ERP database and server logs to take corrective actions if necessary.");
        log.info("#################################END################################\n");
    }

    @Async("asyncExecutor")
    public void processPayments() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Payments...");
        String tokenResponse = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
        String token = stringToJosnNode(tokenResponse).get("access_token").textValue();
        String authHeader = "Bearer " + token;

        Map<String, Path> scheduleIdPaymentsFileMap = scheduleDAO.findAllPayments("NEW");
        String closeScheduleEp =  NIBSS_URL + CLOSE_SCHEDULE_EP;

        scheduleIdPaymentsFileMap.forEach((scheduleId, csvFile) ->
            {
                String endpointUrl =  NIBSS_URL + UPLOAD_PAYMENTS_EP + "/" + scheduleId + "?scheduleId=" + scheduleId;
                try {
                    Map<Integer, String> responseMap = uploadOkHttp(endpointUrl, csvFile.toFile(), authHeader);
                    int status = (int) responseMap.keySet().toArray()[0];
                    String response = responseMap.get(status);
                    log.info("Create Payment Response for schedule id {} :\n {}", scheduleId, response);
                    if (status == HttpURLConnection.HTTP_OK) {

                        // payment is successful, close the schedule.
                        CloseScheduleRequest closeScheduleRequest = CloseScheduleRequest.builder().scheduleId(scheduleId).build();
                        Map<Integer, String> closeSchResMap = send(closeScheduleEp, objToJson(closeScheduleRequest), authHeader);
                        int closeStatus = (int) closeSchResMap.keySet().toArray()[0];
                        String closeResponse = closeSchResMap.get(closeStatus);
                        log.info("Close Schedule Response for schedule id {} :\n {}", scheduleId, closeResponse);
                        String CLOSE_SCHEDULE_STATUS = "PROCESSED";
                        String CLOSE_SCHEDULE_RESCODE = status == HttpURLConnection.HTTP_OK ? "SUCCESS" : "REJECTED";
                        String CLOSE_SCHEDULE_RESMSG = closeResponse;

                        PaymentsResponse paymentsResponse = objectMapper.readValue(response, PaymentsResponse.class);
                        log.info("Processing response from nibss to erp staging table for schedule id: {}", scheduleId);
                        scheduleDAO.updatePaymentsRecord(scheduleId, CLOSE_SCHEDULE_STATUS, CLOSE_SCHEDULE_RESCODE, CLOSE_SCHEDULE_RESMSG, paymentsResponse);
                        log.info("Successfully processed response from nibss to erp staging table for the schedule id: {}", scheduleId);
                    }
                    else {
                        scheduleDAO.updateFailedPaymentsRecord(scheduleId, response);
                        log.info("Create Payment failed for the schedule id {}. nibss erp staging table was updated with the failure message.", scheduleId);
                    }

                } catch (IOException | NibssException e) {
                    log.info("Error occurred while uploading payments file {} to schedule {}. The error msg is: {}. Exception is : {} ", csvFile.toAbsolutePath(), scheduleId, e.getMessage(), e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    csvFile.toFile().delete();
                }
            }
        );

        log.info("All the Payments got processed. There may be some failures. Please check the ERP database and server logs to take corrective actions if necessary.");
        log.info("#################################END################################\n");
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

    private Map<Integer, String> send(String urlStr, String request, String authHeader) throws Exception {
        Map<Integer, String> responseMap = new HashMap<>();
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

        StringBuilder sb = new StringBuilder();
        // checks server's status code first
        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            connection.disconnect();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            log.info("Server returned non-OK status {}. Fault payload is: {}", status, sb.toString());
            connection.disconnect();
            //throw new NibbsException("Server failed to execute the request.");
        }
        responseMap.put(status, sb.toString());

        //return sb.toString();
        return responseMap;
    }

    private Map<Integer, String> uploadOkHttp(String urlStr, File file, String authHeader) throws IOException, NibssException {
        Map<Integer, String> responseMap = new HashMap<>();
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient().newBuilder()
                .build();
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("text/csv");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file","test1.csv",
                        RequestBody.create(mediaType, file))
                .build();
        okhttp3.Request request = new Request.Builder()
                .url(urlStr)
                .method("POST", body)
                .addHeader("Authorization", authHeader).build();
        Response response = client.newCall(request).execute();
        responseMap.put(response.code(), getStringFromInputStream(response.body().byteStream()));
        return responseMap;
        /*if (response.isSuccessful()) {
            return getStringFromInputStream(response.body().byteStream());
        }
        else {
            throw new NibbsException("Exception occurred while uploading payments file. Fault payload is:" + getStringFromInputStream(response.body().byteStream()));
        }*/
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

    // convert InputStream to String
    private String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        final StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    @Async("asyncExecutor")
    public void createSchedules_old() throws Exception {
        log.info("#################################START################################");
        log.info("Retrieving Schedules...");

        log.info("Nibbs Url: " + NIBSS_URL);

        List<ScheduleRequest> scheduleRequests = scheduleDAO.findAllSchedules_dummy("");
        List<ScheduleRequest> failedRequests = new ArrayList<ScheduleRequest>();
        log.info("Schedule Requests: " + scheduleRequests);
        String endpointUrl = NIBSS_URL + CREATE_SCHEDULE_EP;
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