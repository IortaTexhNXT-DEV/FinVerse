import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const css = (name: string) => readFileSync(`${process.cwd()}/src/styles/${name}`, 'utf8');

/** The first rule body of a selector at the top level of a stylesheet. */
function rule(text: string, selector: string): string {
  const at = text.indexOf(`\n${selector} {`);
  expect(at, `${selector} rule`).toBeGreaterThanOrEqual(0);
  return text.slice(at, text.indexOf('}', at));
}

describe('application shell layout', () => {
  it('keeps the shell to the window so the window never scrolls', () => {
    const shell = rule(css('components.css'), '.app-shell');
    expect(shell).toMatch(/height: 100dvh/);
    expect(shell).toMatch(/overflow: hidden/);
  });

  it('makes the content area the one scroll area and the containing block of placed content', () => {
    const main = rule(css('components.css'), '.app-main');
    expect(main).toMatch(/position: relative/);
    expect(main).toMatch(/overflow-y: auto/);
    expect(main).toMatch(/min-height: 0/);
  });

  it('never lets a tab strip scroll vertically', () => {
    const text = css('patterns.css');
    const tabs = text.slice(text.indexOf('/* ---------- Tab strips'));
    expect(rule(tabs, '.tabs')).toMatch(/overflow-y: hidden/);
  });
});
