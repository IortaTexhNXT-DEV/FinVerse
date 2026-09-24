const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/** How long ago a time was, compactly ("45 min", "5 h", "3 d"). */
export function ageText(iso: string, now: number = Date.now()): string {
  const elapsed = Math.max(0, now - new Date(iso).getTime());
  if (elapsed < HOUR) {
    return `${Math.floor(elapsed / MINUTE)} min`;
  }
  if (elapsed < DAY) {
    return `${Math.floor(elapsed / HOUR)} h`;
  }
  return `${Math.floor(elapsed / DAY)} d`;
}
