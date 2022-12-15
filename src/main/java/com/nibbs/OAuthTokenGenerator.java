package com.nibbs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibbs.domain.ScheduleRequest;
import com.opencsv.CSVWriter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OAuthTokenGenerator {

    @Autowired
    private ObjectMapper objectMapper;

    public static final String TMP_FOLDER = "tmp";
    public static final String FILE_SEPERATOR = System.getProperty("file.separator");
    public static final String LOCAL_DOWNLOAD_FOLDER = TMP_FOLDER +  FILE_SEPERATOR + "download" + FILE_SEPERATOR;

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        String authority="https://apitest.nibss-plc.com.ng/reset";
        String client_id="21ed147c-d2a4-4cf9-88f7-51a2d36d7f88";
         String client_secret="NTb8Q~WzJs6SiBloq7Y2N9tzYo3SJ7-dcXdQbcUj";
        String scope="21ed147c-d2a4-4cf9-88f7-51a2d36d7f88/.default";

        //String token = generateToken(scope, client_id, client_secret, authority);
        //System.out.println(token);

        ScheduleRequest schedule = ScheduleRequest.builder()
                .title("Test0221Jul2022")
                .debitBankCode("998")
                .debitAccountNumber("0000014575")
                .debitDescription("Test")
                .paymentMode("Any")
                .referenceNo("121232324242426666666")
                .scheduleType("None")
                .build();

        List<ScheduleRequest> list = new ArrayList<ScheduleRequest>();
        list.add(schedule);
        list.add(schedule);
        list.add(schedule);
        extractToCsv(list);
    }

    public static Path extractToCsv(List<ScheduleRequest> payments) {
        String thisTime = Instant.now().toString();
        String local_path = LOCAL_DOWNLOAD_FOLDER + thisTime;
        Path path = Paths.get(local_path);
        path.getParent().toFile().mkdirs();
        try {
            //CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()));
            CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
            String[] header="FIRSTNAME,LASTNAME,AGE".split(",");
            writer.writeNext(header);
            String data[] = new String[3];
            for (ScheduleRequest payment: payments
            ) {
                data[0] = payment.getDebitAccountNumber();
                data[1] = payment.getPaymentMode();
                data[2] = payment.getScheduleType();
                writer.writeNext(data);
            }
            writer.close();
            System.out.println("CSV file created succesfully.");
        } catch (Exception e) {
            System.out.println("exception :" + e.getMessage());
        }
        return path;
    }

    public static String generateToken0(
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

    public static String generateToken1(
            String SCOPE,
            String CLIENT_ID,
            String CLIENT_SECRET,
            String AUTHORITY)
            throws IOException {

        String parameters = "scope=" + URLEncoder.encode(SCOPE, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&grant_type=client_credentials";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED.toString());
        //headers.add("Accept", MediaType.APPLICATION_JSON.toString()); //Optional in case server sends back JSON data
        //headers.add("Content-Length", "" + parameters.getBytes().length);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("scope", URLEncoder.encode(SCOPE, java.nio.charset.StandardCharsets.UTF_8.toString()));
        map.add("client_id", URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8.toString()));
        map.add("client_secret", URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8.toString()));
        map.add("grant_type", "client_credentials");

        HttpEntity<String> entity = new HttpEntity<String>(parameters, headers);
        //HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<?> response =
                new RestTemplate().postForObject(AUTHORITY,entity, ResponseEntity.class);

        return response.getBody().toString();
    }

    public static String generateToken(
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

    public static String generateToken3(
            String SCOPE,
            String CLIENT_ID,
            String CLIENT_SECRET,
            String AUTHORITY)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {

        String parameters = "scope=" + URLEncoder.encode(SCOPE, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&grant_type=client_credentials";

        // Create a context that doesn’t check certificates.
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustManager = getTrustManager();
        sslContext.init(null, trustManager, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        URL url = new URL(AUTHORITY);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", "" + parameters.getBytes().length);
        connection.setDoOutput(true);
        connection.connect();

        /*BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        out.write(parameters);
        out.close();*/

        // Everything's set up; send the XML that was read in to bytesSOAP.
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                        connection.getOutputStream(), 100000));
        out.write( parameters.getBytes() );
        out.flush();
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

    private static TrustManager[] getTrustManager() {
        TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String t) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String t) {
            }
        }
        };
        return certs;
    }

}