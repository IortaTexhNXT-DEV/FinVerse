import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * Security headers of the web server configurations. nginx drops the server-level headers in a
 * location that sets its own, so the SPA location must repeat every one of them.
 */
const CONFIGS = ['nginx.conf', '../deploy/nginx/default.conf.template'];

const HEADERS = [
  'Strict-Transport-Security "max-age=31536000; includeSubDomains"',
  'X-Content-Type-Options nosniff',
  'X-Frame-Options DENY',
  'Referrer-Policy no-referrer',
  'Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=(), usb=()"',
];

const CSP = [
  "default-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
];

function location(config: string, path: string): string {
  const start = config.indexOf(`location ${path} {`);
  expect(start, `location ${path}`).toBeGreaterThan(-1);
  return config.slice(start, config.indexOf('\n  }', start));
}

describe.each(CONFIGS)('security headers of %s', (file) => {
  const config = readFileSync(join(process.cwd(), file), 'utf8');
  const server = config.slice(0, config.indexOf('\n  location '));
  const spa = location(config, '/');

  it.each([
    ['server', server],
    ['SPA location', spa],
  ])('sets every header in the %s', (_where, block) => {
    for (const header of HEADERS) {
      expect(block).toContain(`add_header ${header} always;`);
    }
    const csp = /Content-Security-Policy "([^"]*)"/.exec(block)?.[1] ?? '';
    for (const directive of CSP) {
      expect(csp).toContain(directive);
    }
    expect(csp).not.toContain('unsafe-eval');
  });

  it('hides the server version', () => {
    expect(config).toContain('server_tokens off;');
  });
});
