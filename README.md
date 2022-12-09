# AccessBank

## Build: ##
      
>Clone the project.\
$ mvn clean install\
$ cd target\
$ zip -r AccessBank.zip AccessBankApp-0.0.1-SNAPSHOT.jar config

## Run: ##

**If the accessbank payment api endpoints certificate is from a wellknown CA, then set the property: 'custom.trust=false' in 'config/application.properties' file after unzipping the AccessBank.zip file into DMZ server. Otherwise (If its a self signed certificate), export the certificate from the accessbank api host and import into the custom truststore. Do the following:**
      
      ```
      echo quit\
             | openssl s_client -servername abp-accpaytest.accessbankplc.com -showcerts -connect abp-accpaytest.accessbankplc.com:1001\
             | openssl x509 -outform PEM\
             > abp-accpaytest.accessbankplc.com

      keytool -import -keystore config/ab-truststore.p12 -alias abp-accpaytest -file abp-accpaytest.accessbankplc.com -trustcacerts -storepass changeit
      ```
      
>In DMZ server:\
    $ cd /home/citibank/accessbank\
    $ rm -rf *\
    $ unzip AccessBank.zip\
    Kill the running application java process:\
      $ ps -ef | grep AccessBankApp-0.0.1-SNAPSHOT.jar\
      $ kill -9 `<pid>`\
    Run the Service:\
      $ java -jar AccessBankApp-0.0.1-SNAPSHOT.jar --logging.config=config/logback-spring.xml --spring.config.location=config/application.properties &

## Test: ##
    
>In UAT:\  
curl http://172.23.12.124:8080/initiate

>In ERP:\
SELECT utl_http.request('http://172.23.12.124:8080/initiate') FROM dual;
