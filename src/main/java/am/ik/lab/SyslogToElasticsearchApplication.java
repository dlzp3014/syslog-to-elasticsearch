package am.ik.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

@SpringBootApplication
public class SyslogToElasticsearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyslogToElasticsearchApplication.class, args);
    }

    @Bean
    public IdGenerator idGenerator() {
        return new AlternativeJdkIdGenerator();
    }
}
