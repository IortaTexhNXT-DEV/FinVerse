/**
 * Every screen takes dates with the one date picker of BIBS (dd-MMM-yyyy) and months with its month
 * picker (MMM-yyyy), never the browser's own date, month or date-time field, which shows the value
 * in the format of the computer (mm/dd/yyyy).
 *
 * The check reads the screen sources: a plain input may not be a date, month or date-time field,
 * and a form field component that is given a date must render it with the shared date picker.
 */
import { describe, expect, it } from 'vitest';

const raw = import.meta.glob<string>(
  ['./**/*.tsx', '../components/**/*.tsx', '!./**/*.test.tsx', '!../components/**/*.test.tsx'],
  { query: '?raw', import: 'default', eager: true },
);

/** Sources keyed by their path below src, e.g. "features/claims/ClaimsPage.tsx". */
const sources = new Map(
  Object.entries(raw).map(([path, source]) => [
    path.startsWith('./') ? `features/${path.slice(2)}` : path.slice(3),
    source,
  ]),
);

const NATIVE_TYPES = /['"](date|month|datetime-local|week)['"]/;
const PICKERS = /\b(DateInput|TypedInput|MonthInput)\b/;

interface Element {
  tag: string;
  attributes: string;
  line: number;
}

/** The JSX elements of a source with their attributes (up to the end of the opening tag). */
function elements(source: string): Element[] {
  const found: Element[] = [];
  for (const match of source.matchAll(/<([A-Za-z][\w.]*)\b/g)) {
    const start = match.index;
    let depth = 0;
    let end = start + match[0].length;
    for (; end < source.length; end++) {
      const c = source[end];
      if (c === '{') depth++;
      else if (c === '}') depth--;
      else if (c === '>' && depth === 0) break;
    }
    found.push({
      tag: match[1] ?? '',
      attributes: source.slice(start + match[0].length, end),
      line: source.slice(0, start).split('\n').length,
    });
  }
  return found;
}

/** The value of an element's type attribute: the literal or the expression, '' when absent. */
function typeOf(attributes: string): string {
  const literal = /\btype=("[^"]*")/.exec(attributes);
  if (literal !== null) {
    return literal[1] ?? '';
  }
  const expression = /\btype=\{/.exec(attributes);
  if (expression === null) {
    return '';
  }
  let depth = 0;
  let i = expression.index + 'type='.length;
  const from = i;
  for (; i < attributes.length; i++) {
    if (attributes[i] === '{') depth++;
    else if (attributes[i] === '}' && --depth === 0) break;
  }
  return attributes.slice(from, i + 1);
}

/** The source path of the module a specifier names, relative to src. */
function resolve(fromPath: string, specifier: string): string | undefined {
  let base: string;
  if (specifier.startsWith('@/')) {
    base = specifier.slice(2);
  } else if (specifier.startsWith('.')) {
    const parts = fromPath.split('/').slice(0, -1);
    for (const part of specifier.split('/')) {
      if (part === '..') parts.pop();
      else if (part !== '.') parts.push(part);
    }
    base = parts.join('/');
  } else {
    return undefined;
  }
  return [`${base}.tsx`, `${base}/index.tsx`].find((p) => sources.has(p));
}

/** The source that defines a component used in a file: the file itself or the module it imports. */
function definitionOf(path: string, source: string, component: string): string | undefined {
  const own = new RegExp(`(function|const)\\s+${component}\\b`);
  if (own.test(source)) {
    return path;
  }
  const imported = new RegExp(
    `import\\s*\\{[^}]*\\b${component}\\b[^}]*\\}\\s*from\\s*'([^']+)'`,
  ).exec(source);
  const specifier = imported?.[1];
  return specifier === undefined ? undefined : resolve(path, specifier);
}

/** Why an element would show the browser's own date field, or undefined when it does not. */
function problemOf(path: string, source: string, { tag, attributes, line }: Element) {
  const type = typeOf(attributes);
  if (tag === 'input') {
    return NATIVE_TYPES.test(type) ? `${path}:${line} native ${type} field` : undefined;
  }
  if (!/^[A-Z]/.test(tag) || PICKERS.test(tag) || !/['"]date['"]/.test(type)) {
    return undefined;
  }
  const defined = definitionOf(path, source, tag.split('.')[0] ?? tag);
  const picked = defined !== undefined && PICKERS.test(sources.get(defined) ?? '');
  return picked ? undefined : `${path}:${line} <${tag}> takes a date without the date picker`;
}

/** The places in the screens that would show the browser's own date field. */
function nativeDateFields(): string[] {
  return [...sources]
    .filter(([path]) => path.startsWith('features/'))
    .flatMap(([path, source]) => elements(source).map((e) => problemOf(path, source, e)))
    .filter((problem): problem is string => problem !== undefined);
}

describe('date fields on every screen', () => {
  it('reads the screen sources', () => {
    expect(sources.size).toBeGreaterThan(500);
    expect(sources.has('components/ui/DateInput.tsx')).toBe(true);
  });

  it('finds a native date field, a native month field and a field component without the picker', () => {
    const probe = elements(
      [
        '<input id={id} type="date" value={a} />',
        "<input type={dated ? 'month' : 'text'} />",
        '<Field label="x">{(id) => <input id={id} type="text" />}</Field>',
      ].join('\n'),
    );
    expect(probe.map((e) => e.tag)).toEqual(['input', 'input', 'Field', 'input']);
    expect(probe.map((e) => NATIVE_TYPES.test(typeOf(e.attributes)))).toEqual([
      true,
      true,
      false,
      false,
    ]);
  });

  it('uses the dd-MMM-yyyy date picker on every screen, never the browser date field', () => {
    expect(nativeDateFields()).toEqual([]);
  });
});
