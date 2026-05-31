import http from 'k6/http';
import { check } from 'k6';

// Monolito: porta 18080
const BASE = 'http://localhost:18080';

export function login(username, password) {
  const res = http.post(`${BASE}/rest/user/authenticate`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'login 200': r => r.status === 200 });
  // O monolito devolve o token em texto plano
  return res.body;
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    }
  };
}

export { BASE };
