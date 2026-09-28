import { useState } from 'react';
import type { ProgrammeInvoice, Soa, SoaInput } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { formatAmount } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { InsurerSelect } from '../common/EbSelects';
import { orNone } from '../common/formValues';
import { useEbMutation } from '../common/useEbMutation';
import { EMPTY_SOA, soaErrors } from './soaLogic';
import { useBaseCurrency } from '@/context/workspaceContext';

/**
 * Receive SOA: an insurer's statement of account for the programme, with the invoices it bills;
 * the same SOA number or file is refused a second time.
 */
export function SoaReceiveDialog({
  programmeId,
  invoices,
  onClose,
}: Readonly<{ programmeId: number; invoices: ProgrammeInvoice[]; onClose: () => void }>) {
  const baseCurrency = useBaseCurrency();
  const [input, setInput] = useState<SoaInput>({ ...EMPTY_SOA, currency: baseCurrency });
  const [chosen, setChosen] = useState<string[]>([]);
  const [file, setFile] = useState<File>();
  const [submitted, setSubmitted] = useState(false);
  const receive = useEbMutation(
    (companyId, v: { input: SoaInput; file: File }) =>
      ebServiceApi.receiveSoa(companyId, programmeId, v.input, chosen, v.file),
    (s: Soa) => `SOA ${s.soaNo} registered`,
    onClose,
  );
  const errors = submitted ? soaErrors(input, file) : {};
  const set = (patch: Partial<SoaInput>) => setInput({ ...input, ...patch });
  const save = () => {
    setSubmitted(true);
    if (file && Object.keys(soaErrors(input, file)).length === 0) {
      receive.mutate({ input: { ...input, receivedOn: orNone(input.receivedOn) }, file });
    }
  };
  const offered = invoices.filter(
    (i) => input.insurerCode === '' || i.insurerCode === input.insurerCode,
  );
  return (
    <Modal
      open
      title="Receive SOA"
      onClose={onClose}
      footer={
        <DialogFooter busy={receive.isPending} label="Register" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={receive.error} />
        <div className="form-grid">
          <Field label="Insurer" required error={errors.insurer}>
            {(id) => (
              <InsurerSelect
                id={id}
                value={input.insurerCode}
                onChange={(v) => set({ insurerCode: v })}
              />
            )}
          </Field>
          <Field label="Insurer's SOA No." required error={errors.soaNo}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={input.insurerSoaNo}
                onChange={(e) => set({ insurerSoaNo: e.target.value })}
              />
            )}
          </Field>
          <Field label="Period From" required error={errors.period}>
            {(id) => (
              <DateInput
                id={id}
                value={input.periodFrom}
                onChange={(e) => set({ periodFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Period To" required>
            {(id) => (
              <DateInput
                id={id}
                min={orNone(input.periodFrom)}
                value={input.periodTo}
                onChange={(e) => set({ periodTo: e.target.value })}
              />
            )}
          </Field>
          <Field label="Amount" required error={errors.amount}>
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="decimal"
                value={input.amount}
                onChange={(e) => set({ amount: e.target.value })}
              />
            )}
          </Field>
          <Field label="Received On" hint="Today when blank">
            {(id) => (
              <DateInput
                id={id}
                value={input.receivedOn ?? ''}
                onChange={(e) => set({ receivedOn: e.target.value })}
              />
            )}
          </Field>
        </div>
        {offered.length > 0 && (
          <fieldset className="eb-group">
            <legend>Invoices Billed</legend>
            <div className="eb-checklist">
              {offered.map((i) => (
                <label key={i.invoiceNo} className="checkbox">
                  <input
                    type="checkbox"
                    checked={chosen.includes(i.invoiceNo)}
                    onChange={(e) =>
                      setChosen(
                        e.target.checked
                          ? [...chosen, i.invoiceNo]
                          : chosen.filter((n) => n !== i.invoiceNo),
                      )
                    }
                  />
                  {i.invoiceNo} – {i.arn} – {formatAmount(i.grossPremium)}
                </label>
              ))}
            </div>
          </fieldset>
        )}
        <Field label="Remarks">
          {(id) => (
            <input
              id={id}
              className="input"
              value={input.remarks ?? ''}
              onChange={(e) => set({ remarks: e.target.value })}
            />
          )}
        </Field>
        <Field label="SOA" required error={errors.file}>
          {(id) => (
            <FileDropZone
              id={id}
              accept=".pdf,.xls,.xlsx"
              maxSizeMb={10}
              onChange={(files) => setFile(files[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
