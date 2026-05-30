import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '20s', target: 50 },
    { duration: '10s', target: 0 },
  ],
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
