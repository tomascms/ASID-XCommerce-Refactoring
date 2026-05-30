import http from 'k6/http';
import { check, sleep } from 'k6';
import { getAuthToken } from '../common/functions.js';
import { MO_BASE_URL, MS_BASE_URL } from '../common/constants.js';

export const options = {
  vus: __ENV.VUS || 50,
  duration: __ENV.DURATION || '10s',
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  console.log('--- Iniciando Setup: Autenticando root e criando usuários ---');

  const prefix = __ENV.prefix;

  if (!prefix || (prefix !== 'ms' && prefix !== 'mo')) {
    console.error(`[ERROR] A variável de ambiente 'prefix' é obrigatória e deve ser 'mo' ou 'ms'. Valor recebido: ${prefix}`);
    return null;
  }

  const BASE_URL = prefix === 'ms' ? MS_BASE_URL : MO_BASE_URL;

  const rootToken = getAuthToken();

  if (!rootToken) {
    console.error('Failed to authenticate');
    return;
  }

  const numVus = parseInt(options.vus);
  const users = [];

  for (let i = 0; i < numVus; i++) {
    const uniqueId = Date.now() + i;
    const username = `testuser_${uniqueId}`;
    const password = 'Password123!';

    const createUserRes = http.post(`${BASE_URL}/rest/user`, JSON.stringify({
      username: username,
      password: password,
      firstName: 'Performance',
      lastName: 'Test',
      emailAddress: `${username}@example.com`,
      address: 'Test Street 123',
    }), {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${rootToken}`,
      },
    });

    if (createUserRes.status === 200) {
      const userLoginRes = http.post(`${BASE_URL}/rest/user/authenticate`, JSON.stringify({
        username: username,
        password: password,
      }), { headers: { 'Content-Type': 'application/json' } });

      if (userLoginRes.status === 200) {
        users.push({
          token: userLoginRes.body.replace(/"/g, ''),
        });
      }
    }
  }

  console.log(`--- Setup finalizado: ${users.length} usuários prontos ---`);
  return { users };
}

export default function (data) {
  const userIdx = __VU - 1;
  const user = data.users[userIdx];
  const prefix = __ENV.prefix;
  const BASE_URL = prefix === 'ms' ? MS_BASE_URL : MO_BASE_URL;
  
  if (!user) {
    console.error(`VU ${__VU} não encontrou usuário no array.`);
    return;
  }

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${user.token}`,
    },
  };

  const getCartRes = http.get(`${BASE_URL}/rest/shoppingCart/get`, params);
  check(getCartRes, { 'get cart status 200': (r) => r.status === 200 });

  const productId = Math.floor(Math.random() * 4);
  const addProductRes = http.patch(`${BASE_URL}/rest/shoppingCart/addProduct`, JSON.stringify({
    id: productId,
    quantity: 1,
  }), params);
  check(addProductRes, { 'add product status 200': (r) => r.status === 200 });

  const increaseRes = http.patch(`${BASE_URL}/rest/shoppingCart/addProduct`, JSON.stringify({
    id: productId,
    quantity: 1,
  }), params);
  check(increaseRes, { 'increase quantity status 200': (r) => r.status === 200 });

  const checkoutRes = http.get(`${BASE_URL}/rest/order/checkout`, params);
  check(checkoutRes, { 'checkout status 200': (r) => r.status === 200 });

  sleep(1);
}

export function teardown(data) {
  console.log('--- Iniciando Teardown ---');
  console.log(`Fluxo concluído para o pool de usuários.`);
}

export function handleSummary(data) {
  return {
    'stdout': JSON.stringify(data),
  };
}
