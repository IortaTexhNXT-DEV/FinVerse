import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

/** The KPI cards of the Renewal dashboards are a list of cards: no bullet and no indent before each card. */
describe('KPI card list style', () => {
  const css = readFileSync(resolve(__dirname, '../renewal.css'), 'utf8');
  const rule = /\.rnw-kpi-cards\s*\{([^}]*)\}/.exec(css)?.[1] ?? '';

  it('shows no bullet and no list indent', () => {
    expect(rule).toMatch(/list-style:\s*none/);
    expect(rule).toMatch(/padding:\s*0/);
  });
});
