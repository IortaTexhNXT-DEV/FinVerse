import { useQuery } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import type { ItemAction, ItemFilters, ItemRow } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';
import { ItemDialog, StatusDialog } from './ItemDialogs';
import { ACTION_LABELS, actionsFor } from './pendingLogic';

type Acting =
  { mode: 'edit'; item: ItemRow } | { mode: 'status'; item: ItemRow; action: ItemAction };

function columns(showProgramme: boolean): Column<ItemRow>[] {
  const list: Column<ItemRow>[] = [
    {
      key: 'item',
      header: 'Item',
      render: (i) => (
        <CellStack main={i.subject} sub={<EbLov type={EB_LOV.itemType} code={i.itemType} />} />
      ),
    },
    {
      key: 'member',
      header: 'Member',
      render: (i) => (
        <CellStack main={i.memberRef ?? '—'} sub={i.memberChangeRef ?? i.accountArn ?? undefined} />
      ),
    },
    {
      key: 'party',
      header: 'Responsible',
      render: (i) => <CellStack main={ebLabel(i.responsible)} sub={i.partyCode ?? undefined} />,
    },
    { key: 'due', header: 'Due', kind: 'date', render: (i) => formatDate(i.dueDate) },
    {
      key: 'age',
      header: 'Days Past Due',
      kind: 'center',
      render: (i) => (i.daysPastDue > 0 ? String(i.daysPastDue) : ''),
    },
    {
      key: 'followUps',
      header: 'Follow-ups',
      defaultHidden: true,
      kind: 'center',
      render: (i) =>
        i.escalatedAt ? (
          <Tag tone="danger" title={`Escalated ${formatDate(i.escalatedAt)}`}>
            Escalated
          </Tag>
        ) : (
          String(i.followUpsSent)
        ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (i) => <StatusBadge status={i.status} />,
    },
  ];
  if (showProgramme) {
    list.unshift({
      key: 'programme',
      header: 'Programme',
      kind: 'code',
      render: (i) => (
        <CellStack
          main={
            <Link to={`/eb/programmes/${String(i.programmeId)}?tab=pending`}>{i.programmeNo}</Link>
          }
          sub={i.clientName}
        />
      ),
    });
  }
  return list;
}

/**
 * The tracked items of a filter (FR-EB-057) with their row actions: Edit, Mark Received, Mark
 * Released and Close Item. Used by the Pending Items screen and the programme's Pending Items tab.
 */
export function PendingItemsTable({
  filters,
  showProgramme,
}: Readonly<{ filters: ItemFilters; showProgramme: boolean }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [acting, setActing] = useState<Acting>();
  const close = () => setActing(undefined);
  const items = useQuery({
    queryKey: ['eb', 'items', companyId, filters, page],
    queryFn: () => ebApi.items(companyId, filters, page),
    enabled: companyId > 0,
  });
  const maintain = can('EB_MARKET') || can('EB_PROCESS');
  const cols = columns(showProgramme);
  if (maintain) {
    cols.push({
      key: 'actions',
      header: '',
      render: (i) => (
        <span className="eb-actions">
          {i.status === 'PENDING' && (
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Edit ${i.subject}`}
              icon={<Pencil size={14} />}
              onClick={() => setActing({ mode: 'edit', item: i })}
            />
          )}
          {actionsFor(i.status).map((a) => (
            <Button
              key={a}
              variant="secondary"
              size="sm"
              onClick={() => setActing({ mode: 'status', item: i, action: a })}
            >
              {ACTION_LABELS[a]}
            </Button>
          ))}
        </span>
      ),
    });
  }
  return (
    <>
      <ErrorAlert error={items.error} onRetry={() => void items.refetch()} />
      <DataTable<ItemRow>
        loading={items.isLoading}
        rows={items.data?.content ?? []}
        rowKey={(i) => i.id}
        columns={cols}
        emptyMessage="No pending items to display"
      />
      <PageFooter data={items.data} noun="items" onPage={setPage} />
      {acting?.mode === 'edit' && <ItemDialog item={acting.item} onClose={close} />}
      {acting?.mode === 'status' && (
        <StatusDialog item={acting.item} action={acting.action} onClose={close} />
      )}
    </>
  );
}
