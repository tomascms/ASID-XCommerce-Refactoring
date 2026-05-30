# Estrutura do Relatório Final — XCommerce: Monólito vs Microserviços
> Template ACM · Máx 20 páginas sem figuras · Deadline: 31/05/2026 23h59 Blackboard
> Critérios: Fundamentação Arquitetural 40% | Avaliação Experimental 30% | Implementação/Deployment 15% | Discussão Crítica 10% | Apresentação 5%

---

## ÍNDICE DE FIGURAS (a incluir no relatório)

| # | Título | Secção | Tipo |
|---|--------|--------|------|
| Fig. 1 | C4 Container — Monólito XCommerce | §2.1 | Diagrama arquitetura |
| Fig. 2 | C4 Component — interior do monólito (packages partilhando EntityManager) | §2.1 | Diagrama arquitetura |
| Fig. 3 | Log SQL Hibernate anotado — N+1 no checkout (5 itens → 11 queries) | §2.2 | Evidência empírica |
| Fig. 4 | C4 Container — Microserviços (8 serviços + Gateway + Kafka + Postgres lógico) | §3.1 | Diagrama arquitetura |
| Fig. 5 | C4 Component — zoom em `order-service` | §3.1 | Diagrama arquitetura |
| Fig. 6 | Topologia dos tópicos Kafka (saga por coreografia) | §3.1 | Diagrama integração |
| Fig. 7 | Papel arquitetural do API Gateway | §3.3 | Diagrama lógico |
| Fig. 8 | Database per Service — instância única, BDs lógicas separadas | §3.4 | Diagrama infra |
| Fig. 9 | Sequence — adicionar ao carrinho (TO-BE) | §4.1 | Sequence diagram |
| Fig. 10 | Sequence comparativo — consultar encomendas (AS-IS vs TO-BE) | §4.2 | Sequence diagram |
| Fig. 11 | Sequence — checkout completo (REST + Kafka, pontos de falha) | §4.3 | Sequence diagram |
| Fig. 12 | Diagrama de "estado zombie" — falha parcial controlada | §4.4 | Diagrama de falha |
| Fig. 13 | Latência p50/p95/p99 — monólito vs microserviços (3 workflows) | §5.4 | Gráfico barras |
| Fig. 14 | Disponibilidade durante falha (T3) — % respostas 2xx ao longo do tempo | §5.4 | Gráfico linhas |
| Fig. 15 | Consumo CPU/RAM — idle e em carga (T5) | §5.4 | Gráfico barras |
| Fig. 16 | Tabela visual — acoplamento eliminado vs deslocado | §6.2 | Tabela analítica |

> **Anexos digitais (no Drive, não contam para 20 páginas):** screenshots Grafana/Prometheus/Jaeger/Kafdrop, traces completos, logs brutos. Detalhes em §Anexos.

---

## ABSTRACT (~150 palavras)

**O que escrever:**
Problema arquitetural estudado (1 frase) → abordagem (refactoring XCommerce) → decisões principais (decomposição em N serviços, Kafka para fluxos assíncronos, API Gateway) → principais resultados experimentais (latência checkout, isolamento de falhas) → conclusão arquitetural.

**Não fazer:** Descrever o sistema. Resumir a implementação.

---

## 1. INTRODUÇÃO

### 1.1 Problema Arquitetural Central

**O que escrever:**
Identificar UMA linha de análise clara. Sugestão com base no nosso caso:

> "O problema central é o acoplamento no fluxo de checkout do XCommerce Monolítico: uma falha única na base de dados PostgreSQL derruba autenticação, catálogo, carrinho e processamento de encomenda simultaneamente. Estudamos se a decomposição em microserviços melhora o isolamento de falhas e a deployability independente, e a que custo em termos de latência e complexidade operacional."

**O que NÃO fazer:** Dizer "estudamos monólito vs microserviços em geral" — demasiado vago.

**Alternativas que existiam (mencionar brevemente):**
- Modular monolith (separação lógica sem deployment independente)
- SOA com ESB
- Microserviços (abordagem adotada)

### 1.2 Motivação

**O que escrever:**
- No monólito XCommerce: userId hardcoded = 1, stock não é validado nem decrementado no checkout, toda a aplicação escala como uma unidade, falha da BD afeta tudo.
- Limitações observadas no AS-IS que motivam o estudo (não a solução).

### 1.3 Objetivos e Questões de Investigação

**O que escrever — ligar cada QI a um atributo de qualidade, com hipóteses falsificáveis (threshold quantitativo explícito):**

| QI | Atributo | Hipótese (com critério de refutação) |
|----|----------|--------------------------------------|
| QI1a: Qual o overhead da decomposição em workflows de **consulta**? | Performance | **H1a:** Em consultas (listar carrinho, listar encomendas), o overhead p95 dos microserviços face ao monólito é < 50 ms sob carga moderada. Refutada se overhead ≥ 50 ms. |
| QI1b: Qual o overhead da decomposição em workflows **transacionais**? | Performance | **H1b:** No checkout, a latência p95 dos microserviços é ≤ 3× a do monólito sob carga equivalente. Refutada se > 3×. |
| QI2: O isolamento de falhas melhora a *availability* quando um serviço de domínio falha? | Availability | **H2:** Com `catalog-service` indisponível, a consulta de encomendas mantém taxa de sucesso ≥ 99% nos microserviços; no monólito, BD indisponível derruba todas as operações (taxa sucesso ~0%). |
| QI3: A decomposição introduz inconsistência observável em falha parcial? | Consistency / Reliability | **H3:** Em N execuções de checkout com falha injetada no `payment-service`, observa-se ≥ 1 caso de stock decrementado sem encomenda confirmada (estado *zombie*). Confirma a limitação ausência de Saga. |
| QI4: Que tipos de acoplamento são eliminados e quais são deslocados? | Maintainability | **H4:** A decomposição elimina acoplamento estrutural (FKs JPA cruzadas, schema partilhado) mas introduz acoplamento por contrato (REST/Kafka), runtime (cascata de falhas) e operacional (Gateway, Postgres). Avaliada qualitativamente em §6.2. |
| QI5: Qual o custo operacional da arquitetura distribuída em *idle* e em carga? | Cost | **H5:** Os microserviços consomem ≥ 2× a memória residente do monólito em *idle*, para a mesma funcionalidade. |

**Para cada hipótese indicar (template aplicado em §5):**
- Cenário experimental que a avalia
- Métricas recolhidas (com unidades)
- Resultado esperado e *threshold* de refutação
- Resultado observado e interpretação arquitetural

> **Nota sobre "redução do acoplamento":** A formulação evita a palavra "eliminação". A decomposição reduz alguns tipos de acoplamento e desloca outros — esta é a contribuição central da análise (ver §6.2).

### 1.4 Âmbito do Trabalho Realizado e Contributo do Grupo

**O que escrever — clarificar origem do código e contributo (Pt.9 do feedback):**

| Componente | Origem | Trabalho do grupo |
|------------|--------|-------------------|
| Monólito XCommerce | Fork de Omar Iraqi (referência completa em §8) | Estudo, instrumentação para testes, ativação de logging SQL |
| `auth-service`, `catalog-service`, `cart-service`, `order-service`, `user-service` | Refactoring da lógica do monólito | Implementação dos serviços a partir do código original; nova camada de comunicação |
| `inventory-service`, `payment-service`, `notification-service` | Implementação de raiz | **Extensão funcional** — não existiam no monólito (ver §3.2) |
| `api-gateway`, `bff`, `web-ui` | Implementação de raiz | Nova camada de entrada e composição |
| Configuração Kafka, Resilience4j, Docker Compose, observabilidade | Configuração/integração | Trabalho de integração do grupo |
| Scripts de teste, dashboards Grafana | De raiz | Trabalho do grupo |

> Detalhes completos da imputação ficheiro-a-ficheiro no Anexo B.

---

## 2. SISTEMA DE REFERÊNCIA — AS-IS (Monólito)

> **REGRA CRÍTICA:** Nesta secção NÃO aparecem Kafka, InventoryService, chamadas HTTP entre serviços, nem nada da solução TO-BE. Apenas o que foi *observado* no monólito original.

### 2.1 Arquitetura do Monólito XCommerce

**O que escrever:**
- Spring Boot 2.1.9, Java 8, PostgreSQL única instância, Redis cache, JWT
- MVC-SOA: organização interna por serviços mas deployment único
- 33 classes Java, 5 packages
- Único artefacto executável (build.gradle)

**Figuras desta secção:**
- **Figura 1** — C4 Nível 2 (Container) do monólito: Spring Boot + PostgreSQL + Redis + Browser
- **Figura 2** — C4 Nível 3 (Component) interno: packages auth/catalog/cart/order/user partilhando o mesmo `EntityManager` JPA. Esta figura sustenta visualmente o argumento do acoplamento estrutural.

### 2.2 Problemas Arquiteturais Observados

**O que escrever — cada problema com evidência concreta:**

#### Problema 1: Acoplamento total no checkout
- No monólito, `GET /rest/order/checkout` acede diretamente a ShoppingCart, Product, Order, User na mesma transação JPA
- Uma falha na BD derruba todas as funcionalidades simultaneamente
- **Evidência empírica do N+1 (Figura 3):** Log SQL Hibernate capturado durante checkout com 5 itens no carrinho.
  - **Como obter:** ativar `spring.jpa.show-sql=true` e `logging.level.org.hibernate.SQL=DEBUG` no `application.properties`; executar checkout; recortar trecho relevante; anotar com setas.
  - **Decomposição da fórmula:**
    - `1` = `SELECT * FROM "order" WHERE id = ?` (query inicial)
    - `N` = `SELECT * FROM cart_item WHERE order_id = ?` × N items (lazy-load por item)
    - `M` = `SELECT * FROM product WHERE id = ?` × M produtos (lazy-load por item)
    - **Total observado:** 1 + 5 + 5 = 11 queries para checkout de 5 itens (preencher com valor real)
  - **Sustentação:** sem este log anotado, o argumento N+1 fica não-evidenciado — *requisito explícito do feedback*.

#### Problema 2: Escalabilidade monolítica
- Catalog tem mais carga que checkout, mas escalam juntos
- Para escalar catálogo replica-se toda a aplicação

#### Problema 3: userId hardcoded = 1
- `OrderRestController` e `ShoppingCartRestController` usam userId fixo
- Impossível multi-utilizador real sem refactoring

#### Problema 4: Stock não validado no checkout
- Campo `quantity` existe em Product mas não é verificado nem decrementado no fluxo de checkout
- **Assumir explicitamente:** isto é uma limitação funcional do sistema original, não um problema de consistência distribuída — o monólito simplesmente não implementa gestão de stock

### 2.3 Dependências Estruturais (AS-IS)
- Carrinho ↔ Catálogo: CartItem guarda entidade JPA Product completa (acoplamento estrutural)
- Encomenda ↔ Todos os domínios: Order depende de User, CartItem, Product na mesma transação
- Auth ↔ Domínio: segurança transversal a todos os controllers via Spring Security

---

## 3. ARQUITETURA TO-BE — Microserviços

> **REGRA CRÍTICA:** Esta secção apresenta *decisões* que tomámos e os *trade-offs* que introduzem. Separar claramente o que resolvemos e o que ficou mais complexo.

### 3.1 Visão Geral da Decomposição

**O que escrever:**
- Padrão seguido: decomposição por Bounded Context (DDD)
- 8 serviços de negócio + API Gateway + (opcional: BFF + Web UI)
- Comunicação mista: REST síncrono (OpenFeign) + Kafka assíncrono (coreografia)
- Database per service: instância PostgreSQL única com schemas/DBs lógicos separados (ver §3.4 — honestidade sobre o que isto garante e não garante)

**Figuras desta secção:**
- **Figura 4** — C4 Nível 2 dos microserviços: Gateway, 8 serviços, Kafka, Postgres lógico. Convenção: setas sólidas = REST síncrono; tracejadas = Kafka.
- **Figura 5** — C4 Nível 3 (zoom em `order-service`): controllers, Feign clients para inventory/catalog/cart, Kafka producers/consumers, repositório local.
- **Figura 6** — Topologia dos tópicos Kafka (`order-placed`, `order-placed-events`, `payment-events`, `order-confirmed-events`) com produtores e consumidores.

**Tabela de serviços:**
| Serviço | Porta | BD | Bounded Context | Justificação da fronteira |
|---------|-------|-----|-----------------|--------------------------|
| auth-service | 8081 | xcommerce_auth | Autenticação/sessão | Ciclo de vida independente; pode ser substituído por Keycloak sem afetar outros |
| catalog-service | 8082 | xcommerce_catalog | Produtos, marcas, categorias | Maior carga de leitura; pode escalar independentemente |
| cart-service | 8083 | xcommerce_cart | Estado volátil de carrinho | Dados efémeros; Redis seria alternativa; isolável |
| order-service | 8084 | xcommerce_orders | Ciclo de vida de encomendas | Estado transacional crítico; fronteira natural de negócio |
| inventory-service | 8085 | xcommerce_inventory | Gestão de stock | **Extensão funcional** — não existia no monólito (ver 3.3) |
| payment-service | 8087 | — | Processamento de pagamento | **Extensão funcional** — não existia no monólito (ver 3.3) |
| notification-service | 8088 | — | Envio de notificações | Assíncrono por natureza; falha não deve bloquear o fluxo principal |
| user-service | 8086 | xcommerce_users | Perfis de utilizador | Separado de auth (ver trade-off 3.2) |

> Não basta enunciar princípios gerais. Para cada fronteira: que problema resolve, que alternativa existia, que trade-off introduz.

#### Cart / Catalog
- **Problema que resolve:** No monólito, CartItem guarda entidade JPA Product completa — acoplamento estrutural. Cart e Catalog partilham o mesmo schema.
- **Decisão:** Cart passa a guardar apenas `productId` e `quantity`. Elimina-se o acoplamento estrutural.
- **Alternativa:** Manter Cart e Catalog no mesmo serviço (CatalogCart service mais agregado).
- **Trade-off introduzido:** Cart deixa de ter acesso direto a dados do produto (nome, preço). Para mostrar o carrinho com detalhes, order-service precisa de chamar catalog-service via REST (dependência em runtime). Se catalog-service falhar, o carrinho mostra apenas IDs.

#### Auth / User
- **Problema que resolve:** No monólito, autenticação e perfil de utilizador estão acoplados. Auth pode evoluir para OAuth2/Keycloak sem tocar nos dados de perfil.
- **Decisão:** auth-service gere credenciais (username/password/JWT); user-service gere perfis (nome, morada, role).
- **Alternativa:** Um único identity-service com ambas as responsabilidades.
- **Trade-off introduzido:** Introduz sincronização — `POST /auth/internal/sync` e `POST /users/internal/sync`. Quando um utilizador se regista, auth-service publica evento Kafka → user-service cria o perfil. **Risco:** janela de inconsistência entre criação de credenciais e criação de perfil. Sem compensação implementada, se user-service falhar após auth criar a conta, o utilizador existe em auth mas não tem perfil.

#### Inventory (extensão funcional — não era refactoring)
- **O que era no monólito:** campo `quantity` em Product, não validado no checkout
- **Decisão:** Criar inventory-service que valida e decrementa stock no checkout
- **Justificação:** Completar a semântica funcional era necessário para tornar o sistema comparável em condições reais
- **Trade-off introduzido:** order-service → inventory-service via REST síncrono (OpenFeign) antes de confirmar encomenda. Se inventory-service falhar, checkout falha. Sem compensação implementada (não há Saga com rollback de stock).

#### Payment (extensão funcional — não era refactoring)
- **Decisão:** Assíncrono via Kafka — notificações não bloqueiam resposta ao utilizador
- **Alternativa:** Chamada REST síncrona após checkout
- **Trade-off:** Se Kafka falhar, notificações perdem-se (sem dead-letter queue implementada).

### 3.3 Papel do API Gateway

**O que escrever — papel arquitetural primeiro, configuração depois:**

**Responsabilidades arquiteturais:**
1. Ponto único de entrada — esconde estrutura interna dos serviços do cliente
2. Routing por path (`/products/**` → catalog-service)
3. Propagação de identidade — extrai JWT e injeta headers `X-User-Name`, `X-User-Role` para os serviços internos
4. Resiliência — Circuit Breaker (Resilience4j) por rota com fallback

**Problema que resolve:** sem Gateway, o cliente precisaria de conhecer os endereços de 8+ serviços e gerir JWT em cada chamada.

**Riscos introduzidos:**
- SPOF: se o Gateway falhar, toda a aplicação fica inacessível (mitigado com múltiplas instâncias em produção, não implementado no protótipo)
- Latência adicional: cada pedido passa por uma hop extra
- Circuit Breaker não resolve inconsistência de dados — apenas protege contra cascata de falhas

**Configuração concreta** (resumida — detalhes em Anexo X):
- 10 rotas configuradas, cada uma com circuit breaker e fallback `/fallback/*`

**Figura 7** — Diagrama lógico do Gateway: entrada → routing por path → circuit breaker → fallback. Mostra o papel arquitetural antes da configuração concreta.

### 3.4 Database Per Service — Honestidade sobre a implementação

**O que escrever:**

**O que implementámos:** instância PostgreSQL única com múltiplas bases de dados lógicas (`xcommerce_auth`, `xcommerce_catalog`, `xcommerce_cart`, `xcommerce_orders`, `xcommerce_inventory`, `xcommerce_users`).

**O que isto garante:**
- Ownership lógico dos dados: cada serviço acede apenas à sua BD
- Sem foreign keys cruzadas entre serviços
- Schema evolution independente

**O que NÃO garante:**
- Isolamento físico: a instância PostgreSQL é uma dependência operacional comum — se falhar, todos os serviços que dependem de BD ficam indisponíveis
- Transações distribuídas: não existem transações ACID que abranjam dois serviços
- Em caso de falha parcial no checkout (stock decrementado, pagamento falhou), não há rollback automático

**Comparação:** isolamento físico completo exigiria uma instância PostgreSQL por serviço ou Cloud SQL separado — não implementado por custo e complexidade do protótipo.

**Figura 8** — Diagrama "Database per Service (lógico)": uma instância PostgreSQL contendo 6 bases de dados lógicas, com setas a indicar que cada serviço acede apenas à sua. A figura torna visualmente óbvia a distinção entre isolamento lógico e físico.

### 3.5 Comunicação Inter-Serviço — Kafka vs REST

**O que escrever:**

#### REST Síncrono (OpenFeign) — usado quando:
- Resposta necessária para prosseguir (order → inventory: precisa de saber se há stock)
- order → catalog: precisa de preço do produto
- order → cart: precisa de itens do carrinho

**Trade-off:** latência acumulada no checkout (cada chamada REST adiciona RTT); se um serviço falhar, o checkout falha.

#### Kafka Assíncrono — usado quando:
- Resposta não é necessária imediatamente (notificações)
- Fluxo pode ser eventualmente consistente (confirmação de pagamento)

**Fluxo de checkout com Kafka (Saga por Coreografia):**
```
order-service → publica "order-placed" → inventory-service consome (decrementa stock)
order-service → publica "order-placed-events" → payment-service consome (processa pagamento)
payment-service → publica "payment-events" (PAYMENT_SUCCESSFUL/FAILED)
order-service consome → confirma ou cancela encomenda
order-service → publica "order-confirmed-events" → notification-service consome
```

**Problemas que o Kafka introduz e que NÃO resolvemos (assumir como limitações):**
- **Entrega duplicada:** Kafka garante at-least-once. Se inventory-service processar a mesma mensagem duas vezes, o stock é decrementado duas vezes. Solução: idempotência por messageId — **não implementada**.
- **Compensação:** Se pagamento falhar após stock decrementado, o stock não é restaurado. Não há Saga compensatória implementada.
- **Dead-letter queue:** Mensagens que falham são perdidas. Não implementado.
- **Consistência eventual:** entre `order-placed` e `order-confirmed`, a encomenda existe em estado intermédio.

---

## 4. ANÁLISE DE WORKFLOWS

> Workflows representativos: dois "caminho-feliz" (consulta + transacional) e um de **falha parcial controlada**. Para cada um: owner dos dados, dependências, falhas possíveis, estado inconsistente, mitigação, lacuna.

### 4.1 Workflow 1 — Adicionar Produto ao Carrinho (simples)

**Figura 9** — Sequence diagram: cliente → Gateway → cart-service → (opcional) catalog-service.

| Aspeto | Monólito | Microserviços |
|--------|----------|---------------|
| Owner dos dados | Único schema | cart-service (cartItem), catalog-service (product) |
| Queries | 1 query Product + 1 INSERT CartItem | REST GET /products/{id} + INSERT local |
| Falha possível | BD única falha → tudo para | catalog-service falha → cart-service não consegue validar produto |
| Estado inconsistente | Não (transação local) | CartItem com productId de produto inexistente (se catalog falhar após validação) |
| Mitigação | — | Circuit Breaker no Gateway; cart-service pode aceitar sem validação |
| Lacuna | — | Sem validação de existência de produto em runtime no cart-service |

**Comparação de latência (preencher com dados reais):**
- Monólito: X ms (p50), Y ms (p95)
- Microserviços: X ms (p50), Y ms (p95)
- Overhead de comunicação: delta ms

### 4.2 Workflow 2 — Consultar Encomendas (simples)

**Figura 10** — Sequence diagram comparativo: monólito (uma transação JPA com N+1) vs microserviços (uma chamada local a `xcommerce_orders`, sem JOIN cruzado).

**No monólito:**
- `GET /rest/order/list` → userId hardcoded = 1 → JOIN Order + User + CartItem + Product
- N+1 problem: 1 query Order + N queries lazy-load por cada OrderItem para Product
  - **Evidência:** [inserir log SQL Hibernate]

**Nos microserviços:**
- `GET /order/list` com header X-User-Name → order-service consulta apenas xcommerce_orders
- Sem JOIN cruzado; dados de produto não são retornados (apenas productId)
- **Trade-off:** resposta mais pobre em dados mas sem dependência de catalog-service

### 4.3 Workflow 3 — Checkout (transacional crítico)

**Figura 11** — Sequence diagram completo do checkout TO-BE: 8 passos, REST síncronos (linhas sólidas) + eventos Kafka (tracejadas). Pontos de inconsistência possível marcados a vermelho.

**Passo a passo com ownership:**

| Passo | Serviço | Owner dos dados | Falha possível | Consequência |
|-------|---------|-----------------|----------------|--------------|
| 1. Autenticar | auth-service | xcommerce_auth | auth falha | Checkout bloqueado — aceitável |
| 2. Ler carrinho | cart-service | xcommerce_cart | cart falha | Checkout bloqueado — aceitável |
| 3. Verificar stock | inventory-service (REST) | xcommerce_inventory | inventory falha | Checkout bloqueado; stock não decrementado — consistente |
| 4. Criar order | order-service | xcommerce_orders | order falha após stock decrementado | **Inconsistência: stock decrementado sem order criada** |
| 5. Publicar evento Kafka | order-service | — | Kafka indisponível | Order criada mas pagamento nunca processado — **estado zombie** |
| 6. Confirmar order | order-service (Kafka listener) | xcommerce_orders | — | |
| 7. Notificar | notification-service (Kafka) | — | Mensagem perdida | Utilizador não recebe email — aceitável (não crítico) |
| 8. Notificar | notification-service (Kafka) | — | Mensagem perdida | Utilizador não recebe email — aceitável (não crítico) |

**Limitações assumidas:**
- Não há Saga compensatória: passos 3→4 e 4→5 não têm rollback
- Sem idempotência no inventory-service e payment-service
- Sem dead-letter queue para mensagens Kafka falhadas

**Comparação de latência checkout (preencher com dados reais):**
- Monólito: X ms (p50), Y ms (p95) — tudo em memória, uma transação
- Microserviços: X ms (p50), Y ms (p95) — inclui 3 chamadas REST + Kafka
- Overhead: delta ms → interpretação arquitetural

### 4.4 Workflow 4 — Falha Parcial Controlada (sustenta H3)

> Este workflow **não é caminho-feliz**: é a injeção deliberada de uma falha a meio do checkout para observar o estado dos dados. Sustenta H3 (existência de estado *zombie* sem Saga).

**Cenário:** o checkout decorre normalmente até ao passo 6, momento em que `payment-service` é parado (`docker stop payment-service`) imediatamente antes de processar o evento `order-placed-events`.

**Figura 12** — Diagrama de "estado zombie": variante do checkout marcando a vermelho onde o sistema fica em estado inconsistente (stock decrementado, encomenda em PENDING, pagamento nunca processado, sem compensação).

**Estado observável após a falha:**

| Tabela / Tópico | Estado esperado | Estado observado |
|------------------|-----------------|-------------------|
| `xcommerce_inventory.stock` | Decrementado (passo 3 já executou) | Decrementado |
| `xcommerce_orders.order` | PENDING (não confirmada nem cancelada) | PENDING — *estado zombie* |
| Kafka topic `payment-events` | Vazio para esta order | Vazio |
| `xcommerce_cart.cart_item` | Limpo | Limpo (irreversível) |

**Lacunas evidenciadas:**
- Sem Saga compensatória: stock não é restaurado
- Sem timeout/dead-letter no `order-service` para detectar pagamento ausente
- Sem operação manual de reconciliação implementada

**Como reproduzir** (instruções no Anexo C, script `T_failure_partial.sh`).

**Hipótese avaliada:** H3 — confirma a limitação da TO-BE em ausência de mecanismos de coordenação distribuída.

---
<!--
## 5. AVALIAÇÃO EXPERIMENTAL

### 5.1 Ambiente Experimental

**O que escrever:**
- Hardware: [specs da máquina — CPU, RAM, OS]
- Docker Compose local vs cloud (indicar qual foi usado para cada teste)
- Ferramentas: k6 / Locust / JMeter (indicar qual)
- **Warm-up obrigatório:** 30s a 1min de carga ligeira antes de iniciar a medição (descartar resultados) — necessário para estabilizar o JIT da JVM e cache de queries Hibernate. Sem isto, monólito aparece artificialmente lento nos primeiros segundos.
- Mínimo 3 execuções por cenário; reportar média ± desvio-padrão; identificar e descartar outliers (ex: > 3σ).
- **Observabilidade durante testes:** Jaeger ativo em todas as execuções microserviços para decompor a latência por hop (Gateway → serviço → BD). Sem isto não é possível interpretar onde está o overhead.

### 5.2 Variáveis Controladas

- Carga: X utilizadores virtuais, Y requests/segundo
- Dados seed: mesmo dataset em monólito e microserviços (mesmos produtos, utilizadores, carrinhos)
- Timeout: igual em ambos os cenários
- Hardware: mesma máquina, sem outras cargas concorrentes
- Tamanho do carrinho fixo (5 itens) para tornar o N+1 reproduzível

### 5.3 Cenários de Teste

Para cada cenário, preencher a tabela:

#### Cenário T1 — Adicionar ao Carrinho (carga normal)

| Campo | Valor |
|-------|-------|
| Objetivo | Medir overhead de latência da decomposição em operação simples |
| Workflow | POST /rest/shoppingCart/addProduct vs POST /cart |
| Carga | X VUs, Y req/s, duração Z min |
| Métricas | p50, p95, p99 latência; throughput; taxa de erro |
| Resultado esperado | Overhead p95 < 50 ms (1 hop extra Gateway + cart-service) |
| *Threshold* refutação | Overhead p95 ≥ 50 ms |
| Resultado observado | [preencher] |
| Interpretação | [preencher — decompor com Jaeger por hop] |
| Hipótese avaliada | H1a |

#### Cenário T2 — Checkout sob carga

| Campo | Valor |
|-------|-------|
| Objetivo | Medir latência e taxa de sucesso do fluxo transacional completo |
| Workflow | GET /rest/order/checkout vs POST /order/checkout |
| Carga | X VUs simultâneos |
| Métricas | p50, p95 latência; taxa de sucesso; consistência do stock após N checkouts |
| Resultado esperado | Microserviços mais lento (3 REST calls + Kafka) |
| *Threshold* refutação | p95 microserviços > 3× p95 monólito |
| Resultado observado | [preencher] |
| Interpretação | [preencher — usar trace Jaeger para decompor por hop] |
| Hipótese avaliada | H1b |

#### Cenário T3 — Isolamento de Falhas (availability)

| Campo | Valor |
|-------|-------|
| Objetivo | Verificar se falha num serviço não afeta os restantes |
| Setup TO-BE | Parar catalog-service; tentar consultar encomendas e fazer checkout |
| Setup AS-IS | Parar BD PostgreSQL; tentar qualquer operação |
| Métricas | Disponibilidade de cada endpoint (% de respostas 2xx vs 5xx) durante 5 min |
| Resultado esperado | TO-BE: consulta encomendas mantém ≥ 99% sucesso; checkout falha (depende de catalog). AS-IS: tudo falha (~0% sucesso). |
| *Threshold* refutação | TO-BE consulta encomendas < 99% sucesso |
| Resultado observado | [preencher] |
| Interpretação | [preencher] |
| Hipótese avaliada | H2 |

#### Cenário T4 — Falha Parcial e Estado Zombie

| Campo | Valor |
|-------|-------|
| Objetivo | Demonstrar que ausência de Saga produz estado inconsistente observável |
| Setup | Executar N checkouts; em cada um, parar `payment-service` antes do consumo do evento |
| Métricas | Nº de orders em PENDING permanente; nº de itens de stock decrementados sem order confirmada |
| Resultado esperado | ≥ 1 caso de stock decrementado sem order confirmada |
| Resultado observado | [preencher] |
| Interpretação | Confirma a limitação assumida em §6.3 |
| Hipótese avaliada | H3 |

#### Cenário T5 — Custo de Recursos

| Campo | Valor |
|-------|-------|
| Objetivo | Comparar consumo CPU/memória entre arquiteturas |
| Métricas | CPU total, memória residente total (RSS), número de processos/containers |
| Monólito | 1 processo Spring Boot + Postgres + Redis |
| Microserviços | 8 containers + Gateway + Postgres + Kafka + Zookeeper |
| Cenários | (a) idle 10min; (b) carga T1 sustentada |
| Resultado esperado | Microserviços ≥ 2× memória idle |
| Resultado observado | [preencher] |
| Interpretação | [preencher] |
| Hipótese avaliada | H5 |

### 5.4 Resultados

**Apresentar por cenário:**
- Tabela com média, desvio-padrão, p50/p95/p99 (≥ 3 execuções)
- Gráfico comparativo monólito vs microserviços (Figura 13 — barras p50/p95/p99 por workflow)
- Gráfico de disponibilidade durante falha controlada (Figura 14 — eixo tempo, % 2xx, T3)
- Gráfico de recursos (Figura 15 — barras CPU/RAM idle e em carga, T5)
- Referência aos CSVs em Anexo D

**Quando mencionar N+1 queries:**
Explicitar: "1 = `SELECT * FROM "order" WHERE id = ?`, N = `SELECT * FROM cart_item WHERE order_id = ?` × N items, M = `SELECT * FROM product WHERE id = ?` × M produtos. Com 5 itens = 11 queries observadas. Evidência: Figura 3 (log SQL anotado)."

---

## 6. DISCUSSÃO CRÍTICA

### 6.1 Resposta às Questões de Investigação

Para cada QI: o que os dados mostram, se confirmam ou refutam a hipótese, que nuance existe.

**QI1 (Performance):**
- A decomposição introduziu X ms de overhead no checkout (Y% face ao monólito)
- Em operações simples de consulta, overhead é Z ms — aceitável ou não?
- Conclusão: microserviços são [mais/menos] lentos, mas o trade-off justifica-se se [condição]

**QI2 (Availability):**
- Isolamento de falhas: catalog down → encomendas ainda funcionam ✓
- Mas: API Gateway down → tudo falha (novo SPOF)
- Conclusão: melhorou isolamento de domínio, mas introduziu novos pontos de falha

**QI3 (Deployability):**
- É possível atualizar catalog-service com `docker compose up -d catalog-service` sem downtime dos outros
- Mas: mudanças de schema da BD lógica ainda afetam apenas o serviço próprio ✓
- Limitação: sem health checks sofisticados, Gateway pode encaminhar para instância em restart

**QI4 (Cost):**
- Monólito: ~X MB RAM, Y% CPU em idle
- Microserviços: ~X MB RAM (soma de todos os containers), Y% CPU em idle
- Overhead operacional: significativamente mais recursos em idle para a mesma carga

### 6.2 Acoplamento: o que foi eliminado vs o que foi deslocado

> **Contribuição central da análise.** Em vez de afirmar "redução do acoplamento", classificamos por **tipo** de acoplamento (ver QI4/H4).

**Figura 16** — Tabela visual "acoplamento eliminado vs deslocado":

| Tipo de acoplamento | Monólito | Microserviços | Veredicto |
|---------------------|----------|---------------|-----------|
| **Estrutural** (FKs JPA, entidades partilhadas) | Forte (CartItem→Product) | Eliminado (apenas `productId`) | ✓ Eliminado |
| **Dados / Schema** (schema partilhado) | Forte (schema único) | Eliminado lógico, mantido físico (uma instância) | ⚠ Parcial |
| **Temporal** (chamadas síncronas obrigatórias) | Implícito na transação | **Deslocado** para REST síncrono (order→inventory→catalog) | ↔ Deslocado |
| **Eventos** (consumo assíncrono) | Não existia | **Introduzido** via Kafka (order↔payment↔notification) | ⊕ Novo |
| **Build-time** (deploy conjunto) | Forte (1 artefacto) | Eliminado (deploy independente por serviço) | ✓ Eliminado |
| **Runtime** (cascata de falhas) | Local (BD única) | **Deslocado** — falha em catalog corta checkout | ↔ Deslocado |
| **Operacional** (infraestrutura) | Postgres | **Aumentado** — Gateway, Kafka, Postgres lógico = mais SPOFs | ⊕ Novo |
| **Sincronização de identidade** | Não existia (auth integrado) | **Introduzido** — auth↔user via internal-sync | ⊕ Novo |

**Conclusão:** A decomposição **não elimina o acoplamento — recategoriza-o**. Eliminam-se acoplamentos estáticos (FKs, schema, deploy) ao custo de introduzir acoplamentos dinâmicos (REST runtime, Kafka eventual, infraestrutura distribuída). O ganho líquido depende do atributo de qualidade priorizado.

### 6.3 Limitações da Solução TO-BE
<!--
**Assumir explicitamente:**
1. Sem Saga compensatória — inconsistência possível em falha parcial no checkout
2. Sem idempotência no inventory-service e payment-service
3. Sem dead-letter queue no Kafka
4. Database per service apenas lógico, não físico
5. Auth/User: janela de inconsistência na criação de utilizador
6. Inventory e Payment são extensões funcionais, não apenas refactoring — comparação não é estritamente equivalente
-->
### 6.4 O que a Decomposição NÃO Resolve

- Consistência distribuída: Kafka asynchronous choreography introduz consistência eventual sem garantias
- Complexidade operacional: 10+ containers para a mesma funcionalidade
- Debugging distribuído: um request de checkout passa por 5+ serviços — Jaeger/Zipkin necessário para rastrear

---

## 7. CONCLUSÕES

### 7.1 Síntese das Decisões Arquiteturais

Tabela: decisão → problema que resolve → custo introduzido → evidência experimental

| Decisão | Problema resolvido | Custo/risco | Evidência |
|---------|-------------------|-------------|-----------|
| Decomposição por bounded context | Acoplamento estrutural entre domínios | Latência adicional (+X ms checkout) | Cenário T1, T2 |
| Database per service (lógico) | Ownership claro dos dados, schema evolution independente | Sem isolamento físico; SPOF na instância PostgreSQL | — |
| Kafka para pagamento/notificações | Desacoplamento temporal; notificações não bloqueiam checkout | Consistência eventual; sem compensação implementada | Cenário T2 |
| REST síncrono para inventory/cart | Resposta necessária para prosseguir checkout | Cascata de falhas se serviço intermediário falhar | Cenário T3 |
| API Gateway + Circuit Breaker | Ponto único de entrada; protecção contra cascata | Novo SPOF; latência extra por hop | Cenário T3 |

### 7.2 Resposta à Questão Principal

> "Em que medida a decomposição arquitetural contribui para melhorar atributos de qualidade como performance, availability, deployability e cost, face à solução monolítica original?"

- **Performance:** [resultado concreto — X% overhead em checkout, Y% em consultas]
- **Availability:** melhora o isolamento de falhas entre domínios, mas introduz novos SPOFs (Gateway, Kafka)
- **Deployability:** melhora significativamente — deploy independente por serviço demonstrado
- **Cost:** aumenta — X containers vs 1, mais recursos em idle, mais complexidade operacional

### 7.3 Trabalho Futuro

- Implementar Saga compensatória com outbox pattern
- Idempotência em inventory-service e payment-service
- Isolamento físico de BD (instância por serviço)
- Dead-letter queue para mensagens Kafka falhadas
- Testes de carga mais elevada (stress testing)

---

## 8. REFERÊNCIAS

Formato ACM. Incluir pelo menos:
- Newman, S. (2021). *Building Microservices* (2nd ed.). O'Reilly.
- Richardson, C. (2018). *Microservices Patterns*. Manning.
- Fowler, M. & Lewis, J. (2014). Microservices. martinfowler.com
- Evans, E. (2003). *Domain-Driven Design*. Addison-Wesley.
- Artigo sobre Saga pattern (Richardson, 2018 ou equivalente)
- Documentação Spring Cloud Gateway, Resilience4j

---

## ANEXOS (ficheiros digitais no Drive — não contam para as 20 páginas)

### Anexo A — Instruções de Execução (README)
- Como correr o monólito (`docker-compose up` em `01-monolith/`)
- Como correr os microserviços (`docker-compose up` em `02-microservices/`)
- Como reproduzir os testes de carga

### Anexo B — Origem do Código
- Monólito: fork de https://github.com/oiraqi/xcommerce-monolithic (Omar IRAQI)
- Microserviços: implementação própria do grupo G11 (listar o que foi escrito do zero vs adaptado)
- Indicar quais serviços foram completamente novos (inventory, payment, notification, bff, web-ui, api-gateway) vs refactoring de lógica existente (auth, user, catalog, cart, order)

### Anexo C — Scripts de Teste
- Scripts k6/Locust/JMeter para cada cenário (T1-T4)
- Instruções de execução

### Anexo D — Resultados Brutos
- CSVs com métricas por cenário
- Tabela de mapeamento: ficheiro CSV → cenário experimental

### Anexo E — Observabilidade
- Screenshots Grafana (dashboards durante testes)
- Screenshots Prometheus (métricas)
- Traces Jaeger (checkout end-to-end)
- Screenshots Kafdrop (tópicos Kafka durante checkout)

### Anexo F — Endpoints Comparativos
- Tabela completa: monólito vs microserviços (ver análise já feita)
- Nota: monólito usava Spring Data REST automático para produtos/categorias/marcas — não havia controllers explícitos

### Anexo G — Auto-avaliação Individual
(1 página por elemento — obrigatório conforme enunciado §8.3)
- Principais tarefas realizadas
- Contributo técnico específico
- Percentagem estimada de participação

---

## CHECKLIST ANTES DE SUBMETER

### Estrutura
- [ ] Formato ACM (Word ou LaTeX)
- [ ] Máximo 20 páginas sem figuras
- [ ] Todas as figuras têm legenda e são referenciadas no texto
- [ ] Referências no formato ACM

### Conteúdo obrigatório (critérios de avaliação)
- [ ] Problema arquitetural central identificado claramente na introdução (§1.1)
- [ ] Hipóteses falsificáveis com *threshold* quantitativo (§1.3)
- [ ] Âmbito do trabalho realizado: o que é nosso vs reutilizado (§1.4)
- [ ] AS-IS e TO-BE separados — sem referências a Kafka/microserviços na secção AS-IS
- [ ] Cada fronteira de serviço justificada concretamente (não só princípios gerais)
- [ ] Auth/User separação justificada com trade-off explícito
- [ ] Inventory e Payment assumidos como extensão funcional
- [ ] Database per service: honestidade sobre ser lógico, não físico
- [ ] Kafka justificado concretamente para cada uso (não "Kafka é usado em microserviços")
- [ ] Limitações assumidas explicitamente (sem Saga, sem idempotência, sem DLQ)
- [ ] N+1 queries explicado com log SQL real **anotado** (Fig. 3)
- [ ] Workflow de **falha parcial controlada** com evidência (§4.4, Fig. 12)
- [ ] Tabela de **acoplamento eliminado vs deslocado** (§6.2, Fig. 16)
- [ ] Para cada hipótese: cenário experimental, métricas, *threshold* refutação, resultado observado
- [ ] Warm-up declarado nos testes; média±desvio sobre ≥ 3 execuções
- [ ] Jaeger usado para decompor latência por hop nos microserviços
- [ ] Todas as 16 figuras presentes e referenciadas no texto

### Anexos (Drive)
- [ ] README de execução
- [ ] Tabela de origem do código
- [ ] Scripts de teste
- [ ] CSVs de resultados
- [ ] Screenshots Grafana/Prometheus/Jaeger
- [ ] Auto-avaliação individual de cada elemento
