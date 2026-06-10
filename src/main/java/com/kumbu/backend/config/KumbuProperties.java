package com.kumbu.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "kumbu")
public class KumbuProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private Admin admin = new Admin();
    private Monetization monetization = new Monetization();
    private App app = new App();
    private Mail mail = new Mail();
    private Sms sms = new Sms();
    private OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class App {
        private String publicUrl = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Mail {
        private String from = "noreply@kumbu.app";
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenMinutes = 60;
        private long refreshTokenDays = 30;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
        /** Padrões extra (ex.: rede local no telemóvel). Spring 5.3+ */
        private List<String> allowedOriginPatterns = List.of();
    }

    @Getter
    @Setter
    public static class Storage {
        private String basePath = "./uploads";
        private String publicBaseUrl = "http://localhost:8080/files";
        /** Documentos de identidade — nunca expostos em /files/** */
        private String privatePath = "./private-uploads";
    }

    @Getter
    @Setter
    public static class Monetization {
        private String companyName = "Kumbu Lda";
        private String multicaixaPhone = "+244 9XX XXX XXX";
        private String baiAccount = "";
        private String baiIban = "";
        private String bfaAccount = "";
        private String bfaIban = "";
        private String bicAccount = "";
        private String bicIban = "";
        private String emisEntity = "KUMBU";
    }

    @Getter
    @Setter
    public static class Admin {
        private String bootstrapEmail = "admin@kumbu.app";
        private String bootstrapPassword = "Admin123!";
    }

    @Getter
    @Setter
    public static class Sms {
        /** log | twilio | africastalking */
        private String provider = "log";
        private String twilioAccountSid;
        private String twilioAuthToken;
        private String twilioFromNumber;
        private String africasTalkingUsername;
        private String africasTalkingApiKey;
        private String africasTalkingFrom = "KUMBU";
    }

    @Getter
    @Setter
    public static class OAuth {
        /** Valida aud do id_token Google (opcional em dev). */
        private String googleClientId;
        /** Reservado para fluxo authorization-code server-side; nunca expor ao cliente. */
        private String googleClientSecret;
        private String facebookAppId;
        private String facebookAppSecret;
        /** Dev/rede restrita: aceita perfil validado no browser se Graph API falhar por timeout. */
        private boolean facebookTrustClientProfile = false;
    }
}
