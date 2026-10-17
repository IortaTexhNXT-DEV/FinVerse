// Group Profiles screen: the list (profiles and members), then the detail of one profile
// (definition, permissions by area and action, members, pending changes).
import { sleep } from 'k6';
import { THRESHOLDS, get, signIn } from './lib/common.js';

const VIEWER = __ENV.VIEWER || 'uamapprover';

export const options = {
  scenarios: {
    groupProfiles: {
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
  const roles = get(data.token, '/nbadmin/roles', 'GET /nbadmin/roles').json() || [];
  get(data.token, '/nbadmin/users', 'GET /nbadmin/users');
  get(
    data.token,
    '/nbadmin/access-requests?scope=ALL&groupProfiles=true&status=PENDING',
    'GET /nbadmin/access-requests (pending)',
  );
  if (roles.length > 0) {
    const role = roles[Math.floor(Math.random() * roles.length)];
    get(data.token, '/nbadmin/access-matrix', 'GET /nbadmin/access-matrix');
    get(
      data.token,
      `/nbadmin/access-requests?scope=ALL&groupProfiles=true&text=${encodeURIComponent(role.code)}`,
      'GET /nbadmin/access-requests (profile)',
    );
  }
  sleep(Number(__ENV.THINK || 3));
}
