# 03-tests — Testes HTTP XCommerce

Testes funcionais para o Monólito e Microserviços usando ficheiros `.http` (compatíveis com IntelliJ HTTP Client, VS Code REST Client, e JetBrains).

## Estrutura

```
03-tests/
├── monolito/               # Testes para o monólito (porta 8080)
│   ├── auth.http           # Login, registo, atualização de perfil
│   ├── shopping-cart.http  # Carrinho de compras
│   └── orders.http         # Encomendas e backoffice
│
├── microservicos/          # Testes para microserviços via Gateway (porta 9000)
│   ├── auth.http           # Identity Service — login, registo, gestão de utilizadores
│   ├── catalog.http        # Catalog Service — produtos e categorias
│   ├── shopping-cart.http  # Cart Service — gestão do carrinho
│   ├── orders.http         # Order Service — encomendas e backoffice
│   └── inventory-notifications.http  # Serviços internos (inventário, notificações)
│
└── scenarios/              # Cenários end-to-end realistas
    ├── scenario-1-novo-utilizador-compra.http   # Registo → explorar → comprar
    ├── scenario-2-gestao-carrinho.http           # Adicionar/remover/checkout
    └── scenario-3-admin-backoffice.http          # Gestão admin completa
```

## Como usar

### Pré-requisitos
- VS Code com extensão [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- **OU** IntelliJ IDEA / WebStorm (suporte nativo a `.http`)

### Autenticação

1. Abrir `auth.http` (monolito ou microservicos)
2. Executar o request de login/authenticate
3. Copiar o token da resposta
4. Substituir `SEU_TOKEN_AQUI` nos ficheiros que precisam de autenticação

### Cenários

Os ficheiros em `scenarios/` têm os passos numerados e comentados — executar **na ordem indicada** para simular o fluxo completo.

| Cenário | O que simula |
|---------|-------------|
| Cenário 1 | Novo utilizador: registo → explorar catálogo → 1ª compra → review |
| Cenário 2 | Utilizador habitual: adiciona vários produtos, muda de ideias, checkout |
| Cenário 3 | Admin: cria produtos, processa encomendas, envia notificações |

## URLs

| Serviço | URL |
|---------|-----|
| Monólito | `http://localhost:18080` |
| API Gateway (microserviços) | `http://localhost:9000` |
| Jaeger (tracing) | `http://localhost:16686` |
| Grafana (métricas) | `http://localhost:3000` |
