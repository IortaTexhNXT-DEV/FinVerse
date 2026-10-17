import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pmTermsApi } from '@/api/pmTerms';
import type { ClientResponseCode, ClientResponseView } from '@/api/pmTerms';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { clientResponseProblems } from './termsTable';

const RESPONSES: { code: ClientResponseCode; label: string }[] = [
  { code: 'ACCEPTED', label: 'Accepted' },
  { code: 'REJECTED', label: 'Rejected' },
  { code: 'RETURNED', label: 'Return for Revision' },
];

const TONES = { ACCEPTED: 'success', REJECTED: 'danger', RETURNED: 'warning' } as const;

const COLUMNS: Column<ClientResponseView>[] = [
  {
    key: 'response',
    header: 'Response',
    render: (r) => (
      <StatusBadge status={r.response} label={r.responseName} tone={TONES[r.response]} />
    ),
  },
  { key: 'date', header: 'Response Date', render: (r) => formatDate(r.responseDate) },
  { key: 'remarks', header: 'Client Remarks', render: (r) => r.remarks ?? '—' },
  { key: 'by', header: 'Recorded By', render: (r) => r.recordedBy },
  { key: 'at', header: 'Recorded On', render: (r) => formatDateTime(r.recordedAt) },
];

function RecordDialog({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [response, setResponse] = useState<ClientResponseCode>('ACCEPTED');
  const [responseDate, setResponseDate] = useState(today());
  const [remarks, setRemarks] = useState('');
  const [tried, setTried] = useState(false);
  const input = { response, responseDate, remarks };
  const problems = tried ? clientResponseProblems(input, today()) : {};
  const record = useMutation({
    mutationFn: () => pmTermsApi.recordClientResponse(id, input),
    onSuccess: async (saved) => {
      toast.success(`Client response recorded: ${saved.responseName}`);
      await queryClient.invalidateQueries({ queryKey: ['proposal', id] });
      await queryClient.invalidateQueries({ queryKey: ['pm-terms'] });
      onClose();
    },
  });
  const send = () => {
    setTried(true);
    if (Object.keys(clientResponseProblems(input, today())).length === 0) {
      record.mutate();
    }
  };
  return (
    <Modal
      title="Record Client Response"
      open
      size="md"
      onClose={onClose}
      helper="Accepted needs the client's acceptance e-mail in the documents of the request."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={record.isPending} onClick={send}>
            Record Response
          </Button>
        </>
      }
    >
      <ErrorAlert error={record.error} />
      <div className="form-grid">
        <Field label="Client Response" required>
          {(fid) => (
            <select
              id={fid}
              className="select"
              value={response}
              onChange={(e) => setResponse(e.target.value as ClientResponseCode)}
            >
              {RESPONSES.map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Response Date" required error={problems.responseDate}>
          {(fid) => (
            <DateInput
              id={fid}
              max={today()}
              value={responseDate}
              onChange={(e) => setResponseDate(e.target.value)}
            />
          )}
        </Field>
        <Field label="Client Remarks" required={response !== 'ACCEPTED'} error={problems.remarks}>
          {(fid) => (
            <textarea
              id={fid}
              className="input"
              rows={3}
              maxLength={1000}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * The client response to the proposal of a quotation request (BDOI FRS FRPM.010.01): the
 * responses recorded and, once the proposal was sent, Record Client Response.
 */
export function ClientResponseCard({
  id,
  canRecord,
}: Readonly<{ id: number; canRecord: boolean }>) {
  const { can } = useAuth();
  const [open, setOpen] = useState(false);
  const responses = useQuery({
    queryKey: ['pm-terms', 'quotation', id, 'client-responses'],
    queryFn: () => pmTermsApi.clientResponses(id),
  });
  const action =
    canRecord && can('PROPOSAL_REQUEST') ? (
      <Button onClick={() => setOpen(true)}>Record Client Response</Button>
    ) : undefined;
  return (
    <Card title="Client Response" actions={action} flush>
      <ErrorAlert error={responses.error} />
      <DataTable
        columns={COLUMNS}
        rows={responses.data ?? []}
        rowKey={(r) => r.id}
        loading={responses.isLoading}
        emptyMessage="No client response recorded yet"
      />
      {open && <RecordDialog id={id} onClose={() => setOpen(false)} />}
    </Card>
  );
}
