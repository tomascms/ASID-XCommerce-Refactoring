# 04-QI — Testes Experimentais XCommerce

Testes de suporte à Avaliação Experimental (Secção 6 do relatório).
Cobrem H2a (latência), H2b (recursos) e H3 (falha parcial/consistência).

---

## Pré-requisitos

- Docker Desktop em execução
- k6 instalado (`brew install k6` ou https://k6.io/docs/get-started/installation/)
- psql instalado (`brew install libpq`)
- As duas arquiteturas **não devem correr em simultâneo** (conflito de recursos)

---

## Passo 2 — H2a: Latência sob carga

Três cenários × três níveis de carga × duas arquiteturas = 54 corridas.
Duração total estimada: ~4h (corridas de 3 min cada, automatizadas).

```bash
# Monolito (com monolito a correr + seed feito)
./04-QI/monolitico/h2a/correr-h2a.sh

# Microserviços (depois de fazer docker compose down no monolito)
docker compose -f 01-monolith/docker-compose.yml down
docker compose -f 02-microservices/docker-compose.yml up -d
./04-QI/microserviços/h2a/correr-h2a.sh
```

Resultados em:
- `monolitico/h2a/resultados/*.json`
- `microserviços/h2a/resultados/*.json`

### Cenários

| ID | Endpoint | Hops mono | Hops micro | Hipótese |
|----|----------|-----------|------------|----------|
| T1 | GET /products | 1 | 1 (gateway→catalog) | baseline |
| T2 | GET /rest/order/list | 1 (3+2N queries) | 1 (só IDs) | W2 / N+1 |
| T3 | POST /rest/order/checkout | 1 (transação JPA) | 5 + Kafka | H2a central |

---

## Passo 3 — H2b: Recursos em idle

Não precisa de seed. Mede RAM e CPU com os serviços em repouso.

```bash
# Monolito — arrancar, esperar 5 min, capturar
docker compose -f 01-monolith/docker-compose.yml up -d
sleep 300
./04-QI/monolitico/h2b/capturar-h2b.sh mono 1

# Microserviços — idem
docker compose -f 01-monolith/docker-compose.yml down
docker compose -f 02-microservices/docker-compose.yml up -d
sleep 300
./04-QI/microserviços/h2b/capturar-h2b.sh micro 1
```

Repetir 3 vezes (run 1, 2, 3) e usar a mediana. Resultados em `*/h2b/resultados/*.csv`.

---

## Passo 4 — H3: Falha parcial e consistência

### Cenário A — Estado zombie (microserviços)

Demonstra que sem Saga, a encomenda fica em `HANDLING` quando o inventory-service está parado.

```bash
# Microserviços a correr + seed feito
./04-QI/microserviços/h3/cenario-zombie.sh
```

Evidências geradas em `microserviços/h3/resultados/`:
- `zombie-orders.txt` — encomenda em HANDLING
- `zombie-inventory-antes.txt` / `zombie-inventory-depois.txt` — stock inalterado
- `zombie-kafka-lag.txt` — lag = 1, 0 consumers ativos
- `zombie-cart.txt` — carrinho vazio (irreversível)

Após o teste: `docker start xcommerce-inventory-service`

### Cenário B — Rollback ACID (monolito)

Demonstra que a transação JPA faz rollback completo quando a BD falha a meio.

```bash
# Monolito a correr + seed feito
./04-QI/monolitico/h3/cenario-rollback-mono.sh
```

Evidências em `monolitico/h3/resultados/`:
- `rollback-antes-orders.txt` / `rollback-depois-orders.txt` — contagens iguais
- `rollback-antes-stock.txt` / `rollback-depois-stock.txt` — stock inalterado
- `rollback-cart.txt` — carrinho intacto

---

## Passo 5 — Análise e tabelas

Preencher com os resultados obtidos:

- `analise/h2a.md` — tabela p50/p95/p99 por cenário, carga e arquitetura
- `analise/h2b.md` — tabela RAM/CPU idle por contentor e total
- `analise/h3.md` — tabela estado observado vs esperado (confirma H3)

---

## Estrutura

```
04-QI/
├── seed/
│   ├── seed-monolito.sh          passo 1 para o monolito
│   └── seed-microservicos.sh     passo 1 para os microserviços
├── analise/                      tabelas e interpretação (preencher após testes)
├── monolitico/
│   ├── h2a/   t1-catalogo.js, t2-encomendas.js, t3-checkout.js, correr-h2a.sh
│   ├── h2b/   capturar-h2b.sh
│   └── h3/    cenario-rollback-mono.sh
└── microserviços/
    ├── h2a/   t1-catalogo.js, t2-encomendas.js, t3-checkout.js, correr-h2a.sh
    ├── h2b/   capturar-h2b.sh
    └── h3/    cenario-zombie.sh
```
