# syntax=docker/dockerfile:1.7
# Base images are pinned by digest (multi-architecture index), so a rebuild uses the same bytes until
# the pin is refreshed on purpose. Refresh (monthly, and for every base image security fix):
#   docker buildx imagetools inspect node:22-alpine                        | grep -m1 Digest
#   docker buildx imagetools inspect nginxinc/nginx-unprivileged:1.30-alpine | grep -m1 Digest
# then replace the sha256 values below, rebuild, and let the pipeline's image scan pass.
# ---- build stage -------------------------------------------------------------------
FROM node:26-alpine@sha256:0b36e8c136b94cd4fcf02188228e76c31ad5872eef3fec8cbd2eee500cfd9e80 AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
# The dependency audit runs as a gate in the pipeline (npm audit --audit-level=high).
RUN npm ci --no-audit --no-fund
COPY . .
# Theme pack of the client deployment (src/theme/packs/<pack>): branding, colours, fonts, logo.
ARG VITE_THEME_PACK=bdoi
ENV VITE_THEME_PACK=${VITE_THEME_PACK}
RUN npm run build

# ---- runtime stage: static files served by an unprivileged nginx, /api proxied to the backend ----
# nginx runs as uid/gid 101 (never root), listens on 8080 (plain HTTP, docker compose) or 8443 (TLS,
# Kubernetes configuration of deploy/nginx) and writes only below /tmp, /var/cache/nginx and
# /etc/nginx/conf.d, so the container also runs with a read-only root file system.
FROM nginxinc/nginx-unprivileged:1.30-alpine@sha256:4714e0b1b2577eaa1a6131d07c958b67f0eb68e6d0521e90c6e5287db8cf0bc5
# Security fixes of the Alpine packages published after the pinned base image (the image scan fails
# on a fixed high or critical vulnerability otherwise).
USER root
RUN apk upgrade --no-cache
COPY --from=build /workspace/dist /usr/share/nginx/html
# The nginx image renders templates with envsubst (only variables defined in the environment).
COPY nginx.conf /etc/nginx/templates/default.conf.template
ENV BROKERVERSE_BACKEND_HOST=backend
# The entrypoint reads the container's resolvers into NGINX_LOCAL_RESOLVERS (used by nginx.conf).
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
USER 101:101
EXPOSE 8080 8443
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:8080/healthz || exit 1
