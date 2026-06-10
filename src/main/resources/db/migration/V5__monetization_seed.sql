-- Fases, features, produtos e provedores de pagamento (Angola)

INSERT INTO monetization_phases (id, name, description, min_users, max_users, sort_order) VALUES
('PHASE_1', 'Arranque', '0 → ~500 utilizadores. Criar volume e actividade.', 0, 500, 1),
('PHASE_2', 'Tração', '~500 → 1500 utilizadores. Receita consistente.', 500, 1500, 2),
('PHASE_3', 'Escala', '1500 → 5000 utilizadores. Maximizar receita.', 1500, 5000, 3),
('PHASE_4', 'Avançado', '5000+ utilizadores. Monetização inteligente.', 5000, NULL, 4);

INSERT INTO monetization_features (id, phase_id, feature_type, name, description, is_active, requires_approval, sort_order) VALUES
-- Fase 1
('f1_highlight_top',     'PHASE_1', 'HIGHLIGHT_TOP',     'Subir ao topo',      'Anúncio sobe ao topo da listagem — 300 Kz', FALSE, TRUE, 1),
('f1_highlight_simple',  'PHASE_1', 'HIGHLIGHT_SIMPLE',  'Destaque simples',   'Badge de destaque na listagem — 700 Kz', FALSE, TRUE, 2),
('f1_highlight_urgent',  'PHASE_1', 'HIGHLIGHT_URGENT',  'Urgente',            'Etiqueta urgente + prioridade — 1.500 Kz', FALSE, TRUE, 3),
-- Fase 2
('f2_vip_plan',          'PHASE_2', 'VIP_PLAN',          'Plano VIP',          '1.500 Kz/mês — mais anúncios e melhor posição', FALSE, TRUE, 1),
('f2_seller_verify_basic','PHASE_2','SELLER_VERIFY_BASIC','Verificação básica', 'Selo verificado básico — 500 Kz', FALSE, TRUE, 2),
('f2_seller_verify_full','PHASE_2', 'SELLER_VERIFY_FULL','Verificação completa','KYC completo — 2.000 Kz', FALSE, TRUE, 3),
-- Fase 3
('f3_boost',             'PHASE_3', 'BOOST',             'Boost',              'Microtransacções para subir anúncio várias vezes', FALSE, TRUE, 1),
('f3_business_starter',  'PHASE_3', 'BUSINESS_STARTER',  'Business Starter',   '10.000 Kz/mês — carros, imóveis, lojas', FALSE, TRUE, 2),
('f3_business_pro',      'PHASE_3', 'BUSINESS_PRO',      'Business Pro',       '25.000 Kz/mês — plano premium para negócios', FALSE, TRUE, 3),
('f3_premium_highlight', 'PHASE_3', 'PREMIUM_HIGHLIGHT', 'Destaque premium',   'Destaques até 5.000–10.000 Kz', FALSE, TRUE, 4),
-- Fase 4
('f4_contact_fee',       'PHASE_4', 'CONTACT_FEE',       'Taxa de contacto',   'Ver número do vendedor — 100 Kz', FALSE, TRUE, 1),
('f4_paid_leads',        'PHASE_4', 'PAID_LEADS',        'Leads pagos',        'Marketplace de leads para serviços', FALSE, TRUE, 2),
('f4_advertising',       'PHASE_4', 'ADVERTISING',       'Publicidade',        'Banners e promoções pagas', FALSE, TRUE, 3),
('f4_job_posting',       'PHASE_4', 'JOB_POSTING',       'Publicação de emprego','Anúncio de emprego pago', FALSE, TRUE, 4);

INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, max_listings, category_hint, sort_order, metadata) VALUES
-- Fase 1 — Destaques
('prod_highlight_top_7d',    'HIGHLIGHT_TOP',     'Subir ao topo (7 dias)',    'Anúncio no topo da categoria', 300,  7, NULL, 'geral', 1, '{"boost_score": 10}'),
('prod_highlight_simple_7d', 'HIGHLIGHT_SIMPLE',  'Destaque simples (7 dias)', 'Badge destaque visível',       700,  7, NULL, 'moda,beleza,moveis', 2, '{"boost_score": 25}'),
('prod_highlight_urgent_7d', 'HIGHLIGHT_URGENT',  'Urgente (7 dias)',          'Etiqueta URGENTE + topo',      1500, 7, NULL, 'geral', 3, '{"boost_score": 50}'),
-- Fase 2 — VIP + Verificação
('prod_vip_monthly',         'VIP_PLAN',          'VIP Mensal',                'Mais anúncios + posição melhor', 1500, 30, 15, 'moda,beleza,servicos', 1, '{"listing_bonus": 12}'),
('prod_verify_basic',        'SELLER_VERIFY_BASIC','Verificação Básica',       'Selo verificado (telefone+email)', 500, 365, NULL, NULL, 1, '{"tier": "BASIC"}'),
('prod_verify_full',         'SELLER_VERIFY_FULL','Verificação Completa',     'KYC com documento',            2000, 365, NULL, NULL, 2, '{"tier": "FULL"}'),
-- Fase 3 — Boost + Business + Premium
('prod_boost_single',        'BOOST',             'Boost único',               'Sobe 1 posição imediatamente', 200,  NULL, NULL, 'electronicos', 1, '{"boost_score": 5}'),
('prod_boost_pack_5',        'BOOST',             'Pack 5 Boosts',             '5 boosts para o mesmo anúncio', 800,  NULL, NULL, 'electronicos', 2, '{"boost_count": 5, "boost_score": 5}'),
('prod_business_starter',    'BUSINESS_STARTER',  'Business Starter',          'Para lojas e revendedores',    10000, 30, 50, 'carros,imoveis', 1, '{"plan": "STARTER"}'),
('prod_business_pro',        'BUSINESS_PRO',      'Business Pro',              'Plano completo para negócios', 25000, 30, 200, 'carros,imoveis', 2, '{"plan": "PRO"}'),
('prod_premium_highlight',   'PREMIUM_HIGHLIGHT', 'Destaque Premium (14 dias)','Máxima visibilidade',          7500, 14, NULL, 'carros,imoveis', 1, '{"boost_score": 100}'),
('prod_premium_highlight_max','PREMIUM_HIGHLIGHT','Destaque Premium Max (30d)', 'Topo absoluto por 30 dias',   10000, 30, NULL, 'carros,imoveis', 2, '{"boost_score": 150}'),
-- Fase 4
('prod_contact_unlock',      'CONTACT_FEE',       'Ver contacto',              'Desbloquear número do vendedor', 100, NULL, NULL, 'servicos', 1, '{}'),
('prod_lead_single',         'PAID_LEADS',        'Lead único',                'Acesso a 1 lead qualificado',  500, NULL, NULL, 'servicos', 1, '{}'),
('prod_lead_pack_10',        'PAID_LEADS',        'Pack 10 Leads',             '10 leads para serviços',       4000, NULL, NULL, 'servicos', 2, '{"lead_count": 10}'),
('prod_ad_banner_week',      'ADVERTISING',       'Banner 7 dias',             'Banner na homepage',           15000, 7, NULL, NULL, 1, '{}'),
('prod_job_posting',         'JOB_POSTING',       'Publicar vaga',             'Anúncio de emprego activo 30d', 3000, 30, NULL, 'emprego', 1, '{}');

-- Provedores de pagamento locais angolanos
INSERT INTO monetization_payment_providers (id, name, provider_type, bank_name, account_holder, account_number, iban, phone_number, instructions, sort_order) VALUES
('prov_multicaixa_express', 'Multicaixa Express', 'MULTICAIXA_EXPRESS', 'EMIS', 'Kumbu Lda', NULL, NULL, '+244 9XX XXX XXX',
 '1. Abra a app Multicaixa Express\n2. Escolha "Pagamentos" → "Pagamento de Serviços"\n3. Entidade: KUMBU (ou use transferência)\n4. Referência: use o código gerado\n5. Confirme e envie o comprovativo', 1),
('prov_bai_transfer', 'Transferência BAI', 'BANK_TRANSFER', 'BAI - Banco Angolano de Investimentos', 'Kumbu Lda', 'XXXX XXXX XXXX XXXX', 'AO06 XXXX XXXX XXXX XXXX XXXX X', NULL,
 'Transferência para conta BAI. Use o código de referência no concepto/motivo da transferência. Envie comprovativo após pagamento.', 2),
('prov_bfa_transfer', 'Transferência BFA', 'BANK_TRANSFER', 'BFA - Banco de Fomento Angola', 'Kumbu Lda', 'XXXX XXXX XXXX XXXX', 'AO06 XXXX XXXX XXXX XXXX XXXX X', NULL,
 'Transferência para conta BFA. Indique o código de referência no motivo. Envie comprovativo.', 3),
('prov_bic_transfer', 'Transferência BIC', 'BANK_TRANSFER', 'BIC - Banco BIC', 'Kumbu Lda', 'XXXX XXXX XXXX XXXX', 'AO06 XXXX XXXX XXXX XXXX XXXX X', NULL,
 'Transferência BIC. Use o código de referência. Envie comprovativo.', 4),
('prov_emis_reference', 'Referência EMIS', 'EMIS_REFERENCE', 'EMIS', 'Kumbu Lda', NULL, NULL, NULL,
 'Pagamento por referência EMIS no Multicaixa (ATM ou Express). Entidade e referência serão geradas no checkout.', 5);
