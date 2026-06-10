-- Seed data: categorias, métodos de pagamento, filtros, marketing, admin bootstrap placeholder

INSERT INTO catalog_categories (id, name, icon_key, accent_hex, sort_order, kind) VALUES
    ('telemoveis', 'Telemóveis', 'smartphone', '1565C0', 1, 'product'),
    ('carros', 'Carros', 'directions_car', '37474F', 2, 'product'),
    ('imoveis', 'Imóveis', 'home', '2E7D32', 3, 'stay'),
    ('servicos', 'Serviços', 'build', 'F57C00', 4, 'product'),
    ('moda', 'Moda', 'checkroom', 'AD1457', 5, 'product'),
    ('eletronicos', 'Electrónicos', 'devices', '4527A0', 6, 'product'),
    ('moveis', 'Móveis', 'chair', '795548', 7, 'product'),
    ('desporto', 'Desporto', 'sports_soccer', '00838F', 8, 'product'),
    ('empregos', 'Empregos', 'work', '283593', 9, 'job')
ON CONFLICT DO NOTHING;

INSERT INTO catalog_subcategories (category_id, id, label, sort_order) VALUES
    ('telemoveis', 'smartphones', 'Smartphones', 1),
    ('telemoveis', 'acessorios', 'Acessórios', 2),
    ('carros', 'sedan', 'Sedan', 1),
    ('carros', 'suv', 'SUV', 2),
    ('imoveis', 'apartamento', 'Apartamento', 1),
    ('imoveis', 'casa', 'Casa', 2),
    ('empregos', 'ti', 'TI', 1),
    ('empregos', 'vendas', 'Vendas', 2)
ON CONFLICT DO NOTHING;

INSERT INTO app_payment_methods (id, label, icon_key, sort_order, is_default) VALUES
    ('multicaixa', 'Multicaixa Express', 'payment', 1, TRUE),
    ('transferencia', 'Transferência Bancária', 'account_balance', 2, FALSE),
    ('dinheiro', 'Dinheiro na entrega', 'money', 3, FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO app_category_sort_filters (id, label, sort_mode, sort_order) VALUES
    ('default', 'Relevância', 'default', 1),
    ('rating', 'Melhor avaliação', 'rating_desc', 2),
    ('price', 'Menor preço', 'price_asc', 3)
ON CONFLICT DO NOTHING;

INSERT INTO app_marketing_blocks (id, kind, title, subtitle, gradient_from, gradient_to, sort_order) VALUES
    ('hero-1', 'hero', 'Bem-vindo ao Kumbú', 'Compre e venda com segurança', 'C62828', 'AD1457', 1),
    ('offer-1', 'offers', 'Ofertas da semana', 'Até 30% de desconto', '1565C0', '4527A0', 2)
ON CONFLICT DO NOTHING;

INSERT INTO legal_documents (slug, title, intro, sections) VALUES
    ('terms', 'Termos de Utilização', 'Leia atentamente os termos.', '[]'::JSONB),
    ('privacy', 'Política de Privacidade', 'Como tratamos os seus dados.', '[]'::JSONB)
ON CONFLICT DO NOTHING;
