// Sign-in and the menu: every virtual user signs in once (the morning peak spread over the ramp),
// then opens screens: profile, menu inputs, home widgets and counters, as the web client does on
// each navigation. SIGNIN_EVERY=n signs in again every n iterations.
import { sleep } from 'k6';
import { THRESHOLDS, get, signIn, userFor } from './lib/common.js';

export const options = {
  scenarios: {
    signin: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.RAMP || '1m', target: Number(__ENV.VUS || 150) },
        { duration: __ENV.HOLD || '3m', target: Number(__ENV.VUS || 150) },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: THRESHOLDS,
};

const EVERY = Number(__ENV.SIGNIN_EVERY || 0);
let token = null;
let alerts = false;

export function menu(t) {
  const me = get(t, '/auth/me', 'GET /auth/me');
  alerts = (me.json('permissions') || []).includes('ALERT_VIEW');
  get(t, '/auth/password-status', 'GET /auth/password-status');
  get(t, '/system/session-policy', 'GET /system/session-policy');
  get(t, '/role-homes/mine', 'GET /role-homes/mine');
  get(t, '/notifications/unread-count', 'GET /notifications/unread-count');
  get(t, '/approvals/counts', 'GET /approvals/counts');
  if (alerts) {
    get(t, '/alerts/summary', 'GET /alerts/summary');
  }
}

export default function () {
  if (token === null || (EVERY > 0 && __ITER % EVERY === 0)) {
    token = signIn(userFor(__VU));
  }
  menu(token);
  sleep(Number(__ENV.THINK || 5));
}
