# syntax=docker/dockerfile:1.7

FROM node:24.19.0-alpine AS build

WORKDIR /workspace

COPY acg17-ui/package.json acg17-ui/package-lock.json ./

RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY acg17-ui/ ./

RUN npm run build

FROM nginx:1.28-alpine AS runtime

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --spider http://127.0.0.1/ || exit 1

