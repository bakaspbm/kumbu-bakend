package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class NotificationMailService {

    private final JavaMailSender mailSender;
    private final SmsService smsService;
    private final String fromAddress;
    private final String publicUrl;
    private final boolean mailEnabled;

    public NotificationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            KumbuProperties properties,
            SmsService smsService) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.smsService = smsService;
        this.fromAddress = properties.getMail().getFrom();
        this.publicUrl = trimTrailingSlash(properties.getApp().getPublicUrl());
        this.mailEnabled = properties.getMail().isEnabled() && mailSender != null;
    }

    public String sendPasswordReset(String email, String token) {
        String link = actionLink("/recuperar-palavra-passe", token);
        String subject = "Kumbú — Redefinir palavra-passe";
        String html = """
                <p>Olá,</p>
                <p>Recebemos um pedido para redefinir a palavra-passe da sua conta Kumbú.</p>
                <p><a href="%s">Clique aqui para criar uma nova palavra-passe</a></p>
                %s
                <p>O link expira em 1 hora. Se não fez este pedido, ignore este email.</p>
                <p>— Equipa Kumbú</p>
                """.formatted(link, localhostFallbackHtml("/recuperar-palavra-passe", token));
        return deliver(email, subject, html, link);
    }

    public String sendEmailVerification(String email, String token) {
        String link = actionLink("/confirmar-email", token);
        String subject = "Kumbú — Confirmar email";
        String html = """
                <p>Olá,</p>
                <p>Obrigado por se registar na Kumbú. Confirme o seu email para activar a conta:</p>
                <p><a href="%s">Confirmar email</a></p>
                %s
                <p>O link expira em 24 horas.</p>
                <p>— Equipa Kumbú</p>
                """.formatted(link, localhostFallbackHtml("/confirmar-email", token));
        return deliver(email, subject, html, link);
    }

    public void sendPhoneOtp(String phone, String otp) {
        smsService.sendOtp(phone, otp);
    }

    /** @return link de acção quando o email não foi enviado (modo dev / SMTP indisponível) */
    private String deliver(String to, String subject, String htmlBody, String actionLink) {
        if (mailEnabled) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("[EMAIL] Enviado '{}' → {}", subject, to);
                return null;
            } catch (Exception e) {
                log.warn("[EMAIL] Falha ao enviar para {}: {} — use o link nos logs ou emailActionLink", to, e.getMessage());
            }
        }
        log.warn("[EMAIL] SMTP inactivo — '{}' → {} | Link: {}", subject, to, actionLink);
        return actionLink;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    private String actionLink(String path, String token) {
        return publicUrl + path + "?token=" + encodeToken(token);
    }

    private String localhostFallbackHtml(String path, String token) {
        String localhostLink = localhostMirrorLink(path, token);
        if (localhostLink == null) {
            return "";
        }
        return """
                <p><small>No mesmo PC, se o link acima der erro, use \
                <a href="%s">abrir em localhost</a> ou copie o endereço para o browser.</small></p>
                """.formatted(localhostLink);
    }

    private String localhostMirrorLink(String path, String token) {
        if (!isPrivateNetworkUrl(publicUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(publicUrl);
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            return "http://localhost:" + port + path + "?token=" + encodeToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeToken(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static boolean isPrivateNetworkUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                return false;
            }
            InetAddress address = InetAddress.getByName(host);
            return address.isSiteLocalAddress() || address.isLoopbackAddress();
        } catch (Exception e) {
            return hostLooksPrivate(url);
        }
    }

    private static boolean hostLooksPrivate(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            return host.startsWith("192.168.")
                    || host.startsWith("10.")
                    || host.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..+");
        } catch (Exception e) {
            return false;
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) return "http://localhost:3000";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
