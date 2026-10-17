import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/**
 * Download a generated document under a name of the user's choice; the attached document keeps its
 * own name (FRRN.029.02).
 */
export function SaveAsDialog({
  attachmentId,
  fileName,
  onClose,
}: Readonly<{ attachmentId: number; fileName: string; onClose: () => void }>) {
  const [name, setName] = useState(fileName);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const save = async () => {
    setBusy(true);
    try {
      const file = await attachmentsApi.download(attachmentId);
      saveFile(file.blob, name.trim() === '' ? fileName : name.trim());
      onClose();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal
      open
      title="Download As"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={() => void save()}>
            Download
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <Field label="File name">
        {(id) => (
          <input id={id} className="input" value={name} onChange={(e) => setName(e.target.value)} />
        )}
      </Field>
    </Modal>
  );
}
