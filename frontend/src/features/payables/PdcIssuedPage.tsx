import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { payablesApi } from '@/api/payables';
import type { Pdc, PdcStatus } from '@/api/payables';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { DateReasonModal } from './DateReasonModal';
import { daysBetween } from './payablesMath';
import { usePayablesLookups } from './usePayablesLookups';

type View = 'OUTSTANDING' | 'SETTLED' | 'ALL';
type Action = 'present' | 'clear' | 'cancel' | 'replace';

const VIEWS: { id: View; label: string }[] = [
  { id: 'OUTSTANDING', label: 'Outstanding' },
  { id: 'SETTLED', label: 'Presented / cleared' },
  { id: 'ALL', label: 'All' },
];

const STATUSES: Record<View, PdcStatus[]> = {
  OUTSTANDING: ['ISSUED', 'DUE'],
  SETTLED: ['PRESENTED', 'CLEARED'],
  ALL: [],
};

const TITLES: Record<Action, string> = {
  present: 'Confirm presentation (Dr PDC clearing / Cr bank)',
  clear: 'Cleared on the bank statement',
  cancel: 'Stop cheque and reverse the payment',
  replace: 'Replace the cheque',
};

/** Register of post-dated cheques issued: due list, presentation confirmation and history. */
export default function PdcIssuedPage() {
  const { companyId, bankName } = usePayablesLookups();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [view, setView] = useState<View>('OUTSTANDING');
  const [target, setTarget] = useState<{ pdc: Pdc; action: Action } | null>(null);
  const [history, setHistory] = useState<Pdc | null>(null);

  const list = useQuery({
    queryKey: ['pdc-issued', companyId, view],
    queryFn: () => payablesApi.pdcs(companyId, STATUSES[view]),
    enabled: companyId > 0,
  });
  const events = useQuery({
    queryKey: ['pdc-history', history?.id],
    queryFn: () => payablesApi.pdcHistory(history?.id ?? 0),
    enabled: history !== null,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['pdc-issued'] });
  const due = useMutation({
    mutationFn: () => payablesApi.refreshDue(today()),
    onSuccess: async (n) => {
      await refresh();
      toast.success(`${n} cheque(s) marked due`);
    },
  });
  const act = useMutation({
    mutationFn: ({
      pdc,
      action,
      date,
      reason,
      chequeDate,
    }: { pdc: Pdc; action: Action } & Record<'date' | 'reason' | 'chequeDate', string>) => {
      switch (action) {
        case 'present':
          return payablesApi.presentPdc(pdc.id, date);
        case 'clear':
          return payablesApi.clearPdc(pdc.id, date);
        case 'cancel':
          return payablesApi.cancelPdc(pdc.id, date, reason);
        default:
          return payablesApi.replacePdc(pdc.id, chequeDate, date, reason);
      }
    },
    onSuccess: async (p) => {
      await refresh();
      setTarget(null);
      toast.success(`Cheque ${p.chequeNo}: ${humanize(p.status).toLowerCase()}`);
    },
  });
  const rows = list.data ?? [];
  const outstanding = rows.filter((p) => p.status === 'ISSUED' || p.status === 'DUE');
  const now = today();
  const dueNow = outstanding.filter((p) => p.chequeDate <= now);
  const sum = (ps: Pdc[]) => ps.reduce((a, p) => a + p.baseAmount, 0);

  const actions = (p: Pdc) => {
    const buttons: { action: Action; label: string }[] = [];
    if (p.status === 'ISSUED' || p.status === 'DUE') {
      buttons.push(
        { action: 'present', label: 'Present' },
        { action: 'replace', label: 'Replace' },
        { action: 'cancel', label: 'Cancel' },
      );
    }
    if (p.status === 'PRESENTED') {
      buttons.push({ action: 'clear', label: 'Cleared' });
    }
    return buttons;
  };

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="PDC Issued Register"
        description="Post-dated cheques issued: the liability sits in PDC clearing until the cheque is presented."
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            busy={due.isPending}
            onClick={() => due.mutate()}
          >
            Mark Due Cheques
          </Button>
        }
      />
      <div className="grid-4">
        <Kpi label="Outstanding cheques" value={outstanding.length} />
        <Kpi label="Outstanding amount (LC)" value={<Amount value={sum(outstanding)} />} />
        <Kpi label="Due today or earlier" value={dueNow.length} />
        <Kpi label="Due amount (LC)" accent value={<Amount value={sum(dueNow)} />} />
      </div>
      <Tabs tabs={VIEWS} active={view} onChange={setView} />
      <ErrorAlert error={list.error ?? due.error} />
      <Card flush>
        <DataTable<Pdc>
          loading={list.isLoading}
          rows={rows}
          rowKey={(p) => p.id}
          caption="Post-dated cheques issued"
          onRowClick={(p) => setHistory(p)}
          columns={[
            { key: 'q', header: 'Cheque', render: (p) => <strong>{p.chequeNo}</strong> },
            { key: 'd', header: 'Cheque Date', render: (p) => formatDate(p.chequeDate) },
            {
              key: 'n',
              header: 'Days',
              numeric: true,
              render: (p) => daysBetween(now, p.chequeDate),
            },
            { key: 'p', header: 'Payee', render: (p) => `${p.partyCode} ${p.payeeName}` },
            { key: 'b', header: 'Bank', render: (p) => bankName(p.bankAccountId) },
            { key: 'i', header: 'Issued', render: (p) => formatDate(p.issueDate) },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (p) => <Amount value={p.amount} />,
            },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (p) =>
                can('RECEIPT_PAYMENT_AUTHORIZE') && (
                  <div className="row">
                    {actions(p).map((b) => (
                      <Button
                        key={b.action}
                        size="sm"
                        variant={b.action === 'cancel' ? 'danger' : 'secondary'}
                        onClick={(e) => {
                          e.stopPropagation();
                          setTarget({ pdc: p, action: b.action });
                        }}
                      >
                        {b.label}
                      </Button>
                    ))}
                  </div>
                ),
            },
          ]}
        />
      </Card>
      <DateReasonModal
        title={target === null ? '' : `${TITLES[target.action]} – ${target.pdc.chequeNo}`}
        open={target !== null}
        withDate
        withReason={target?.action === 'cancel' || target?.action === 'replace'}
        withChequeDate={target?.action === 'replace'}
        confirmLabel="Confirm"
        busy={act.isPending}
        error={act.error}
        onClose={() => setTarget(null)}
        onConfirm={(v) => target !== null && act.mutate({ ...target, ...v })}
      />
      <Modal
        title={`Cheque ${history?.chequeNo ?? ''} – status history`}
        open={history !== null}
        onClose={() => setHistory(null)}
      >
        <ErrorAlert error={events.error} />
        <DataTable
          loading={events.isLoading}
          rows={events.data ?? []}
          rowKey={(e) => `${e.createdAt}-${e.toStatus}`}
          columns={[
            { key: 'd', header: 'Date', render: (e) => formatDate(e.eventDate) },
            {
              key: 'f',
              header: 'From',
              render: (e) => (e.fromStatus === undefined ? '' : humanize(e.fromStatus)),
            },
            { key: 't', header: 'To', render: (e) => <StatusBadge status={e.toStatus} /> },
            { key: 'j', header: 'Journal', render: (e) => e.batchNo ?? '' },
            { key: 'r', header: 'Remarks', render: (e) => e.remarks ?? '' },
            {
              key: 'u',
              header: 'By',
              render: (e) => `${e.createdBy} · ${formatDateTime(e.createdAt)}`,
            },
          ]}
        />
      </Modal>
    </div>
  );
}
