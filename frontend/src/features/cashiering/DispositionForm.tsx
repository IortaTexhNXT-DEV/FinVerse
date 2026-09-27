import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { DispositionBody, UnappliedItem } from './cashieringApi';
import { dispositionErrors, dispositionFields } from './cashieringLogic';

const FIELD_LABELS: Record<ReturnType<typeof dispositionFields>[number], string> = {
  targetInvoiceNo: 'Apply to Invoice No.',
  targetClientCode: 'Reclass to Client Code',
  targetUnit: 'Transfer to Marketing Unit',
  payeeName: 'Refund Payee',
};

function initial(item: UnappliedItem): DispositionBody {
  const c = item.current;
  return {
    dispositionType: c?.dispositionType ?? '',
    amount: c?.amount ?? item.balance,
    targetInvoiceNo: c?.targetInvoiceNo,
    targetClientCode: c?.targetClientCode,
    targetUnit: c?.targetUnit,
    payeeName: c?.payeeName ?? item.payorName,
    remarks: c?.remarks,
  };
}

/**
 * The disposition of an unapplied item (CSHID.024): the type picks the fields to fill (invoice
 * to apply to, client to reclass to, unit to transfer to, refund payee), the amount stays within
 * the balance and a reclass or transfer moves the whole balance.
 */
export function DispositionForm({
  item,
  busy,
  onSave,
}: Readonly<{ item: UnappliedItem; busy: boolean; onSave: (body: DispositionBody) => void }>) {
  const types = useQuery({
    queryKey: ['cashiering', 'disposition-types'],
    queryFn: cashieringApi.dispositionTypes,
  });
  const [body, setBody] = useState<DispositionBody>(() => initial(item));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const type = types.data?.find((t) => t.code === body.dispositionType);
  const action = type?.action ?? '';
  const set = (patch: Partial<DispositionBody>) => setBody((b) => ({ ...b, ...patch }));
  const save = () => {
    const found = dispositionErrors(body, action, item.balance);
    if (body.dispositionType === '') {
      found.dispositionType = 'Select the disposition type';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave(body);
    }
  };
  return (
    <Card title={item.current ? 'Update Disposition' : 'Assign Disposition'}>
      <div className="stack">
        <div className="form-grid">
          <CodeSelect
            label="Disposition Type"
            required
            value={body.dispositionType}
            options={(types.data ?? []).map((t) => t.code)}
            empty="Select…"
            error={errors.dispositionType}
            labelOf={(code) => types.data?.find((t) => t.code === code)?.description ?? code}
            onChange={(dispositionType) => set({ dispositionType })}
          />
          <TextField
            label="Amount"
            type="number"
            required
            value={String(body.amount)}
            onChange={(v) => set({ amount: Number(v) })}
            error={errors.amount}
            hint={`Balance ${item.balance.toFixed(2)}`}
          />
          {dispositionFields(action).map((f) => (
            <TextField
              key={f}
              label={FIELD_LABELS[f]}
              required
              value={body[f] ?? ''}
              onChange={(v) => set({ [f]: v })}
              error={errors[f]}
              maxLength={250}
            />
          ))}
          <Field label="Remarks">
            {(id) => (
              <textarea
                id={id}
                className="input"
                rows={2}
                maxLength={250}
                value={body.remarks ?? ''}
                onChange={(e) => set({ remarks: e.target.value })}
              />
            )}
          </Field>
        </div>
        {type?.requiresApproval && (
          <p className="muted">
            This disposition type is approved by a second user before it is processed.
          </p>
        )}
        <div className="row">
          <Button variant="secondary" busy={busy} onClick={save}>
            Save Disposition
          </Button>
        </div>
      </div>
    </Card>
  );
}
