# Migração Supabase → Spring Boot (Kumbu Backend)

Este documento descreve a migração completa para o backend Spring Boot como **única fonte de verdade**.

## Estado actual

| Componente | Backend | Supabase |
|------------|---------|----------|
| App Flutter | ✅ REST (default) | ❌ removido do `main.dart` |
| Site Next.js | ✅ REST (default) | fallback só se `NEXT_PUBLIC_KUMBU_API_URL=supabase` |
| Admin Next.js | ✅ REST (default) | fallback só se `NEXT_PUBLIC_KUMBU_API_URL=supabase` |
| Auth email/password | ✅ | — |
| OAuth Google/Facebook | ✅ `/auth/oauth/{provider}` | — |
| SMS OTP | ✅ `/auth/phone/*` (log em dev) | — |
| Reset password | ✅ | — |
| Verificação email | ✅ | — |
| Rate limiting auth | ✅ | — |
| Security headers | ✅ | — |

## Passos para produção

### 1. Base de dados

```bash
cd Kumbu_bakend
docker compose up -d
```

Usa **PostgreSQL próprio** — não o Postgres do Supabase.

### 2. Variáveis obrigatórias (produção)

```env
SPRING_PROFILES_ACTIVE=prod
KUMBU_JWT_SECRET=<mínimo 32 caracteres aleatórios>
SPRING_DATASOURCE_URL=jdbc:postgresql://...
KUMBU_CORS_ORIGINS=https://app.kumbu.app,https://admin.kumbu.app
KUMBU_ADMIN_EMAIL=admin@kumbu.app
KUMBU_ADMIN_PASSWORD=<password forte>
```

### 3. Frontends

**Admin + Site** (`.env.local`):
```env
NEXT_PUBLIC_KUMBU_API_URL=https://api.kumbu.app/api/v1
```

**Flutter** (`assets/api.local.json`):
```json
{ "baseUrl": "https://api.kumbu.app" }
```

### 4. Desactivar Supabase

1. Remover variáveis `NEXT_PUBLIC_SUPABASE_*` dos `.env.local`
2. Não definir `supabase.local.json` na app Flutter
3. (Opcional) Desactivar projecto Supabase no dashboard

### 5. SMS em produção

Configurar provider SMS (Infobip, SMS.ru, Africa's Talking) — ver logs `[DEV] SMS OTP` em desenvolvimento.

### 6. Email em produção

Adicionar ao `application-prod.yml`:
```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ...
    password: ...
kumbu:
  mail:
    from: noreply@kumbu.app
  app:
    public-url: https://kumbu.app
```

## Rollback de emergência

Defina temporariamente:
```env
NEXT_PUBLIC_KUMBU_API_URL=supabase
```
Isto reactiva o fallback Supabase nos frontends Next.js (requer credenciais Supabase ainda configuradas).

## Segurança implementada

- JWT stateless + refresh token rotation
- BCrypt strength 12
- Rate limit em `/api/v1/auth/*` (30 req/min/IP)
- Headers: HSTS, X-Frame-Options DENY
- Ban de utilizadores (admin + API)
- Audit log centralizado
- Tokens de reset/verify/OTP com hash SHA-256 e expiração

## Checklist pós-migração

- [ ] Backend a correr com Postgres dedicado
- [ ] JWT secret único em produção
- [ ] HTTPS em todos os domínios
- [ ] CORS restrito aos domínios reais
- [ ] SMTP configurado
- [ ] SMS provider configurado
- [ ] Backup automático PostgreSQL
- [ ] Monitorização (`/actuator/health`)
- [ ] Supabase desactivado
