import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { acslApi } from './api';

type Key = 'insurer' | 'from' | 'to' | 'file';

function problems(insurer: string, from: string, to: string, file: File | undefined) {
  const found: Partial<Record<Key, string>> = {};
  if (insurer.trim() === '') {
    found.insurer = 'Enter the insurer code';
  }
  if (from === '') {
    found.from = 'Enter the start of the period';
  }
  if (to === '' || (from !== '' && to < from)) {
    found.to = 'Enter an end on or after the start';
  }
  if (!file) {
    found.file = 'Choose the statement file';
  }
  return found;
}

/**
 * Uploads an insurer statement of account (ACSL 2.14.0): the insurer, the period and the file in
 * the insurer's layout (CSV, Excel, ODS or text); the reconciliation runs on upload.
 */
export function SoaUploadDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [file, setFile] = useState<File>();
  const [errors, setErrors] = useState<Partial<Record<Key, string>>>({});
  const layouts = useQuery({ queryKey: ['acsl', 'layouts'], queryFn: acslApi.layouts });
  const layout =
    layouts.data?.find((l) => l.insurerCode === insurer.trim().toUpperCase()) ??
    layouts.data?.find((l) => l.insurerCode === '*');
  const upload = useMutation({
    mutationFn: (f: File) =>
      acslApi.uploadSoa(
        companyId,
        { insurerCode: insurer.trim(), periodFrom: from, periodTo: to },
        f,
      ),
    onSuccess: async (u) => {
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      toast.success(`${u.uploadNo}: ${String(u.rowsLoaded)} of ${String(u.rowsRead)} rows loaded`);
      void navigate(`/acsl/soa/${String(u.id)}`);
    },
  });
  const submit = () => {
    const found = problems(insurer, from, to, file);
    setErrors(found);
    if (Object.keys(found).length === 0 && file) {
      upload.mutate(file);
    }
  };
  return (
    <Modal
      open
      title="Upload an Insurer SOA"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={upload.isPending} onClick={submit}>
            Upload and Reconcile
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <Field label="Insurer Code" required error={errors.insurer}>
          {(id) => (
            <input
              id={id}
              className="input"
              aria-invalid={errors.insurer !== undefined}
              value={insurer}
              onChange={(e) => setInsurer(e.target.value)}
            />
          )}
        </Field>
        <div className="form-grid">
          <Field label="Period From" required error={errors.from}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            )}
          </Field>
          <Field label="Period To" required error={errors.to}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            )}
          </Field>
        </div>
        <Field
          label="Statement File"
          required
          error={errors.file}
          hint={layout ? `Columns: ${layout.headers.join(', ')}` : 'CSV, Excel, ODS or text'}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type="file"
              accept=".csv,.xlsx,.ods,.txt"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
