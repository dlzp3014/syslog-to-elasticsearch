package am.ik.lab;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.util.Map;
import java.util.function.BiFunction;

@Component
public class SyslogHandler
    implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

    private final ElasticsearchSink sink;

    private static final Logger err = LoggerFactory.getLogger(SyslogHandler.class);

    private final LogParser logParser = new LogParser();

    public SyslogHandler(ElasticsearchSink sink) {
        this.sink = sink;
    }

    @Override
    public Publisher<Void> apply(NettyInbound in, NettyOutbound out) {
        Flux<String> incoming = in.receive().asString();
        incoming.compose(this::parse) //
            .flatMap(this.sink::handleMesssage) //
            .subscribe();
        return Flux.never();
    }

    Flux<Map<String, Object>> parse(Flux<String> incoming) {
        return incoming
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
