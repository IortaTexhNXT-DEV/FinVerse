/** Module prefixes of the permission codes, in words (the same rule as the server's names). */
const PREFIXES: Record<string, string> = {
  UAM: 'User access',
  SCR: 'Screening',
  RNW: 'Renewal',
  EB: 'Employee benefits',
  BCL: 'Claims handling',
  PKG: 'Package',
  MIG: 'Data migration',
  LOV: 'Lists of values',
  NB: 'New business',
};

const ACRONYMS = new Set([
  'TSU',
  'MBS',
  'QS',
  'PRF',
  'KYC',
  'AML',
  'STR',
  'ACSL',
  'FRBS',
  'GL',
  'AR',
  'AP',
  'DV',
  'PR',
  'OR',
  'SOA',
  'EOD',
  'BIR',
  'VAT',
  'EWT',
  'SL',
  'SOD',
  'CSF',
  'ID',
  'LAMD',
]);

/**
 * The readable name of a permission code: the module prefix in words and the rest in lower case,
 * for example UAM_ENROLL is "User access enroll" and PKG_TSU_APPROVE "Package TSU approve".
 */
export function permissionLabel(code: string): string {
  if (code.trim() === '') {
    return '';
  }
  const words = code.split('_').map((part, i) => {
    if (i === 0 && PREFIXES[part] !== undefined) {
      return PREFIXES[part];
    }
    return ACRONYMS.has(part) ? part : part.toLowerCase();
  });
  const text = words.join(' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Readable names of permission codes, sorted by code and separated by commas. */
export function permissionLabels(codes: readonly string[]): string {
  return [...codes]
    .sort((a, b) => a.localeCompare(b))
    .map(permissionLabel)
    .join(', ');
}
