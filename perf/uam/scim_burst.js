// SCIM burst of UIDM-ISC: a full re-sync of the users (POST, or PUT when the user exists) and a
// series of group-profile membership changes (PATCH /Groups/{id}, one member per call).
//
//   SCIM_TOKEN=... k6 run scim_burst.js                 # 600 users, then 2,000 membership changes
//   SCIM_TOKEN=... MODE=groups k6 run scim_burst.js     # membership changes only
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { BASE, THRESHOLDS } from './lib/common.js';

const SCIM = `${BASE}/integration/v1/scim/v2`;
const TOKEN = __ENV.SCIM_TOKEN;
const USERS = Number(__ENV.SCIM_USERS || 600);
const CHANGES = Number(__ENV.SCIM_CHANGES || 2000);
const PROFILES = Number(__ENV.SCIM_PROFILES || 60);
const PREFIX = __ENV.SCIM_PREFIX || 'p';
const MODE = __ENV.MODE || 'both';
const VUS = Number(__ENV.VUS || 10);

const scenarios = {};
if (MODE !== 'groups') {
  scenarios.users = { executor: 'shared-iterations', vus: VUS, iterations: USERS, exec: 'user' };
}
if (MODE !== 'users') {
  scenarios.memberships = {
    executor: 'shared-iterations',
    vus: VUS,
    iterations: CHANGES,
    exec: 'membership',
    startTime: MODE === 'both' ? __ENV.GROUPS_START || '90s' : '0s',
  };
}

export const options = { scenarios, thresholds: THRESHOLDS };

const headers = {
  Authorization: `Bearer ${TOKEN}`,
  'Content-Type': 'application/scim+json',
};

function userId(n) {
  return `${PREFIX}${String(900000000 + n)}`;
}

function profile(n) {
  return `PERF_GP_${String((n % PROFILES) + 1).padStart(2, '0')}`;
}

function body(n) {
  const id = userId(n);
  return JSON.stringify({
    schemas: [
      'urn:ietf:params:scim:schemas:core:2.0:User',
      'urn:bibs:params:scim:schemas:extension:bdo:2.0:User',
    ],
    userName: id,
    externalId: `BDO\\${PREFIX}perf${n}`,
    displayName: `Perf User ${n}`,
    name: { givenName: 'Perf', familyName: `User ${n}` },
    emails: [{ value: `${id}@perf.example.ph`, primary: true }],
    active: true,
    groups: [{ value: profile(n) }, { value: profile(n + 7) }],
  });
}

export function user() {
  const index = (exec.scenario.iterationInTest % USERS) + 1;
  const id = userId(index);
  let res = http.post(`${SCIM}/Users`, body(index), {
    headers,
    tags: { kind: 'scim', name: 'POST /Users' },
    // 409: the user exists already, the re-sync sends it again with PUT.
    responseCallback: http.expectedStatuses(201, 409),
  });
  if (res.status === 409) {
    // Known user: the re-sync sends the full record again.
    res = http.put(`${SCIM}/Users/${id}`, body(index), {
      headers,
      tags: { kind: 'scim', name: 'PUT /Users/{id}' },
    });
  }
  check(res, { 'user synchronised': (r) => r.status === 200 || r.status === 201 });
}

export function membership() {
  const i = exec.scenario.iterationInTest;
  const member = userId((i % USERS) + 1);
  const op = i % 2 === 0 ? 'add' : 'remove';
  const group = profile(i * 13 + 3);
  const patch =
    op === 'add'
      ? { op: 'add', path: 'members', value: [{ value: member }] }
      : { op: 'remove', path: `members[value eq "${member}"]` };
  const res = http.patch(
    `${SCIM}/Groups/${group}`,
    JSON.stringify({ schemas: ['urn:ietf:params:scim:api:messages:2.0:PatchOp'], Operations: [patch] }),
    { headers, tags: { kind: 'scim', name: 'PATCH /Groups/{id}' } },
  );
  check(res, { 'membership changed': (r) => r.status === 200 || r.status === 204 });
}
