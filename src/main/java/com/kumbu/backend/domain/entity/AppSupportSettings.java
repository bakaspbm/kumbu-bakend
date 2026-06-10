package com.kumbu.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "app_support_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSupportSettings {

    @Id
    @Builder.Default
    private String id = "default";

    @Column(name = "welcome_message", nullable = false)
    @Builder.Default
    private String welcomeMessage = "Olá! Como podemos ajudar?";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quick_actions", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> quickActions = new ArrayList<>();

    @Column(name = "auto_reply_message", nullable = false)
    @Builder.Default
    private String autoReplyMessage = "";
}
