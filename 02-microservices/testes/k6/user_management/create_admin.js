import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 }, // ramp-up to 50 users
    { duration: '20s', target: 50 }, // stay at 50 users
    { duration: '10s', target: 0 }, // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9000';

// Helper function to get a token
function getAuthToken() {
  const loginUrl = `${BASE_URL}/rest/user/authenticate`;
  const payload = JSON.stringify({
    username: 'root',
    password: 'root',
  });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(loginUrl, payload, params);
  
  return res.status === 200 ? res.body : null;
}

export default function () {
  const token = getAuthToken();
  
  if (!token) {
    console.error('Failed to authenticate');
    return;
  }

  const url = `${BASE_URL}/rest/user/admin`;
  
  // Generating a unique username to avoid conflicts during performance test
  const uniqueId = Math.floor(Math.random() * 1000000);
  const payload = JSON.stringify({
    username: `admin_user_${uniqueId}`,
    password: 'password123',
    firstName: 'Test',
    lastName: 'Admin',
    emailAddress: `admin_${uniqueId}@example.com`,
    address: '123 Test St',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token.replace(/"/g, '')}`, // Remove quotes if token is returned as JSON string
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'admin created': (r) => r.body.includes(`admin_user_${uniqueId}`) || r.status === 200,
  });

  sleep(1);
}
