package am.ik.lab;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ElasticsearchSink {

    private final WebClient webClient;

    private final IdGenerator idGenerator;

    private final Logger log = LoggerFactory.getLogger(ElasticsearchSink.class);

    public ElasticsearchSink(WebClient.Builder builder, Elasticsearch elasticsearch,
                             IdGenerator idGenerator) {
        this.webClient = builder
            .baseUrl(elasticsearch.getUrl())
            .defaultHeaders(headers -> headers.setBasicAuth(elasticsearch.getUsername(), elasticsearch.getPassword()))
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                .tcpConfiguration(tcpClient -> tcpClient
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .doOnConnected(connection -> connection
                        .addHandler(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                        .addHandler(
                            new WriteTimeoutHandler(3, TimeUnit.SECONDS))))
                .wiretap(true)))
            .build();
        this.idGenerator = idGenerator;
    }

    public Mono<Void> handleMesssage(Map<String, Object> payload) {
        final OffsetDateTime dateTime;
        try {
            dateTime = OffsetDateTime.parse((String) payload.get("syslog_timestamp"));
        } catch (DateTimeParseException e) {
            return Mono.error(e);
        }
        final UUID id = this.idGenerator.generateId();
        String index = "drain-" + dateTime.toLocalDate();
        // https://stackoverflow.com/a/51321602/5861829
        return this.webClient.put()
            .uri(index + "/doc/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(payload)
            .retrieve()
            .bodyToFlux(DataBuffer.class)
            .retryBackoff(10, Duration.ofSeconds(1), Duration.ofMinutes(5))
            .doOnError(e -> log.error(e.getMessage(), e))
            .map(DataBufferUtils::release)
            .then();
    }
}
