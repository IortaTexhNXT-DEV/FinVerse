import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ebApi } from '@/api/eb';
import type { ItemAction, ItemRow } from '@/api/eb';
import { LovSelect } from '@/components/broking/LovSelect';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { InsurerSelect } from '../common/EbSelects';
import { useEbMutation } from '../common/useEbMutation';
import {
  ACTION_LABELS,
  RESPONSIBLE,
  emptyItem,
  itemForm,
  itemInput,
  validateItem,
} from './pendingLogic';
import type { ItemForm } from './pendingLogic';

function ProgrammeSelect({
  id,
  value,
  onChange,
}: Readonly<{ id: string; value: string; onChange: (v: string) => void }>) {
  const companyId = useCompanyId();
  const programmes = useQuery({
    queryKey: ['eb', 'programmes', companyId, 'pick'],
    queryFn: () => ebApi.programmes(companyId, { tab: 'ALL' }, 0, 200),
    enabled: companyId > 0,
  });
  return (
    <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Select programme</option>
      {(programmes.data?.content ?? []).map((p) => (
        <option key={p.id} value={p.id}>
          {p.programmeNo} · {p.clientName}
        </option>
      ))}
    </select>
  );
}

/** The fields of what is expected: programme and type (new), subject, member, party, due date. */
function ItemFields({
  form,
  errors,
  isNew,
  fixedProgramme,
  set,
}: Readonly<{
  form: ItemForm;
  errors: Record<string, string>;
  isNew: boolean;
  fixedProgramme: boolean;
  set: (key: keyof ItemForm, value: string) => void;
}>) {
  return (
    <div className="form-grid">
      {isNew && !fixedProgramme && (
        <Field label="Programme" required error={errors.programmeId}>
          {(id) => (
            <ProgrammeSelect
              id={id}
              value={form.programmeId}
              onChange={(v) => set('programmeId', v)}
            />
          )}
        </Field>
      )}
      {isNew && (
        <Field label="Item Type" required error={errors.itemType}>
          {(id) => (
            <LovSelect
              id={id}
              type={EB_LOV.itemType}
              value={form.itemType}
              onChange={(v) => set('itemType', v)}
              required
            />
          )}
        </Field>
      )}
      <Field label="Expected Item" required error={errors.subject}>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={200}
            value={form.subject}
            onChange={(e) => set('subject', e.target.value)}
          />
        )}
      </Field>
      <Field label="Member">
        {(id) => (
          <input
            id={id}
            className="input"
            placeholder="Employee no. or name"
            value={form.memberRef}
            onChange={(e) => set('memberRef', e.target.value)}
          />
        )}
      </Field>
      <Field label="Member Change No.">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.memberChangeRef}
            onChange={(e) => set('memberChangeRef', e.target.value)}
          />
        )}
      </Field>
      <Field label="Responsible" required error={errors.responsible}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.responsible}
            onChange={(e) => set('responsible', e.target.value)}
          >
            {RESPONSIBLE.map((r) => (
              <option key={r.code} value={r.code}>
                {r.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      {form.responsible === 'INSURER' && (
        <Field label="Insurer">
          {(id) => (
            <InsurerSelect id={id} value={form.partyCode} onChange={(v) => set('partyCode', v)} />
          )}
        </Field>
      )}
      <Field label="Due Date" required error={errors.dueDate}>
        {(id) => (
          <DateInput
            id={id}
            value={form.dueDate}
            onChange={(e) => set('dueDate', e.target.value)}
          />
        )}
      </Field>
      <Field
        label="Follow-up Recipients"
        error={errors.recipientEmail}
        hint="Comma separated. When empty, follow-ups go to the insurer, the HR contacts or the AO."
      >
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.recipientEmail}
            onChange={(e) => set('recipientEmail', e.target.value)}
          />
        )}
      </Field>
      <Field label="Remarks">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.remarks}
            onChange={(e) => set('remarks', e.target.value)}
          />
        )}
      </Field>
    </div>
  );
}

/** Add Pending Item / Edit Pending Item (FR-EB-057). */
export function ItemDialog({
  item,
  programmeId,
  onClose,
}: Readonly<{ item?: ItemRow; programmeId?: number; onClose: () => void }>) {
  const isNew = item === undefined;
  const [form, setForm] = useState<ItemForm>(isNew ? emptyItem(programmeId) : itemForm(item));
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateItem(form, isNew) : {};
  const save = useEbMutation(
    (companyId, value: ItemForm) =>
      item === undefined
        ? ebApi.openItem(companyId, itemInput(value))
        : ebApi.updateItem(companyId, item.id, itemInput(value)),
    isNew ? 'Pending item added' : 'Pending item saved',
    onClose,
  );
  const submit = () => {
    setSubmitted(true);
    if (Object.keys(validateItem(form, isNew)).length === 0) {
      save.mutate(form);
    }
  };
  return (
    <Modal
      open
      title={isNew ? 'Add Pending Item' : 'Edit Pending Item'}
      onClose={onClose}
      footer={
        <DialogFooter busy={save.isPending} label="Save Item" onClose={onClose} onSave={submit} />
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <ItemFields
          form={form}
          errors={errors}
          isNew={isNew}
          fixedProgramme={programmeId !== undefined}
          set={(key, value) => setForm((f) => ({ ...f, [key]: value }))}
        />
      </div>
    </Modal>
  );
}

/** Mark Received, Mark Released or Close Item with its date and remarks. */
export function StatusDialog({
  item,
  action,
  onClose,
}: Readonly<{ item: ItemRow; action: ItemAction; onClose: () => void }>) {
  const [date, setDate] = useState(today());
  const [receivedOn, setReceivedOn] = useState(today());
  const [remarks, setRemarks] = useState('');
  const needsReceived = action === 'CLOSE' && item.status === 'PENDING';
  const change = useEbMutation<undefined, ItemRow>(
    (companyId: number) =>
      ebApi.changeItem(companyId, item.id, {
        action,
        date: date || undefined,
        receivedOn: needsReceived ? receivedOn : undefined,
        remarks: remarks.trim() || undefined,
      }),
    `${item.subject}: ${ACTION_LABELS[action].toLowerCase()}`,
    onClose,
  );
  const dateError = date === '' && action === 'RECEIVE' ? 'Enter the date received' : undefined;
  return (
    <Modal
      open
      title={`${ACTION_LABELS[action]} – ${item.subject}`}
      onClose={onClose}
      footer={
        <DialogFooter
          busy={change.isPending}
          label={ACTION_LABELS[action]}
          onClose={onClose}
          onSave={() => {
            if (dateError === undefined) {
              change.mutate(undefined);
            }
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={change.error} />
        <div className="form-grid">
          <Field label={action === 'RECEIVE' ? 'Date Received' : 'Date'} required error={dateError}>
            {(id) => (
              <DateInput
                id={id}
                max={today()}
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            )}
          </Field>
          {needsReceived && (
            <Field label="Date Received" required>
              {(id) => (
                <DateInput
                  id={id}
                  max={today()}
                  value={receivedOn}
                  onChange={(e) => setReceivedOn(e.target.value)}
                />
              )}
            </Field>
          )}
          <Field label="Remarks">
            {(id) => (
              <input
                id={id}
                className="input"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}
