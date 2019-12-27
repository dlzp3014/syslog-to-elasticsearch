package am.ik.lab;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
public class SyslogHandler
    implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

    private final ElasticsearchSink sink;

    private final Counter incomingPayloads;

    private final Counter incomingLogs;

    private static final Logger err = LoggerFactory.getLogger(SyslogHandler.class);

    private static final String LF = "\n";

    private static final String SPLIT_PATTERN = "(?<=" + LF + ")";

    private final LogParser logParser = new LogParser();

    public SyslogHandler(ElasticsearchSink sink, MeterRegistry meterRegistry) {
        this.sink = sink;
        this.incomingPayloads = meterRegistry.counter("payloads.incoming");
        this.incomingLogs = meterRegistry.counter("logs.incoming");
    }

    @Override
    public Publisher<Void> apply(NettyInbound in, NettyOutbound out) {
        Flux<String> incoming = in.receive().asString();
        incoming
            .doOnNext(__ -> this.incomingPayloads.increment())
            .transform(this::parse) //
            .doOnNext(__ -> this.incomingLogs.increment())
            .flatMap(this.sink::handleMessage) //
            .subscribe();
        return Flux.never();
    }

    Flux<Map<String, Object>> parse(Flux<String> incoming) {
        return incoming
            .flatMapIterable(s -> Arrays.asList(s.split(SPLIT_PATTERN))) //
            .windowUntil(s -> s.endsWith(LF)) //
            .flatMap(f -> f.collect(Collectors.joining())) //
            .map(String::trim) //
            .filter(s -> !s.isEmpty()) //
            .onBackpressureDrop(this::onDropped) //
            .map(logParser::parse);
    }


    void onDropped(String s) {
        err.warn("Dropped! {}", s);
    }

    void handleMessage(Map<String, Object> payload) {
        System.out.println(payload);
    }
}
