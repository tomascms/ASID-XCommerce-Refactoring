import http from 'k6/http';
import { check, sleep } from 'k6';
import { getAuthToken } from '../common/functions.js';
import { MO_BASE_URL, MS_BASE_URL } from '../common/constants.js';

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '20s', target: 50 },
    { duration: '10s', target: 0 },
  ],
};

export default function () {
  const prefix = __ENV.prefix;

  if (!prefix || (prefix !== 'ms' && prefix !== 'mo')) {
    console.error(`[ERROR] A variável de ambiente 'prefix' é obrigatória e deve ser 'mo' ou 'ms'. Valor recebido: ${prefix}`);
    return null;
  }

  const BASE_URL = prefix === 'ms' ? MS_BASE_URL : MO_BASE_URL;

  const token = getAuthToken();

  if (!token) {
    console.error('Failed to authenticate');
    return;
  }

  const url = `${BASE_URL}/rest/shoppingCart/removeProduct`;
  const payload = JSON.stringify({
    id: 1, // Product ID
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token.replace(/"/g, '')}`,
    },
  };

  const res = http.patch(url, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  sleep(1);
}
