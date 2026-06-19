# Monitorização Kumbu — Sentry + Uptime

Guia para saber quando utilizadores ou serviços têm problemas em **kumbu-market.com**.

**Deploy beta (site Vercel + API no PC):** [DEPLOY_BETA_PC.md](./DEPLOY_BETA_PC.md)

## Arquitectura

| Camada | Ferramenta | O que detecta |
|--------|------------|---------------|
| Disponibilidade | UptimeRobot (grátis) | Site, admin ou API offline |
| Erros técnicos | Sentry (grátis até ~5k eventos/mês) | Crashes JS, erros 500, timeouts |
| Suporte humano | Admin → Support Inbox | Utilizador pediu ajuda |
| Moderação | Admin → Reports | Denúncias de conteúdo |
| Auditoria | Admin → Audit | Acções de admins |

---

## 1. Sentry — configuração inicial (15 min)

### Criar conta e projectos

1. Registar em [sentry.io](https://sentry.io)
2. Criar **3 projectos** (platform):
   - `kumbu-site` → Next.js
   - `kumbu-admin` → Next.js
   - `kumbu-api` → Spring Boot
3. Copiar o **DSN** de cada projecto

### Variáveis por ambiente

#### Site (`Kumbu_site_user/.env.local` ou env do servidor)

```env
NEXT_PUBLIC_SENTRY_DSN=https://xxx@oXXX.ingest.sentry.io/XXX
NEXT_PUBLIC_SENTRY_ENVIRONMENT=production
```

#### Admin (`kumbu-admin/.env.local`)

```env
NEXT_PUBLIC_SENTRY_DSN=https://xxx@oXXX.ingest.sentry.io/XXX
NEXT_PUBLIC_SENTRY_ENVIRONMENT=production
```

#### API (`Kumbu_bakend/.env`)

```env
SENTRY_DSN=https://xxx@oXXX.ingest.sentry.io/XXX
SENTRY_ENVIRONMENT=production
SENTRY_TRACES_SAMPLE_RATE=0.1
```

> Sem DSN, o Sentry fica **desactivado** — dev local não envia nada.

### Source maps (opcional, recomendado em produção)

Para stack traces legíveis no site/admin, no CI ou servidor de build:

```env
SENTRY_ORG=sua-org
SENTRY_PROJECT=kumbu-site   # ou kumbu-admin
SENTRY_AUTH_TOKEN=sntrys_...
```

Gerar token em Sentry → Settings → Auth Tokens (scope: `project:releases`).

### Testar

- **API**: provocar erro 500 num endpoint de teste → aparece em Sentry em segundos
- **Site**: no browser dev, `throw new Error("teste sentry")` numa página → Issues
- **Admin**: idem

### O que vês no Sentry

- URL da página / endpoint
- Stack trace
- Browser / OS
- User ID (site, se logado — via `SentryUserBridge`)
- Ambiente (`production`, `staging`, etc.)

### Alertas Sentry

Em cada projecto → **Alerts** → New Alert Rule:

- *Issues* → “When a new issue is created” → Email ou Slack/Telegram
- Opcional: “When an issue affects ≥ 10 users in 1 hour”

---

## 2. UptimeRobot — checklist (10 min)

Registar em [uptimerobot.com](https://uptimerobot.com) (plano free: 50 monitors).

### Monitors a criar

| Nome | URL | Tipo | Intervalo |
|------|-----|------|-----------|
| Kumbu Site | `https://kumbu-market.com` | HTTP(s) | 5 min |
| Kumbu Admin | `https://admin.kumbu-market.com/login` | HTTP(s) | 5 min |
| Kumbu API Health | `https://api.kumbu-market.com/actuator/health` | HTTP(s) | 5 min |

### Configuração recomendada

- **Alert contacts**: email principal + Telegram (bot UptimeRobot)
- **Alert when down**: após **2 checks falhados** (~10 min)
- **Keyword monitor** (API): opcional — URL health, keyword `UP` no JSON `"status":"UP"`

Resposta esperada da API:

```json
{"status":"UP"}
```

### Pós-deploy — verificar manualmente

```bash
curl -s https://api.kumbu-market.com/actuator/health
curl -I https://kumbu-market.com
curl -I https://admin.kumbu-market.com/login
```

Todos devem responder **200** (ou 307/308 seguido de 200 no site).

---

## 3. Rotina operacional (beta)

### Diária (5 min)

- [ ] UptimeRobot — tudo verde?
- [ ] Sentry — novos issues? Priorizar `INTERNAL_ERROR`, checkout, auth, chat
- [ ] Admin → **Support Inbox** — conversas em espera
- [ ] Admin → **Reports** — denúncias pendentes

### Semanal

- [ ] Sentry — marcar issues resolvidos / ignorar ruído
- [ ] Rever logs do VPS (disco, RAM, CPU)
- [ ] Confirmar backups PostgreSQL

---

## 4. Onde olhar por tipo de problema

| Sintoma | Onde investigar |
|---------|-----------------|
| Página branca / crash | Sentry (projecto site) |
| Erro ao comprar/publicar | Sentry site + API + logs backend |
| Login falha | Sentry API + rate limit nos logs |
| API lenta | Sentry performance + VPS |
| Site não abre | UptimeRobot + Caddy/Nginx + DNS Hostinger |
| Utilizador pediu ajuda | Admin → Support Inbox |
| Conteúdo impróprio | Admin → Reports |

---

## 5. DNS Hostinger (kumbu-market.com)

| Registo | Tipo | Destino |
|---------|------|---------|
| `@` | A | IP do VPS |
| `www` | CNAME | `kumbu-market.com` |
| `api` | A | IP do VPS |
| `admin` | A | IP do VPS |

SSL: Caddy ou Nginx com Let's Encrypt no VPS.

---

## 6. Privacidade

- Sentry **não** envia passwords ( `sendDefaultPii: false` )
- Site envia apenas `userId`, email e nome quando logado
- Analytics de marketing (GA4, etc.) é separado e requer consentimento no banner de cookies

---

## Ficheiros relevantes no código

| Projecto | Ficheiros |
|----------|-----------|
| Site | `src/instrumentation.ts`, `src/instrumentation-client.ts`, `sentry.*.config.ts`, `SentryUserBridge` |
| Admin | Idem |
| API | `pom.xml` (sentry-spring-boot-starter), `GlobalExceptionHandler`, `application.yml` |

Ver também `.env.example`, `.env.production.example` e `.env.local.example` em cada repo.
