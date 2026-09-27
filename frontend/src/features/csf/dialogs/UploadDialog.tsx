import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import type { AccountLine } from '@/api/csf';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { CSF_LOV } from '../csfCodes';

/** File types the contact centre uploads (BRCSF-007): text, spreadsheets, images and PDF. */
export const CSF_ACCEPT =
  '.pdf,.doc,.docx,.txt,.rtf,.xls,.xlsx,.ods,.csv,.png,.jpg,.jpeg,.heic,.heif,.gif,.bmp,.tif,.tiff,.webp';

/**
 * Upload (FR-CSF-032; BRCSF-007 / 7.001): a document to the client or one of its accounts, with a
 * document type of the contact centre list; the file type and content are checked before it is
 * stored.
 */
export function UploadDialog({
  companyId,
  clientId,
  clientCode,
  accounts,
  onClose,
}: Readonly<{
  companyId: number;
  clientId: number;
  clientCode: string;
  accounts: AccountLine[];
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState('');
  const [documentType, setDocumentType] = useState('');
  const [description, setDescription] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const upload = useMutation({
    mutationFn: (file: File) =>
      csfApi.upload(companyId, clientId, file, {
        accountId: target === '' ? undefined : Number(target),
        documentType,
        description: description.trim() || undefined,
      }),
    onSuccess: (d) => {
      toast.success(`${d.fileName} uploaded to ${d.reference}`);
      void queryClient.invalidateQueries({ queryKey: ['csf'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Upload Document"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            disabled={files.length === 0 || documentType === ''}
            busy={upload.isPending}
            onClick={() => {
              const file = files.at(0);
              if (file) {
                upload.mutate(file);
              }
            }}
          >
            Upload
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} title="Cannot upload the document" />
        <div className="form-grid">
          <Field label="Attach To" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={target}
                onChange={(e) => setTarget(e.target.value)}
              >
                <option value="">Client {clientCode}</option>
                {accounts.map((a) => (
                  <option key={a.id} value={String(a.id)}>
                    Account {a.arn}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Document Type" required>
            {(id) => (
              <LovSelect
                id={id}
                type={CSF_LOV.documentType}
                value={documentType}
                onChange={setDocumentType}
                required
              />
            )}
          </Field>
          <Field label="Description">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={200}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            )}
          </Field>
        </div>
        <FileDropZone accept={CSF_ACCEPT} onChange={setFiles} busy={upload.isPending} />
      </div>
    </Modal>
  );
}
