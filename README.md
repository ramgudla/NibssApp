# Nibbs

## How to Build? ##
      
>Clone the project.\
$ mvn clean install\
$ cd target\
$ zip -r NibbsApp.zip NibbsIntegration-0.0.1-SNAPSHOT.jar config

## How to Run? ##

>In DMZ server:\
    $ cd /home/nibbs\
    $ rm -rf *\
    $ unzip NibbsApp.zip (After unzipping, you will see a folder named 'config'. Inside this folder, there is a file named 'application.properties'. Update the db details in this file.)\
    Kill the running application java process:\
      $ ps -ef | grep NibbsIntegration-0.0.1-SNAPSHOT.jar\
      $ kill -9 `<pid>`\
    Run the Service:\
      $ java -Dserver.port=8081 -jar NibbsIntegration-0.0.1-SNAPSHOT.jar --logging.config=config/logback-spring.xml --spring.config.location=config/application.properties &

## How to Test? ##
    
>In UAT:\  
curl http://<DMZ_SERVER_IP>:8081/create/schedules

>In ERP:\
SELECT utl_http.request('http://<DMZ_SERVER_IP>:8081/create/schedules') FROM dual;
