# Kumbu Backend — Plataforma Completa

API REST Spring Boot para **Kumbú** (marketplace C2C Angola).

## Migração completa (Supabase → Spring Boot)

Ver **`MIGRATION.md`** para o guia completo.

Os frontends usam o backend Spring Boot **por defeito**. Supabase só activo se definires `NEXT_PUBLIC_KUMBU_API_URL=supabase` (rollback de emergência).

| Projeto | Caminho | Modo REST |
|---------|---------|-----------|
| App Flutter | `kumbu_app_user` | `assets/api.local.json` → `baseUrl` |
| Site web | `Kumbu_site_user` | `NEXT_PUBLIC_KUMBU_API_URL` |
| Admin | `kumbu-admin` | `NEXT_PUBLIC_KUMBU_API_URL` |

**Fallback Supabase** mantido em todos os frontends quando REST não está configurado.

---

## Arrancar tudo

```bash
# Backend + PostgreSQL
cd Kumbu_bakend
docker compose up -d

# Admin (porta 3001)
cd kumbu-admin
# .env.local → NEXT_PUBLIC_KUMBU_API_URL=http://localhost:8080/api/v1
npm run dev

# Site (porta 3000)
cd Kumbu_site_user
# .env.local → NEXT_PUBLIC_KUMBU_API_URL=http://localhost:8080/api/v1
npm run dev

# App Flutter
# assets/api.local.json → { "baseUrl": "http://10.0.2.2:8080" }
flutter run
```

**Swagger:** http://localhost:8080/swagger-ui.html  
**Admin:** `admin@kumbu.app` / `Admin123!`

---

## API — Módulos

### Público (sem auth)
- `GET /api/v1/catalog/*` — categorias, anúncios, pesquisa
- `GET /api/v1/platform/*` — marketing, suporte, filtros, legal

### Utilizador (JWT)
- `/api/v1/auth/*` — registo, login, refresh, logout
- `/api/v1/users/*` — perfil, favoritos, carrinho, export GDPR
- `/api/v1/store/*` — loja, payment methods
- `/api/v1/orders/*` — checkout, compras, vendas
- `/api/v1/chat/*` + WebSocket `/ws/chat` — mensagens
- `/api/v1/notifications/*`
- `/api/v1/reviews/*`
- `/api/v1/jobs/*` — CVs, candidaturas
- `/api/v1/rentals/*` — alugueres imóveis
- `/api/v1/files/*` — uploads

### Admin (role ADMIN)
- `/api/v1/admin/users/*` — gestão utilizadores
- `/api/v1/admin/products/*` — moderação anúncios
- `/api/v1/admin/orders/*`
- `/api/v1/admin/conversations/*`
- `/api/v1/admin/reports/*`
- `/api/v1/admin/reviews/*`
- `/api/v1/admin/notifications/*`
- `/api/v1/admin/categories/*`
- `/api/v1/admin/marketing-blocks/*`
- `/api/v1/admin/legal/*`
- `/api/v1/admin/filters/*`
- `/api/v1/admin/payment-methods/*`
- `/api/v1/admin/support-settings/*`
- `/api/v1/admin/admins/*`
- `/api/v1/admin/audit/*`
- `/api/v1/admin/dashboard/*`
- `/api/v1/admin/analytics/*`

---

## Configuração por frontend

### Flutter (`assets/api.local.json`)
```json
{ "baseUrl": "http://10.0.2.2:8080" }
```
Em dispositivo físico: IP da máquina (ex: `192.168.1.10`).

### Next.js (admin + site)
```env
NEXT_PUBLIC_KUMBU_API_URL=http://localhost:8080/api/v1
```

---

## Variáveis backend

| Variável | Descrição |
|----------|-----------|
| `KUMBU_JWT_SECRET` | Chave JWT (min. 32 chars) |
| `KUMBU_CORS_ORIGINS` | Origens permitidas |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC |
| `KUMBU_ADMIN_EMAIL` | Bootstrap super admin |
| `KUMBU_ADMIN_PASSWORD` | Password bootstrap |

---

## Deploy

```bash
mvn clean package -DskipTests
docker compose up -d   # inclui API + Postgres
```

CI: `.github/workflows/ci.yml`

---

## Limitações conhecidas

- OAuth (Google/Facebook) e SMS OTP — ainda não implementados no backend
- Realtime admin — polling via refresh (sem WebSocket no painel)
- Alguns componentes do site ainda importam Supabase directamente (fallback activo)
