# Frontend Angular compilado dentro de Docker (no hace falta Node en la maquina).
# Se sirve con Caddy en el puerto 80 del contenedor.
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund
COPY . .
RUN npx ng build

FROM caddy:2-alpine
COPY Caddyfile.local /etc/caddy/Caddyfile
COPY --from=build /app/dist/pedidos360-frontend/browser /srv
