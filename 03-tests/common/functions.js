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

  const res = http.post(loginUrl, payload, params); // Pass the stringified payload

  if (res.status === 200) {
    try {
      return res.body;
    } catch (e) {
      console.error(`[ERROR] Failed to parse authentication response body: ${e.message}`);
      console.error(`[ERROR] Response body was: ${res.body}`);
      return null;
    }
  } else {
      console.error(`Authentication failed! Status: ${res.status}`);
      console.error(`Response Body: ${res.body}`);
      console.error(`Response Headers: ${JSON.stringify(res.headers)}`);
  }
  return null;
}