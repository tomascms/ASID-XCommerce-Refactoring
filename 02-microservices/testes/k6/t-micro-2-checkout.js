import http from 'k6/http';
import { sleep, check } from 'k6';

// T-MICRO-2: Latência checkout via Gateway → order-service → inventory/catalog/cart (REST) → Kafka
// Espelho de t-mono-2-checkout.js: 1 VU, 60s de medição.
// Baseline monólito: p50=7.1ms, p95=11.4ms (1 transação JPA local)
// Hipótese H2: p95 aqui ≥ 22.8ms (2× monólito) devido a 3+ chamadas REST síncronas em série
//
// Diferença chave face ao monólito:
//   - checkout usa X-User-Name do JWT (não userId=1 hardcoded) → cada VU tem identidade real
//   - addProduct usa PATCH /rest/shoppingCart/addProduct com campo "productId" (não "id")
//   - checkout é POST /rest/order/checkout (não GET)
//
// Uso:
//   TOKEN=$(curl -s -X POST http://localhost:9000/rest/user/login \
//     -H "Content-Type: application/json" \
//     -d '{"username":"admin","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
//   k6 run --out csv=resultados/t-micro-2-exec1.csv -e TOKEN=$TOKEN t-micro-2-checkout.js

const BASE_URL = 'http://localhost:9000';
const TOKEN    = __ENV.TOKEN || '';

export let options = {
  vus: 1,
  stages: [
    { duration: '30s', target: 1 },  // warm-up
    { duration: '60s', target: 1 },  // medição
    { duration: '10s', target: 0 },  // cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed:   ['rate<0.1'],
  },
};

const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${TOKEN}`,
};

export default function () {
  // Adiciona produto ao carrinho antes de cada checkout
  http.patch(
    `${BASE_URL}/rest/shoppingCart/addProduct`,
    JSON.stringify({ productId: 1, quantity: 1 }),
    { headers }
  );

  // Checkout — operação que medimos: 3 chamadas REST internas + Kafka publish
  const res = http.post(`${BASE_URL}/rest/order/checkout`, null, { headers });

  check(res, {
    'checkout ok': (r) => r.status === 200 || r.status === 201,
  });
}
