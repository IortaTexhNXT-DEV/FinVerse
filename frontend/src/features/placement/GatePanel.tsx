import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { placementApi } from '@/api/placement';
import type { Evidence, Gate } from '@/api/placement';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';

function ClientConfirmationDialog({
  gate,
  onClose,
  onDone,
}: Readonly<{ gate: Gate; onClose: () => void; onDone: () => void }>) {
  const toast = useToast();
  const [channel, setChannel] = useState('');
  const [remarks, setRemarks] = useState('');
  const direct = gate.rule === 'DIRECT_PAYMENT';
  const confirm = useMutation({
    mutationFn: () =>
      direct
        ? placementApi.confirmDirect(gate.arn, remarks.trim() || undefined)
        : placementApi.confirmClient(gate.arn, channel, remarks.trim() || undefined),
    onSuccess: () => {
      toast.success(`${gate.arn} is ready for placement`);
      onDone();
    },
  });
  return (
    <Modal
      open
      title={direct ? 'Release Direct Payment Account' : 'Record Client Confirmation'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={confirm.isPending}
            disabled={!direct && channel === ''}
            onClick={() => confirm.mutate()}
          >
            {direct ? 'Release Account' : 'Record Confirmation'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={confirm.error} />
        {!direct && (
          <Field
            label="Confirmation channel"
            required
            hint="Attach the confirmation e-mail or form on the account (document type Payment / client confirmation)."
          >
            {(id) => (
              <LovSelect
                id={id}
                type="CLIENT_CONFIRMATION_CHANNEL"
                value={channel}
                onChange={setChannel}
                required
              />
            )}
          </Field>
        )}
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Payment gate of an account (BRD 2.3.1, BRNB.068/114): the rule that applies to its segment and
 * line, whether it is open, and the evidence behind it (matched payments, client confirmation
 * with user and date). Other Lines are confirmed here; paid lines open from payment reports.
 */
export function GatePanel({ gate, onChanged }: Readonly<{ gate: Gate; onChanged: () => void }>) {
  const { can } = useAuth();
  const [confirming, setConfirming] = useState(false);
  const canConfirm =
    can('BILLING_MANAGE') && gate.status === 'AWAITING_PAYMENT' && gate.rule !== 'PAYMENT_MATCHED';
  return (
    <Card
      title="Payment Gate"
      actions={
        canConfirm && (
          <Button variant="primary" onClick={() => setConfirming(true)}>
            {gate.rule === 'DIRECT_PAYMENT'
              ? 'Release Direct Payment'
              : 'Record Client Confirmation'}
          </Button>
        )
      }
    >
      <div className="stack">
        <dl className="detail-list">
          <dt>Rule</dt>
          <dd>
            {humanize(gate.rule)} — {gate.ruleDescription}
          </dd>
          <dt>Gate</dt>
          <dd>
            <StatusBadge status={gate.open ? 'OPEN' : 'PENDING'} /> {humanize(gate.paymentStatus)}
            {gate.paymentSource !== undefined && (
              <span className="muted"> · {gate.paymentSource}</span>
            )}
          </dd>
          <dt>Segment / line</dt>
          <dd>
            {gate.marketSegment ?? '—'} / {humanize(gate.lineCode)}
          </dd>
        </dl>
        {gate.rule === 'PAYMENT_MATCHED' && gate.status === 'AWAITING_PAYMENT' && (
          <p className="muted">
            The premium must be matched as paid: upload the CLPC or payment report on CLPC Billing.
          </p>
        )}
        <DataTable<Evidence>
          caption="Evidence"
          rows={gate.evidence}
          rowKey={(e) => e.id}
          emptyMessage="No evidence recorded yet."
          columns={[
            { key: 'kind', header: 'Evidence', render: (e) => humanize(e.kind) },
            {
              key: 'source',
              header: 'Source',
              render: (e) => `${humanize(e.source)} · ${e.reference}`,
            },
            {
              key: 'detail',
              header: 'Detail',
              render: (e) =>
                [
                  e.channel && humanize(e.channel),
                  e.remarks,
                  e.paidOn && `paid ${formatDate(e.paidOn)}`,
                ]
                  .filter(Boolean)
                  .join(' · '),
            },
            {
              key: 'amount',
              header: 'Amount',
              numeric: true,
              render: (e) => formatAmount(e.amount),
            },
            {
              key: 'by',
              header: 'Recorded',
              render: (e) => `${e.createdBy}, ${formatDateTime(e.createdAt)}`,
            },
            {
              key: 'opened',
              header: 'Opened Gate',
              render: (e) => (e.gateOpened ? <StatusBadge status="CONFIRMED" /> : '—'),
            },
          ]}
        />
      </div>
      {confirming && (
        <ClientConfirmationDialog
          gate={gate}
          onClose={() => setConfirming(false)}
          onDone={() => {
            setConfirming(false);
            onChanged();
          }}
        />
      )}
    </Card>
  );
}
