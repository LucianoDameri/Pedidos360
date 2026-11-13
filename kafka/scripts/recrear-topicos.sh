#!/usr/bin/env bash
# Borra y vuelve a crear los topicos de Pedidos360 con la configuracion de crear-topicos.sh
# (3 particiones y 3 replicas en los dos topicos principales, min.insync.replicas = 2).
#
# USAR CON LOS MICROSERVICIOS DETENIDOS (ms-pedidos, ms-inventario, ms-analitica y ms-kafka-admin).
# Los mensajes de estos topicos se pierden: en el proyecto son solo datos de prueba.
# Uso (desde la raiz del proyecto):  bash kafka/scripts/recrear-topicos.sh
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"
TOPICOS=(pedidos.eventos inventario.alertas pedidos.eventos.DLT inventario.alertas.DLT)
GRUPOS=(analitica-ventas analitica-reposicion)
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kt()  { docker exec "$BROKER_CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP" "$@"; }
kcg() { docker exec "$BROKER_CONTAINER" kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" "$@"; }

echo "== 1. Comprobando que los 3 brokers estan activos"
brokers=$(docker exec "$BROKER_CONTAINER" kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" 2>/dev/null | grep -c "(id: " || true)
echo "   brokers activos: $brokers"
if [ "$brokers" -lt 3 ]; then
  echo "ERROR: se esperaban 3 brokers. Levanta kafka-3 primero:  docker-compose up -d kafka-3"
  exit 1
fi

echo "== 2. Borrando consumer groups"
for g in "${GRUPOS[@]}"; do
  kcg --delete --group "$g" || echo "   (no se pudo borrar $g: no existia o aun tiene miembros)"
done

echo "== 3. Borrando topicos"
for t in "${TOPICOS[@]}"; do
  kt --delete --if-exists --topic "$t"
done
echo "   Esperando a que Kafka termine de borrarlos..."
restantes=""
for i in $(seq 1 30); do
  restantes=$(kt --list | grep -x -F -f <(printf '%s\n' "${TOPICOS[@]}") || true)
  [ -z "$restantes" ] && break
  sleep 2
done
if [ -n "$restantes" ]; then
  echo "ERROR: siguen existiendo: $restantes"
  exit 1
fi

echo "== 4. Creando topicos"
bash "$DIR/crear-topicos.sh"

echo "== 5. Resultado (PartitionCount / ReplicationFactor)"
for t in pedidos.eventos inventario.alertas; do
  kt --describe --topic "$t" | sed -n 1p
done
