import type {
  Bucket,
  BucketCounts,
  ReconItem,
  ReconSchedule,
  ReconSide,
  ScheduleInput,
} from './prodreconApi';

/** One compared field of the side-by-side view. */
export interface SideRow {
  key: keyof ReconSide;
  label: string;
  bdoi: string;
  insurer: string;
  /** The two sides disagree (flagged by the matcher or visibly different). */
  differs: boolean;
}

const FIELDS: readonly { key: keyof ReconSide; label: string; flag: string }[] = [
  { key: 'policyNo', label: 'Policy No.', flag: 'POLICY_NO' },
  { key: 'referenceNo', label: 'Invoice / Reference No.', flag: 'REFERENCE_NO' },
  { key: 'pnNo', label: 'PN No.', flag: 'PN_NO' },
  { key: 'assuredName', label: 'Assured Name', flag: 'ASSURED_NAME' },
  { key: 'periodFrom', label: 'Period From', flag: 'PERIOD_FROM' },
  { key: 'periodTo', label: 'Period To', flag: 'PERIOD_TO' },
  { key: 'basicPremium', label: 'Basic Premium', flag: 'BASIC_PREMIUM' },
  { key: 'grossPremium', label: 'Gross Premium', flag: 'GROSS_PREMIUM' },
  { key: 'commission', label: 'Commission', flag: 'COMMISSION' },
];

function text(value: string | number | undefined): string {
  if (value === undefined) {
    return '';
  }
  return typeof value === 'number' ? value.toFixed(2) : value;
}

/**
 * The BDOI and insurer values of an item side by side (PRCID.013), the matcher's discrepancies
 * flagged. A field the matcher did not compare is flagged only when both sides have a value that
 * differs.
 */
export function sideBySide(item: Pick<ReconItem, 'bdoi' | 'insurer' | 'discrepancies'>): SideRow[] {
  const flagged = new Set(item.discrepancies.map((d) => d.split(/[\s:]/)[0]?.toUpperCase()));
  return FIELDS.map(({ key, label, flag }) => {
    const bdoi = text(item.bdoi?.[key]);
    const insurer = text(item.insurer?.[key]);
    const bothGiven = bdoi !== '' && insurer !== '';
    return {
      key,
      label,
      bdoi,
      insurer,
      differs: flagged.has(flag) || (bothGiven && bdoi.toUpperCase() !== insurer.toUpperCase()),
    };
  });
}

/** Tabs of a cycle's items with their counts. */
export function bucketTabs(counts: BucketCounts): { id: Bucket; label: string }[] {
  return [
    { id: 'ALL', label: `All (${String(counts.total)})` },
    { id: 'MATCHED', label: `Matched (${String(counts.matched)})` },
    { id: 'DISCREPANCY', label: `With Discrepancy (${String(counts.discrepancy)})` },
    { id: 'BDOI_ONLY', label: `BDOI Only (${String(counts.bdoiOnly)})` },
    { id: 'INSURER_ONLY', label: `Insurer Only (${String(counts.insurerOnly)})` },
  ];
}

/** Share of the cycle's items matched, 0-100, rounded down. */
export function matchedPercent(counts: BucketCounts): number {
  if (counts.total === 0) {
    return 0;
  }
  return Math.floor(((counts.matched + counts.discrepancy) * 100) / counts.total);
}

/** "2026-09-01" as "Sep 2026". */
export function monthLabel(iso: string | undefined): string {
  if (iso === undefined || !/^\d{4}-\d{2}/.test(iso)) {
    return '';
  }
  const [year, month] = iso.split('-');
  const date = new Date(Date.UTC(Number(year), Number(month) - 1, 1));
  return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric', timeZone: 'UTC' });
}

/** First and last day of the month of a "yyyy-MM" value. */
export function monthRange(month: string): { from: string; to: string } {
  const [year, m] = month.split('-').map(Number);
  const last = new Date(Date.UTC(year ?? 1970, m ?? 1, 0)).getUTCDate();
  const mm = String(m).padStart(2, '0');
  return { from: `${String(year)}-${mm}-01`, to: `${String(year)}-${mm}-${String(last)}` };
}

/** Addresses typed with commas, semicolons or spaces. */
export function addresses(value: string): string[] {
  return value
    .split(/[\s,;]+/)
    .map((a) => a.trim())
    .filter((a) => a !== '');
}

function isAddress(value: string): boolean {
  const parts = value.split('@');
  if (parts.length !== 2) {
    return false;
  }
  const [local = '', domain = ''] = parts;
  const dot = domain.lastIndexOf('.');
  return local !== '' && dot > 0 && dot < domain.length - 1;
}

/** Whether an address list is valid (at least one, each with an @ and a dot after it). */
export function validAddresses(value: string): boolean {
  const list = addresses(value);
  return list.length > 0 && list.every(isAddress);
}

/** Whether an item may be paired with an insurer-only row (manual matching, PRCID.014). */
export function mayPair(item: Pick<ReconItem, 'status'>): boolean {
  return item.status === 'BDOI_ONLY';
}

/** Whether an item may be split back into its two sides (a wrong pairing). */
export function maySplit(item: Pick<ReconItem, 'status'>): boolean {
  return item.status === 'MATCHED' || item.status === 'MATCHED_WITH_DISCREPANCY';
}

/** Last run day of a monthly schedule. */
export const MAX_MONTH_DAY = 28;
/** Last run day of a weekly schedule (Sunday). */
export const MAX_WEEK_DAY = 7;
const DEFAULT_RUN_DAY = 5;

/** The form of a new schedule or of the schedule being edited. */
export function scheduleForm(
  companyId: number,
  schedule: ReconSchedule | undefined,
): ScheduleInput {
  if (schedule === undefined) {
    return {
      companyId,
      insurerCode: '',
      frequency: 'MONTHLY',
      runDay: DEFAULT_RUN_DAY,
      autoSend: false,
      recipients: '',
      active: true,
    };
  }
  return {
    companyId,
    insurerCode: schedule.insurerCode,
    frequency: schedule.frequency,
    runDay: schedule.runDay,
    autoSend: schedule.autoSend,
    recipients: schedule.recipients ?? '',
    active: schedule.active,
  };
}

/** The first problem of a schedule, undefined when it can be saved. */
export function scheduleProblem(input: ScheduleInput): string | undefined {
  if (input.insurerCode.trim() === '') {
    return 'Insurer code is required';
  }
  const max = input.frequency === 'WEEKLY' ? MAX_WEEK_DAY : MAX_MONTH_DAY;
  if (!Number.isInteger(input.runDay) || input.runDay < 1 || input.runDay > max) {
    return input.frequency === 'WEEKLY'
      ? 'Run day is a weekday from 1 (Monday) to 7 (Sunday)'
      : 'Run day is a day of the month from 1 to 28';
  }
  if (input.autoSend && input.recipients.trim() !== '' && !validAddresses(input.recipients)) {
    return 'Enter valid e-mail addresses';
  }
  return undefined;
}
