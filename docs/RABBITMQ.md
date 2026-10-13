# Mensajería asíncrona con RabbitMQ – Pedidos360

Los microservicios siguen funcionando igual que antes (HTTP + JWT de Azure AD). RabbitMQ se
agrega **al lado** para las tareas que no necesitan responder en el momento: notificar al
cliente, alertar stock bajo y generar el ticket de un pedido pagado. Si RabbitMQ se cae, la
compra y el pago **no fallan**: el productor solo registra el error en el log.

## 1. Infraestructura

| Componente | Detalle |
|---|---|
| Cluster | 2 nodos `rabbitmq-1` y `rabbitmq-2` (`rabbitmq:3.12-management-alpine`), misma cookie de Erlang |
| Config del cluster | `rabbitmq/rabbitmq.conf` (peer discovery `classic_config`) |
| AMQP | nodo 1 `:5672`, nodo 2 `:5673` |
| Dashboard | nodo 1 `http://<IP-EC2>:15672`, nodo 2 `http://<IP-EC2>:15673` (usuario `admin` / `admin123`) |
| Conexión de los servicios | `RABBITMQ_ADDRESSES=rabbitmq-1:5672,rabbitmq-2:5672` (si un nodo cae, usan el otro) |
| Administrador | `ms-rabbit-admin` en `:8086` (API REST protegida con JWT) |

## 2. Diseño: 3 exchanges, 3 colas principales y 3 DLQ

| Exchange | Tipo | Quién publica |
|---|---|---|
| `pedidos.exchange` | topic | ms-pedidos (`pedido.creado`, `pedido.cancelado`) |
| `pagos.exchange` | topic | ms-pagos (`pago.aprobado`) |
| `dlx.exchange` | direct | RabbitMQ (mensajes rechazados) |

| Cola principal | Binding | Consumidor | Qué hace |
|---|---|---|---|
| `notificaciones.queue` | `pedidos.exchange` ← `pedido.*` y `pagos.exchange` ← `pago.aprobado` | ms-clientes | Notifica al cliente el pedido y el pago (simula correo) |
| `inventario.stock.queue` | `pedidos.exchange` ← `pedido.*` | ms-inventario | Alerta si el stock quedó bajo el mínimo |
| `tickets.pago.queue` | `pagos.exchange` ← `pago.aprobado` | ms-pedidos | Genera el ticket del pedido pagado |

| DLQ | Binding en `dlx.exchange` | Recibe de |
|---|---|---|
| `notificaciones.dlq` | `notificaciones.dead` | `notificaciones.queue` |
| `inventario.dlq` | `inventario.dead` | `inventario.stock.queue` |
| `tickets.dlq` | `tickets.dead` | `tickets.pago.queue` |

```
ms-pedidos --pedido.creado/cancelado--> pedidos.exchange --pedido.*--> notificaciones.queue (ms-clientes)
                                                          \--pedido.*--> inventario.stock.queue (ms-inventario)
ms-pagos   --pago.aprobado-----------> pagos.exchange   --pago.aprobado--> notificaciones.queue (ms-clientes)
                                                          \--pago.aprobado--> tickets.pago.queue (ms-pedidos)
cola principal --(NACK sin requeue)--> dlx.exchange --> *.dlq
```

**Retención:** los mensajes de cada DLQ expiran a las 24 horas (`x-message-ttl`, valor `app.messaging.retention.dlq-ttl-ms`
en `application.yml`), de modo que la DLQ conserva un historial para analizar los fallos sin crecer sin límite.

> Si ya habías levantado una versión anterior (con `notificaciones.pedido.queue` y `notificaciones.pago.queue`, o DLQ sin TTL),
> RabbitMQ rechaza redeclarar una cola con argumentos distintos. Bórralas antes (dashboard → Queues → *Delete*, o
> `DELETE /queues/{name}` del administrador) y reinicia los microservicios.

## 3. Dónde está cada cosa en el código

| Concepto | Dónde |
|---|---|
| Nombres centralizados | `application.yml` de cada servicio, bloque `app.messaging` (+ clase `config/MessagingProperties`) |
| Beans `Queue`, `Exchange`, `Binding` | `config/RabbitConfig` de cada servicio |
| Productores | `messaging/publisher/PedidoEventPublisher` (ms-pedidos), `PagoEventPublisher` (ms-pagos) |
| Consumidores (`@RabbitListener`) | `messaging/listener/*` agrupados por dominio: `NotificacionListener`, `StockListener`, `TicketListener` |
| Lógica de negocio (sin RabbitMQ) | `service/NotificacionService`, `AlertaStockService`, `TicketService` |
| Administrador | `ms-rabbit-admin`: `controller` → `service/RabbitAdminService` → `RabbitAdmin` |

## 4. ACK manual y manejo de errores (consumidores)

`spring.rabbitmq.listener.simple.acknowledge-mode: manual` y `prefetch: 1`.

| Situación | Decisión |
|---|---|
| Se procesó bien | `basicAck` |
| Error **no recuperable** (JSON inválido, evento sin datos, cliente/pedido/stock inexistente) | `basicNack(requeue=false)` → DLX → DLQ, y se escribe `[RABBIT][DLQ]` en el log |
| Error **recuperable** (por ejemplo la BD no responde) | 1 reintento (`basicNack(requeue=true)`); si falla otra vez (`redelivered`) → `basicNack(requeue=false)` → DLQ |

## 5. API del administrador (`ms-rabbit-admin`, puerto 8086)

Todas las rutas requieren `Authorization: Bearer <token de Azure AD>`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/queues` | Crea una cola (`name`, `durable`, `deadLetterExchange`, `deadLetterRoutingKey`, `messageTtlMs`, `maxLength`) |
| GET | `/queues/{name}` | Mensajes y consumidores de la cola (404 si no existe) |
| DELETE | `/queues/{name}` | Elimina la cola |
| POST | `/queues/{name}/purge` | Vacía la cola |
| POST | `/exchanges` | Crea un exchange (`name`, `type`: DIRECT/TOPIC/FANOUT/HEADERS, `durable`) |
| DELETE | `/exchanges/{name}` | Elimina el exchange |
| POST | `/bindings` | Crea un binding (`queue`, `exchange`, `routingKey`) |
| DELETE | `/bindings?queue=&exchange=&routingKey=` | Elimina el binding |
| POST | `/messages` | Publica un mensaje de prueba (`exchange`, `routingKey`, `payload`) |

Validaciones: nombre obligatorio, solo `A-Z a-z 0-9 . _ -` (máx. 255) y sin prefijo `amq.`;
tipo de exchange obligatorio; TTL y largo máximo > 0; `deadLetterRoutingKey` exige `deadLetterExchange`.
Errores en JSON `{"error": "..."}` con 400 (datos inválidos), 404 (no existe), 409 (RabbitMQ rechaza)
o 503 (sin conexión con RabbitMQ).

Rutas del API Gateway a agregar (integración HTTP a `http://<IP-EC2>:8086/...`, mismo authorizer JWT):
`ANY /queues`, `ANY /queues/{proxy+}`, `ANY /exchanges`, `ANY /exchanges/{proxy+}`, `ANY /bindings`, `ANY /messages`.

## 6. Despliegue en la EC2

```bash
cd ~/Pedidos360
git pull
docker-compose down            # evita el error 'ContainerConfig' de docker-compose 1.29
docker-compose up --build -d
docker-compose ps              # todos deben quedar "Up" (los rabbitmq en "healthy")
```

Una sola vez, para que las colas se repliquen en los 2 nodos:

```bash
docker exec rabbitmq-1 rabbitmqctl set_policy ha-pedidos360 "^(notificaciones|inventario|tickets)\." '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

En el Security Group de la EC2 abre (solo para tu IP) los puertos **15672**, **15673** y **8086**.
La EC2 necesita memoria para 7 servicios Java + MySQL + 2 nodos RabbitMQ: si se queda lenta, usa
al menos una instancia de 4 GB.

## 7. Pruebas funcionales

1. **Cluster:** `docker exec rabbitmq-1 rabbitmqctl cluster_status` muestra los 2 nodos; el dashboard (Overview → Nodes) muestra ambos.
2. **Topología:** dashboard → Exchanges (3) y Queues (3 + 3 DLQ, con `DLX` en las principales y `TTL` en las DLQ).
3. **Flujo feliz:** en la tienda, compra un producto →
   `docker-compose logs ms-pedidos | grep RABBIT` (publicó `PEDIDO_CREADO`),
   `docker-compose logs ms-clientes | grep NOTIFICACION`, `docker-compose logs ms-inventario | grep -E "STOCK"`.
   Luego paga el pedido → `[NOTIFICACION]` del pago en ms-clientes y `[TICKET]` en ms-pedidos.
4. **DLQ por cliente inexistente:** `POST /messages` con
   `{"exchange":"pedidos.exchange","routingKey":"pedido.creado","payload":"{\"tipo\":\"PEDIDO_CREADO\",\"pedidoId\":1,\"clienteId\":999999,\"productoId\":1,\"cantidad\":1}"}`
   → log `[RABBIT][DLQ]` en ms-clientes y 1 mensaje en `notificaciones.dlq`.
5. **DLQ por JSON inválido:** igual pero con `"payload":"esto no es json"` → llega a las DLQ de notificaciones e inventario.
   En el dashboard: Queues → `notificaciones.dlq` → *Get messages* (se ve el header `x-death` con el motivo).
6. **Administrador:** crear cola/exchange/binding y eliminarlos; probar nombre vacío (400) y cola inexistente (404).
7. **Seguridad:** cualquier ruta del administrador sin token responde 401.
8. **Tolerancia a fallos:** `docker stop rabbitmq-1` → compra y paga otra vez (los consumidores se reconectan al nodo 2);
   `docker start rabbitmq-1` para restaurar.
9. **RabbitMQ caído:** detén ambos nodos y crea un pedido: la compra funciona y ms-pedidos registra `No se pudo publicar`.
