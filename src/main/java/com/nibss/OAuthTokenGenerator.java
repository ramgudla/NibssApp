package com.nibss;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OAuthTokenGenerator {

    @Autowired
    private ObjectMapper objectMapper;

    public static final String TMP_FOLDER = "tmp";
    public static final String FILE_SEPERATOR = System.getProperty("file.separator");
    public static final String LOCAL_DOWNLOAD_FOLDER = TMP_FOLDER +  FILE_SEPERATOR + "download" + FILE_SEPERATOR;

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

    private String upload0(String urlStr, File file, String authHeader) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Content-Type:", "application/vnd.ms-excel");
        headers.setBearerAuth(authHeader);

        // This nested HttpEntiy is important to create the correct
        // Content-Disposition entry with metadata "name" and "filename"
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename("test.csv")
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(Files.readAllBytes(file.toPath()), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);
        try {
            ResponseEntity<?> response = new RestTemplate().exchange(
                    urlStr,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
        } catch (HttpClientErrorException e) {
            System.out.println(e.getResponseBodyAsString());
            throw e;
        }
        return "successful";
    }


    private String upload(String urlStr, File file, String authHeader) throws IOException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            if (httpConn instanceof HttpURLConnection) {
                //((HttpURLConnection) urlconnection).setRequestMethod("POST");
                httpConn.setRequestProperty("Authorization", authHeader);
                httpConn.setRequestProperty("Content-type", "text/plain");
                httpConn.connect();
            }

            BufferedOutputStream bos = new BufferedOutputStream(httpConn.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int i;
            // read byte by byte until end of stream
            while ((i = bis.read()) > 0) {
                bos.write(i);
            }
            bis.close();
            bos.close();

            List<String> response = new ArrayList<String>();
            // checks server's status code first
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getErrorStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                reader.close();
                System.out.println("Error response: "+response);
                throw new IOException("Server returned non-OK status: " + status);
            }

        } catch (Exception e) {
            throw e;
        }
        return "Successful";
    }

    public String upload2(String urlStr, File file, String authHeader) throws Exception{
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        httpConn.setRequestProperty("Authorization", authHeader);

        String boundary = UUID.randomUUID().toString();
        //connection.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(httpConn.getOutputStream());

        //request.writeBytes("--" + boundary + "\r\n");
        //request.writeBytes("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
        //request.writeBytes("fileDescription" + "\r\n");

        //request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n\r\n");
        request.write(Files.readAllBytes(file.toPath()));
        request.writeBytes("\r\n");

        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();

        List<String> response = new ArrayList<String>();
        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            System.out.println("Error response: "+response);
            throw new IOException("Server returned non-OK status: " + status);
        }
        return "Successful";
    }

    public String upload3(String urlStr, File logFileToUpload, String authHeader) throws Exception{
        // Connect to the web server endpoint
        URL serverUrl =
                new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();

        String boundaryString = "----SomeRandomText";

// Indicate that we want to write to the HTTP request body
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.addRequestProperty("Authorization", authHeader);
        urlConnection.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);

// Include value from the myFileDescription text area in the post data
        /*httpRequestBodyWriter.write("\n\n--" + boundaryString + "\n");
        httpRequestBodyWriter.write("Content-Disposition: form-data; name=\"file\"");
        httpRequestBodyWriter.write("\n\n");
        httpRequestBodyWriter.write("Log file for 20150208");*/

        OutputStream outputStreamToRequestBody = urlConnection.getOutputStream();
        BufferedWriter httpRequestBodyWriter =
                new BufferedWriter(new OutputStreamWriter(outputStreamToRequestBody));

// Include the section to describe the file
        httpRequestBodyWriter.write("\n--" + boundaryString + "\n");
        httpRequestBodyWriter.write("Content-Disposition: form-data;"
                + "name=\"file\";"
                + "filename=\""+ logFileToUpload.getName() +"\""
                + "\nContent-Type: text/csv\n\n");
        httpRequestBodyWriter.flush();

// Write the actual file contents
        FileInputStream inputStreamToLogFile = new FileInputStream(logFileToUpload);

        int bytesRead;
        byte[] dataBuffer = new byte[1024];
        while((bytesRead = inputStreamToLogFile.read(dataBuffer)) != -1) {
            outputStreamToRequestBody.write(dataBuffer, 0, bytesRead);
        }

        outputStreamToRequestBody.flush();

// Mark the end of the multipart http request
        httpRequestBodyWriter.write("\n--" + boundaryString + "--\n");
        httpRequestBodyWriter.flush();

// Close the streams
        outputStreamToRequestBody.close();
        httpRequestBodyWriter.close();

        List<String> response = new ArrayList<String>();
        // checks server's status code first
        int status = urlConnection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            urlConnection.disconnect();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            System.out.println("Error response: "+response);
            throw new IOException("Server returned non-OK status: " + status);
        }

        return "success";
    }

    private String upload5(String urlStr, File file, String authHeader) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());

            // server back-end URL
            HttpPost httppost = new HttpPost(urlStr);
            httppost.setHeader("Authorization", authHeader);
            MultipartEntity entity = new MultipartEntity();

            // set the file input stream and file name as arguments
            entity.addPart("file", new InputStreamBody(fis, file.getName()));
            httppost.setEntity(entity);

            // execute the request
            HttpResponse response = httpclient.execute(httppost);

            int statusCode = response.getStatusLine().getStatusCode();

            org.apache.http.HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, "UTF-8");

            System.out.println("[" + statusCode + "] " + responseString);

        } catch (ClientProtocolException e) {
            System.err.println("Unable to make connection");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Unable to read file");
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {}
        }
        return "success";
    }

    public void uploadTest (String urlStr, File file, String authHeader) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());

            // server back-end URL
            HttpPost httppost = new HttpPost(urlStr);
            MultipartEntity entity = new MultipartEntity();

            // set the file input stream and file name as arguments
            entity.addPart("file", new InputStreamBody(fis, file.getName()));
            httppost.setHeader("Content-Type", "text/csv");
            httppost.setHeader("Authorization", authHeader);
            httppost.setEntity(entity);

            // execute the request
            HttpResponse response = httpclient.execute(httppost);

            int statusCode = response.getStatusLine().getStatusCode();
            org.apache.http.HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, "UTF-8");

            System.out.println("[" + statusCode + "] " + responseString);

        } catch (ClientProtocolException e) {
            System.err.println("Unable to make connection");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Unable to read file");
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {}
        }
    }
}