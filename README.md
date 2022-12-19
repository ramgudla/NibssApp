# Nibbs

## How to Build? ##
      
>Clone the project.\
$ mvn clean install\
$ cd target\
$ zip -r NibbsApp.zip NibbsIntegration-0.0.1-SNAPSHOT.jar config

## How to import cert to truststore? In production may not be needed!##
>**If the nibbs api endpoints certificate is from a wellknown CA, then set the property: 'custom.trust=false' in 'config/application.properties' file after unzipping the NibbsApp.zip file into DMZ server. Otherwise (If its a self signed certificate), export the certificate from the nibbs api host and import into the custom truststore. Do the following:**
      
      ```
      echo quit\
               | openssl s_client -servername apitest.nibss-plc.com.ng -showcerts -connect apitest.nibss-plc.com.ng:443\
               | openssl x509 -outform PEM\
               > apitest.nibss-plc.com.ng
               
      keytool -import -keystore nibbs-truststore.p12 -alias apitest-nibss -file apitest.nibss-plc.com.ng -trustcacerts -storepass changeit
      keytool -list -keystore nibbs-truststore.p12 -storepass changeit
      ```


## How to Run? ##

>In DMZ server:\
    $ cd /home/nibbs\
    $ rm -rf *\
    $ unzip NibbsApp.zip (After unzipping, you will see a folder named 'config'. Inside this folder, there is a file named 'application.properties'. Update the db details in this file.)\
    Kill the running application java process:\
      $ ps -ef | grep NibbsIntegration-0.0.1-SNAPSHOT.jar\
      $ kill -9 `<pid>`\
    Run the Service:\
      $ java -Dserver.port=8081 -Dhttps.proxyHost=172.23.12.67 -Dhttps.proxyPort=4145 -jar NibbsIntegration-0.0.1-SNAPSHOT.jar --logging.config=config/logback-spring.xml --spring.config.location=config/application.properties &


## How to Test? ##
    
>In UAT:\  
curl http://<DMZ_SERVER_IP>:8081/create/schedules
curl http://<DMZ_SERVER_IP>:8081/process/payments

>In DMZ:\  
curl http://localhost:8081/create/schedules\\
curl http://localhost:8081/process/payments

>In ERP:\
SELECT utl_http.request('http://<DMZ_SERVER_IP>:8081/create/schedules') FROM dual;\
SELECT utl_http.request('http://<DMZ_SERVER_IP>:8081/process/payments') FROM dual;
