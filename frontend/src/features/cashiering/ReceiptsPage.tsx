import { useQuery } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { ReceiptCriteria, ReceiptSummary } from './cashieringApi';
import { IssueOrDialog } from './IssueOrDialog';
import { ReceiptActionsTab } from './ReceiptActionsTab';
import './cashiering.css';

const TABS = [
  { id: 'receipts', label: 'Receipts' },
  { id: 'actions', label: 'Cancellations and Reinstatements' },
] as const;
type TabId = (typeof TABS)[number]['id'];

type Filters = Omit<ReceiptCriteria, 'companyId'>;

const FILTER_FIELDS: readonly [keyof Filters, string, 'text' | 'number' | 'date'][] = [
  ['clientCode', 'Client Code', 'text'],
  ['invoiceNo', 'Invoice No.', 'text'],
  ['policyNo', 'Policy No.', 'text'],
  ['payor', 'Payor Name', 'text'],
  ['assured', 'Assured Name', 'text'],
  ['insurer', 'Insurer Code', 'text'],
  ['amount', 'Amount', 'number'],
  ['from', 'Date From', 'date'],
  ['to', 'Date To', 'date'],
];

const COLUMNS: Column<ReceiptSummary>[] = [
  {
    key: 'no',
    header: 'Receipt No.',
    render: (r) => (
      <>
        <strong>{r.receiptNo}</strong>
        <span className="cell-sub">
          {r.kind} · {humanize(r.receiptClass)}
        </span>
      </>
    ),
  },
  { key: 'date', header: 'Date', render: (r) => formatDate(r.receiptDate) },
  {
    key: 'payor',
    header: 'Payor / Assured',
    render: (r) => (
      <>
        {r.payorName}
        <span className="cell-sub">{r.assuredName ?? r.payorCode ?? ''}</span>
      </>
    ),
  },
  { key: 'mode', header: 'Mode', render: (r) => humanize(r.mode) },
  { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.amount} /> },
  {
    key: 'applied',
    header: 'Applied',
    numeric: true,
    render: (r) => <Amount value={r.appliedAmount} />,
  },
  {
    key: 'unapplied',
    header: 'Unapplied',
    numeric: true,
    render: (r) => <Amount value={r.unappliedAmount} />,
  },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

function FilterPanel({
  value,
  onChange,
}: Readonly<{ value: Filters; onChange: (f: Filters) => void }>) {
  return (
    <div className="worklist-filters csh-filters">
      {FILTER_FIELDS.map(([key, label, type]) => (
        <TextField
          key={key}
          label={label}
          type={type}
          value={value[key] ?? ''}
          onChange={(v) => onChange({ ...value, [key]: v })}
        />
      ))}
      <CodeSelect
        label="Kind"
        value={value.kind ?? ''}
        options={['AR', 'OR']}
        empty="All"
        labelOf={(c) => c}
        onChange={(v) => onChange({ ...value, kind: v as Filters['kind'] })}
      />
      <CodeSelect
        label="Status"
        value={value.status ?? ''}
        options={['ISSUED', 'CANCELLED', 'REINSTATED']}
        empty="All"
        onChange={(v) => onChange({ ...value, status: v as Filters['status'] })}
      />
    </div>
  );
}

function ReceiptList({ companyId }: Readonly<{ companyId: number }>) {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Filters>({});
  const [applied, setApplied] = useState<Filters>({});
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'receipts', companyId, applied, page],
    queryFn: () => cashieringApi.receipts({ companyId, ...applied }, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  return (
    <>
      <WorklistToolbar
        placeholder="Search Receipt No."
        onSearch={(receiptNo) => {
          setApplied({ ...filters, receiptNo });
          setPage(0);
        }}
        filters={{ open, onToggle: () => setOpen((o) => !o) }}
      />
      {open && <FilterPanel value={filters} onChange={setFilters} />}
      <ErrorAlert error={list.error} />
      {!list.isLoading && rows.length === 0 ? (
        <EmptyState message="No receipts match the search" />
      ) : (
        <DataTable
          caption="Receipts"
          columns={COLUMNS}
          rows={rows}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          onRowClick={(r) => void navigate(`/cashiering/receipts/${r.id}`)}
        />
      )}
      <PageFooter data={list.data} noun="receipts" onPage={setPage} />
    </>
  );
}

/**
 * Receipts (CSHID.010-015): the search over acknowledgement and official receipts with the ten
 * criteria, the Head Office OR issuance, and the cancellation and reinstatement requests waiting
 * for approval.
 */
export default function ReceiptsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('receipts');
  const [issuing, setIssuing] = useState(false);
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Receipts"
        description="Acknowledgement receipts (AR) and Head Office official receipts (OR): search, open, print, cancel and reinstate."
        actions={
          can('CASH_RECEIPT') && (
            <Button
              variant="accent"
              icon={<FilePlus2 size={16} />}
              onClick={() => setIssuing(true)}
            >
              Issue Official Receipt
            </Button>
          )
        }
      />
      <Card flush>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        <div className="stack">
          {tab === 'receipts' ? (
            <ReceiptList companyId={companyId} />
          ) : (
            <ReceiptActionsTab companyId={companyId} />
          )}
        </div>
      </Card>
      {issuing && <IssueOrDialog companyId={companyId} onClose={() => setIssuing(false)} />}
    </div>
  );
}
