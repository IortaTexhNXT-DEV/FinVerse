import { api } from './client';

export type SubscriptionFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';
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

/** When a subscription runs, in words: "Daily", "Weekly on Monday", "Monthly on day 31". */
export function scheduleText(
  s: Pick<ReportSubscription, 'frequency' | 'dayOfWeek' | 'dayOfMonth'>,
): string {
  switch (s.frequency) {
    case 'WEEKLY':
      return `Weekly on ${WEEKDAYS[(s.dayOfWeek ?? 1) - 1] ?? ''}`;
    case 'MONTHLY':
      return `Monthly on day ${s.dayOfMonth ?? 1}`;
    default:
      return 'Daily';
  }
}

/** Recipients typed as a list separated by commas, semicolons or spaces. */
export function recipientsOf(text: string): string[] {
  return text
    .split(/[\s,;]+/)
    .map((r) => r.trim())
    .filter((r) => r !== '');
}
