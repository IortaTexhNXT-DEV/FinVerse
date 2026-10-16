// Users screen: the list, then the detail of one user (group profiles, history, sessions,
// directory profile, data access).
import { sleep } from 'k6';
import { THRESHOLDS, get, signIn } from './lib/common.js';

const VIEWER = __ENV.VIEWER || 'uamadmin';

export const options = {
  scenarios: {
    users: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 30),
      duration: __ENV.DURATION || '3m',
    },
  },
  thresholds: THRESHOLDS,
};

export function setup() {
  return { token: signIn(VIEWER) };
}

export default function (data) {
  const list = get(data.token, '/admin/users', 'GET /admin/users');
  const users = list.json() || [];
  if (users.length > 0) {
    const u = users[Math.floor(Math.random() * users.length)];
    get(data.token, `/admin/users/${u.id}/group-profiles`, 'GET /admin/users/{id}/group-profiles');
    get(data.token, `/admin/users/${u.id}/access-history`, 'GET /admin/users/{id}/access-history');
    get(data.token, `/admin/users/${u.id}/data-scope`, 'GET /admin/users/{id}/data-scope');
    get(
      data.token,
      `/admin/sessions?username=${encodeURIComponent(u.username)}&page=0&size=20`,
      'GET /admin/sessions',
    );
    get(
      data.token,
      `/admin/identity/users/${encodeURIComponent(u.username)}/profile`,
      'GET /admin/identity/users/{u}/profile',
    );
  }
  sleep(Number(__ENV.THINK || 3));
}
