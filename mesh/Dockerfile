FROM node:24-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY .meshrc.yml .meshrc.yml
COPY openapi openapi

EXPOSE 4000

CMD ["sh", "-c", "echo 'Starting GraphQL Mesh on /graphql'; npm run build && npm run start"]
