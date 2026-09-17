FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY angular.json tsconfig*.json ./
COPY public ./public
COPY src ./src
RUN npm run build -- --configuration production,docker

FROM nginx:1.28-alpine AS runtime
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/api-proxy.conf /etc/nginx/api-proxy.conf
COPY --from=build /app/dist/frontend/browser/ /usr/share/nginx/html/
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
