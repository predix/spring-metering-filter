# spring-metering-filter

A spring filter that provides RESTful API metering with Nurego.

## LICENSE
This project is licensed under Apache v2.

## Build

```unix
mvn clean package
```
## Run Integration Tests

* To execute the integration tests, you need to export valid values for NUREGO_API_TEST_KEY and NUREGO_API_TEST_URL.
* If you are behind a network proxy, you will also need to configure:
	*  HTTPS_PROXY_HOST
	*  HTTPS_PROXY_PORT
	*  NON_PROXY_HOSTS
```unix
mvn clean verify
```

