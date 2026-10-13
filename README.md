# Pedidos360

Sistema de gestión de pedidos con microservicios Spring Boot, frontend Angular, autenticación con Azure AD,
API Gateway en AWS y mensajería asíncrona con RabbitMQ.

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
| MySQL | 3306 | Una base de datos por microservicio |
| RabbitMQ | 5672 / 15672, 5673 / 15673 | Clúster de 2 nodos (`rabbitmq-1`, `rabbitmq-2`) |

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

## Cómo levantarlo

```bash
docker-compose up --build -d     # servicios, MySQL y el clúster RabbitMQ
docker-compose ps                # todos "Up"; los rabbitmq en "healthy"
```

Los dos nodos comparten la cookie de Erlang (`RABBITMQ_ERLANG_COOKIE`) y se unen por la lista fija de
`rabbitmq/rabbitmq.conf`. Con el clúster arriba, se aplica una vez la política de alta disponibilidad:

```bash
docker exec rabbitmq-1 rabbitmqctl set_policy ha-pedidos360 "^(notificaciones|inventario|tickets)\." '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

Dashboard: `http://<IP-EC2>:15672` (nodo 1) y `:15673` (nodo 2). Frontend: `cd pedidos360-frontend && npm install && ng serve`.

## Puertos del Security Group (EC2)

Abrir solo para la IP del equipo: 15672 y 15673 (dashboard RabbitMQ) y 8086 (administrador).
El tráfico del frontend entra por el API Gateway.

## Datos iniciales

Al arrancar con la base vacía, `ms-productos` carga 12 productos y `ms-inventario` su stock
(`CatalogoInicial` y `StockInicial`). Nunca sobrescriben datos existentes. Respaldos y ofertas: carpeta `docs/`.
