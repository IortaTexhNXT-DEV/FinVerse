import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { InsurerChoice, PackageRequest } from '@/api/productmaint';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { InsurerChoices } from '@/features/proposals/ProposalFormParts';
import { offered } from './packageRequest';

interface DialogProps {
  request: PackageRequest;
  onClose: () => void;
  onDone: (label: string) => void;
}

function Dialog({
  title,
  confirm,
  busy,
  disabled = false,
  error,
  onClose,
  onConfirm,
  children,
}: Readonly<{
  title: string;
  confirm: string;
  busy: boolean;
  disabled?: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: () => void;
  children: ReactNode;
}>) {
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={busy} disabled={disabled} onClick={onConfirm}>
            {confirm}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {children}
      </div>
    </Modal>
  );
}

function CommentBox({
  value,
  onChange,
}: Readonly<{ value: string; onChange: (v: string) => void }>) {
  return (
    <Field label="Comment">
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={3}
          maxLength={1000}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/** TSU Team Lead recommendation, shown to the TSU Head (BRPM.009). */
export function RecommendDialog({ request, onClose, onDone }: Readonly<DialogProps>) {
  const [text, setText] = useState('');
  const run = useMutation({
    mutationFn: () => productMaintApi.recommend(request.id, text.trim()),
    onSuccess: () => onDone('Recommended'),
  });
  return (
    <Dialog
      title="Recommend for Approval"
      confirm="Recommend"
      busy={run.isPending}
      disabled={text.trim() === ''}
      error={run.error}
      onClose={onClose}
      onConfirm={() => run.mutate()}
    >
      <Field
        label="Recommendation"
        required
        hint="Visible to the TSU Head and kept on the request."
      >
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={4}
            maxLength={2000}
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
        )}
      </Field>
    </Dialog>
  );
}

/** Opens the next negotiation round with the chosen insurers (PMADD04). */
export function ReviseDialog({ request, onClose, onDone }: Readonly<DialogProps>) {
  const rounds = useQuery({
    queryKey: ['package-request-tab', request.id, 'rounds'],
    queryFn: () => productMaintApi.rounds(request.id),
  });
  const latest = rounds.data?.at(-1);
  const defaults = (latest?.responses ?? [])
    .filter((r) => r.outcome !== 'DECLINED')
    .map((r) => r.insurerCode);
  const [insurers, setInsurers] = useState<string[] | null>(null);
  const [notes, setNotes] = useState('');
  const chosen = insurers ?? defaults;
  const run = useMutation({
    mutationFn: () => productMaintApi.revise(request.id, chosen, notes.trim() || undefined),
    onSuccess: (r) => onDone(`Round ${r.roundNo} opened`),
  });
  return (
    <Dialog
      title="Revise Quotation Slip"
      confirm="Open New Round"
      busy={run.isPending}
      disabled={chosen.length === 0}
      error={rounds.error ?? run.error}
      onClose={onClose}
      onConfirm={() => run.mutate()}
    >
      <p className="muted">
        Round {latest?.roundNo ?? 1} is closed and a new slip is prepared for the insurers below.
      </p>
      <InsurerChoices selected={chosen} onChange={setInsurers} />
      <Field label="What changes in this round">
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={3}
            maxLength={4000}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        )}
      </Field>
    </Dialog>
  );
}

const ROLES = [
  { value: 'PANEL', label: 'Panel' },
  { value: 'LEAD', label: 'Lead' },
  { value: 'PARTICIPANT', label: 'Participant' },
];

/** Terms final: the insurers whose terms the package takes, with role and share (BRPM.010). */
export function TermsFinalDialog({ request, onClose, onDone }: Readonly<DialogProps>) {
  const rounds = useQuery({
    queryKey: ['package-request-tab', request.id, 'rounds'],
    queryFn: () => productMaintApi.rounds(request.id),
  });
  const offers = (rounds.data?.at(-1)?.responses ?? []).filter((r) => offered(r.outcome));
  const [choices, setChoices] = useState<Record<string, InsurerChoice>>({});
  const [comment, setComment] = useState('');
  const toggle = (code: string) =>
    setChoices(
      code in choices
        ? Object.fromEntries(Object.entries(choices).filter(([k]) => k !== code))
        : { ...choices, [code]: { insurerCode: code, role: 'PANEL' } },
    );
  const patch = (code: string, p: Partial<InsurerChoice>) =>
    setChoices({ ...choices, [code]: { ...(choices[code] ?? { insurerCode: code }), ...p } });
  const run = useMutation({
    mutationFn: () =>
      productMaintApi.termsFinal(request.id, Object.values(choices), comment.trim() || undefined),
    onSuccess: () => onDone('Terms final'),
  });
  return (
    <Dialog
      title="Terms Final"
      confirm="Make Terms Final"
      busy={run.isPending}
      disabled={Object.keys(choices).length === 0}
      error={rounds.error ?? run.error}
      onClose={onClose}
      onConfirm={() => run.mutate()}
    >
      <p className="muted">
        Every insurer of the latest round must have an outcome. Choose the offers the package takes;
        the comparative master is compiled from this round.
      </p>
      {offers.length === 0 && <p className="muted">No insurer has offered terms yet.</p>}
      {offers.map((r) => (
        <div key={r.insurerCode} className="form-grid">
          <label className="checkbox">
            <input
              type="checkbox"
              checked={r.insurerCode in choices}
              onChange={() => toggle(r.insurerCode)}
            />
            {r.insurerName} · {r.rate ?? '—'}%
          </label>
          {r.insurerCode in choices && (
            <>
              <SelectInput
                label="Role"
                value={choices[r.insurerCode]?.role}
                options={ROLES}
                onChange={(role) => patch(r.insurerCode, { role })}
              />
              <NumberInput
                label="Share %"
                hint="Co-insurance only; shares total 100."
                value={choices[r.insurerCode]?.sharePercent}
                onChange={(sharePercent) => patch(r.insurerCode, { sharePercent })}
              />
            </>
          )}
        </div>
      ))}
      <CommentBox value={comment} onChange={setComment} />
    </Dialog>
  );
}

/** MBS set-up of the package version (BRPM.015); a new package needs its risk code. */
export function SetupDialog({ request, onClose, onDone }: Readonly<DialogProps>) {
  const isNew = request.productCode === undefined;
  const [productCode, setProductCode] = useState('');
  const [productName, setProductName] = useState(request.title);
  const [changeSummary, setChangeSummary] = useState('');
  const [comment, setComment] = useState('');
  const run = useMutation({
    mutationFn: () =>
      productMaintApi.setup(request.id, {
        productCode: isNew ? productCode.trim() : undefined,
        productName: isNew ? productName.trim() : undefined,
        changeSummary: changeSummary.trim() || undefined,
        comment: comment.trim() || undefined,
      }),
    onSuccess: (p) => onDone(`Version ${p.resultingVersionNo ?? ''} set up`),
  });
  return (
    <Dialog
      title="Set Up Package Version"
      confirm="Set Up Version"
      busy={run.isPending}
      disabled={isNew && productCode.trim() === ''}
      error={run.error}
      onClose={onClose}
      onConfirm={() => run.mutate()}
    >
      <p className="muted">
        The signed-off terms become a draft catalog version. Complete it in the version editor and
        submit it for validation; the request is released when the version is validated.
      </p>
      {isNew && (
        <>
          <TextInput
            label="Risk code of the new package"
            required
            upper
            value={productCode}
            onChange={setProductCode}
          />
          <TextInput label="Product name" value={productName} onChange={setProductName} />
        </>
      )}
      <TextInput label="Change summary" value={changeSummary} onChange={setChangeSummary} />
      <CommentBox value={comment} onChange={setComment} />
    </Dialog>
  );
}
