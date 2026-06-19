# Beta: Site na Vercel + API no PC (Cloudflare Tunnel)

Guia passo-a-passo para publicar **kumbu-market.com** na Vercel enquanto o backend corre no teu PC em Windows, exposto via **Cloudflare Tunnel** (HTTPS grátis, sem abrir portas no router).

**Tempo estimado:** 1–2 horas na primeira vez.

---

## Arquitectura

```
Utilizador
    │
    ├─► https://kumbu-market.com          ──► Vercel (site Next.js)
    ├─► https://admin.kumbu-market.com    ──► Vercel (admin, opcional)
    │
    └─► https://api.kumbu-market.com      ──► Cloudflare Tunnel ──► PC :8080
                                                      │
                                              PostgreSQL + Redis (Docker)
```

---

## Pré-requisitos

- [ ] Domínio **kumbu-market.com** (Hostinger)
- [ ] Conta grátis [Cloudflare](https://dash.cloudflare.com/sign-up)
- [ ] Conta grátis [Vercel](https://vercel.com) (+ GitHub com os repos)
- [ ] PC Windows com Docker Desktop
- [ ] Java 17 + Maven (se correr API fora do Docker)
- [ ] Repos no GitHub: `Kumbu_site_user`, `Kumbu_bakend`, `kumbu-admin`

---

## Parte 1 — DNS no Cloudflare (20 min)

### 1.1 Adicionar domínio ao Cloudflare

1. Cloudflare Dashboard → **Add a site** → `kumbu-market.com`
2. Plano **Free**
3. Cloudflare mostra 2 nameservers (ex.: `ada.ns.cloudflare.com`)

### 1.2 Alterar nameservers na Hostinger

1. Hostinger → Domínios → **kumbu-market.com** → DNS / Nameservers
2. Escolhe **Custom DNS** (ou “Alterar nameservers”)
3. Cola os 2 nameservers do Cloudflare
4. Aguarda propagação (5 min – 24 h; normalmente &lt; 1 h)

> Enquanto propaga, podes continuar as partes 2 e 3 no PC.

### 1.3 Registos DNS (Cloudflare → DNS → Records)

**Não cries ainda** o registo `api` — o túnel cria-o automaticamente. Cria só:

| Tipo | Nome | Conteúdo | Proxy |
|------|------|----------|-------|
| — | — | *(api será criado pelo túnel)* | — |

Os registos `kumbu-market.com` e `www` serão adicionados pela **Vercel** na Parte 4.

---

## Parte 2 — Backend no PC (30 min)

### 2.1 Instalar ferramentas

```powershell
# Docker Desktop — https://www.docker.com/products/docker-desktop/

# Cloudflared (túnel)
winget install Cloudflare.cloudflared
```

### 2.2 Configurar ambiente

```powershell
cd C:\Users\PC\Documents\Projetos\Kumbu_bakend
copy .env.beta-pc.example .env
notepad .env
```

Preenche **obrigatoriamente**:

| Variável | Exemplo |
|----------|---------|
| `POSTGRES_PASSWORD` | password forte |
| `KUMBU_JWT_SECRET` | string aleatória ≥ 32 caracteres |
| `KUMBU_ADMIN_PASSWORD` | ≠ `Admin123!` |
| `KUMBU_CORS_ORIGINS` | já vem com URLs Vercel + domínio |

Gera JWT seguro (PowerShell):

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

### 2.3 Subir base de dados + API

```powershell
cd C:\Users\PC\Documents\Projetos\Kumbu_bakend
docker compose up -d postgres redis
```

**Opção A — API em Docker (recomendado):**

```powershell
docker compose up -d --build api
curl http://localhost:8080/actuator/health
```

**Opção B — API com Maven (dev mais rápido):**

```powershell
# .env com SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kumbu
mvn spring-boot:run
```

Resposta esperada: `{"status":"UP"}`

### 2.4 PC sempre disponível

- Desactiva **suspensão** do Windows enquanto o beta estiver activo
- Energia → Ecrã e suspensão → **Nunca** (ligado à corrente)
- Backend e Docker devem arrancar quando ligas o PC (ou usa `scripts/start-beta-pc.ps1`)

---

## Parte 3 — Cloudflare Tunnel (20 min)

### 3.1 Login

```powershell
cloudflared tunnel login
```

Abre o browser → escolhe o domínio **kumbu-market.com**.

### 3.2 Criar túnel

```powershell
cloudflared tunnel create kumbu-api
```

Anota o **Tunnel ID** (UUID).

### 3.3 Configurar rotas

Copia o exemplo e edita o UUID:

```powershell
copy scripts\cloudflared-config.example.yml $env:USERPROFILE\.cloudflared\config.yml
notepad $env:USERPROFILE\.cloudflared\config.yml
```

Coloca o **Tunnel ID** e confirma `service: http://localhost:8080`.

### 3.4 DNS do túnel

```powershell
cloudflared tunnel route dns kumbu-api api.kumbu-market.com
```

Isto cria `api.kumbu-market.com` → túnel (HTTPS automático).

### 3.5 Arrancar túnel

```powershell
cloudflared tunnel run kumbu-api
```

Deixa esta janela **aberta** (ou instala como serviço Windows — ver §3.6).

### 3.6 Testar API pública

```powershell
curl https://api.kumbu-market.com/actuator/health
```

Deve responder `{"status":"UP"}`.

### 3.7 (Opcional) Túnel como serviço Windows

```powershell
cloudflared service install
cloudflared tunnel run kumbu-api
# Ou configurar serviço com config.yml — ver docs Cloudflare
```

Documentação: [developers.cloudflare.com/cloudflare-one/connections/connect-apps](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)

---

## Parte 4 — Site na Vercel (25 min)

### 4.1 Importar projecto

1. [vercel.com/new](https://vercel.com/new) → Import Git Repository
2. Repo **`Kumbu_site_user`**
3. Framework: **Next.js** (detectado automaticamente)
4. Root Directory: `.` (raiz do repo)

### 4.2 Variáveis de ambiente (Vercel → Settings → Environment Variables)

| Nome | Valor | Ambientes |
|------|-------|-----------|
| `NEXT_PUBLIC_KUMBU_API_URL` | `https://api.kumbu-market.com/api/v1` | Production, Preview |
| `NEXT_PUBLIC_SENTRY_DSN` | *(opcional)* | Production |
| `NEXT_PUBLIC_SENTRY_ENVIRONMENT` | `production` | Production |

### 4.3 Deploy

Clica **Deploy**. Aguarda build (~3–5 min).

### 4.4 Domínio customizado

1. Vercel → Project → **Settings → Domains**
2. Adiciona:
   - `kumbu-market.com`
   - `www.kumbu-market.com`
3. Vercel indica registos DNS — adiciona no **Cloudflare DNS**:

| Tipo | Nome | Conteúdo (Vercel indica o exacto) |
|------|------|-----------------------------------|
| A | `@` | `76.76.21.21` |
| CNAME | `www` | `cname.vercel-dns.com` |

4. Cloudflare: registo `www` → **Proxy ON** (laranja) — OK para site estático/SSR
5. Aguarda SSL (Vercel + Cloudflare)

### 4.5 Testar site

1. Abre `https://kumbu-market.com`
2. Registo / login
3. Lista produtos, chat, checkout básico
4. DevTools → Network: pedidos a `api.kumbu-market.com` (não `localhost`)

---

## Parte 5 — Admin na Vercel (opcional, 15 min)

Repete Parte 4 com repo **`kumbu-admin`**:

| Variável | Valor |
|----------|-------|
| `NEXT_PUBLIC_KUMBU_API_URL` | `https://api.kumbu-market.com/api/v1` |

Domínio: `admin.kumbu-market.com` (CNAME na Cloudflare → Vercel).

Actualiza no `.env` do backend (já incluído em `.env.beta-pc.example`):

```env
KUMBU_CORS_ORIGIN_PATTERNS=https://*.vercel.app
```

*(Reinicia a API após alterar CORS.)*

---

## Parte 6 — OAuth Google/Facebook

Quando o site estiver público, actualiza consolas OAuth:

**Google Cloud Console** → Credenciais → Origens JavaScript:

```
https://kumbu-market.com
https://www.kumbu-market.com
```

**Facebook Developers** → App → Domínios / OAuth redirect:

```
kumbu-market.com
```

Credenciais (`KUMBU_GOOGLE_*`, `KUMBU_FACEBOOK_*`) ficam só no `.env` do **backend no PC**.

---

## Parte 7 — Monitorização

Segue [MONITORING.md](./MONITORING.md):

- **UptimeRobot**: `kumbu-market.com`, `api.kumbu-market.com/actuator/health`
- **Sentry**: DSN no Vercel + `SENTRY_DSN` no `.env` do PC

---

## Checklist final

- [ ] `curl https://api.kumbu-market.com/actuator/health` → UP
- [ ] Site abre em HTTPS
- [ ] Login/registo funciona
- [ ] Imagens de produtos carregam (`https://api.kumbu-market.com/files/...`)
- [ ] Chat / notificações (WebSocket via `wss://api.kumbu-market.com/ws/chat`)
- [ ] PC ligado, Docker + túnel activos
- [ ] Sentry recebe erros de teste

---

## Resolução de problemas

| Problema | Causa provável | Solução |
|----------|----------------|---------|
| CORS error no browser | Origem não está em `KUMBU_CORS_ORIGINS` | Adiciona URL exacta do site (com `https://`) e reinicia API |
| API 502 / timeout | Túnel parado ou API down | `cloudflared tunnel run` + `docker compose ps` |
| Mixed content | API em HTTP | Usar sempre `https://api.kumbu-market.com` |
| Imagens não aparecem | `KUMBU_STORAGE_URL` errado | Deve ser `https://api.kumbu-market.com/files` |
| WebSocket falha | Túnel ou CORS WS | Confirmar `wss://api.kumbu-market.com/ws/chat` no DevTools |
| OAuth origin_mismatch | Google/Facebook | Adicionar domínio HTTPS nas consolas |
| Site OK, API offline | PC dormiu | Desactivar suspensão; usar serviço cloudflared |

---

## Limitações deste setup

- PC tem de estar **ligado 24/7** durante o beta
- Internet de casa instável = site instável
- Uploads ficam no disco do PC (`./uploads` ou volume Docker)
- **Não** recomendado para tráfego alto — migrar API para VPS quando validares o produto

---

## Próximo passo (produção)

Quando estiveres pronto: VPS único (Docker Compose) com site + API + admin + Caddy — sem depender do PC. Ver `.env.production.example` e `docker-compose.yml`.

---

## Scripts úteis

```powershell
# Arrancar stack local (postgres + redis + API)
.\scripts\start-beta-pc.ps1

# Túnel (janela separada)
cloudflared tunnel run kumbu-api
```
