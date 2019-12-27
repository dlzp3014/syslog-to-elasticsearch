package am.ik.lab;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SyslogHandlerTest {

    @Test
    void parseSimple() {
        final SyslogHandler syslogHandler = new SyslogHandler(null, mock(MeterRegistry.class));
        final Flux<Map<String, Object>> parsed = syslogHandler.parse(Flux.just( //
            "260 <14>1 2019-06-15T08:56:22.346718+00:00 xyz aaa [APP/PROC/WEB/2] - - 2019-06-15 08:56:22.346   6 --- [nio-8080-exec-8] Foo        : Hello1\r\n\r",
            "260 <14>1 2019-06-15T08:56:23.346718+00:00 xyz aaa [APP/PROC/WEB/2] - - 2019-06-15 08:56:23.346   6 --- [nio-8080-exec-8] Foo        : Hello2\r\n\r"));
        StepVerifier.create(parsed)
            .assertNext(m -> assertThat(m.get("syslog_message")).isEqualTo("2019-06-15 08:56:22.346   6 --- [nio-8080-exec-8] Foo        : Hello1"))
            .assertNext(m -> assertThat(m.get("syslog_message")).isEqualTo("2019-06-15 08:56:23.346   6 --- [nio-8080-exec-8] Foo        : Hello2"))
            .verifyComplete();
    }

    @Test
    void parseSplited() {
        final SyslogHandler syslogHandler = new SyslogHandler(null, mock(MeterRegistry.class));
        final Flux<Map<String, Object>> parsed = syslogHandler.parse(Flux.just( //
            "260 <14>1 2019-06-15T08:56:22.346718+00:00 xyz aaa [APP/PROC/WEB/2] - - 2019-06-15 08:56:22.346   ",
            "6 --- [nio-8080-exec-8] Foo        : Hello1\r\n\r260 <14>1 2019-06-15T08:56:23.346718+00:00 xyz aaa [APP/PROC/WEB/2]",
            " - - 2019-06-15 08:56:23.346   6 --- [nio-8080-exec-8] Foo        : Hello2\r\n\r"));
        StepVerifier.create(parsed)
            .assertNext(m -> assertThat(m.get("syslog_message")).isEqualTo("2019-06-15 08:56:22.346   6 --- [nio-8080-exec-8] Foo        : Hello1"))
            .assertNext(m -> assertThat(m.get("syslog_message")).isEqualTo("2019-06-15 08:56:23.346   6 --- [nio-8080-exec-8] Foo        : Hello2"))
            .verifyComplete();
    }
}