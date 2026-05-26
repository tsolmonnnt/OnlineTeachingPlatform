# syntax=docker/dockerfile:1.7
# ---------- Stage 1: build the Vite/React bundle ----------
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Empty by default so production builds use relative URLs (same-origin via Nginx).
# Override at build time:  docker build --build-arg VITE_API_BASE_URL=https://example.com ...
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ---------- Stage 2: serve the static bundle ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
