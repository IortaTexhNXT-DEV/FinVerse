/**
 * The one acronym map of the labels users read (BDO style guide: Title Case, acronyms kept in
 * capitals). Every label helper (`humanize`, `titleCase`, `statusPhrase`, the status pills and the
 * list-of-values fallbacks) reads it, so a code such as RA_SENT reads "RA Sent", never "Ra Sent".
 */
export const ACRONYM_LIST: readonly string[] = [
  // documents and references
  'AR',
  'ARN',
  'CL',
  'COC',
  'DV',
  'ID',
  'IAAF',
  'LOA',
  'OR',
  'PDC',
  'PN',
  'PR',
  'PRF',
  'PS',
  'QS',
  'RA',
  'RFQ',
  'SOA',
  'STR',
  'TIN',
  'TOR',
  // business lines, units and parties
  'AO',
  'CBG',
  'CSF',
  'EB',
  'HMO',
  'HO',
  'HR',
  'IA',
  'LAMD',
  'MBS',
  'NB',
  'NCR',
  'SM',
  'TL',
  'TSU',
  'UH',
  // cover, tax and accounting
  'ACSL',
  'ADA',
  'BIR',
  'CLPC',
  'CPC2',
  'CTPL',
  'CWT',
  'DP',
  'DST',
  'DTIP',
  'EOD',
  'FFY',
  'FRBS',
  'GL',
  'GLI',
  'GPA',
  'IBNR',
  'LGT',
  'OTC',
  'SI',
  'SL',
  'TSI',
  'UPR',
  'VAT',
  'WTAX',
  // compliance and platform
  'AML',
  'AMLC',
  'KYC',
  'LOV',
  'PEP',
  'SLA',
];

/** Codes shown in capitals inside labels. */
export const ACRONYMS: ReadonlySet<string> = new Set(ACRONYM_LIST);

/** Words with a fixed mixed-case spelling ("ManCom", "ePolicy"). */
export const WORD_FORMS: Readonly<Record<string, string>> = {
  MANCOM: 'ManCom',
  EPOLICY: 'ePolicy',
};

/** A word of a code in its label form: the acronym in capitals or the fixed spelling, else null. */
export function acronymOf(word: string): string | null {
  const upper = word.toUpperCase();
  if (ACRONYMS.has(upper)) {
    return upper;
  }
  return WORD_FORMS[upper] ?? null;
}
