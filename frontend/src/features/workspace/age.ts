import { formatAge } from '@/utils/format';

/** How long an item has been in its stage, in words ("45 min", "5 hrs", "22 days"). */
export function ageText(iso: string, now: number = Date.now()): string {
  return formatAge(iso, now);
}
