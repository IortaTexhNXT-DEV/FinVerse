import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { addresses, renewalPlacementApi } from '@/api/renewalPlacement';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';

interface DialogProps {
  refs: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Footer({
  label,
  busy,
  disabled,
  onClose,
  onConfirm,
}: Readonly<{
  label: string;
  busy: boolean;
  disabled: boolean;
  onClose: () => void;
  onConfirm: () => void;
}>) {
  return (
    <>
      <Button variant="ghost" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

function CopyField({
  value,
  onChange,
}: Readonly<{ value: string; onChange: (v: string) => void }>) {
  return (
    <Field label="CC recipients" hint="Separate the addresses with commas">
      {(id) => (
        <input id={id} className="input" value={value} onChange={(e) => onChange(e.target.value)} />
      )}
    </Field>
  );
}

/**
 * Send Placement (FRRN.29.03, FRRN.29.04): the insurers of the selected accounts with the
 * recipients of Insurer Maintenance, which the user can change, add or remove, and copy recipients;
 * insurers enrolled in MFT receive the files in their MFT location.
 */
export function SendPlacementDialog({
  refs,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & {
    onConfirm: (body: { recipients: Record<string, string[]>; cc: string[] }) => void;
  }
>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['renewal', 'placement-recipients', companyId, refs],
    queryFn: () => renewalPlacementApi.recipients(companyId, refs),
  });
  const [edits, setEdits] = useState<Record<string, string>>({});
  const [cc, setCc] = useState('');
  const text = (code: string, fallback: string[]) => edits[code] ?? fallback.join(', ');
  const confirm = () => {
    const recipients: Record<string, string[]> = {};
    (insurers.data ?? []).forEach((i) => {
      recipients[i.insurerCode] = addresses(text(i.insurerCode, i.recipients));
    });
    onConfirm({ recipients, cc: addresses(cc) });
  };
  return (
    <Modal
      open
      title={`Send Placement (${String(refs.length)})`}
      onClose={onClose}
      footer={
        <Footer
          label="Send"
          busy={busy}
          disabled={insurers.isLoading || (insurers.data ?? []).length === 0}
          onClose={onClose}
          onConfirm={confirm}
        />
      }
    >
      <ErrorAlert error={error ?? insurers.error} />
      {(insurers.data ?? []).length === 0 && !insurers.isLoading && (
        <p className="muted">Generate the placement of the selected accounts first.</p>
      )}
      {(insurers.data ?? []).map((i) => (
        <Field
          key={i.insurerCode}
          label={`${i.insurerName} (${String(i.accounts)} slip${i.accounts === 1 ? '' : 's'})`}
          hint={i.mft ? 'Enrolled in MFT: the files go to its MFT location' : 'Recipients'}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={i.mft}
              value={i.mft ? 'MFT' : text(i.insurerCode, i.recipients)}
              onChange={(e) => setEdits((old) => ({ ...old, [i.insurerCode]: e.target.value }))}
            />
          )}
        </Field>
      ))}
      <CopyField value={cc} onChange={setCc} />
    </Modal>
  );
}

/** Send Insurance Advice (FRRN.032.02): nominated recipients and copy recipients for all accounts. */
export function SendAdviceDialog({
  refs,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps & { onConfirm: (body: { to: string[]; cc: string[] }) => void }>) {
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
  return (
    <Modal
      open
      title={`Send Insurance Advice (${String(refs.length)})`}
      onClose={onClose}
      footer={
        <Footer
          label="Send via CCM"
          busy={busy}
          disabled={addresses(to).length === 0}
          onClose={onClose}
          onConfirm={() => onConfirm({ to: addresses(to), cc: addresses(cc) })}
        />
      }
    >
      <ErrorAlert error={error} />
      <p className="muted">{refs.join(', ')}</p>
      <Field label="Recipients" required hint="Separate the addresses with commas">
        {(id) => (
          <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
        )}
      </Field>
      <CopyField value={cc} onChange={setCc} />
    </Modal>
  );
}

/** Return to Marketing (FRRN.031.01) with one of the return reasons and remarks. */
export function ReturnPlacementDialog({
  refs,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps & { onConfirm: (body: { reasonCode: string; remarks: string }) => void }>) {
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  return (
    <Modal
      open
      title={`Return to Marketing (${String(refs.length)})`}
      onClose={onClose}
      footer={
        <Footer
          label="Return"
          busy={busy}
          disabled={reason === '' || remarks.trim() === ''}
          onClose={onClose}
          onConfirm={() => onConfirm({ reasonCode: reason, remarks })}
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Return reason" required>
        {(id) => (
          <LovSelect
            id={id}
            type="RNW_PLACEMENT_RETURN_REASON"
            value={reason}
            onChange={setReason}
            required
          />
        )}
      </Field>
      <Field label="Remarks" required>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
