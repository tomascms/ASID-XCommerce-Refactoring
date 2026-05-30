# XCommerce — Avaliação Experimental

Este repositório contém o monólito XCommerce e a sua proposta de decomposição em microserviços, com os **scripts e dados experimentais** que sustentam a Secção 6 do relatório.

## Como reproduzir os testes

### Pré-requisitos

- **Docker** (Compose v2)
- **k6** (`brew install k6` ou ver [k6.io](https://k6.io))
- **Python 3** (para parsing de resultados)
- **curl**

### Pipeline completa (recomendado)

```bash
./run-all-tests.sh
```

Este comando:
1. Sobe o monólito (`01-monolith/`) e os microserviços (`02-microservices/`)
2. Espera 45 segundos para garantir que tudo está saudável
3. Executa, em sequência, os quatro grupos de testes:
   - **H1** — Análise estática de dependências
   - **H2a** — Testes de velocidade (k6) em ambas as arquiteturas
   - **H2b** — Captura de recursos (`docker stats`) em idle e sob carga
   - **H3** — Cenário de falha controlada nas duas arquiteturas
4. Imprime o resumo final dos resultados

**Tempo total:** cerca de 15 minutos.

### Correr testes individualmente

Cada teste tem um script próprio. Útil para reproduzir apenas uma das experiências.

| Hipótese | Monólito | Microserviços |
|---|---|---|
| **H1** Dependências | `./analise-dependencias.sh` (análise única) | |
| **H2a** Velocidade | `01-monolith/testes/testes-velocidade.sh` | `02-microservices/testes/testes-velocidade.sh` |
| **H2b** Recursos | `01-monolith/testes/testes-recursos.sh` | `02-microservices/testes/testes-recursos.sh` |
| **H3** Falha | `01-monolith/testes/testes-falha.sh` | `02-microservices/testes/testes-falha.sh` |

### Apenas subir as stacks

```bash
cd 01-monolith && docker compose up -d
cd 02-microservices && docker compose up -d
```

## O que cada teste faz

### H1 — Análise estática de dependências

Conta dependências cross-domain em ambas as arquiteturas (foreign keys, entidades partilhadas, repositórios cruzados, chamadas REST, tópicos Kafka, propagação de identidade). Não corre nada — analisa código.

### H2a — Velocidade

Dois cenários, executados nas duas arquiteturas:

- **T1** — `GET /products` com 10 utilizadores virtuais durante 100 segundos. Mede o custo de uma operação simples (1 hop síncrono).
- **T2** — Checkout completo com 1 utilizador virtual durante 100 segundos. Mede o custo de uma operação que coordena múltiplos serviços (5 hops síncronos).

Resultados em ficheiros JSON com mediana, p95 e *throughput*.

### H2b — Recursos

Captura `docker stats` em dois momentos: estado ocioso e durante carga sustentada. Mede consumo de RAM e CPU de cada contentor.

### H3 — Falha controlada

- **Microserviços:** parar `inventory-service`, fazer um checkout, verificar que a encomenda fica em estado `HANDLING` para sempre (estado *zombie*).
- **Monólito:** parar a base de dados, tentar checkout, verificar que o motor JPA faz *rollback* automático e o sistema regressa a estado consistente.

## Onde ficam os resultados

```
01-monolith/testes/resultados/
├── t-mono-1.json          ← T1 leitura, números k6
├── t-mono-2.json          ← T2 checkout, números k6
├── recursos-idle.txt      ← docker stats em idle
├── recursos-carga.txt     ← docker stats sob carga
└── falha-bd.txt           ← rollback ACID

02-microservices/testes/k6/output/
├── t-micro-1.json
├── t-micro-2.json
├── recursos-idle.txt
├── recursos-carga.txt
├── zombie-order.txt       ← order presa em HANDLING
├── zombie-stock.txt       ← stock antes/depois
└── zombie-kafka.txt       ← consumer group sem membros

H1-dependencias.txt        ← análise de acoplamento (raiz)
```

## Observabilidade durante os testes

Os microserviços expõem três interfaces de observabilidade úteis para tirar capturas de ecrã durante os testes:

| Ferramenta | URL | Para quê |
|---|---|---|
| **Grafana** | <http://localhost:3000> | Dashboards de latência e recursos (login: `admin` / `admin`) |
| **Prometheus** | <http://localhost:9090> | Métricas em tempo real |
| **Jaeger** | <http://localhost:16686> | *Traces* das chamadas REST entre serviços |
| **Kafdrop** | <http://localhost:9001> | Tópicos Kafka, mensagens, consumer groups |

Recomendam-se as seguintes capturas para a Secção 6 do relatório:

- **Para H2a (velocidade):**
  - Jaeger: *trace* de um checkout mostrando os 5 *spans* das chamadas REST em série
  - Grafana: gráfico de p95 ao longo do tempo durante T2
- **Para H2b (recursos):**
  - Grafana: painel de uso de memória dos vários serviços sob carga
- **Para H3 (estado zombie):**
  - Kafdrop: vista do tópico `order-created-events` com *lag* não consumido
  - Captura do `psql` a mostrar order em `HANDLING`

## Estrutura do repositório

```
.
├── 01-monolith/                   # Monólito Spring Boot
│   ├── docker-compose.yml
│   ├── xcommerce-monolithic-master/   # Código-fonte
│   └── testes/
│       ├── testes-velocidade.sh
│       ├── testes-recursos.sh
│       ├── testes-falha.sh
│       ├── t-mono-1-leitura.js
│       ├── t-mono-2-checkout.js
│       └── resultados/            # Saída dos testes
│
├── 02-microservices/              # Decomposição em microserviços
│   ├── docker-compose.yml
│   ├── identity-service/          # 6 serviços + gateway
│   ├── catalog-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── inventory-service/
│   ├── notification-service/
│   ├── api-gateway/
│   └── testes/
│       ├── testes-velocidade.sh
│       ├── testes-recursos.sh
│       ├── testes-falha.sh
│       └── k6/
│           ├── t-micro-1-leitura.js
│           ├── t-micro-2-checkout.js
│           └── output/            # Saída dos testes
│
├── analise-dependencias.sh        # Análise estática (H1)
├── run-all-tests.sh               # Pipeline completa
└── README.md                      # Este ficheiro
```

## Notas

- Os testes de velocidade correm **sem *think time*** entre iterações para saturar a arquitetura e medir o seu comportamento sob pressão sustentada. Esta é uma metodologia mais conservadora que carga moderada — qualquer diferença observada sob saturação manifestar-se-á também sob carga real.
- A taxa de erro reportada deve ser sempre 0% se o setup estiver correto. Erros indicam problema de configuração ou recurso (BD vazia, stock esgotado).
- O cenário de falha (H3) reinicia automaticamente o serviço parado no fim, deixando o sistema utilizável.
