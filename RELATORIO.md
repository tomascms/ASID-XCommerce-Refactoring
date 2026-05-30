# Estrutura do Relatório Final: XCommerce: Monólito vs Microserviços
> Template ACM · Máx 20 páginas sem figuras · Deadline: 31/05/2026 23h59 Blackboard
> Critérios: Fundamentação Arquitetural 40% | Avaliação Experimental 30% | Implementação/Deployment 15% | Discussão Crítica 10% | Apresentação 5%

---

## ÍNDICE DE FIGURAS (a incluir no relatório)

| # | Título | Secção | Tipo |
|---|--------|--------|------|
| Fig. 1 | C4 Container: Monólito XCommerce | §2.1 | Diagrama arquitetura |
| Fig. 2 | C4 Component: interior do monólito (packages partilhando EntityManager) | §2.1 | Diagrama arquitetura |
| Fig. 3 | Log SQL Hibernate anotado: N+1 no checkout (5 itens → 11 queries) | §2.2 | Evidência empírica |
| Fig. 4 | C4 Container: Microserviços (8 serviços + Gateway + Kafka + Postgres lógico) | §3.1 | Diagrama arquitetura |
| Fig. 5 | C4 Component: zoom em `order-service` | §3.1 | Diagrama arquitetura |
| Fig. 6 | Topologia dos tópicos Kafka (saga por coreografia) | §3.1 | Diagrama integração |
| Fig. 7 | Papel arquitetural do API Gateway | §3.3 | Diagrama lógico |
| Fig. 8 | Database per Service: instância única, BDs lógicas separadas | §3.4 | Diagrama infra |
| Fig. 9 | Sequence: adicionar ao carrinho (TO-BE) | §4.1 | Sequence diagram |
| Fig. 10 | Sequence comparativo: consultar encomendas (AS-IS vs TO-BE) | §4.2 | Sequence diagram |
| Fig. 11 | Sequence: checkout completo (REST + Kafka, pontos de falha) | §4.3 | Sequence diagram |
| Fig. 12 | Diagrama de "estado zombie": falha parcial controlada | §4.4 | Diagrama de falha |
| Fig. 13 | Latência p50/p95/p99: monólito vs microserviços (3 workflows) | §5.4 | Gráfico barras |
| Fig. 14 | Disponibilidade durante falha (T3): % respostas 2xx ao longo do tempo | §5.4 | Gráfico linhas |
| Fig. 15 | Consumo CPU/RAM: idle e em carga (T5) | §5.4 | Gráfico barras |
| Fig. 16 | Tabela visual: acoplamento eliminado vs deslocado | §6.2 | Tabela analítica |

> **Anexos digitais (no Drive, não contam para 20 páginas):** screenshots Grafana/Prometheus/Jaeger/Kafdrop, traces completos, logs brutos. Detalhes em §Anexos.

---

## ABSTRACT

Este trabalho estuda empiricamente os trade-offs da transição de uma arquitetura monolítica para microserviços, usando o XCommerce (uma aplicação de comércio eletrónico em Spring Boot) como caso de estudo. O problema central é o acoplamento estrutural do fluxo de checkout: no monólito, uma única transação JPA agrega validação de utilizador, leitura de catálogo, esvaziamento de carrinho e criação de encomenda, tornando qualquer falha num domínio num ponto único de falha para todos os outros. A solução proposta decompõe o sistema em oito serviços autónomos delimitados por Bounded Contexts do DDD, com comunicação mista REST/Kafka e API Gateway com circuit breakers. Os resultados experimentais mostram que o checkout no monólito tem latência p95 de 11.4 ms (1 VU); o isolamento de falhas é confirmado: a paragem da base de dados do monólito derruba 100% dos endpoints em menos de 2 segundos, enquanto nos microserviços a falha de um serviço não afeta os restantes. O custo é real: 17 containers consomem ~4.9× mais memória em idle. A decomposição não elimina o acoplamento, recategoriza-o de estático para dinâmico, transferindo-o para dependências REST em runtime e consistência eventual via Kafka.

---

## 1. INTRODUÇÃO

As arquiteturas de software para sistemas de comércio eletrónico enfrentam um dilema recorrente: à medida que o sistema cresce, o monólito que inicialmente simplificou o desenvolvimento torna-se um obstáculo à evolução independente dos seus domínios funcionais. A literatura de engenharia de software propõe a arquitetura de microserviços como resposta a este problema, mas a transição não é isenta de custos: introduz latência de rede, consistência eventual e complexidade operacional que, em certos contextos, podem superar os benefícios obtidos.

### 1.1 Problema Arquitetural

Este trabalho estuda empiricamente essa transição a partir de um caso concreto: o **XCommerce**, uma aplicação de comércio eletrónico implementada como monólito em Spring Boot, onde autenticação, catálogo de produtos, carrinho de compras e processamento de encomendas partilham um único processo JVM e uma única instância de base de dados PostgreSQL.

O problema arquitetural central reside no **acoplamento estrutural do fluxo de checkout**: esta operação executa, na mesma transação JPA, a validação do utilizador, a leitura do catálogo, o esvaziamento do carrinho e a criação da encomenda. Daqui resultam dois problemas concretos:

- **Ponto único de falha:** uma degradação em qualquer domínio (por exemplo, sobrecarga no processamento de encomendas) propaga-se imediatamente a todos os restantes, incluindo autenticação e consulta de catálogo.
- **Deployability acoplada:** qualquer alteração ao código de um módulo obriga ao redeployment completo da aplicação, interrompendo temporariamente todas as funcionalidades, independentemente da dimensão da alteração.

A questão central é: **a decomposição em microserviços melhora o isolamento de falhas e a deployability independente por domínio, e a que custo em termos de latência e complexidade operacional?**

### 1.2 Alternativas Arquiteturais Consideradas

Para resolver este problema foram consideradas três abordagens:

**Monólito Modular.** Introduz separação lógica entre módulos mantendo um único artefacto de deployment. Reduz o acoplamento de código mas não elimina o ponto único de falha operacional nem permite scaling ou redeployment independente por domínio.

**SOA com ESB.** Decompõe o sistema em serviços integrados por um Enterprise Service Bus centralizado, responsável pelo roteamento, transformação de mensagens e orquestração de workflows. O problema desta abordagem é que o próprio barramento se torna um novo ponto único de falha: desloca o problema original sem o resolver. Adicionalmente, segue o princípio de *smart pipes, dumb endpoints*, concentrando lógica de integração num componente externo aos serviços, o que cria dependência forte no barramento e dificulta a evolução independente de cada serviço.

**Microserviços com DDD** *(abordagem adotada).* Cada domínio funcional é implementado como um serviço autónomo, com a sua própria base de dados e ciclo de deployment independente. As fronteiras entre serviços são definidas por **Bounded Contexts** do Domain-Driven Design: cada serviço modela apenas o conceito relevante dentro da sua fronteira, sem partilhar entidades com outros serviços. Por exemplo, `Product` no catalog-service contém nome, descrição e atributos; no cart-service existe apenas `productId + quantity`, sem entidade JPA partilhada, sem acoplamento estrutural. Esta abordagem permite avaliar empiricamente os trade-offs de isolamento de falhas, latência inter-serviço e deployability que motivam a decomposição.

### 1.3 Questões de Investigação e Hipóteses

O trabalho é orientado por três questões de investigação:

| | Questão | Hipótese | Critério de validação |
|---|---|---|---|
| **QI1** | Uma falha num serviço fica contida ao seu domínio sem afetar os restantes? | H1: uma falha no catalog-service não afeta auth-service nem cart-service. | ≥ 95% de sucesso em `/auth/login` e `/cart` com catalog-service parado. |
| **QI2** | Qual o overhead de latência das chamadas inter-serviço face à execução in-process do monólito? | H2: o checkout nos microserviços tem latência média ≥ 2× superior à do monólito. | Medição de P50 e P95 em `/order/checkout` sob carga equivalente nas duas arquiteturas. |
| **QI3** | É possível reimplantar um serviço individualmente sem causar indisponibilidade nos restantes? | H3: o redeployment do catalog-service não causa erros nos restantes serviços. | Taxa de erro < 1% em `/cart` e `/auth/login` durante restart controlado do catalog-service. |

### 1.4 Estrutura do Documento

- **Secção 2:** arquitetura AS-IS do monólito e problemas arquiteturais observados
- **Secção 3:** arquitetura TO-BE em microserviços, decisões de decomposição e trade-offs introduzidos
- **Secção 4:** desenho experimental
- **Secção 5:** resultados e discussão
- **Secção 6:** conclusões, limitações e trabalho futuro

---

## 2. SISTEMA DE REFERÊNCIA: AS-IS (Monólito)

> **REGRA CRÍTICA:** Nesta secção NÃO aparecem Kafka, InventoryService, chamadas HTTP entre serviços, nem nada da solução TO-BE. Apenas o que foi *observado* no monólito original.

### 2.1 Arquitetura do Monólito XCommerce

O XCommerce original é uma aplicação de comércio eletrónico implementada como monólito em Spring Boot 2.1.9 / Java 8. Todo o código (autenticação, catálogo de produtos, carrinho de compras e processamento de encomendas) é compilado e implantado num único artefacto JAR, executado num único processo JVM.

A persistência é assegurada por uma única instância PostgreSQL partilhada por todos os domínios funcionais. O carrinho de compras utiliza Redis como cache em memória. A autenticação é baseada em JWT, gerida transversalmente por Spring Security.

Internamente, o código organiza-se em 33 classes distribuídas por 5 packages:

| Package | Conteúdo |
|---------|----------|
| `controllers` | `UserRestController`, `OrderRestController`, `ShoppingCartRestController` |
| `services` | `UserService`, `OrderService`, `ShoppingCartService` |
| `data/entities` | `User`, `Order`, `OrderLine`, `Product`, `Brand`, `Category`, `Review`, `Authority`, `BaseEntity`, `OrderStatus`, `IllegalStatusChangeException` |
| `data/repositories` | `UserRepository`, `OrderRepository`, `ProductRepository`, `BrandRepository`, `CategoryRepository`, `ReviewRepository`, `AuthorityRepository`, `BaseRepository` |
| `security` | `JwtHelper`, `JwtInterceptingFilter`, `SecurityConfigurer`, `UserAuditor` |

Esta estrutura segue um padrão MVC-SOA: existe separação lógica entre camadas, mas todos os módulos partilham o mesmo `EntityManager` JPA e o mesmo schema de base de dados. Não existe qualquer fronteira de deployment ou de falha entre os domínios.

O acoplamento entre domínios é estrutural: `OrderLine` referencia diretamente a entidade JPA `Product`; o `ShoppingCart` armazena em cache Redis a entidade `Product` completa, incluindo preço, desconto, peso e relações com `Brand` e `Category`. Qualquer módulo pode aceder a dados de qualquer outro domínio sem restrição.

Do ponto de vista operacional, uma alteração em qualquer módulo (mesmo que mínima) obriga ao redeployment completo da aplicação. Não existe mecanismo de atualização independente por domínio.

> **Figura 1:** C4 Nível 2 (Container): Browser → Spring Boot Application → PostgreSQL; Spring Boot Application → Redis.

> **Figura 2:** C4 Nível 3 (Component): packages `controllers`, `services`, `data/entities` e `security` dentro do container Spring Boot, todos partilhando o mesmo `EntityManager` JPA apontado para o schema PostgreSQL único. Esta figura evidencia o acoplamento estrutural analisado na secção seguinte.

### 2.2 Problemas Arquiteturais Observados

**Acoplamento total no checkout.** Quando um utilizador faz checkout, o sistema executa numa única operação: valida o utilizador, lê o carrinho, percorre os produtos e cria a encomenda. Uma falha em qualquer um destes passos (por exemplo, sobrecarga no processamento de encomendas) derruba simultaneamente todas as funcionalidades da aplicação, incluindo operações completamente independentes como a consulta do catálogo.

Este fluxo expõe ainda um problema de **N+1 queries**: para serializar uma encomenda com 5 itens, o sistema emite 11 queries à base de dados em vez de uma, concretamente uma query para a encomenda, cinco para as linhas de encomenda, e cinco para os produtos correspondentes. Com o crescimento do número de itens, o número de queries cresce proporcionalmente.

> **Figura 3:** Log SQL Hibernate anotado: sequência de 11 queries durante checkout de 5 itens.

**Escalabilidade monolítica.** O catálogo de produtos é tipicamente o domínio com maior volume de tráfego, enquanto o checkout é uma operação pontual. No monólito não é possível escalar um domínio de forma independente: aumentar a capacidade do catálogo implica replicar toda a aplicação.

**Identidade do utilizador hardcoded.** `OrderRestController` e `ShoppingCartRestController` operam sempre com o identificador de utilizador fixo em `1`. A extração da identidade a partir do token JWT está comentada no código com a nota `// To be removed`. O sistema não suporta múltiplos utilizadores em simultâneo.

**Stock não validado.** O campo de stock existe na entidade `Product`, mas o fluxo de checkout não o verifica nem o atualiza. Uma compra é registada independentemente da disponibilidade do produto. Esta é uma limitação funcional do sistema original, não um problema introduzido pela arquitetura.

---

### 2.3 Dependências Estruturais (AS-IS)

| Dependência | Manifestação |
|-------------|-------------|
| Carrinho ↔ Catálogo | O carrinho guarda o objeto `Product` completo em cache (preço, peso, desconto, categoria e marca) em vez de guardar apenas o identificador do produto |
| Encomenda ↔ Todos os domínios | O checkout acede em sequência ao utilizador, ao carrinho e aos produtos, criando a encomenda numa única operação sem isolamento entre domínios |
| Autenticação ↔ Domínio | A camada de segurança é transversal a todos os controladores; a integração com JWT ficou incompleta, evidenciada pelo uso de `@WithMockUser` no controlador do carrinho |

---

## 3. ARQUITETURA TO-BE: Microserviços

> **REGRA CRÍTICA:** Esta secção apresenta *decisões* que tomámos e os *trade-offs* que introduzem. Separar claramente o que resolvemos e o que ficou mais complexo.

### 3.1 Visão Geral da Decomposição

A arquitetura TO-BE decompõe o XCommerce em oito serviços de negócio autónomos, um API Gateway e um serviço de composição de API (api-composition-service), todos orquestrados via Docker Compose numa rede interna `xcommerce-network`. O princípio de decomposição é o **Bounded Context** do Domain-Driven Design: cada serviço modela apenas os conceitos do seu domínio, expõe a sua própria API e é o único dono da sua base de dados lógica.

A comunicação inter-serviço é **mista por design**:

- **REST síncrono via OpenFeign:** usado quando a resposta é necessária para prosseguir. O `order-service` tem três Feign clients: `InventoryClient`, `CatalogClient` e `CartClient`. Durante o checkout, estas três chamadas executam em série antes de a encomenda ser criada.
- **Kafka assíncrono por coreografia:** usado quando o emissor não precisa de esperar pela resposta. Após criar a encomenda, o `order-service` publica eventos e retorna imediatamente ao cliente. O processamento de pagamento e as notificações ocorrem de forma independente.

A persistência segue o padrão *database per service*: uma única instância PostgreSQL com oito bases de dados lógicas isoladas, sendo que cada serviço acede apenas à sua (ver §3.4 para os limites desta abordagem).

A topologia dos tópicos Kafka é a seguinte:

| Tópico | Produtor | Consumidor(es) | Conteúdo |
|--------|----------|---------------|---------|
| `order-placed` | order-service | (nenhum) | Identificador de encomenda + username |
| `order-placed-events` | order-service | payment-service (`payment-group`), notification-service (`notification-group`) | Identificador + dados de inventário |
| `payment-events` | payment-service | order-service (`order-group`), notification-service (`notification-group`) | `PAYMENT_SUCCESSFUL` ou `PAYMENT_FAILED` |
| `order-confirmed-events` | order-service | notification-service (`notification-group`) | Identificador de encomenda confirmada |
| `order-cancelled-events` | order-service | notification-service (`notification-group`) | Identificador de encomenda cancelada |
| `product-events` | catalog-service | notification-service (`notification-group`) | Eventos de produto |
| `user-events` | auth-service | notification-service (`notification-group`) | Eventos de utilizador |

> **Figura 4:** C4 Nível 2: Gateway (porta 9000) → 8 serviços de negócio + api-composition-service; setas sólidas = REST síncrono (OpenFeign); setas tracejadas = Kafka; Postgres lógico com 8 BDs.

> **Figura 5:** C4 Nível 3 (zoom em `order-service`): controller REST → `OrderService` → Feign clients (`InventoryClient`, `CatalogClient`, `CartClient`) + `KafkaTemplate` (producer) + `@KafkaListener` (consumer de `payment-events`) → repositório `xcommerce_orders`.

> **Figura 6:** Topologia Kafka: produtores e consumidores por tópico, com indicação do `groupId` de cada consumer group.

**Tabela de serviços:**
| Serviço | Porta | BD | Bounded Context | Justificação da fronteira |
|---------|-------|-----|-----------------|--------------------------|
| auth-service | 8081 | xcommerce_auth | Autenticação/sessão | Ciclo de vida independente; pode ser substituído por Keycloak sem afetar outros |
| catalog-service | 8082 | xcommerce_catalog | Produtos, marcas, categorias | Maior carga de leitura; pode escalar independentemente |
| cart-service | 8083 | xcommerce_cart | Estado volátil de carrinho | Dados efémeros; Redis seria alternativa; isolável |
| order-service | 8084 | xcommerce_orders | Ciclo de vida de encomendas | Estado transacional crítico; fronteira natural de negócio |
| inventory-service | 8085 | xcommerce_inventory | Gestão de stock | **Extensão funcional:** não existia no monólito (ver 3.3) |
| notification-service | 8088 | (n/a) | Envio de notificações | Assíncrono por natureza; falha não deve bloquear o fluxo principal |
| user-service | 8086 | xcommerce_users | Perfis de utilizador | Separado de auth (ver trade-off 3.2) |

> Não basta enunciar princípios gerais. Para cada fronteira: que problema resolve, que alternativa existia, que trade-off introduz.

#### Cart / Catalog
- **Problema que resolve:** No monólito, CartItem guarda entidade JPA Product completa, criando acoplamento estrutural. Cart e Catalog partilham o mesmo schema.
- **Decisão:** Cart passa a guardar apenas `productId` e `quantity`. Elimina-se o acoplamento estrutural.
- **Alternativa:** Manter Cart e Catalog no mesmo serviço (CatalogCart service mais agregado).
- **Trade-off introduzido:** Cart deixa de ter acesso direto a dados do produto (nome, preço). Para mostrar o carrinho com detalhes, order-service precisa de chamar catalog-service via REST (dependência em runtime). Se catalog-service falhar, o carrinho mostra apenas IDs.

#### Auth / User
- **Problema que resolve:** No monólito, autenticação e perfil de utilizador estão acoplados. Auth pode evoluir para OAuth2/Keycloak sem tocar nos dados de perfil.
- **Decisão:** auth-service gere credenciais (username/password/JWT); user-service gere perfis (nome, morada, role).
- **Alternativa:** Um único identity-service com ambas as responsabilidades.
- **Trade-off introduzido:** Introduz sincronização via `POST /auth/internal/sync` e `POST /users/internal/sync`. Quando um utilizador se regista, auth-service publica evento Kafka → user-service cria o perfil. **Risco:** janela de inconsistência entre criação de credenciais e criação de perfil. Sem compensação implementada, se user-service falhar após auth criar a conta, o utilizador existe em auth mas não tem perfil.

#### Inventory (extensão funcional: não era refactoring)
- **O que era no monólito:** campo `quantity` em Product, não validado no checkout
- **Decisão:** Criar inventory-service que valida e decrementa stock no checkout
- **Justificação:** Completar a semântica funcional era necessário para tornar o sistema comparável em condições reais
- **Trade-off introduzido:** order-service → inventory-service via REST síncrono (OpenFeign) antes de confirmar encomenda. Se inventory-service falhar, checkout falha. Sem compensação implementada (não há Saga com rollback de stock).

#### Payment (extensão funcional: não era refactoring)
- **Decisão:** Assíncrono via Kafka: notificações não bloqueiam resposta ao utilizador
- **Alternativa:** Chamada REST síncrona após checkout
- **Trade-off:** Se Kafka falhar, notificações perdem-se (sem dead-letter queue implementada).

### 3.3 Papel do API Gateway

Sem um ponto de entrada centralizado, o cliente precisaria de conhecer os endereços de 8 serviços distintos, gerir o token JWT em cada chamada e lidar individualmente com falhas de cada serviço. O API Gateway resolve este problema ao ser o único componente com o qual o cliente comunica diretamente.

As suas quatro responsabilidades são:

**1. Ponto único de entrada.** Toda a comunicação externa entra pela porta 9000. O cliente nunca sabe que existem serviços separados por baixo: a estrutura interna da aplicação fica completamente encapsulada.

**2. Routing por path.** Cada pedido é encaminhado para o serviço correto com base no caminho do URL: `/products/**` vai para o `catalog-service`, `/rest/order/**` vai para o `order-service`, e assim por diante.

**3. Autenticação e autorização.** O Gateway valida a assinatura do token JWT em cada pedido, extrai o utilizador e o seu papel, e injeta os headers `X-User-Name` e `X-User-Role` antes de encaminhar o pedido. Os serviços internos confiam nesses headers sem voltar a validar o token: a responsabilidade de autenticação está centralizada num único ponto.

**4. Resiliência por rota.** Cada rota tem um circuit breaker com timeout de 5 segundos via Resilience4j. Quando um serviço demora a responder ou falha, o Gateway ativa um endpoint de fallback (`/fallback/*`) que devolve uma resposta degradada em vez de propagar o erro ao cliente.

O Gateway introduz contudo dois riscos. É um **ponto único de falha**: se ficar indisponível, toda a aplicação fica inacessível. Em produção este risco seria mitigado com múltiplas instâncias, mas no protótipo corre apenas uma. Acrescenta também **latência adicional** em cada pedido, uma vez que todos passam por mais uma camada antes de chegar ao serviço destino.

É importante notar que o circuit breaker protege contra cascatas de falhas, mas não resolve problemas de consistência de dados. Se o `order-service` aceitar um pedido e o `inventory-service` falhar a seguir, o circuit breaker não desfaz a encomenda já criada.

> **Figura 7:** Diagrama lógico do Gateway: pedido do cliente → `AuthenticationFilter` (validação JWT, injeção de headers) → routing por path → circuit breaker → serviço destino ou fallback.

### 3.4 Database Per Service: Honestidade sobre a Implementação

O isolamento de dados entre serviços é uma consequência direta da decomposição por domínios. Se o `order-service` pudesse aceder diretamente à base de dados do `catalog-service` para ler preços, estaria acoplado ao schema interno desse serviço: qualquer alteração ao schema do catálogo poderia partir o serviço de encomendas. O mesmo problema que existia no monólito, onde todos os domínios partilhavam o mesmo schema, reapareceria agora entre serviços. O princípio é simples: cada serviço comunica com os outros através de APIs, nunca diretamente pela base de dados. Isto garante que cada serviço pode alterar o seu schema, migrar a sua base de dados ou mudar de tecnologia de armazenamento sem quebrar os outros.

O padrão *database per service* estabelece exatamente isto: cada serviço é o único dono dos seus dados. Para o implementar, existiam três abordagens possíveis. A primeira seria uma **instância PostgreSQL por serviço**, com isolamento físico completo, onde a falha da base de dados de um serviço não afeta os restantes. O custo é operacional: 8 instâncias a correr em simultâneo num ambiente local tornaria o protótipo pesado e difícil de gerir. A segunda seria usar **tecnologias de armazenamento diferentes por serviço**, por exemplo Redis para o carrinho (dados efémeros e de acesso frequente) e PostgreSQL para os restantes. Esta abordagem seria a mais fiel ao padrão em produção, mas introduziria complexidade de operação e observabilidade fora do âmbito deste trabalho. A terceira (a escolhida) é uma **única instância PostgreSQL com bases de dados lógicas separadas**.

O que foi implementado são oito bases de dados lógicas: `xcommerce_auth`, `xcommerce_catalog`, `xcommerce_cart`, `xcommerce_orders`, `xcommerce_inventory`, `xcommerce_users`, `xcommerce_payment` e `xcommerce_notification`. Cada serviço está configurado para aceder apenas à sua, não existem foreign keys cruzadas entre serviços, e cada schema pode evoluir de forma independente. Para um protótipo cujo foco é avaliar os trade-offs arquiteturais da decomposição, o isolamento lógico é suficiente para demonstrar os benefícios de ownership independente dos dados.

O que esta abordagem **não garante** é o isolamento físico. A instância PostgreSQL é uma dependência operacional partilhada: se falhar, todos os serviços que dependem de base de dados ficam simultaneamente indisponíveis. Não existem transações ACID que abranjam dois serviços: se durante o checkout o stock for decrementado e o pagamento falhar a seguir, não há rollback automático que restaure o stock.

> **Figura 8:** Diagrama *Database per Service* (lógico): uma instância PostgreSQL contendo as oito bases de dados lógicas, com cada serviço a aceder apenas à sua. A figura torna visualmente explícita a distinção entre isolamento lógico e isolamento físico.

### 3.5 Comunicação Inter-Serviço: Kafka vs REST

A escolha entre comunicação síncrona e assíncrona não foi uniforme: dependeu de uma questão concreta para cada caso: o serviço que faz o pedido precisa da resposta para continuar?

**REST síncrono** foi utilizado nas situações em que a resposta é necessária para prosseguir o fluxo. Durante o checkout, o `order-service` precisa de saber se há stock disponível antes de criar a encomenda: sem essa resposta, não pode avançar. Da mesma forma, precisa de obter o preço atual do produto a partir do `catalog-service` e os itens do carrinho a partir do `cart-service`. Estas três chamadas são feitas via OpenFeign, de forma síncrona e bloqueante.

O trade-off é direto: cada chamada REST adiciona latência ao checkout, e se qualquer um destes serviços estiver indisponível, o checkout falha. É uma dependência em runtime que não existia no monólito, onde tudo corria no mesmo processo.

**Kafka assíncrono** foi utilizado nas situações em que a resposta não é necessária para continuar. Após a encomenda ser criada, o `order-service` publica os eventos e a resposta ao utilizador não precisa de esperar pelo processamento do pagamento nem pelo envio de notificações. O fluxo completo é o seguinte:

1. `order-service` publica em `order-created-events` com o identificador da encomenda e itens para reserva de stock.
2. `inventory-service` consome esse evento, tenta reservar stock e publica o resultado em `stock-reservation-events`
3. `order-service` consome `stock-reservation-events`: se o stock foi reservado com sucesso, a encomenda passa a `CONFIRMED`; se falhou, passa a `CANCELLED`.

Esta abordagem introduz três limitações que não foram resolvidas e que são assumidas explicitamente.

**Entrega duplicada.** O Kafka garante entrega *at-least-once*: em caso de falha ou reprocessamento, a mesma mensagem pode ser entregue mais do que uma vez. Se o `payment-service` receber o mesmo evento de encomenda duas vezes, processa dois pagamentos para a mesma encomenda. A solução seria implementar idempotência: cada mensagem teria um identificador único e o serviço verificaria se já a processou antes de agir. Não está implementado.

**Ausência de compensação.** O checkout decorre em dois momentos distintos: o stock é decrementado por REST síncrono antes de a encomenda ser criada, e o pagamento é processado depois de forma assíncrona. Se o pagamento falhar, a encomenda fica cancelada mas o stock não é reposto: ficou decrementado sem que nenhuma venda tenha sido concluída. A solução seria implementar o padrão **Saga**: uma sequência de transações locais coordenadas, onde cada passo tem uma ação compensatória definida. Se o pagamento falhar, a Saga dispararia automaticamente a reposição do stock. Não está implementado.
- **Ausência de compensação.** O checkout decorre em dois momentos distintos: o stock é decrementado por REST síncrono antes de a encomenda ser criada. Se a criação da encomenda falhar após o stock ter sido decrementado, o stock não é reposto: ficou decrementado sem que nenhuma venda tenha sido concluída. A solução seria implementar o padrão **Saga**: uma sequência de transações locais coordenadas, onde cada passo tem uma ação compensatória definida. Se a criação da encomenda falhar, a Saga dispararia automaticamente a reposição do stock. Não está implementado.
**Mensagens perdidas.** Se o `notification-service` ou o `payment-service` falharem ao processar uma mensagem (por exemplo, por um erro interno ou por estarem temporariamente indisponíveis) essa mensagem perde-se sem hipótese de recuperação. A solução seria configurar uma **dead-letter queue**: mensagens que falham após N tentativas seriam encaminhadas para uma fila separada, onde poderiam ser inspecionadas e reprocessadas manualmente. Não está configurado em nenhum serviço.

---

## 4. ANÁLISE DE WORKFLOWS

> Workflows representativos: dois "caminho-feliz" (consulta + transacional) e um de **falha parcial controlada**. Para cada um: owner dos dados, dependências, falhas possíveis, estado inconsistente, mitigação, lacuna.

### 4.1 Workflow 1: Adicionar Produto ao Carrinho (simples)

**Figura 9:** Sequence diagram: cliente → Gateway → cart-service → (opcional) catalog-service.

| Aspeto | Monólito | Microserviços |
|--------|----------|---------------|
| Owner dos dados | Único schema | cart-service (cartItem), catalog-service (product) |
| Queries | 1 query Product + 1 INSERT CartItem | REST GET /products/{id} + INSERT local |
| Falha possível | BD única falha → tudo para | catalog-service falha → cart-service não consegue validar produto |
| Estado inconsistente | Não (transação local) | CartItem com productId de produto inexistente (se catalog falhar após validação) |
| Mitigação | (n/a) | Circuit Breaker no Gateway; cart-service pode aceitar sem validação |
| Lacuna | (n/a) | Sem validação de existência de produto em runtime no cart-service |

**Comparação de latência T1:**
- Monólito `GET /rest/catalog/products`: p50 = 8.8 ms, p95 = 16 ms, 1 query JPA, sem rede
- Microserviços `GET /products`: p50 = 11.2 ms, p95 = 19.8 ms, Gateway + catalog-service + BD lógica; overhead p95 = +3.8 ms (+24%)

### 4.2 Workflow 2: Consultar Encomendas (simples)

**Figura 10:** Sequence diagram comparativo: monólito (uma transação JPA com N+1) vs microserviços (uma chamada local a `xcommerce_orders`, sem JOIN cruzado).

**No monólito:**
- `GET /rest/order/list` → userId hardcoded = 1 → SELECT order + N queries lazy-load por cada OrderLine para Product + Brand + Category
- N+1 problem: `3 + 2N` queries, evidenciado em T4 com N=4 produtos → 11 queries

**Nos microserviços:**
- `GET /order/list` com header X-User-Name → order-service consulta apenas xcommerce_orders
- Sem JOIN cruzado; dados de produto não são retornados (apenas productId)
- **Trade-off:** resposta mais pobre em dados mas sem dependência de catalog-service

### 4.3 Workflow 3: Checkout (transacional crítico)

**Figura 11:** Sequence diagram completo do checkout TO-BE: 8 passos, REST síncronos (linhas sólidas) + eventos Kafka (tracejadas). Pontos de inconsistência possível marcados a vermelho.

**Passo a passo com ownership:**

| Passo | Serviço | Owner dos dados | Falha possível | Consequência |
|-------|---------|-----------------|----------------|--------------|
| 1. Autenticar | auth-service | xcommerce_auth | auth falha | Checkout bloqueado: aceitável |
| 2. Ler carrinho | cart-service | xcommerce_cart | cart falha | Checkout bloqueado: aceitável |
| 3. Verificar stock | inventory-service (REST) | xcommerce_inventory | inventory falha | Checkout bloqueado; stock não decrementado: consistente |
| 4. Criar order | order-service | xcommerce_orders | order falha após stock decrementado | **Inconsistência: stock decrementado sem order criada** |
| 5. Publicar evento Kafka | order-service | (n/a) | Kafka indisponível | Order criada mas pagamento nunca processado: **estado zombie** |
| 7. Confirmar order | order-service (Kafka listener) | xcommerce_orders | (n/a) | |
| 8. Notificar | notification-service (Kafka) | (n/a) | Mensagem perdida | Utilizador não recebe email: aceitável (não crítico) |

**Limitações assumidas:**
- Não há Saga compensatória: passos 3→4 e 4→5 não têm rollback
- Sem idempotência no inventory-service e payment-service
- Sem dead-letter queue para mensagens Kafka falhadas

**Comparação de latência checkout:**
- Monólito: p50 = 7.1 ms, p95 = 11.4 ms, 1 transação JPA local + 11 queries (N+1) + escrita Redis
- Microserviços: p50 = 19.9 ms, p95 = 32.8 ms, Gateway + order-service + 3 chamadas REST síncronas (inventory/cart) + Kafka publish; overhead p95 = +21.4 ms (+188%)
- H2 **confirmada**: p95 microserviços = 32.8 ms = **2.9× o monólito** (threshold = 2×)

### 4.4 Workflow 4: Falha Parcial Controlada (sustenta H3)

> Este workflow **não é caminho-feliz**: é a injeção deliberada de uma falha a meio do checkout para observar o estado dos dados. Sustenta H3 (existência de estado *zombie* sem Saga).

**Cenário:** o checkout decorre normalmente até ao passo 6, momento em que `payment-service` é parado (`docker stop payment-service`) imediatamente antes de processar o evento `order-placed-events`.

**Figura 12:** Diagrama de "estado zombie": variante do checkout marcando a vermelho onde o sistema fica em estado inconsistente (stock decrementado, encomenda em PENDING, pagamento nunca processado, sem compensação).

**Estado observável após a falha (order id=139, produto 1, quantity=1):**

| Tabela / Tópico | Estado esperado (sem falha) | Estado observado |
|------------------|----------------------------|-------------------|
| `xcommerce_inventory.inventory` | Stock decrementado via REST síncrono (passo 3 executou) | **304 → 302 (−1 unidade)** |
| `xcommerce_orders.orders` | CONFIRMED após pagamento | **HANDLING**, *estado zombie*: payment-service parado antes de consumir o evento |
| Kafka topic `order-placed-events` | Consumido por payment-service | **Não consumido**: consumer group sem instância activa |
| Kafka topic `payment-events` | Evento PAYMENT_SUCCESSFUL publicado | **Vazio**: nunca publicado |
| Carrinho | Limpo (checkout executou) | **Limpo**: irreversível |

O stock decrementou 2 unidades no output do script porque o teste foi executado duas vezes: cada execução decrementa exactamente 1. A inconsistência por execução é: −1 de stock sem order confirmada.

**Lacunas evidenciadas:**
- Sem Saga compensatória: stock não é restaurado
- Sem timeout/dead-letter no `order-service` para detectar pagamento ausente
- Sem operação manual de reconciliação implementada

**Como reproduzir** (script `t-micro-4-estado-zombie.sh` em `02-microservices/testes/`, automático).

O script faz checkout, para o `payment-service` em menos de 1 segundo (antes dos 2 s de delay de processamento no `PaymentProcessor.java:30`), verifica o estado na BD e confirma o stock decrementado sem order confirmada.

**Hipótese avaliada:** H3: confirma a limitação da TO-BE em ausência de mecanismos de coordenação distribuída.

---
<!--
## 5. AVALIAÇÃO EXPERIMENTAL

### 5.1 Ambiente Experimental

Os testes foram executados numa máquina local Apple M4 Pro com 48 GB de RAM e macOS 25.4. Ambos os sistemas correm em Docker: o monólito com 3 containers (`monolith-app`, `monolith-db`, `monolith-redis`) na porta 18080; os microserviços com 11+ containers na porta 9000. As duas stacks correm em simultâneo em redes Docker distintas, sem interferência.

A ferramenta de carga é **k6** (v0.56). Cada cenário de latência inclui 30 segundos de warm-up antes da janela de medição, necessário para estabilizar o JIT da JVM e o cache de queries Hibernate. Os resultados reportados correspondem à janela de medição (60 segundos), com 1 execução por cenário dado que as execuções repetidas foram consistentes (variação < 5% em p95).

**T1 (leitura):** 10 VUs concorrentes, 60 s de medição, endpoint `GET /rest/catalog/products`.  
**T2 (checkout):** 1 VU, 60 s de medição, endpoint `GET /rest/order/checkout` precedido de `PATCH /rest/shoppingCart/addProduct`. O checkout usa 1 VU porque o monólito tem `userId=1` hardcoded em todos os controllers: múltiplos VUs partilhariam o mesmo carrinho Redis e criariam condições de corrida artificiais que não existiriam num sistema com autenticação real.  
**T3 (falha):** teste manual com dois terminais, um em loop de pedidos a 1 req/s, outro executa `docker stop monolith-db`.  
**T5 (recursos):** `docker stats --no-stream` em idle (10 s sem carga) e a meio de uma execução T1 com 10 VUs.

### 5.2 Variáveis Controladas

- **Carga T1:** 10 VUs, ramp-up gradual de 30 s, 60 s de medição, 10 s cool-down
- **Carga T2:** 1 VU, mesma estrutura de fases
- **Dataset:** mesmos produtos e utilizadores seed em monólito e microserviços
- **Carrinho T2:** produto id=0, quantity=1 adicionado antes de cada checkout
- **Hardware:** mesma máquina para ambas as arquiteturas, sem carga concorrente relevante durante os testes

### 5.3 Cenários de Teste

#### Cenário T1: Leitura do catálogo (carga normal)

| Campo | Valor |
|-------|-------|
| Objetivo | Medir latência base em operação de leitura simples: 1 query à BD, sem lógica de negócio |
| Endpoint monólito | `GET /rest/catalog/products` |
| Endpoint microserviços | `GET /products` (Gateway → catalog-service) |
| Carga | 10 VUs, 60 s de medição após 30 s warm-up |
| Resultado monólito | p50 = 8.8 ms · p95 = 16 ms · erro = 0% · ~1580 req |
| Resultado microserviços | p50 = 11.2 ms · p95 = 19.8 ms · erro = 0% · ~800 req · overhead p95 = +3.8 ms (+24%) |
| Hipótese avaliada | H2: overhead de comunicação inter-serviço |

#### Cenário T2: Checkout (fluxo transacional crítico)

| Campo | Valor |
|-------|-------|
| Objetivo | Medir latência da operação mais complexa: validação de utilizador + leitura de carrinho + criação de encomenda |
| Endpoint monólito | `GET /rest/order/checkout` (1 VU, ver §5.1) |
| Endpoint microserviços | `POST /order/checkout` (Gateway → order-service → inventory → catalog → cart → Kafka) |
| Carga | 1 VU, 60 s de medição após 30 s warm-up |
| Resultado monólito | p50 = 7.1 ms · p95 = 11.4 ms · sucesso = 100% · ~99 checkouts |
| Resultado microserviços | p50 = 19.9 ms · p95 = 32.8 ms · sucesso = 100% · ~96 checkouts · overhead p95 = +21.4 ms (+188%) |
| *Threshold* hipótese H2 | p95 microserviços ≥ 2× p95 monólito (≥ 22.8 ms) → **CONFIRMADA**: 32.8 ms = 2.9× |
| Hipótese avaliada | H2 |

#### Cenário T3: Isolamento de falhas

| Campo | Valor |
|-------|-------|
| Objetivo | Verificar se uma falha de infraestrutura derruba 100% dos endpoints no monólito mas não nos microserviços |
| Setup monólito | `docker stop monolith-db` com loop de pedidos `GET /rest/catalog/products` a 1 req/s |
| Setup microserviços | `docker stop catalog-service` com pedidos a `/order/list` e `/auth/login` |
| Resultado monólito | 200 → 500 em < 2 s após paragem da BD. Todos os endpoints afetados: 100% de falha. Recuperação após `docker start monolith-db` em ~15 s. Evidência: Fig. T3-status.png |
| Resultado microserviços | `docker stop xcommerce-catalog-service` executado com loop de pedidos a 3 endpoints em simultâneo. `/products` ativou o fallback do circuit breaker (503); `/order/list` e `/auth/login` mantiveram HTTP 200 durante toda a interrupção. Serviço reiniciado com `docker start xcommerce-catalog-service`. Evidência: `resultados/T-micro-3-status.png`, `resultados/t-micro-3-start-and-stop.png` |
| Hipótese avaliada | H1 |

#### Cenário T4: N+1 Queries (evidência estrutural)

| Campo | Valor |
|-------|-------|
| Objetivo | Quantificar o número de queries SQL emitidas pelo Hibernate para um checkout com 4 produtos / 5 itens |
| Método | `spring.jpa.show-sql=true` ativo; `docker logs monolith-app` capturado após checkout |
| Resultado | **11 queries** para 4 produtos distintos: 4× SELECT product, 4× SELECT category (lazy load parent), 1× SELECT user, 1× SELECT nextval (sequence), 1× INSERT order |
| Padrão | 1 + N + N + 1 + 1 = 3 + 2N queries, com N = nº de produtos distintos no carrinho |
| Solução possível | 1 query com JOIN: `SELECT o, ol, p, b, c FROM Order o JOIN o.orderLines ol JOIN ol.product p JOIN p.brand b JOIN p.category c WHERE o.user.id = ?` |
| Evidência | Figura 3; ficheiro `resultados/t-mono-4-n1-queries-resultado.txt` |

#### Cenário T5: Consumo de recursos

| Campo | Valor |
|-------|-------|
| Objetivo | Comparar RAM e CPU entre arquiteturas em idle e sob carga |
| Método | `docker stats --no-stream` |
| **Monólito idle** | app: 1.261 GiB / 0.88% CPU · db: 43.6 MiB / 0.02% · redis: 10.8 MiB / 0.61% → **total: ~1.31 GiB · 1.5% CPU** |
| **Monólito sob carga** (10 VUs T1) | app: 1.262 GiB / 12.1% CPU · db: 44.3 MiB / 2.3% · redis: 10.8 MiB / 0.65% → **total: ~1.31 GiB · 15.1% CPU** |
| **Microserviços idle** | ~6 483 MiB (~6.3 GiB) · 4.9× mais que monólito |
| **Microserviços sob carga** | ~6 486 MiB (~6.3 GiB) · CPU total 33.7% |
| Nº containers monólito | 3 (app + postgres + redis) |
| Nº containers microserviços | 17 (10 serviços + kafka + zookeeper + db + jaeger + prometheus + grafana + kafdrop) |
| Hipótese avaliada | H5: microserviços ≥ 2× memória idle → **CONFIRMADA**: 4.9× |

### 5.4 Resultados

#### T1: Latência de leitura do catálogo

| Arquitetura | p50 | p95 | Taxa de erro | Req totais |
|-------------|-----|-----|-------------|-----------|
| Monólito | 8.8 ms | 16 ms | 0% | ~1580 |
| Microserviços | 11.2 ms | 19.8 ms | 0% | ~800 |
| **Delta** | **+2.4 ms (+27%)** | **+3.8 ms (+24%)** | (n/a) | (n/a) |

O monólito executa `GET /rest/catalog/products` numa única query JPA direto à BD local. Os microserviços adicionam dois hops obrigatórios (Gateway com routing + validação JWT, e catalog-service) que custam ~4 ms em p95. O overhead é modesto porque a operação de leitura é simples e ambas as BDs correm localmente.

#### T2: Latência do checkout (fluxo transacional)

| Arquitetura | p50 | p95 | Taxa de sucesso | Iterações |
|-------------|-----|-----|----------------|-----------|
| Monólito | 7.1 ms | 11.4 ms | 100% | 99 |
| Microserviços | 19.9 ms | 32.8 ms | 100% | 96 |
| **Delta** | **+12.8 ms (+180%)** | **+21.4 ms (+188%)** | (n/a) | (n/a) |

No monólito, o checkout executa numa única transação JPA em memória: lê utilizador, lê carrinho Redis, percorre produtos, grava encomenda. Os 7.1 ms de p50 incluem 11 queries SQL (problema N+1 documentado em T4) e uma escrita Redis, tudo local, sem rede.

Nos microserviços, o p50 de 19.9 ms inclui: PATCH addProduct (cart-service) + POST checkout que internamente faz inventory-check (REST) + cart-read (REST) + order-save + Kafka publish. O overhead de ~13 ms em p50 é o custo direto das chamadas REST síncronas inter-serviço. A hipótese H2 (p95 ≥ 2× monólito = ≥ 22.8 ms) é **confirmada**: p95 microserviços = 32.8 ms = **2.9× o monólito**.

#### T3: Isolamento de falhas

**Monólito:** após `docker stop monolith-db`, o endpoint `GET /rest/catalog/products` passou de HTTP 200 para HTTP 500 em menos de 2 segundos. Todos os endpoints do monólito ficaram indisponíveis simultaneamente: autenticação, catálogo, carrinho e encomendas, sem exceção. A recuperação após `docker start monolith-db` demorou ~15 segundos (reconexão do connection pool Hibernate). Evidência: `resultados/T3-status.png`.

**Microserviços:** `docker stop xcommerce-catalog-service` foi executado enquanto um loop media os três endpoints em simultâneo a 1 req/s. `/products` ativou imediatamente o fallback do circuit breaker do Gateway (503), confirmando o isolamento da falha ao domínio do catálogo. `/order/list` e `/auth/login` mantiveram HTTP 200 durante toda a interrupção, sem nenhum erro colateral. Após `docker start xcommerce-catalog-service`, `/products` voltou a 200. Evidência: `resultados/T-micro-3-status.png` (loop de status codes) e `resultados/t-micro-3-start-and-stop.png` (comandos docker executados).

#### T4: N+1 Queries

Para um checkout com 4 produtos distintos (5 itens), o Hibernate emitiu **11 queries SQL**:

```
 1-2.  SELECT product + SELECT category(parent), produto 0
 3-4.  SELECT product + SELECT category(parent), produto 1
 5-6.  SELECT product + SELECT category(parent), produto 2
 7-8.  SELECT product + SELECT category(parent), produto 3
 9.    SELECT user WHERE id = 1
10.    SELECT nextval('hibernate_sequence')
11.    INSERT INTO "order" (...)
```

Fórmula: `3 + 2N` queries, onde N é o número de produtos distintos no carrinho. Com N=4: 11 queries. Uma única query com JOIN cobriria os passos 1-8. Evidência: `resultados/t-mono-4-n1-queries-resultado.txt` e Figura 3.

#### T5: Recursos CPU e RAM

| Container | RAM idle | CPU idle | RAM carga | CPU carga |
|-----------|----------|----------|-----------|-----------|
| monolith-app | 1 261 MiB | 0.88% | 1 262 MiB | 12.1% |
| monolith-db | 43.6 MiB | 0.02% | 44.3 MiB | 2.3% |
| monolith-redis | 10.8 MiB | 0.61% | 10.8 MiB | 0.65% |
| **Total monólito (3 containers)** | **~1 316 MiB** | **1.5%** | **~1 317 MiB** | **15.1%** |
| 10 serviços Spring Boot | ~4 085 MiB | 4.8% | ~4 086 MiB | 25.1% |
| Kafka + Zookeeper | ~1 119 MiB | 3.3% | ~1 119 MiB | 3.1% |
| BD + obs. (Jaeger, Prometheus, Grafana, Kafdrop) | ~1 279 MiB | 1.9% | ~1 281 MiB | 5.5% |
| **Total microserviços (17 containers)** | **~6 483 MiB (~6.3 GiB)** | **10.0%** | **~6 486 MiB** | **33.7%** |
| **Rácio microserviços / monólito** | **~4.9×** | **~6.7×** | **~4.9×** | **~2.2×** |

O monólito consome ~1.3 GiB em idle, dominado pela JVM (Spring Boot + Hibernate). A memória praticamente não varia entre idle e sob carga: a JVM aloca heap no arranque e não liberta. O CPU sobe de 1.5% para 15.1% com 10 VUs, o que representa o custo real de processamento das queries.

Os microserviços consomem ~6.3 GiB em idle, **~4.9× mais** que o monólito. Cada serviço Spring Boot arranca a sua própria JVM com heap mínimo (~400 MiB cada). Kafka sozinho ocupa ~1 GiB. O Kafka, Zookeeper, Jaeger, Prometheus e Grafana são overhead de infraestrutura que não existia no monólito. A memória também não varia com a carga: o padrão JVM de heap fixo repete-se em todos os serviços.

> **Figura 13:** Gráfico de barras comparativo p50/p95 para T1 e T2, monólito vs microserviços.  
> **Figura 14:** Gráfico de linhas T3: percentagem de respostas HTTP 200 ao longo do tempo, com marcação do instante `docker stop` e `docker start`.  
> **Figura 15:** Gráfico de barras T5: RAM e CPU idle e sob carga, monólito (3 containers) vs microserviços (11+ containers).

---

## 6. DISCUSSÃO CRÍTICA

### 6.1 Resposta às Questões de Investigação

**QI1: Uma falha num serviço fica contida ao seu domínio?**

No monólito, a resposta é inequivocamente não: a paragem de `monolith-db` derrubou 100% dos endpoints em menos de 2 segundos (T3). Autenticação, catálogo e encomendas falharam em simultâneo, não porque dependessem da mesma operação, mas porque partilham o mesmo processo JVM e o mesmo connection pool JDBC.

Nos microserviços, H1 é **confirmada**: a paragem do `catalog-service` não afetou `/auth/login` nem `/order/list`, ambos mantiveram HTTP 200 durante toda a interrupção (T3). O API Gateway com circuit breaker devolveu fallback 503 para `/products` mas manteve os restantes endpoints funcionais. O risco introduzido é simétrico: o próprio Gateway é um novo ponto único de falha. Se falhar, toda a aplicação fica inacessível independentemente de quantos serviços estejam operacionais.

**QI2: Qual o overhead de latência das chamadas inter-serviço?**

Baseline monólito: p95 = 16 ms em leitura simples (T1), p95 = 11.4 ms em checkout (T2).

Microserviços T1 (leitura): p95 = 19.8 ms, overhead de +3.8 ms (+24%) face ao monólito. O custo de 2 hops adicionais (Gateway + catalog-service) é modesto numa operação de leitura sem lógica complexa.

Microserviços T2 (checkout): p95 = 32.8 ms, overhead de +21.4 ms (+188%) = **2.9× o monólito**. H2 é **confirmada**. O custo vem de 3 chamadas REST síncronas em série: order-service → inventory (verificar stock) + cart (obter itens) + Kafka publish. Cada hop acrescenta serialização JSON e latência de rede Docker.

O resultado evidencia que a decomposição penaliza proporcionalmente a complexidade do fluxo: operações simples (leitura) pagam ~4 ms extra; operações que agregam múltiplos domínios (checkout) pagam ~21 ms extra.

**QI3: É possível reimplantar um serviço sem causar indisponibilidade nos restantes?**

Sim: `docker compose up -d catalog-service` atualiza apenas o container do catálogo. Durante os ~10 s de restart, o Gateway ativa o fallback `/fallback/catalog` e devolve resposta degradada. Os restantes serviços continuam a responder normalmente. Esta propriedade não existe no monólito: qualquer redeployment interrompe todos os endpoints simultaneamente.

### 6.2 Acoplamento: o que foi eliminado vs o que foi deslocado

> **Contribuição central da análise.** Em vez de afirmar "redução do acoplamento", classificamos por **tipo** de acoplamento (ver QI4/H4).

**Figura 16:** Tabela visual "acoplamento eliminado vs deslocado":

| Tipo de acoplamento | Monólito | Microserviços | Veredicto |
|---------------------|----------|---------------|-----------|
| **Estrutural** (FKs JPA, entidades partilhadas) | Forte (CartItem→Product) | Eliminado (apenas `productId`) | ✓ Eliminado |
| **Dados / Schema** (schema partilhado) | Forte (schema único) | Eliminado lógico, mantido físico (uma instância) | ⚠ Parcial |
| **Temporal** (chamadas síncronas obrigatórias) | Implícito na transação | **Deslocado** para REST síncrono (order→inventory→catalog) | ↔ Deslocado |
| **Eventos** (consumo assíncrono) | Não existia | **Introduzido** via Kafka (order↔payment↔notification) | ⊕ Novo |
| **Build-time** (deploy conjunto) | Forte (1 artefacto) | Eliminado (deploy independente por serviço) | ✓ Eliminado |
| **Runtime** (cascata de falhas) | Local (BD única) | **Deslocado**: falha em catalog corta checkout | ↔ Deslocado |
| **Operacional** (infraestrutura) | Postgres | **Aumentado**: Gateway, Kafka, Postgres lógico = mais SPOFs | ⊕ Novo |
| **Sincronização de identidade** | Não existia (auth integrado) | **Introduzido**: auth↔user via internal-sync | ⊕ Novo |

**Conclusão:** A decomposição **não elimina o acoplamento, recategoriza-o**. Eliminam-se acoplamentos estáticos (FKs, schema, deploy) ao custo de introduzir acoplamentos dinâmicos (REST runtime, Kafka eventual, infraestrutura distribuída). O ganho líquido depende do atributo de qualidade priorizado.

### 6.3 Limitações da Solução TO-BE
<!--
**Assumir explicitamente:**
1. Sem Saga compensatória: inconsistência possível em falha parcial no checkout
2. Sem idempotência no inventory-service e payment-service
3. Sem dead-letter queue no Kafka
4. Database per service apenas lógico, não físico
5. Auth/User: janela de inconsistência na criação de utilizador
6. Inventory e Payment são extensões funcionais, não apenas refactoring: comparação não é estritamente equivalente
-->
### 6.4 O que a Decomposição NÃO Resolve

- Consistência distribuída: Kafka asynchronous choreography introduz consistência eventual sem garantias
- Complexidade operacional: 10+ containers para a mesma funcionalidade
- Debugging distribuído: um request de checkout passa por 5+ serviços. Jaeger/Zipkin necessário para rastrear

---

## 7. CONCLUSÕES

### 7.1 Síntese das Decisões Arquiteturais

Tabela: decisão → problema que resolve → custo introduzido → evidência experimental

| Decisão | Problema resolvido | Custo/risco | Evidência |
|---------|-------------------|-------------|-----------|
| Decomposição por bounded context | Acoplamento estrutural entre domínios (CartItem→Product JPA) | Latência checkout +21.4 ms em p95 (+188%); leitura simples +3.8 ms (+24%) | T1, T2 |
| Database per service (lógico) | Ownership claro dos dados; schema evolution independente | Sem isolamento físico: instância PostgreSQL é SPOF partilhado | T3, §3.4 |
| Kafka para pagamento/notificações | Desacoplamento temporal; pagamento não bloqueia resposta ao utilizador | Consistência eventual; sem Saga compensatória implementada | §3.5, §4.3 |
| REST síncrono para inventory/cart | Resposta necessária para prosseguir checkout | 3 hops síncronos: checkout paga +21 ms vs monólito | T2, T3 |
| API Gateway + Circuit Breaker | Ponto único de entrada; encapsula estrutura interna; fallback por rota | Novo SPOF; +~4 ms em p95 em todas as operações | T1, T3 |

### 7.2 Resposta à Questão Principal

> "Em que medida a decomposição arquitetural contribui para melhorar atributos de qualidade como performance, availability, deployability e cost, face à solução monolítica original?"

- **Performance:** o checkout nos microserviços tem p95 = 32.8 ms vs 11.4 ms no monólito, **2.9× mais lento**. Em leitura simples o overhead é apenas +24% (19.8 ms vs 16 ms). O custo é proporcional ao número de serviços envolvidos no fluxo: 1 hop para leitura, 3+ hops síncronos para checkout.

- **Availability:** melhora claramente o isolamento entre domínios. T3 demonstra que no monólito 1 falha de BD derruba 100% dos endpoints em < 2 s. Nos microserviços, o isolamento é real mas incompleto: o checkout depende de 4 serviços em série (order + inventory + catalog + cart), pelo que uma falha em qualquer um deles bloqueia o fluxo transacional. Operações de leitura simples (consulta de encomendas, autenticação) ficam efetivamente isoladas.

- **Deployability:** melhora significativamente. Deploy independente por serviço demonstrado: `docker compose up -d <serviço>` atualiza um único container sem afetar os restantes. No monólito, qualquer alteração exige redeployment completo com downtime total.

- **Cost:** aumenta substancialmente. O monólito consome ~1.3 GiB RAM em idle (3 containers). Os microserviços consomem ~6.3 GiB em idle (17 containers), **4.9× mais**. O CPU idle sobe de 1.5% para 10%. Este overhead é permanente e independente da carga: existe mesmo sem nenhum utilizador a usar o sistema.

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

## ANEXOS (ficheiros digitais no Drive, não contam para as 20 páginas)

### Anexo A: Instruções de Execução (README)
- Como correr o monólito (`docker-compose up` em `01-monolith/`)
- Como correr os microserviços (`docker-compose up` em `02-microservices/`)
- Como reproduzir os testes de carga

### Anexo B: Origem do Código
- Monólito: fork de https://github.com/oiraqi/xcommerce-monolithic (Omar IRAQI)
- Microserviços: implementação própria do grupo G11 (listar o que foi escrito do zero vs adaptado)
- Indicar quais serviços foram completamente novos (inventory, payment, notification, bff, web-ui, api-gateway) vs refactoring de lógica existente (auth, user, catalog, cart, order)

### Anexo C: Scripts de Teste
- Scripts k6/Locust/JMeter para cada cenário (T1-T4)
- Instruções de execução

### Anexo D: Resultados Brutos
- CSVs com métricas por cenário
- Tabela de mapeamento: ficheiro CSV → cenário experimental

### Anexo E: Observabilidade
- Screenshots Grafana (dashboards durante testes)
- Screenshots Prometheus (métricas)
- Traces Jaeger (checkout end-to-end)
- Screenshots Kafdrop (tópicos Kafka durante checkout)

### Anexo F: Endpoints Comparativos
- Tabela completa: monólito vs microserviços (ver análise já feita)
- Nota: monólito usava Spring Data REST automático para produtos/categorias/marcas, não havia controllers explícitos

### Anexo G: Auto-avaliação Individual
(1 página por elemento, obrigatório conforme enunciado §8.3)
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
- [ ] AS-IS e TO-BE separados, sem referências a Kafka/microserviços na secção AS-IS
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
