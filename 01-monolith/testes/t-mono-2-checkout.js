import http from 'k6/http';
import { sleep, check } from 'k6';

// T-MONO-2: Latência do checkout — operação transacional crítica
// Monólito em :18080
// O checkout executa numa única transação JPA:
//   1. Lê utilizador (userId hardcoded = 1)
//   2. Lê carrinho do Redis
//   3. Itera produtos → cria OrderLines
//   4. Guarda Order na BD
//   5. Esvazia carrinho no Redis
//
// Nota: 1 VU obrigatório — o monólito usa userId=1 hardcoded para todos os requests.
// Com múltiplos VUs, as corridas no mesmo carrinho Redis causam falhas artificiais.
//
// Uso:
//   k6 run --out csv=resultados/t-mono-2-exec1.csv -e TOKEN=<jwt> t-mono-2-checkout.js

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

export default function () {
  // Adiciona produto ao carrinho antes de cada checkout
  // (o checkout devolve sem fazer nada se o carrinho estiver vazio)
  http.patch(`${BASE_URL}/rest/shoppingCart/addProduct`,
    JSON.stringify({ id: 0, quantity: 1 }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  // Checkout — a operação que medimos
  const res = http.get(`${BASE_URL}/rest/order/checkout`);

  check(res, {
    'checkout ok': (r) => r.status === 200,
  });
}
