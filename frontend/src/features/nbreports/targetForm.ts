import type { SalesTarget, UnitLevel } from '@/api/nbReports';

/** Target form values as typed by the user. */
export interface TargetForm {
  unitLevel: UnitLevel;
  unitCode: string;
  periodFrom: string;
  periodTo: string;
  targetCount: string;
  targetPremium: string;
  targetCommission: string;
}

/** First and last day of a month (`yyyy-MM`). */
export function monthRange(month: string): { from: string; to: string } {
  const [y, m] = month.split('-').map(Number);
  const year = y ?? 1970;
  const index = (m ?? 1) - 1;
  const last = new Date(Date.UTC(year, index + 1, 0)).getUTCDate();
  const mm = String(index + 1).padStart(2, '0');
  return { from: `${String(year)}-${mm}-01`, to: `${String(year)}-${mm}-${String(last)}` };
}

/** An empty form for a level and month. */
export function emptyTarget(level: UnitLevel, month: string): TargetForm {
  const { from, to } = monthRange(month);
  return {
    unitLevel: level,
    unitCode: '',
    periodFrom: from,
    periodTo: to,
    targetCount: '0',
    targetPremium: '0.00',
    targetCommission: '0.00',
  };
}

/** The form of an existing target. */
export function formOf(t: SalesTarget): TargetForm {
  return {
    unitLevel: t.unitLevel,
    unitCode: t.unitCode,
    periodFrom: t.periodFrom,
    periodTo: t.periodTo,
    targetCount: String(t.targetCount),
    targetPremium: t.targetPremium.toFixed(2),
    targetCommission: t.targetCommission.toFixed(2),
  };
}

function nonNegative(text: string): boolean {
  const n = Number(text);
  return text.trim() !== '' && Number.isFinite(n) && n >= 0;
}

/** Field errors of the form (empty when valid). */
export function targetErrors(f: TargetForm): Partial<Record<keyof TargetForm, string>> {
  const errors: Partial<Record<keyof TargetForm, string>> = {};
  if (f.unitCode.trim() === '') {
    errors.unitCode = 'Enter the unit code or the officer username';
  }
  if (f.periodFrom === '' || f.periodTo === '') {
    errors.periodTo = 'Enter the period';
  } else if (f.periodTo < f.periodFrom) {
    errors.periodTo = 'The period end must not be before the start';
  }
  if (!nonNegative(f.targetCount) || !Number.isInteger(Number(f.targetCount))) {
    errors.targetCount = 'Enter a whole number of bookings (0 or more)';
  }
  if (!nonNegative(f.targetPremium)) {
    errors.targetPremium = 'Enter an amount of 0 or more';
  }
  if (!nonNegative(f.targetCommission)) {
    errors.targetCommission = 'Enter an amount of 0 or more';
  }
  return errors;
}

/** The request of a valid form. */
export function toTarget(f: TargetForm): SalesTarget {
  return {
    unitLevel: f.unitLevel,
    unitCode: f.unitCode.trim(),
    periodFrom: f.periodFrom,
    periodTo: f.periodTo,
    targetCount: Number(f.targetCount),
    targetPremium: Number(f.targetPremium),
    targetCommission: Number(f.targetCommission),
  };
}
