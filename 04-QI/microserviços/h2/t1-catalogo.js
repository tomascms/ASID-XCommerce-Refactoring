// T1 — Microserviços: GET /products via gateway → catalog-service
// Baseline de leitura leve: 1 hop efectivo (gateway → catalog-service → BD)
// QI2: serve de baseline para comparar com T3 (checkout, 5 hops síncronos)
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
  const res = http.get(`${BASE}/products`, authHeaders(token));
  check(res, {
    'T1 status 200': r => r.status === 200,
    'T1 body não vazio': r => r.body && r.body.length > 2,
  });
  sleep(0.1);
}
