# Plano de Testes — Monólito XCommerce
> Contexto: estes testes servem para recolher dados empíricos para o relatório ASID.
> O monólito corre em http://localhost:18080
> Credenciais: username=root / password=root

---

## O que este plano cobre

| Teste | Tipo | Objetivo no relatório | Secção relatório |
|-------|------|-----------------------|-----------------|
| T-MONO-1 | Performance (k6) | Baseline latência leitura — p50/p95/p99 | §4.1, §5.3 T1 |
| T-MONO-2 | Performance (k6) | Baseline latência checkout — p50/p95/p99 | §4.3, §5.3 T2 |
| T-MONO-3 | Falha total (manual) | Provar que 1 falha derruba tudo — evidência §2.2 | §5.3 T3 |
| T-MONO-4 | N+1 queries (manual) | Log SQL com 11 queries para 5 itens — Fig. 3 | §2.2, §5.4 |
| T-MONO-5 | Recursos (script) | CPU e RAM idle e sob carga | §5.3 T5 |

---

## T-MONO-1 — Latência de Leitura

**Ficheiro:** `t-mono-1-leitura.js`
**Ferramenta:** k6 (já instalado)
**O que mede:** latência de `GET /rest/catalog/products` — 1 query directa à BD, sem lógica de negócio.
**Hipótese a validar:** esta será a baseline para comparar com `GET /products` nos microserviços (Gateway → catalog-service → BD).

**Como correr:**
```bash
# Obter token primeiro:
TOKEN=$(curl -s -X POST http://localhost:18080/rest/user/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' | tr -d '"')

# Correr 3 vezes e guardar CSV:
cd /Users/I769175/ASID-XCommerce-Refactoring-1/01-monolith/testes
k6 run --out csv=resultados/t-mono-1-exec1.csv -e TOKEN=$TOKEN t-mono-1-leitura.js
k6 run --out csv=resultados/t-mono-1-exec2.csv -e TOKEN=$TOKEN t-mono-1-leitura.js
k6 run --out csv=resultados/t-mono-1-exec3.csv -e TOKEN=$TOKEN t-mono-1-leitura.js
```

**O que anotar:** p50, p95, p99, taxa de erro. Média das 3 execuções.

---

## T-MONO-2 — Latência do Checkout

**Ficheiro:** `t-mono-2-checkout.js`
**Ferramenta:** k6
**O que mede:** latência de `GET /rest/order/checkout` — a operação crítica que executa numa única transação JPA: ler utilizador + ler carrinho Redis + percorrer produtos + criar order + esvaziamento do carrinho.
**Hipótese a validar:** H2 — "o checkout nos microserviços tem latência ≥ 2× a do monólito".

**Como correr:**
```bash
k6 run --out csv=resultados/t-mono-2-exec1.csv -e TOKEN=$TOKEN t-mono-2-checkout.js
k6 run --out csv=resultados/t-mono-2-exec2.csv -e TOKEN=$TOKEN t-mono-2-checkout.js
k6 run --out csv=resultados/t-mono-2-exec3.csv -e TOKEN=$TOKEN t-mono-2-checkout.js
```

**O que anotar:** p50, p95, p99, taxa de sucesso.

---

## T-MONO-3 — Falha Total (MANUAL)

**Ficheiro:** `t-mono-3-falha-total.sh`
**Tipo:** manual — requer dois terminais
**O que demonstra:** no monólito, parar a BD derruba 100% dos endpoints — não existe isolamento entre domínios. É o contra-argumento para o isolamento de falhas dos microserviços (§5.3 T3).

**Instruções detalhadas:** ver secção "TESTES MANUAIS" no fim deste documento.

---

## T-MONO-4 — N+1 Queries (MANUAL)

**Tipo:** manual — requer ler os logs do container
**O que demonstra:** ao fazer `GET /rest/order/list` com uma order de 5 itens, o Hibernate emite 11 queries em vez de 1. Esta é a Fig. 3 do relatório — evidência empírica do problema de performance do monólito.

**Instruções detalhadas:** ver secção "TESTES MANUAIS" no fim deste documento.

---

## T-MONO-5 — Recursos CPU e RAM

**Ficheiro:** `t-mono-5-recursos.sh`
**O que mede:** memória e CPU do monólito em idle e sob carga (3 containers: app + postgres + redis).
**Para comparar com:** os microserviços que têm 11+ containers para a mesma funcionalidade.

**Como correr:**
```bash
cd /Users/I769175/ASID-XCommerce-Refactoring-1/01-monolith/testes
./t-mono-5-recursos.sh
```

---

## TESTES MANUAIS — Instruções passo a passo

### T-MONO-3 — Falha Total da BD

**Contexto:** provas que no monólito não existe isolamento de falhas. Uma falha na BD derruba tudo.

**Pré-requisito:** monólito UP em :18088, token obtido.

**Terminal 1 — loop de pedidos contínuos:**
```bash
TOKEN=$(curl -s -X POST http://localhost:18080/rest/user/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' | tr -d '"')

# Faz pedidos a cada segundo e mostra o status HTTP
while true; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:18080/rest/catalog/products)
  echo "$(date '+%H:%M:%S') GET /products → $STATUS"
  sleep 1
done
```

**Terminal 2 — ao fim de 15 segundos, pára a BD:**
```bash
docker stop monolith-db
echo "BD parada às $(date)"
# Espera 30 segundos a observar o Terminal 1
sleep 30
docker start monolith-db
echo "BD reiniciada às $(date)"
```

**O que vais observar:**
- Antes: `200` em todos os pedidos
- Após `docker stop`: todos ficam `500` ou `Connection refused`
- Após `docker start`: recupera gradualmente

**Screenshot / output para o relatório:** copiar o output do Terminal 1 mostrando a transição 200→500→200. Vai para §5.3 T3 e §2.2.

---

### T-MONO-4 — N+1 Queries

**Contexto:** o relatório afirma que um checkout com 5 itens produz 11 queries SQL. Precisas do log real para a Fig. 3.

**Passo 1 — Garantir que `spring.jpa.show-sql=true` está activo:**
```bash
grep "show-sql" /Users/I769175/ASID-XCommerce-Refactoring-1/01-monolith/xcommerce-monolithic-master/final/src/main/resources/application.properties
# Deve mostrar: spring.jpa.show-sql=true  ← já está activo
```

**Passo 2 — Adicionar 5 produtos ao carrinho:**
```bash
TOKEN=$(curl -s -X POST http://localhost:18080/rest/user/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' | tr -d '"')

# Produto 0 — 2 unidades
curl -s -X PATCH http://localhost:18080/rest/shoppingCart/addProduct \
  -H "Content-Type: application/json" \
  -d '{"id":0,"quantity":2}'

# Produto 1 — 1 unidade
curl -s -X PATCH http://localhost:18080/rest/shoppingCart/addProduct \
  -H "Content-Type: application/json" \
  -d '{"id":1,"quantity":1}'

# Produto 2 — 1 unidade
curl -s -X PATCH http://localhost:18080/rest/shoppingCart/addProduct \
  -H "Content-Type: application/json" \
  -d '{"id":2,"quantity":1}'

# Produto 3 — 1 unidade
curl -s -X PATCH http://localhost:18080/rest/shoppingCart/addProduct \
  -H "Content-Type: application/json" \
  -d '{"id":3,"quantity":1}'

echo "Carrinho com 5 itens (4 produtos) pronto."
```

**Passo 3 — Fazer checkout e capturar os logs SQL:**
```bash
# Limpa os logs anteriores para facilitar a leitura
docker logs monolith-app --since 5s > /dev/null 2>&1

# Faz o checkout
curl -s http://localhost:18080/rest/order/checkout

# Captura os logs SQL gerados durante o checkout
echo "=== LOGS SQL DO CHECKOUT ===" > resultados/t-mono-4-n1-queries.txt
docker logs monolith-app --since 10s 2>&1 | grep -E "Hibernate:|select|insert|update" \
  >> resultados/t-mono-4-n1-queries.txt

cat resultados/t-mono-4-n1-queries.txt
```

**O que deves ver (11 queries para 5 itens em 4 produtos):**
```
Hibernate: select * from "order" where id = ?           ← 1 query: order
Hibernate: select * from order_line where order_id = ?  ← 1 query: linhas
Hibernate: select * from product where id = ?           ← produto 0
Hibernate: select * from product where id = ?           ← produto 1
Hibernate: select * from product where id = ?           ← produto 2
Hibernate: select * from product where id = ?           ← produto 3
... (mais queries para brand, category por cada produto)
```

**Passo 4 — Anotar no relatório (§2.2 e Fig. 3):**
Conta as queries `select * from product` — deve haver 1 por produto distinto.
A fórmula é: 1 (order) + 1 (order_lines) + N (products) = 2 + N queries.
Com 4 produtos distintos = 6+ queries mínimo. Com lazy loading de Brand e Category por produto, chega a 11+.

---

## Tabela de resultados a preencher

Após correr todos os testes, preenche esta tabela e copia para §5.3 do relatório:

| Teste | Métrica | Exec 1 | Exec 2 | Exec 3 | Média |
|-------|---------|--------|--------|--------|-------|
| T-MONO-1 | p50 latência (ms) | | | | |
| T-MONO-1 | p95 latência (ms) | | | | |
| T-MONO-1 | taxa de erro (%) | | | | |
| T-MONO-2 | p50 latência (ms) | | | | |
| T-MONO-2 | p95 latência (ms) | | | | |
| T-MONO-2 | taxa de sucesso (%) | | | | |
| T-MONO-5 | RAM idle — app (MB) | | | | |
| T-MONO-5 | RAM idle — total (MB) | | | | |
| T-MONO-5 | CPU idle (%) | | | | |
| T-MONO-3 | Tempo até 1º erro (s) | | | | |
| T-MONO-3 | Endpoints afectados | todos | todos | todos | 100% |
| T-MONO-4 | Nº queries no checkout | | | | |
