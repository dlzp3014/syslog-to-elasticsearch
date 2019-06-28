package am.ik.lab;

import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    private final Grok syslog5424;

    private final Grok springBoot;

    private final Grok goRouter;

    private final Pattern kvPattern = Pattern.compile("([a-zA-Z0-9\\-_]+)=\"([^\"]+)\"");

    private final Pattern cfHostname = Pattern.compile("([a-zA-Z0-9\\-_]+)\\.([a-zA-Z0-9\\-_]+)\\.([a-zA-Z0-9\\-_]+)");

    public LogParser() {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();
        try (InputStream inputStream = new ClassPathResource("patterns/linux-syslog").getInputStream()) {
            grokCompiler.register(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.syslog5424 = grokCompiler.compile("(?:%{INT:syslog6587_msglen} )?<%{POSINT:syslog_pri}>(%{SPACE})?(?:%{NONNEGINT:syslog5424_ver} )?" +
            "(?:%{SYSLOGTIMESTAMP:syslog_timestamp}|%{TIMESTAMP_ISO8601:syslog_timestamp}) %{SYSLOGHOST:syslog_hostname} %{DATA:syslog_program}(?:\\[%{POSINT:syslog_pid}\\])?(:)? (?:%{DATA" +
            ":syslog_procid}|\\-) (?:%{DATA:syslog_msgid}|\\-)(?: %{SYSLOG5424SD:syslog_sd}| \\-)? %{GREEDYDATA:syslog_message}");
        this.springBoot = grokCompiler.compile("(?<timestamp>%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME})\\s+%{LOGLEVEL:level} (\\[%{DATA:application},%{DATA:trace_id},%{DATA:span_id}," +
            "%{DATA:exported}\\])" +
            "?\\s*%{NUMBER:pid} --- \\[\\s*%{DATA:thread}\\] %{DATA:logger}\\s+:\\s+%{GREEDYDATA:message}");
        this.goRouter = grokCompiler.compile("%{URIHOST:request_host} %{NOTSPACE} \\[%{TIMESTAMP_ISO8601:access_timestamp}\\] \\\"%{WORD:request_method} %{URIPATHPARAM:request_url} " +
            "%{SYSLOGPROG:request_protocol}\\\" %{NUMBER:status_code:int} %{NUMBER:bytes_received:int} %{NUMBER:bytes_sent:int} \\\"%{NOTSPACE:referer}\\\" \\\"%{DATA:user_agent}\\\" " +
            "\\\"%{URIHOST:remote_address}\\\" \\\"%{URIHOST:backend_address}\\\" x_forwarded_for:\\\"%{DATA:x_forwarded_for}\\\" x_forwarded_proto:\\\"%{WORD:x_forwarded_proto}\\\" " +
            "vcap_request_id:\\\"%{DATA:x_vcap_request_id}\\\" response_time:%{NUMBER:response_time:float} app_id:\\\"%{DATA:app_td}\\\" app_index:\\\"%{DATA:app_index}\\\" " +
            "x_b3_traceid:\\\"%{DATA:trace_id}\\\" x_b3_spanid:\\\"%{DATA:span_id}\\\" x_b3_parentspanid:\\\"%{DATA:parent_span_id}\\\"( b3:\\\"%{DATA:b3}\\\")?");
    }

    public Map<String, Object> parse(String log) {
        final Map<String, Object> syslog = this.parseSyslog5424(log);
        String message = (String) syslog.get("syslog_message");
        if (!StringUtils.isEmpty(message)) {
            final Map<String, Object> springBoot = this.parseSpringBoot(message);
            if (!springBoot.isEmpty()) {
                syslog.putAll(springBoot);
                syslog.put("is_spring_boot", true);
            }

            final String procId = (String) syslog.get("syslog_procid");
            if (procId != null && procId.contains("RTR")) {
                final Map<String, Object> goRouter = this.parseGoRouter(message);
                if (!goRouter.isEmpty()) {
                    syslog.putAll(goRouter);
                    syslog.put("is_go_router", true);
                }
            }
        }
        final String hostname = (String) syslog.get("syslog_hostname");
        if (!StringUtils.isEmpty(hostname)) {
            final Matcher matcher = this.cfHostname.matcher(hostname);
            if (matcher.matches()) {
                syslog.put("cf_org", matcher.group(1));
                syslog.put("cf_space", matcher.group(2));
                syslog.put("cf_app", matcher.group(3));
            }
        }

        syslog.remove("DATA");
        syslog.remove("IPORHOST");
        syslog.remove("SPACE");
        syslog.remove("YEAR");
        syslog.remove("MONTH");
        syslog.remove("MONTHDAY");
        syslog.remove("MONTHNUM");
        syslog.remove("TIME");
        syslog.remove("DAY");
        syslog.remove("HOUR");
        syslog.remove("MINUTE");
        syslog.remove("SECOND");
        syslog.remove("ISO8601_TIMEZONE");
        syslog.remove("NOTSPACE");
        syslog.remove("URIPARAM");
        syslog.remove("URIPATH");
        syslog.remove("program");
        syslog.remove("port");
        return syslog;
    }

    Map<String, Object> parseSyslog5424(String log) {
        final LinkedHashMap<String, Object> syslog5424 = new LinkedHashMap<>(this.syslog5424.capture(log));
        final String syslogSd = (String) syslog5424.get("syslog_sd");
        if (!StringUtils.isEmpty(syslogSd)) {
            syslog5424.putAll(this.kv(syslogSd));
            syslog5424.remove("syslog_sd");
        }
        final Object syslogTimestamp = syslog5424.get("syslog_timestamp");
        if (syslogTimestamp instanceof List) {
            for (Object o : ((List) syslogTimestamp)) {
                if (o != null) {
                    syslog5424.put("syslog_timestamp", o);
                    break;
                }
            }
        }
        return syslog5424;
    }

    Map<String, Object> parseSpringBoot(String log) {
        return this.springBoot.capture(log);
    }

    Map<String, Object> parseGoRouter(String log) {
        return this.goRouter.capture(log);
    }

    Map<String, String> kv(String s) {
        final Matcher matcher = kvPattern.matcher(s);
        Map<String, String> kv = new LinkedHashMap<>();
        while (matcher.find()) {
            kv.put(matcher.group(1), matcher.group(2));
        }
        return kv;
    }
}
