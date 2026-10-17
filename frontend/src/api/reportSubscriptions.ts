import { api } from './client';

export type SubscriptionFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'LAST_WORKING_DAY';

/** A date period relative to the business date of each run. */
export type RelativePeriod =
  | 'PREVIOUS_DAY'
  | 'MONTH_TO_DATE'
  | 'PREVIOUS_MONTH'
  | 'QUARTER_TO_DATE'
  | 'PREVIOUS_QUARTER'
  | 'YEAR_TO_DATE'
  | 'PREVIOUS_YEAR';

/** The relative periods with their labels. */
export const RELATIVE_PERIODS: { value: RelativePeriod; label: string }[] = [
  { value: 'PREVIOUS_DAY', label: 'Previous day' },
  { value: 'MONTH_TO_DATE', label: 'Month to date' },
  { value: 'PREVIOUS_MONTH', label: 'Previous month' },
  { value: 'QUARTER_TO_DATE', label: 'Quarter to date' },
  { value: 'PREVIOUS_QUARTER', label: 'Previous quarter' },
  { value: 'YEAR_TO_DATE', label: 'Year to date' },
  { value: 'PREVIOUS_YEAR', label: 'Previous year' },
];
export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** A subscription to a report (G04). */
export interface SubscriptionInput {
  reportCode?: string;
  parameters: Record<string, string>;
  frequency: SubscriptionFrequency;
  dayOfWeek?: DayOfWeek | null;
  dayOfMonth?: number | null;
  format: string;
  recipients: string[];
  /** Time of day of the run (HH:mm, business time); null for 06:00. */
  runTime?: string | null;
  /** Period of the date parameters at each run; null to keep the dates entered. */
  datePeriod?: RelativePeriod | null;
}

export interface ReportSubscription {
  id: number;
  reportCode: string;
  username: string;
  parameters: Record<string, string>;
  frequency: SubscriptionFrequency;
  /** 1 Monday to 7 Sunday. */
  dayOfWeek: number | null;
  dayOfMonth: number | null;
  format: string;
  recipients: string[];
  active: boolean;
  lastRunDate: string | null;
  createdAt: string;
  runTime: string | null;
  datePeriod: RelativePeriod | null;
}

/** One run of a subscription: sent, failed, or denied (permission or data scope lost). */
export interface SubscriptionRun {
  id: number;
  runDate: string;
  status: 'SUCCEEDED' | 'FAILED' | 'DENIED';
  reportRunId: number | null;
  rowCount: number;
  detail: string | null;
  runAt: string;
}

export const reportSubscriptionApi = {
  mine: () => api.get<ReportSubscription[]>('/report-subscriptions'),
  subscribe: (input: SubscriptionInput) =>
    api.post<ReportSubscription>('/report-subscriptions', input),
  update: (id: number, input: SubscriptionInput) =>
    api.put<ReportSubscription>(`/report-subscriptions/${id}`, input),
  deactivate: (id: number) =>
    api.post<ReportSubscription>(`/report-subscriptions/${id}/deactivate`),
  runs: (id: number) => api.get<SubscriptionRun[]>(`/report-subscriptions/${id}/runs`),
};

const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function dayText(s: Pick<ReportSubscription, 'frequency' | 'dayOfWeek' | 'dayOfMonth'>): string {
  switch (s.frequency) {
    case 'WEEKLY':
      return `Weekly on ${WEEKDAYS[(s.dayOfWeek ?? 1) - 1] ?? ''}`;
    case 'MONTHLY':
      return `Monthly on day ${s.dayOfMonth ?? 1}`;
    case 'LAST_WORKING_DAY':
      return 'Monthly on the last working day';
    default:
      return 'Daily';
  }
}

/**
 * When a subscription runs, in words: "Daily at 06:00", "Weekly on Monday at 18:00", "Monthly on
 * the last working day at 06:00", with its date period.
 */
export function scheduleText(
  s: Pick<ReportSubscription, 'frequency' | 'dayOfWeek' | 'dayOfMonth'> &
    Partial<Pick<ReportSubscription, 'runTime' | 'datePeriod'>>,
): string {
  const period = RELATIVE_PERIODS.find((p) => p.value === s.datePeriod)?.label;
  const time = (s.runTime ?? '06:00').slice(0, 5);
  const suffix = period ? ', ' + period.toLowerCase() : '';
  return `${dayText(s)} at ${time}${suffix}`;
}

/** Recipients typed as a list separated by commas, semicolons or spaces. */
export function recipientsOf(text: string): string[] {
  return text
    .split(/[\s,;]+/)
    .map((r) => r.trim())
    .filter((r) => r !== '');
}
