import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { configUploadsApi } from '@/api/configUploads';
import type { ConfigUploadJob, ConfigUploadRow } from '@/api/configUploads';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Pager } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { rowAction, rowStatus, rowSummary, rowTabs } from './uploads';
import type { RowFilter } from './uploads';

/**
 * The rows of an upload: what each valid row does (adds or updates a record), and the messages of
 * the rows that are not valid, naming the column.
 */
export function UploadRows({ job }: Readonly<{ job: ConfigUploadJob }>) {
  const [filter, setFilter] = useState<RowFilter>(job.invalidRows > 0 ? 'INVALID' : 'ALL');
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['config-uploads', 'rows', job.id, job.status, filter, page],
    queryFn: () => configUploadsApi.rows(job.id, filter === 'ALL' ? undefined : filter, page),
  });
  const tabs = rowTabs(job);
  return (
    <div className="stack">
      <Tabs
        tabs={tabs}
        active={filter}
        onChange={(f) => {
          setFilter(f);
          setPage(0);
        }}
      />
      <ErrorAlert error={rows.error} />
      <DataTable<ConfigUploadRow>
        callout="config-upload-rows"
        loading={rows.isLoading}
        rows={rows.data?.content ?? []}
        rowKey={(r) => r.rowNo}
        emptyMessage="No row in this view."
        columns={[
          { key: 'row', header: 'Row', numeric: true, width: '70px', render: (r) => r.rowNo },
          { key: 'values', header: 'Record', truncate: true, render: (r) => rowSummary(r) },
          { key: 'action', header: 'Preview', width: '170px', render: (r) => rowAction(r.action) },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            width: '130px',
            render: (r) => {
              const s = rowStatus(r.status);
              return <StatusBadge status={r.status} label={s.label} tone={s.tone} />;
            },
          },
          {
            key: 'messages',
            header: 'Messages',
            render: (r) => r.messages ?? r.resultRef ?? '',
          },
        ]}
      />
      <Pager
        page={rows.data?.page ?? 0}
        totalPages={rows.data?.totalPages ?? 0}
        total={rows.data?.totalElements ?? 0}
        size={rows.data?.size}
        onPage={setPage}
      />
    </div>
  );
}
