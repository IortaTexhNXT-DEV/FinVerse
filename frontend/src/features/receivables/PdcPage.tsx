import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarCheck, History } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { useNavigate } from 'react-router-dom';
import { receivablesApi } from '@/api/receivables';
import type { Pdc, PdcStatus } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize, today } from '@/utils/format';
import { pdcActions } from './receivablesMath';
import { useReceivablesLookups } from './useReceivablesLookups';

const TABS = [
  { id: 'held', label: 'On hand & due' },
  { id: 'due', label: 'Due to be banked' },
  { id: 'all', label: 'Register' },
] as const;
type Tab = (typeof TABS)[number]['id'];

const HELD: PdcStatus[] = ['ON_HAND', 'DUE'];

/**
 * Post-dated cheques received: on hand, due to be banked (deposit converts the cheque into a
 * receipt for approval), clearing, bounce and return, with the status history of each cheque.
 * New PDCs are registered from the receipt entry screen (mode PDC).
 */
export default function PdcPage() {
  const { can } = useAuth();
  const maker = can('RECEIPT_PAYMENT_MAINTAIN');
  const allowed = (status: PdcStatus) => (maker ? pdcActions(status) : []);
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { companyId } = useReceivablesLookups();
  const [tab, setTab] = useState<Tab>('held');
  const [asOf, setAsOf] = useState(today());
  const [historyOf, setHistoryOf] = useState<number | null>(null);
  const [returning, setReturning] = useState<Pdc | null>(null);
  const [reason, setReason] = useState('');

  const list = useQuery({
    queryKey: ['pdcs', companyId],
    queryFn: () => receivablesApi.pdcs(companyId),
    enabled: companyId > 0,
  });
  const history = useQuery({
    queryKey: ['pdc', historyOf],
    queryFn: () => receivablesApi.pdc(historyOf ?? 0),
    enabled: historyOf !== null,
  });
  const action = useMutation({
    mutationFn: (run: () => Promise<{ label: string; receiptId?: number }>) => run(),
    onSuccess: async (result) => {
      setReturning(null);
      await queryClient.invalidateQueries({ queryKey: ['pdcs'] });
      toast.success(result.label);
      if (result.receiptId !== undefined) {
        await navigate(`/receivables/receipts/${result.receiptId}`);
      }
    },
  });
  const all = list.data ?? [];
  const rows = all.filter((p) => {
    if (tab === 'all') {
      return true;
    }
    return HELD.includes(p.status) && (tab === 'held' || p.chequeDate <= asOf);
  });
  const deposit = (p: Pdc) =>
    action.mutate(async () => {
      const receipt = await receivablesApi.depositPdc(
        p.id,
        asOf < p.chequeDate ? p.chequeDate : asOf,
      );
      return {
        label: `Receipt ${receipt.summary.receiptNo} raised for approval`,
        receiptId: receipt.summary.id,
      };
    });
  const clear = (p: Pdc) =>
    action.mutate(async () => {
      const cleared = await receivablesApi.clearPdc(p.id, asOf);
      return { label: `${cleared.pdcNo} cleared` };
    });

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="PDC Received"
        description="Post-dated cheques are memorandum items until banked; banking raises a receipt."
        actions={
          maker && (
            <Button
              variant="accent"
              icon={<CalendarCheck size={16} />}
              busy={action.isPending}
              onClick={() =>
                action.mutate(async () => {
                  const due = await receivablesApi.markDue(companyId, asOf);
                  return { label: `${due.length} cheque(s) marked due` };
                })
              }
            >
              Mark Due as of Date
            </Button>
          )
        }
      />
      <ErrorAlert error={action.error ?? list.error} />
      <Card>
        <div className="form-grid">
          <Field label="As of / banking date">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={asOf}
                onChange={(e) => setAsOf(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Card flush>
        <DataTable<Pdc>
          loading={list.isLoading}
          rows={rows}
          rowKey={(p) => p.id}
          caption="Post-dated cheques"
          columns={[
            { key: 'due', header: 'Due Date', render: (p) => formatDate(p.chequeDate) },
            { key: 'chq', header: 'Cheque No.', render: (p) => p.chequeNo },
            { key: 'party', header: 'Customer', render: (p) => `${p.partyCode} ${p.payerName}` },
            { key: 'drawee', header: 'Drawee Bank', render: (p) => p.draweeBank },
            { key: 'bank', header: 'Deposit to', render: (p) => p.bankAccountCode },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (p) => <Amount value={p.amount} />,
            },
            { key: 'no', header: 'PDC No.', render: (p) => p.pdcNo },
            { key: 'rcv', header: 'Received', render: (p) => formatDate(p.receivedDate) },
            { key: 'st', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
            {
              key: 'act',
              header: 'Actions',
              render: (p) => (
                <div className="row">
                  {allowed(p.status).includes('deposit') && (
                    <Button size="sm" onClick={() => deposit(p)}>
                      Bank
                    </Button>
                  )}
                  {allowed(p.status).includes('clear') && (
                    <Button size="sm" onClick={() => clear(p)}>
                      Cleared
                    </Button>
                  )}
                  {allowed(p.status).includes('return') && (
                    <Button size="sm" variant="ghost" onClick={() => setReturning(p)}>
                      Return
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<History size={14} />}
                    aria-label={`History of ${p.pdcNo}`}
                    onClick={() => setHistoryOf(p.id)}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title="Confirmation Audit Trail"
        open={historyOf !== null}
        onClose={() => setHistoryOf(null)}
      >
        <DataTable
          loading={history.isLoading}
          rows={history.data?.history ?? []}
          rowKey={(e) => `${e.toStatus}-${e.eventDate}-${e.createdBy}`}
          caption="Status history"
          columns={[
            { key: 'date', header: 'Date', render: (e) => formatDate(e.eventDate) },
            {
              key: 'from',
              header: 'From',
              render: (e) => (e.fromStatus === undefined ? '' : humanize(e.fromStatus)),
            },
            { key: 'to', header: 'To', render: (e) => humanize(e.toStatus) },
            { key: 'rcpt', header: 'Receipt', render: (e) => e.receiptNo ?? '' },
            { key: 'rem', header: 'Remarks', render: (e) => e.remarks ?? '' },
            { key: 'by', header: 'User', render: (e) => e.createdBy },
          ]}
        />
      </Modal>
      <Modal
        title={`Return cheque ${returning?.chequeNo ?? ''}`}
        open={returning !== null}
        onClose={() => setReturning(null)}
        footer={
          <Button
            variant="danger"
            disabled={reason.trim() === ''}
            busy={action.isPending}
            onClick={() =>
              action.mutate(async () => {
                const p = await receivablesApi.returnPdc(returning?.id ?? 0, asOf, reason);
                return { label: `${p.pdcNo} returned` };
              })
            }
          >
            Return to Customer
          </Button>
        }
      >
        <Field label="Reason" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </Modal>
    </div>
  );
}
