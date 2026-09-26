import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { MouseEvent } from 'react';
import { taxApi } from '@/api/tax';
import type { ReturnStatus, TaxReturn } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import type { ReturnAction } from './ReturnActionModal';
import { ReturnActionModal } from './ReturnActionModal';

const STATUSES: ('' | ReturnStatus)[] = ['', 'DRAFT', 'FILED', 'PAID', 'CANCELLED'];

/** Returns register: prepare from the calendar or a worksheet, then refresh, file, pay, cancel. */
export default function TaxReturnsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [year, setYear] = useState(() => Number(today().slice(0, 4)));
  const [status, setStatus] = useState<'' | ReturnStatus>('');
  const [action, setAction] = useState<{ kind: ReturnAction; ret: TaxReturn } | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const returns = useQuery({
    queryKey: ['tax-returns', companyId, year, status],
    queryFn: () => taxApi.returns(companyId, year, status === '' ? undefined : status),
    enabled: companyId > 0,
  });
  const refresh = useMutation({
    mutationFn: taxApi.refreshReturn,
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['tax-returns'] });
      toast.success(`Return ${r.returnNo} recomputed`);
    },
  });
  const manage = can('TAX_MANAGE');

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="Tax Returns"
        description="Returns DRAFT → FILED → PAID. Filing needs a second user; payment posts the remittance clearing the tax payable."
      />
      <Card>
        <div className="row">
          <Field label="Year">
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
              />
            )}
          </Field>
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={status}
                onChange={(e) => setStatus(STATUSES.find((s) => s === e.target.value) ?? '')}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === '' ? 'All' : s}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={returns.error ?? refresh.error} />
      <Card flush>
        <DataTable<TaxReturn>
          rows={returns.data ?? []}
          loading={returns.isLoading}
          rowKey={(r) => r.id}
          caption="Tax returns"
          onRowClick={(r) => setDetailId(r.id)}
          columns={[
            { key: 'n', header: 'Return', render: (r) => <strong>{r.returnNo}</strong> },
            { key: 'f', header: 'Form', render: (r) => r.formCode },
            { key: 'p', header: 'Period', render: (r) => r.periodLabel },
            { key: 'd', header: 'Due', render: (r) => formatDate(r.dueDate) },
            {
              key: 't',
              header: 'Tax Due',
              numeric: true,
              render: (r) => <Amount value={r.taxDue} />,
            },
            {
              key: 'c',
              header: 'Credits',
              numeric: true,
              render: (r) => <Amount value={r.taxCredits} />,
            },
            {
              key: 'a',
              header: 'Payable',
              numeric: true,
              render: (r) => <Amount value={r.amountPayable} />,
            },
            {
              key: 's',
              header: 'Status',
              render: (r) => (
                <div className="row">
                  <StatusBadge status={r.status} />
                  {r.overdue && <span className="badge danger">Overdue</span>}
                </div>
              ),
            },
            { key: 'b', header: 'Filed by', render: (r) => r.filedBy ?? '' },
            {
              key: 'x',
              header: 'Actions',
              render: (r) => (manage ? actions(r) : null),
            },
          ]}
        />
      </Card>
      {action !== null && (
        <ReturnActionModal
          action={action.kind}
          taxReturn={action.ret}
          onClose={() => setAction(null)}
        />
      )}
      {detailId !== null && <ReturnDetail id={detailId} onClose={() => setDetailId(null)} />}
    </div>
  );

  function actions(r: TaxReturn) {
    const open = (kind: ReturnAction) => (e: MouseEvent) => {
      e.stopPropagation();
      setAction({ kind, ret: r });
    };
    return (
      <div className="row">
        {r.status === 'DRAFT' && (
          <>
            <Button
              size="sm"
              variant="ghost"
              busy={refresh.isPending && refresh.variables === r.id}
              onClick={(e) => {
                e.stopPropagation();
                refresh.mutate(r.id);
              }}
            >
              Refresh
            </Button>
            <Button size="sm" variant="secondary" onClick={open('FILE')}>
              File
            </Button>
            <Button size="sm" variant="ghost" onClick={open('CANCEL')}>
              Cancel
            </Button>
          </>
        )}
        {r.status === 'FILED' && (
          <Button size="sm" variant="accent" onClick={open('PAY')}>
            Pay
          </Button>
        )}
      </div>
    );
  }
}

function ReturnDetail({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const detail = useQuery({ queryKey: ['tax-return', id], queryFn: () => taxApi.getReturn(id) });
  const r = detail.data;
  return (
    <Modal
      title={r === undefined ? 'Return' : `${r.returnNo} – ${r.formCode} ${r.periodLabel}`}
      open
      onClose={onClose}
    >
      <ErrorAlert error={detail.error} />
      {r !== undefined && (
        <div className="stack">
          <p className="muted">
            Prepared by {r.preparedBy}
            {r.filedBy === undefined
              ? ''
              : `, filed by ${r.filedBy} on ${formatDate(r.filedOn)} (${r.filingReference ?? ''})`}
            {r.statusReason === undefined ? '' : ` – ${r.statusReason}`}
          </p>
          <DataTable
            rows={r.lines}
            rowKey={(l) => l.lineNo}
            caption="Return lines"
            columns={[
              { key: 'd', header: 'Line', render: (l) => l.description },
              {
                key: 'b',
                header: 'Base',
                numeric: true,
                render: (l) => <Amount value={l.baseAmount} />,
              },
              {
                key: 'a',
                header: 'Amount',
                numeric: true,
                render: (l) => <Amount value={l.amount} />,
              },
            ]}
          />
          {r.remittance !== undefined && (
            <p>
              Paid {formatDate(r.remittance.paidOn)} from {r.remittance.bankAccountCode ?? '—'} ref{' '}
              {r.remittance.paymentReference}, journal{' '}
              {r.remittance.journalBatchNo ?? 'none (no tax due)'}
              {r.remittance.late ? ' – late' : ''}
            </p>
          )}
        </div>
      )}
    </Modal>
  );
}
