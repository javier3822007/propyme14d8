#!/bin/bash
# ProPyme - Script de compilación y ejecución
# Requiere JDK 21+ instalado (https://adoptium.net)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$SCRIPT_DIR/out"
JAR="$SCRIPT_DIR/ProPyme.jar"

echo "============================================"
echo "  ProPyme Transparente — AT 2026"
echo "============================================"

# Check java
if ! command -v javac &>/dev/null; then
    echo "ERROR: javac no encontrado. Instale JDK 21+"
    echo "Descarga: https://adoptium.net/temurin/releases/?version=21"
    exit 1
fi

JAVA_VER=$(javac -version 2>&1 | grep -oP '\d+' | head -1)
echo "Java detectado: versión $JAVA_VER"

if [ ! -f "$JAR" ]; then
    echo "Compilando fuentes..."
    mkdir -p "$OUT"
    find "$SCRIPT_DIR/src" -name "*.java" > "$SCRIPT_DIR/sources.txt"
    javac -encoding UTF-8 --release 21 -d "$OUT" @"$SCRIPT_DIR/sources.txt"
    if [ $? -ne 0 ]; then echo "ERROR de compilación"; exit 1; fi
    cat > "$SCRIPT_DIR/MANIFEST.MF" << 'MFEOF'
Manifest-Version: 1.0
Main-Class: cl.propyme.Main
MFEOF
    cd "$OUT" && jar cfm "$JAR" "$SCRIPT_DIR/MANIFEST.MF" .
    echo "JAR generado: ProPyme.jar"
fi

echo "Iniciando aplicación..."
java -jar "$JAR"
