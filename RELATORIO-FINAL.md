# Relatório Final XCommerce

**Da arquitetura monolítica aos microserviços: estudo empírico de uma transição em comércio eletrónico**

**Liandro Macedo da Cruz** — PG61472
**Miguel Angelo** — *afiliação, e-mail*
**Rafael R. Prudente dos Santos** — *afiliação, e-mail*
**Tomás CMS** — *afiliação, e-mail*

---

## Abstract

A literatura de engenharia de software apresenta a arquitetura de microserviços como resposta à rigidez das aplicações monolíticas, sustentada em argumentos de modularidade, isolamento de falhas e escalabilidade independente. Este trabalho examina empiricamente essa transição num caso concreto, o XCommerce — uma aplicação de comércio eletrónico originalmente implementada como monólito em Spring Boot — propondo e avaliando uma decomposição em sete serviços autónomos guiada pelo Domain-Driven Design. A questão central não é se a decomposição traz benefícios, mas que natureza têm os custos que introduz. Com base em três questões de investigação — sobre a redistribuição do acoplamento, o crescimento do custo com a complexidade dos fluxos e a consistência sob falha parcial — e em medições controladas com paridade funcional entre arquiteturas, mostra-se que a decomposição (i) substitui acoplamento estático por acoplamento dinâmico de magnitude equivalente, (ii) penaliza desproporcionalmente operações que envolvem múltiplos domínios e (iii) regride a consistência transacional na ausência de padrões de compensação. Os resultados sugerem que a adoção de microserviços é uma reorganização de problemas arquiteturais, e não a sua eliminação.

**CCS Concepts:** Software architectures · Microservices · Software evolution · Performance evaluation

**Keywords:** arquitetura monolítica, microserviços, Domain-Driven Design, bounded context, consistência distribuída, atributos de qualidade, Spring Boot, comércio eletrónico.

---

## 1. Introdução

### 1.1 Contexto e Problema

A arquitetura de software determina, em larga medida, a capacidade de uma aplicação evoluir. Em sistemas de comércio eletrónico, onde os domínios funcionais — catálogo, carrinho, encomendas, autenticação — apresentam ritmos de evolução, padrões de utilização e exigências de escala distintos, o modelo monolítico tende a tornar-se um obstáculo: o acoplamento estrutural entre módulos cristaliza-se no esquema da base de dados, qualquer alteração obriga a um redeployment integral e a saturação de um único domínio compromete o funcionamento de todos os outros.

A literatura propõe a arquitetura de microserviços como resposta a estas limitações. Trabalhos como *Building Microservices* [Newman, 2021] e *Microservices Patterns* [Richardson, 2018] argumentam que a decomposição em serviços autónomos, alinhada com os Bounded Contexts do Domain-Driven Design [Evans, 2003], permite o desenvolvimento, deployment e escalonamento independentes por domínio. A transição não é, todavia, isenta de custos: introduz latência de rede, consistência eventual e complexidade operacional que, em determinados contextos, podem superar os benefícios obtidos.

O presente trabalho examina esta transição num caso concreto. O sistema de partida é o XCommerce, uma aplicação Spring Boot em que a autenticação, o catálogo, o carrinho e o processamento de encomendas partilham um único processo, uma única base de dados PostgreSQL e um único ciclo de deployment. O fluxo de checkout, em particular, executa numa única transação JPA a validação do utilizador, a leitura do catálogo, o esvaziamento do carrinho e a criação da encomenda. Esta operação concentra simultaneamente a maior complexidade funcional e o maior risco arquitetural: uma falha em qualquer dos passos compromete toda a aplicação.

### 1.2 Objetivos

Este trabalho propõe uma decomposição do XCommerce em microserviços e avalia empiricamente os efeitos dessa decomposição sobre quatro atributos de qualidade — desempenho, disponibilidade, *deployability* e custo operacional — explicitamente identificados no enunciado da unidade curricular. A questão de fundo não é se a decomposição traz benefícios (a literatura responde-o há mais de uma década), mas que natureza têm os custos que introduz. A investigação é orientada por três questões, apresentadas em §1.3, cada uma das quais aborda uma dimensão distinta do trade-off arquitetural.

A contribuição do trabalho é dupla. Por um lado, fornece evidência quantitativa, recolhida sob condições de paridade funcional entre as duas arquiteturas, sobre o custo concreto da decomposição. Por outro, demonstra empiricamente que a comunicação assíncrona, sem padrões adicionais de compensação, regride a consistência sob falha parcial face ao baseline transacional do monólito. Inclui-se ainda uma auditoria crítica das próprias fronteiras de decomposição, motivada por uma decisão inicial que se revelou não justificada pelos princípios DDD que pretendia aplicar.

### 1.3 Questões de Investigação

O trabalho é orientado por três questões. Cada uma é formulada como hipótese mensurável e associada a um critério de validação observável.

**QI1 — Estrutural.** *A decomposição em microserviços elimina ou apenas redistribui o acoplamento entre domínios?*

Hipótese H1: a decomposição substitui acoplamento estrutural compile-time (chaves estrangeiras, entidades partilhadas, deployment conjunto) por acoplamento temporal runtime (chamadas síncronas) e por acoplamento eventual (mensagens assíncronas), sem reduzir o número total de dependências cross-domain. A validação assenta na contagem das categorias de dependência observáveis em cada arquitetura.

**QI2 — Quantitativa.** *Como evolui o custo da decomposição, em latência e em recursos, à medida que aumenta a complexidade dos fluxos e o número de serviços envolvidos?*

Hipótese H2 (duas componentes): (a) o custo de latência cresce de forma super-linear com o número de chamadas síncronas em série; (b) o consumo de memória em estado ocioso é aproximadamente proporcional ao número de processos. Para H2a comparam-se as latências de um fluxo simples (uma chamada) com as de um fluxo complexo (cinco chamadas em série) nas duas arquiteturas. Para H2b mede-se o consumo de memória em ambiente ocioso, controlando a paridade funcional.

**QI3 — Transacional.** *A comunicação assíncrona melhora ou degrada a consistência sob falha parcial face à transação ACID do monólito?*

Hipótese H3: na ausência de mecanismos de compensação (Saga, idempotência, *dead-letter queues*), a comunicação assíncrona apresenta maior potencial de inconsistência do que o baseline transacional do monólito. A validação consiste na injeção controlada de falha a meio de um fluxo transacional e na observação do estado dos dados em ambas as arquiteturas.

A escalabilidade horizontal por componente, frequentemente apresentada como argumento principal a favor de microserviços, **não é objeto deste trabalho**. A sua avaliação empírica exige infraestrutura — orquestração com múltiplos nós, equipas paralelas, carga assimétrica entre domínios — fora do âmbito de um protótipo local. A propriedade é discutida qualitativamente em §7 e identificada como linha de trabalho futuro.

---

## 2. Trabalho Relacionado e Enquadramento Teórico

A bibliografia consultada concentra-se em três áreas. A primeira diz respeito à decomposição arquitetural propriamente dita: Newman [2021] sistematiza as motivações, padrões e armadilhas da transição para microserviços; Fowler e Lewis [2014] cunham a definição operacional do termo; Richardson [2018] cataloga os padrões aplicáveis. A segunda área trata do desenho do domínio: Evans [2003] introduz os conceitos de Bounded Context e *Ubiquitous Language*, que orientam neste trabalho a definição das fronteiras entre serviços. A terceira aborda os padrões de resiliência e consistência distribuída — circuit breakers, Saga, *outbox*, idempotência — relevantes para a compreensão das limitações que a decomposição introduz e dos mecanismos disponíveis para as mitigar.

Do ponto de vista metodológico, optou-se por uma abordagem comparativa entre o sistema original (AS-IS) e a proposta (TO-BE). A comparação é estabelecida em condições de paridade funcional — uma condição que exigiu intervenções no monólito original para o equiparar funcionalmente à proposta, descritas em §6.2 — e os resultados são interpretados à luz dos quatro atributos de qualidade identificados no enunciado: desempenho, disponibilidade, *deployability* e custo operacional.

---

## 3. Sistema de Referência (AS-IS)

### 3.1 Caracterização do Monólito

O XCommerce na sua forma original é uma aplicação Spring Boot com Java 8, organizada internamente em camadas (controllers, serviços, repositórios e entidades) mas implantada como um único artefacto. Toda a funcionalidade — autenticação, gestão do catálogo de produtos, carrinho de compras e processamento de encomendas — corre no mesmo processo, partilha o mesmo gestor de persistência JPA e escreve numa única instância PostgreSQL. O carrinho usa adicionalmente Redis como cache em memória.

Internamente, o código segue o padrão MVC clássico, com as responsabilidades distribuídas por cerca de três dezenas de classes agrupadas por camada. A separação lógica é clara — controladores que recebem requests, serviços que orquestram, repositórios que persistem — mas não corresponde a qualquer fronteira de deployment ou de falha. Todos os módulos partilham o mesmo *connection pool* JDBC e o mesmo classpath, pelo que uma falha originada num domínio propaga-se imediatamente aos restantes.

### 3.2 Limitações que Motivam a Decomposição

A análise do sistema original revela três limitações estruturais e duas limitações funcionais que merecem registo.

A primeira limitação estrutural é o **acoplamento entre os domínios do carrinho e do catálogo**. O carrinho não armazena meras referências aos produtos: guarda em cache Redis a entidade JPA completa, incluindo preço, peso, desconto, categoria e marca. Esta opção, eficaz em termos de leitura, transforma qualquer alteração ao modelo de produto numa potencial corrupção do estado do carrinho.

A segunda é o **acoplamento total no fluxo de checkout**. A operação executa numa única transação a validação do utilizador, a leitura do carrinho, a iteração sobre os produtos e a criação da encomenda. A arquitetura não permite isolar a falha de um destes passos: uma sobrecarga no processamento de encomendas afeta simultaneamente a autenticação e a consulta de catálogo, simplesmente porque tudo corre no mesmo processo.

A terceira é a **deployability conjunta**. Qualquer alteração, mesmo a um único módulo, obriga ao redeployment integral. Esta característica não constitui um problema em si — em sistemas pequenos é mesmo desejável — mas torna-se limitante quando equipas independentes precisam de iterar em domínios distintos a ritmos distintos.

A estas limitações estruturais somam-se duas limitações funcionais que importa explicitar, porque influenciam diretamente a comparação entre arquiteturas. A versão original do XCommerce **não valida o stock no checkout**, apesar de o campo correspondente existir na entidade `Product`; e os controladores **utilizam um identificador de utilizador fixo** em vez de extrair a identidade do token JWT. Estas duas limitações, sendo funcionais e não arquiteturais, foram corrigidas no decorrer deste trabalho para garantir paridade na comparação experimental (§6.2).

---

## 4. Proposta Arquitetural (TO-BE)

### 4.1 Princípios Orientadores

A proposta decompõe o XCommerce em sete serviços de negócio autónomos e um *API Gateway*, alinhando cada serviço com um *Bounded Context* do Domain-Driven Design. A regra central é que cada serviço é o único proprietário dos dados do seu domínio: comunica com os restantes apenas através de APIs ou de mensagens assíncronas, nunca por acesso direto à base de dados alheia.

A escolha entre comunicação síncrona e assíncrona não é uniforme: depende, em cada caso, de uma questão concreta — *o serviço que faz o pedido precisa da resposta para continuar?* Quando precisa (verificar stock antes de criar uma encomenda, obter o preço atualizado de um produto), opta-se por chamadas REST síncronas. Quando não precisa (notificar o utilizador, processar um pagamento de forma diferida), opta-se por mensagens publicadas em Kafka. Cada uma destas opções introduz problemas distintos, discutidos adiante.

### 4.2 Decomposição por Bounded Context

A decomposição em sete serviços não foi axiomática nem uniforme. Para cada serviço, a justificação parte sempre da pergunta inversa: o que existia no monólito original, e que motivo concreto leva a constituir aqui um serviço próprio em vez de manter o conceito agregado a outro? A resposta tem três planos: (i) o ponto de partida no monólito; (ii) a razão pela qual a separação é defensável; (iii) as alternativas rejeitadas e as razões da rejeição. Apresentam-se em seguida os cinco casos mais relevantes, segundo este formato, terminando com o *trade-off* que a decisão introduz.

#### 4.2.1 Carrinho e Catálogo

- **No monólito.** Existem dois domínios funcionais — carrinho e catálogo — partilhando a mesma base de dados e o mesmo modelo de classes. O carrinho mantém em cache a entidade completa do produto (preço, peso, desconto, marca, categoria), e o serviço de encomendas acede diretamente a este objeto durante o checkout.
- **Porque a separação é defensável.** Carrinho e catálogo respondem a propósitos distintos. O catálogo é um conjunto de dados de leitura predominante, evolução lenta e elevado volume de tráfego — beneficia de cache agressivo e, idealmente, da capacidade de escalar independentemente da escrita. O carrinho é o oposto: dados efémeros, escrita frequente, baixo volume por utilizador, sem necessidade de leitura partilhada. Manter os dois fundidos obriga ambos a partilhar decisões operacionais (esquema de base de dados, política de cache, ciclo de deployment) que só fazem sentido para um deles. A separação permite ainda eliminar o acoplamento estrutural — o carrinho deixa de conhecer o modelo do produto, passando a guardar apenas o seu identificador.
- **Alternativas rejeitadas.** A primeira alternativa foi manter os dois domínios num único serviço, mais coeso por proximidade funcional (ambos dizem respeito à navegação de compras). Foi rejeitada porque preservaria os mesmos problemas do monólito: partilha de modelo, deployment conjunto, impossibilidade de escalar independentemente. A segunda alternativa foi inverter a propriedade dos dados — colocar o stock no serviço de carrinho para evitar uma chamada remota — também rejeitada, pois transformaria o carrinho num serviço composto e violaria o princípio de propriedade exclusiva por domínio.
- **Trade-off introduzido.** O carrinho perde acesso direto a dados do produto (nome, preço atualizado). Para apresentar um carrinho com detalhes legíveis, ou para processar o checkout, o serviço de encomendas necessita de interrogar o serviço de catálogo em runtime. Se este último estiver indisponível, a apresentação degrada (mostra apenas identificadores) e o checkout falha. Trata-se de uma manifestação concreta da redistribuição de acoplamento prevista por H1: o que era estrutural no monólito passa a ser temporal nos microserviços.

#### 4.2.2 Identidade (autenticação e perfil de utilizador)

- **No monólito.** A autenticação e o perfil do utilizador estão fundidos numa única entidade `User`, com credenciais, dados pessoais e papéis funcionais coabitando na mesma tabela. A camada de segurança é transversal a toda a aplicação.
- **Porque a separação foi inicialmente defendida.** A intuição inicial seguia o princípio comum em arquiteturas distribuídas: a autenticação tende a ter requisitos distintos dos do perfil (credenciais sensíveis, validação criptográfica, integração eventual com *Identity Providers* externos como Keycloak ou Auth0), pelo que justificaria um serviço autónomo. A separação tornaria também possível evoluir o mecanismo de autenticação sem tocar nos dados de perfil.
- **Porque a separação foi posteriormente revertida.** A análise crítica da implementação revelou que os critérios DDD que justificam manter fronteiras — linguagem ubíqua distinta, autonomia operacional, ciclos de vida desalinhados — não se verificavam no caso. Ambos os serviços modelavam o mesmo conceito de domínio (utilizador), e a operação de registo obrigava sempre a sincronização imediata em ambos os sentidos: uma falha de qualquer um dos lados após a persistência do outro deixava o sistema num estado inconsistente. A separação introduzia complexidade operacional sem benefício de domínio que a justificasse. Os dois serviços foram fundidos num único serviço de identidade, que continua a ser o ponto natural de integração quando a evolução para um IdP externo for de facto necessária.
- **Alternativas rejeitadas.** Manter a separação inicial e introduzir compensação por *outbox pattern* para mitigar a janela de inconsistência. Foi rejeitada porque o esforço necessário não compensava: a sincronização cruzada continuaria a existir e a sua complexidade seria simplesmente deslocada para o mecanismo de compensação.
- **Trade-off introduzido.** A fusão simplifica a operação atual ao custo de adiar a separação para o momento em que ela seja efetivamente justificada — por exemplo, quando o serviço de identidade evoluir para um IdP externo, situação em que os dados de perfil ficarão naturalmente num serviço próprio. Esta auditoria das próprias fronteiras é discutida em maior profundidade em §7.3 e constitui ela mesma uma observação metodológica relevante.

#### 4.2.3 Inventário

- **No monólito.** Existe um campo de quantidade na entidade do produto, mas não é validado nem decrementado durante o checkout. O sistema regista compras independentemente da disponibilidade real do stock — uma limitação funcional do sistema original (§3.2), e não uma propriedade arquitetural.
- **Porque a separação é defensável.** O stock e o catálogo respondem a ritmos e padrões de utilização diferentes. O catálogo é maioritariamente de leitura, com escrita pouco frequente; o stock é o oposto, com escrita constante a cada encomenda processada. A separação permite, em teoria, escalar e otimizar cada um de forma independente. Acresce que o stock está sujeito a operações específicas (reservas, decremento atómico, restauro em caso de cancelamento) que não fazem sentido como parte da API do catálogo. A criação do serviço autónomo de inventário corrige simultaneamente a limitação funcional do monólito (passa a haver validação real) e estabelece uma fronteira coerente do ponto de vista DDD.
- **Alternativas rejeitadas.** Manter o stock como atributo do produto no serviço de catálogo, evitando um serviço adicional. Rejeitada por dois motivos: (a) o ciclo de vida do stock — atualizações frequentes, padrões de leitura e escrita distintos, requisitos de concorrência — não corresponde ao do catálogo; (b) manter o stock no catálogo obrigaria o serviço de catálogo a expor uma API de escrita à medida do checkout, contaminando uma API de outro modo predominantemente de leitura.
- **Mudança aplicada também ao monólito.** Para que a comparação experimental entre arquiteturas seja justa, a validação de stock no checkout foi também adicionada à versão monolítica (§6.2). A diferença entre arquiteturas medida nos testes reflete, por isso, o custo da decomposição arquitetural, e não a presença ou ausência de funcionalidade.
- **Trade-off introduzido.** O serviço de encomendas passa a depender, em runtime e de forma síncrona, do serviço de inventário. Se este último falhar, o checkout falha. Acresce que, na ausência de mecanismos de compensação (Saga), o stock pode ser decrementado sem que a encomenda venha a ser confirmada — situação analisada em §5.3 e que sustenta empiricamente a QI3.

#### 4.2.4 Pagamento

- **No monólito.** Não existe processamento de pagamento. A encomenda é criada e considerada concluída sem qualquer interação com um sistema externo de cobrança.
- **Porque a separação é defensável.** Numa aplicação de comércio eletrónico real, o pagamento é uma operação intrinsecamente lenta e sujeita a falhas externas, dado que envolve a interação com gateways de cartões, bancos ou plataformas como Stripe ou PayPal. Manter este processamento síncrono no fluxo do checkout transformaria o tempo de resposta percebido pelo utilizador na soma da latência interna do sistema com a latência variável do gateway de pagamento — facilmente da ordem dos segundos. A separação para um serviço autónomo, comunicando por mensagens assíncronas, permite responder ao utilizador imediatamente após o registo da encomenda (em estado intermédio) e atualizar o estado quando a confirmação do pagamento chegar.
- **Alternativas rejeitadas.** A primeira alternativa foi processar o pagamento de forma síncrona dentro do serviço de encomendas. Rejeitada pelo motivo acima: penaliza a experiência do utilizador sem necessidade. A segunda alternativa foi não introduzir pagamento de todo, mantendo paridade funcional com o monólito original; rejeitada por duas razões: (a) excluiria do estudo um padrão de comunicação central na arquitetura proposta — a comunicação assíncrona via Kafka — sem o qual a infraestrutura de mensageria não teria utilização real; (b) impediria a construção do cenário de falha parcial controlada que sustenta a QI3 (§5.3).
- **Mudança aplicada (ou não) ao monólito.** Ao contrário do inventário, o pagamento não foi adicionado ao monólito. Sendo uma operação assíncrona via Kafka, o seu processamento não influencia a latência observável do checkout — a medição termina quando o gateway devolve a resposta com a encomenda em estado intermédio, antes do pagamento ser efetivamente processado em segundo plano. Adicionar pagamento síncrono ao monólito acrescentaria latência que não tem equivalente na proposta de microserviços, distorcendo a comparação.
- **Trade-off introduzido.** A consistência entre encomenda e pagamento passa a ser eventual: a encomenda é criada antes da confirmação do pagamento, e o seu estado final depende do consumo de um evento que pode falhar, atrasar-se ou ser duplicado. A garantia transacional do monólito é substituída por um conjunto de propriedades que requerem padrões adicionais — idempotência ao nível do consumidor, *dead-letter queue* para mensagens que falham repetidamente, Saga para coordenar a compensação — nenhum dos quais implementado neste protótipo. A escolha é deliberada e sustenta empiricamente a investigação à QI3.

#### 4.2.5 Notificações

- **No monólito.** Não existe envio de notificações ao utilizador (confirmação de encomenda, alteração de estado, alertas). O sistema regista as encomendas mas não comunica com o exterior.
- **Porque a separação é defensável.** As notificações têm uma propriedade que as distingue de qualquer outra operação do sistema: a sua falha não compromete a integridade dos dados nem a continuação do fluxo principal. Um utilizador que conclua uma encomenda e não receba o correio eletrónico de confirmação tem uma má experiência, mas a encomenda foi registada e processada corretamente. Esta independência semântica face à operação principal torna o domínio das notificações o caso mais simples e claro de comunicação assíncrona: nenhum produtor de eventos precisa de saber se a notificação foi efetivamente enviada, nem deve esperar por essa confirmação. Justifica-se, por isso, um serviço autónomo dedicado.
- **Alternativas rejeitadas.** Chamada síncrona a partir do serviço de encomendas após a criação da encomenda, ou após a confirmação do pagamento. Rejeitada porque transformaria o tempo de envio do correio eletrónico — operação tipicamente da ordem das centenas de milissegundos — em latência percebida pelo utilizador, sem benefício correspondente. Outra alternativa rejeitada foi colocar a lógica de notificação dentro do próprio serviço que origina cada evento (encomendas envia o seu próprio email, pagamentos envia o seu, etc.), o que duplicaria configuração de servidores SMTP, modelos de mensagem e lógica de envio em vários serviços.
- **Trade-off introduzido.** Em caso de falha do consumidor, ou de falha do próprio Kafka, as notificações perdem-se sem hipótese de recuperação na ausência de uma *dead-letter queue*. O impacto, no entanto, é limitado pelo próprio carácter não-crítico da operação: a falha de notificação não compromete o estado dos dados nem o funcionamento das restantes operações.

### 4.3 Padrões de Comunicação Inter-Serviço

A passagem de uma arquitetura monolítica para microserviços obriga a uma decisão que não existia no monólito: como é que dois serviços, agora em processos distintos, trocam informação? A escolha não é uniforme nem axiomática. Depende, em cada caso, de uma única pergunta — *o serviço que faz o pedido precisa da resposta para continuar a operação corrente?* — e tem implicações distintas conforme a resposta.

#### Comunicação síncrona (REST)

Optou-se por chamadas síncronas sempre que a resposta é necessária para prosseguir o fluxo. Os exemplos típicos são a validação de disponibilidade de stock antes da criação de uma encomenda, a obtenção do preço atualizado de um produto e a leitura do carrinho do utilizador no momento do checkout. Em todos estes casos, prosseguir sem a resposta tornaria o resultado da operação semanticamente incorreto: criar uma encomenda sem validar stock, registar um valor com base num preço potencialmente desatualizado, ou processar um carrinho que entretanto pode ter mudado.

A comunicação síncrona neste trabalho assenta em chamadas HTTP entre serviços, com os respetivos tempos limite e mecanismos de proteção contra cascatas de falhas (discutidos em §4.4). Cada chamada introduz, por construção, três custos: latência de rede, possibilidade de falha pelo serviço destino estar indisponível, e dependência runtime — o serviço que faz o pedido fica acoplado, em tempo de execução, ao serviço que o atende. Estes custos eram inexistentes no monólito, onde toda a coordenação ocorria dentro do mesmo processo.

#### Comunicação assíncrona (Kafka)

Optou-se por mensagens assíncronas via Kafka sempre que a resposta não é necessária para prosseguir. Os dois casos relevantes na arquitetura proposta são o processamento de pagamento e o envio de notificações. Em ambos, o produtor do evento — o serviço de encomendas ou outro qualquer — completa a sua operação imediatamente após publicar a mensagem, sem esperar pelo seu consumo. O processamento do pagamento e o envio da notificação ocorrem posteriormente, em consumidores independentes, sem bloquear a resposta ao utilizador.

A vantagem é direta: o tempo de resposta percebido pelo utilizador deixa de incluir operações intrinsecamente lentas (interação com gateways de pagamento externos) ou não-críticas (envio de correio eletrónico). A desvantagem é igualmente direta: a coordenação entre serviços perde a garantia transacional que existia no monólito. O estado final do sistema passa a depender do consumo correto e atempado de mensagens, e essa garantia tem de ser reconstruída por outras vias.

#### Limitações da comunicação assíncrona não resolvidas neste protótipo

A literatura sobre sistemas distribuídos identifica três classes de problemas que decorrem da utilização de mensageria assíncrona, e que requerem padrões adicionais para serem resolvidos. As três foram deliberadamente **não implementadas** neste trabalho, e a sua omissão é assumida como limitação central da proposta — uma limitação que sustenta empiricamente a investigação à QI3 (§5.3).

- **Entrega duplicada de mensagens.** O Kafka, como a maioria dos sistemas de mensageria distribuída, oferece entrega *at-least-once*: a mesma mensagem pode ser entregue mais do que uma vez em caso de falha do consumidor antes da confirmação, ou de reprocessamento controlado. Sem proteção, um evento de pagamento entregue duas vezes resultaria em dois pagamentos para a mesma encomenda. A solução é introduzir **idempotência** ao nível do consumidor: cada mensagem teria um identificador único e o serviço verificaria se já a processou antes de agir. Não foi implementada.

- **Ausência de compensação transacional entre serviços.** No monólito, a transação ACID garantia que ou todas as operações de um fluxo se aplicavam, ou nenhuma se aplicava. Nos microserviços com mensageria assíncrona, esta garantia desaparece: o stock pode ser decrementado por uma chamada síncrona e o pagamento falhar posteriormente no consumidor assíncrono, deixando o sistema num estado inconsistente sem mecanismo automático de reversão. A solução conhecida na literatura é o padrão **Saga**: uma sequência de transações locais coordenadas, em que cada passo tem associada uma ação compensatória explícita (no caso, o restauro de stock seria a compensação do decremento). Não foi implementado.

- **Mensagens perdidas após falhas repetidas.** Se um consumidor falhar consistentemente ao processar uma mensagem (por erro persistente, *bug*, ou indisponibilidade prolongada), a mensagem é descartada e perde-se. A solução padrão é configurar uma **dead-letter queue**: após N tentativas falhadas, a mensagem é encaminhada para uma fila separada, onde pode ser inspecionada e reprocessada manualmente, mantendo-se assim a auditabilidade completa do fluxo. Não foi configurada.

A não-implementação destes três padrões é uma decisão metodológica, e não uma omissão técnica: o protótipo destina-se precisamente a expor o que acontece quando microserviços comunicam por eventos sem as proteções que tornariam a sua utilização correta em produção. A análise empírica deste cenário, conduzida em §5.3 e §6.3.5, sustenta a resposta à QI3.

### 4.4 Resiliência das Comunicações Síncronas

A introdução de chamadas remotas obriga a planear para falhas que não existiam no monólito. Uma chamada bloqueante a um serviço temporariamente indisponível esgota recursos no cliente — *threads* ocupadas a aguardar resposta, ligações abertas, memória reservada — e pode propagar a indisponibilidade em cascata. Se o serviço de encomendas bloqueia à espera do serviço de inventário, o gateway bloqueia à espera do serviço de encomendas, e os utilizadores vêem o sistema inteiro indisponível por causa de um único componente caído.

A proposta endereça este risco em duas camadas. Todas as rotas do gateway são protegidas por *circuit breakers* (implementados com a biblioteca Resilience4j), que cortam automaticamente os pedidos a um serviço que esteja a falhar repetidamente. Em vez de tentar e falhar com cada pedido, o sistema deteta o padrão de falhas, abre o circuito e devolve imediatamente uma resposta degradada, libertando recursos para os pedidos que ainda têm hipótese de sucesso. Periodicamente, o circuito tenta uma nova chamada para verificar se o serviço recuperou. As chamadas síncronas entre serviços (por exemplo, do serviço de encomendas para o de inventário) estão também protegidas por *timeouts*, que limitam o tempo máximo de espera por uma resposta.

A consequência prática é o isolamento da falha ao seu domínio: a paragem do serviço de catálogo não bloqueia indefinidamente o serviço de encomendas, e os pedidos para o catálogo recebem rapidamente uma resposta degradada em vez de propagarem a indisponibilidade. Esta propriedade é demonstrada experimentalmente em §6.3.2.

Importa, contudo, reconhecer o que estes mecanismos **não** resolvem. Os *circuit breakers* protegem contra cascatas de falhas — a degradação não se espalha — mas não resolvem problemas de consistência de dados. Se o serviço de encomendas aceitar um pedido e o serviço de inventário falhar imediatamente a seguir, nenhum *circuit breaker* desfaz a encomenda já criada. A consistência distribuída pertence ao domínio dos padrões discutidos na secção anterior (Saga, idempotência, *outbox*) e não pode ser confundida com a resiliência da comunicação síncrona.

### 4.5 Persistência: Database per Service

A decomposição por domínios implica isolamento dos dados. Se o serviço de encomendas pudesse ler diretamente da base de dados do serviço de catálogo, ficaria acoplado ao esquema interno desse serviço, e qualquer alteração ao modelo do catálogo poderia corromper o funcionamento do primeiro — reproduzindo, entre serviços, exatamente o tipo de acoplamento que a decomposição pretendia eliminar. O padrão *database per service* resolve esta questão ao estabelecer que cada serviço é o proprietário exclusivo dos seus dados.

Foram consideradas três opções para a sua implementação. A primeira, mais simples de operar, consiste em manter uma única instância PostgreSQL com bases de dados lógicas separadas — opção que preserva o isolamento *lógico* dos dados mas mantém um ponto único de falha *físico*: se a instância partilhada falhar, todos os serviços ficam simultaneamente indisponíveis, contradizendo a razão de ter decomposto o sistema. A segunda opção consistiria em utilizar tecnologias diferentes por domínio (por exemplo, Redis para o carrinho, dada a natureza efémera dos seus dados, e PostgreSQL para os restantes). Esta opção seria mais fiel ao padrão tal como descrito na literatura, mas introduziria complexidade operacional fora do âmbito do trabalho. A opção adotada é uma instância PostgreSQL dedicada por serviço, todas com a mesma tecnologia para preservar a homogeneidade do ambiente, mas com isolamento físico real entre processos.

O que esta opção continua a não resolver são as garantias transacionais entre serviços. Não existe, na proposta, qualquer mecanismo que garanta que o stock decrementado num serviço só é confirmado se a encomenda for criada com sucesso num outro. Esta limitação justifica a discussão de Saga apresentada em §7.4 como linha de trabalho futuro.

### 4.6 O Papel do API Gateway

Sem um ponto de entrada centralizado, o cliente teria de conhecer os endereços de cada serviço, gerir o token de autenticação em cada chamada e lidar individualmente com as falhas de cada um. O *API Gateway* resolve este problema concentrando quatro responsabilidades arquiteturais: é o único ponto de entrada externo, executa o roteamento por caminho do URL para o serviço correto, valida o token JWT em cada pedido e injeta a identidade do utilizador como cabeçalhos próprios antes de encaminhar, e ativa respostas degradadas quando um serviço destino está indisponível.

O gateway introduz, contudo, dois riscos. Constitui um novo ponto único de falha — risco mitigável em produção pela replicação em múltiplas instâncias, mas não mitigado neste protótipo — e adiciona uma camada extra a cada pedido, com o consequente impacto em latência. É também importante notar que os mecanismos de resiliência ao seu nível protegem contra cascatas de falhas, mas não contra problemas de consistência de dados: se o serviço de encomendas aceitar um pedido e o serviço de inventário falhar imediatamente a seguir, nenhum *circuit breaker* desfaz a encomenda já criada.

---

## 5. Análise de Workflows

A análise comparativa entre as duas arquiteturas concentra-se em três fluxos representativos: um fluxo de leitura simples (consulta do catálogo), um fluxo transacional crítico (checkout) e um cenário de falha parcial controlada. A escolha destes três casos cobre o espectro de complexidade relevante para as três questões de investigação: o primeiro mostra o custo da decomposição em ausência de coordenação inter-serviço; o segundo evidencia o custo quando a coordenação é múltipla e síncrona; o terceiro expõe a fragilidade da consistência distribuída sem padrões de compensação.

### 5.1 Fluxo Simples: Consulta do Catálogo

A consulta do catálogo é a operação de leitura mais frequente num sistema de comércio eletrónico e representa o caso de menor complexidade arquitetural: uma única consulta à base de dados, sem lógica de negócio adicional. No monólito, o pedido é servido diretamente pelo processo aplicacional. Na proposta de microserviços, atravessa apenas dois processos: o gateway, que valida e encaminha, e o serviço de catálogo, que executa a consulta.

Não existe, neste fluxo, partilha de dados entre serviços. O potencial de inconsistência é nulo. As medições experimentais correspondentes são apresentadas em §6.

### 5.2 Fluxo Transacional: Checkout

O checkout é a operação mais complexa do sistema e a mais ilustrativa do trade-off arquitetural em estudo. No monólito executa numa única transação JPA: o serviço lê o utilizador, lê o carrinho, valida o stock dos produtos (operação adicionada para paridade — ver §6.2), cria a encomenda, esvazia o carrinho. Toda esta sequência decorre num único processo, com transação atómica garantida pelo motor JPA.

Na proposta de microserviços, a mesma operação lógica requer a coordenação de cinco serviços distintos. A figura 1 apresenta o sequência completo: o serviço de encomendas lê o carrinho a partir do serviço correspondente, verifica o stock no serviço de inventário, obtém o preço atual no serviço de catálogo, decrementa o stock novamente no serviço de inventário, persiste a encomenda na sua própria base de dados, esvazia o carrinho e publica um evento que será consumido posteriormente pelo serviço de pagamento. A sequência envolve cinco chamadas síncronas em série e uma publicação assíncrona.

Esta multiplicação de pontos de coordenação tem três consequências que se manifestam empiricamente em §6. A latência total do fluxo é a soma das latências individuais, acrescidas de serialização HTTP e atravessamentos de fronteiras de processo. O número de modos de falha aumenta proporcionalmente ao número de serviços envolvidos. E, por fim, qualquer das chamadas síncronas que falhe deixa o sistema num estado intermédio: por exemplo, se o stock for decrementado e o pagamento falhar a seguir (cenário analisado em §5.3), a inconsistência é real e não é desfeita automaticamente.

> *Figura 1 — Sequence diagram comparativo do checkout (a inserir).*

### 5.3 Falha Parcial Controlada

O terceiro fluxo não é um caminho-feliz: é a injeção deliberada de uma falha a meio do checkout, com o objetivo de observar o estado dos dados quando a coordenação é interrompida. O cenário consiste em parar o serviço de pagamento imediatamente após a publicação do evento que esse serviço deveria consumir, mas antes do consumo efetivo.

No monólito, a falha equivalente — a paragem da base de dados durante a transação — é tratada pelo motor JPA: o *rollback* automático aborta a transação antes de qualquer alteração ser persistida. O sistema regressa a um estado consistente sem intervenção.

Na proposta de microserviços, o resultado é qualitativamente distinto. O stock fica decrementado (porque a respetiva chamada síncrona já tinha sido concluída antes da falha). A encomenda fica registada num estado intermédio. A mensagem publicada permanece no Kafka sem consumidor. Não existe qualquer mecanismo automático que reverta o decremento de stock ou conclua a encomenda — o sistema permanece num estado que se designa, na literatura de sistemas distribuídos, como *zombie*. Este resultado é a evidência central que sustenta H3, e é o tipo de problema que padrões como Saga ou *outbox* — não implementados neste protótipo — viriam mitigar.

---

## 6. Avaliação Experimental

### 6.1 Metodologia

Os testes foram executados em ambiente local (Apple M4 Pro, 48 GB RAM), com ambas as arquiteturas em contentores Docker e em redes isoladas para evitar interferência. A ferramenta de carga utilizada foi o k6, que regista a duração de cada pedido individualmente e calcula percentis (mediana, p95) sobre a amostra completa de uma janela de medição de 60 segundos, precedida de 30 segundos de aquecimento.

Os testes correm sem *think time* entre iterações, opção deliberada para saturar a arquitetura e medir o seu comportamento sob pressão sustentada. Esta metodologia é mais conservadora que a alternativa de simular utilizadores reais com pausas, no sentido em que qualquer diferença observada sob saturação manifestar-se-á também sob carga moderada — o inverso não sendo, no entanto, verdadeiro.

Os percentis reportados são calculados pelo k6 sobre todos os pedidos da janela de medição. Os ficheiros de saída — em formato CSV e JSON — encontram-se arquivados nos diretórios de testes do repositório e são referenciados como evidência primária.

### 6.2 Paridade Funcional

Antes de iniciar a comparação experimental, foi necessário corrigir duas limitações do monólito original que distorceriam os resultados se mantidas. O sistema original não validava o stock no checkout, apesar do campo correspondente existir; e os controladores utilizavam um identificador de utilizador fixo, em vez de extrair a identidade do token JWT. Estas duas omissões, sendo funcionais e não arquiteturais, foram corrigidas para garantir que a comparação de desempenho mede de facto o custo da decomposição arquitetural, e não o custo de funcionalidade ausente numa das versões.

A correção da validação de stock no monólito acrescenta uma consulta SQL adicional ao checkout (verificação da quantidade disponível), equivalente funcional da chamada síncrona ao serviço de inventário no caso microserviços. A correção da identidade hardcoded substitui o valor fixo pela extração do *Spring Security Context*, o que impõe uma consulta adicional ao repositório de utilizadores — equivalente funcional dos cabeçalhos `X-User-Name` injetados pelo gateway no caso microserviços.

A operação de pagamento, ausente do monólito, **não tem equivalente funcional adicionado**. Trata-se, recorde-se, de uma operação assíncrona via Kafka, e a sua execução não influencia a latência observável do checkout (que termina, no caso microserviços, com a ordem em estado intermédio enquanto o pagamento decorre num *thread* separado). Inclui-la no monólito acrescentaria latência sem corresponder a paridade real, dado que o monólito a executaria sincronamente.

### 6.3 Resultados por Atributo de Qualidade

A apresentação dos resultados segue os quatro atributos de qualidade explicitamente identificados no enunciado da unidade curricular: desempenho, disponibilidade, *deployability* e custo operacional.

#### 6.3.1 Desempenho

A medição do desempenho assenta nos dois fluxos representativos identificados em §5: a consulta do catálogo (um único hop) e o checkout (cinco hops em série). Os resultados estão sumariados na tabela 1.

**Tabela 1 — Latências comparadas (carga sustentada, paridade funcional)**

| Fluxo | Métrica | Monólito | Microserviços | Variação |
|-------|---------|----------|---------------|----------|
| Consulta do catálogo | mediana | 2,3 ms | 1,9 ms | −17 % |
| Consulta do catálogo | percentil 95 | 3,4 ms | 2,9 ms | −15 % |
| Consulta do catálogo | requests servidos | 329 444 | 404 277 | +23 % |
| Checkout | mediana (iteração) | 2,6 ms | 7,6 ms | +194 % |
| Checkout | percentil 95 (iteração) | 3,9 ms | 13,5 ms | +245 % |
| Checkout | iterações servidas | 36 604 | 11 825 | −68 % |

A leitura conjunta destas duas medições é o resultado central do trabalho relativamente à QI2. No fluxo simples, a decomposição **não penaliza** o desempenho: os microserviços apresentam latência mediana e percentil 95 inferiores às do monólito, com vantagem adicional de 23 % no número de pedidos servidos. Este resultado, contraintuitivo, deriva do isolamento do processo associado ao serviço de catálogo: a sua máquina virtual Java é dedicada exclusivamente a esse domínio, com cache e *garbage collector* otimizados para esse uso, ao passo que o monólito partilha estes recursos entre todos os domínios.

No fluxo transacional, no entanto, a decomposição penaliza o desempenho de forma dramática. O percentil 95 do checkout multiplica-se por 3,5 face ao monólito, e o número de operações por segundo cai em 68 %. A diferença entre os dois resultados — uma operação simples não penalizada, uma operação complexa fortemente penalizada — é incompatível com qualquer crescimento linear do custo com o número de chamadas. Se o custo crescesse linearmente com o número de hops, esperar-se-ia, partindo de uma penalização nula em um hop, qualquer coisa entre nenhuma penalização e uma penalização equivalente a quatro vezes a unitária. A penalização observada (mais de duzentas vezes a unitária) é compatível apenas com **crescimento super-linear**, conforme previsto por H2a. Este crescimento é atribuível à acumulação de serializações HTTP, atravessamentos de fronteiras de processo e dessincronização entre os ciclos de *garbage collection* das diferentes máquinas virtuais.

**H2a confirma-se** em ambos os sentidos: o custo da decomposição é negligenciável (ou favorável) em fluxos simples e dramaticamente penalizador em fluxos com múltiplas coordenações síncronas.

#### 6.3.2 Disponibilidade

A avaliação da disponibilidade consiste em provocar a falha de um componente e observar o impacto sobre o sistema. No monólito, a paragem da base de dados torna todos os endpoints simultaneamente indisponíveis em poucos segundos: autenticação, catálogo, encomendas, todos os domínios partilham o mesmo *connection pool* e todos sucumbem à mesma falha. A recuperação, após o restabelecimento da base de dados, demora cerca de quinze segundos para o reestabelecimento completo.

Na proposta de microserviços, a paragem do serviço de catálogo provoca o ativamento do *circuit breaker* correspondente no gateway, que passa a devolver respostas degradadas para os pedidos a este serviço. Os restantes endpoints — autenticação, listagem de encomendas, consulta do carrinho — mantêm a sua disponibilidade durante a totalidade da interrupção.

O ganho em disponibilidade é, portanto, **real mas restrito a operações que não dependam do serviço caído**. O checkout, que coordena cinco serviços em série, falha se qualquer um deles falhar — uma manifestação de que o acoplamento foi redistribuído (do *connection pool* partilhado para o conjunto de chamadas síncronas), não eliminado.

#### 6.3.3 Deployability

A *deployability* não é mensurável quantitativamente da mesma forma que as anteriores, mas é observável qualitativamente. No monólito, qualquer alteração — mesmo a um único módulo — implica a reconstrução e o redeployment integrais da aplicação, com interrupção temporária de todas as funcionalidades. Na proposta de microserviços, a atualização de um serviço é independente das restantes: durante o redeployment do serviço de catálogo, o gateway ativa a resposta degradada para esse domínio e os outros serviços continuam disponíveis sem interrupção.

Esta propriedade — apresentada na literatura como uma das principais motivações para a decomposição — é confirmada empiricamente no protótipo. O seu valor pleno, no entanto, manifestar-se-ia num contexto de equipas paralelas a iterar sobre domínios distintos a ritmos distintos, contexto este ausente da experiência atual.

#### 6.3.4 Custo Operacional

O custo operacional é avaliado em termos de consumo de memória e de número de componentes a manter operacionais. As medições correspondentes, em condições de paridade funcional, apresentam-se na tabela 2.

**Tabela 2 — Recursos consumidos em estado ocioso**

| Recurso | Monólito | Microserviços | Variação |
|---------|----------|---------------|----------|
| Memória RAM total | aprox. 1,3 GiB | aprox. 5,3 GiB | × 4,1 |
| Número de contentores | 3 | 17 | × 5,7 |
| Componentes de infraestrutura | 1 (PostgreSQL) | 4 (Kafka, Zookeeper, 7 PostgreSQL, observabilidade) | — |

O consumo de memória dos microserviços é aproximadamente quatro vezes superior ao do monólito, em condições de inatividade. O facto de a memória não variar significativamente entre estado ocioso e sob carga (em ambas arquiteturas) é típico do padrão de gestão de memória da máquina virtual Java, com *heap* pré-alocado: o custo é estrutural, e existe mesmo na ausência de utilizadores. **H2b confirma-se**: o custo cresce aproximadamente em proporção ao número de processos.

A este custo de memória soma-se um custo de complexidade operacional não trivial: dezassete componentes a monitorizar, configurar e manter atualizados, em vez de três. O ponto é particularmente relevante para a discussão final, dado que o protótipo não exerce as propriedades — escalabilidade independente, autonomia de equipas — que poderiam justificar este custo adicional.

#### 6.3.5 Falha Parcial e Consistência

A QI3 é avaliada pelo cenário descrito em §5.3. O resultado, sumariado na tabela 3, é qualitativamente distinto entre as duas arquiteturas.

**Tabela 3 — Estado do sistema após falha parcial**

| Componente | Monólito | Microserviços |
|------------|----------|---------------|
| Stock | sem alteração (rollback automático) | decrementado |
| Encomenda | não criada (rollback automático) | criada em estado intermédio |
| Mensagem publicada | n/a | sem consumidor disponível |
| Recuperação | automática | exige intervenção manual |

**H3 confirma-se**. Na ausência de mecanismos de compensação (Saga, idempotência, *dead-letter queue*), os microserviços apresentam um estado inconsistente que não pode ocorrer no monólito. A consequência prática é importante: a consistência distribuída não é uma propriedade gratuita dos microserviços — é uma propriedade que tem de ser explicitamente reconstruída, com esforço adicional, em substituição da garantia transacional que o monólito oferecia automaticamente.

---

## 7. Discussão

### 7.1 Resposta às Questões de Investigação

Os resultados experimentais sustentam as três hipóteses formuladas em §1.3. A QI1 confirma-se pela contagem das categorias de dependência entre domínios: as dependências estruturais compile-time (chaves estrangeiras, entidades partilhadas) são eliminadas, mas o seu lugar é ocupado por dependências runtime (chamadas síncronas, eventos assíncronos, identidade propagada por cabeçalhos). O total de dependências cross-domain não diminui — apenas muda de natureza, deslocando-se do plano estático para o plano dinâmico. A redistribuição é tangível e mensurável.

A QI2 confirma-se em ambas as suas componentes. O custo de latência da decomposição é negligenciável ou negativo em fluxos simples — o serviço dedicado pode mesmo ser mais rápido do que o monólito por benefício de isolamento de processo — mas cresce de forma super-linear à medida que o fluxo passa a coordenar múltiplos serviços. A penalização observada no checkout (3,5 vezes face ao monólito, com cinco chamadas em série) é incompatível com crescimento linear e compatível com a acumulação não-linear de custos de serialização, atravessamento de fronteiras e dessincronização de processos. O custo de memória, por seu turno, é aproximadamente proporcional ao número de processos e existe mesmo na ausência de utilizadores.

A QI3 confirma-se de forma qualitativa pelo cenário de falha parcial. A comunicação assíncrona, sem padrões adicionais de compensação, regride a consistência face ao baseline transacional do monólito. O ganho de desacoplamento temporal — o utilizador não tem de esperar pelo processamento do pagamento — é pago em complexidade de consistência: é necessário desenhar e implementar Saga, idempotência e *dead-letter queues*, padrões nenhum dos quais trivial. O monólito oferecia, sob esta perspetiva, gratuitamente uma propriedade que os microserviços têm de reconquistar com esforço adicional.

### 7.2 Limitações da Solução Proposta

A proposta TO-BE tem limitações conhecidas e assumidas, que importa explicitar para enquadrar adequadamente a interpretação dos resultados.

A primeira é a ausência de **padrões de compensação**: nem Saga, nem idempotência, nem *dead-letter queue* foram implementados. A consequência é a possibilidade real de inconsistência sob falha parcial, exatamente como evidencia o cenário do estado *zombie*. A escolha foi deliberada para sustentar QI3, mas representa uma limitação de produção que teria de ser endereçada num sistema real.

A segunda é a natureza de **simulação** do serviço de pagamento. O processamento real de um pagamento envolve a integração com gateways externos, sujeitos a latência variável, falhas transitórias e exigências de segurança. A simulação adotada (atraso constante, probabilidade fixa de falha) é suficiente para o objetivo experimental, mas não substitui um teste com o pagamento real.

A terceira é a **autenticação por JWT artesanal**. O modelo adotado (assinatura HMAC com segredo partilhado) é adequado a um protótipo, mas em produção exigiria rotação de chaves ou a integração com um *Identity Provider* externo, com revogação e renovação de tokens.

### 7.3 Auditoria das Próprias Fronteiras

Durante o trabalho, a separação inicial entre serviço de autenticação e serviço de utilizador foi auditada criticamente à luz dos princípios DDD e revelou-se *over-engineering*. Os dois serviços não modelavam conceitos distintos do domínio, não tinham linguagem ubíqua diferenciada e a sua operação obrigava a sincronização cruzada em todas as criações de utilizador. A fusão num único serviço de identidade simplificou a operação sem perda de propriedades arquiteturais.

Esta experiência sugere uma heurística operacional para identificar bounded contexts genuínos: **fronteiras que exigem sincronização imediata em ambos os sentidos não são bounded contexts genuínos — são módulos que não deveriam ter sido separados**. A decomposição apenas se justifica quando há autonomia genuína de domínio: linguagem distinta, ciclos de vida desalinhados, equipas potencialmente independentes. Sem estas condições, a decomposição introduz custos sem benefícios correspondentes.

### 7.4 O que a Decomposição não Resolve

Os resultados sugerem que a decomposição em microserviços é uma reorganização de problemas arquiteturais, e não a sua eliminação. Há limitações que persistem ou que são introduzidas pela própria decomposição.

A consistência distribuída requer, como visto, padrões adicionais não triviais de compensação. A complexidade operacional aumenta proporcionalmente ao número de componentes, mesmo quando estes não estão a ser exercitados. O *debugging* distribuído exige ferramentas dedicadas (rastreio distribuído com Jaeger, métricas com Prometheus, logs centralizados) sem as quais é impraticável correlacionar acontecimentos entre processos.

A escalabilidade horizontal por componente — frequentemente apresentada como o argumento principal a favor de microserviços — não foi exercitada neste trabalho. A literatura sustenta que, sob carga assimétrica entre domínios, a possibilidade de escalar apenas o componente saturado representa um ganho efetivo. A demonstração empírica desta propriedade exige, no entanto, infraestrutura de orquestração com múltiplos nós e múltiplas instâncias por serviço, fora do âmbito de um protótipo local.

---

## 8. Contributo do Grupo

Para clarificar a natureza da contribuição original, importa distinguir o que foi reaproveitado, o que foi adaptado e o que foi desenhado e implementado raiz pelo grupo.

O **monólito de partida** é um projeto Spring Boot pré-existente, utilizado como caso de estudo. O grupo estudou-o, identificou as suas limitações arquiteturais e funcionais, e introduziu duas correções funcionais necessárias para garantir paridade na avaliação experimental: a validação de stock no checkout e a extração da identidade do utilizador a partir do token JWT (em substituição do identificador fixo).

A **proposta de microserviços** foi desenhada e implementada raiz pelo grupo, incluindo a definição das fronteiras entre serviços, a escolha dos padrões de comunicação (síncrona vs. assíncrona) por fluxo, a configuração do gateway e dos *circuit breakers*, a estrutura de eventos Kafka entre serviços e a auditoria crítica que conduziu à fusão do serviço de autenticação com o de utilizador num único serviço de identidade.

O **protocolo experimental** — desenho dos cenários de teste, paridade funcional aplicada ao monólito, métricas recolhidas e interpretação à luz dos quatro atributos de qualidade — foi também original do grupo. As ferramentas utilizadas (k6 para carga, Jaeger para rastreio, Prometheus e Grafana para métricas, Kafdrop para inspeção de eventos) são padrão da indústria e foram integradas ao trabalho.

A discussão dos resultados, a auditoria das próprias decisões arquiteturais e a articulação das três questões de investigação são contribuição metodológica do grupo. Em particular, a distinção entre serviços de domínio genuíno e serviços com função de instrumentação experimental (inventário e pagamento), e a auditoria crítica da fronteira inicial entre autenticação e utilizador, são análises originais que não correspondem a aplicação mecânica de princípios genéricos.

---

## 9. Conclusões e Trabalho Futuro

### 9.1 Conclusões

O trabalho avaliou empiricamente, em condições de paridade funcional, a transição de uma aplicação de comércio eletrónico de uma arquitetura monolítica para uma arquitetura baseada em microserviços. As três questões de investigação que orientaram o estudo foram respondidas com base em medições controladas e na observação de cenários de falha.

A primeira conclusão é que a decomposição em microserviços **não elimina o acoplamento**, redistribui-o. As dependências estáticas entre módulos (chaves estrangeiras, entidades partilhadas, deployment conjunto) são substituídas por dependências dinâmicas equivalentes (chamadas síncronas, eventos assíncronos, identidade propagada). O total de dependências entre domínios não diminui.

A segunda conclusão é que o **custo da decomposição é desproporcionalmente penalizador para fluxos complexos**. Operações simples não são penalizadas e podem mesmo beneficiar do isolamento de processo. Operações que coordenam múltiplos serviços em série pagam, no entanto, um custo super-linear: a multiplicação por 3,5 do percentil 95 do checkout não é explicável por crescimento linear do número de chamadas. O custo de memória é, por seu turno, aproximadamente proporcional ao número de processos e existe mesmo na ausência de utilizadores.

A terceira conclusão é que a **comunicação assíncrona, sem padrões adicionais de compensação, regride a consistência sob falha parcial**. O ganho de desacoplamento temporal é pago em complexidade de consistência: padrões como Saga, idempotência e *dead-letter queues* são exigências resultantes da própria decomposição, e não sofisticações opcionais.

A apreciação global, à luz dos quatro atributos de qualidade considerados, é mista. A decomposição melhora claramente a *deployability* e parcialmente a disponibilidade. Penaliza dramaticamente o desempenho em operações multi-domínio e tem custo operacional substancialmente superior. O ganho líquido depende de fatores não exercitados no protótipo: equipas paralelas a evoluir domínios distintos a ritmos distintos, e carga assimétrica entre domínios que justifique escalonamento independente. Para a escala e o contexto do XCommerce no estado atual, o custo arquitetural excede os benefícios técnicos colhidos. Este resultado, longe de invalidar a decomposição, esclarece as condições em que se torna efetivamente vantajosa.

### 9.2 Trabalho Futuro

Várias linhas de continuação são identificáveis. A implementação dos padrões de compensação (Saga com *outbox pattern*, idempotência ao nível dos consumidores Kafka, *dead-letter queues* configuradas) eliminaria a regressão de consistência demonstrada em §6.3.5 e tornaria a comparação mais favorável aos microserviços nesta dimensão. A demonstração empírica do ganho de escalabilidade horizontal exigiria orquestração com Kubernetes ou equivalente, com auto-scaling baseado em métricas por serviço, e cenários de carga assimétrica entre domínios. A integração com um *Identity Provider* externo (Keycloak, Auth0) substituiria o JWT artesanal e introduziria realismo adicional. Por fim, testes em escala superior — múltiplas máquinas, milhares de utilizadores simultâneos — permitiriam identificar pontos de saturação distintos em cada arquitetura, possivelmente alterando a apreciação do custo da decomposição em escalas onde o monólito se torna ele próprio limitado.

---

## Referências

- Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.
- Fowler, M., & Lewis, J. (2014). *Microservices: a definition of this new architectural term*. martinfowler.com.
- Newman, S. (2021). *Building Microservices* (2nd ed.). O'Reilly Media.
- Richardson, C. (2018). *Microservices Patterns: With examples in Java*. Manning Publications.

---

## Anexos

Os elementos seguintes constam dos anexos arquivados no repositório do projeto e na partilha do grupo, sendo aqui apenas referenciados:

- detalhes da estrutura interna do código (pacotes, classes e dependências) das duas arquiteturas;
- ficheiros de configuração de infraestrutura (Docker Compose, Prometheus, gateway);
- resultados brutos das medições experimentais (CSV e JSON) dos cenários T1, T2, T3, T5 e Workflow 4;
- diagramas de sequência detalhados para os fluxos analisados em §5;
- capturas de ecrã das ferramentas de observabilidade (Jaeger, Grafana, Kafdrop) ilustrando os cenários discutidos;
- excertos relevantes do código que implementa as decisões arquiteturais discutidas (controllers, configuração de circuit breakers, consumidores Kafka).
