package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
public class SmsService {

    private final KumbuProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public SmsService(KumbuProperties properties) {
        this.properties = properties;
    }

    public void sendOtp(String phone, String otp) {
        String provider = normalizeProvider(properties.getSms().getProvider());
        String message = "Kumbú: o seu código de verificação é " + otp + ". Expira em 5 minutos.";

        switch (provider) {
            case "twilio" -> sendTwilio(phone, message);
            case "africastalking" -> sendAfricasTalking(phone, message);
            case "log" -> log.info("[SMS] OTP → {} | code: {}", phone, otp);
            default -> throw ApiException.badRequest("Fornecedor SMS não suportado: " + provider);
        }
    }

    public boolean isConfigured() {
        String provider = normalizeProvider(properties.getSms().getProvider());
        return switch (provider) {
            case "log" -> true;
            case "twilio" -> hasText(properties.getSms().getTwilioAccountSid())
                    && hasText(properties.getSms().getTwilioAuthToken())
                    && hasText(properties.getSms().getTwilioFromNumber());
            case "africastalking" -> hasText(properties.getSms().getAfricasTalkingUsername())
                    && hasText(properties.getSms().getAfricasTalkingApiKey());
            default -> false;
        };
    }

    private void sendTwilio(String phone, String message) {
        KumbuProperties.Sms sms = properties.getSms();
        if (!isConfigured()) {
            throw ApiException.badRequest("SMS Twilio não configurado no servidor.");
        }

        String body = formEncode(
                "To", phone,
                "From", sms.getTwilioFromNumber(),
                "Body", message
        );
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + sms.getTwilioAccountSid() + "/Messages.json";
        String basic = Base64.getEncoder().encodeToString(
                (sms.getTwilioAccountSid() + ":" + sms.getTwilioAuthToken()).getBytes(StandardCharsets.UTF_8));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[SMS] Twilio falhou {} → {}", response.statusCode(), response.body());
                throw ApiException.badRequest("Não foi possível enviar o SMS. Tente novamente.");
            }
            log.info("[SMS] Twilio enviado → {}", phone);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[SMS] Twilio erro: {}", ex.getMessage());
            throw ApiException.badRequest("Falha ao enviar SMS.");
        }
    }

    private void sendAfricasTalking(String phone, String message) {
        KumbuProperties.Sms sms = properties.getSms();
        if (!isConfigured()) {
            throw ApiException.badRequest("SMS Africa's Talking não configurado no servidor.");
        }

        String from = hasText(sms.getAfricasTalkingFrom()) ? sms.getAfricasTalkingFrom() : "KUMBU";
        String body = formEncode(
                "username", sms.getAfricasTalkingUsername(),
                "to", phone,
                "message", message,
                "from", from
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.africastalking.com/version1/messaging"))
                    .header("apiKey", sms.getAfricasTalkingApiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[SMS] Africa's Talking falhou {} → {}", response.statusCode(), response.body());
                throw ApiException.badRequest("Não foi possível enviar o SMS. Tente novamente.");
            }
            log.info("[SMS] Africa's Talking enviado → {}", phone);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[SMS] Africa's Talking erro: {}", ex.getMessage());
            throw ApiException.badRequest("Falha ao enviar SMS.");
        }
    }

    private static String normalizeProvider(String raw) {
        if (raw == null || raw.isBlank()) return "log";
        return raw.trim().toLowerCase();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String formEncode(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) sb.append('&');
            sb.append(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
