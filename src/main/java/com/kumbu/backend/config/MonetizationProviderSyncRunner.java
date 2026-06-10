package com.kumbu.backend.config;

import com.kumbu.backend.domain.entity.MonetizationPaymentProvider;
import com.kumbu.backend.repository.MonetizationPaymentProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonetizationProviderSyncRunner implements ApplicationRunner {

    private final MonetizationPaymentProviderRepository providerRepository;
    private final KumbuProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        KumbuProperties.Monetization config = properties.getMonetization();
        if (config == null) return;

        syncProvider("prov_multicaixa_express", p -> {
            p.setAccountHolder(config.getCompanyName());
            if (!config.getMulticaixaPhone().isBlank()) {
                p.setPhoneNumber(config.getMulticaixaPhone());
            }
        });
        syncProvider("prov_bai_transfer", p -> {
            p.setAccountHolder(config.getCompanyName());
            if (!config.getBaiAccount().isBlank()) p.setAccountNumber(config.getBaiAccount());
            if (!config.getBaiIban().isBlank()) p.setIban(config.getBaiIban());
        });
        syncProvider("prov_bfa_transfer", p -> {
            p.setAccountHolder(config.getCompanyName());
            if (!config.getBfaAccount().isBlank()) p.setAccountNumber(config.getBfaAccount());
            if (!config.getBfaIban().isBlank()) p.setIban(config.getBfaIban());
        });
        syncProvider("prov_bic_transfer", p -> {
            p.setAccountHolder(config.getCompanyName());
            if (!config.getBicAccount().isBlank()) p.setAccountNumber(config.getBicAccount());
            if (!config.getBicIban().isBlank()) p.setIban(config.getBicIban());
        });

        log.info("Monetization payment providers synced from config");
    }

    private void syncProvider(String id, java.util.function.Consumer<MonetizationPaymentProvider> updater) {
        providerRepository.findById(id).ifPresent(p -> {
            updater.accept(p);
            providerRepository.save(p);
        });
    }
}
