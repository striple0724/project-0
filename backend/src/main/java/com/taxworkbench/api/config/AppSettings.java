package com.taxworkbench.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppSettings {
    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final Runtime runtime = new Runtime();
    private final Limits limits = new Limits();
    private final Hosts hosts = new Hosts();

    public Security getSecurity() {
        return security;
    }

    public Cors getCors() {
        return cors;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Limits getLimits() {
        return limits;
    }

    public Hosts getHosts() {
        return hosts;
    }

    public static class Security {
        private boolean requireAuth = false;
        private String cryptoKeyBase64 = "";

        public boolean isRequireAuth() {
            return requireAuth;
        }

        public void setRequireAuth(boolean requireAuth) {
            this.requireAuth = requireAuth;
        }

        public String getCryptoKeyBase64() {
            return cryptoKeyBase64;
        }

        public void setCryptoKeyBase64(String cryptoKeyBase64) {
            this.cryptoKeyBase64 = cryptoKeyBase64;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedOriginPatterns = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }
    }

    public static class Runtime {
        private String publicBaseUrl = "http://localhost:8080";

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class Limits {
        private final Upload upload = new Upload();
        private final Download download = new Download();

        public Upload getUpload() {
            return upload;
        }

        public Download getDownload() {
            return download;
        }
    }

    public static class Upload {
        private String maxFileSize = "20MB";
        private String maxRequestSize = "30MB";

        public String getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public String getMaxRequestSize() {
            return maxRequestSize;
        }

        public void setMaxRequestSize(String maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
        }
    }

    public static class Download {
        private long maxBytes = 50 * 1024 * 1024;

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    public static class Hosts {
        private String local = "http://localhost:8080";
        private String dev = "https://dev-api.example.com";
        private String prod = "https://api.example.com";

        public String getLocal() {
            return local;
        }

        public void setLocal(String local) {
            this.local = local;
        }

        public String getDev() {
            return dev;
        }

        public void setDev(String dev) {
            this.dev = dev;
        }

        public String getProd() {
            return prod;
        }

        public void setProd(String prod) {
            this.prod = prod;
        }
    }
}
