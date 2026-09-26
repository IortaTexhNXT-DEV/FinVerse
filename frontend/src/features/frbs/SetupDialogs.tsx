import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { frbsApi } from './api';
import type { RecipientBody, RuleBody, ServiceFeeRecipient, ServiceFeeRule } from './api';
import { ruleErrors } from './serviceFee';

/** Adds or changes a service-fee rate (FRBS 2.10.0; values AQ20). */
export function RuleDialog({
  rule,
  onDone,
  onClose,
}: Readonly<{ rule?: ServiceFeeRule; onDone: () => void; onClose: () => void }>) {
  const [body, setBody] = useState<RuleBody>(
    rule ?? {
      segment: '',
      marketSegments: [],
      rate: 0,
      netOfWtax: true,
      effectiveFrom: new Date().toISOString().slice(0, 10),
      active: true,
    },
  );
  const [markets, setMarkets] = useState(body.marketSegments.join(','));
  const [checked, setChecked] = useState(false);
  const value = {
    ...body,
    marketSegments: markets
      .split(',')
      .map((m) => m.trim())
      .filter(Boolean),
  };
  const errors = checked ? ruleErrors(value) : {};
  const save = useMutation({
    mutationFn: () => (rule ? frbsApi.updateRule(rule.id, value) : frbsApi.createRule(value)),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={rule ? `Change Rate · ${rule.segment}` : 'New Service-Fee Rate'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            onClick={() => {
              setChecked(true);
              if (Object.keys(ruleErrors(value)).length === 0) {
                save.mutate();
              }
            }}
          >
            Save Rate
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="frbs-form">
          <Field label="Service-Fee Segment" required error={errors.segment}>
            {(id) => (
              <LovSelect
                id={id}
                type="SERVICE_FEE_SEGMENT"
                value={body.segment}
                onChange={(v) => setBody({ ...body, segment: v })}
              />
            )}
          </Field>
          <Field
            label="Market Segments"
            required
            error={errors.marketSegments}
            hint="Comma separated, e.g. CBG,RETAIL"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                value={markets}
                onChange={(e) => setMarkets(e.target.value)}
              />
            )}
          </Field>
          <Field label="Rate (%)" required error={errors.rate}>
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.0001"
                className="input"
                value={body.rate}
                onChange={(e) => setBody({ ...body, rate: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Effective From" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={body.effectiveFrom}
                onChange={(e) => setBody({ ...body, effectiveFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Effective To" error={errors.effectiveTo}>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={body.effectiveTo ?? ''}
                onChange={(e) => setBody({ ...body, effectiveTo: e.target.value || undefined })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={body.netOfWtax}
              onChange={(e) => setBody({ ...body, netOfWtax: e.target.checked })}
            />{' '}
            Commission net of withholding tax
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={body.active}
              onChange={(e) => setBody({ ...body, active: e.target.checked })}
            />{' '}
            Active
          </label>
        </div>
      </div>
    </Modal>
  );
}

/** Names the recipient and cost centre of a sales unit (FRBS 2.10.0; recipients AQ20). */
export function RecipientDialog({
  recipient,
  onDone,
  onClose,
}: Readonly<{ recipient?: ServiceFeeRecipient; onDone: () => void; onClose: () => void }>) {
  const companyId = useCompanyId();
  const [body, setBody] = useState<RecipientBody>(
    recipient ?? { salesUnit: '', payeeCode: '', payeeName: '', active: true },
  );
  const [checked, setChecked] = useState(false);
  const missing = (v: string) => (checked && v.trim() === '' ? 'Required' : undefined);
  const complete = [body.salesUnit, body.payeeCode, body.payeeName].every((v) => v.trim() !== '');
  const save = useMutation({
    mutationFn: () =>
      recipient
        ? frbsApi.updateRecipient(recipient.id, body)
        : frbsApi.createRecipient(companyId, body),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={recipient ? `Change Recipient · ${recipient.salesUnit}` : 'New Recipient'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            onClick={() => {
              setChecked(true);
              if (complete) {
                save.mutate();
              }
            }}
          >
            Save Recipient
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="frbs-form">
          <Field label="Sales Unit" required error={missing(body.salesUnit)}>
            {(id) => (
              <input
                id={id}
                className="input"
                disabled={recipient !== undefined}
                maxLength={20}
                value={body.salesUnit}
                onChange={(e) => setBody({ ...body, salesUnit: e.target.value })}
              />
            )}
          </Field>
          <Field
            label="Payee Code"
            required
            error={missing(body.payeeCode)}
            hint="Payee of the Disbursement master"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                value={body.payeeCode}
                onChange={(e) => setBody({ ...body, payeeCode: e.target.value })}
              />
            )}
          </Field>
          <Field label="Payee Name" required error={missing(body.payeeName)}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={250}
                value={body.payeeName}
                onChange={(e) => setBody({ ...body, payeeName: e.target.value })}
              />
            )}
          </Field>
          <Field label="Cost Centre" hint="Blank: the unit's cost centre or the cost-centre rules">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={20}
                value={body.costCenter ?? ''}
                onChange={(e) => setBody({ ...body, costCenter: e.target.value || undefined })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={body.active}
              onChange={(e) => setBody({ ...body, active: e.target.checked })}
            />{' '}
            Active
          </label>
        </div>
      </div>
    </Modal>
  );
}
