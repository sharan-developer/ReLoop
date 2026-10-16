package com.reloop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Maps the "reloop.*" block of application.yml (fed by environment variables). */
@Getter
@Setter
@ConfigurationProperties(prefix = "reloop")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Cloudinary cloudinary = new Cloudinary();
    private Razorpay razorpay = new Razorpay();
    private boolean cookieSecure;
    private String corsOrigins = "http://localhost:5173";
    private boolean seedDemoData;
    private String uploadDir = "./uploads";
    private String publicBaseUrl = "http://localhost:5173";

    @Getter @Setter public static class Jwt { private String secret; private long accessMinutes = 15; private long refreshDays = 7; }
    @Getter @Setter public static class Admin { private String email; private String password; }
    @Getter @Setter public static class Cloudinary {
        private String cloudName; private String apiKey; private String apiSecret;
        public boolean enabled() { return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret); }
    }
    @Getter @Setter public static class Razorpay {
        private String keyId; private String keySecret;
        public boolean enabled() { return notBlank(keyId) && notBlank(keySecret); }
    }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
