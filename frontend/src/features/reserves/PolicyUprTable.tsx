import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { reservesApi } from '@/api/reserves';
import type { UprDetail } from '@/api/reserves';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { SelectInput } from '@/features/assets/FormControls';
import { formatDate, humanize } from '@/utils/format';

/** Policy-level UPR of a run (drill-down), paged, optionally for one line of business. */
export function PolicyUprTable({ runId, lines }: Readonly<{ runId: number; lines: string[] }>) {
  const [line, setLine] = useState('');
  const [page, setPage] = useState(0);
  const upr = useQuery({
    queryKey: ['reserve-upr', runId, line, page],
    queryFn: () => reservesApi.upr(runId, line || undefined, page),
  });
  const data = upr.data;
  return (
    <div className="stack">
      <div className="form-grid">
        <SelectInput
          label="Line of business"
          blank="All lines"
          value={line}
          options={lines.map((l) => ({ value: l, label: l }))}
          onChange={(v) => {
            setLine(v);
            setPage(0);
          }}
        />
      </div>
      <ErrorAlert error={upr.error} />
      <DataTable<UprDetail>
        loading={upr.isLoading}
        rows={data?.content ?? []}
        rowKey={(d) => `${d.policyId}|${d.documentNo}`}
        emptyMessage="No unearned premium."
        columns={[
          {
            key: 'd',
            header: 'Policy / endorsement',
            render: (d) => <strong>{d.documentNo}</strong>,
          },
          { key: 'k', header: 'Kind', render: (d) => humanize(d.kind) },
          { key: 'l', header: 'Line', render: (d) => d.businessLine },
          {
            key: 'c',
            header: 'Cover',
            render: (d) => `${formatDate(d.coverFrom)} – ${formatDate(d.coverTo)}`,
          },
          { key: 'b', header: 'Basis', render: (d) => humanize(d.basis) },
          {
            key: 'u',
            header: 'Units (earned / total)',
            numeric: true,
            render: (d) => `${d.earnedUnits} / ${d.totalUnits}`,
          },
          {
            key: 'p',
            header: 'Premium',
            numeric: true,
            render: (d) => <Amount value={d.premium} />,
          },
          { key: 'r', header: 'UPR', numeric: true, render: (d) => <Amount value={d.upr} /> },
          { key: 'ri', header: 'RI UPR', numeric: true, render: (d) => <Amount value={d.riUpr} /> },
          { key: 'dac', header: 'DAC', numeric: true, render: (d) => <Amount value={d.dac} /> },
          { key: 'ucr', header: 'UCR', numeric: true, render: (d) => <Amount value={d.ucr} /> },
        ]}
      />
      {data !== undefined && data.totalPages > 1 && (
        <div className="pagination">
          <span className="muted">
            Page {data.page + 1} of {data.totalPages} · {data.totalElements} transactions
          </span>
          <div className="spacer" />
          <Button
            size="sm"
            variant="secondary"
            disabled={data.page === 0}
            onClick={() => setPage(data.page - 1)}
          >
            Previous
          </Button>
          <Button
            size="sm"
            variant="secondary"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => setPage(data.page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
