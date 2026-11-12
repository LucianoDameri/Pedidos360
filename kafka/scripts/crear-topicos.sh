#!/usr/bin/env bash
# Crea (o actualiza) los topicos de Pedidos360. Es idempotente: se puede ejecutar muchas veces.
# Uso (desde la raiz del proyecto, con el docker-compose levantado):  bash kafka/scripts/crear-topicos.sh
#
# Los valores son los mismos que declaran los microservicios (application.yml) y docs/KAFKA.md.
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"

kt()  { docker exec "$BROKER_CONTAINER" kafka-topics  --bootstrap-server "$BOOTSTRAP" "$@"; }
kcf() { docker exec "$BROKER_CONTAINER" kafka-configs --bootstrap-server "$BOOTSTRAP" "$@"; }

# crear_topico <nombre> <particiones> <replicas> <retention.ms> <cleanup.policy> <min.insync.replicas>
crear_topico() {
  local nombre="$1" particiones="$2" replicas="$3" retencion="$4" cleanup="$5" minisr="$6"
  echo ">> $nombre (particiones=$particiones, replicas=$replicas, retencion=${retencion}ms, cleanup=$cleanup)"
  # kafka-configs exige corchetes cuando el valor es una lista: cleanup.policy=[compact,delete]
  if [[ "$cleanup" == *,* ]]; then cleanup="[$cleanup]"; fi
  kt --create --if-not-exists --topic "$nombre" --partitions "$particiones" --replication-factor "$replicas"
  kcf --alter --entity-type topics --entity-name "$nombre" \
      --add-config "retention.ms=${retencion},cleanup.policy=${cleanup},min.insync.replicas=${minisr}"
}

#                nombre                    part  repl  retention.ms   cleanup         min.isr
crear_topico "pedidos.eventos"            3     3     604800000      delete          2   # 7 dias
crear_topico "inventario.alertas"         3     3     2592000000     "compact,delete" 2  # 30 dias, ultimo estado por producto
crear_topico "pedidos.eventos.DLT"        1     3     1209600000     delete          2   # 14 dias
crear_topico "inventario.alertas.DLT"     1     3     1209600000     delete          2   # 14 dias

echo
echo "Topicos actuales:"
kt --list
