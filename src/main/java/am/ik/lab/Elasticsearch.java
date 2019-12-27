package am.ik.lab;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "elasticsearch")
public class Elasticsearch {

    private final String url;

    private final String username;

    private final String password;

    @ConstructorBinding
    public Elasticsearch(@DefaultValue("http://localhost:9200") String url, @DefaultValue("") String username, @DefaultValue("") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
