import http from 'k6/http';
import { sleep, check } from 'k6';

// T-MONO-1: Latência de leitura simples — GET /rest/catalog/products
// Monólito em :18088 — 1 query directa à BD, sem lógica de negócio
//
// Uso:
//   k6 run --out csv=resultados/t-mono-1-exec1.csv -e TOKEN=<jwt> t-mono-1-leitura.js

const BASE_URL = 'http://localhost:18080';
const TOKEN    = __ENV.TOKEN || '';

export let options = {
  stages: [
    { duration: '30s', target: 10 },  // warm-up — estabilizar JVM e cache Hibernate
    { duration: '60s', target: 10 },  // medição real
    { duration: '10s', target: 0  },  // cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.05'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/rest/catalog/products`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'tem produtos': (r) => r.body && r.body.length > 10,
  });
}
