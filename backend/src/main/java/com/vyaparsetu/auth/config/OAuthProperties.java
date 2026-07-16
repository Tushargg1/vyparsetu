package com.vyaparsetu.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.oauth")
public class OAuthProperties {
    private String frontendUrl = "http://localhost:5173";
    private Provider google = new Provider();
    private Provider apple = new Provider();

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
    public Provider getGoogle() { return google; }
    public void setGoogle(Provider google) { this.google = google; }
    public Provider getApple() { return apple; }
    public void setApple(Provider apple) { this.apple = apple; }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String authorizationUri = "";
        private String tokenUri = "";
        private String issuerUri = "";

        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri)
                    && notBlank(authorizationUri) && notBlank(tokenUri) && notBlank(issuerUri);
        }
        private boolean notBlank(String value) { return value != null && !value.isBlank(); }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getAuthorizationUri() { return authorizationUri; }
        public void setAuthorizationUri(String authorizationUri) { this.authorizationUri = authorizationUri; }
        public String getTokenUri() { return tokenUri; }
        public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }
        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    }
}