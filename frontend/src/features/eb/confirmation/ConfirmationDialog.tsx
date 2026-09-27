import { useState } from 'react';
import type { LineView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { ConfirmationInput, Proposal } from '@/api/ebMarket';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { formatAmount } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { orNone } from '../common/formValues';
import { useEbMutation } from '../common/useEbMutation';
import { confirmationErrors, premiumOn } from './confirmationLogic';

/**
 * Record Confirmation: how and when the client confirmed, the chosen proposal of each active line
 * (the recommendation by default) and the client's e-mail or signed document.
 */
export function ConfirmationDialog({
  cycleId,
  lines,
  proposals,
  recommended,
  onClose,
}: Readonly<{
  cycleId: number;
  lines: LineView[];
  proposals: Proposal[];
  recommended: Record<string, number>;
  onClose: () => void;
}>) {
  const [input, setInput] = useState<ConfirmationInput>(() => ({
    channel: '',
    confirmedOn: '',
    remarks: '',
    choices: lines.flatMap((l) => {
      const proposalId = recommended[l.benefitLine];
      return proposalId === undefined ? [] : [{ lineNo: l.lineNo, proposalId }];
    }),
  }));
  const [file, setFile] = useState<File>();
  const [submitted, setSubmitted] = useState(false);
  const confirm = useEbMutation(
    (companyId, v: ConfirmationInput) => ebMarketApi.confirm(companyId, cycleId, v, file),
    "Client's confirmation recorded",
    onClose,
  );
  const errors = submitted ? confirmationErrors(input, lines, file) : {};
  const choose = (lineNo: number, proposalId: number | undefined) =>
    setInput({
      ...input,
      choices: [
        ...input.choices.filter((c) => c.lineNo !== lineNo),
        ...(proposalId === undefined ? [] : [{ lineNo, proposalId }]),
      ],
    });
  const save = () => {
    setSubmitted(true);
    if (Object.keys(confirmationErrors(input, lines, file)).length === 0) {
      confirm.mutate({
        ...input,
        confirmedOn: orNone(input.confirmedOn),
        remarks: orNone(input.remarks),
      });
    }
  };
  return (
    <Modal
      open
      title="Record Client Confirmation"
      onClose={onClose}
      footer={
        <DialogFooter busy={confirm.isPending} label="Record" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={confirm.error} />
        <div className="form-grid">
          <Field label="Confirmed By" required error={errors.channel}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={input.channel}
                onChange={(e) =>
                  setInput({ ...input, channel: e.target.value as ConfirmationInput['channel'] })
                }
              >
                <option value="">Select</option>
                <option value="EMAIL">E-mail</option>
                <option value="SIGNED_DOCUMENT">Signed document</option>
              </select>
            )}
          </Field>
          <Field label="Confirmed On" hint="Today when blank">
            {(id) => (
              <DateInput
                id={id}
                value={input.confirmedOn ?? ''}
                onChange={(e) => setInput({ ...input, confirmedOn: e.target.value })}
              />
            )}
          </Field>
        </div>
        {errors.choices && <div className="alert danger">{errors.choices}</div>}
        {lines.map((l) => (
          <Field key={l.lineNo} label={`Line ${String(l.lineNo)} – ${l.benefitLine}`} required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={input.choices.find((c) => c.lineNo === l.lineNo)?.proposalId ?? ''}
                onChange={(e) =>
                  choose(l.lineNo, e.target.value ? Number(e.target.value) : undefined)
                }
              >
                <option value="">Select the chosen proposal</option>
                {proposals
                  .filter((p) => p.lines.some((pl) => pl.benefitLine === l.benefitLine))
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.insurerName} – {p.proposalNo} – {formatAmount(premiumOn(p, l.benefitLine))}
                    </option>
                  ))}
              </select>
            )}
          </Field>
        ))}
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              value={input.remarks ?? ''}
              onChange={(e) => setInput({ ...input, remarks: e.target.value })}
            />
          )}
        </Field>
        <Field label="Client's Confirmation" required error={errors.file}>
          {(id) => (
            <FileDropZone
              id={id}
              accept=".pdf,.doc,.docx,.msg,.eml"
              maxSizeMb={10}
              onChange={(files) => setFile(files[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
