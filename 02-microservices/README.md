# XCommerce — Microservices (ASID 2025/2026)

Refactoring do monólito XCommerce para uma arquitetura de microserviços com Spring Boot, Spring Cloud Gateway, Keycloak, Kafka, PostgreSQL e observabilidade (Jaeger, Prometheus, Grafana).

Este README descreve **como subir todo o sistema do zero** num PC sem nada instalado a não ser o Docker.

---

## 1. Pré-requisitos

| Ferramenta | Versão mínima | Como verificar |
|---|---|---|
| Docker Desktop (ou Docker Engine + Compose v2) | 24.x | `docker --version` e `docker compose version` |
| Git (apenas para clonar) | qualquer | `git --version` |
| RAM livre | ~6 GB | — |
| Portas livres no host | 3000, 5432, 8080, 8081–8088, 9000, 9001, 9090, 9092, 16686 | — |

> Não é preciso instalar Java, Maven, Node ou Postgres no host. Tudo corre dentro de containers.

---

## 2. Obter o código

```bash
git clone <URL-do-repositorio>
cd <pasta-do-repositorio>/02-microservices
```

Confirma que estás na pasta certa: tem de existir o ficheiro `docker-compose.yml`.

```bash
ls docker-compose.yml
```

---

## 3. Subir todo o sistema (1 comando)

```bash
docker compose up -d --build
```

O que acontece:
- O Docker faz build das imagens dos 10 microserviços em Java (~3–6 min na primeira vez, dependendo da rede e do CPU).
- Sobe a infraestrutura base: Postgres, Zookeeper, Kafka, Keycloak.
- Sobe a observabilidade: Jaeger, Prometheus, Grafana, Kafdrop.
- Sobe os microserviços de negócio + gateway.

> Na primeira execução, o Maven dentro de cada imagem faz download das dependências. As builds seguintes usam cache de camadas e são muito mais rápidas.

---

## 4. Verificar que tudo está saudável

Espera ~60–90 segundos depois do build acabar e corre:

```bash
docker compose ps
```

Todos os serviços devem aparecer com `STATUS = Up (healthy)` ou `Up`. Se algum aparecer como `unhealthy`, espera mais 30s e volta a correr — o Keycloak e os serviços com migrations Flyway podem demorar mais a ficar prontos.

Verificação rápida do gateway:

```bash
curl -s http://localhost:9000/actuator/health
```

Deve devolver `{"status":"UP"}`.

---

## 5. Aceder à aplicação

| O quê | URL | Notas |
|---|---|---|
| API Gateway | http://localhost:9000 | Entrada única para todos os endpoints REST |
| Keycloak Admin | http://localhost:8080 | `admin` / `admin` |
| Kafdrop (Kafka UI) | http://localhost:9001 | Ver tópicos e mensagens |
| Jaeger (tracing) | http://localhost:16686 | Pesquisar traces distribuídos |
| Prometheus | http://localhost:9090 | Métricas |
| Grafana | http://localhost:3000 | `admin` / `admin` (na 1ª entrada) |

### Credenciais pré-seedadas pela aplicação

| Username | Password | Role |
|---|---|---|
| `admin` | `123456` | SUPERADMIN |

(Existem mais utilizadores semeados; este é o suficiente para a demo.)

---

## 6. Demo guiada (5 minutos)

> **Importante:** num arranque do zero o catálogo está vazio. Começa pelo passo de admin (passo 8) para criar marca, categoria e pelo menos um produto, e só depois faz o fluxo cliente.

Use os endpoints REST diretamente via API Gateway (`http://localhost:9000`) ou via o ficheiro `xcommerce-api-tests.http`.

1. **Autenticar** — `POST /auth/login` com `admin` / `123456` → obtém JWT.
2. **Ver produtos** — `GET /catalog/products`.
3. **Adicionar ao carrinho** — `POST /cart/items`.
4. **Finalizar compra** — `POST /order/checkout`.
6. Em **Jaeger** (http://localhost:16686) vê os traces distribuídos.
7. **Admin** (usar header `X-User-Role: SUPERADMIN`):
   - Criar marca, categoria, produto novo.
   - Listar utilizadores.

---

## 7. Parar o sistema

Parar mantendo os dados:
```bash
docker compose stop
```

Voltar a arrancar:
```bash
docker compose start
```

---

## 8. Limpar tudo (estado virgem)

```bash
docker compose down -v --rmi local
```

Apaga containers, volumes (BD) e imagens construídas localmente. Próximo `docker compose up -d --build` recomeça do zero.

---

## 9. Resolução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| `port is already allocated` | Outro processo a usar a porta (5432 do Postgres é o suspeito habitual) | Para o serviço local: `brew services stop postgresql` (macOS) ou alterar a porta no `docker-compose.yml`. |
| Serviços a ficar `unhealthy` | Ainda a inicializar (Flyway, Keycloak) | Esperar 60–90s e correr `docker compose ps` outra vez. Se persistir: `docker compose logs <serviço> --tail 100`. |
| Build muito lenta na 1ª vez | Maven a baixar dependências | Normal. Builds seguintes são ~10× mais rápidas. |
| Web UI mostra 502/503 | O gateway ou um serviço dependente ainda não subiu | Esperar e refrescar. |
| Quero ver logs de um serviço | — | `docker compose logs -f <nome-do-serviço>` (ex.: `order-service`, `gateway`). |

---

## 10. Arquitetura (resumo)

```
                       ┌──────────────────┐
                       │    gateway       │  :9000  (Spring Cloud Gateway + JWT)
                       └──────┬───────────┘
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
      auth-service     catalog-service     order-service
      cart-service     inventory-service   payment-service
      user-service     notification-service
                              │
                  ┌───────────┼───────────┐
                  ▼           ▼           ▼
              Postgres      Kafka     Keycloak
                              │
                              ▼
                  Jaeger / Prometheus / Grafana
```

- **Gateway** valida JWT e injeta `X-User-Name` / `X-User-Role` para os serviços downstream.
- **Eventos**: orders e payments publicam em Kafka; `notification-service` consome.
- **Cada serviço tem a sua BD lógica** (database-per-service) na mesma instância Postgres.
