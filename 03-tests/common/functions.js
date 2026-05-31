import http from 'k6/http';
import { MO_BASE_URL, MS_BASE_URL, defaultMsAdminUserAuth, defaultMoAdminUserAuth } from './constants.js';

export function getAuthToken() {
  const prefix = __ENV.prefix;

  if (!prefix || (prefix !== 'ms' && prefix !== 'mo')) {
    console.error(`[ERROR] A variável de ambiente 'prefix' é obrigatória e deve ser 'mo' ou 'ms'. Valor recebido: ${prefix}`);
    return null;
  }

  const authData = prefix === 'ms' ? defaultMsAdminUserAuth : defaultMoAdminUserAuth;
  const BASE_URL = prefix === 'ms' ? MS_BASE_URL : MO_BASE_URL;

  const loginUrl = `${BASE_URL}/rest/user/authenticate`;
  const params = { 
    headers: { 
      'Content-Type': 'application/json'
    } 
  };

  const payload = JSON.stringify(authData);

  const res = http.post(loginUrl, payload, params);

  if (res.status === 200) {
    try {
      const data = JSON.parse(res.body);
      if (data && typeof data === 'object' && data.token) {
        return data.token;
      }
    } catch (e) {
      // Not a JSON or doesn't match the expected structure, return as raw string
    }
    return res.body;
  } else {
      console.error(`Authentication failed! Status: ${res.status}`);
      console.error(`Response Body: ${res.body}`);
      console.error(`Response Headers: ${JSON.stringify(res.headers)}`);
  }
  return null;
}
