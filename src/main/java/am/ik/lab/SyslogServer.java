package am.ik.lab;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

@Component
public class SyslogServer implements DisposableBean {

    private final DisposableServer server;

    public SyslogServer(SyslogHandler syslogHandler) {
        this.server = TcpServer.create()
            .host("0.0.0.0")
            .port(1514)
            .wiretap(true)
            .handle(syslogHandler)
            .bindNow();
    }

    @Override
    public void destroy() {
        this.server.onDispose().subscribe();
    }
}
