# spring-metering-filter

A spring filter that provides RESTful API metering with Nurego.

## LICENSE
This project is licensed under Apache v2.

## Build

```unix
mvn clean package
```
## Run Integration Tests

* To execute the integration tests, you need to export valid values for the following:

  * NUREGO_API_TEST_URL
  * NUREGO_TEST_USERNAME
  * NUREGO_TEST_PASSWORD
  * NUREGO_TEST_INSTANCE_ID

	* NUREGO_API_TEST_URL
	* NUREGO_TEST_USERNAME
	* NUREGO_TEST_PASSWORD
	* NUREGO_TEST_INSTANCE_ID
Updated instructions on how to run integration tests.
* If you are behind a network proxy, you will also need to configure:
  * HTTPS_PROXY_HOST
  * HTTPS_PROXY_PORT
  * NON_PROXY_HOSTS

* To execute the integration tests, you will need to source the env variables from the spring-filters-config repo, under the fixMeteringIntegTests branch. In the commands below, I have the spring-filters-config file in another folder.

* If you are on Internet, run 
```unix 
mvn clean verify -s ../spring-filters-config/mvn_settings.xml 
```

* To execute the integration tests, you will need to source the env variables from the spring-filters-config repo, under the fixMeteringIntegTests branch. 

* If you are behind a network proxy, run 
```unix
mvn clean verify -s ../spring-filters-config/mvn_settings.xml -Dhttp.proxyHost=sjc1intproxy02.crd.ge.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=sjc1intproxy02.crd.ge.com -Dhttps.proxyPort=8080
```
* If you are on Internet, run 
```unix 
mvn clean verify -s ../spring-filters-config/mvn_settings.xml 
```


