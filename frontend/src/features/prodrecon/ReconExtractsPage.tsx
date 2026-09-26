import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { SendExtractDialog } from './ExtractParts';
import { extractColumns } from './extractColumns';
import { prodreconApi } from './prodreconApi';
import type { ReconExtract } from './prodreconApi';
import { monthRange } from './prodreconLogic';

function ExtractDialog({
  busy,
  error,
  onClose,
  onExtract,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onExtract: (insurer: string, from: string, to: string) => void;
}>) {
  const [insurer, setInsurer] = useState('');
  const [month, setMonth] = useState(today().slice(0, 7));
  const [invalid, setInvalid] = useState<string>();
  const extract = () => {
    if (insurer.trim() === '' || month === '') {
      setInvalid('Insurer and production month are required');
      return;
    }
    const { from, to } = monthRange(month);
    onExtract(insurer.trim().toUpperCase(), from, to);
  };
  return (
    <Modal
      title="New Production Extract"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={extract}>
            Extract Register
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Insurer Code" required error={invalid}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={30}
              value={insurer}
              onChange={(e) => {
                setInsurer(e.target.value);
                setInvalid(undefined);
              }}
            />
          )}
        </Field>
        <Field label="Production Month" required hint="Accounts booked in the month">
          {(id) => (
            <input
              id={id}
              type="month"
              className="input"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
          )}
        </Field>
        <p className="muted">
          The register lists the accounts booked for the insurer in the month; it opens the
          month&apos;s reconciliation cycle if none is open.
        </p>
      </div>
    </Modal>
  );
}

/**
 * Production registers (PRCID.001-008): scheduled and manual extracts per insurer, downloaded or
 * sent to the insurer by protected e-mail.
 */
export default function ReconExtractsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [sending, setSending] = useState<ReconExtract>();
  const extracts = useQuery({
    queryKey: ['prodrecon', 'extracts', companyId, insurer, page],
    queryFn: () => prodreconApi.extracts(companyId, insurer || undefined, page),
    enabled: companyId > 0,
  });
  const done = async (message: string) => {
    setCreating(false);
    setSending(undefined);
    await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
    toast.success(message);
  };
  const create = useMutation({
    mutationFn: (v: { insurer: string; from: string; to: string }) =>
      prodreconApi.extract(companyId, v.insurer, v.from, v.to),
    onSuccess: (e) => done(`${e.extractNo} extracted: ${String(e.rowCount)} account(s)`),
  });
  const send = useMutation({
    mutationFn: (v: { id: number; to: string[]; cc: string[] }) =>
      prodreconApi.send(v.id, v.to, v.cc),
    onSuccess: (e) => done(`${e.fileName} sent`),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Production Reconciliation"
        title="Production Extracts"
        description="Registers of booked production extracted for each insurer, by schedule or on request."
        actions={
          can('RECON_PROCESS') ? (
            <Button icon={<FilePlus2 size={16} />} onClick={() => setCreating(true)}>
              New Extract
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={extracts.error ?? send.error} />
      <Card>
        <div className="stack">
          <WorklistToolbar
            placeholder="Search Insurer Code"
            onSearch={(text) => {
              setInsurer(text.trim().toUpperCase());
              setPage(0);
            }}
          />
          <DataTable
            caption="Production extracts"
            columns={extractColumns(can('RECON_SEND') ? setSending : undefined)}
            rows={extracts.data?.content ?? []}
            rowKey={(e) => e.id}
            loading={extracts.isLoading}
            emptyMessage="No items to display"
          />
          <PageFooter data={extracts.data} noun="extracts" onPage={setPage} />
        </div>
      </Card>
      {creating && (
        <ExtractDialog
          busy={create.isPending}
          error={create.error}
          onClose={() => setCreating(false)}
          onExtract={(ins, from, to) => create.mutate({ insurer: ins, from, to })}
        />
      )}
      {sending !== undefined && (
        <SendExtractDialog
          extract={sending}
          busy={send.isPending}
          onClose={() => setSending(undefined)}
          onSend={(to, cc) => send.mutate({ id: sending.id, to, cc })}
        />
      )}
    </div>
  );
}
