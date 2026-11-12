#!/usr/bin/env bash
# Lista los consumer groups y muestra offsets, lag y miembros de cada uno.
# Uso: bash kafka/scripts/grupos-consumidores.sh [group-id]
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"

kcg() { docker exec "$BROKER_CONTAINER" kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" "$@"; }

if [ $# -ge 1 ]; then
  GRUPOS=("$1")
else
  mapfile -t GRUPOS < <(kcg --list | tr -d '\r' | sed '/^$/d')
fi

for g in "${GRUPOS[@]}"; do
  echo "=============================== grupo: $g"
  kcg --describe --group "$g"
  echo "--- miembros"
  kcg --describe --group "$g" --members --verbose
done
