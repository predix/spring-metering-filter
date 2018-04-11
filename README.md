# spring-metering-filter

A spring filter that provides RESTful API metering with Nurego.

## LICENSE
This project is licensed under Apache v2.

## Build

```unix
mvn clean package
```
## Run Integration Tests

* To execute the integration tests, you will need to source the env variables from the spring-filters-config repo, under the fixMeteringIntegTests branch. In the commands below, I have the spring-filters-config file in another folder.

* If you are behind a network proxy, run 
```unix
mvn clean verify -s ../spring-filters-config/mvn_settings.xml -Dhttp.proxyHost=sjc1intproxy02.crd.ge.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=sjc1intproxy02.crd.ge.com -Dhttps.proxyPort=8080
```
* If you are on Internet, run 
```unix 
mvn clean verify -s ../spring-filters-config/mvn_settings.xml 
```
