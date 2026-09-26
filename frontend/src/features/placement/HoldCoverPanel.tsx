import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { placementApi } from '@/api/placement';
import type { HoldCover } from '@/api/placement';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate } from '@/utils/format';

type Mode = 'request' | 'confirm' | 'decline';

const TITLES: Record<Mode, string> = {
  request: 'Request Hold Cover',
  confirm: 'Record Hold Cover Confirmation',
  decline: 'Record Hold Cover Decline',
};

interface Values {
  date: string;
  reference: string;
  expiry: string;
}

function send(arn: string, mode: Mode, v: Values) {
  const optional = (s: string) => (s.trim() === '' ? undefined : s.trim());
  if (mode === 'request') {
    return placementApi.requestHoldCover(arn, optional(v.date));
  }
  if (mode === 'confirm') {
    return placementApi.confirmHoldCover(arn, {
      reference: v.reference.trim(),
      confirmedOn: optional(v.date),
      expiryDate: optional(v.expiry),
    });
  }
  return placementApi.declineHoldCover(arn, optional(v.reference));
}

function HoldCoverDialog({
  arn,
  mode,
  onClose,
  onDone,
}: Readonly<{ arn: string; mode: Mode; onClose: () => void; onDone: () => void }>) {
  const toast = useToast();
  const [values, setValues] = useState<Values>({ date: '', reference: '', expiry: '' });
  const set = (patch: Partial<Values>) => setValues((v) => ({ ...v, ...patch }));
  const run = useMutation({
    mutationFn: () => send(arn, mode, values),
    onSuccess: (h) => {
      toast.success(`Hold cover of ${arn}: ${h.status.toLowerCase()}`);
      onDone();
    },
  });
  const missingReference = mode === 'confirm' && values.reference.trim() === '';
  return (
    <Modal
      open
      title={TITLES[mode]}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={run.isPending}
            disabled={missingReference}
            onClick={() => run.mutate()}
          >
            {mode === 'request' ? 'Send Request' : 'Record'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} />
        {mode !== 'decline' && (
          <Field
            label={mode === 'request' ? 'Hold cover starts on' : 'Confirmed on'}
            hint="Today when left empty."
          >
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={values.date}
                onChange={(e) => set({ date: e.target.value })}
              />
            )}
          </Field>
        )}
        {mode !== 'request' && (
          <Field label="Insurer reference" required={mode === 'confirm'}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={60}
                value={values.reference}
                onChange={(e) => set({ reference: e.target.value })}
              />
            )}
          </Field>
        )}
        {mode === 'confirm' && (
          <Field label="Expires on" hint="The requested expiry when left empty.">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={values.expiry}
                onChange={(e) => set({ expiry: e.target.value })}
              />
            )}
          </Field>
        )}
        {mode === 'request' && (
          <p className="muted">
            The hold cover request (template) is e-mailed as PDF to the insurer branch placement
            mailbox for the configured number of days.
          </p>
        )}
      </div>
    </Modal>
  );
}

/**
 * Hold cover of an account (BRNB.072/103): request it from the insurer, record the insurer's
 * confirmation (reference and date) or decline, and follow the expiry monitored by the
 * HOLD_COVER_EXPIRY job.
 */
export function HoldCoverPanel({
  arn,
  status,
  holdCovers,
  onChanged,
}: Readonly<{ arn: string; status: string; holdCovers: HoldCover[]; onChanged: () => void }>) {
  const { can } = useAuth();
  const [mode, setMode] = useState<Mode | null>(null);
  const current = holdCovers[0];
  const open = current !== undefined && ['REQUESTED', 'CONFIRMED'].includes(current.status);
  const inPlacement = ['READY_FOR_PLACEMENT', 'PLACED', 'RETURNED_BY_INSURER'].includes(status);
  const record = can('PLACEMENT_MANAGE') || can('ACCOUNT_MAINTAIN');
  return (
    <Card
      title="Hold Cover"
      actions={
        <span className="row">
          {can('PLACEMENT_MANAGE') && inPlacement && !open && (
            <Button variant="primary" onClick={() => setMode('request')}>
              Request Hold Cover
            </Button>
          )}
          {record && (
            <Button variant="secondary" onClick={() => setMode('confirm')}>
              Record Confirmation
            </Button>
          )}
          {record && open && (
            <Button variant="ghost" onClick={() => setMode('decline')}>
              Record Decline
            </Button>
          )}
        </span>
      }
    >
      <DataTable<HoldCover>
        caption="Hold covers"
        rows={holdCovers}
        rowKey={(h) => h.id}
        emptyMessage="No hold cover requested."
        columns={[
          { key: 'status', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
          { key: 'insurer', header: 'Insurer', render: (h) => h.insurerCode },
          {
            key: 'period',
            header: 'Period',
            render: (h) => `${formatDate(h.startDate)} to ${formatDate(h.expiryDate)}`,
          },
          { key: 'ref', header: 'Insurer Reference', render: (h) => h.insurerRef ?? '—' },
          { key: 'confirmed', header: 'Confirmed On', render: (h) => formatDate(h.confirmedOn) },
          { key: 'alerted', header: 'Expiry Alert', render: (h) => formatDate(h.alertedOn) },
        ]}
      />
      {mode && (
        <HoldCoverDialog
          arn={arn}
          mode={mode}
          onClose={() => setMode(null)}
          onDone={() => {
            setMode(null);
            onChanged();
          }}
        />
      )}
    </Card>
  );
}
