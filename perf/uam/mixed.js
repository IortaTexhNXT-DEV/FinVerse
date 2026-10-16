// 150 concurrent users at once: most work in the menu, User Access staff on Users and Group
// Profiles, and UIDM-ISC sending membership changes at the same time.
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { BASE, THRESHOLDS, get, signIn, userFor } from './lib/common.js';
import { menu } from './signin_menu.js';

const DURATION = __ENV.DURATION || '3m';

export const options = {
  scenarios: {
    menu: {
      executor: 'ramping-vus',
      stages: [
        { duration: __ENV.RAMP || '1m', target: 120 },
        { duration: DURATION, target: 120 },
      ],
      exec: 'browse',
    },
    users: { executor: 'constant-vus', vus: 15, duration: DURATION, exec: 'users' },
    profiles: { executor: 'constant-vus', vus: 15, duration: DURATION, exec: 'profiles' },
    scim: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.SCIM_RATE || 10),
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 10,
      exec: 'membership',
    },
  },
  thresholds: THRESHOLDS,
};

let token = null;

export function setup() {
  // One session each for the User Access staff (one person signs in once, not once per tab).
  return { admin: signIn('uamadmin'), checker: signIn('uamapprover') };
}

export function browse() {
  if (token === null) {
    token = signIn(userFor(__VU));
  }
  menu(token);
  sleep(5);
}

export function users(data) {
  const t = data.admin;
  const list = get(t, '/admin/users', 'GET /admin/users').json() || [];
  if (list.length > 0) {
    const u = list[Math.floor(Math.random() * list.length)];
    get(t, `/admin/users/${u.id}/group-profiles`, 'GET /admin/users/{id}/group-profiles');
    get(t, `/admin/users/${u.id}/access-history`, 'GET /admin/users/{id}/access-history');
  }
  sleep(5);
}

export function profiles(data) {
  const t = data.checker;
  get(t, '/nbadmin/roles', 'GET /nbadmin/roles');
  get(t, '/nbadmin/users', 'GET /nbadmin/users');
  get(t, '/nbadmin/access-matrix', 'GET /nbadmin/access-matrix');
  sleep(5);
}

export function membership() {
  const i = exec.scenario.iterationInTest;
  const member = `p${String(900000001 + (i % 600))}`;
  const group = `PERF_GP_${String(((i * 13) % 60) + 1).padStart(2, '0')}`;
  const op =
    i % 2 === 0
      ? { op: 'add', path: 'members', value: [{ value: member }] }
      : { op: 'remove', path: `members[value eq "${member}"]` };
  const res = http.patch(
    `${BASE}/integration/v1/scim/v2/Groups/${group}`,
    JSON.stringify({ schemas: ['urn:ietf:params:scim:api:messages:2.0:PatchOp'], Operations: [op] }),
    {
      headers: {
        Authorization: `Bearer ${__ENV.SCIM_TOKEN}`,
        'Content-Type': 'application/scim+json',
      },
      tags: { kind: 'scim', name: 'PATCH /Groups/{id}' },
    },
  );
  check(res, { 'membership changed': (r) => r.status === 200 });
}
