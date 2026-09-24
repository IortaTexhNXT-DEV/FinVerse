import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, CircleAlert, Upload } from 'lucide-react';
import { useState } from 'react';
import { clientsApi } from '@/api/clients';
import type { ClientDetail, KycChecklist } from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Attachments } from '@/components/attachments/Attachments';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

function ChecklistItems({ checklist }: Readonly<{ checklist: KycChecklist }>) {
  if (checklist.items.length === 0) {
    return <p className="muted">No KYC document is required for this client type.</p>;
  }
  return (
    <ul className="checklist">
      {checklist.items.map((item) => {
        const done = item.documents.length > 0;
        return (
          <li key={item.documentType} className="checklist-item">
            <span className={done ? 'checklist-state done' : 'checklist-state missing'}>
              {done ? (
                <CheckCircle2 size={18} aria-label="Uploaded" />
              ) : (
                <CircleAlert size={18} aria-label="Missing" />
              )}
            </span>
            <span className="checklist-label">
              {item.label}
              {!item.required && <span className="muted"> (additional)</span>}
            </span>
            <span className="checklist-files muted">
              {done
                ? item.documents
                    .map((d) => `${d.fileName} · ${d.uploadedBy} · ${formatDateTime(d.uploadedAt)}`)
                    .join('; ')
                : 'Not uploaded yet'}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function UploadForm({ clientId }: Readonly<{ clientId: number }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [documentType, setDocumentType] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [inputKey, setInputKey] = useState(0);
  const upload = useMutation({
    mutationFn: (chosen: File) => clientsApi.uploadKyc(clientId, documentType, chosen),
    onSuccess: async (_checklist, chosen) => {
      toast.success(`${chosen.name} uploaded`);
      setFile(null);
      setInputKey((k) => k + 1);
      await queryClient.invalidateQueries({ queryKey: ['crm'] });
      await queryClient.invalidateQueries({ queryKey: ['attachments', 'Client'] });
    },
  });
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        if (file !== null) {
          upload.mutate(file);
        }
      }}
    >
      <ErrorAlert error={upload.error} />
      <div className="form-grid">
        <Field label="Document type" required>
          {(id) => (
            <LovSelect
              id={id}
              type="DOCUMENT_TYPE"
              value={documentType}
              onChange={setDocumentType}
            />
          )}
        </Field>
        <Field label="File" required hint="PDF, image or office document">
          {(id) => (
            <input
              key={inputKey}
              id={id}
              className="input"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          )}
        </Field>
      </div>
      <div className="row">
        <Button
          type="submit"
          variant="primary"
          icon={<Upload size={16} />}
          busy={upload.isPending}
          disabled={documentType === '' || file === null}
        >
          Upload KYC document
        </Button>
      </div>
    </form>
  );
}

/**
 * KYC & Documents (BRNB.030/090): the mandatory documents of the client type with their upload
 * status, typed upload, and every document attached to the client.
 */
export function KycTab({ client }: Readonly<{ client: ClientDetail }>) {
  const { can } = useAuth();
  const checklist = useQuery({
    queryKey: ['crm', 'checklist', client.id],
    queryFn: () => clientsApi.checklist(client.id),
  });
  const data = checklist.data;
  return (
    <div className="stack">
      <Card
        title="KYC checklist"
        actions={
          data && (
            <span className={data.complete ? 'badge success' : 'badge warning'}>
              {data.complete ? 'Complete' : `${String(data.missing.length)} missing`}
            </span>
          )
        }
      >
        <ErrorAlert error={checklist.error} />
        {data ? (
          <ChecklistItems checklist={data} />
        ) : (
          <span className="spinner" aria-label="Loading" />
        )}
      </Card>
      {can('CLIENT_MAINTAIN') && client.status !== 'INACTIVE' && (
        <Card title="Upload a KYC document">
          <UploadForm clientId={client.id} />
        </Card>
      )}
      <Attachments entityType="Client" entityId={client.id} title="All client documents" />
    </div>
  );
}
