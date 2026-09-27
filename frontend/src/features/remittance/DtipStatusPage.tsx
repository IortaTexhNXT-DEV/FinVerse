import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
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
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { remittanceApi } from './api';
import type { AccountHit, DtipRow } from './api';
import { dtipFlags, joinParts, TAG_LABELS } from './remittanceLabels';
import './remittance.css';

const TABS = [
  { id: 'dtip', label: 'DTIP Status' },
  { id: 'accounts', label: 'Accounts in Batches' },
] as const;
type TabId = (typeof TABS)[number]['id'];

const STATUSES = [
  'UNPROCESSED',
  'WITH_OUTSTANDING_BALANCE',
  'REVIEW_IN_PROCESS',
  'REQUESTED_FOR_HOLD',
  'APPROVED',
  'PARTIALLY_REMITTED',
  'FULLY_REMITTED',
];

function dtipColumns(queue: ((row: DtipRow) => void) | undefined): Column<DtipRow>[] {
  const columns: Column<DtipRow>[] = [
    {
      key: 'inv',
      header: 'Invoice No.',
      render: (r) => (
        <>
          <Link to={remittanceApi.invoiceLink(r.invoiceNo)}>{r.invoiceNo}</Link>
          <div className="remit-muted">{r.assuredName}</div>
        </>
      ),
    },
    { key: 'ins', header: 'Insurer', render: (r) => r.insurerCode },
    { key: 'pay', header: 'Payment', render: (r) => humanize(r.paymentStatus) },
    { key: 'paid', header: 'Paid AR', numeric: true, render: (r) => <Amount value={r.paidAr} /> },
    { key: 'due', header: 'DTIP', numeric: true, render: (r) => <Amount value={r.dtipDue} /> },
    {
      key: 'rem',
      header: 'Remitted',
      numeric: true,
      render: (r) => <Amount value={r.dtipRemitted} />,
    },
    {
      key: 'bal',
      header: 'Outstanding DTIP',
      numeric: true,
      render: (r) => <Amount value={r.dtipBalance} />,
    },
    {
      key: 'tag',
      header: 'Extraction Tag',
      render: (r) => (r.tag ? joinParts([TAG_LABELS[r.tag], r.reasons], ': ') : ''),
    },
    {
      key: 'flags',
      header: 'Flags',
      render: (r) => (
        <span className="tag-list">
          {dtipFlags(r).map((f) => (
            <span key={f} className="tag">
              {f}
            </span>
          ))}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Remittance Status',
      render: (r) => <StatusBadge status={r.remittanceStatus} />,
    },
  ];
  if (queue !== undefined) {
    columns.push({
      key: 'eod',
      header: 'Actions',
      render: (r) => (
        <Button size="sm" variant="secondary" onClick={() => queue(r)}>
          Queue for End of Day
        </Button>
      ),
    });
  }
  return columns;
}

const ACCOUNT_COLUMNS: Column<AccountHit>[] = [
  {
    key: 'batch',
    header: 'Batch No.',
    render: (a) => <Link to={`/remittance/batches/${a.batchId}`}>{a.batchNo}</Link>,
  },
  { key: 'inv', header: 'Invoice No.', render: (a) => a.line.invoiceNo },
  { key: 'endt', header: 'Endorsement', render: (a) => a.line.endorsementNo ?? '' },
  { key: 'pol', header: 'Policy No.', render: (a) => a.line.policyNo ?? '' },
  { key: 'assured', header: 'Name of Assured', render: (a) => a.line.assuredName },
  {
    key: 'paid',
    header: 'Paid AR',
    numeric: true,
    render: (a) => <Amount value={a.line.amounts.paidAr} />,
  },
  {
    key: 'net',
    header: 'Net Due',
    numeric: true,
    render: (a) => <Amount value={a.line.amounts.netDue} />,
  },
  {
    key: 'stage',
    header: 'Status',
    render: (a) => <StatusBadge status={a.line.exclusion?.excluded ? 'EXCLUDED' : a.stage} />,
  },
];

function DtipTab({ companyId, canQueue }: Readonly<{ companyId: number; canQueue: boolean }>) {
  const toast = useToast();
  const [query, setQuery] = useState('');
  const [insurer, setInsurer] = useState('');
  const [status, setStatus] = useState('');
  const [filters, setFilters] = useState(false);
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['remittance', 'dtip', companyId, query, insurer, status, page],
    queryFn: () => remittanceApi.dtip(companyId, query, insurer, status, page),
    enabled: companyId > 0,
  });
  const queue = useMutation({
    mutationFn: (row: DtipRow) => remittanceApi.queueEod(companyId, row.invoiceNo),
    onSuccess: (r) => toast.success(`${r.invoiceNo} queued for the end-of-day extraction`),
  });
  return (
    <div>
      <ErrorAlert error={rows.error ?? queue.error} />
      <WorklistToolbar
        placeholder="Search Invoice, ARN, Policy or Assured"
        onSearch={(text) => {
          setQuery(text);
          setPage(0);
        }}
        filters={{ open: filters, onToggle: () => setFilters(!filters) }}
      />
      {filters && (
        <div className="worklist-filters remit-form">
          <Field label="Insurer Code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={insurer}
                onChange={(e) => setInsurer(e.target.value)}
              />
            )}
          </Field>
          <Field label="Remittance Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                <option value="">All statuses</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      )}
      <DataTable
        caption="DTIP status"
        columns={dtipColumns(canQueue ? (r) => queue.mutate(r) : undefined)}
        rows={rows.data?.content ?? []}
        rowKey={(r) => r.invoiceNo}
        loading={rows.isLoading}
      />
      <PageFooter data={rows.data} noun="invoices" onPage={setPage} />
    </div>
  );
}

function AccountsTab({ companyId }: Readonly<{ companyId: number }>) {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['remittance', 'accounts', companyId, query, page],
    queryFn: () => remittanceApi.accounts(companyId, query, page),
    enabled: companyId > 0,
  });
  return (
    <div>
      <ErrorAlert error={rows.error} />
      <WorklistToolbar
        placeholder="Search Invoice, Batch, Endorsement, Policy or Assured"
        onSearch={(text) => {
          setQuery(text);
          setPage(0);
        }}
      />
      <DataTable
        caption="Accounts in remittance batches"
        columns={ACCOUNT_COLUMNS}
        rows={rows.data?.content ?? []}
        rowKey={(a) => `${a.batchNo}-${a.line.invoiceNo}`}
        loading={rows.isLoading}
      />
      <PageFooter data={rows.data} noun="accounts" onPage={setPage} />
    </div>
  );
}

/**
 * DTIP status and account search (RMTID.005/024-026/028/032): the due to insurer of each invoice
 * with its payment and remittance status, flags and current extraction tag; the accounts of the
 * remittance batches by invoice, batch, endorsement, policy or assured. Invoices looked up here can
 * be queued for the end-of-day extraction.
 */
export default function DtipStatusPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('dtip');
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="DTIP Status"
        description="What is due to the insurers per invoice, and where each account stands in remittance."
      />
      <Card flush>
        <div>
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          {tab === 'dtip' ? (
            <DtipTab companyId={companyId} canQueue={can('REMIT_PROCESS')} />
          ) : (
            <AccountsTab companyId={companyId} />
          )}
        </div>
      </Card>
    </div>
  );
}
