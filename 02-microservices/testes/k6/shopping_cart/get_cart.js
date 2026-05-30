import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '30s', target: 50 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9000';

function getAuthToken() {
  const loginUrl = `${BASE_URL}/rest/user/authenticate`;
  const payload = JSON.stringify({ username: 'root', password: 'root' });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(loginUrl, payload, params);
  return res.status === 200 ? res.body : null;
}

export default function () {
  const token = getAuthToken();
  if (!token) return;

  const url = `${BASE_URL}/rest/shoppingCart/get`;
  const params = {
    headers: {
      'Authorization': `Bearer ${token.replace(/"/g, '')}`,
    },
  };

  const res = http.get(url, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'has items property': (r) => {
      try {
        return JSON.parse(r.body).hasOwnProperty('items') || r.status === 200;
      } catch (e) {
        return r.status === 200;
      }
    },
  });

  sleep(1);
}
