# Testes — Monólito XCommerce

Scripts para reproduzir as experiências da Secção 6 do relatório, no monólito.

## Pré-requisitos

- Monólito a correr: `cd 01-monolith && docker compose up -d`
- `k6` instalado
- `python3` e `curl` no PATH

## Scripts disponíveis

```bash
./testes-velocidade.sh      # T1 + T2 com k6 — sustenta H2a
./testes-recursos.sh        # docker stats idle + sob carga — sustenta H2b
./testes-falha.sh           # paragem da BD durante checkout — sustenta H3
```

Todos os scripts validam que o monólito está acessível antes de correr.

## Saídas

Os resultados ficam em `resultados/`:

| Ficheiro | Conteúdo |
|---|---|
| `t-mono-1.json` | Resumo k6 do teste T1 (leitura simples) |
| `t-mono-2.json` | Resumo k6 do teste T2 (checkout) |
| `recursos-idle.txt` | `docker stats` em estado ocioso |
| `recursos-carga.txt` | `docker stats` durante T2 |
| `falha-bd.txt` | Estado dos dados após falha controlada (deve mostrar rollback) |

## Resultado esperado

| Teste | Métrica chave | Valor esperado |
|---|---|---|
| T1 | iter p95 | cerca de 3-4 ms |
| T2 | iter p95 | cerca de 4-5 ms |
| Recursos | RAM idle | cerca de 1,3 GiB (3 contentores) |
| Falha | Estado da BD | Sem alterações (rollback ACID confirmado) |

## Credenciais

- **Bootstrap:** `root` / `root` (utilizador SUPERADMIN criado automaticamente no arranque)
