import { useState } from 'react';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { prodreconApi } from './prodreconApi';
import type { ReconExtract } from './prodreconApi';
import { addresses, validAddresses } from './prodreconLogic';

/** Sends a production register to the insurer by protected e-mail (PRCID.004-006). */
export function SendExtractDialog({
  extract,
  busy,
  onClose,
  onSend,
}: Readonly<{
  extract: ReconExtract;
  busy: boolean;
  onClose: () => void;
  onSend: (to: string[], cc: string[]) => void;
}>) {
  const [to, setTo] = useState(extract.recipients ?? '');
  const [cc, setCc] = useState('');
  const [error, setError] = useState<string>();
  const send = () => {
    if (to.trim() !== '' && !validAddresses(to)) {
      setError('Enter valid e-mail addresses');
      return;
    }
    onSend(addresses(to), addresses(cc));
  };
  return (
    <Modal
      title={`Send ${extract.fileName}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={send}>
            Send to Insurer
          </Button>
        </>
      }
    >
      <div className="stack">
        <Field
          label="To"
          error={error}
          hint="Leave blank to use the insurer's reconciliation contacts"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setError(undefined);
              }}
            />
          )}
        </Field>
        <Field label="Cc">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
        <p className="muted">
          The register is sent as a password-protected workbook; the password goes to the insurer
          separately.
        </p>
      </div>
    </Modal>
  );
}

/** A download button for a register. */
export function DownloadButton({ extract }: Readonly<{ extract: ReconExtract }>) {
  const download = useFileDownload();
  return (
    <Button
      size="sm"
      variant="ghost"
      busy={download.isPending}
      onClick={(e) => {
        e.stopPropagation();
        download.mutate(() => prodreconApi.extractFile(extract.id));
      }}
    >
      Download
    </Button>
  );
}
