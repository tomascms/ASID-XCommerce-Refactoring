// T1 — Monolito: GET /rest/shoppingCart/get
// Baseline de leitura leve: 1 hop (controller → Redis → resposta)
// Nota: o monolito não expõe GET /products publicamente.
//       O carrinho é o endpoint de leitura autenticada mais simples disponível.
// QI2: serve de baseline para comparar com T3 (checkout, 1 transação JPA)
// Carga: 10 VUs fixos, 3 min

import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders, BASE } from './lib-auth.js';

const VUS  = parseInt(__ENV.VUS  || '10');
const SEED_USER = __ENV.SEED_USER || 'user1';
const SEED_PASS = __ENV.SEED_PASS || 'password';

export const options = {
  scenarios: {
    catalogo: {
      executor: 'constant-vus',
      vus: VUS,
      duration: '3m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate<0.01'],
  },
};

export function setup() {
  const token = login(SEED_USER, SEED_PASS);
  return { token };
}

export default function ({ token }) {
  const res = http.get(`${BASE}/rest/shoppingCart/get`, authHeaders(token));
  check(res, {
    'T1 status 200': r => r.status === 200,
    'T1 body não vazio': r => r.body && r.body.length > 1,
  });
  sleep(0.1);
}
