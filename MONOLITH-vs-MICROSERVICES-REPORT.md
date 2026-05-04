# Relatório completo — Monólito vs Microserviços (XCommerce)

> Documento explicativo e técnico com tudo o que está implementado neste workspace.

---

## Sumário

- **Introdução**
- **O Monólito (XCommerce Monolithic)**
  - O que é
  - Estrutura do projecto e como funciona
  - O que tem / o que faz
- **A Arquitetura de Microserviços (XCommerce Microservices)**
  - O que é
  - Como está organizado aqui (lista de serviços)
  - O que tem / o que faz cada serviço
- **Diferenças técnicas entre Monólito e Microserviços**
  - Principais diferenças e o que cada diferença proporciona
  - Prós e contras de cada abordagem
- **Resultados dos testes presentes no repositório**
  - Sumário dos resultados (exemplos reais extraídos)
  - Interpretação e impacto
- **Conclusões e recomendações**
- **Referências a ficheiros do repositório (para leitura aprofundada)**

---

## Introdução

Este relatório descreve, em detalhe, a versão monolítica do XCommerce e a versão baseada em microserviços que existem neste workspace, explica como cada uma funciona, lista o que existe em cada implementação, compara as abordagens, apresenta resultados dos testes que estão no projecto e fornece conclusões e recomendações.

Os ficheiros de referência usados para compilar este relatório incluem:

- [01-monolith/xcommerce-monolithic-master/README.md](01-monolith/xcommerce-monolithic-master/README.md#L1)
- [02-microservices/api-gateway/HELP.md](02-microservices/api-gateway/HELP.md#L1)
- [02-microservices/auth-service/HELP.md](02-microservices/auth-service/HELP.md#L1)
- [03-testing/results/test-results-20260504-131641.csv](03-testing/results/test-results-20260504-131641.csv#L1)
- Código dos microserviços em `02-microservices/*`

---

## O Monólito (XCommerce Monolithic)

### O que é

Um monólito é uma aplicação onde toda a lógica (UI, negócio, persistência, integrações) coexiste numa única base de código e é executada como um único processo ou conjunto de processos idênticos. A versão monolítica do XCommerce usa Java + Spring Boot, JPA/Hibernate, PostgreSQL e Redis conforme documentado em [01-monolith/xcommerce-monolithic-master/README.md](01-monolith/xcommerce-monolithic-master/README.md#L1).

### Estrutura do projecto e como funciona

- O projecto tem um módulo `final/` que contém a aplicação Spring Boot pronta a construir/rodar (ver README).
- Componentes típicos:
  - Camada de entidades (JPA) e repositories.
  - Camada de serviços (regras de negócio).
  - Controladores REST (endpoints expostos para o frontend).
  - Integrações com Redis (cache) e PostgreSQL (persistência).
  - Autenticação baseada em JWT.
- Como funciona em termos de execução:
  - Ao arrancar, a aplicação inicia um servidor embutido (Tomcat/Jetty dependendo da versão Spring Boot) que expõe endpoints REST.
  - As mesmas instâncias tratam autenticação, catálogo, carrinho, encomendas, pagamentos, etc.

### O que tem / o que faz

Funcionalidades implementadas (resumo das responsabilidades):
- Gestão de utilizadores (registo, autenticação, perfis)
- Catálogo de produtos (brands, categories, products)
- Carrinho de compras persistente por utilizador
- Processo de encomenda (criar/consultar/alterar estado)
- Reviews de produtos
- Back-office para gestão de inventário/brands/categories/products
- Integrações externas (por exemplo: conversor de moeda)

Vantagens da implantação monolítica aqui:
- Simplicidade para desenvolver e testar localmente (um único artefacto).
- Menor sobrecarga operacional inicial.
- Facilita transacções ACID em operações que tocam várias partes do domínio.

Limitações:
- Escalabilidade toda-ou-nada: só se replica a aplicação inteira.
- Risco de dependências cruzadas que tornam o código mais difícil de manter com o crescimento.

---

## A Arquitetura de Microserviços (XCommerce Microservices)

### O que é

Arquitetura onde a aplicação é composta por vários serviços pequenos, cada um com responsabilidade delimitada, comunicando por APIs (REST, eventos, etc.). A versão neste workspace segue esse padrão e contém vários serviços Spring Boot independentes, um API Gateway, e componentes de suporte (dashboard, etc.).

### Como está organizado neste workspace

Pasta principal: `02-microservices/`

Serviços detectados (cada um é um projecto Spring Boot separado):

- api-gateway (gateway e roteamento)
- auth-service (serviço de autenticação/identidade)
- cart-service
- catalog-service
- dashboard (app Node/Express para back-office / UI de suporte)
- inventory-service
- notification-service
- order-service
- payment-service
- user-service

Cada serviço tem o seu próprio `pom.xml`/`mvnw`, `Dockerfile`, e normalmente uma pasta `src/` com código e `target/` para artefactos.

Referência de ajuda/arranque do gateway e auth: [02-microservices/api-gateway/HELP.md](02-microservices/api-gateway/HELP.md#L1), [02-microservices/auth-service/HELP.md](02-microservices/auth-service/HELP.md#L1).

### O que faz cada serviço (resumo de responsabilidades)

- api-gateway: Roteamento, autenticação central (token forwarding), consolidação de endpoints e políticas transversais (rate-limiting, logging, etc.).
- auth-service: Gestão de contas/autenticação (login, refresh tokens, criação de utilizadores internos), integração com sistema de identidade.
- user-service: Gestão de perfil de utilizador (dados pessoais, estados, roles) — ver controlador exemplar em [02-microservices/user-service/src/main/java/com/xcommerce/user_service/controller/UserController.java](02-microservices/user-service/src/main/java/com/xcommerce/user_service/controller/UserController.java#L1).
- catalog-service: CRUD de produtos, categorias, marcas, pesquisa por nome/categoria.
- cart-service: Estado do carrinho por utilizador; operações de adicionar/remover produtos.
- order-service: Criação/gestão de encomendas, alteração de estado, histórico.
- payment-service: Integração com gateways de pagamento (simulada/dummy para testes).
- inventory-service: Gestão de stock e reservas para encomendas.
- notification-service: Envio de e-mails/notifications sobre estado de encomendas, etc.
- dashboard: UI/serviço auxiliar para backoffice/monitorização.

Cada serviço é independente em termos de ciclo de vida, possivelmente com a sua própria base de dados (ou esquema) e configuração.

---

## Diferenças técnicas entre Monólito e Microserviços

Abaixo estão diferenças importantes, o que causam, e os efeitos práticos.

- Desagregação de responsabilidades
  - Monólito: Todas responsabilidades no mesmo deploy. Simplicidade inicial, mas aumenta a complexidade com o tempo.
  - Microserviços: Cada serviço tem responsabilidade única — facilita manutenção e deploys independentes.
  - Efeito: Permite deployments mais rápidos por equipa; peca por complexidade na integração.

- Deploy e Operações
  - Monólito: Um artefacto. Escalar = replicar a aplicação inteira.
  - Microserviços: Vários artefactos. Escalar por serviço (ex.: apenas `order-service`).
  - Efeito: Melhor custo/recursos ao escalar hotspots; maior overhead (CI/CD, containerização, orquestração).

- Escalabilidade
  - Monólito: Escala vertical/horizontal integral.
  - Microserviços: Escala horizontal específica por serviço.
  - Efeito: Eficiência de recursos para serviços que exigem mais throughput.

- Consistência de dados / Transacções
  - Monólito: Fácil usar transacções ACID no mesmo banco.
  - Microserviços: Normalmente eventual consistency e padrões como sagas para orquestrar transacções distribuídas.
  - Efeito: Maior complexidade para garantir consistência, necessidade de compensações.

- Tolerância a falhas / Resiliência
  - Monólito: Um componente falho pode afetar toda a aplicação.
  - Microserviços: Isolamento de falhas por serviço se desenhado corretamente.
  - Efeito: Potencial maior disponibilidade, mas depende de infra e padrões (circuit breakers, retries).

- Latência e Overhead de Comunicação
  - Monólito: Chamadas internas (método) — baixíssima latência.
  - Microserviços: Comunicação via rede (HTTP/gRPC/eventos) — latência e complexidade adicional.
  - Efeito: Design precisa considerar chamadas remotas, caching, e redes.

- Desenvolvimento e Organização de Equipa
  - Monólito: Simples para equipas pequenas; código tende a crescer e conflitar.
  - Microserviços: Facilita equipas independentes, stacks heterogéneas.

- Observabilidade / Debugging
  - Monólito: Tracing mais direto.
  - Microserviços: Requer tracing distribuído, logs agregados e métricas por serviço.

---

## Prós e Contras — resumo prático

Monólito — Prós
- Fácil de desenvolver e testar localmente.
- Menos infra necessária inicialmente.
- Simplicidade em transacções e queries complexas que cruzam domínios.

Monólito — Contras
- Escala menos eficiente para cargas distintas por domínio.
- Dificuldade de evoluir com equipas grandes.
- Deploys mais arriscados (uma alteração pode afetar todo o sistema).

Microserviços — Prós
- Escalabilidade por serviço, melhor utilização de recursos.
- Deploys independentes, menor risco ao alterar serviços específicos.
- Possibilidade de usar tecnologias diferentes por serviço.

Microserviços — Contras
- Maior complexidade operacional (orquestração, CI/CD, observabilidade).
- Complexidade em garantir consistência e orquestrar transacções distribuídas.
- Possível latência adicional e custos de comunicação.

---

## Resultados dos testes presentes no repositório

No directório `03-testing/results/` existem vários ficheiros CSV com resultados de testes de disponibilidade, custo e escalabilidade. Um dos ficheiros de teste de carga mais recentes extraídos é:

- [03-testing/results/test-results-20260504-131641.csv](03-testing/results/test-results-20260504-131641.csv#L1)

Conteúdo relevante extraído (resumo):

- Timestamp: 20260504-131641
- TestDuration: 60s
- RequestsPerSecond: 20
- TotalRequests: 11,674
- SuccessfulRequests: 11,674
- FailedRequests: 0
- SuccessRate: 100%
- AverageResponseTime: 1.35ms
- MaxResponseTime: 17.01ms
- MinResponseTime: 0ms
- Throughput: 194.35 eq/s

Interpretação e impacto:
- O teste mostra alta taxa de sucesso (100%) durante o período de teste com baixa latência média (1.35ms). Isto sugere que, nas condições desta execução (20 rps), a arquitetura suportou a carga com folga.
- `MaxResponseTime` de 17ms indica picos muito modestos para o perfil de teste aplicado.
- O corredor de `Throughput` e `RequestsPerSecond` aponta que o sistema consegue processar pedidos de forma estável.

Observações importantes:
- Estes valores dependem fortemente do ambiente onde os testes foram executados (recursos, rede, contêineres, latência do host).
- Para validar escalabilidade e comportamento em produção, são necessários testes com cargas variáveis, testes de stress até ao ponto de degradação e cenários multi-região.

---

## Conclusões e recomendações

- Para um projecto educativo e para validação de conceitos, manter ambas as implementações (monólito e microserviços) é útil: o monólito serve para acelerar desenvolvimento inicial e entendimento do domínio; microserviços são preferíveis quando se pretende escalar independentemente componentes e suportar equipas paralelas.
- Recomendação prática para evolução:
  1. Começar com o monólito para validarem rapidamente regras de negócio e modelo de dados se a equipa for pequena e o domínio for os testes de viabilidade.
  2. Extrair serviços críticos para microserviços conforme o sistema cresce (p.ex. `order-service`, `payment-service`, `inventory-service`). Adoptar uma estratégia incremental (estrangulamento), não reescrever tudo de uma vez.
  3. Investir em observabilidade (tracing distribuído, logs centralizados, métricas) e em CI/CD automatizado antes de migrar para microserviços em produção.
  4. Validar padrões de consistência (sagas) e design de eventos para operações distribuídas.

---

## Leituras e ficheiros chave do repositório

- Monólito: [01-monolith/xcommerce-monolithic-master/README.md](01-monolith/xcommerce-monolithic-master/README.md#L1)
- Microservices: ver directório `02-microservices/` (cada serviço contém `HELP.md`, `Dockerfile` e `pom.xml`). Exemplos:
  - [02-microservices/api-gateway/HELP.md](02-microservices/api-gateway/HELP.md#L1)
  - [02-microservices/auth-service/HELP.md](02-microservices/auth-service/HELP.md#L1)
  - Controller exemplo: [02-microservices/user-service/src/main/java/com/xcommerce/user_service/controller/UserController.java](02-microservices/user-service/src/main/java/com/xcommerce/user_service/controller/UserController.java#L1)
- Resultados de testes: [03-testing/results/](03-testing/results/)

---

Se quiser, eu:
- adiciono sínteses gráficas (diagramas simples) para comparar escalabilidade e deploy flows;
- extraio estatísticas agregadas de todos os CSVs em `03-testing/results/` e adiciono tabelas e gráficos ao relatório;
- commito e faço push automático do ficheiro para o repositório remoto.

Diz-me qual destas ações queres que eu faça a seguir.

---

## Anexos — Estatísticas agregadas e gráficos (adicionados)

Adicionei uma secção com sínteses e gráficos criados a partir dos ficheiros em `03-testing/results/`.

### Síntese rápida (valores agregados das amostras)

- Scalability (amostra 2026-04-08):
  - Monólito — Avg Latency (média): ~17.46 ms; Avg Throughput: ~11.32 eq/s
  - Microserviços — Avg Latency (média): ~50.69 ms; Avg Throughput: ~11.82 eq/s
- Academic tests (leituras/escritas/workflow): latências médias aproximadas — GET: Mon ~18.4ms / Micro ~52.8ms; POST: Mon ~128.4ms / Micro ~287.7ms; Workflow: Mon ~342.6ms / Micro ~568.5ms
- Disponibilidade: Monólito mostrou "Total Failure" com downtime ~45s; microserviços apresentaram falhas isoladas com downtime ~42-63s.
- Custos (amostra): custo estimado por request — Monólito ~0.00062 ; Microserviços ~0.00099

