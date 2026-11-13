# Pedidos360

Sistema de gestión de pedidos con microservicios Spring Boot, frontend Angular, autenticación con Azure AD,
API Gateway en AWS y mensajería asíncrona con RabbitMQ y Kafka.

## Arquitectura

| Componente | Puerto | Función |
|---|---|---|
| `pedidos360-frontend` | 4200 | Angular + MSAL (login con Azure AD, JWT en cada llamada) |
| `ms-bff` | 8080 | Backend for Frontend |
| `ms-clientes` | 8081 | Clientes; consume `notificaciones.queue` |
| `ms-productos` | 8082 | Catálogo (productos, ofertas, fotos) |
| `ms-pedidos` | 8083 | Pedidos; publica `pedido.*` y consume `tickets.pago.queue` |
| `ms-inventario` | 8084 | Stock; consume `inventario.stock.queue` |
| `ms-pagos` | 8085 | Pagos; publica `pago.aprobado` |
| `ms-rabbit-admin` | 8086 | Administrador de RabbitMQ (colas, exchanges y bindings por REST) |
| `ms-analitica` | 8087 | Consume `pedidos.eventos` e `inventario.alertas` (Kafka); entrega ventas y alertas |
| `ms-kafka-admin` | 8088 | Administrador de Kafka (tópicos, consumer groups, mensajes por REST) |
| MySQL | 3306 | Una base de datos por microservicio |
| RabbitMQ | 5672 / 15672, 5673 / 15673 | Clúster de 2 nodos (`rabbitmq-1`, `rabbitmq-2`) |
| Kafka | 9092, 9093, 9094 | 3 brokers (`kafka-1`, `kafka-2`, `kafka-3`) y ensamble de 3 Zookeeper |
| Kafka UI | 8090 | Tópicos, mensajes, consumer groups y métricas |

Todas las llamadas del frontend pasan por el API Gateway de AWS, que valida el JWT; los microservicios lo validan de nuevo.

## Mensajería (RabbitMQ)

| Exchange | Tipo |
|---|---|
| `pedidos.exchange` | topic |
| `pagos.exchange` | topic |
| `dlx.exchange` | direct |

| Cola | Binding | Consumidor | DLQ |
|---|---|---|---|
| `notificaciones.queue` | `pedido.*` (pedidos) y `pago.aprobado` (pagos) | ms-clientes | `notificaciones.dlq` |
| `inventario.stock.queue` | `pedido.*` (pedidos) | ms-inventario | `inventario.dlq` |
| `tickets.pago.queue` | `pago.aprobado` (pagos) | ms-pedidos | `tickets.dlq` |

- ACK manual (`acknowledge-mode: manual`, `prefetch: 1`). Error no recuperable: NACK sin requeue y el mensaje va a la DLQ.
  Error recuperable: un reintento y, si vuelve a fallar, DLQ. Cada rechazo queda en el log con la marca `[RABBIT][DLQ]`.
- Las DLQ conservan los mensajes 24 horas.
- Los nombres de colas, exchanges y routing keys están en el `application.yml` de cada servicio (bloque `app.messaging`),
  y los beans `Queue`, `Exchange` y `Binding` en `config/RabbitConfig`.

Detalle completo, API del administrador y pruebas: [docs/RABBITMQ.md](docs/RABBITMQ.md).
Colección de Postman: `docs/Pedidos360-RabbitMQ.postman_collection.json`.

## Streaming de eventos (Kafka)

| Tópico | Productor | Consumidor (grupo) |
|---|---|---|
| `pedidos.eventos` (3 particiones, 3 réplicas, clave `pedidoId`) | ms-pedidos | ms-analitica (`analitica-ventas`) |
| `inventario.alertas` (3 particiones, clave `productoId`) | ms-inventario | ms-analitica (`analitica-reposicion`) |

Cada tópico tiene su DLT (`<tópico>.DLT`). Casos de uso, modelo de mensajes JSON, parámetros, errores y pruebas:
[docs/KAFKA.md](docs/KAFKA.md). Scripts de administración: `kafka/scripts/`. Postman: `docs/Pedidos360-Kafka.postman_collection.json`.

## Cómo levantarlo

```bash
docker-compose up --build -d     # servicios, MySQL y el clúster RabbitMQ
docker-compose ps                # todos "Up"; rabbitmq y kafka en "healthy"
bash kafka/scripts/crear-topicos.sh   # opcional: los servicios ya crean los tópicos al arrancar
```

Kafka UI: `http://<IP-EC2>:8090` (usuario y clave en el servicio `kafka-ui` del compose). Con Kafka el stack necesita unos 5 GB de RAM:
en una EC2 pequeña conviene agregar swap o levantar por partes. Nunca usar `docker-compose down -v` (borra las bases de datos).

Los dos nodos comparten la cookie de Erlang (`RABBITMQ_ERLANG_COOKIE`) y se unen por la lista fija de
`rabbitmq/rabbitmq.conf`. La política de alta disponibilidad (`ha-pedidos360`) la crea sola el servicio de un solo uso `rabbitmq-politica`. Para aplicarla a mano o comprobarla:

```bash
docker exec rabbitmq-1 rabbitmqctl set_policy ha-pedidos360 "^(notificaciones|inventario|tickets)\." '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

Dashboard: `http://<IP-EC2>:15672` (nodo 1) y `:15673` (nodo 2). Frontend: `cd pedidos360-frontend && npm install && ng serve`.

## Probarlo en cualquier máquina

Hay tres formas, de la más simple a la más completa. En todas el login es Azure AD y los datos vienen
del API Gateway en la nube, por lo que **la EC2 debe estar encendida** (Learner Lab activo).

1. **URL pública (no requiere instalar nada):** `https://34-227-91-156.sslip.io`.
2. **Solo el frontend con Docker** (no requiere Node ni Angular):
   ```bash
   docker-compose -f docker-compose.frontend.yml up -d --build
   ```
   Abrir `http://localhost:4200`. Para detenerlo: `docker-compose -f docker-compose.frontend.yml down`.
3. **Frontend con Node 18+:** `cd pedidos360-frontend && npm ci && npx ng serve` y abrir `http://localhost:4200`.

Importante: los únicos orígenes registrados en Azure AD (Redirect URI) y en el CORS del API Gateway son
`http://localhost:4200` y `https://34-227-91-156.sslip.io`. Otro puerto o dominio será rechazado.

El stack completo (backend, MySQL, RabbitMQ y Kafka con `docker-compose up --build -d`) no necesita archivo `.env`
(ver `.env.example`) y requiere una máquina con varios GB de RAM libres.

## Puertos del Security Group (EC2)

Abrir solo para la IP del equipo: 15672 y 15673 (dashboard RabbitMQ), 8086 (administrador RabbitMQ), 8090 (Kafka UI), 8087 y 8088 (analítica y administrador Kafka).
El tráfico del frontend entra por el API Gateway.

## Datos iniciales

Al arrancar con la base vacía, `ms-productos` carga 12 productos y `ms-inventario` su stock
(`CatalogoInicial` y `StockInicial`). Nunca sobrescriben datos existentes. Respaldos y ofertas: carpeta `docs/`.
