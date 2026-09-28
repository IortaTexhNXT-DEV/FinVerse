import { useQuery } from '@tanstack/react-query';
import { moduleLabel } from '@/utils/businessLabels';
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
import { InsurerName } from '@/components/broking/LovLabel';
import { CellStack } from '@/components/ui/CellStack';
import { PolicyDetails } from './PolicyDetails';

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
        // The reason of a disabled box is on its wrapper: a disabled input shows no tooltip.
        <span
          title={
            lockedElsewhere(i)
              ? `${i.invoiceNo} is locked by ${moduleLabel(i.flags.lockOwner)} until its open transaction is done`
              : undefined
          }
        >
          <input
            type="checkbox"
            aria-label={`Select ${i.invoiceNo}`}
            checked={selected.includes(i.invoiceNo)}
            disabled={lockedElsewhere(i)}
            onChange={() => toggle(i.invoiceNo)}
          />
        </span>
      ),
    },
    {
      key: 'policy',
      header: 'Policy No. / ARN',
      kind: 'code',
      render: (i) => <CellStack main={i.policyNo} sub={i.arn} />,
    },
    {
      key: 'no',
      header: 'Invoice No. / Booked',
      kind: 'code',
      render: (i) => (
        <CellStack main={<strong>{i.invoiceNo}</strong>} sub={formatDate(i.bookingDate)} />
      ),
    },
    {
      key: 'assured',
      header: 'Assured',
      render: (i) => i.assuredName,
    },
    { key: 'insurer', header: 'Insurer', render: (i) => <InsurerName code={i.insurerCode} /> },
    {
      key: 'gross',
      header: 'Gross Premium',
      numeric: true,
      render: (i) => <Amount value={i.grossPremium} />,
    },
    {
      key: 'payment',
      header: 'Payment / Lock',
      kind: 'status',
      render: (i) => (
        <CellStack
          main={<StatusBadge status={i.paymentStatus} />}
          sub={i.flags.lockOwner ? `Locked by ${moduleLabel(i.flags.lockOwner)}` : undefined}
        />
      ),
    },
  ];
}

/** An invoice locked by another team's open transaction cannot be selected. */
function lockedElsewhere(i: { flags: { lockOwner?: string } }): boolean {
  return i.flags.lockOwner !== undefined && i.flags.lockOwner !== 'ADJUSTMENT';
}

/**
 * Step 1 of a new request (ADJID.001/024): the policy first - booked invoices of the Operations
 * ledger with their insurer policy number, searched by policy, ARN, invoice or client; several may
 * be chosen (one request each) and the policy and placement of each chosen invoice are shown before
 * anything is saved. Invoices locked by another module (remittance queue) cannot be chosen.
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
        placeholder="Search policy, ARN, invoice or client"
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
      {selected.length > 0 && <PolicyDetails invoiceNos={selected} />}
    </div>
  );
}
