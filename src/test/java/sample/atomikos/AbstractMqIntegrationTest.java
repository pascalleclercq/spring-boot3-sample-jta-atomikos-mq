package sample.atomikos;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@SpringBootTest
@Testcontainers
abstract class AbstractMqIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("perftest")
            .withUsername("postgres")
            .withPassword("postgres").withCommand("postgres -c max_prepared_transactions=100");

    @Container
    static final GenericContainer<?> mqContainer = new GenericContainer<>("icr.io/ibm-messaging/mq:9.4.3.1-r1")
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withEnv("MQ_APP_PASSWORD", "apppass123")
            .withEnv("MQ_ADMIN_PASSWORD", "adminpass123")
            .withEnv("MQ_ENABLE_EMBEDDED_WEB_SERVER", "true")
            .withExposedPorts(1414, 9443)
            .waitingFor(Wait.forLogMessage(".*AMQ5026I.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",   postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username",         postgresContainer::getUsername);
        registry.add("spring.datasource.password",     postgresContainer::getPassword);
        registry.add("ibm.mq.connName",
                () -> "localhost(" + mqContainer.getMappedPort(1414) + ")");
    }
}
