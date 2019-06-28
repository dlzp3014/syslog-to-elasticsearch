package am.ik.lab;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogParserTest {

    static final String ORATOS_SPRING_BOOT_LOG = "854 <14>1 2019-06-13T04:28:00.44929+00:00 ip-10-0-8-8.ap-northeast-1.compute.internal pod" +
        ".log/gateway/spring-cloud-gateway-6d4b5f5cc7- - - " +
        "[kubernetes@47450 " +
        "pod-template-hash=\"6d4b5f5cc7\" app=\"spring-cloud-gateway\" namespace_name=\"gateway\" object_name=\"spring-cloud-gateway-6d4b5f5cc7-2nsbf\" container_name=\"spring-cloud-gateway\" " +
        "vm_id=\"ip-10-0-8-8.ap-northeast-1.compute.internal\"] 2019-06-13 04:28:00.449  INFO [gateway,daa2b2740125654f,daa2b2740125654f,true] 1 --- [or-http-epoll-4] RTR                       " +
        "               : date:2019-06-13T04:28:00.449082Zu0009method:GETu0009path:/favicon-32x32.pngu0009status:200u0009host:blog.ik.amu0009address:126.112.54" +
        ".50u0009elapsed:5msu0009crawler:falseu0009user-agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537" +
        ".36u0009referer:nullu0009";

    static final String CF_RTR_LOG = "762 <14>1 2019-06-15T08:56:22.500567+00:00 APJ.development.demo-micrometer bcf22a82-8804-4a4e-b790-671e5d697350 [RTR/0] - - demo-micrometer.cfapps.io - " +
        "[2019-06-15T08:56:22.295+0000] \"GET /favicon.ico HTTP/1.1\" 200 0 946 \"https://demo-micrometer.cfapps.io/hello\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36\" \"10.10.66.62:4188\" \"10.10.149.26:61064\" x_forwarded_for:\"221.243.200.129, 10.10.66.62\" x_forwarded_proto:\"https\" " +
        "vcap_request_id:\"273e9644-c498-4c41-5185-b8f2abf51450\" response_time:0.204763851 app_id:\"bcf22a82-8804-4a4e-b790-671e5d697350\" app_index:\"2\" x_b3_traceid:\"92400b454fd0c48a\" " +
        "x_b3_spanid:\"92400b454fd0c48a\" x_b3_parentspanid:\"-\" b3:\"92400b454fd0c48a-92400b454fd0c48a-d\"";

    static final String CF_SPRING_BOOT_LOG = "260 <14>1 2019-06-15T08:56:22.346718+00:00 APJ.development.demo-micrometer bcf22a82-8804-4a4e-b790-671e5d697350 [APP/PROC/WEB/2] - - 2019-06-15 " +
        "08:56:22.346  " +
        "INFO 6 --- [nio-8080-exec-8] o.s.web.servlet.DispatcherServlet        : Completed initialization in 14 ms";

    private LogParser logParser = new LogParser();

    @Test
    void kv() {
        String s = "[kubernetes@47450 pod-template-hash=\"6d4b5f5cc7\" app=\"spring-cloud-gateway\" namespace_name=\"gateway\" object_name=\"spring-cloud-gateway-6d4b5f5cc7-2nsbf\" " +
            "container_name=\"spring-cloud-gateway\" vm_id=\"ip-10-0-8-8.ap-northeast-1.compute.internal\"]";

        final Map<String, String> kv = logParser.kv(s);
        assertThat(kv.get("pod-template-hash")).isEqualTo("6d4b5f5cc7");
        assertThat(kv.get("app")).isEqualTo("spring-cloud-gateway");
        assertThat(kv.get("namespace_name")).isEqualTo("gateway");
        assertThat(kv.get("object_name")).isEqualTo("spring-cloud-gateway-6d4b5f5cc7-2nsbf");
        assertThat(kv.get("container_name")).isEqualTo("spring-cloud-gateway");
        assertThat(kv.get("vm_id")).isEqualTo("ip-10-0-8-8.ap-northeast-1.compute.internal");
    }

    @Test
    void parseCfRouter() {
        final Map<String, Object> parsed = logParser.parse(CF_RTR_LOG);
        System.out.println(parsed);
        assertThat(parsed.get("syslog_procid")).isEqualTo("[RTR/0]");
    }

    @Test
    void parseCfApp() {
    }

    @Test
    void parseCfSpringBoot() {
        final Map<String, Object> parsed = logParser.parse(CF_SPRING_BOOT_LOG);
        System.out.println(parsed);
        assertThat(parsed.get("syslog_procid")).isEqualTo("[APP/PROC/WEB/2]");
        assertThat(parsed.get("level")).isEqualTo("INFO");
        assertThat(parsed.get("thread")).isEqualTo("nio-8080-exec-8");
        assertThat(parsed.get("message")).isEqualTo("Completed initialization in 14 ms");
    }


    @Test
    void parseOratosApp() {
    }

    @Test
    void parseOratosSpringBoot() {
        final Map<String, Object> parsed = logParser.parse(ORATOS_SPRING_BOOT_LOG);
        System.out.println(parsed);
        assertThat(parsed.get("level")).isEqualTo("INFO");
        assertThat(parsed.get("thread")).isEqualTo("or-http-epoll-4");
        assertThat(parsed.get("application")).isEqualTo("gateway");
        assertThat(parsed.get("trace_id")).isEqualTo("daa2b2740125654f");
        assertThat(parsed.get("message")).isEqualTo("date:2019-06-13T04:28:00.449082Zu0009method:GETu0009path:/favicon-32x32.pngu0009status:200u0009host:blog.ik.amu0009address:126.112.54" +
            ".50u0009elapsed:5msu0009crawler:falseu0009user-agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537" +
            ".36u0009referer:nullu0009");
    }
}