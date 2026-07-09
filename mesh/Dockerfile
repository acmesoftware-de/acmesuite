FROM node:20-alpine

WORKDIR /app

COPY package.json package.json
RUN npm install --no-audit --no-fund

COPY .meshrc.yml .meshrc.yml
COPY openapi openapi

EXPOSE 4000

CMD ["sh", "-c", "echo 'Starting GraphQL Mesh on /graphql'; npm run build && npm run start"]
