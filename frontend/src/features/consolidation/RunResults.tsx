import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { ConsolidationRun, RunLine, TbLine } from '@/api/consolidation';
import { reportApi } from '@/api/reports';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Tabs } from '@/components/ui/Tabs';
import { ReportTable } from '@/features/reports/ReportTable';

type ResultTab = 'tb' | 'elim' | 'bs' | 'pl';

const TABS: readonly { id: ResultTab; label: string }[] = [
  { id: 'tb', label: 'Consolidated trial balance' },
  { id: 'elim', label: 'Eliminations' },
  { id: 'bs', label: 'Balance sheet' },
  { id: 'pl', label: 'Income statement' },
];

interface Props {
  run: ConsolidationRun;
  groupCode: string;
  companyCode: (id?: number) => string;
}

/** Consolidated statement (balance sheet or income statement) rendered from the report engine. */
function Statement({
  code,
  groupCode,
  asOf,
}: Readonly<{ code: string; groupCode: string; asOf: string }>) {
  const statement = useQuery({
    queryKey: ['con-statement', code, groupCode, asOf],
    queryFn: () => reportApi.run(code, { groupCode, asOfDate: asOf }),
  });
  return (
    <>
      <ErrorAlert error={statement.error} />
      {statement.data && <ReportTable result={statement.data} />}
      {statement.data?.notes.map((n) => (
        <p key={n} className="muted">
          {n}
        </p>
      ))}
    </>
  );
}

/** Results of a consolidation run: trial balance, eliminations and statements. */
export function RunResults({ run, groupCode, companyCode }: Readonly<Props>) {
  const [tab, setTab] = useState<ResultTab>('tb');
  const eliminations = (run.lines ?? []).filter((l) => l.type !== 'TRANSLATED');
  return (
    <Card title={`Results · ${run.runNo} · ${run.currency}`}>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'tb' && (
        <DataTable<TbLine>
          rows={run.trialBalance ?? []}
          rowKey={(l) => l.accountCode}
          columns={[
            { key: 'c', header: 'Account', render: (l) => <strong>{l.accountCode}</strong> },
            { key: 'n', header: 'Name', render: (l) => l.accountName },
            { key: 'k', header: 'Class', render: (l) => l.accountClass },
            {
              key: 'a',
              header: 'Aggregated',
              numeric: true,
              render: (l) => <Amount value={l.aggregated} />,
            },
            {
              key: 'e',
              header: 'Eliminations',
              numeric: true,
              render: (l) => <Amount value={l.eliminations} />,
            },
            {
              key: 't',
              header: 'Consolidated',
              numeric: true,
              render: (l) => <Amount value={l.consolidated} />,
            },
          ]}
        />
      )}
      {tab === 'elim' && (
        <DataTable<RunLine>
          rows={eliminations}
          rowKey={(l) => l.lineNo}
          emptyMessage="No eliminations or translation adjustments."
          columns={[
            { key: 'r', header: 'Rule', render: (l) => l.ruleCode ?? 'CTA' },
            { key: 'c', header: 'Company', render: (l) => companyCode(l.companyId) },
            { key: 'a', header: 'Account', render: (l) => `${l.accountCode} ${l.accountName}` },
            { key: 'd', header: 'Description', render: (l) => l.description ?? '' },
            {
              key: 'm',
              header: 'Amount (Dr +)',
              numeric: true,
              render: (l) => <Amount value={l.amount} />,
            },
          ]}
        />
      )}
      {tab === 'bs' && <Statement code="GL-CON-BS" groupCode={groupCode} asOf={run.asOfDate} />}
      {tab === 'pl' && <Statement code="GL-CON-PL" groupCode={groupCode} asOf={run.asOfDate} />}
    </Card>
  );
}
