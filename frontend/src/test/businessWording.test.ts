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

/** The source without comments: block and JSX comments, then line comments. */
export function withoutComments(source: string): string {
  return source
    .replace(/\/\*[\s\S]*?\*\//g, (block) => block.replace(/[^\n]/g, ' '))
    .replace(/(^|[^:'"`\\])\/\/.*$/gm, '$1');
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
    expect(withoutComments("const url = 'http://host/x';")).toContain('http://host/x');
  });
});
