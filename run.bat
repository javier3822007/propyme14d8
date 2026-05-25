@echo off
REM ProPyme - Script de compilacion y ejecucion para Windows
REM Requiere JDK 21+ en el PATH

echo ============================================
echo   Sistema ProPyme Transparente v0.10.0
echo ============================================

where javac >nul 2>&1
if errorlevel 1 (
    echo ERROR: javac no encontrado. Instale JDK 21+
    echo Descarga: https://adoptium.net/temurin/releases/?version=21
    pause & exit /b 1
)

REM Delegar compilacion y ejecucion a PowerShell para manejar rutas con espacios
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$base = Split-Path -Parent '%~f0';" ^
  "$src  = Join-Path $base 'src\main\java';" ^
  "$out  = Join-Path $base 'out';" ^
  "$jar  = Join-Path $base 'ProPyme.jar';" ^
  "$mf   = Join-Path $base 'MANIFEST.MF';" ^
  "Write-Host 'Compilando fuentes...';" ^
  "if (Test-Path $out) { Remove-Item $out -Recurse -Force };" ^
  "New-Item -ItemType Directory -Path $out | Out-Null;" ^
  "$files = Get-ChildItem -Path $src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName;" ^
  "& javac -encoding UTF-8 --release 21 -d $out $files;" ^
  "if ($LASTEXITCODE -ne 0) { Write-Host 'ERROR de compilacion'; exit 1 };" ^
  "Set-Content -Path $mf -Value \"Manifest-Version: 1.0`nMain-Class: cl.propyme.Main\" -Encoding ASCII;" ^
  "Push-Location $out;" ^
  "& jar cfm $jar $mf .;" ^
  "Pop-Location;" ^
  "Write-Host 'JAR generado. Iniciando aplicacion...';" ^
  "& java -jar $jar"

pause
