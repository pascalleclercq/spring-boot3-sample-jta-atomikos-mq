# spring-boot3-sample-jta-atomikos-mq

## Prequisites

* Docker
* Java 17
* Maven 3+

## How To Run

*  in a console, execute ```docker compose up```
* Run the SampleAtomikosApplication (a Spring Boot app).
* In the log, you should be able to read only one ```----> josh``` and ```Simulated error``` and ```Count is 1``` twice, because one of the 2 messages commit and the other one rollback 

