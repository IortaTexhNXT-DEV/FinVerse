# syntax=docker/dockerfile:1.7
# ---- build stage -------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---- runtime stage: static files served by nginx, /api proxied to the backend -------
FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
# The nginx image renders templates with envsubst (only variables defined in the environment).
COPY nginx.conf /etc/nginx/templates/default.conf.template
ENV FINVERSE_BACKEND_HOST=backend
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:8080/healthz || exit 1
