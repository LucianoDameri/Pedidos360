#!/usr/bin/env bash
# Inspecciona los topicos: particiones, lider, replicas, ISR y configuracion efectiva.
# Uso: bash kafka/scripts/describir-topicos.sh [nombre-de-topico]
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"

kt()  { docker exec "$BROKER_CONTAINER" kafka-topics  --bootstrap-server "$BOOTSTRAP" "$@"; }
kcf() { docker exec "$BROKER_CONTAINER" kafka-configs --bootstrap-server "$BOOTSTRAP" "$@"; }

if [ $# -ge 1 ]; then
  TOPICOS=("$1")
else
  mapfile -t TOPICOS < <(kt --list | tr -d '\r' | grep -v '^__')
fi

for t in "${TOPICOS[@]}"; do
  echo "=============================== $t"
  kt  --describe --topic "$t"
  kcf --describe --entity-type topics --entity-name "$t"
done
