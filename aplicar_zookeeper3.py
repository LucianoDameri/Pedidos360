#!/usr/bin/env python3
"""Convierte el Zookeeper unico en un ensamble de 3 nodos (idempotente).

Ejecutar desde la raiz del repo:  python aplicar_zookeeper3.py
- docker-compose.yml: zookeeper pasa a ser el nodo 1; agrega zookeeper-2 y zookeeper-3,
  volumenes propios y KAFKA_ZOOKEEPER_CONNECT con los 3 nodos.
- kafka/scripts/estado-zookeeper.sh: muestra el rol (leader/follower) de cada nodo.
- kafka/scripts/recrear-topicos.sh: arregla el 'head -1' que cortaba la salida.
- README.md y docs/KAFKA.md: menciones del ensamble.
"""
import re
import sys
from pathlib import Path

ZK_SERVERS = "zookeeper:2888:3888;zookeeper-2:2888:3888;zookeeper-3:2888:3888"
CONNECT_OLD = "KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181"
CONNECT_NEW = "KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181,zookeeper-2:2181,zookeeper-3:2181"


def leer(p):
    # Normaliza CRLF -> LF (el repo guarda LF; asi el diff de git solo muestra los cambios reales)
    return Path(p).read_bytes().decode("utf-8").replace("\r\n", "\n")


def escribir(p, txt):
    Path(p).write_bytes(txt.encode("utf-8"))


def nodo(n):
    return f"""  zookeeper-{n}:
    image: confluentinc/cp-zookeeper:7.5.3
    container_name: zookeeper-{n}
    hostname: zookeeper-{n}
    restart: always
    environment:
      ZOOKEEPER_SERVER_ID: {n}
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
      ZOOKEEPER_INIT_LIMIT: 10
      ZOOKEEPER_SYNC_LIMIT: 5
      ZOOKEEPER_SERVERS: "{ZK_SERVERS}"
      KAFKA_HEAP_OPTS: "-Xms128m -Xmx256m"
    volumes:
      - zookeeper_data_{n}:/var/lib/zookeeper/data
      - zookeeper_log_{n}:/var/lib/zookeeper/log
    healthcheck:
      test: ["CMD-SHELL", "echo srvr | nc localhost 2181 | grep -q Mode"]
      interval: 15s
      timeout: 10s
      retries: 10
      start_period: 20s
    networks:
      - pedidos360-net

"""


def compose():
    p = "docker-compose.yml"
    t = leer(p)
    if "zookeeper-2:" in t and "ZOOKEEPER_SERVERS" in t:
        print("docker-compose.yml: ya aplicado")
        return
    # 1) nodo 1 = servicio zookeeper existente (conserva volumenes y datos)
    viejo = "      ZOOKEEPER_CLIENT_PORT: 2181\n      ZOOKEEPER_TICK_TIME: 2000\n      KAFKA_HEAP_OPTS"
    nuevo = ("      ZOOKEEPER_SERVER_ID: 1\n      ZOOKEEPER_CLIENT_PORT: 2181\n      ZOOKEEPER_TICK_TIME: 2000\n"
             "      ZOOKEEPER_INIT_LIMIT: 10\n      ZOOKEEPER_SYNC_LIMIT: 5\n"
             f"      ZOOKEEPER_SERVERS: \"{ZK_SERVERS}\"\n      KAFKA_HEAP_OPTS")
    if t.count(viejo) != 1:
        sys.exit("No encontre el bloque del servicio zookeeper")
    t = t.replace(viejo, nuevo)

    # 2) nodos 2 y 3 justo antes de kafka-1
    marca = "  kafka-1:\n    image: confluentinc/cp-kafka"
    if t.count(marca) != 1:
        sys.exit("No encontre el servicio kafka-1")
    t = t.replace(marca, nodo(2) + nodo(3) + marca)

    # 3) brokers apuntan a los 3 nodos
    if t.count(CONNECT_OLD + "\n") != 3:
        sys.exit("Esperaba 3 lineas KAFKA_ZOOKEEPER_CONNECT")
    t = t.replace(CONNECT_OLD + "\n", CONNECT_NEW + "\n")

    # 4) volumenes
    vol = "  zookeeper_log:\n"
    if t.count(vol) != 1:
        sys.exit("No encontre los volumenes de zookeeper")
    t = t.replace(vol, vol + "  zookeeper_data_2:\n  zookeeper_log_2:\n  zookeeper_data_3:\n  zookeeper_log_3:\n")

    # 5) comentario de cabecera
    t = t.replace("KAFKA (EP3): Zookeeper + 2 brokers + Kafka UI.",
                  "KAFKA (EP3): ensamble de 3 Zookeeper + 3 brokers + Kafka UI.")
    escribir(p, t)
    print("docker-compose.yml: OK")


def script_estado():
    p = Path("kafka/scripts/estado-zookeeper.sh")
    if p.exists():
        print("estado-zookeeper.sh: ya existe")
        return
    escribir(p, """#!/usr/bin/env bash
# Muestra el rol de cada nodo del ensamble Zookeeper (1 leader + 2 follower) y los brokers registrados.
# Uso: bash kafka/scripts/estado-zookeeper.sh
set -uo pipefail

for z in zookeeper zookeeper-2 zookeeper-3; do
  modo=$(docker exec "$z" bash -c 'echo srvr | nc localhost 2181 | grep Mode' 2>/dev/null | tr -d '\\r')
  echo "$z -> ${modo:-sin respuesta}"
done

echo
echo "Brokers registrados en Zookeeper (ids):"
docker exec zookeeper zookeeper-shell localhost:2181 ls /brokers/ids 2>/dev/null | tail -1
""")
    print("estado-zookeeper.sh: creado")


def recrear():
    p = "kafka/scripts/recrear-topicos.sh"
    t = leer(p)
    n = t.replace('--describe --topic "$t" | head -1', '--describe --topic "$t" | sed -n 1p')
    if n == t:
        print("recrear-topicos.sh: sin cambios (ya corregido o distinto)")
    else:
        escribir(p, n)
        print("recrear-topicos.sh: OK")


def docs():
    p = "README.md"
    t = leer(p)
    t2 = t.replace("3 brokers (`kafka-1`, `kafka-2`, `kafka-3`) con Zookeeper",
                   "3 brokers (`kafka-1`, `kafka-2`, `kafka-3`) y ensamble de 3 Zookeeper")
    if t2 != t:
        escribir(p, t2)
        print("README.md: OK")
    p = "docs/KAFKA.md"
    t = leer(p)
    t2 = t.replace("| Zookeeper | 2181 (interno) | Coordinación del clúster |",
                   "| Zookeeper (3 nodos: `zookeeper`, `zookeeper-2`, `zookeeper-3`) | 2181 (interno) | Coordinación del clúster; quórum de 2 de 3, tolera la caída de 1 nodo |")
    if t2 != t:
        escribir(p, t2)
        print("docs/KAFKA.md: OK")
    p = "docs/DESPLIEGUE-EP3.md"
    t = leer(p)
    t2 = t.replace("(`docker-compose up -d zookeeper kafka-1 kafka-2 kafka-3 kafka-ui`)",
                   "(`docker-compose up -d zookeeper zookeeper-2 zookeeper-3 kafka-1 kafka-2 kafka-3 kafka-ui`)")
    if t2 != t:
        escribir(p, t2)
        print("docs/DESPLIEGUE-EP3.md: OK")


if __name__ == "__main__":
    if not Path("docker-compose.yml").exists():
        sys.exit("Ejecuta este script desde la raiz del repo (donde esta docker-compose.yml)")
    compose()
    script_estado()
    recrear()
    docs()
