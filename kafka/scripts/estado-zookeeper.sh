#!/usr/bin/env bash
# Muestra el rol de cada nodo del ensamble Zookeeper (1 leader + 2 follower) y los brokers registrados.
# Uso: bash kafka/scripts/estado-zookeeper.sh
set -uo pipefail

for z in zookeeper zookeeper-2 zookeeper-3; do
  modo=$(docker exec "$z" bash -c 'echo srvr | nc localhost 2181 | grep Mode' 2>/dev/null | tr -d '\r')
  echo "$z -> ${modo:-sin respuesta}"
done

echo
echo "Brokers registrados en Zookeeper (ids):"
docker exec zookeeper zookeeper-shell localhost:2181 ls /brokers/ids 2>/dev/null | tail -1
