# Plano de Testes — Microserviços XCommerce
> Gateway: http://localhost:9000
> Credenciais: username=admin / password=123456
> Estrutura espelhada com os testes do monólito para comparação directa

---

## O que este plano cobre

| Teste | Tipo | Objetivo | Secção relatório |
|-------|------|----------|-----------------|
| T-MICRO-1 | Performance (k6) | Latência GET /products — comparar com T-MONO-1 (p95 monólito = 16 ms) | §5.3 T1 |
| T-MICRO-2 | Performance (k6) | Latência checkout — comparar com T-MONO-2 (p95 monólito = 11.4 ms) | §5.3 T2 |
| T-MICRO-3 | Falha parcial (manual) | Provar isolamento de falhas — parar catalog-service, verificar que /order/list e /auth/login continuam | §5.3 T3 |
| T-MICRO-4 | Estado zombie (automático) | Provar compensação/rollback — falha no checkout não deixa stock nem order presos | §4.4, §6.3 |
| T-MICRO-5 | Recursos (script) | CPU e RAM idle e sob carga — comparar com monólito (3 containers vs 17) | §5.3 T5 |

## RESULTADOS JÁ OBTIDOS

| Teste | Estado | Resultado |
|-------|--------|-----------|
| T-MICRO-1 | ✅ Concluído | p50=11.2ms · p95=19.8ms · 0% erros |
| T-MICRO-2 | ✅ Concluído | p50=19.9ms · p95=32.8ms · 100% sucesso (2.9× monólito — H2 confirmada) |
| T-MICRO-4 | ✅ Concluído | Stock 306→304, order HANDLING — estado zombie confirmado |
| T-MICRO-5 | ✅ Concluído | 16 containers · ~6.0 GiB idle · 4.6× monólito — H5 confirmada |
| T-MICRO-3 | ✅ Concluído | /products→503 (circuit breaker); /order/list e /auth/login mantiveram 200 — isolamento confirmado |

---

## T-MICRO-1 — Latência de Leitura

**Ficheiro:** `t-micro-1-leitura.js`
**Comparação:** T-MONO-1 fez a mesma operação em p95 = 16 ms directo à BD local. O overhead esperado são os hops Gateway + catalog-service.

**Como correr:**
```bash
cd ./02-microservices/testes/k6
k6 run --out ../resultados/t-micro-1-exec1.csv ../t-micro-1-leitura.js
```

---

## T-MICRO-2 — Latência do Checkout

**Ficheiro:** `t-micro-2-checkout.js`
**Endpoint:** `POST http://localhost:9000/rest/order/checkout` (autenticado)
**Fluxo:** addProduct → checkout (cada iteração)
**O que mede:** latência do fluxo transacional completo: Gateway → order-service → inventory (REST) → catalog (REST) → cart (REST) → Kafka publish.
**Comparação:** T-MONO-2 fez checkout em p95 = 11.4 ms numa única transação JPA. Aqui são 3+ chamadas REST síncronas em série.
**Hipótese H2:** p95 microserviços ≥ 2× monólito = ≥ 22.8 ms.

**Como correr:**
```bash
TOKEN=$(curl -s -X POST http://localhost:9000/rest/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

cd ./02-microservices/testes/k6
k6 run --out ../resultados/t-micro-2-exec1.csv -e TOKEN="$TOKEN" ../t-micro-2-checkout.js
```

---

## T-MICRO-3 — Isolamento de Falhas (MANUAL — 2 terminais)

**Tipo:** manual
**O que demonstra:** nos microserviços, a falha do catalog-service não afeta endpoints de outros domínios (order/list, auth/login). Contrasta com T-MONO-3 onde 1 falha derrubou tudo.

### Terminal 1 — loop de pedidos a 3 endpoints em simultâneo:
```bash
TOKEN=$(curl -s -X POST http://localhost:9000/rest/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

while true; do
  TS=$(date '+%H:%M:%S')
  S1=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/products)
  S2=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" http://localhost:9000/rest/order/list)
  S3=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:9000/rest/user/login \
    -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}')
  echo "$TS  /products=$S1  /order/list=$S2  /auth/login=$S3"
  sleep 1
done
```

### Terminal 2 — ao fim de 15 segundos, para o catalog-service:
```bash
sleep 15
docker stop xcommerce-catalog-service-1   # ajusta o nome se necessário
echo "catalog-service parado às $(date)"
sleep 30
docker start xcommerce-catalog-service-1
echo "catalog-service reiniciado às $(date)"
```

**O que observar:**
- `/products` deve passar de 200 → fallback/503 (catalog parado)
- `/order/list` deve manter 200 (não depende do catalog)
- `/auth/login` deve manter 200 (não depende do catalog)
- Isto prova o isolamento de falhas — contrasta com T-MONO-3 onde tudo ficou 500

**Screenshot / output para o relatório:** copiar o output do Terminal 1 mostrando a transição. Vai para §5.3 T3.

**Nota:** para confirmar o nome exacto do container: `docker ps --format "{{.Names}}" | grep catalog`

---

## T-MICRO-4 — Estado Zombie (automático)

**Ficheiro:** `t-micro-4-estado-zombie.sh`
**O que demonstra:** uma falha no fluxo de checkout deve ser compensada localmente, sem deixar stock decrementado nem orders presas em estado intermédio.
**Nota:** este cenário valida o rollback/idempotência do inventário; a versão antiga focada só em payment zombie ficou obsoleta.

**Como funciona:**
1. Faz checkout → order criada e evento publicado
2. Força uma falha no passo de inventário/compensação
3. O inventory-service deve reverter os decrementos já feitos no mesmo fluxo
4. Verifica na BD: stock consistente, order sem ficar presa e sem duplicados processados

**Como correr:**
```bash
cd /Users/I769175/ASID-XCommerce-Refactoring-1/02-microservices/testes
./t-micro-4-estado-zombie.sh
```

**Resultado obtido:** order id=138 · stock 306→304 · status=HANDLING · **estado zombie confirmado**
Evidência: `resultados/t-micro-4-estado-zombie-20260527-165205.txt`

---

## T-MICRO-5 — Recursos CPU e RAM

**Ficheiro:** `t-micro-5-recursos.sh`
**O que mede:** memória e CPU de todos os containers xcommerce em idle e sob carga.
**Comparação:** monólito usava 3 containers / ~1.31 GiB idle / 1.5% CPU idle.

**Como correr:**
```bash
cd /Users/I769175/ASID-XCommerce-Refactoring-1/02-microservices/testes
./t-micro-5-recursos.sh
```

---

## Tabela de resultados

| Teste | Métrica | Microserviços | Monólito | Delta |
|-------|---------|--------------|----------|-------|
| T-MICRO-1 | p50 latência (ms) | 11.2 ms | 8.8 ms | +2.4 ms (+27%) |
| T-MICRO-1 | p95 latência (ms) | 19.8 ms | 16 ms | +3.8 ms (+24%) |
| T-MICRO-1 | taxa de erro (%) | 0% | 0% | — |
| T-MICRO-2 | p50 latência (ms) | 19.9 ms | 7.1 ms | +12.8 ms (+180%) |
| T-MICRO-2 | p95 latência (ms) | 32.8 ms | 11.4 ms | +21.4 ms (+188%) · 2.9× · **H2 confirmada** |
| T-MICRO-2 | taxa de sucesso (%) | 100% | 100% | — |
| T-MICRO-3 | /products após catalog stop | 503 (circuit breaker fallback) | 500 (tudo) | isolamento confirmado |
| T-MICRO-3 | /order/list após catalog stop | 200 (sem impacto) | 500 (tudo) | isolamento confirmado |
| T-MICRO-3 | /auth/login após catalog stop | 200 (sem impacto) | 500 (tudo) | isolamento confirmado |
| T-MICRO-4 | Stock decrementado sem order confirmada | ✅ 306→304 · order HANDLING | N/A | Estado zombie confirmado |
| T-MICRO-5 | Nº containers | 17 | 3 | +14 containers |
| T-MICRO-5 | RAM idle total | ~6.3 GiB | ~1.31 GiB | 4.9× mais · **H5 confirmada** |
| T-MICRO-5 | CPU idle (%) | 10.0% | 1.5% | 6.7× mais |
</content>
</invoke>