# Cenários de Teste de Performance (k6)

Este diretório contém os scripts de teste de carga e performance desenvolvidos utilizando a ferramenta **k6**. Os testes visam validar a escalabilidade e o tempo de resposta das APIs do XCommerce em duas arquiteturas: **Monolítica (MO)** e **Microserviços (MS)**.

## 🚀 Como Executar

Todos os testes exigem a definição da variável de ambiente `prefix` (valor `mo` ou `ms`) para direcionar as requisições para a arquitetura correta.

### Execução em Lote
Para rodar todos os testes sequencialmente e gerar relatórios automáticos:
```bash
# Para o Monolito
./run-all.sh mo

# Para os Microserviços
./run-all.sh ms
```

### Execução Individual
Para rodar um script específico utilizando o helper interativo:
```bash
./run-individual.sh [mo|ms]
```

Ou diretamente via k6:
```bash
k6 run -e prefix=mo <subpasta>/<arquivo>.js
```

---

## 👥 O que são VUS (Virtual Users)?

No contexto do k6, **VUs (Virtual Users)** são entidades que executam o script de teste de forma paralela e independente. 

- **Simulação Realista:** Cada VU representa um usuário único interagindo com o sistema simultaneamente.
- **Concorrência:** Diferente de uma execução sequencial, as VUs permitem testar como o servidor gerencia múltiplas requisições ao mesmo tempo.
- **Configuração:** A maioria dos testes está configurada para **50 VUs**, com estágios de ramp-up (subida gradual) e estabilidade.

---

## 📂 Estrutura de Testes

### 1. Gestão de Usuários (`user_management/`)
- **`authenticate.js`**: Login e validação de tokens JWT.
- **`create_user.js`**: Criação de novos clientes.
- **`create_admin.js`**: Criação de contas administrativas.
- **`update_user.js`**: Atualização de dados cadastrais.

### 2. Carrinho de Compras (`shopping_cart/`)
- **`get_cart.js`**: Recupera o estado atual do carrinho.
- **`add_product.js`**: Adiciona produtos (Patch).
- **`decrease_quantity.js`**: Reduz quantidade de itens (Patch).
- **`remove_product.js`**: Remove itens do carrinho (Patch).

### 3. Pedidos e Backoffice (`orders/`)
- **`checkout.js`**: Finalização de compra.
- **`list_orders.js`**: Listagem de pedidos do usuário logado.
- **`backoffice_list.js`**: Consulta administrativa de pedidos.
- **`update_status.js`**: Atualização de status de entrega (Random status 1-4).

### 4. Fluxo Completo (`scenarios/`)
- **`sale-checkout.js`**: Cenário end-to-end (Setup de usuário ➡️ Get Cart ➡️ Add Product ➡️ Checkout).
    *   **Configurável:** Aceita `VUS` e `DURATION` via `-e`.
    *   **Exemplo:** `k6 run -e prefix=ms -e VUS=100 -e DURATION=30s scenarios/sale-checkout.js`

### 5. Utilitários e Testes de Conexão (`testes/`)
- **`testAuthToken.js`**: Script simples para validar se a autenticação e obtenção de token estão funcionando para o prefixo selecionado.

---

## 🛠️ Configurações Comuns (`common/`)
- **`constants.js`**: Define as URLs base (MO: 8080, MS: 9000) e credenciais padrão.
- **`functions.js`**: Funções auxiliares, como `getAuthToken()`, que automatiza a autenticação baseada no `prefix`.

---

## 📊 Relatórios e Saída
Os resultados das execuções são exportados em formato JSON para a pasta:
`output/`

Os arquivos seguem o padrão: `<timestamp>_<prefix>_<nome_do_teste>.json`.
