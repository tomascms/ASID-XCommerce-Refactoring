import http from 'k6/http';
import { check } from 'k6';

// T-MONO-2: Latência do checkout — operação transacional crítica
// Monólito em :18080
// O checkout executa:
//   1. Resolve userId a partir do JWT (SecurityContextHolder + UserRepository.findByUsername)
//   2. Lê utilizador
//   3. Lê carrinho do Redis
//   4. VALIDA stock para cada produto do carrinho (paridade com inventory-service)
//   5. Itera produtos → cria OrderLines
//   6. Guarda Order na BD
//   7. Esvazia carrinho no Redis
//
// Notas:
// - 1 VU obrigatório: cada VU usa o mesmo utilizador autenticado (root).
//   Múltiplos VUs em paralelo no mesmo carrinho causam corridas no Redis.
// - Sem `sleep()` — saturamos a arquitetura para medir overhead sob pressão.
// - Equivalência funcional ao t-micro-2-checkout.js: ambos validam stock,
//   ambos usam JWT real, ambos sem think time.
//
// Uso:
//   TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
//     -d '{"username":"root","password":"root"}' \
//     http://localhost:18080/rest/user/authenticate)
//   k6 run --out csv=resultados/t-mono-2-exec1.csv -e TOKEN=$TOKEN t-mono-2-checkout.js

const BASE_URL = 'http://localhost:18080';
const TOKEN    = __ENV.TOKEN || '';

export let options = {
  vus: 1,
  stages: [
    { duration: '30s', target: 1 },   // warm-up
    { duration: '60s', target: 1 },   // medição
    { duration: '10s', target: 0  },  // cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed:   ['rate<0.05'],
  },
};

const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${TOKEN}`,
};

export default function () {
  // Adiciona produto ao carrinho (se vazio o checkout não faz nada).
  // ID 1 corresponde ao produto "F1 Plus" (Spring Data REST gera IDs em sequência).
  http.patch(`${BASE_URL}/rest/shoppingCart/addProduct`,
    JSON.stringify({ id: 1, quantity: 1 }),
    { headers }
  );

  // Checkout — a operação que medimos.
  // Inclui: lookup do user via JWT, validação de stock, criação da Order, esvazia carrinho.
  const res = http.get(`${BASE_URL}/rest/order/checkout`, { headers });

  check(res, {
    'checkout ok': (r) => r.status === 200,
  });
}
