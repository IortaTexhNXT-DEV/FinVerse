/** Numbering scheme being edited (FRBS 2.3.2). */
export interface NumberingForm {
  parentCode: string;
  separator: string;
  width: number;
  active: boolean;
}

const CODE = /^[0-9A-Z.-]+$/;
const MAX_WIDTH = 6;

/**
 * Field errors of a numbering scheme.
 *
 * @param f scheme
 * @returns errors by field
 */
export function numberingProblems(f: NumberingForm): Partial<Record<keyof NumberingForm, string>> {
  const problems: Partial<Record<keyof NumberingForm, string>> = {};
  if (!CODE.test(f.parentCode.trim())) {
    problems.parentCode = 'Enter the code of an existing parent account';
  }
  if (!Number.isInteger(f.width) || f.width < 1 || f.width > MAX_WIDTH) {
    problems.width = 'Between 1 and 6 digits';
  }
  return problems;
}

/**
 * The code of the first child of a scheme, as the server generates it.
 *
 * @param f scheme
 * @returns e.g. "1210.01"
 */
export function numberingExample(f: NumberingForm): string {
  const width = Math.min(Math.max(Math.trunc(f.width) || 1, 1), MAX_WIDTH);
  return `${f.parentCode.trim() || 'PARENT'}${f.separator}${'1'.padStart(width, '0')}`;
}
