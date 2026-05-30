#!/bin/bash

CONTAINER_NAME="monolithic-container"
IMAGE_NAME="final_app"

echo "### 1. Parando o container da aplicação..."
docker stop $CONTAINER_NAME 2>/dev/null || true

echo "### 2. Removendo o container da aplicação..."
docker rm $CONTAINER_NAME 2>/dev/null || true

echo "### 3. Removendo a imagem da aplicação..."
docker rmi $IMAGE_NAME 2>/dev/null || true

echo "### 4. Buildando a aplicação com Gradle..."
./gradlew build -x test

echo "### 5. Inicializando o novo container..."
docker-compose up -d --build app

echo "### Script finalizado com sucesso!"
