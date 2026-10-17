import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { TextField } from './CashFields';
import type { DayTotal } from './dayEndLogic';
import { dayTotals } from './dayEndLogic';
import type { ReceiptRecord } from './recordsApi';
import { recordsApi } from './recordsApi';

const TOTAL_COLUMNS: Column<DayTotal>[] = [
  { key: 'group', header: 'Total per', render: (t) => t.group },
  { key: 'label', header: 'Payment Type / Bank Account', render: (t) => t.label },
  { key: 'currency', header: 'Currency', render: (t) => t.currency },
  { key: 'count', header: 'Records', numeric: true, render: (t) => t.count },
  { key: 'amount', header: 'Amount', numeric: true, render: (t) => <Amount value={t.amount} /> },
];

const RECORD_COLUMNS: Column<ReceiptRecord>[] = [
  { key: 'no', header: 'Record Number', render: (r) => <strong>{r.recordNo}</strong> },
  { key: 'type', header: 'Receipt Type', render: (r) => r.receiptTypeLabel ?? r.receiptKind },
  { key: 'receipt', header: 'AR / OR Number', render: (r) => r.receiptNo ?? '' },
  {
    key: 'payor',
    header: 'Payor',
    truncate: true,
    render: (r) => r.party?.payorName ?? r.party?.clientName ?? r.party?.insurerName ?? '',
  },
  { key: 'bank', header: 'Bank Account', render: (r) => r.bankAccountName ?? '' },
  { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.total} /> },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.stage} label={r.statusLabel} />,
  },
];

/**
 * Day-End List (FRS.CSH.02.01.16): the creation records of a day with their status, and the totals
 * per payment type and per bank account for the cashier's reconciliation.
 */
export default function DayEndListPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [date, setDate] = useState('');
  const list = useQuery({
    queryKey: ['cashiering', 'day-end', companyId, date],
    queryFn: () => recordsApi.day(companyId, date || undefined),
    enabled: companyId > 0,
  });
  const records = list.data ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Day-End List"
        description="Records of the day with the totals per payment type and bank account."
      />
      <ErrorAlert error={list.error} />
      <Card>
        <div className="worklist-filters csh-filters">
          <TextField
            label="Date"
            type="date"
            value={date}
            onChange={setDate}
            hint="Today when blank"
          />
        </div>
      </Card>
      <Card title="Totals" flush>
        <DataTable
          caption="Totals of the day"
          columns={TOTAL_COLUMNS}
          rows={dayTotals(records)}
          rowKey={(t) => t.key}
          loading={list.isLoading}
          emptyMessage="No record on this day"
        />
      </Card>
      <Card title="Records of the Day" flush>
        <DataTable
          caption="Records of the day"
          columns={RECORD_COLUMNS}
          rows={records}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          emptyMessage="No record on this day"
          onRowClick={(r) => void navigate(`/cashiering/records/${r.id}`)}
        />
      </Card>
    </div>
  );
}
