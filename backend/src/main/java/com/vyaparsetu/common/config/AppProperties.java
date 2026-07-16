package com.vyaparsetu.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Security security = new Security();
    private Features features = new Features();
    private Storage storage = new Storage();
    private Ai ai = new Ai();

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }

    /** LLM provider config. A provider chain falls back across these in order. */
    public static class Ai {
        private Groq groq = new Groq();
        private Gemini gemini = new Gemini();
        public Groq getGroq() { return groq; }
        public void setGroq(Groq groq) { this.groq = groq; }
        public Gemini getGemini() { return gemini; }
        public void setGemini(Gemini gemini) { this.gemini = gemini; }

        public static class Groq {
            private String baseUrl = "https://api.groq.com/openai/v1";
            private String apiKey = "";
            private String model = "llama-3.3-70b-versatile";
            private String fallbackModel = "llama-3.1-8b-instant";
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            public String getFallbackModel() { return fallbackModel; }
            public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
        }

        public static class Gemini {
            private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
            private String apiKey = "";
            private String model = "gemini-2.0-flash";
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
        }
    }

    public static class Security {
        private Jwt jwt = new Jwt();
        private Otp otp = new Otp();
        private Totp totp = new Totp();
        private WebAuthn webauthn = new WebAuthn();
        private Cors cors = new Cors();
        public Jwt getJwt() { return jwt; }
        public void setJwt(Jwt jwt) { this.jwt = jwt; }
        public Otp getOtp() { return otp; }
        public void setOtp(Otp otp) { this.otp = otp; }
        public Totp getTotp() { return totp; }
        public void setTotp(Totp totp) { this.totp = totp; }
        public WebAuthn getWebauthn() { return webauthn; }
        public void setWebauthn(WebAuthn webauthn) { this.webauthn = webauthn; }
        public Cors getCors() { return cors; }
        public void setCors(Cors cors) { this.cors = cors; }
    }

    public static class Cors {
        private java.util.List<String> allowedOrigins =
                new java.util.ArrayList<>(java.util.List.of("http://localhost:5173"));
        public java.util.List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(java.util.List<String> v) { this.allowedOrigins = v; }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenTtlMinutes = 15;
        private long refreshTokenTtlDays = 30;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTokenTtlMinutes() { return accessTokenTtlMinutes; }
        public void setAccessTokenTtlMinutes(long v) { this.accessTokenTtlMinutes = v; }
        public long getRefreshTokenTtlDays() { return refreshTokenTtlDays; }
        public void setRefreshTokenTtlDays(long v) { this.refreshTokenTtlDays = v; }
    }

    public static class Otp {
        private int length = 6;
        private long ttlMinutes = 5;
        private int maxAttempts = 5;
        private long resendCooldownSeconds = 30;
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public long getTtlMinutes() { return ttlMinutes; }
        public void setTtlMinutes(long ttlMinutes) { this.ttlMinutes = ttlMinutes; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getResendCooldownSeconds() { return resendCooldownSeconds; }
        public void setResendCooldownSeconds(long v) { this.resendCooldownSeconds = v; }
    }

    public static class Totp {
        private String issuer = "VyaparMantra";
        private String encryptionKey = "";
        private long challengeTtlMinutes = 5;
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getEncryptionKey() { return encryptionKey; }
        public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
        public long getChallengeTtlMinutes() { return challengeTtlMinutes; }
        public void setChallengeTtlMinutes(long value) { this.challengeTtlMinutes = value; }
    }

    public static class WebAuthn {
        private String rpId = "vyparsetu.vercel.app";
        private String rpName = "VyaparMantra";
        private java.util.List<String> origins = new java.util.ArrayList<>(java.util.List.of("http://localhost:5173"));
        private long ceremonyTtlMinutes = 5;
        public String getRpId() { return rpId; }
        public void setRpId(String rpId) { this.rpId = rpId; }
        public String getRpName() { return rpName; }
        public void setRpName(String rpName) { this.rpName = rpName; }
        public java.util.List<String> getOrigins() { return origins; }
        public void setOrigins(java.util.List<String> origins) { this.origins = origins; }
        public long getCeremonyTtlMinutes() { return ceremonyTtlMinutes; }
        public void setCeremonyTtlMinutes(long value) { this.ceremonyTtlMinutes = value; }
    }

    public static class Features {
        private Toggle procurement = new Toggle();
        private Toggle demoLogin = new Toggle();
        private WhatsappFeature whatsapp = new WhatsappFeature();
        public Toggle getProcurement() { return procurement; }
        public void setProcurement(Toggle procurement) { this.procurement = procurement; }
        public Toggle getDemoLogin() { return demoLogin; }
        public void setDemoLogin(Toggle demoLogin) { this.demoLogin = demoLogin; }
        public WhatsappFeature getWhatsapp() { return whatsapp; }
        public void setWhatsapp(WhatsappFeature whatsapp) { this.whatsapp = whatsapp; }
    }

    public static class Toggle {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * WhatsApp feature config. {@code provider=meta} activates the real Meta
     * Cloud API client (needs token/phoneNumberId); otherwise messages are logged.
     */
    public static class WhatsappFeature {
        private boolean enabled = false;
        private String provider = "noop";          // noop | meta
        private String apiUrl = "https://graph.facebook.com/v21.0";
        private String token;                       // Meta permanent/system-user token
        private String phoneNumberId;               // Meta phone number id (sender)
        private String verifyToken;                 // webhook verification token
        private int draftTtlHours = 24;             // abandoned draft order expiry
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
        public int getDraftTtlHours() { return draftTtlHours; }
        public void setDraftTtlHours(int draftTtlHours) { this.draftTtlHours = draftTtlHours; }
    }

    public static class Storage {
        private String type = "local";
        private String localPath = "./storage";
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLocalPath() { return localPath; }
        public void setLocalPath(String localPath) { this.localPath = localPath; }
    }
}
