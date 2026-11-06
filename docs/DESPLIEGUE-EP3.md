# Despliegue en AWS (EC2) y pruebas de integración - EP3

## 1. Actualizar la EC2

```bash
cd ~/pedidos360
git pull
docker-compose build ms-pedidos ms-inventario ms-analitica ms-kafka-admin
docker-compose up -d
docker-compose ps
```

No usar `docker-compose down -v`. Si la EC2 se queda sin memoria al construir, levantar primero Zookeeper y Kafka
(`docker-compose up -d zookeeper kafka-1 kafka-2 kafka-ui`) y después los servicios nuevos; revisar con `free -m`.

Frontend: `cd pedidos360-frontend && npm install && ng build` o `ng serve` según lo acordado en EP2.

## 2. Security Group

Abrir solo para la IP del equipo: 8087, 8088 y 8090. Kafka UI: `http://34.227.91.156:8090`.

## 3. API Gateway (`v40douxdrf`)

Crear dos rutas hacia la EC2, con el mismo autorizador JWT de las demás:

| Ruta | Destino |
|---|---|
| `ANY /analitica/{proxy+}` | `http://34.227.91.156:8087/analitica/{proxy}` |
| `ANY /kafka/{proxy+}` | `http://34.227.91.156:8088/kafka/{proxy}` |

Desplegar el stage. Con eso el frontend (inicio y `/analitica`) consume ambos servicios con token.

## 4. Pruebas de integración (con capturas para el documento de evidencias)

| # | Prueba | Evidencia |
|---|---|---|
| 1 | `docker-compose ps` con todos los contenedores Up | Captura de la terminal |
| 2 | Login con Azure AD en el frontend | Captura del inicio con el nombre del usuario |
| 3 | Llamada con token a `/analitica/resumen` por el API Gateway (Postman) y la misma sin token (401) | Dos capturas |
| 4 | Crear y pagar un pedido en el frontend | Captura de "Mis pedidos" |
| 5 | Kafka UI: Topics -> `pedidos.eventos` -> Messages con los eventos del pedido | Captura |
| 6 | Kafka UI: Consumers -> `analitica-ventas` Stable, lag 0 | Captura |
| 7 | Kafka UI: Brokers (2 brokers, métricas JMX) y Topics (particiones, réplicas) | Captura |
| 8 | Frontend: "Los más vendidos" en el inicio y página `/analitica` | Captura |
| 9 | Alerta de stock: provocar `STOCK_BAJO` y verla en `inventario.alertas` y en `/analitica` | Captura |
| 10 | DLT: `bash kafka/scripts/probar-dlt.sh` y mensaje en `pedidos.eventos.DLT` | Captura |
| 11 | Administrador: crear un tópico con `POST /kafka/topics` y verlo en Kafka UI | Captura |
| 12 | Dashboard RabbitMQ (`:15672`) con las colas y el clúster de 2 nodos | Captura |
| 13 | Caída de un broker (`docker stop kafka-2`): la compra sigue funcionando; luego `docker start kafka-2` | Captura de Kafka UI |
| 14 | `mvn test` en los 4 módulos Kafka con resultado BUILD SUCCESS | Captura |

Las mismas pruebas desde el frontend publicado en Azure (si el equipo lo despliega allí) cubren el requisito "AWS y Azure".

## 5. Entrega

- Repositorios públicos en GitHub (Settings -> General -> Change visibility). Revisar que no hay claves ni `package-lock.json` en el commit.
- Documento de evidencias: tabla anterior con captura y breve explicación por prueba, más el contenido de `docs/KAFKA.md` (casos de uso, modelos JSON, configuración).
- Encargo y presentación (guion sugerido, 8 a 10 minutos): arquitectura (RabbitMQ vs Kafka y por qué ambos) -> casos de uso -> modelo de mensajes -> demo en vivo (pedido, Kafka UI, `/analitica`) -> manejo de errores/DLT -> administración -> conclusiones.
