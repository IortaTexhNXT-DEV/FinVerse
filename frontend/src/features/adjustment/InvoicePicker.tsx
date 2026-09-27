import { useQuery } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { OpsInvoiceSummary } from '@/api/operations';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

interface InvoicePickerProps {
  selected: string[];
  onChange: (invoiceNos: string[]) => void;
}

function columns(selected: string[], toggle: (no: string) => void): Column<OpsInvoiceSummary>[] {
  return [
    {
      key: 'select',
      width: '44px',
      header: <span className="visually-hidden">Select</span>,
      render: (i) => (
        <input
          type="checkbox"
          aria-label={`Select ${i.invoiceNo}`}
          checked={selected.includes(i.invoiceNo)}
          disabled={i.flags.lockOwner !== undefined && i.flags.lockOwner !== 'ADJUSTMENT'}
          onChange={() => toggle(i.invoiceNo)}
        />
      ),
    },
    {
      key: 'no',
      header: 'Invoice No. / ARN',
      render: (i) => (
        <>
          <strong>{i.invoiceNo}</strong>
          <div className="muted">{i.arn}</div>
        </>
      ),
    },
    { key: 'assured', header: 'Assured', render: (i) => i.assuredName },
    { key: 'booked', header: 'Booked', render: (i) => formatDate(i.bookingDate) },
    {
      key: 'gross',
      header: 'Gross Premium',
      numeric: true,
      render: (i) => <Amount value={i.grossPremium} />,
    },
    { key: 'payment', header: 'Payment', render: (i) => <StatusBadge status={i.paymentStatus} /> },
    {
      key: 'lock',
      header: 'Lock',
      render: (i) => (i.flags.lockOwner ? <span className="tag">{i.flags.lockOwner}</span> : '—'),
    },
  ];
}

/**
 * Step 1 of a new request (ADJID.001/024): booked invoices of the Operations ledger, searched by
 * invoice, ARN, policy or client; several may be chosen (one request each). Invoices locked by
 * another module (remittance queue) cannot be chosen.
 */
export function InvoicePicker({ selected, onChange }: Readonly<InvoicePickerProps>) {
  const companyId = useCompanyId();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['adjustment', 'invoices', companyId, q, page],
    queryFn: () => opsApi.invoices(companyId, { q: q || undefined }, page),
    enabled: companyId > 0,
  });
  const toggle = (no: string) =>
    onChange(selected.includes(no) ? selected.filter((s) => s !== no) : [...selected, no]);
  const rows = (list.data?.content ?? []).filter(
    (i) => !i.kind.includes('MINUS') && i.kind !== 'CANCELLATION',
  );
  return (
    <div className="stack">
      <WorklistToolbar
        placeholder="Search Invoice No., ARN, policy or client"
        onSearch={(text) => {
          setQ(text);
          setPage(0);
        }}
      />
      {selected.length > 0 && (
        <div className="adj-selected" aria-label="Invoices chosen">
          {selected.map((no) => (
            <span key={no} className="tag">
              {no}{' '}
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                aria-label={`Remove ${no}`}
                onClick={() => toggle(no)}
              >
                <X size={12} aria-hidden="true" />
              </button>
            </span>
          ))}
        </div>
      )}
      <ErrorAlert error={list.error} />
      <DataTable
        caption="Booked invoices"
        columns={columns(selected, toggle)}
        rows={rows}
        rowKey={(i) => i.invoiceNo}
        loading={list.isLoading}
        emptyMessage="No items to display"
      />
      <PageFooter data={list.data} noun="invoices" onPage={setPage} />
    </div>
  );
}
