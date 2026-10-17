import { Plus, Trash2 } from 'lucide-react';
import { EmptyRow } from '@/components/ui/EmptyRow';
import { useState } from 'react';
import { ebMarketApi } from '@/api/ebMarket';
import type { Proposal, TorItem } from '@/api/ebMarket';
import { LovSelect } from '@/components/broking/LovSelect';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { InsurerSelect } from '../common/EbSelects';
import { useEbMutation } from '../common/useEbMutation';
import {
  BLANK_PLAN,
  blankProposal,
  proposalErrors,
  toProposalInput,
  totalPremium,
} from './proposalForm';
import type { AnswerRow, FactorRow, PlanRow, ProposalForm } from './proposalForm';
import { Notice } from '@/components/ui/Notice';
import { useBaseCurrency } from '@/context/workspaceContext';
import { CurrencySelect } from '@/components/broking/Lookups';

type Setter = (patch: Partial<ProposalForm>) => void;

const PLAN_FIELDS: readonly { key: keyof PlanRow; label: string; numeric?: boolean }[] = [
  { key: 'planCode', label: 'Plan' },
  { key: 'planName', label: 'Plan Name' },
  { key: 'members', label: 'Members', numeric: true },
  { key: 'premiumRate', label: 'Rate', numeric: true },
  { key: 'annualPremium', label: 'Annual Premium', numeric: true },
  { key: 'sumInsured', label: 'Sum Insured', numeric: true },
];

/** The plans offered: benefit line, plan, members, rate, annual premium and sum insured. */
function PlansTable({ form, set }: Readonly<{ form: ProposalForm; set: Setter }>) {
  const update = (index: number, patch: Partial<PlanRow>) =>
    set({ plans: form.plans.map((p, i) => (i === index ? { ...p, ...patch } : p)) });
  return (
    <>
      <table className="table eb-edit-table">
        <thead>
          <tr>
            <th>Benefit Line</th>
            {PLAN_FIELDS.map((f) => (
              <th key={f.key}>{f.label}</th>
            ))}
            <th aria-label="Remove" />
          </tr>
        </thead>
        <tbody>
          {form.plans.map((p, i) => (
            <tr key={i}>
              <td>
                <LovSelect
                  id={`plan-line-${String(i)}`}
                  type={EB_LOV.benefitLine}
                  value={p.benefitLine}
                  onChange={(v) => update(i, { benefitLine: v })}
                />
              </td>
              {PLAN_FIELDS.map((f) => (
                <td key={f.key}>
                  <input
                    className="input"
                    inputMode={f.numeric ? 'decimal' : undefined}
                    aria-label={`${f.label} ${String(i + 1)}`}
                    value={p[f.key]}
                    onChange={(e) => update(i, { [f.key]: e.target.value })}
                  />
                </td>
              ))}
              <td>
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`Remove plan ${String(i + 1)}`}
                  icon={<Trash2 size={14} />}
                  onClick={() => set({ plans: form.plans.filter((_, j) => j !== i) })}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="eb-actions">
        <Button
          variant="secondary"
          size="sm"
          icon={<Plus size={14} />}
          onClick={() => set({ plans: [...form.plans, BLANK_PLAN] })}
        >
          Add Plan
        </Button>
        <span>
          Total annual premium <Amount value={totalPremium(form)} />
        </span>
      </div>
    </>
  );
}

/** The insurer's answer to each released TOR item, with its deviations. */
function AnswersTable({
  form,
  items,
  set,
}: Readonly<{ form: ProposalForm; items: TorItem[]; set: Setter }>) {
  const update = (index: number, patch: Partial<AnswerRow>) =>
    set({ answers: form.answers.map((a, i) => (i === index ? { ...a, ...patch } : a)) });
  return (
    <table className="table eb-edit-table">
      <thead>
        <tr>
          <th>Item</th>
          <th>Requirement</th>
          <th>Offered</th>
          <th>Deviation</th>
          <th>Remark</th>
        </tr>
      </thead>
      <tbody>
        {items.length === 0 && (
          <EmptyRow columns={5} message="No released terms of reference to answer" />
        )}
        {form.answers.map((a, i) => {
          const item = items.find((t) => t.id === a.torItemId);
          return (
            <tr key={a.torItemId}>
              <td>{item?.description}</td>
              <td>{item?.requirement}</td>
              <td>
                <input
                  className="input"
                  aria-label={`Offered ${item?.description ?? ''}`}
                  value={a.offeredValue}
                  onChange={(e) => update(i, { offeredValue: e.target.value })}
                />
              </td>
              <td className="center">
                <input
                  type="checkbox"
                  aria-label={`Deviation ${item?.description ?? ''}`}
                  checked={a.deviation}
                  onChange={(e) => update(i, { deviation: e.target.checked })}
                />
              </td>
              <td>
                <input
                  className="input"
                  aria-label={`Remark ${item?.description ?? ''}`}
                  value={a.remark}
                  onChange={(e) => update(i, { remark: e.target.value })}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

/** The capability factors rated from 1 to 5. */
function FactorsList({ form, set }: Readonly<{ form: ProposalForm; set: Setter }>) {
  const update = (index: number, patch: Partial<FactorRow>) =>
    set({ factors: form.factors.map((f, i) => (i === index ? { ...f, ...patch } : f)) });
  return (
    <div className="stack">
      {form.factors.map((f, i) => (
        <div key={i} className="form-grid">
          <Field label={`Factor ${String(i + 1)}`}>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.capabilityFactor}
                value={f.factorCode}
                onChange={(v) => update(i, { factorCode: v })}
              />
            )}
          </Field>
          <Field label="Rating (1 to 5)">
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={f.rating}
                onChange={(e) => update(i, { rating: e.target.value })}
              />
            )}
          </Field>
          <Field label="Note">
            {(id) => (
              <input
                id={id}
                className="input"
                value={f.value}
                onChange={(e) => update(i, { value: e.target.value })}
              />
            )}
          </Field>
        </div>
      ))}
      <div>
        <Button
          variant="secondary"
          size="sm"
          icon={<Plus size={14} />}
          onClick={() =>
            set({ factors: [...form.factors, { factorCode: '', rating: '', value: '' }] })
          }
        >
          Add Factor
        </Button>
      </div>
    </div>
  );
}

/**
 * Record Proposal: the insurer's proposal as received, with its plans and premiums, the answers
 * to the released TOR, the capability ratings and the proposal document.
 */
export function ProposalDialog({
  cycleId,
  items,
  onClose,
}: Readonly<{ cycleId: number; items: TorItem[]; onClose: () => void }>) {
  const baseCurrency = useBaseCurrency();
  const [form, setForm] = useState<ProposalForm>(() => blankProposal(items, '', baseCurrency));
  const [file, setFile] = useState<File>();
  const [submitted, setSubmitted] = useState(false);
  const record = useEbMutation(
    (companyId, f: ProposalForm) =>
      ebMarketApi.recordProposal(companyId, cycleId, toProposalInput(f), file),
    (p: Proposal) => `Proposal ${p.proposalNo} recorded`,
    onClose,
  );
  const set: Setter = (patch) => setForm((f) => ({ ...f, ...patch }));
  const errors = submitted ? proposalErrors(form, file) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(proposalErrors(form, file)).length === 0) {
      record.mutate(form);
    }
  };
  return (
    <Modal
      open
      title="Record Proposal"
      onClose={onClose}
      footer={
        <DialogFooter busy={record.isPending} label="Record" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={record.error} />
        <div className="form-grid">
          <Field label="Insurer" required error={errors.insurer}>
            {(id) => (
              <InsurerSelect
                id={id}
                value={form.insurerCode}
                onChange={(v) => set({ insurerCode: v })}
              />
            )}
          </Field>
          <Field label="Received On" hint="Today when blank">
            {(id) => (
              <DateInput
                id={id}
                value={form.receivedOn}
                onChange={(e) => set({ receivedOn: e.target.value })}
              />
            )}
          </Field>
          <Field label="Valid Until">
            {(id) => (
              <DateInput
                id={id}
                value={form.validUntil}
                onChange={(e) => set({ validUntil: e.target.value })}
              />
            )}
          </Field>
          <Field label="Currency">
            {(id) => (
              <CurrencySelect
                id={id}
                value={form.currency}
                onChange={(code) => set({ currency: code })}
              />
            )}
          </Field>
        </div>
        <h3 className="eb-subtitle">Plans and Premiums</h3>
        {errors.plans && <Notice tone="error">{errors.plans}</Notice>}
        <PlansTable form={form} set={set} />
        <h3 className="eb-subtitle">Answers to the Terms of Reference</h3>
        <AnswersTable form={form} items={items} set={set} />
        <h3 className="eb-subtitle">Capability</h3>
        {errors.factors && <Notice tone="error">{errors.factors}</Notice>}
        <FactorsList form={form} set={set} />
        <div className="form-grid">
          <Field label="Terms">
            {(id) => (
              <textarea
                id={id}
                className="textarea"
                rows={2}
                value={form.terms}
                onChange={(e) => set({ terms: e.target.value })}
              />
            )}
          </Field>
          <Field label="Exclusions">
            {(id) => (
              <textarea
                id={id}
                className="textarea"
                rows={2}
                value={form.exclusions}
                onChange={(e) => set({ exclusions: e.target.value })}
              />
            )}
          </Field>
        </div>
        <Field label="Proposal Document" required error={errors.file}>
          {(id) => (
            <FileDropZone
              id={id}
              accept=".pdf,.doc,.docx,.xls,.xlsx"
              maxSizeMb={15}
              onChange={(files) => setFile(files[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
