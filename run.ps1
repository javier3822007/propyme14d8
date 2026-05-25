# ProPyme - Script de compilacion y ejecucion para Windows (PowerShell)
# Requiere JDK 21+ en el PATH
# Uso: clic derecho -> "Ejecutar con PowerShell"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Out       = Join-Path $ScriptDir "out"
$Jar       = Join-Path $ScriptDir "ProPyme.jar"
$Src       = Join-Path $ScriptDir "src\main\java"

Write-Host "============================================"
Write-Host "  Sistema ProPyme Transparente v0.10.0"
Write-Host "============================================"

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: javac no encontrado. Instale JDK 21+"
    Write-Host "Descarga: https://adoptium.net/temurin/releases/?version=21"
    Read-Host "Presione Enter para salir"
    exit 1
}

Write-Host "Compilando fuentes..."
if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null

$files = Get-ChildItem -Path $Src -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

& javac -encoding UTF-8 --release 21 -d $Out $files
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR de compilacion"
    Read-Host "Presione Enter para salir"
    exit 1
}

$manifest = Join-Path $ScriptDir "MANIFEST.MF"
Set-Content -Path $manifest -Value "Manifest-Version: 1.0`nMain-Class: cl.propyme.Main" -Encoding ASCII

Push-Location $Out
& jar cfm $Jar $manifest .
Pop-Location

Write-Host "JAR generado: ProPyme.jar"
Write-Host "Iniciando aplicacion..."
& java -jar $Jar
Read-Host "Presione Enter para salir"
