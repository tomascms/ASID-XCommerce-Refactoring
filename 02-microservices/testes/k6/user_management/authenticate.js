import http from 'k6/http';
import { check, sleep } from 'k6';

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

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9000';

export default function () {
  const url = `${BASE_URL}/rest/user/authenticate`;
  const payload = JSON.stringify({
    username: 'root',
    password: 'root',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'body is not empty': (r) => r.body.length > 0,
  });

  sleep(1);
}
