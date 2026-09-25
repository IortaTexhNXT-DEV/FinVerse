import { useQuery } from '@tanstack/react-query';
import { FolderPlus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CASE_TABS, CASE_TYPE_LABELS, tabParam } from './acsl';
import type { CaseTab } from './acsl';
import { acslApi } from './api';
import type { AcslCase, CaseType, Counts, CaseStage } from './api';
import { NewCaseDialog } from './NewCaseDialog';

const COLUMNS: Column<AcslCase>[] = [
  {
    key: 'no',
    header: 'Case No.',
    render: (c) => (
      <>
        <strong>{c.caseNo}</strong>
        <span className="cell-sub">{formatDate(c.createdAt)}</span>
      </>
    ),
  },
  { key: 'type', header: 'Type', render: (c) => CASE_TYPE_LABELS[c.type] },
  { key: 'subject', header: 'Subject', render: (c) => c.subject },
  {
    key: 'invoice',
    header: 'Invoice / Root',
    render: (c) => (
      <>
        {c.account.invoiceNo ?? '—'}
        <span className="cell-sub">{c.account.rootInvoiceNo ?? ''}</span>
      </>
    ),
  },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (c) => (c.account.amount === undefined ? '—' : <Amount value={c.account.amount} />),
  },
  {
    key: 'from',
    header: 'Requested By',
    render: (c) => (
      <>
        {c.requestedBy}
        <span className="cell-sub">{c.requesterRef ?? ''}</span>
      </>
    ),
  },
  { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.stage} /> },
];

function tabsWithCounts(counts: Counts<CaseStage> | undefined) {
  return CASE_TABS.map((t) => {
    const count = t.id === 'ALL' ? undefined : counts?.[t.id];
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

/**
 * ACSL cases board (ACSL 2.5.0-2.6.2): investigations, analysis requests (the refund validations
 * of Marketing), AR refund applications and payment reversals by stage, searchable by case,
 * subject, invoice or AR.
 */
export default function AcslHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = tabParam<CaseTab>(params.get('stage'), CASE_TABS, 'ALL');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [type, setType] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [opening, setOpening] = useState(false);
  const stage = tab === 'ALL' ? undefined : tab;
  const list = useQuery({
    queryKey: ['acsl', 'cases', companyId, stage, type, q, page],
    queryFn: () =>
      acslApi.cases(companyId, {
        stage,
        q,
        page,
        type: type === '' ? undefined : (type as CaseType),
      }),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['acsl', 'caseCounts', companyId],
    queryFn: () => acslApi.caseCounts(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · ACSL"
        title="ACSL Cases"
        description="Account investigations, analysis requests from Marketing, AR refund applications and payment reversals, from receipt to result."
        actions={
          can('ACSL_PROCESS') && (
            <Button
              variant="primary"
              icon={<FolderPlus size={16} />}
              onClick={() => setOpening(true)}
            >
              Open a Case
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<CaseTab>
            tabs={tabsWithCounts(counts.data)}
            active={tab}
            onChange={(next) => {
              setPage(0);
              setParams(next === 'ALL' ? {} : { stage: next });
            }}
          />
        </div>
        <WorklistToolbar
          placeholder="Search Case No."
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
          filters={{ open: showFilters, onToggle: () => setShowFilters((v) => !v) }}
        />
        {showFilters && (
          <div className="worklist-filters form-grid">
            <Field label="Case Type">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={type}
                  onChange={(e) => setType(e.target.value)}
                >
                  <option value="">All types</option>
                  {(Object.keys(CASE_TYPE_LABELS) as CaseType[]).map((t) => (
                    <option key={t} value={t}>
                      {CASE_TYPE_LABELS[t]}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>
        )}
        <DataTable
          caption="ACSL cases"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(c) => c.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
          onRowClick={(c) => void navigate(`/acsl/cases/${String(c.id)}`)}
        />
        <PageFooter data={list.data} noun="cases" onPage={setPage} />
      </Card>
      {opening && <NewCaseDialog onClose={() => setOpening(false)} />}
    </div>
  );
}
