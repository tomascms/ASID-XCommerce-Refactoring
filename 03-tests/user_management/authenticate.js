import http from 'k6/http';
import { check, sleep } from 'k6';
import { MO_BASE_URL, MS_BASE_URL, defaultMsAdminUserAuth, defaultMoAdminUserAuth } from '../common/constants.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 }, // ramp-up to 50 users over 30s
    { duration: '1m', target: 50 },  // stay at 50 users for 1m
    { duration: '30s', target: 0 },  // ramp-down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],   // less than 1% of requests should fail
  },
};

export default function () {
  const prefix = __ENV.prefix;

  if (!prefix || (prefix !== 'ms' && prefix !== 'mo')) {
    console.error(`[ERROR] A variável de ambiente 'prefix' é obrigatória e deve ser 'mo' ou 'ms'. Valor recebido: ${prefix}`);
    return null;
  }

  const authData = prefix === 'ms' ? defaultMsAdminUserAuth : defaultMoAdminUserAuth;
  const BASE_URL = prefix === 'ms' ? MS_BASE_URL : MO_BASE_URL;

  const url = `${BASE_URL}/rest/user/authenticate`;

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, authData, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'body is not empty': (r) => r.body.length > 0,
  });

  sleep(1);
}