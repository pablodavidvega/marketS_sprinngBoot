# ─────────────────────────────────────────────
#  MarketS — Levantar entorno de desarrollo
#  Uso: .\start-dev.ps1
#  Requisito previo: haber ejecutado .\setup.ps1
# ─────────────────────────────────────────────

$Root     = $PSScriptRoot
$Frontend = Join-Path $Root "frontend"
$Uploads  = "C:\uploads\hana"

# Validaciones rápidas antes de arrancar
if (-not (Test-Path $Uploads)) {
    Write-Host "[error] Falta la carpeta de uploads. Ejecuta primero: .\setup.ps1" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path (Join-Path $Frontend "node_modules"))) {
    Write-Host "[error] Faltan dependencias npm. Ejecuta primero: .\setup.ps1" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "===  MarketS Dev  ===" -ForegroundColor Cyan
Write-Host "  Backend  → http://localhost:8081" -ForegroundColor Yellow
Write-Host "  Frontend → http://localhost:3000" -ForegroundColor Yellow
Write-Host ""

# Backend en ventana separada
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "cd '$Root'; Write-Host '[ BACKEND ] Spring Boot' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
) -WindowStyle Normal

# Espera a que el backend arranque
Start-Sleep -Seconds 5

# Frontend en ventana cmd (evita restricciones de ejecución de PowerShell)
Start-Process cmd -ArgumentList @(
    "/k", "cd /d `"$Frontend`" && echo [ FRONTEND ] Vite && npm run dev"
) -WindowStyle Normal

Write-Host "[ok] Ventanas abiertas. Ctrl+C en cada una para detener." -ForegroundColor Green
Write-Host ""
