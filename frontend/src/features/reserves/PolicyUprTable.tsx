import { PeriodCell } from '@/components/ui/PeriodCell';
import { useQuery } from '@tanstack/react-query';
import { PageFooter } from '@/components/ui/Pager';
import { useState } from 'react';
import { reservesApi } from '@/api/reserves';
import type { UprDetail } from '@/api/reserves';
import { Amount } from '@/components/ui/Amount';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { SelectInput } from '@/features/assets/FormControls';
import { humanize } from '@/utils/format';
import { LineLabel } from '@/components/broking/LovLabel';

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
            header: 'Policy / Endorsement',
            render: (d) => <strong>{d.documentNo}</strong>,
          },
          { key: 'k', header: 'Kind', render: (d) => humanize(d.kind) },
          {
            key: 'l',
            header: 'Line',
            render: (d) => (d.businessLine ? <LineLabel code={d.businessLine} /> : ''),
          },
          {
            key: 'c',
            header: 'Cover',
            kind: 'period',
            render: (d) => <PeriodCell from={d.coverFrom} to={d.coverTo} />,
          },
          { key: 'b', header: 'Basis', render: (d) => humanize(d.basis) },
          {
            key: 'u',
            header: 'Units (earned / Total)',
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
      <PageFooter data={data} onPage={(p) => setPage(p)} />
    </div>
  );
}
