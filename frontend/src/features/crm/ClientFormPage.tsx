import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Save } from 'lucide-react';
import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientDetail } from '@/api/clients';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { ContactSection, IdentitySection, SegmentSection } from './ClientFormFields';
import { DuplicateWarnings } from './DuplicateWarnings';
import {
  EMPTY_CLIENT_FORM,
  duplicateQuery,
  fromClient,
  toRequest,
  validateClient,
} from './clientForm';
import type { ClientForm } from './clientForm';

function Saved({ client, onAnother }: Readonly<{ client: ClientDetail; onAnother: () => void }>) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="success-panel">
        <CheckCircle2 size={48} aria-hidden="true" className="text-success" />
        <h2>Prospect saved</h2>
        <p>
          {client.displayName} can now be quoted. Upload the KYC documents and submit them for
          verification to confirm the client.
        </p>
        <ReferenceChip label="Prospect code" value={client.prospectCode} />
        <div className="row">
          <Button
            variant="accent"
            onClick={() => void navigate(`/crm/clients/${String(client.id)}`)}
          >
            Open client
          </Button>
          <Button variant="secondary" onClick={onAnother}>
            Create another
          </Button>
        </div>
      </div>
    </Card>
  );
}

/** The client form of a new client, or of an existing one once loaded. */
function ClientEditor({ existing }: Readonly<{ existing?: ClientDetail }>) {
  const editId = existing?.id;
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ClientForm>(() =>
    existing === undefined ? EMPTY_CLIENT_FORM : fromClient(existing),
  );
  const [submitted, setSubmitted] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [created, setCreated] = useState<ClientDetail | null>(null);
  const set = (patch: Partial<ClientForm>) => setForm((f) => ({ ...f, ...patch }));
  const onHardMatch = useCallback((value: boolean) => setBlocked(value), []);
  const errors = submitted ? validateClient(form, today()) : {};

  const save = useMutation({
    mutationFn: () =>
      editId === undefined
        ? clientsApi.create(toRequest(form, companyId))
        : clientsApi.update(editId, toRequest(form)),
    onSuccess: async (client) => {
      await queryClient.invalidateQueries({ queryKey: ['crm'] });
      if (editId === undefined) {
        setCreated(client);
        toast.success(`Prospect ${client.prospectCode} saved`);
      } else {
        toast.success(`${client.code} updated`);
        void navigate(`/crm/clients/${String(client.id)}`);
      }
    },
  });
  const submit = () => {
    setSubmitted(true);
    if (Object.keys(validateClient(form, today())).length === 0 && !blocked) {
      save.mutate();
    }
  };

  if (created !== null) {
    return (
      <div className="stack">
        <PageHeader section="Clients" title="New client" />
        <Saved
          client={created}
          onAnother={() => {
            setCreated(null);
            setForm(EMPTY_CLIENT_FORM);
            setSubmitted(false);
          }}
        />
      </div>
    );
  }
  return (
    <div className="stack">
      <PageHeader
        section="Clients"
        title={existing === undefined ? 'New client' : `Edit ${existing.code}`}
        description="Only the client type and name are needed to save a prospect; complete the rest before submitting the KYC. Duplicates are checked as you type."
      />
      <ErrorAlert error={save.error} />
      <form
        className="stack"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <IdentitySection
          form={form}
          errors={errors}
          set={set}
          typeLocked={existing?.status === 'CONFIRMED'}
        />
        <DuplicateWarnings
          query={duplicateQuery(form, companyId, editId)}
          onHardMatch={onHardMatch}
        />
        <ContactSection form={form} errors={errors} set={set} />
        <SegmentSection form={form} errors={errors} set={set} />
        {submitted && Object.keys(errors).length > 0 && (
          <div className="alert warning" role="alert">
            Correct the highlighted fields before saving.
          </div>
        )}
        <div className="row">
          <Button
            type="submit"
            variant="accent"
            icon={<Save size={16} />}
            busy={save.isPending}
            disabled={blocked}
          >
            {editId === undefined ? 'Save as prospect' : 'Save changes'}
          </Button>
          <Button variant="secondary" onClick={() => void navigate(-1)}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}

/**
 * New client or client change (BRNB.029/030/048/049): identity, contact and address, segment and
 * bank relationship, with live duplicate warnings. A new client is saved as a prospect with only
 * its type and name if need be; the success screen shows the prospect code.
 */
export default function ClientFormPage() {
  const params = useParams();
  const editId = params.id === undefined ? undefined : Number(params.id);
  const existing = useQuery({
    queryKey: ['crm', 'client', editId],
    queryFn: () => clientsApi.get(editId ?? 0),
    enabled: editId !== undefined,
  });
  if (editId === undefined) {
    return <ClientEditor />;
  }
  if (existing.data === undefined) {
    return existing.error ? (
      <ErrorAlert error={existing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <ClientEditor key={existing.data.id} existing={existing.data} />;
}
