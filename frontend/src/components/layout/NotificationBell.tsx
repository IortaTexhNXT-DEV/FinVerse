import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BellDot } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { messagingApi } from '@/api/messaging';
import type { AppNotification } from '@/api/messaging';
import { NotificationItem } from '@/features/workspace/NotificationItem';
import { groupByDay } from '@/features/workspace/notificationLogic';

const REFRESH_MS = 60_000;

/**
 * Header bell with the signed-in user's notifications (BRNB.015): returned requests, work
 * assigned, status changes of the user's records. The panel groups them by day, each with its
 * icon, title, summary, record reference and relative time; opening one marks it read and goes
 * to the record. Mark one or all as read, and View All opens the notifications page.
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const panel = useRef<HTMLDivElement>(null);
  const count = useQuery({
    queryKey: ['notifications', 'count'],
    queryFn: messagingApi.unreadCount,
    refetchInterval: REFRESH_MS,
  });
  const list = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => messagingApi.notifications(false),
    enabled: open,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['notifications'] });
  const read = useMutation({ mutationFn: messagingApi.markRead, onSuccess: refresh });
  const readAll = useMutation({ mutationFn: messagingApi.markAllRead, onSuccess: refresh });

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (e: MouseEvent) => {
      if (panel.current && !panel.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', escape);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', escape);
    };
  }, [open]);

  const unread = count.data?.unread ?? 0;
  const openItem = (n: AppNotification) => {
    if (!n.read) {
      read.mutate(n.id);
    }
    setOpen(false);
    if (n.link) {
      void navigate(n.link);
    }
  };
  const groups = groupByDay(list.data?.content ?? []);

  return (
    <div className="notification-bell" ref={panel}>
      <button
        type="button"
        className="btn btn-ghost btn-sm header-tool"
        aria-label={`Notifications: ${unread} unread`}
        aria-expanded={open}
        title="Notifications"
        onClick={() => setOpen(!open)}
      >
        <BellDot size={18} aria-hidden="true" />
        {unread > 0 && (
          <span className="header-count" aria-hidden="true">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div className="notification-panel" role="dialog" aria-label="Notifications">
          <div className="notification-head">
            <strong>Notifications</strong>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              disabled={unread === 0}
              onClick={() => readAll.mutate()}
            >
              Mark All Read
            </button>
          </div>
          <div className="notification-scroll">
            {list.isLoading && <div className="skeleton-line wide" />}
            {groups.map((g) => (
              <section key={g.label} aria-label={g.label}>
                <h3 className="notification-day">{g.label}</h3>
                <ul className="notification-list">
                  {g.items.map((n) => (
                    <NotificationItem
                      key={n.id}
                      notification={n}
                      onOpen={openItem}
                      onMarkRead={(x) => read.mutate(x.id)}
                    />
                  ))}
                </ul>
              </section>
            ))}
            {list.data?.content.length === 0 && (
              <p className="muted notification-empty">No notifications. You are all caught up.</p>
            )}
          </div>
          <div className="notification-foot">
            <Link to="/notifications" onClick={() => setOpen(false)}>
              View All Notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
