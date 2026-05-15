# ─────────────────────────────────────────────
#  MarketS — Setup inicial (correr una sola vez)
#  Uso: .\setup.ps1
# ─────────────────────────────────────────────

$Root     = $PSScriptRoot
$Frontend = Join-Path $Root "frontend"
$Uploads  = "C:\uploads\hana"

Write-Host ""
Write-Host "===  MarketS Setup  ===" -ForegroundColor Cyan
Write-Host ""

# 1. Carpeta de uploads
Write-Host "[1/2] Verificando carpeta de uploads..." -ForegroundColor Yellow
if (-not (Test-Path $Uploads)) {
    New-Item -ItemType Directory -Force $Uploads | Out-Null
    Write-Host "      Creada: $Uploads" -ForegroundColor Green
} else {
    Write-Host "      Ya existe: $Uploads" -ForegroundColor Gray
}

# 2. Dependencias npm del frontend
Write-Host "[2/2] Instalando dependencias npm del frontend..." -ForegroundColor Yellow
Push-Location $Frontend
npm install
Pop-Location

Write-Host ""
Write-Host "[ok] Setup completado. Ya puedes ejecutar .\start-dev.ps1" -ForegroundColor Green
Write-Host ""
