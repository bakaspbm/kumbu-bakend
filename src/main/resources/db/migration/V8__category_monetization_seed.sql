-- Estratégias e produtos por categoria (Angola)

INSERT INTO monetization_category_strategies
(category_id, primary_monetization, secondary_monetizations, strategy_title, strategy_description, why_description, cta_message, cta_button_label, revenue_tier, enabled_feature_types, sort_order, metadata) VALUES

('eletronicos', 'HIGHLIGHT_TOP',
 '["BOOST","SELLER_VERIFY_BASIC","SELLER_VERIFY_FULL","HIGHLIGHT_SIMPLE","HIGHLIGHT_URGENT"]',
 'Eletrónicos — venda rápida',
 'Alta concorrência. Vendedores querem vender rápido.',
 'Muitos vendedores, produtos com alta procura. Destaque + boost convertem melhor.',
 'Quer vender o teu equipamento hoje?', 'Boost agora', 'HIGH',
 '["HIGHLIGHT_TOP","HIGHLIGHT_SIMPLE","HIGHLIGHT_URGENT","BOOST","SELLER_VERIFY_BASIC","SELLER_VERIFY_FULL"]', 1,
 '{"note":"Telemóveis partilham mesma lógica"}'),

('telemoveis', 'HIGHLIGHT_TOP',
 '["BOOST","SELLER_VERIFY_BASIC","HIGHLIGHT_URGENT"]',
 'Telemóveis — venda rápida',
 'Alta concorrência em smartphones e acessórios.',
 'Produtos com muita procura. Vendedores pagam por visibilidade imediata.',
 'Quer vender o teu iPhone hoje?', 'Boost agora', 'HIGH',
 '["HIGHLIGHT_TOP","HIGHLIGHT_SIMPLE","HIGHLIGHT_URGENT","BOOST","SELLER_VERIFY_BASIC"]', 2,
 '{"alias_of":"eletronicos"}'),

('moda', 'VIP_PLAN',
 '["HIGHLIGHT_SIMPLE","HIGHLIGHT_TOP"]',
 'Moda — volume com VIP',
 'Margem baixa → assinatura mensal barata em vez de destaque caro.',
 'Muitos revendedores informais. Vender volume com plano VIP acessível.',
 'Publique mais peças por menos', 'Assinar VIP', 'MEDIUM',
 '["VIP_PLAN","HIGHLIGHT_SIMPLE","HIGHLIGHT_TOP"]', 3,
 '{"listing_pack":true}'),

('eletrodomesticos', 'HIGHLIGHT_SIMPLE',
 '["BOOST","SELLER_VERIFY_BASIC","SELLER_VERIFY_FULL"]',
 'Eletrodomésticos — confiança',
 'Produtos caros, compra pensada. Verificação aumenta conversão.',
 'Ticket médio-alto. Compradores exigem vendedor verificado.',
 '✔ Vendedor verificado vende mais', 'Verificar conta', 'MEDIUM',
 '["HIGHLIGHT_SIMPLE","HIGHLIGHT_URGENT","BOOST","SELLER_VERIFY_BASIC","SELLER_VERIFY_FULL"]', 4, '{}'),

('beleza', 'VIP_PLAN',
 '["HIGHLIGHT_SIMPLE","BUSINESS_STARTER"]',
 'Beleza — mini lojinhas',
 'Vendas recorrentes. Pequenos negócios femininos/informais.',
 'Ideal para criar lojinhas dentro da plataforma com VIP + página de vendedor.',
 'Crie a sua lojinha na Kumbu', 'Plano VIP + Loja', 'MEDIUM',
 '["VIP_PLAN","HIGHLIGHT_SIMPLE","BUSINESS_STARTER"]', 5,
 '{"includes_mini_store":true}'),

('moveis', 'HIGHLIGHT_SIMPLE',
 '["BOOST","SELLER_VERIFY_BASIC","HIGHLIGHT_URGENT"]',
 'Móveis — destaque pago',
 'Produtos grandes, decisão lenta, alto valor.',
 'Vendedores aceitam pagar destaque porque o ticket justifica.',
 'Destaque o seu móvel', 'Destacar anúncio', 'MEDIUM',
 '["HIGHLIGHT_SIMPLE","HIGHLIGHT_URGENT","BOOST","SELLER_VERIFY_BASIC"]', 6, '{}'),

('carros', 'PREMIUM_HIGHLIGHT',
 '["BUSINESS_STARTER","BUSINESS_PRO","CONTACT_FEE","BOOST"]',
 'Carros — OURO PURO',
 'Ticket alto (milhões Kz). Vendedores profissionais. Alta competição.',
 'Aqui pode cobrar caro: destaque premium + plano business + contacto pago.',
 'Destaque premium para stand ou particular', 'Destaque Premium', 'PREMIUM',
 '["PREMIUM_HIGHLIGHT","BUSINESS_STARTER","BUSINESS_PRO","CONTACT_FEE","BOOST","HIGHLIGHT_URGENT"]', 7,
 '{"price_range":"5000-15000"}'),

('desporto', 'BOOST',
 '["HIGHLIGHT_TOP","HIGHLIGHT_SIMPLE"]',
 'Desporto — boost barato',
 'Baixo volume vs outras categorias. Preço médio.',
 'Não espere muito dinheiro aqui — boost e destaques baratos.',
 'Suba o seu anúncio', 'Boost', 'LOW',
 '["BOOST","HIGHLIGHT_TOP","HIGHLIGHT_SIMPLE"]', 8, '{}'),

('servicos', 'VIP_PLAN',
 '["PAID_LEADS","HIGHLIGHT_SIMPLE","CONTACT_FEE"]',
 'Serviços — clientes constantes',
 'Profissionais querem clientes constantes. Confiança é tudo.',
 'VIP + leads pagos no futuro. "Receba mais clientes" → assinatura.',
 'Receba mais clientes', 'Assinar VIP', 'HIGH',
 '["VIP_PLAN","HIGHLIGHT_SIMPLE","PAID_LEADS","CONTACT_FEE"]', 9, '{}'),

('empregos', 'JOB_POSTING',
 '["HIGHLIGHT_URGENT","BUSINESS_STARTER"]',
 'Emprego — publicação paga',
 'Empresas pagam para contratar. Candidatos NÃO pagam.',
 'Modelo B2B: cobrar publicação de vaga + destaque + plano empresa.',
 'Publique a sua vaga', 'Publicar vaga', 'MEDIUM',
 '["JOB_POSTING","HIGHLIGHT_URGENT","BUSINESS_STARTER"]', 10,
 '{"candidates_free":true}'),

('imoveis', 'PREMIUM_HIGHLIGHT',
 '["BUSINESS_STARTER","BUSINESS_PRO","BOOST"]',
 'Imóveis — TOP 3 receita',
 'Ticket altíssimo. Querem vender/alugar rápido.',
 'Destaque premium + plano business para imobiliárias.',
 'Destaque para imobiliária ou particular', 'Destaque Premium', 'PREMIUM',
 '["PREMIUM_HIGHLIGHT","BUSINESS_STARTER","BUSINESS_PRO","BOOST","HIGHLIGHT_URGENT"]', 11,
 '{"price_range":"3000-10000"}');

-- Produtos por categoria (preços específicos)
-- Limpar category_hint genérico — usar category_id
UPDATE monetization_products SET category_id = NULL WHERE category_id IS NULL;

-- Eletrónicos
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_eletronicos_highlight_top', 'HIGHLIGHT_TOP', 'Subir ao topo (7d)', 'Topo da categoria Electrónicos', 300, 7, 'eletronicos', 1, '{"boost_score":10}', TRUE, NOW()),
('prod_eletronicos_highlight_simple', 'HIGHLIGHT_SIMPLE', 'Destaque simples (7d)', 'Badge destaque', 700, 7, 'eletronicos', 2, '{"boost_score":25}', TRUE, NOW()),
('prod_eletronicos_highlight_urgent', 'HIGHLIGHT_URGENT', 'Urgente (7d)', 'Etiqueta URGENTE', 1500, 7, 'eletronicos', 3, '{"boost_score":50}', TRUE, NOW()),
('prod_eletronicos_boost', 'BOOST', 'Boost único', 'Sobe posição imediatamente', 200, NULL, 'eletronicos', 4, '{"boost_score":5}', TRUE, NOW()),
('prod_eletronicos_boost_5', 'BOOST', 'Pack 5 Boosts', '5 boosts', 800, NULL, 'eletronicos', 5, '{"boost_count":5,"boost_score":5}', TRUE, NOW()),
('prod_eletronicos_verify_basic', 'SELLER_VERIFY_BASIC', 'Verificação básica', 'Selo verificado', 500, 365, 'eletronicos', 6, '{"tier":"BASIC"}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Telemóveis (mesmos preços eletrónicos)
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_telemoveis_boost', 'BOOST', 'Boost — vender hoje', 'Quer vender o teu iPhone hoje?', 200, NULL, 'telemoveis', 1, '{"boost_score":5}', TRUE, NOW()),
('prod_telemoveis_highlight_top', 'HIGHLIGHT_TOP', 'Subir ao topo (7d)', 'Topo em Telemóveis', 300, 7, 'telemoveis', 2, '{"boost_score":10}', TRUE, NOW()),
('prod_telemoveis_highlight_urgent', 'HIGHLIGHT_URGENT', 'Urgente (7d)', 'Venda rápida', 1500, 7, 'telemoveis', 3, '{"boost_score":50}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Moda
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, max_listings, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_moda_vip', 'VIP_PLAN', 'VIP Moda (30d)', 'Mais anúncios + posição melhor', 1500, 30, 20, 'moda', 1, '{"listing_bonus":17}', TRUE, NOW()),
('prod_moda_highlight_light', 'HIGHLIGHT_SIMPLE', 'Destaque leve (7d)', 'Destaque barato para moda', 400, 7, NULL, 'moda', 2, '{"boost_score":15}', TRUE, NOW()),
('prod_moda_listing_pack', 'VIP_PLAN', 'Pacote 10 anúncios extra', 'Volume para revendedores', 800, 30, 10, 'moda', 3, '{"listing_bonus":10}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Eletrodomésticos
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_eletrodom_highlight', 'HIGHLIGHT_SIMPLE', 'Destaque médio (7d)', 'Destaque para eletrodomésticos', 1000, 7, 'eletrodomesticos', 1, '{"boost_score":30}', TRUE, NOW()),
('prod_eletrodom_boost', 'BOOST', 'Boost', 'Subir anúncio', 300, NULL, 'eletrodomesticos', 2, '{"boost_score":5}', TRUE, NOW()),
('prod_eletrodom_verify', 'SELLER_VERIFY_BASIC', 'Verificação', '✔ Vendedor verificado', 500, 365, 'eletrodomesticos', 3, '{"tier":"BASIC"}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Beleza
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, max_listings, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_beleza_vip', 'VIP_PLAN', 'VIP Beleza + Mini Loja', 'Lojinha dentro da plataforma', 1500, 30, 25, 'beleza', 1, '{"includes_mini_store":true,"listing_bonus":22}', TRUE, NOW()),
('prod_beleza_highlight', 'HIGHLIGHT_SIMPLE', 'Destaque barato (7d)', 'Destaque acessível', 500, 7, NULL, 'beleza', 2, '{"boost_score":20}', TRUE, NOW()),
('prod_beleza_store', 'BUSINESS_STARTER', 'Mini Loja Pro', 'Página de vendedor completa', 5000, 30, 40, 'beleza', 3, '{"includes_mini_store":true}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Móveis
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_moveis_highlight', 'HIGHLIGHT_SIMPLE', 'Destaque (7d)', 'Destaque para móveis', 1200, 7, 'moveis', 1, '{"boost_score":35}', TRUE, NOW()),
('prod_moveis_boost', 'BOOST', 'Boost', 'Subir anúncio', 400, NULL, 'moveis', 2, '{"boost_score":5}', TRUE, NOW()),
('prod_moveis_verify', 'SELLER_VERIFY_BASIC', 'Verificação', 'Selo de confiança', 500, 365, 'moveis', 3, '{"tier":"BASIC"}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Carros (preços premium)
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_carros_premium_7d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium (7d)', 'Máxima visibilidade — carros', 5000, 7, 'carros', 1, '{"boost_score":100}', TRUE, NOW()),
('prod_carros_premium_14d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium (14d)', 'Topo absoluto', 10000, 14, 'carros', 2, '{"boost_score":120}', TRUE, NOW()),
('prod_carros_premium_30d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium Max (30d)', 'Para stands profissionais', 15000, 30, 'carros', 3, '{"boost_score":150}', TRUE, NOW()),
('prod_carros_business', 'BUSINESS_STARTER', 'Plano Business Carros', 'Stand / revendedor profissional', 15000, 30, 'carros', 4, '{"max_listings":50}', TRUE, NOW()),
('prod_carros_business_pro', 'BUSINESS_PRO', 'Business Pro Carros', 'Stand premium', 25000, 30, 'carros', 5, '{"max_listings":200}', TRUE, NOW()),
('prod_carros_contact', 'CONTACT_FEE', 'Ver contacto', 'Desbloquear número do vendedor', 100, NULL, 'carros', 6, '{}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Desporto (barato)
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_desporto_boost', 'BOOST', 'Boost', 'Subir anúncio', 200, NULL, 'desporto', 1, '{"boost_score":5}', TRUE, NOW()),
('prod_desporto_highlight', 'HIGHLIGHT_TOP', 'Destaque barato (7d)', 'Destaque acessível', 400, 7, 'desporto', 2, '{"boost_score":10}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Serviços
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, max_listings, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_servicos_vip', 'VIP_PLAN', 'VIP Serviços', 'Receba mais clientes', 1500, 30, 15, 'servicos', 1, '{"listing_bonus":12}', TRUE, NOW()),
('prod_servicos_highlight', 'HIGHLIGHT_SIMPLE', 'Destaque (7d)', 'Mais visibilidade', 700, 7, NULL, 'servicos', 2, '{"boost_score":25}', TRUE, NOW()),
('prod_servicos_lead', 'PAID_LEADS', 'Lead único', 'Acesso a 1 cliente qualificado', 500, NULL, NULL, 'servicos', 3, '{}', TRUE, NOW()),
('prod_servicos_leads_10', 'PAID_LEADS', 'Pack 10 Leads', '10 clientes potenciais', 4000, NULL, NULL, 'servicos', 4, '{"lead_count":10}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Emprego
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_empregos_post', 'JOB_POSTING', 'Publicar vaga (30d)', 'Anúncio de emprego activo', 3000, 30, 'empregos', 1, '{}', TRUE, NOW()),
('prod_empregos_post_premium', 'JOB_POSTING', 'Publicar vaga Premium', 'Publicação + destaque', 5000, 30, 'empregos', 2, '{"includes_highlight":true}', TRUE, NOW()),
('prod_empregos_highlight', 'HIGHLIGHT_URGENT', 'Destaque de vaga (+7d)', 'Vaga em destaque', 2000, 7, 'empregos', 3, '{"boost_score":50}', TRUE, NOW()),
('prod_empregos_empresa', 'BUSINESS_STARTER', 'Plano Empresa', 'Múltiplas vagas', 10000, 30, 'empregos', 4, '{"max_jobs":10}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Imóveis
INSERT INTO monetization_products (id, feature_type, name, description, price_kz, duration_days, category_id, sort_order, metadata, is_active, created_at) VALUES
('prod_imoveis_premium_7d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium (7d)', 'Imóvel em destaque', 3000, 7, 'imoveis', 1, '{"boost_score":80}', TRUE, NOW()),
('prod_imoveis_premium_14d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium (14d)', 'Alta visibilidade', 6000, 14, 'imoveis', 2, '{"boost_score":100}', TRUE, NOW()),
('prod_imoveis_premium_30d', 'PREMIUM_HIGHLIGHT', 'Destaque Premium (30d)', 'Para imobiliárias', 10000, 30, 'imoveis', 3, '{"boost_score":130}', TRUE, NOW()),
('prod_imoveis_business', 'BUSINESS_STARTER', 'Plano Imobiliária', 'Business para imobiliárias', 15000, 30, 'imoveis', 4, '{"max_listings":80}', TRUE, NOW()),
('prod_imoveis_business_pro', 'BUSINESS_PRO', 'Business Pro Imóveis', 'Plano completo', 25000, 30, 'imoveis', 5, '{"max_listings":200}', TRUE, NOW()),
('prod_imoveis_boost', 'BOOST', 'Boost imóvel', 'Subir anúncio', 500, NULL, 'imoveis', 6, '{"boost_score":5}', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;
