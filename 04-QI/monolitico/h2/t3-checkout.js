// T3 — Monolito: GET /rest/order/checkout
// Fluxo transacional completo: 1 transação JPA local
// (lê user + carrinho Redis + produtos, cria encomenda, limpa carrinho — tudo no mesmo processo)
// QI2: caso complexo — comparar p95 com T1 (baseline) e com T3 dos microserviços (5 hops)
// Carga: 10 VUs fixos, 3 min
// PRÉ-CONDIÇÃO: cada VU precisa de carrinho com 2 itens — reposto no setup() e em cada iteração

import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE } from './lib-auth.js';

const VUS        = parseInt(__ENV.VUS  || '10');
const SEED_PASS  = __ENV.SEED_PASS  || 'password';
// IDs gerados pelo seed-monolito.sh: Produto Teste 1=2601, Produto Teste 2=2602
// Sobrepor via: -e PRODUCT_ID1=X -e PRODUCT_ID2=Y
const PRODUCT_IDS = [
  parseInt(__ENV.PRODUCT_ID1 || '2601'),
  parseInt(__ENV.PRODUCT_ID2 || '2602'),
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
  // Para cada VU, faz login com userN e repõe carrinho antes de medir
  const tokens = [];
  for (let i = 1; i <= VUS; i++) {
    const username = `user${i}`;
    const token = login(username, SEED_PASS);
    if (!token) continue;

    const hdrs = authHeaders(token);
    // Limpar carrinho existente e adicionar 2 itens frescos
    http.patch(`${BASE}/rest/shoppingCart/removeProduct`,
      JSON.stringify({ id: PRODUCT_IDS[0], quantity: 999 }), hdrs);
    http.patch(`${BASE}/rest/shoppingCart/removeProduct`,
      JSON.stringify({ id: PRODUCT_IDS[1], quantity: 999 }), hdrs);
    http.patch(`${BASE}/rest/shoppingCart/addProduct`,
      JSON.stringify({ id: PRODUCT_IDS[0], quantity: 2 }), hdrs);
    http.patch(`${BASE}/rest/shoppingCart/addProduct`,
      JSON.stringify({ id: PRODUCT_IDS[1], quantity: 1 }), hdrs);

    tokens.push({ username, token });
  }
  return { tokens };
}

export default function ({ tokens }) {
  // Cada VU usa o seu próprio utilizador
  const idx   = __VU - 1;
  const entry = tokens[idx % tokens.length];
  if (!entry) return;

  const hdrs = authHeaders(entry.token);

  // Repor carrinho a cada iteração (checkout esvazia-o)
  http.patch(`${BASE}/rest/shoppingCart/addProduct`,
    JSON.stringify({ id: PRODUCT_IDS[0], quantity: 2 }), hdrs);
  http.patch(`${BASE}/rest/shoppingCart/addProduct`,
    JSON.stringify({ id: PRODUCT_IDS[1], quantity: 1 }), hdrs);

  // Checkout — esta é a chamada que medimos
  const res = http.get(`${BASE}/rest/order/checkout`, hdrs);
  check(res, {
    'T3 checkout 200': r => r.status === 200,
  });
  sleep(0.5);
}
