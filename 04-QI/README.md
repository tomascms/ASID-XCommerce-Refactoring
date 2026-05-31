# 04-QI — Testes Experimentais XCommerce

Testes de suporte à Avaliação Experimental (Secção 6 do relatório).
Cobrem H2 (latência + recursos) e H3 (falha parcial/consistência).

---

## Pré-requisitos

- Docker Desktop em execução
- k6 instalado (`brew install k6` ou https://k6.io/docs/get-started/installation/)
- As duas arquiteturas **não devem correr em simultâneo** (conflito de recursos)

---

## Passo 1 — Seed de dados

Popula as BDs com utilizadores, produtos, stock e encomendas de teste.  
**Executar depois de os serviços estarem healthy** (`docker compose ps`).

```bash
# Microserviços
cd 02-microservices && docker compose up -d
# aguardar ~2 min até todos healthy
bash 04-QI/seed/seed-microservicos.sh

# Monolito
cd 01-monolith && docker compose up -d
# aguardar healthcheck
bash 04-QI/seed/seed-monolito.sh
```

O que é criado:

| BD / Sistema | Dados |
|---|---|
| xcommerce_identity (micro) | 50 utilizadores `user1`..`user50`, password = `password` |
| xcommerce_catalog (micro) | 1 categoria + 1 marca + 20 produtos `Produto Teste 1..20` |
| xcommerce_inventory (micro) | stock 1000 unidades para os 20 produtos |
| xcommerce_orders (micro) | 250 encomendas CONFIRMED (5 × 50 utilizadores), 4 itens cada |
| xcommerce / monolito | idem — utilizadores, produtos (id 2601-2620), stock, encomendas |

O seed é **idempotente**: pode ser re-executado sem erros.

---

## Passo 2 — H2: Latência sob carga (k6)

Dois cenários × duas arquiteturas.  
Carga: 10 VUs fixos, 3 min, 2 runs por cenário.

```bash
# Microserviços (com microserviços a correr + seed feito)
bash 04-QI/microserviços/h2/correr-h2a.sh

# Monolito (depois de fazer down nos microserviços)
docker compose -f 02-microservices/docker-compose.yml down
cd 01-monolith && docker compose up -d
bash 04-QI/seed/seed-monolito.sh
bash 04-QI/monolitico/h2/correr-h2a.sh
```

Resultados em:
- `monolitico/h2/resultados/*.json`
- `microserviços/h2/resultados/*.json`

### Cenários

| ID | Endpoint Micro | Endpoint Mono | Descrição |
|----|---------------|--------------|-----------|
| T1 | GET /products | GET /rest/shoppingCart/get | leitura leve (baseline) |
| T3 | POST /rest/order/checkout | GET /rest/order/checkout | checkout transacional |

---

## Passo 3 — H3: Falha parcial e consistência

### Cenário A — Estado zombie (microserviços)

Demonstra que sem Saga, a encomenda fica em `HANDLING` quando o inventory-service está parado.

```bash
# Microserviços a correr + seed feito
bash 04-QI/microserviços/h3/cenario-zombie.sh
```

Evidências em `microserviços/h3/resultados/`:
- `zombie-orders.txt` — encomenda em HANDLING
- `zombie-inventory-antes.txt` / `zombie-inventory-depois.txt` — stock inalterado
- `zombie-kafka-lag.txt` — lag = 1, 0 consumers ativos
- `zombie-cart.txt` — carrinho vazio (irreversível)

Após o teste: `docker start xcommerce-inventory-service`

### Cenário B — Rollback ACID (monolito)

Demonstra que a transação JPA faz rollback completo quando a BD falha a meio.

```bash
# Monolito a correr + seed feito
bash 04-QI/monolitico/h3/cenario-rollback-mono.sh
```

Evidências em `monolitico/h3/resultados/`:
- `rollback-antes-orders.txt` / `rollback-depois-orders.txt` — contagens iguais
- `rollback-antes-stock.txt` / `rollback-depois-stock.txt` — stock inalterado
- `rollback-cart.txt` — carrinho intacto

---

## Passo 4 — H4: Recursos em idle

Não precisa de seed. Mede RAM e CPU com os serviços em repouso.

```bash
# Microserviços — arrancar, esperar 5 min, capturar
docker compose -f 02-microservices/docker-compose.yml up -d
sleep 300
bash 04-QI/microserviços/h4/capturar-h2b.sh micro 1

# Monolito — idem
docker compose -f 02-microservices/docker-compose.yml down
cd 01-monolith && docker compose up -d
sleep 300
bash 04-QI/monolitico/h4/capturar-h2b.sh mono 1
```

Repetir 3 vezes (run 1, 2, 3) e usar a mediana. Resultados em `*/h4/resultados/*.csv`.

---

## Estrutura

```
04-QI/
├── seed/
│   ├── seed-monolito.sh          passo 1 para o monolito
│   └── seed-microservicos.sh     passo 1 para os microserviços
├── monolitico/
│   ├── h1/   analise-dependencias.sh + resultados/
│   ├── h2/   t1-catalogo.js, t3-checkout.js, correr-h2a.sh + resultados/
│   ├── h3/   cenario-rollback-mono.sh + resultados/
│   └── h4/   capturar-h2b.sh + resultados/
└── microserviços/
    ├── h1/   analise-dependencias.sh + resultados/
    ├── h2/   t1-catalogo.js, t3-checkout.js, correr-h2a.sh
    ├── h3/   cenario-zombie.sh
    └── h4/   capturar-h2b.sh
```
