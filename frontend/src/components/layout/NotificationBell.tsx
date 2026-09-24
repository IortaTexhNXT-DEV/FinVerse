import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BellDot } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messagingApi } from '@/api/messaging';
import type { AppNotification } from '@/api/messaging';
import { formatDateTime } from '@/utils/format';

const REFRESH_MS = 60_000;

/**
 * Header bell with the signed-in user's notifications (BRNB.015): returned requests, work
 * assigned, status changes of the user's records. Opening a notification marks it read and
 * navigates to the record.
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
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
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
              Mark all read
            </button>
          </div>
          <ul className="notification-list">
            {(list.data?.content ?? []).map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className={n.read ? 'notification-item' : 'notification-item unread'}
                  onClick={() => openItem(n)}
                >
                  <span className="notification-title">{n.title}</span>
                  {n.body && <span className="notification-body">{n.body}</span>}
                  <span className="notification-time">{formatDateTime(n.createdAt)}</span>
                </button>
              </li>
            ))}
            {list.data?.content.length === 0 && (
              <li className="muted notification-empty">You are all caught up.</li>
            )}
          </ul>
        </div>
      )}
    </div>
  );
}
