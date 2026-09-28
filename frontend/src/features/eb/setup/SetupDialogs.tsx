import { useState } from 'react';
import type {
  RequiredDocument,
  RequiredDocumentInput,
  ThresholdRule,
  ThresholdRuleInput,
} from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { LovSelect } from '@/components/broking/LovSelect';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { orNone } from '../common/formValues';
import { useEbMutation } from '../common/useEbMutation';
import { docInput, ruleErrors, ruleInput } from './setupLogic';
import { Notice } from '@/components/ui/Notice';
import { useBaseCurrency } from '@/context/workspaceContext';

/**
 * Add or edit a value threshold rule: above the amount of total sum insured or annual premium, a
 * comparative needs BDOI Management's approval. The change waits for another user's authorization.
 */
export function ThresholdRuleDialog({
  rule,
  onClose,
}: Readonly<{ rule?: ThresholdRule; onClose: () => void }>) {
  const baseCurrency = useBaseCurrency();
  const [input, setInput] = useState<ThresholdRuleInput>(() => ruleInput(rule, baseCurrency));
  const [submitted, setSubmitted] = useState(false);
  const save = useEbMutation(
    (c, v: ThresholdRuleInput) => ebServiceApi.saveThresholdRule(c, v, rule?.id),
    'Threshold rule saved; it waits for authorization',
    onClose,
  );
  const errors = submitted ? ruleErrors(input) : {};
  const set = (patch: Partial<ThresholdRuleInput>) => setInput({ ...input, ...patch });
  const submit = () => {
    setSubmitted(true);
    if (Object.keys(ruleErrors(input)).length === 0) {
      save.mutate({
        ...input,
        benefitLine: orNone(input.benefitLine),
        effectiveTo: orNone(input.effectiveTo),
        description: orNone(input.description),
      });
    }
  };
  return (
    <Modal
      open
      title={rule ? 'Edit Threshold Rule' : 'Add Threshold Rule'}
      onClose={onClose}
      footer={<DialogFooter busy={save.isPending} label="Save" onClose={onClose} onSave={submit} />}
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="Benefit Line" hint="Every line when blank">
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.benefitLine}
                value={input.benefitLine ?? ''}
                placeholder="All lines"
                onChange={(v) => set({ benefitLine: v })}
              />
            )}
          </Field>
          <Field label="Measure" required error={errors.measure}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={input.measure}
                onChange={(e) => set({ measure: e.target.value as ThresholdRuleInput['measure'] })}
              >
                <option value="">Select</option>
                <option value="TSI">Total sum insured</option>
                <option value="ANNUAL_PREMIUM">Annual premium</option>
              </select>
            )}
          </Field>
          <Field label="Amount" required error={errors.amount}>
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="decimal"
                value={input.amount}
                onChange={(e) =>
                  set({ amount: e.target.value === '' ? '' : Number(e.target.value) })
                }
              />
            )}
          </Field>
          <Field label="Currency">
            {(id) => (
              <input
                id={id}
                className="input"
                value={input.currency ?? ''}
                onChange={(e) => set({ currency: e.target.value })}
              />
            )}
          </Field>
          <Field label="Effective From" required error={errors.effectiveFrom}>
            {(id) => (
              <DateInput
                id={id}
                value={input.effectiveFrom}
                onChange={(e) => set({ effectiveFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Effective To">
            {(id) => (
              <DateInput
                id={id}
                value={input.effectiveTo ?? ''}
                onChange={(e) => set({ effectiveTo: e.target.value })}
              />
            )}
          </Field>
        </div>
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={input.description ?? ''}
              onChange={(e) => set({ description: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Add or edit a required document: the document type a process needs, for every benefit line or
 * one line, mandatory or optional. The change waits for another user's authorization.
 */
export function RequiredDocumentDialog({
  doc,
  onClose,
}: Readonly<{ doc?: RequiredDocument; onClose: () => void }>) {
  const [input, setInput] = useState<RequiredDocumentInput>(() => docInput(doc));
  const [submitted, setSubmitted] = useState(false);
  const save = useEbMutation(
    (c, v: RequiredDocumentInput) => ebServiceApi.saveRequiredDocument(c, v, doc?.id),
    'Required document saved; it waits for authorization',
    onClose,
  );
  const valid = input.processType !== '' && input.documentType !== '';
  const set = (patch: Partial<RequiredDocumentInput>) => setInput({ ...input, ...patch });
  const submit = () => {
    setSubmitted(true);
    if (valid) {
      save.mutate({ ...input, benefitLine: orNone(input.benefitLine) });
    }
  };
  return (
    <Modal
      open
      title={doc ? 'Edit Required Document' : 'Add Required Document'}
      onClose={onClose}
      footer={<DialogFooter busy={save.isPending} label="Save" onClose={onClose} onSave={submit} />}
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        {submitted && !valid && (
          <Notice tone="error">Select the process and the document type</Notice>
        )}
        <div className="form-grid">
          <Field label="Process" required>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.processType}
                value={input.processType}
                onChange={(v) => set({ processType: v })}
              />
            )}
          </Field>
          <Field label="Benefit Line" hint="Every line when blank">
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.benefitLine}
                value={input.benefitLine ?? ''}
                placeholder="All lines"
                onChange={(v) => set({ benefitLine: v })}
              />
            )}
          </Field>
          <Field label="Document Type" required>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.documentType}
                value={input.documentType}
                onChange={(v) => set({ documentType: v })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={input.mandatory}
              onChange={(e) => set({ mandatory: e.target.checked })}
            />
            Mandatory
          </label>
        </div>
      </div>
    </Modal>
  );
}
