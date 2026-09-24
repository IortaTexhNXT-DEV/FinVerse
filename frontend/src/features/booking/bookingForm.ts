import type {
  CancellationKind,
  EndorsementRequest,
  EndorsementType,
  PeriodBasis,
  PreviewLine,
  WorkbenchRow,
  WorkbenchTab,
} from '@/api/booking';

/** Workbench tabs in the BDOI order (BRNB.036). */
export const WORKBENCH_TABS: readonly { id: WorkbenchTab; label: string }[] = [
  { id: 'READY', label: 'Ready to Book' },
  { id: 'QUEUED', label: 'Queued for Batch' },
  { id: 'BOOKED', label: 'Booked Account' },
  { id: 'FAILED', label: 'Failed' },
];

/** The bulk actions a tab offers on its selected rows. */
export interface TabActions {
  bookNow: boolean;
  addToBatch: boolean;
  confirmBatch: boolean;
}

export function actionsOf(tab: WorkbenchTab): TabActions {
  return {
    bookNow: tab !== 'BOOKED',
    addToBatch: tab === 'READY' || tab === 'FAILED',
    confirmBatch: tab === 'QUEUED',
  };
}

/** Adds or removes a key from a selection (immutable). */
export function toggle<T>(selection: ReadonlySet<T>, key: T): Set<T> {
  const next = new Set(selection);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  return next;
}

/** Selects every row of the page, or clears the selection when all are selected. */
export function toggleAll<T>(selection: ReadonlySet<T>, keys: readonly T[]): Set<T> {
  const all = keys.length > 0 && keys.every((k) => selection.has(k));
  return all ? new Set<T>() : new Set(keys);
}

/** The ARNs of the selected rows. */
export function selectedArns(
  rows: readonly WorkbenchRow[],
  selection: ReadonlySet<number>,
): string[] {
  return rows.filter((r) => selection.has(r.id)).map((r) => r.arn);
}

/** The ARNs handed over by another screen as `?arns=ARN-1,ARN-2` (Placement "For Booking"). */
export function handedArns(param: string | null): string[] {
  if (param === null) {
    return [];
  }
  const arns = param
    .split(',')
    .map((a) => a.trim())
    .filter((a) => a !== '');
  return [...new Set(arns)];
}

/** The ids of the rows whose ARN was handed over, added to the user's own selection. */
export function withHanded(
  rows: readonly WorkbenchRow[],
  selection: ReadonlySet<number>,
  handed: readonly string[],
): Set<number> {
  const result = new Set(selection);
  rows.filter((r) => handed.includes(r.arn)).forEach((r) => result.add(r.id));
  return result;
}

/** Where a workbench row opens: the invoice once booked, else the pre-booking confirmation. */
export function rowLink(row: WorkbenchRow): string {
  return row.invoiceId === undefined
    ? `/booking/book/${encodeURIComponent(row.arn)}`
    : `/booking/invoices/${row.invoiceId}`;
}

/** Debit and credit totals of journal lines. */
export function journalTotals(lines: readonly Pick<PreviewLine, 'side' | 'amount'>[]): {
  debit: number;
  credit: number;
  balanced: boolean;
} {
  const cents = (side: string) =>
    lines.filter((l) => l.side === side).reduce((sum, l) => sum + Math.round(l.amount * 100), 0);
  const debit = cents('DEBIT');
  const credit = cents('CREDIT');
  return { debit: debit / 100, credit: credit / 100, balanced: debit > 0 && debit === credit };
}

/** Endorsement entry form (BRNB.076/081). */
export interface EndorsementForm {
  type: EndorsementType;
  effectiveDate: string;
  basis: PeriodBasis;
  sumInsuredChange: string;
  ratePercent: string;
  description: string;
  bookingDate: string;
}

/** Cancellation dialog (BRNB.094). */
export interface CancellationForm {
  kind: CancellationKind;
  basis: PeriodBasis;
  effectiveDate: string;
  reasonCode: string;
  description: string;
  bookingDate: string;
}

export type FieldErrors = Partial<Record<string, string>>;

function numberOrUndefined(value: string): number | undefined {
  return value.trim() === '' ? undefined : Number(value);
}

function changeError(form: EndorsementForm): string | undefined {
  if (form.type === 'NON_FINANCIAL') {
    return undefined;
  }
  const change = numberOrUndefined(form.sumInsuredChange);
  if (change === undefined || Number.isNaN(change) || change === 0) {
    return 'Enter the change of the sum insured';
  }
  if (form.type === 'POSITIVE' && change < 0) {
    return 'A positive endorsement increases the sum insured';
  }
  if (form.type === 'NEGATIVE' && change > 0) {
    return 'A negative endorsement reduces the sum insured (enter a minus)';
  }
  return undefined;
}

/** Field errors of the endorsement form; empty when it can be previewed and posted. */
export function endorsementErrors(form: EndorsementForm): FieldErrors {
  const errors: FieldErrors = {};
  if (form.effectiveDate === '') {
    errors.effectiveDate = 'Enter the effective date';
  }
  if (form.description.trim() === '') {
    errors.description = 'Describe the change';
  }
  const change = changeError(form);
  if (change !== undefined) {
    errors.sumInsuredChange = change;
  }
  const rate = numberOrUndefined(form.ratePercent);
  if (rate !== undefined && (Number.isNaN(rate) || rate < 0)) {
    errors.ratePercent = 'Enter a rate of zero or more, or leave it blank';
  }
  return errors;
}

/** The request of an endorsement form. */
export function endorsementRequest(arn: string, form: EndorsementForm): EndorsementRequest {
  const financial = form.type !== 'NON_FINANCIAL';
  return {
    arn,
    type: form.type,
    effectiveDate: form.effectiveDate,
    basis: financial ? form.basis : undefined,
    sumInsuredChange: financial ? numberOrUndefined(form.sumInsuredChange) : undefined,
    ratePercent: financial ? numberOrUndefined(form.ratePercent) : undefined,
    description: form.description.trim(),
    bookingDate: form.bookingDate || undefined,
  };
}

/** Field errors of the cancellation dialog. */
export function cancellationErrors(form: CancellationForm): FieldErrors {
  const errors: FieldErrors = {};
  if (form.effectiveDate === '') {
    errors.effectiveDate = 'Enter the cancellation date';
  }
  if (form.reasonCode === '') {
    errors.reasonCode = 'Choose the reason';
  }
  if (form.description.trim() === '') {
    errors.description = 'Describe the cancellation';
  }
  return errors;
}

/** The request of a cancellation. */
export function cancellationRequest(arn: string, form: CancellationForm): EndorsementRequest {
  return {
    arn,
    type: 'CANCELLATION',
    cancellationKind: form.kind,
    basis: form.kind === 'PARTIAL' ? form.basis : undefined,
    effectiveDate: form.effectiveDate,
    reasonCode: form.reasonCode,
    description: form.description.trim(),
    bookingDate: form.bookingDate || undefined,
  };
}

/** Whether a form has no errors. */
export function isValid(errors: FieldErrors): boolean {
  return Object.values(errors).every((e) => e === undefined);
}

/** Readable labels of the coded values. */
export const KIND_LABELS: Record<string, string> = {
  BOOKING: 'Booking',
  ENDORSEMENT_PLUS: 'Positive endorsement',
  ENDORSEMENT_MINUS: 'Negative endorsement',
  CANCELLATION: 'Cancellation',
  POSITIVE: 'Positive (additional premium)',
  NEGATIVE: 'Negative (return premium)',
  NON_FINANCIAL: 'Non-financial',
  FLAT: 'Flat (from inception)',
  FLAT_RETAIN_DST: 'Flat, retaining DST',
  PARTIAL: 'Partial (unexpired term)',
  PRO_RATA: 'Pro-rata (days)',
  SHORT_PERIOD: 'Short period (table)',
};

export function labelOf(code: string | undefined): string {
  return code === undefined ? '' : (KIND_LABELS[code] ?? code);
}
