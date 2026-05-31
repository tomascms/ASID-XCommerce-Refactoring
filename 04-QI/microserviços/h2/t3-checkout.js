// T3 — Microserviços: POST /rest/order/checkout via gateway → order-service
// Fluxo transacional coordenado: 5 hops síncronos
// cart(read) → catalog(price×item) → cart(clear) → order(persist) → Kafka(publish)
// QI2: caso complexo — comparar rácio p95(micro)/p95(mono) entre T1 e T3
// Carga: 10 VUs fixos, 3 min
// PRÉ-CONDIÇÃO: cada VU precisa de carrinho com 2 itens — reposto no setup() e em cada iteração

import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE } from './lib-auth.js';

const VUS        = parseInt(__ENV.VUS        || '10');
const SEED_PASS  = __ENV.SEED_PASS           || 'password';
// IDs dos produtos no catalog-service — obtidos após seed-microservicos.sh
// Sobrepor via: -e PRODUCT_ID1=X -e PRODUCT_ID2=Y
const PRODUCT_IDS = [
  parseInt(__ENV.PRODUCT_ID1 || '1'),
  parseInt(__ENV.PRODUCT_ID2 || '2'),
];

export const options = {
  scenarios: {
    checkout: {
      executor: 'constant-vus',
      vus: VUS,
      duration: '3m',
      gracefulStop: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed:   ['rate<0.05'],
  },
};

export function setup() {
  const tokens = [];
  for (let i = 1; i <= VUS; i++) {
    const username = `user${i}`;
    const token = login(username, SEED_PASS);
    if (!token) continue;

    const hdrs = authHeaders(token);
    // Limpar e repor carrinho com 2 itens
    http.del(`${BASE}/rest/shoppingCart`, hdrs);
    http.post(`${BASE}/rest/shoppingCart`,
      JSON.stringify({ productId: PRODUCT_IDS[0], quantity: 2 }), hdrs);
    http.post(`${BASE}/rest/shoppingCart`,
      JSON.stringify({ productId: PRODUCT_IDS[1], quantity: 1 }), hdrs);

    tokens.push({ username, token });
  }
  return { tokens };
}

export default function ({ tokens }) {
  const idx   = __VU - 1;
  const entry = tokens[idx % tokens.length];
  if (!entry) return;

  const hdrs = authHeaders(entry.token);

  // Repor carrinho a cada iteração (checkout esvazia-o)
  http.post(`${BASE}/rest/shoppingCart`,
    JSON.stringify({ productId: PRODUCT_IDS[0], quantity: 2 }), hdrs);
  http.post(`${BASE}/rest/shoppingCart`,
    JSON.stringify({ productId: PRODUCT_IDS[1], quantity: 1 }), hdrs);

  // Checkout — esta é a chamada que medimos
  const res = http.post(`${BASE}/rest/order/checkout`, null, hdrs);
  check(res, {
    'T3 checkout 2xx': r => r.status >= 200 && r.status < 300,
  });
  sleep(0.5);
}
