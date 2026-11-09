#!/bin/bash
set -e

# Detectar el directorio donde está este script (portable, funciona desde cualquier ruta)
BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$BASE/src/main/java"
OUT="$BASE/out"
JAR="$BASE/ProPyme.jar"
MANIFEST="$BASE/MANIFEST.MF"

echo "=== Limpiando build anterior ==="
rm -rf "$OUT" && mkdir -p "$OUT"

echo "=== Compilando fuentes ==="
# Cada ruta entre comillas: javac separa el archivo @ por espacios
find "$SRC" -name "*.java" | sed 's/.*/"&"/' > "$BASE/sources.txt"
javac -encoding UTF-8 -source 21 -target 21 -d "$OUT" @"$BASE/sources.txt"
echo "Compilación OK"

echo "=== Empaquetando JAR ==="
cat > "$MANIFEST" << 'EOF'
Manifest-Version: 1.0
Main-Class: cl.propyme.Main
EOF

cd "$OUT"
jar cfm "$JAR" "$MANIFEST" .
echo "JAR generado: $JAR"
echo "Tamaño: $(du -sh "$JAR" | cut -f1)"

echo "=== Listo ==="
