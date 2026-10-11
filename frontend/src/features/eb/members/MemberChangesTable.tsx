import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { MemberChange, MemberChangeFilters } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { ebLabel } from '../common/ebCodes';
import { MemberChangeDetail } from './MemberChangeDetail';

function columns(showProgramme: boolean): Column<MemberChange>[] {
  const all: (Column<MemberChange> | false)[] = [
    { key: 'no', header: 'Change', kind: 'code', render: (c) => c.changeNo },
    showProgramme && {
      key: 'programme',
      header: 'Programme',
      render: (c) => <CellStack main={c.clientName ?? ''} sub={c.programmeNo ?? ''} />,
    },
    {
      key: 'line',
      header: 'Benefit Line',
      render: (c) => <CellStack main={c.benefitLine} sub={`Policy year ${String(c.policyYear)}`} />,
    },
    {
      key: 'lines',
      header: 'Members',
      render: (c) =>
        Object.entries(
          c.lines.reduce<Record<string, number>>(
            (acc, l) => ({ ...acc, [l.action]: (acc[l.action] ?? 0) + 1 }),
            {},
          ),
        )
          .map(([a, n]) => `${ebLabel(a)} ${String(n)}`)
          .join(', '),
    },
    { key: 'source', header: 'Requested By', render: (c) => ebLabel(c.source) },
    {
      key: 'premium',
      header: 'Premium',
      render: (c) => (c.financial ? 'Affects premium' : 'No premium'),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (c) => <StatusBadge status={c.status} />,
    },
    {
      key: 'captured',
      header: 'Captured',
      render: (c) => (
        <CellStack main={formatDateTime(c.createdAt)} sub={<UserName login={c.createdBy} />} />
      ),
    },
  ];
  return all.filter((c): c is Column<MemberChange> => c !== false);
}

/** The member changes matching the filters, paged; a row opens the change. */
export function MemberChangesTable({
  filters,
  showProgramme,
  openId,
}: Readonly<{ filters: MemberChangeFilters; showProgramme: boolean; openId?: number }>) {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<number | undefined>(openId);
  const list = useQuery({
    queryKey: ['eb', 'member-changes', filters, page],
    queryFn: () => ebServiceApi.memberChanges(companyId, filters, page),
  });
  return (
    <>
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <DataTable<MemberChange>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(c) => c.id}
        columns={columns(showProgramme)}
        onRowClick={(c) => setOpen(c.id)}
        emptyMessage="No member changes to display"
      />
      <PageFooter data={list.data} noun="changes" onPage={setPage} />
      {open !== undefined && <MemberChangeDetail id={open} onClose={() => setOpen(undefined)} />}
    </>
  );
}
