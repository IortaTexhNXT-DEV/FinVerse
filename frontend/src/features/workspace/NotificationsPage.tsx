import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCheck } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { messagingApi } from '@/api/messaging';
import type { AppNotification } from '@/api/messaging';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { FilterChips } from '@/components/ui/FilterChips';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { NotificationItem } from './NotificationItem';
import { groupByDay, kindOf } from './notificationLogic';
import type { NotificationKind } from './notificationLogic';

type View = 'all' | 'unread';

const KINDS: readonly { id: NotificationKind | ''; label: string }[] = [
  { id: '', label: 'All Types' },
  { id: 'assigned', label: 'Assigned to Me' },
  { id: 'returned', label: 'Returned or Rejected' },
  { id: 'approved', label: 'Approved or Completed' },
  { id: 'due', label: 'Due and Reminders' },
  { id: 'info', label: 'Other' },
];

/**
 * All notifications of the signed-in user, grouped by day, with filters kept in the URL (unread
 * only, type, text), mark one or all as read, and a link to each record.
 */
export default function NotificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const view: View = params.get('view') === 'unread' ? 'unread' : 'all';
  const kind = (params.get('type') ?? '') as NotificationKind | '';
  const text = params.get('q') ?? '';
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['notifications', 'page', view, page],
    queryFn: () => messagingApi.notifications(view === 'unread', page),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['notifications'] });
  const read = useMutation({ mutationFn: messagingApi.markRead, onSuccess: refresh });
  const readAll = useMutation({ mutationFn: messagingApi.markAllRead, onSuccess: refresh });

  const set = (key: string, value: string) =>
    setParams(
      (current) => {
        const next = new URLSearchParams(current);
        if (value === '' || (key === 'view' && value === 'all')) {
          next.delete(key);
        } else {
          next.set(key, value);
        }
        return next;
      },
      { replace: true },
    );

  const needle = text.toLowerCase();
  const shown = (list.data?.content ?? []).filter(
    (n) =>
      (kind === '' || kindOf(n) === kind) &&
      (needle === '' || `${n.title} ${n.body ?? ''}`.toLowerCase().includes(needle)),
  );
  const open = (n: AppNotification) => {
    if (!n.read) {
      read.mutate(n.id);
    }
    if (n.link) {
      void navigate(n.link);
    }
  };
  const chips = [
    ...(kind === ''
      ? []
      : [
          {
            key: 'type',
            label: `Type: ${KINDS.find((k) => k.id === kind)?.label ?? kind}`,
            onRemove: () => set('type', ''),
          },
        ]),
    ...(text === '' ? [] : [{ key: 'q', label: `Search: ${text}`, onRemove: () => set('q', '') }]),
  ];

  return (
    <div className="stack">
      <PageHeader
        section="My Work"
        title="Notifications"
        actions={
          <Button
            variant="secondary"
            icon={<CheckCheck size={16} />}
            busy={readAll.isPending}
            onClick={() => readAll.mutate()}
          >
            Mark All Read
          </Button>
        }
      />
      <Card flush>
        <Tabs
          tabs={[
            { id: 'all', label: 'All' },
            { id: 'unread', label: 'Unread' },
          ]}
          active={view}
          onChange={(v) => {
            setPage(0);
            set('view', v);
          }}
        />
        <div className="worklist-toolbar">
          <div className="filter-bar">
            <div className="field">
              <label htmlFor="notification-type">Type</label>
              <select
                id="notification-type"
                className="select"
                value={kind}
                onChange={(e) => set('type', e.target.value)}
              >
                {KINDS.map((k) => (
                  <option key={k.id} value={k.id}>
                    {k.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="notification-text">Search</label>
              <input
                id="notification-text"
                className="input"
                placeholder="Search title or reference"
                value={text}
                onChange={(e) => set('q', e.target.value)}
              />
            </div>
          </div>
          <FilterChips
            filters={chips}
            onClearAll={() => setParams(view === 'unread' ? { view } : {}, { replace: true })}
          />
        </div>
        <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
        <div className="notification-page">
          {list.isLoading && (
            <>
              <div className="skeleton-line wide" />
              <div className="skeleton-line" />
            </>
          )}
          {!list.isLoading && shown.length === 0 && (
            <EmptyState message="No notifications match the filters" />
          )}
          {groupByDay(shown).map((g) => (
            <section key={g.label} aria-label={g.label}>
              <h3 className="notification-day">{g.label}</h3>
              <ul className="notification-list">
                {g.items.map((n) => (
                  <NotificationItem
                    key={n.id}
                    notification={n}
                    onOpen={open}
                    onMarkRead={(x) => read.mutate(x.id)}
                  />
                ))}
              </ul>
            </section>
          ))}
        </div>
        <PageFooter data={list.data} onPage={setPage} />
      </Card>
    </div>
  );
}
