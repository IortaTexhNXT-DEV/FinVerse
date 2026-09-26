/**
 * Standing check of the delivery wording (client content rules): no file under src uses the retired
 * word for seed data or names a restricted tool or vendor. The names are stored in ROT13, as in
 * docs/deliverables/src/testplans/build_test_plan.py, so they are not spelled out in this file.
 */
const rot13 = (text: string): string =>
  text.replace(/[a-z]/gi, (c) => {
    const base = c <= 'Z' ? 65 : 97;
    return String.fromCharCode(((c.charCodeAt(0) - base + 13) % 26) + base);
  });

const NAMES = ['pynhqr', 'naguebcvp', 'bcranv', 'pungtcg', 'trzvav', 'pbcvybg', 'yynzn', 'tvguho'].map(rot13);
const RESTRICTED = new RegExp(
  `${rot13('qrzb')}(?!nstrat|li)|\\b(?:${NAMES.join('|')}|${rot13('tcg')}-?\\d)\\b`,
  'i',
);

const sources = import.meta.glob<string>('../**/*.{ts,tsx,js,json,css,html,md}', {
  query: '?raw',
  import: 'default',
  eager: true,
});

describe('delivery wording', () => {
  it('scans the source tree', () => {
    expect(Object.keys(sources).length).toBeGreaterThan(100);
  });

  it('uses seed data wording and names no restricted tool or vendor', () => {
    const hits = Object.entries(sources).flatMap(([file, text]) =>
      text
        .split('\n')
        .map((line, i) => ({ file, line: i + 1, text: line }))
        .filter((l) => RESTRICTED.test(l.text))
        .map((l) => `${l.file}:${l.line}: ${l.text.trim()}`),
    );
    expect(hits).toEqual([]);
  });
});
