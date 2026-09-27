/** Abbreviations shown in capitals (or their usual form) in field labels. */
const ACRONYMS: Record<string, string> = {
  atc: 'ATC',
  dac: 'DAC',
  dst: 'DST',
  ewt: 'EWT',
  fac: 'FAC',
  fst: 'FST',
  fx: 'FX',
  gl: 'GL',
  ibnr: 'IBNR',
  id: 'ID',
  lgt: 'LGT',
  lpo: 'LPO',
  mfad: 'MfAD',
  no: 'no.',
  oslr: 'OSLR',
  pct: '%',
  pdc: 'PDC',
  ri: 'RI',
  tin: 'TIN',
  ucr: 'UCR',
  ulae: 'ULAE',
  upr: 'UPR',
  uw: 'UW',
  vat: 'VAT',
  xol: 'XOL',
};

/** "glAccountCode" -> "GL account code", "mfad_pct" -> "MfAD %". */
function words(camel: string): string {
  return camel
    .replace(/([a-z\d])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .split(' ')
    .map((w) => ACRONYMS[w] ?? w)
    .join(' ');
}

/** "lines" -> "line", "entries" -> "entry" (element name of a collection). */
function singular(name: string): string {
  if (name.endsWith('ies')) {
    return `${name.slice(0, -3)}y`;
  }
  return name.endsWith('s') ? name.slice(0, -1) : name;
}

/**
 * Readable label of a bean-validation field path: "lines[0].amount" becomes "Line 1 amount",
 * "risks[2].sumInsured" becomes "Risk 3 sum insured". Indexed segments keep their position; a
 * plain nested object path keeps only its last segment ("initialPassword.newPassword" becomes
 * "New password").
 */
export function humanizeField(path: string): string {
  const segments = path.split('.');
  const kept = segments.filter((s, i) => i === segments.length - 1 || /\[\d+\]$/.test(s));
  const label = kept
    .map((segment) => {
      const indexed = /^(\w+)\[(\d+)\]$/.exec(segment);
      if (indexed === null) {
        return words(segment);
      }
      return `${words(singular(indexed[1] ?? ''))} ${Number(indexed[2]) + 1}`;
    })
    .join(' ');
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function describePattern(regex: string): string {
  const parts: string[] = [];
  if (regex.includes('A-Z')) parts.push('capital letters');
  if (regex.includes('a-z')) parts.push('lower-case letters');
  if (regex.includes('0-9') || regex.includes('\\d')) parts.push('digits');
  if (regex.includes('\\-')) parts.push('hyphens');
  if (regex.includes('.') && regex.includes('[')) parts.push('dots');
  if (regex.includes('_')) parts.push('underscores');
  return parts.join(', ');
}

/** Replaces technical validator messages with wording a business user understands. */
export function humanizeMessage(message: string): string {
  const pattern = /^must match "(.*)"$/.exec(message);
  if (pattern === null) {
    return message;
  }
  const allowed = describePattern(pattern[1] ?? '');
  return allowed === '' ? 'has an invalid format' : `has an invalid format (allowed: ${allowed})`;
}

/** Field errors of a VALIDATION_FAILED response as readable lines, in the server's order. */
export function fieldErrorLines(errors: Record<string, string>): string[] {
  return Object.entries(errors).map(
    ([field, message]) => `${humanizeField(field)}: ${humanizeMessage(message)}`,
  );
}
