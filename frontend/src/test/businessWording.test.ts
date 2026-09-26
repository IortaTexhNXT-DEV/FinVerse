/**
 * Business-user wording (client feedback, 26-Sep-2026): screens show no requirement or
 * traceability references (BRD requirement ids, annex item numbers, open-question numbers) and no
 * design notes. The references stay in code comments and in hidden `requirementRefs` metadata;
 * this check reads everything else in `frontend/src` except the tests.
 */
const FORBIDDEN: readonly [string, RegExp][] = [
  ['requirement id', /\b[A-Z]{2,6}ID\.\d/],
  ['BRD rule id', /\bBR[A-Z]{2,4}\.\d/],
  ['FRBS section', /\bFRBS \d+\.\d/],
  ['annex reference', /\bAnnex II\b/],
  ['open question', /\b(?:OQ\d+|[A-Z]{1,3}Q\d{2}|Q\d{2})\b/],
  ['functional requirement', /\bFR-[A-Z]{2}-?\d/],
  ['screening requirement', /\bSNSRP-\d/],
  [
    'design note',
    /\blayout to confirm\b|\bparked\b|\bto be confirmed by BDOI\b|\bto confirm with BDOI\b/i,
  ],
];

/** Lines that hold traceability metadata, never shown. */
const METADATA = /\brequirementRefs\b/;

/** The wording filter itself names the patterns it removes. */
const FILTERS = new Set(['../utils/businessText.ts']);

const sources = import.meta.glob<string>('../**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
});

/** A line without its line comment (a "//" not inside a URL such as https://). */
function withoutLineComment(line: string): string {
  let at = line.indexOf('//');
  while (at >= 0) {
    if (at === 0 || line[at - 1] !== ':') {
      return line.slice(0, at);
    }
    at = line.indexOf('//', at + 2);
  }
  return line;
}

/** The source without comments: block and JSX comments, then line comments. */
function withoutComments(source: string): string {
  const blocks: string[] = [];
  let rest = source;
  let start = rest.indexOf('/*');
  while (start >= 0) {
    const end = rest.indexOf('*/', start + 2);
    const stop = end < 0 ? rest.length : end + 2;
    blocks.push(rest.slice(0, start), rest.slice(start, stop).replace(/[^\n]/g, ' '));
    rest = rest.slice(stop);
    start = rest.indexOf('/*');
  }
  blocks.push(rest);
  return blocks.join('').split('\n').map(withoutLineComment).join('\n');
}

describe('business-user wording', () => {
  it('keeps requirement references and design notes off the screens', () => {
    const findings: string[] = [];
    const files = Object.entries(sources).filter(
      ([path]) => !/\.test\.tsx?$/.test(path) && !FILTERS.has(path),
    );
    expect(files.length).toBeGreaterThan(100);
    for (const [path, source] of files) {
      withoutComments(source)
        .split('\n')
        .forEach((line, i) => {
          if (METADATA.test(line)) {
            return;
          }
          for (const [kind, pattern] of FORBIDDEN) {
            const match = pattern.exec(line);
            if (match !== null) {
              findings.push(`${path}:${String(i + 1)} ${kind} "${match[0]}"`);
            }
          }
        });
    }
    expect(findings).toEqual([]);
  });

  it('ignores references in comments', () => {
    const code = "// ADJID.021\n/* BRNB.102 */\nconst label = 'Aging';";
    expect(withoutComments(code)).not.toMatch(/ADJID|BRNB/);
    expect(withoutComments("const url = 'https://host/x';")).toContain('https://host/x');
  });
});
