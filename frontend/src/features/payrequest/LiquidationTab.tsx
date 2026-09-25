import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FileText, Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, today } from '@/utils/format';
import { payRequestApi } from './api';
import type { LiquidationView, PayRequest } from './api';
import { dayTotal, daysErrors, expenseInputs } from './requestForm';
import type { DayDraft, FieldErrors } from './requestForm';

const EXPENSES = [
  ['perDiem', 'Per Diem'],
  ['representation', 'Representation'],
  ['transport', 'Transportation'],
  ['lodging', 'Lodging'],
  ['others', 'Others'],
] as const;

const emptyDay = (): DayDraft => ({
  fieldworkDate: today(),
  particulars: '',
  perDiem: '',
  representation: '',
  transport: '',
  lodging: '',
  others: '',
});

function daysOf(l: LiquidationView | undefined): DayDraft[] {
  if (!l || l.days.length === 0) {
    return [emptyDay()];
  }
  return l.days.map((d) => ({
    fieldworkDate: d.fieldworkDate,
    particulars: d.particulars,
    perDiem: String(d.perDiem),
    representation: String(d.representation),
    transport: String(d.transport),
    lodging: String(d.lodging),
    others: String(d.others),
  }));
}

function DaysTable({
  days,
  errors,
  editable,
  onChange,
}: Readonly<{
  days: DayDraft[];
  errors: FieldErrors;
  editable: boolean;
  onChange: (days: DayDraft[]) => void;
}>) {
  const edit = (i: number, patch: Partial<DayDraft>) =>
    onChange(days.map((d, j) => (j === i ? { ...d, ...patch } : d)));
  return (
    <div className="table-wrap prq-lines">
      <table className="table">
        <caption className="visually-hidden">Fieldwork days</caption>
        <thead>
          <tr>
            <th>Date</th>
            <th>Particulars</th>
            {EXPENSES.map(([, label]) => (
              <th key={label} className="num">
                {label}
              </th>
            ))}
            <th className="num">Total</th>
            <th aria-label="Actions" />
          </tr>
        </thead>
        <tbody>
          {days.map((d, i) => (
            <tr key={`day-${String(i)}`}>
              <td>
                <input
                  type="date"
                  className="input"
                  aria-label={`Date ${String(i + 1)}`}
                  disabled={!editable}
                  value={d.fieldworkDate}
                  onChange={(e) => edit(i, { fieldworkDate: e.target.value })}
                />
                {errors[`day${String(i)}`] && (
                  <span className="field-error">{errors[`day${String(i)}`]}</span>
                )}
              </td>
              <td>
                <input
                  className="input"
                  aria-label={`Particulars ${String(i + 1)}`}
                  disabled={!editable}
                  value={d.particulars}
                  onChange={(e) => edit(i, { particulars: e.target.value })}
                />
              </td>
              {EXPENSES.map(([key, label]) => (
                <td key={key}>
                  <input
                    className="input"
                    inputMode="decimal"
                    aria-label={`${label} ${String(i + 1)}`}
                    disabled={!editable}
                    value={d[key]}
                    onChange={(e) => edit(i, { [key]: e.target.value })}
                  />
                </td>
              ))}
              <td className="num">{formatAmount(dayTotal(d))}</td>
              <td>
                {editable && (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove day ${String(i + 1)}`}
                    icon={<Trash2 size={14} />}
                    disabled={days.length === 1}
                    onClick={() => onChange(days.filter((_, j) => j !== i))}
                  />
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

type LiquidationStep = 'submit' | 'return' | 'post';

function stepLiquidation(requestId: number, action: LiquidationStep) {
  if (action === 'submit') {
    return payRequestApi.submitLiquidation(requestId);
  }
  return action === 'post'
    ? payRequestApi.postLiquidation(requestId)
    : payRequestApi.returnLiquidation(requestId, 'Returned for correction');
}

/** What the viewer may do: the employee edits a draft, a reviewer checks a submitted one. */
function accessOf(l: LiquidationView | undefined, can: (permission: string) => boolean) {
  return {
    editable: can('PRQ_CREATE') && (l === undefined || l.status === 'DRAFT'),
    reviewer: can('PRQ_REVIEW') && l?.status === 'SUBMITTED',
  };
}

const optional = (text: string) => (text.trim() === '' ? undefined : text.trim());

interface LiquidationButtonsProps {
  editable: boolean;
  reviewer: boolean;
  started: boolean;
  saving: boolean;
  stepping: boolean;
  onAddDay: () => void;
  onSave: () => void;
  onStep: (action: LiquidationStep) => void;
  onDownload: () => void;
}

/** Buttons of the liquidation: the employee's save and submit, the reviewer's return and post. */
function LiquidationButtons({
  editable,
  reviewer,
  started,
  saving,
  stepping,
  onAddDay,
  onSave,
  onStep,
  onDownload,
}: Readonly<LiquidationButtonsProps>) {
  return (
    <div className="worklist-actions">
      {editable && (
        <>
          <Button variant="secondary" icon={<Plus size={16} />} onClick={onAddDay}>
            Add Day
          </Button>
          <Button variant="primary" busy={saving} onClick={onSave}>
            Save Liquidation
          </Button>
          {started && (
            <Button variant="accent" busy={stepping} onClick={() => onStep('submit')}>
              Submit Liquidation
            </Button>
          )}
        </>
      )}
      {reviewer && (
        <>
          <Button variant="secondary" onClick={() => onStep('return')}>
            Return to Employee
          </Button>
          <Button variant="accent" busy={stepping} onClick={() => onStep('post')}>
            Check and Post
          </Button>
        </>
      )}
      {started && (
        <Button variant="secondary" icon={<FileText size={16} />} onClick={onDownload}>
          Liquidation Form
        </Button>
      )}
    </div>
  );
}

/**
 * Liquidation of a disbursed cash advance (Appendix D Cash Advance Liquidation Form, AQ18): the
 * employee enters the fieldwork days and submits; a reviewer who is not the employee returns it or
 * posts it (event PRQ_CA_LIQUIDATION).
 */
export function LiquidationTab({ request }: Readonly<{ request: PayRequest }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const l = request.liquidation;
  const [days, setDays] = useState<DayDraft[]>(() => daysOf(l));
  const [costCenter, setCostCenter] = useState(() => l?.costCenter ?? '');
  const [jobLevel, setJobLevel] = useState(() => l?.jobLevel ?? '');
  const [errors, setErrors] = useState<FieldErrors>({});
  const { editable, reviewer } = accessOf(l, can);
  const done = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
    toast.success(message);
  };
  const save = useMutation({
    mutationFn: () =>
      payRequestApi.saveLiquidation(request.id, {
        jobLevel: optional(jobLevel),
        costCenter: optional(costCenter),
        days: expenseInputs(days),
      }),
    onSuccess: (r) => done(`${r.liquidationNo} saved`),
  });
  const step = useMutation({
    mutationFn: (action: LiquidationStep) => stepLiquidation(request.id, action),
    onSuccess: (r) => done(`${r.liquidationNo} ${r.status.toLowerCase()}`),
  });
  const download = useMutation({
    mutationFn: () => payRequestApi.liquidationForm(request.id),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  const total = days.reduce((sum, d) => sum + dayTotal(d), 0);
  const saveDays = () => {
    const found = daysErrors(days);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <Card
      title={l ? `Liquidation ${l.liquidationNo}` : 'Liquidation'}
      actions={l && <StatusBadge status={l.status} />}
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? step.error ?? download.error} />
        <div className="form-grid">
          <Field label="Job Level">
            {(id) => (
              <input
                id={id}
                className="input"
                disabled={!editable}
                value={jobLevel}
                onChange={(e) => setJobLevel(e.target.value)}
              />
            )}
          </Field>
          <Field label="Cost Centre" hint="Cost centre charged with the expenses">
            {(id) => (
              <input
                id={id}
                className="input"
                disabled={!editable}
                value={costCenter}
                onChange={(e) => setCostCenter(e.target.value)}
              />
            )}
          </Field>
          <Field label="Cash Advanced">{() => <Amount value={request.amount} />}</Field>
          <Field label="Over / (Short)">{() => <Amount value={request.amount - total} />}</Field>
        </div>
        <DaysTable days={days} errors={errors} editable={editable} onChange={setDays} />
        <LiquidationButtons
          editable={editable}
          reviewer={reviewer}
          started={l !== undefined}
          saving={save.isPending}
          stepping={step.isPending}
          onAddDay={() => setDays([...days, emptyDay()])}
          onSave={saveDays}
          onStep={(action) => step.mutate(action)}
          onDownload={() => download.mutate()}
        />
        {l?.journalBatchNo && <p className="muted">Posted as journal {l.journalBatchNo}.</p>}
      </div>
    </Card>
  );
}
