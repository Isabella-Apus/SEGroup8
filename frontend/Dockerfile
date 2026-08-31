FROM node:22.14.0-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . ./
RUN npm run build:real

FROM nginx:1.27.4-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/proxy_params /etc/nginx/proxy_params
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1/health || exit 1
