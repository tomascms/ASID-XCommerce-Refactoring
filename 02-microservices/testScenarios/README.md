# Cenários de Teste de Performance (k6)

Este diretório contém os scripts de teste de carga e performance desenvolvidos utilizando a ferramenta **k6**. Os testes visam validar a escalabilidade e o tempo de resposta das APIs do XCommerce sob uma carga padronizada de **50 usuários simultâneos**.

## 🚀 Como Executar

> Nota: estes scripts refletem parcialmente o fluxo legado. Para correr contra a stack atual, o Gateway está em `http://localhost:9000` e a autenticação já depende de Keycloak/JWT.
> Se um script ainda apontar para `8080` ou usar login local antigo, ele precisa ser atualizado antes de reutilização.

### Execução em Lote
Para rodar todos os testes sequencialmente e gerar relatórios automáticos:
```bash
./testScenarios/run-all.sh
```

### Execução Individual
Para rodar um script específico:
```bash
k6 run testScenarios/<subpasta>/<arquivo>.js
```

---

## 👥 O que são VUS (Virtual Users)?

No contexto do k6, **VUs (Virtual Users)** são entidades que executam o script de teste de forma paralela e independente. 

- **Simulação Realista:** Cada VU representa um usuário único interagindo com o sistema simultaneamente.
- **Concorrência:** Diferente de uma execução sequencial, as VUs permitem testar como o servidor gerencia múltiplas requisições ao mesmo tempo (condições de corrida, bloqueios de banco de dados, limites de memória).
- **Consumo de Recursos:** Cada VU consome recursos da máquina que executa o teste (CPU e RAM). Padronizamos este projeto para **50 VUs**, o que representa uma carga moderada a alta para uma aplicação monolítica.

---

## 📂 Estrutura de Testes e Detalhes de Carga

Todos os scripts abaixo foram configurados para rodar com **50 VUs simultâneas**.

### 1. Gestão de Usuários (`user_management/`)
- **`authenticate.js`**: Valida login e tokens JWT.
    *   **Carga:** 50 VUs (Ramp-up e Estabilidade).
- **`create_user.js`**: Criação de novos clientes comuns.
    *   **Carga:** 50 VUs.
- **`create_admin.js`**: Criação de contas administrativas.
    *   **Carga:** 50 VUs.
- **`update_user.js`**: Atualização de dados cadastrais.
    *   **Carga:** 50 VUs.

### 2. Carrinho de Compras (`shopping_cart/`)
- **`get_cart.js`**: Recupera o estado do carrinho.
    *   **Carga:** 50 VUs.
- **`add_product.js`**: Adiciona produtos ao carrinho.
    *   **Carga:** 50 VUs.
- **`decrease_quantity.js`**: Reduz quantidade de itens.
    *   **Carga:** 50 VUs.
- **`remove_product.js`**: Remove itens do carrinho.
    *   **Carga:** 50 VUs.

### 3. Pedidos e Backoffice (`orders/`)
- **`checkout.js`**: Finalização de compra.
    *   **Carga:** 50 VUs.
- **`list_orders.js`**: Listagem de pedidos do usuário.
    *   **Carga:** 50 VUs.
- **`backoffice_list.js`**: Consulta administrativa de pedidos.
    *   **Carga:** 50 VUs.
- **`update_status.js`**: Atualização de status de entrega.
    *   **Carga:** 50 VUs.

### 4. Cenários de Fluxo Completo (`scenarios/`)
- **`sale-checkout.js`**: Fluxo end-to-end completo (Setup ➡️ Get ➡️ Add ➡️ Checkout).
    *   **Carga:** **Padronizada para 50 VUs**, mas aceita customização via parâmetro `VUS`.
    *   **Exemplo:** `k6 run -e VUS=100 testScenarios/scenarios/sale-checkout.js`

---

## 📊 Relatórios e Saída
Todos os scripts estão configurados para exportar um resumo detalhado em formato JSON para a pasta:
`testScenarios/output/`

Os arquivos seguem a nomenclatura `<nome-do-teste>.json`.
