# spring-metering-filter

A spring filter that provides RESTful API metering with Nurego.

## LICENSE
This project is licensed under Apache v2.

## Build

```bash
mvn clean package
```

## Run Integration Tests

* To run integration tests on Internet:
```bash 
source ../spring-filters-config/set-env-metering-filter.sh 

unset NON_PROXY_HOSTS
unset HTTPS_PROXY_PORT
unset HTTPS_PROXY_HOST 

mvn clean install -s ../spring-filters-config/mvn_settings_noproxy.xml
```

* To run integration tests behind a network proxy:
```bash 
source ../spring-filters-config/set-env-metering-filter.sh 

mvn clean install -s ../spring-filters-config/mvn_settings.xml
```