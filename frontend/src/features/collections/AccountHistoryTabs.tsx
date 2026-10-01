import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { collectionsApi } from './api';
import { validity } from './collectionsLogic';
import { displayNameOf } from '@/api/users';
import { UserName } from '@/components/ui/UserName';
import { LovLabel } from '@/components/broking/LovLabel';
import { changedFieldText, changedValueText, dispositionLabel, handOffLabel } from './presentation';
import type { ValueLookups } from './presentation';
import { useLovLabel } from '@/components/broking/useLabels';
import { useDisplayName } from '@/components/ui/useDisplayName';

/** Dispositions (append-only), efforts and hand-offs to Operations (BRCLXN.016-023). */
export function DispositionsTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const dispositions = useQuery({
    queryKey: ['collections', 'dispositions', invoiceNo],
    queryFn: () => collectionsApi.dispositions(invoiceNo),
  });
  const efforts = useQuery({
    queryKey: ['collections', 'efforts', invoiceNo],
    queryFn: () => collectionsApi.efforts(invoiceNo),
  });
  const rules = useQuery({
    queryKey: ['collections', 'disposition-rules'],
    queryFn: collectionsApi.dispositionRules,
    staleTime: 5 * 60_000,
  });
  const handoffs = useQuery({
    queryKey: ['collections', 'handoffs', invoiceNo],
    queryFn: () => collectionsApi.handoffs(invoiceNo),
  });
  return (
    <div className="stack">
      <ErrorAlert error={dispositions.error ?? efforts.error ?? handoffs.error} />
      <Card title="Dispositions">
        <DataTable
          caption="Dispositions"
          columns={[
            {
              key: 'c',
              header: 'Disposition',
              render: (d) => dispositionLabel(rules.data, d.code),
            },
            { key: 'r', header: 'Remarks', render: (d) => d.remarks ?? '' },
            { key: 'a', header: 'Hand-off', render: (d) => handOffLabel(d.opsAction) },
            {
              key: 'b',
              header: 'Encoded By',
              render: (d) => `${displayNameOf(d.createdBy)} · ${formatDateTime(d.createdAt)}`,
            },
            {
              key: 's',
              header: 'Status',
              render: (d) => (
                <StatusBadge status={d.supersededBy === undefined ? 'CURRENT' : 'SUPERSEDED'} />
              ),
            },
          ]}
          rows={dispositions.data ?? []}
          rowKey={(d) => d.id}
          loading={dispositions.isLoading}
          emptyMessage="No disposition yet"
        />
      </Card>
      <Card title="Collection Efforts">
        <DataTable
          caption="Efforts"
          columns={[
            { key: 'd', header: 'When', render: (e) => formatDateTime(e.at) },
            {
              key: 'c',
              header: 'Effort',
              render: (e) => <LovLabel type="CLX_EFFORT_CODE" code={e.code} />,
            },
            {
              key: 'p',
              header: 'Channel / Contact',
              render: (e) => [e.channel, e.contactPerson].filter(Boolean).join(' · '),
            },
            { key: 'r', header: 'Remarks', render: (e) => e.remarks ?? '' },
            { key: 'b', header: 'Encoded By', render: (e) => <UserName login={e.createdBy} /> },
          ]}
          rows={efforts.data ?? []}
          rowKey={(e) => e.id}
          loading={efforts.isLoading}
          emptyMessage="No effort logged yet"
        />
      </Card>
      <Card title="Hand-offs to Operations">
        <DataTable
          caption="Hand-offs"
          columns={[
            {
              key: 'f',
              header: 'Feed',
              render: (h) => handOffLabel(h.feedCode),
            },
            { key: 'k', header: 'Reference', render: (h) => h.key },
            { key: 'd', header: 'Sent', render: (h) => formatDateTime(h.createdAt) },
            { key: 't', header: 'Taken', render: (h) => formatDateTime(h.takenAt) },
            { key: 's', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
          ]}
          rows={handoffs.data ?? []}
          rowKey={(h) => h.id}
          loading={handoffs.isLoading}
          emptyMessage="Nothing handed to Operations"
        />
      </Card>
    </div>
  );
}

/** Field changes with from / to values, and the assignments (BRCLXN.043, 052). */
export function HistoryTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const [page, setPage] = useState(0);
  const changes = useQuery({
    queryKey: ['collections', 'history', invoiceNo, page],
    queryFn: () => collectionsApi.history(invoiceNo, page),
  });
  const assignments = useQuery({
    queryKey: ['collections', 'assignments', invoiceNo],
    queryFn: () => collectionsApi.assignments(invoiceNo),
  });
  const rules = useQuery({
    queryKey: ['collections', 'disposition-rules'],
    queryFn: collectionsApi.dispositionRules,
    staleTime: 5 * 60_000,
  });
  const effort = useLovLabel('CLX_EFFORT_CODE');
  const name = useDisplayName();
  const look: ValueLookups = {
    name,
    disposition: (code) => dispositionLabel(rules.data, code),
    effort,
  };
  return (
    <div className="stack">
      <ErrorAlert error={changes.error ?? assignments.error} />
      <Card title="Assignments">
        <DataTable
          caption="Assignments"
          columns={[
            { key: 'd', header: 'Date', render: (a) => formatDateTime(a.createdAt) },
            {
              key: 'h',
              header: 'Handler',
              render: (a) =>
                `${displayNameOf(a.previousHandler) || '—'} → ${displayNameOf(a.handler)}`,
            },
            { key: 'k', header: 'Kind', render: (a) => humanize(a.kind) },
            {
              key: 'v',
              header: 'Valid',
              render: (a) => validity(a.validFrom, a.validTo, formatDate),
            },
            { key: 'r', header: 'Reason', render: (a) => a.reason },
            { key: 'b', header: 'By', render: (a) => <UserName login={a.assignedBy} /> },
          ]}
          rows={assignments.data ?? []}
          rowKey={(a) => a.id}
          loading={assignments.isLoading}
        />
      </Card>
      <Card title="Field Changes">
        <DataTable
          caption="Field changes"
          columns={[
            { key: 'd', header: 'Changed', render: (c) => formatDateTime(c.changedAt) },
            { key: 'f', header: 'Field', render: (c) => changedFieldText(c.field) },
            {
              key: 'o',
              header: 'From',
              render: (c) => changedValueText(c.field, c.oldValue, look),
            },
            { key: 'n', header: 'To', render: (c) => changedValueText(c.field, c.newValue, look) },
            {
              key: 'u',
              header: 'User / Source IP',
              render: (c) => (
                <>
                  <UserName login={c.username} empty="" />
                  {c.sourceIp && ` · ${c.sourceIp}`}
                </>
              ),
            },
            { key: 'b', header: 'Bulk Ref.', render: (c) => c.bulkRef ?? '' },
          ]}
          rows={changes.data?.content ?? []}
          rowKey={(c) => c.id}
          loading={changes.isLoading}
        />
        <PageFooter data={changes.data} noun="changes" onPage={setPage} />
      </Card>
    </div>
  );
}
