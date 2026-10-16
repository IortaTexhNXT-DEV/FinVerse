// Reports & Certification: the certification extract (CSV and XLSX) and its log.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, THRESHOLDS, bearer, get, signIn } from './lib/common.js';

const VIEWER = __ENV.VIEWER || 'uamadmin';

export const options = {
  scenarios: {
    extract: {
      executor: 'shared-iterations',
      vus: Number(__ENV.VUS || 5),
      iterations: Number(__ENV.ITERATIONS || 20),
    },
  },
  thresholds: THRESHOLDS,
};

export function setup() {
  return { token: signIn(VIEWER) };
}

export default function (data) {
  const format = __ITER % 2 === 0 ? 'CSV' : 'XLSX';
  const params = bearer(data.token, `POST /nbadmin/certification/extract ${format}`, 'extract');
  params.headers['Content-Type'] = 'application/json';
  params.responseType = 'binary';
  const res = http.post(`${API}/nbadmin/certification/extract`, JSON.stringify({ format }), params);
  check(res, { 'extract taken': (r) => r.status === 200 && r.body.byteLength > 0 });
  get(data.token, '/nbadmin/certification/extracts', 'GET /nbadmin/certification/extracts');
  sleep(1);
}
