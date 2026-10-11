import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

/** Caching of the web server configurations: index.html revalidated, hashed files immutable. */
const CONFIGS = ['nginx.conf', '../deploy/nginx/default.conf.template'];

function location(config: string, path: string): string {
  const start = config.indexOf(`location ${path} {`);
  expect(start, `location ${path}`).toBeGreaterThan(-1);
  return config.slice(start, config.indexOf('\n  }', start));
}

describe.each(CONFIGS)('web server caching in %s', (file) => {
  const config = readFileSync(join(process.cwd(), file), 'utf8');

  it('revalidates index.html on every load', () => {
    expect(location(config, '/')).toContain(
      'add_header Cache-Control "no-cache, must-revalidate" always;',
    );
  });

  it('caches the hashed files for a year and never serves index.html for a missing one', () => {
    const assets = location(config, '/assets/');
    expect(assets).toContain('add_header Cache-Control "public, max-age=31536000, immutable";');
    expect(assets).toContain('try_files $uri =404;');
    expect(assets).not.toContain('expires');
  });
});
