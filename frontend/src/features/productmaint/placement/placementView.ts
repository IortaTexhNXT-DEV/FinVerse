import type { PlacementSettings } from '@/api/pmPlacement';
import { formatDateTime } from '@/utils/format';

const DAYS: Readonly<Record<string, string>> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

/** The schedule in words: "Every Thursday at 08:00, reporting period of 7 days". */
export function scheduleText(s: PlacementSettings): string {
  const day = DAYS[s.day] ?? s.day;
  return `Every ${day} at ${s.time}, reporting period of ${s.periodDays} day${s.periodDays === 1 ? '' : 's'}`;
}

/** The next run as a date and time of the business time zone. */
export function nextRunText(s: PlacementSettings): string {
  return formatDateTime(s.nextRun) || s.nextRun;
}
