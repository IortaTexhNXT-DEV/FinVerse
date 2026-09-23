import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { receivablesApi } from '@/api/receivables';
import type { ReceiptFilters, ReceiptSummary } from '@/api/receivables';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';

const STATUSES = ['', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'BOUNCED'];
const MODES = ['', 'CASH', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'PDC'];

/**
 * Official receipts: search for makers (/receivables/receipts) and the approval queue for
 * checkers (/receivables/approvals, pending receipts first).
 */
export default function ReceiptsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const approvals = useLocation().pathname.startsWith('/receivables/approvals');
  const base = approvals ? '/receivables/approvals' : '/receivables/receipts';
  const [filters, setFilters] = useState<Omit<ReceiptFilters, 'companyId'>>({
    page: 0,
    status: approvals ? 'PENDING_APPROVAL' : undefined,
  });
  const query = useQuery({
    queryKey: ['receipts', companyId, filters],
    queryFn: () => receivablesApi.receipts({ ...filters, companyId, size: 25 }),
    enabled: companyId > 0,
  });
  const set = (patch: Partial<ReceiptFilters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = query.data;

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title={approvals ? 'Receipt Approvals' : 'Official Receipts'}
        description="Collections from policyholders, intermediaries, reinsurers and other payers."
        actions={
          !approvals &&
          can('RECEIPT_PAYMENT_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/receivables/receipts/new')}
            >
              New receipt
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.status ?? ''}
                onChange={(e) => set({ status: e.target.value || undefined })}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === '' ? 'All' : humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Mode">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.mode ?? ''}
                onChange={(e) => set({ mode: e.target.value || undefined })}
              >
                {MODES.map((m) => (
                  <option key={m} value={m}>
                    {m === '' ? 'All' : humanize(m)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Receipt no.">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.receiptNo ?? ''}
                onChange={(e) => set({ receiptNo: e.target.value })}
              />
            )}
          </Field>
          <Field label="Party code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.partyCode ?? ''}
                onChange={(e) => set({ partyCode: e.target.value })}
              />
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filters.from ?? ''}
                onChange={(e) => set({ from: e.target.value })}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filters.to ?? ''}
                onChange={(e) => set({ to: e.target.value })}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={query.error} />
      <Card flush>
        <DataTable<ReceiptSummary>
          loading={query.isLoading}
          rows={data?.content ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => void navigate(`${base}/${r.id}`)}
          caption="Receipts"
          columns={[
            { key: 'no', header: 'Receipt no.', render: (r) => <strong>{r.receiptNo}</strong> },
            { key: 'date', header: 'Date', render: (r) => formatDate(r.receiptDate) },
            { key: 'payer', header: 'Payer', render: (r) => `${r.partyCode ?? ''} ${r.payerName}` },
            { key: 'mode', header: 'Mode', render: (r) => humanize(r.mode) },
            { key: 'chq', header: 'Instrument', render: (r) => r.instrumentNo ?? '' },
            { key: 'bank', header: 'Bank', render: (r) => r.bankAccountCode },
            { key: 'ccy', header: 'Ccy', render: (r) => r.currency },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (r) => <Amount value={r.amount} />,
            },
            {
              key: 'onacc',
              header: 'On account',
              numeric: true,
              render: (r) => <Amount value={r.unappliedAmount} />,
            },
            { key: 'by', header: 'Maker', render: (r) => r.createdBy },
            { key: 'st', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'dep', header: 'Deposit', render: (r) => humanize(r.depositStatus) },
          ]}
        />
        {data !== undefined && data.totalPages > 1 && (
          <div className="pagination">
            <span className="muted">
              Page {data.page + 1} of {data.totalPages} · {data.totalElements} receipts
            </span>
            <div className="spacer" />
            <Button
              size="sm"
              variant="secondary"
              disabled={data.page === 0}
              onClick={() => setFilters((f) => ({ ...f, page: data.page - 1 }))}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="secondary"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setFilters((f) => ({ ...f, page: data.page + 1 }))}
            >
              Next
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
}
