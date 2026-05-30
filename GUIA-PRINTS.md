# Guia de Capturas de Ecrã para a Secção 6

Este guia indica **que capturas tirar** das ferramentas de observabilidade (Grafana, Jaeger, Prometheus, Kafdrop) **durante** os testes, e **onde inserir cada uma** na Secção 6 do relatório.

---

## Antes de começar

Garante que ambas as stacks estão a correr:

```bash
cd 01-monolith && docker compose up -d && cd ..
cd 02-microservices && docker compose up -d && cd ..
```

E que tens os 4 dashboards abertos em separadores diferentes do browser:

| Ferramenta | URL | Login |
|---|---|---|
| **Grafana** | <http://localhost:3000> | `admin` / `admin` |
| **Prometheus** | <http://localhost:9090> | — |
| **Jaeger** | <http://localhost:16686> | — |
| **Kafdrop** | <http://localhost:9001> | — |

---

## Capturas a tirar — por hipótese

### Para H2a (Velocidade) — Secção 6.4

**Captura 1: Trace Jaeger de um checkout**

Mostra empiricamente as 5 chamadas REST síncronas em série que sustentam o argumento da super-linearidade.

1. Abre o Jaeger: <http://localhost:16686>
2. Antes de capturar, executa um único checkout para gerar um trace:
   ```bash
   TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}' \
     http://localhost:9000/rest/user/login | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

   curl -X PATCH http://localhost:9000/rest/shoppingCart/addProduct \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"productId":1,"quantity":1}'

   curl -X POST http://localhost:9000/rest/order/checkout \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{}'
   ```
3. No Jaeger, na barra lateral esquerda:
   - **Service:** `api-gateway`
   - **Operation:** `POST /rest/order/checkout`
   - **Lookback:** Last 5 minutes
   - Clica em **Find Traces**
4. Clica no trace mais recente do checkout
5. **Captura:** vista expandida do trace mostrando os spans ordenados (gateway → order-service → cart → catalog → inventory → cart). Idealmente em ecrã inteiro, com os tempos de cada span visíveis.

**Inserir em:** §6.4, junto à interpretação da super-linearidade. Legenda sugerida: *"Trace Jaeger de um checkout completo, evidenciando as cinco chamadas síncronas em série coordenadas pelo serviço de encomendas."*

---

**Captura 2: Throughput durante T2 (Grafana)**

Mostra a estabilidade das medições durante a janela de 60 segundos.

1. Abre o Grafana: <http://localhost:3000>
2. Vai a *Dashboards → Browse* e abre o dashboard de microserviços (criar se ainda não existir, ver secção *"Configurar dashboards básicos"* abaixo)
3. Coloca o dashboard em modo **Live** (botão de play, refresh 5s)
4. **Antes da captura, inicia o T2:**
   ```bash
   cd 02-microservices/testes && ./testes-velocidade.sh
   ```
5. Espera ~30 segundos (warm-up termina, mediação começa)
6. **Captura:** painel de throughput mostrando o pico estável durante a janela de medição

**Inserir em:** §6.4, junto à tabela de resultados de T1/T2.

---

### Para H2b (Recursos) — Secção 6.5

**Captura 3: Memória dos serviços (Grafana)**

Mostra visualmente a desproporção de RAM entre arquiteturas.

1. No Grafana, abre um painel de uso de memória JVM (ou cria com a query Prometheus `jvm_memory_used_bytes{area="heap"}`)
2. Janela temporal: últimos 30 minutos (deve apanhar idle e carga)
3. **Captura:** gráfico mostrando os 8 serviços a consumir memória de forma estável

Em alternativa, simplesmente capturar a saída de `docker stats` em terminal já é evidência suficiente.

**Inserir em:** §6.5, junto à tabela comparativa de RAM.

---

### Para H3 (Estado Zombie) — Secção 6.6

**Captura 4: Consumer lag no Kafdrop**

Mostra a mensagem presa por consumir.

1. Abre o Kafdrop: <http://localhost:9001>
2. **Antes da captura, executa o teste de falha:**
   ```bash
   cd 02-microservices/testes && ./testes-falha.sh
   ```
3. **Imediatamente** após a mensagem `inventory-service parado às HH:MM:SS` mas antes do `Reiniciar inventory-service`, vai ao Kafdrop:
   - Clica no tópico **`order-created-events`**
   - **Captura:** vista do tópico mostrando a última mensagem publicada
4. Volta à página inicial do Kafdrop e clica em **Consumer Groups** (ou na navegação superior):
   - Encontra **`inventory-group`**
   - **Captura:** ecrã mostrando *lag* não nulo e ausência de membros ativos

**Alternativa via terminal** (mais simples):
```bash
docker exec xcommerce-kafka /usr/bin/kafka-consumer-groups \
  --bootstrap-server kafka:29092 \
  --describe --group inventory-group
```
Capturar o output mostrando `LAG` e `CONSUMER-ID = -`.

**Inserir em:** §6.6, junto à tabela de estado dos dados.

---

**Captura 5: psql mostrando order em HANDLING**

Evidência do estado intermédio na BD.

```bash
docker exec xcommerce-db-orders psql -U postgres -d xcommerce_orders \
  -c "SELECT id, username, status FROM orders ORDER BY id DESC LIMIT 3;"
```

A captura do terminal a mostrar `HANDLING` na coluna status é evidência suficiente. **Inserir em:** §6.6, ao lado do print do Kafdrop.

---

## Configurar dashboards básicos no Grafana

Se o Grafana não tiver datasources e dashboards, configura-os uma única vez:

### 1. Adicionar Prometheus como datasource

```bash
curl -s -u admin:admin -X POST http://localhost:3000/api/datasources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prometheus",
    "type": "prometheus",
    "url": "http://prometheus:9090",
    "access": "proxy",
    "isDefault": true
  }'
```

### 2. Importar dashboard pronto

Em Grafana → *Dashboards → Import → Import via dashboard ID*, usar o ID **4701** (JVM Micrometer) — mostra automaticamente memória, CPU, threads de cada serviço.

### 3. Criar painel ad-hoc para T2

Em *Explore* (lupa lateral), com Prometheus selecionado, executa esta query:

```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri="/rest/order/checkout"}[1m])) * 1000
```

Mostra o p95 do checkout em milissegundos, em tempo real durante o T2.

---

## Ordem recomendada para tirar tudo de uma vez

Para minimizar o tempo total, sugere-se esta sequência:

1. Subir as duas stacks (`docker compose up -d` em cada pasta)
2. Esperar 1 minuto para tudo estabilizar
3. Configurar Grafana (uma vez só)
4. Tirar **Captura 4** (Kafdrop antes do teste — vista limpa para comparação)
5. Correr `./testes-falha.sh` nos microserviços e tirar **Capturas 4 e 5** durante a janela de zombie
6. Reiniciar inventory-service (o script já faz isto)
7. Correr `./testes-velocidade.sh` nos microserviços e, em paralelo, tirar **Capturas 1, 2 e 3** no Jaeger e Grafana
8. Guardar todas as capturas em `Anexos/` para referência cruzada com o relatório
