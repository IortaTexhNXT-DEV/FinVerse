import type { AppNotification } from '@/api/messaging';
import { formatDate } from '@/utils/format';

/** Kind of a notification, for its icon. */
export type NotificationKind = 'returned' | 'approved' | 'assigned' | 'due' | 'info';

/** A day group of the notification panel. */
export interface NotificationGroup {
  label: string;
  items: AppNotification[];
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** Local calendar day (yyyy-mm-dd) of a timestamp in Philippine time. */
function dayOf(date: Date): string {
  return new Date(date.getTime() + 8 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

/** Groups notifications, newest first, under Today, Yesterday or their date. */
export function groupByDay(
  list: readonly AppNotification[],
  now = new Date(),
): NotificationGroup[] {
  const today = dayOf(now);
  const yesterday = dayOf(new Date(now.getTime() - DAY_MS));
  const groups: NotificationGroup[] = [];
  const sorted = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  for (const n of sorted) {
    const day = dayOf(new Date(n.createdAt));
    let label = formatDate(day);
    if (day === today) {
      label = 'Today';
    } else if (day === yesterday) {
      label = 'Yesterday';
    }
    const last = groups[groups.length - 1];
    if (last?.label === label) {
      last.items.push(n);
    } else {
      groups.push({ label, items: [n] });
    }
  }
  return groups;
}

/** "Just now", "5 min ago", "3 h ago", "2 days ago", then the date. */
export function relativeTime(iso: string, now = new Date()): string {
  const minutes = Math.floor((now.getTime() - new Date(iso).getTime()) / 60000);
  if (minutes < 1) {
    return 'Just now';
  }
  if (minutes < 60) {
    return `${String(minutes)} min ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${String(hours)} h ago`;
  }
  const days = Math.floor(hours / 24);
  if (days > 6) {
    return formatDate(iso);
  }
  return days === 1 ? '1 day ago' : `${String(days)} days ago`;
}

const REFERENCE = /\b[A-Z]{2,5}(?:-[A-Z]{2,4})?-\d{4}-\d{6}\b/;

/** The record reference named in a notification (ARN-2026-000123, DV-2026-000003...). */
export function referenceOf(n: Pick<AppNotification, 'title' | 'body'>): string | undefined {
  return REFERENCE.exec(`${n.title} ${n.body ?? ''}`)?.[0];
}

/** The kind of a notification, from its title. */
export function kindOf(n: Pick<AppNotification, 'title'>): NotificationKind {
  const t = n.title.toLowerCase();
  if (/return|reject|declin/.test(t)) {
    return 'returned';
  }
  if (/approv|complet|issued|posted|confirm/.test(t)) {
    return 'approved';
  }
  if (/assign|for your|waiting|to review/.test(t)) {
    return 'assigned';
  }
  if (/due|overdue|expir|remind/.test(t)) {
    return 'due';
  }
  return 'info';
}
