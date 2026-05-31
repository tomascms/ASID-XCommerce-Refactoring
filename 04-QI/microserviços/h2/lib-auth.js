import http from 'k6/http';
import { check } from 'k6';

// Microserviços: tudo via gateway na porta 9000
const BASE = 'http://localhost:9000';

export function login(username, password) {
  const res = http.post(`${BASE}/rest/user/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'login 200': r => r.status === 200 });
  // O identity-service devolve { token: "..." }
  return JSON.parse(res.body).token;
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
