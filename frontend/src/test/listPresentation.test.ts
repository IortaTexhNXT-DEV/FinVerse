/// <reference types="node" />
/**
 * List presentation across the screens (product owner review of My Work, 02-Oct-2026): lists show
 * names and labels, never raw codes; the record type is never a loose word beside a status pill;
 * ages read in days; status pills are the one shared pill; and no list scrolls inside a box of its
 * own. This check reads every screen and shared component of `frontend/src` except the tests.
 */
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const sources = import.meta.glob<string>(['../features/**/*.tsx', '../components/**/*.tsx'], {
  query: '?raw',
  import: 'default',
  eager: true,
});

/** The style sheets, read from disk (styles are not loaded in the test runner). */
const STYLES = join(process.cwd(), 'src', 'styles');
const styles: Record<string, string> = Object.fromEntries(
  readdirSync(STYLES)
    .filter((name) => name.endsWith('.css'))
    .map((name) => [name, readFileSync(join(STYLES, name), 'utf8')]),
);

const screens = Object.entries(sources).filter(([path]) => !path.endsWith('.test.tsx'));

/** Every line of the screens that matches, as "file:line: text". */
function findings(pattern: RegExp): string[] {
  return screens.flatMap(([path, source]) =>
    source
      .split('\n')
      .map((line, i) => ({ line, i }))
      .filter(({ line }) => pattern.test(line))
      .map(({ line, i }) => `${path}:${String(i + 1)}: ${line.trim()}`),
  );
}

describe('list presentation', () => {
  it('shows insurers, products, lines, cover types and segments by name, never by code', () => {
    const rawCode =
      /render: \((\w+)(: \w+)?\) => \1\.(insurerCode|productCode|businessLine|lineCode|coverType|marketSegment|segment|salesUnit|productLine|benefitLine)\b( \?\? '[^']*')?( \}|,|$)/;
    expect(findings(rawCode)).toEqual([]);
  });

  it('shows statuses, stages, outcomes and methods by label, never as the raw code', () => {
    const rawStatus =
      /render: \((\w+)(: \w+)?\) => \1\.(status|stage|outcome|channel|method|eventType|component|action|entityType|disposition)\b( \?\? '[^']*')?( \}|,|$)/;
    expect(findings(rawStatus)).toEqual([]);
  });

  it('never puts the record type as a loose word beside a status pill', () => {
    const joined = screens.map(([path, source]) => [path, source.replace(/\s+/g, ' ')] as const);
    const loose = /<StatusBadge [^>]*\/> <\/span> <span className="muted">\{WORKFLOW_NAMES/;
    expect(joined.filter(([, source]) => loose.test(source)).map(([path]) => path)).toEqual([]);
  });

  it('reads ages and durations in words, never "22 d" or "3h"', () => {
    expect(findings(/\$\{[^}]+\} ?[dhm]`/)).toEqual([]);
  });

  it('shows the muted dash in an empty name or label cell, never a blank', () => {
    expect(findings(/render: .*empty=""/)).toEqual([]);
  });

  it('uses the one status pill: no pill without a tone and no raw code inside a pill', () => {
    expect(findings(/<span className="badge">\{\w+\.\w+\}<\/span>/)).toEqual([]);
  });

  it('lets lists use the page scroll: no table scroll box of a fixed height', () => {
    const boxed = Object.entries(styles).filter(([, css]) =>
      /\.table-wrap[^{]*\{[^}]*max-height:\s*(?!\s|none)/.test(css),
    );
    expect(boxed.map(([path]) => path)).toEqual([]);
    const reports = Object.values(styles).join('\n');
    expect(reports).not.toMatch(/\.report-result \{[^}]*max-height/);
  });

  it('keeps neutral zebra rows and a hover distinct from the selected row', () => {
    const css = Object.values(styles).join('\n');
    expect(css).toMatch(
      /\.table tbody tr:nth-child\(even\) \{\s*background: var\(--table-row-alt\);/,
    );
    expect(css).toMatch(/\.table tbody tr:hover \{\s*background: var\(--table-row-hover\);/);
    expect(css).toMatch(/--table-row-selected: var\(--brand-blue-050\);/);
  });
});
