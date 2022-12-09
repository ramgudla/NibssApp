package com.nibbs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class OAuth2Token {

    //configuration, mostly extracted from Azure --> app registered as web application
    private final static String CLIENT_ID = "21ed147c-d2a4-4cf9-88f7-51a2d36d7f88";
    private final static String CLIENT_SECRET = "NTb8Q~WzJs6SiBloq7Y2N9tzYo3SJ7-dcXdQbcUj";
    private final static String SCOPE = "21ed147c-d2a4-4cf9-88f7-51a2d36d7f88/.default";
    private final static String AUTHORITY = "https://apitest.nibss-plc.com.ng/reset";

    @Autowired
    private ObjectMapper objectMapper;

    public static void main(String[] args) throws Exception {
        String token = generateToken(SCOPE, CLIENT_ID, CLIENT_SECRET, AUTHORITY);
        System.out.println("Access Token - " + token);
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
        System.out.println("parameters: " + parameters);
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
        System.out.println(response.toString());

        return response.toString();
    }
}