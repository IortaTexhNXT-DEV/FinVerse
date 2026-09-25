const pad = (n: number) => String(n).padStart(2, '0');

/**
 * Value of a datetime-local input (local time of the browser) for an instant.
 *
 * @param iso instant, e.g. "2030-04-02T09:00:00Z"
 * @returns "2030-04-02T17:00" in Manila, '' when invalid
 */
export function toLocalInput(iso: string | undefined): string {
  if (iso === undefined) {
    return '';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return '';
  }
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Instant of a datetime-local input value.
 *
 * @param value "2030-04-02T17:00"
 * @returns ISO instant, undefined when blank or invalid
 */
export function fromLocalInput(value: string): string | undefined {
  if (value.trim() === '') {
    return undefined;
  }
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

/**
 * Whether a period may be closed on a date when only the previous month may be closed (FRBS
 * 2.6.1).
 *
 * @param periodEnd last day of the period (yyyy-MM-dd)
 * @param closeDate close date (yyyy-MM-dd)
 * @returns true when the period is the month before the close date
 */
export function isPreviousMonth(periodEnd: string, closeDate: string): boolean {
  const [py, pm] = periodEnd.split('-').map(Number);
  const [cy, cm] = closeDate.split('-').map(Number);
  if ([py, pm, cy, cm].some((n) => n === undefined || Number.isNaN(n))) {
    return false;
  }
  const monthsApart = ((cy ?? 0) - (py ?? 0)) * 12 + ((cm ?? 0) - (pm ?? 0));
  return monthsApart === 1;
}

/** Why a close cannot be scheduled or run now, per the previous-month rule (FRBS 2.6.1). */
export interface CloseGuards {
  /** The date part of the chosen close time, or today. */
  closeDate: string;
  /** Refusal of the chosen time, undefined when allowed. */
  schedule?: string;
  /** Refusal of Close Now, undefined when allowed. */
  now?: string;
}

/**
 * Checks a period against the chosen close time and today.
 *
 * @param periodEnd last day of the period, undefined when no period is selected
 * @param chosen datetime-local value, '' for none
 * @param todayDate today (yyyy-MM-dd)
 * @returns guards
 */
export function closeGuards(
  periodEnd: string | undefined,
  chosen: string,
  todayDate: string,
): CloseGuards {
  const closeDate = chosen === '' ? todayDate : chosen.slice(0, 10);
  if (periodEnd === undefined) {
    return { closeDate };
  }
  return {
    closeDate,
    schedule: isPreviousMonth(periodEnd, closeDate)
      ? undefined
      : `Only the month before ${closeDate.slice(0, 7)} may be closed on that date`,
    now: isPreviousMonth(periodEnd, todayDate)
      ? undefined
      : 'Close Now is only for the previous month',
  };
}
