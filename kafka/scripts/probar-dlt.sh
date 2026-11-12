#!/usr/bin/env bash
# Demuestra el manejo de errores: publica un mensaje MALFORMADO en pedidos.eventos.
# ms-analitica no puede leerlo, lo reintenta segun la politica y lo envia a pedidos.eventos.DLT.
#
# Uso:
#   bash kafka/scripts/probar-dlt.sh          # publica el mensaje invalido
#   bash kafka/scripts/probar-dlt.sh leer     # lee lo que hay en la DLT
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"

if [ "${1:-}" = "leer" ]; then
  echo ">> Mensajes en pedidos.eventos.DLT (Ctrl+C para salir)"
  docker exec "$BROKER_CONTAINER" kafka-console-consumer --bootstrap-server "$BOOTSTRAP" \
    --topic pedidos.eventos.DLT --from-beginning --property print.key=true --property print.headers=true \
    --timeout-ms 10000
  exit 0
fi

echo ">> Publicando un JSON invalido en pedidos.eventos"
echo 'prueba-dlt|{"esto no es un evento valido"' | docker exec -i "$BROKER_CONTAINER" kafka-console-producer \
  --bootstrap-server "$BOOTSTRAP" --topic pedidos.eventos \
  --property parse.key=true --property key.separator='|'

echo ">> Listo. Revisa el log de ms-analitica:  docker logs --since 2m ms-analitica | grep KAFKA"
echo ">> Luego lee la DLT:  bash kafka/scripts/probar-dlt.sh leer"
