const DAY_MS = 86_400_000;

/** Whole days an item has been waiting since submission. */
export function ageInDays(submittedAt: string | undefined, now: number = Date.now()): number {
  if (submittedAt === undefined) {
    return 0;
  }
  return Math.max(0, Math.floor((now - new Date(submittedAt).getTime()) / DAY_MS));
}

/** Oldest age in days of a list of submission times (0 when empty). */
export function oldestAge(submittedAt: (string | undefined)[], now: number = Date.now()): number {
  return submittedAt.reduce((max, s) => Math.max(max, ageInDays(s, now)), 0);
}
