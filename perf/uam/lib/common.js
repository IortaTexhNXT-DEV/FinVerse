// Shared settings of the User Access load scripts (k6).
import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://127.0.0.1:18080';
export const API = `${BASE}/api/v1`;
export const PASSWORD = __ENV.PERF_PASSWORD || 'Perf#Local2026';

// Seed users of the SIT/UAT data set; each virtual user signs in as one of them in turn.
export const USERS = (
  __ENV.PERF_USERS ||
  'uamadmin,uamauditor,requestor,uamapprover,secapprover,infosec,admin,badmin,auditor,approver'
).split(',');

// Targets of the 530 named / 150 concurrent users sizing (perf/uam/README.md).
export const THRESHOLDS = {
  http_req_failed: ['rate<0.01'],
  'http_req_duration{kind:read}': ['p(95)<800', 'p(99)<2000'],
  'http_req_duration{kind:signin}': ['p(95)<1500'],
  'http_req_duration{kind:scim}': ['p(95)<1000'],
  'http_req_duration{kind:extract}': ['p(95)<5000'],
};

export function signIn(username) {
  const res = http.post(`${API}/auth/login`, JSON.stringify({ username, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { kind: 'signin', name: 'POST /auth/login' },
  });
  if (!check(res, { 'signed in': (r) => r.status === 200 })) {
    fail(`sign-in of ${username} answered ${res.status}`);
  }
  return res.json('accessToken');
}

export function bearer(token, name, kind = 'read') {
  return { headers: { Authorization: `Bearer ${token}` }, tags: { name, kind } };
}

export function get(token, path, name, kind = 'read') {
  const res = http.get(`${API}${path}`, bearer(token, name || `GET ${path}`, kind));
  check(res, { [`${name || path} 200`]: (r) => r.status === 200 || r.status === 204 });
  return res;
}

export function userFor(vu) {
  return USERS[(vu - 1) % USERS.length];
}
