# Arranca PostgreSQL, Redis e API para beta (API no PC)
# Uso: .\scripts\start-beta-pc.ps1
# Depois, noutra janela: cloudflared tunnel run kumbu-api

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not (Test-Path ".env")) {
    Write-Host "ERRO: Crie .env a partir de .env.beta-pc.example" -ForegroundColor Red
    exit 1
}

Write-Host "A subir PostgreSQL e Redis..." -ForegroundColor Cyan
docker compose up -d postgres redis

Write-Host "A aguardar PostgreSQL..." -ForegroundColor Cyan
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect -f "{{.State.Health.Status}}" kumbu-postgres 2>$null
    if ($status -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    Write-Host "AVISO: PostgreSQL ainda nao healthy — verifique docker compose logs postgres" -ForegroundColor Yellow
}

Write-Host "A subir API (build se necessario)..." -ForegroundColor Cyan
docker compose up -d --build api

Write-Host ""
Write-Host "Health local:" -ForegroundColor Green
try {
    Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 10
} catch {
    Write-Host "API ainda a arrancar — tente: curl http://localhost:8080/actuator/health" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Proximo passo (janela separada):" -ForegroundColor Cyan
Write-Host "  cloudflared tunnel run kumbu-api"
Write-Host ""
Write-Host "Teste publico:" -ForegroundColor Cyan
Write-Host "  curl https://api.kumbu-market.com/actuator/health"
