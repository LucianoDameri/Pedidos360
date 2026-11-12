# Kafka en Pedidos360 (EP3)

Kafka complementa a RabbitMQ. RabbitMQ sigue encargándose de las **tareas** entre servicios (notificar, descontar stock,
cerrar el ticket de pago). Kafka se usa para **eventos que se guardan y se pueden volver a leer**: alimentan la analítica
de ventas y la reposición de inventario sin que los servicios de negocio se enteren de quién los consume.

## 1. Casos de uso

| # | Caso de uso | Tópico | Productor | Consumidor |
|---|---|---|---|---|
| CU1 | **Analítica de ventas.** Cada cambio de estado de un pedido (creado, pagado, cancelado...) se publica como evento. `ms-analitica` acumula por producto las unidades vendidas, los pedidos pagados y los ingresos, y entrega los "más vendidos" que muestra el inicio del frontend. | `pedidos.eventos` | `ms-pedidos` | `ms-analitica` (grupo `analitica-ventas`) |
| CU2 | **Alertas de reposición.** Cuando una venta deja el stock de un producto en su mínimo o menos, `ms-inventario` publica una alerta. `ms-analitica` la guarda para que quien repone vea qué comprar. | `inventario.alertas` | `ms-inventario` | `ms-analitica` (grupo `analitica-reposicion`) |
| CU3 | **Administración del clúster.** `ms-kafka-admin` crea tópicos, cambia su configuración, aumenta particiones, consulta consumer groups y su lag, y publica mensajes de prueba. | todos | - | - |

Un fallo de Kafka nunca rompe una compra: los productores publican de forma asíncrona y, si falla, solo dejan el error en el log.

## 2. Modelo de mensajes (JSON)

Los mensajes son JSON sin cabeceras de tipo Java: productor y consumidor tienen cada uno su propio DTO, así que el contrato es solo este JSON.
Todos llevan `eventId` (UUID, el consumidor lo usa para no procesar dos veces el mismo evento) y `version` (hoy 1).

### 2.1 Tópico `pedidos.eventos` (clave: `pedidoId`)

La clave garantiza que todos los eventos de un mismo pedido caen en la misma partición y se leen en orden.

Entrada (lo que publica `ms-pedidos`):

```json
{
  "eventId": "6f1c2b7e-0d5a-4c1e-9a2e-3b7d8f10a111",
  "tipo": "PEDIDO_CONFIRMADO",
  "version": 1,
  "pedidoId": 12,
  "clienteId": 3,
  "productoId": 7,
  "cantidad": 2,
  "total": 159980,
  "estado": "CONFIRMADO",
  "ocurridoEn": "2026-11-02T14:35:10"
}
```

`tipo`: `PEDIDO_CREADO`, `PEDIDO_CONFIRMADO`, `PEDIDO_EN_PREPARACION`, `PEDIDO_EN_CAMINO`, `PEDIDO_ENTREGADO`, `PEDIDO_CANCELADO`.

Salida (lo que entrega `ms-analitica` en `GET /analitica/ventas/top?limite=5`):

```json
[
  { "productoId": 7, "pedidosCreados": 4, "pedidosConfirmados": 3, "pedidosCancelados": 1,
    "unidadesVendidas": 6, "ingresos": 479940, "actualizadoEn": "2026-11-02T14:35:11" }
]
```

### 2.2 Tópico `inventario.alertas` (clave: `productoId`)

Entrada (lo que publica `ms-inventario`):

```json
{
  "eventId": "a41d9e55-7c3b-4f02-8d61-5e2f0c9b7a22",
  "tipo": "STOCK_BAJO",
  "version": 1,
  "productoId": 7,
  "cantidadActual": 3,
  "stockMinimo": 5,
  "ocurridoEn": "2026-11-02T14:36:02"
}
```

`tipo`: `STOCK_BAJO` (cantidad <= stockMinimo) o `STOCK_AGOTADO` (cantidad = 0).

Salida (`GET /analitica/alertas?limite=10`):

```json
[
  { "id": 15, "eventId": "a41d9e55-...", "productoId": 7, "tipo": "STOCK_BAJO",
    "cantidadActual": 3, "stockMinimo": 5, "ocurridoEn": "2026-11-02T14:36:02",
    "recibidoEn": "2026-11-02T14:36:02" }
]
```

### 2.3 Resumen (`GET /analitica/resumen`)

```json
{ "pedidosCreados": 10, "pedidosConfirmados": 7, "pedidosCancelados": 1, "unidadesVendidas": 15,
  "ingresos": 1250000, "alertasStock": 3, "productosConAlerta": 2 }
```

## 3. Configuración de los tópicos

| Tópico | Particiones | Réplicas | Retención | Cleanup | min.insync.replicas |
|---|---|---|---|---|---|
| `pedidos.eventos` | 3 | 3 | 7 días | delete | 2 |
| `inventario.alertas` | 3 | 3 | 30 días | compact,delete | 2 |
| `pedidos.eventos.DLT` | 1 | 3 | 14 días | delete | 2 |
| `inventario.alertas.DLT` | 1 | 3 | 14 días | delete | 2 |

- Los tópicos se crean al arrancar los servicios (beans `NewTopic`) y también con `bash kafka/scripts/crear-topicos.sh`. Ambos usan los mismos valores.
- Para cambiar las réplicas de un tópico que ya existe: detener los servicios que usan Kafka y ejecutar `bash kafka/scripts/recrear-topicos.sh` (borra y vuelve a crear los tópicos).
- El clúster tiene `auto.create.topics.enable=false`: un tópico mal escrito falla en vez de crearse solo.
- `inventario.alertas` es `compact,delete`: Kafka conserva al menos el último mensaje por producto.
- Productores: `acks=all` e idempotencia activa. Con 3 brokers, 3 réplicas y `min.insync.replicas=2` el sistema sigue leyendo y escribiendo si cae un broker.

## 4. Consumer groups

| Grupo | Tópico | Hilos | Listener |
|---|---|---|---|
| `analitica-ventas` | `pedidos.eventos` | 3 (una por partición) | `PedidoEventoListener` |
| `analitica-reposicion` | `inventario.alertas` | 2 (una por partición) | `AlertaStockListener` |

Asignación `CooperativeStickyAssignor`. Confirmación de offsets manual inmediata (`MANUAL_IMMEDIATE`): solo se confirma un mensaje cuando se procesó (o se envió a la DLT).
Los `@KafkaListener` solo reciben el mensaje y lo delegan a `VentasService` / `AlertasService`; ahí está la lógica y el control de duplicados por `eventId`.

## 5. Manejo de errores y DLT

1. JSON roto o con campos inválidos: `ErrorHandlingDeserializer` / `EventoInvalidoException` -> directo a `<tópico>.DLT`, sin reintentos.
2. Error recuperable (por ejemplo la base de datos caída): 3 reintentos con 1 segundo de pausa; luego a la DLT.
3. Cada envío a la DLT queda en el log con la marca `[KAFKA][DLT]` y el consumidor sigue con el siguiente mensaje.

Prueba rápida: `bash kafka/scripts/probar-dlt.sh` publica un mensaje inválido en `pedidos.eventos` y muestra cómo llega a `pedidos.eventos.DLT`.

## 6. Servicios y puertos

| Componente | Puerto | Función |
|---|---|---|
| Zookeeper | 2181 (interno) | Coordinación del clúster |
| `kafka-1` | 9092 | Broker 1 (listener externo para el host) |
| `kafka-2` | 9093 | Broker 2 |
| `kafka-3` | 9094 | Broker 3 |
| Kafka UI | 8090 | Tópicos, mensajes, consumer groups y métricas (JMX) |
| `ms-analitica` | 8087 | Consumidor + API de analítica |
| `ms-kafka-admin` | 8088 | Administrador de Kafka por REST |

Kafka UI pide usuario y contraseña (definidos en el servicio `kafka-ui` de `docker-compose.yml`, variables `SPRING_SECURITY_USER_NAME` / `SPRING_SECURITY_USER_PASSWORD`).

## 7. API (protegida con JWT de Azure AD)

`ms-analitica` (`/analitica`): `GET /resumen`, `GET /ventas/productos`, `GET /ventas/top?limite=`, `GET /alertas?limite=`.

`ms-kafka-admin` (`/kafka`):

| Método | Ruta | Función |
|---|---|---|
| GET | `/cluster` | Brokers y controlador |
| GET / POST | `/topics` | Listar / crear (`name`, `partitions`, `replicationFactor`, `retentionMs`, `cleanupPolicy`, `minInsyncReplicas`, `maxMessageBytes`) |
| GET / DELETE | `/topics/{name}` | Detalle (particiones, líder, ISR) / eliminar |
| PUT | `/topics/{name}/config` | Cambiar retención, cleanup, minISR, tamaño máximo |
| PUT | `/topics/{name}/partitions` | Aumentar particiones (Kafka no permite reducirlas) |
| GET / DELETE | `/consumer-groups[/{groupId}]` | Listar / detalle con offsets y lag / eliminar (solo si está vacío) |
| POST | `/messages` | Publicar mensaje de prueba (`topic`, `key`, `payload`) |

Colección de Postman: `docs/Pedidos360-Kafka.postman_collection.json`.

## 8. Cómo probar

1. `docker-compose up -d` y `docker-compose ps` (todo "Up", los brokers "healthy").
2. `bash kafka/scripts/crear-topicos.sh` y `bash kafka/scripts/describir-topicos.sh`.
3. Desde el frontend, crear un pedido y pagarlo. En Kafka UI (`:8090`) -> Topics -> `pedidos.eventos` -> Messages aparecen `PEDIDO_CREADO` y `PEDIDO_CONFIRMADO`.
4. En Kafka UI -> Consumers: `analitica-ventas` en estado Stable y lag 0.
5. En el inicio del frontend aparece "Los más vendidos"; en `/analitica` están los totales, las alertas y el estado de Kafka.
6. Bajar el stock de un producto hasta su mínimo (o agotarlo con compras): llega un evento a `inventario.alertas` y aparece en `/analitica`.
7. `bash kafka/scripts/probar-dlt.sh` para ver la DLT.
8. `bash kafka/scripts/grupos-consumidores.sh` para ver grupos y lag desde consola.
9. Pruebas unitarias: `mvn test` dentro de `ms-pedidos`, `ms-inventario`, `ms-analitica` y `ms-kafka-admin`.
