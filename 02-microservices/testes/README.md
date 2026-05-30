# Testes — Microserviços XCommerce

Scripts para reproduzir as experiências da Secção 6 do relatório, na arquitetura de microserviços.

## Pré-requisitos

- Microserviços a correr: `cd 02-microservices && docker compose up -d`
- `k6` instalado
- `python3` e `curl` no PATH

## Scripts disponíveis

```bash
./testes-velocidade.sh      # T1 + T2 com k6 — sustenta H2a
./testes-recursos.sh        # docker stats idle + sob carga — sustenta H2b
./testes-falha.sh           # paragem do inventory-service durante checkout — sustenta H3
```

Todos os scripts validam que o gateway está acessível antes de correr e populam as bases de dados se estiverem vazias.

## Saídas

Os resultados ficam em `k6/output/`:

| Ficheiro | Conteúdo |
|---|---|
| `t-micro-1.json` | Resumo k6 do teste T1 (leitura simples) |
| `t-micro-2.json` | Resumo k6 do teste T2 (checkout) |
| `recursos-idle.txt` | `docker stats` em estado ocioso |
| `recursos-carga.txt` | `docker stats` durante T2 |
| `zombie-order.txt` | Encomenda em estado intermédio (HANDLING) após falha |
| `zombie-stock.txt` | Estado do stock após falha |
| `zombie-kafka.txt` | Consumer group sem membros, lag não consumido |

## Resultado esperado

| Teste | Métrica chave | Valor esperado |
|---|---|---|
| T1 | iter p95 | cerca de 3-4 ms |
| T2 | iter p95 | cerca de 13-15 ms (3,5× o monólito) |
| Recursos | RAM idle | cerca de 5,5 GiB (20 contentores) |
| Falha | Estado da order | `HANDLING` (zombie, sem recuperação automática) |

## Credenciais

- **Bootstrap:** `admin` / `123456` (utilizador ADMIN criado automaticamente pelo `identity-service`)

## Observabilidade

| Ferramenta | URL | Login |
|---|---|---|
| Grafana | <http://localhost:3000> | `admin` / `admin` |
| Prometheus | <http://localhost:9090> | — |
| Jaeger | <http://localhost:16686> | — |
| Kafdrop | <http://localhost:9001> | — |

Recomenda-se ter o Jaeger aberto durante T2 para capturar o *trace* de um checkout (mostra os 5 *spans* síncronos), e o Kafdrop aberto durante o teste de falha para mostrar o *lag* não consumido no tópico `order-created-events`.
