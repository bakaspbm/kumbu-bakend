package com.kumbu.backend.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ChatSafetyService {

    /** Angola (+244), 9 dígitos móveis, formatos comuns com espaços ou hífens. */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?i)(?:\\+244|00244|244)?\\s*9[1-9]\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{3}|\\b9[1-9]\\d{7}\\b");

    public static final String SAFETY_WELCOME = """
            Olá! Antes de combinar o encontro, lembre-se:
            • Prefira shopping, mercado ou outro sítio público e movimentado.
            • Desconfie de pagamento adiantado a quem não conhece.
            • Mantenha a conversa aqui no chat — é mais seguro que partilhar o telefone logo de início.
            • Se algo não bater certo, use «Denunciar».""";

    public static final String PHONE_WARNING = """
            ⚠️ Parece que partilhou um número de telefone. Para evitar burlas, continue a negociar \
            aqui no chat da Kumbú. Se forem encontrar-se, escolha um local público. \
            Não pague antes de ver o produto, salvo se já confiar no vendedor.""";

    public boolean containsPhoneNumber(String text) {
        if (text == null || text.isBlank()) return false;
        return PHONE_PATTERN.matcher(text).find();
    }
}
