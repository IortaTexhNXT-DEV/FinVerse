/**
 * Schedules of scheduled runs in words. The server keeps a schedule as a six-part timing rule in UTC
 * (second, minute, hour, day, month, weekday; "-" for manual runs); users choose and read a
 * frequency and a time in Philippine time (UTC+8), never the rule itself.
 */
export type Frequency = 'MANUAL' | 'HOURLY' | 'DAILY' | 'WEEKDAYS';

export const FREQUENCIES: readonly { id: Frequency; label: string }[] = [
  { id: 'MANUAL', label: 'Manual runs only' },
  { id: 'HOURLY', label: 'Every hour' },
  { id: 'DAILY', label: 'Every day' },
  { id: 'WEEKDAYS', label: 'Monday to Friday' },
];

const PHT_OFFSET = 8;

export interface Schedule {
  frequency: Frequency;
  /** HH:mm in Philippine time (daily and weekday runs), minute of the hour for hourly runs. */
  time: string;
}

const pad = (n: number) => String(n).padStart(2, '0');

const inRange = (n: number, max: number) => Number.isInteger(n) && n >= 0 && n <= max;

/** The minute and hour parts of a six-part rule run at second 0 on any day of any month. */
function ruleParts(r: string): { minute: number; hour: string; weekday: string } | undefined {
  const parts = r.split(/\s+/);
  if (parts.length !== 6 || parts[0] !== '0' || parts[3] !== '*' || parts[4] !== '*') {
    return undefined;
  }
  const minute = Number(parts[1]);
  return inRange(minute, 59)
    ? { minute, hour: parts[2] ?? '', weekday: parts[5] ?? '' }
    : undefined;
}

/** A daily or weekday rule at an hour in UTC, read in Philippine time. */
function dayRun(minute: number, hour: number, weekday: string): Schedule | undefined {
  const time = `${pad((hour + PHT_OFFSET) % 24)}:${pad(minute)}`;
  if (weekday === '*') {
    return { frequency: 'DAILY', time };
  }
  const nextDay = hour + PHT_OFFSET >= 24;
  const weekdays = nextDay ? 'SUN-THU' : 'MON-FRI';
  return weekday === weekdays ? { frequency: 'WEEKDAYS', time } : undefined;
}

/** The frequency and Philippine time of a stored rule; undefined for a rule not made on screen. */
export function readSchedule(rule: string | null | undefined): Schedule | undefined {
  const r = (rule ?? '-').trim();
  if (r === '' || r === '-') {
    return { frequency: 'MANUAL', time: '' };
  }
  const p = ruleParts(r);
  if (p === undefined) {
    return undefined;
  }
  if (p.hour === '*') {
    return p.weekday === '*' ? { frequency: 'HOURLY', time: `00:${pad(p.minute)}` } : undefined;
  }
  const hour = Number(p.hour);
  return inRange(hour, 23) ? dayRun(p.minute, hour, p.weekday) : undefined;
}

/** The stored rule of a schedule chosen on screen. */
export function scheduleRule(s: Schedule): string {
  if (s.frequency === 'MANUAL') {
    return '-';
  }
  const [h = '0', m = '0'] = (s.time || '00:00').split(':');
  const minute = Number(m);
  if (s.frequency === 'HOURLY') {
    return `0 ${String(minute)} * * * *`;
  }
  const utc = (Number(h) - PHT_OFFSET + 24) % 24;
  // Early in the Philippine morning it is still the day before in UTC.
  const weekdays = Number(h) < PHT_OFFSET ? 'SUN-THU' : 'MON-FRI';
  return `0 ${String(minute)} ${String(utc)} * * ${s.frequency === 'WEEKDAYS' ? weekdays : '*'}`;
}

/** A schedule in words: "Manual", "Every hour at :15", "Every day at 08:30", "Monday to Friday at 18:00". */
export function scheduleText(rule: string | null | undefined): string {
  const s = readSchedule(rule);
  if (s === undefined) {
    return 'Set by the System Administrator';
  }
  switch (s.frequency) {
    case 'MANUAL':
      return 'Manual';
    case 'HOURLY':
      return `Every hour at :${s.time.slice(3)}`;
    case 'DAILY':
      return `Every day at ${s.time}`;
    default:
      return `Monday to Friday at ${s.time}`;
  }
}
