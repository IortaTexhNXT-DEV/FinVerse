import { AlarmClock, BellRing, CheckCircle2, Inbox, Undo2 } from 'lucide-react';
import type { AppNotification } from '@/api/messaging';
import { formatDateTime } from '@/utils/format';
import { kindOf, referenceOf, relativeTime } from './notificationLogic';
import type { NotificationKind } from './notificationLogic';
import { readableText } from '@/utils/wording';

const ICONS: Record<NotificationKind, typeof Inbox> = {
  returned: Undo2,
  approved: CheckCircle2,
  assigned: Inbox,
  due: AlarmClock,
  info: BellRing,
};

interface NotificationItemProps {
  notification: AppNotification;
  onOpen: (n: AppNotification) => void;
  onMarkRead: (n: AppNotification) => void;
}

/**
 * One notification: icon, title, one-line summary, record reference chip and relative time;
 * unread ones are highlighted with a Mark Read action. Opening it goes to the record.
 */
export function NotificationItem({
  notification: n,
  onOpen,
  onMarkRead,
}: Readonly<NotificationItemProps>) {
  const Icon = ICONS[kindOf(n)];
  const reference = referenceOf(n);
  return (
    <li className={n.read ? 'notification-row' : 'notification-row unread'}>
      <button type="button" className="notification-item" onClick={() => onOpen(n)}>
        <Icon size={18} aria-hidden="true" className={`notification-icon ${kindOf(n)}`} />
        <span className="notification-text">
          <span className="notification-title">{readableText(n.title)}</span>
          {n.body && (
            <span className="notification-body" title={readableText(n.body)}>
              {readableText(n.body)}
            </span>
          )}
          <span className="notification-meta">
            {reference !== undefined && <code className="notification-ref">{reference}</code>}
            <span className="notification-time" title={formatDateTime(n.createdAt)}>
              {relativeTime(n.createdAt)}
            </span>
          </span>
        </span>
      </button>
      {!n.read && (
        <button
          type="button"
          className="notification-mark"
          aria-label={`Mark read: ${n.title}`}
          title="Mark read"
          onClick={() => onMarkRead(n)}
        >
          <span className="unread-dot" aria-hidden="true" />
        </button>
      )}
    </li>
  );
}
