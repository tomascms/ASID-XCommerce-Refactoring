import http from 'k6/http';
import { sleep, check } from 'k6';

// T-MICRO-1: Latência GET /products via Gateway → catalog-service → BD
// Endpoint público — sem token necessário.
// Espelho de t-mono-1-leitura.js: mesma carga (10 VUs, 60s) para comparação directa.
// Baseline monólito: p50=8.8ms, p95=16ms
//
// Uso:
//   k6 run --out csv=resultados/t-micro-1-exec1.csv t-micro-1-leitura.js

const BASE_URL = 'http://localhost:9000';

export let options = {
  stages: [
    { duration: '30s', target: 10 },  // warm-up
    { duration: '60s', target: 10 },  // medição
    { duration: '10s', target: 0  },  // cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate<0.05'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/products`);

  check(res, {
    'status 200': (r) => r.status === 200,
  });
}
