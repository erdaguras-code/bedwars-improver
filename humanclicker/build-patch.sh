#!/usr/bin/env bash
# Human Clicker - ClickScheduler yamasini orijinal jar uzerine uygular.
#
# Kullanim: ./build-patch.sh <orijinal-jar> [cikti-jar]
#
# ClickScheduler sinifi Minecraft'a hic dokunmadigi icin (yalnizca ClickerConfig
# kullanir) tam bir Loom/Gradle kurulumuna gerek yoktur: sinif orijinal jar'a
# karsi derlenip jar icinde degistirilir. Boylece mixin'ler ve intermediary
# eslemeleri oldugu gibi korunur.

set -euo pipefail

SRC_JAR="${1:?kullanim: build-patch.sh <orijinal-jar> [cikti-jar]}"
OUT_JAR="${2:-dist/humanclicker-0.1.1.jar}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo ">> derleniyor: ClickScheduler.java"
mkdir -p "$WORK/classes"
javac -nowarn -source 21 -target 21 \
  -cp "$SRC_JAR" \
  -d "$WORK/classes" \
  "$ROOT/src/main/java/com/humanclicker/ClickScheduler.java"

echo ">> jar aciliyor"
mkdir -p "$WORK/jar"
unzip -q "$SRC_JAR" -d "$WORK/jar"

echo ">> sinif degistiriliyor"
cp "$WORK/classes/com/humanclicker/ClickScheduler.class" \
   "$WORK/jar/com/humanclicker/ClickScheduler.class"

echo ">> surum bilgisi guncelleniyor"
python3 - "$WORK/jar/fabric.mod.json" <<'PY'
import json, sys
p = sys.argv[1]
with open(p, encoding="utf-8") as f:
    data = json.load(f)
data["version"] = "0.1.1"
data["description"] = (
    "Sol tus basili tutuldugunda saldiri tiklamasini tekrarlar. Tiklama araliklari "
    "her seferinde bagimsiz ve rastgele secilir, ayarlanan CPS bandinin disina cikmaz. "
    "Nisan bir blokta oldugunda tamamen devre disi kalir, boylece blok kirma mekanigi "
    "hic etkilenmez."
)
with open(p, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
    f.write("\n")
PY

echo ">> jar paketleniyor: $OUT_JAR"
mkdir -p "$(dirname "$ROOT/$OUT_JAR")" 2>/dev/null || true
OUT_ABS="$OUT_JAR"
[[ "$OUT_ABS" = /* ]] || OUT_ABS="$ROOT/$OUT_JAR"
mkdir -p "$(dirname "$OUT_ABS")"
rm -f "$OUT_ABS"
( cd "$WORK/jar" && zip -q -r -X "$OUT_ABS" . )

echo ">> tamam: $OUT_ABS"
