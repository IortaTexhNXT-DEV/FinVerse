import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { RecordCertificateDialog } from './RecordCertificateDialog';
import { receivedApi } from './receivedCertificates';
import type { CertificateStatus, ReceivedCertificate } from './receivedCertificates';

type Tab = 'ALL' | CertificateStatus;

function columns(
  mayCancel: boolean,
  onCancel: (c: ReceivedCertificate) => void,
): Column<ReceivedCertificate>[] {
  return [
    {
      key: 'no',
      header: 'Certificate No.',
      render: (c) => (
        <>
          <strong>{c.certificateNo}</strong>
          <span className="cell-sub">Received {formatDate(c.receivedOn)}</span>
        </>
      ),
    },
    {
      key: 'agent',
      header: 'Withholding Agent',
      render: (c) => (
        <>
          {c.agentName}
          <span className="cell-sub">
            {c.agentCode}
            {c.agentTin ? ` · TIN ${c.agentTin}` : ''}
          </span>
        </>
      ),
    },
    {
      key: 'period',
      header: 'Period Covered',
      render: (c) => `${formatDate(c.periodFrom)} – ${formatDate(c.periodTo)}`,
    },
    {
      key: 'income',
      header: 'Income',
      numeric: true,
      render: (c) => <Amount value={c.incomeTotal} />,
    },
    {
      key: 'tax',
      header: 'Tax Withheld',
      numeric: true,
      render: (c) => <Amount value={c.taxTotal} />,
    },
    {
      key: 'journal',
      header: 'Journal',
      render: (c) => (
        <>
          {c.journalBatchNo ?? '—'}
          <span className="cell-sub">
            {c.cancelJournalNo ? `Reversed ${c.cancelJournalNo}` : (c.sourceRef ?? '')}
          </span>
        </>
      ),
    },
    { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
    {
      key: 'actions',
      header: '',
      render: (c) =>
        mayCancel && c.status === 'RECORDED' ? (
          <Button size="sm" variant="secondary" onClick={() => onCancel(c)}>
            Cancel Certificate
          </Button>
        ) : null,
    },
  ];
}

/**
 * Certificates Received (DIS 2.11, OQ41, AQ16): the register of the BIR 2307 certificates of the
 * insurers on BDOI's commission and incentives, recorded by Disbursement or Tax, posted to AR-BIR
 * on hand, and listed in the SAWT and the income tax worksheets.
 */
export default function ReceivedCertificatesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>('ALL');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [recording, setRecording] = useState(false);
  const [cancelling, setCancelling] = useState<ReceivedCertificate>();
  const status = tab === 'ALL' ? undefined : tab;
  const list = useQuery({
    queryKey: ['tax', 'received', companyId, status, q, page],
    queryFn: () => receivedApi.search(companyId, { status, q, page }),
    enabled: companyId > 0,
  });
  const cancel = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => receivedApi.cancel(id, reason),
    onSuccess: async (c) => {
      setCancelling(undefined);
      toast.success(`Certificate ${c.certificateNo} cancelled and its posting reversed`);
      await queryClient.invalidateQueries({ queryKey: ['tax', 'received'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="Certificates Received"
        description="BIR 2307 certificates of the insurers on commission and incentives: recorded, posted to AR-BIR on hand and listed in the SAWT."
        actions={
          (can('TAX_MANAGE') || can('DISB_TAG')) && (
            <Button icon={<FilePlus2 size={16} />} onClick={() => setRecording(true)}>
              Record Certificate
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<Tab>
            tabs={[
              { id: 'ALL', label: 'All' },
              { id: 'RECORDED', label: 'Recorded' },
              { id: 'CANCELLED', label: 'Cancelled' },
            ]}
            active={tab}
            onChange={(next) => {
              setTab(next);
              setPage(0);
            }}
          />
        </div>
        <WorklistToolbar
          placeholder="Search Certificate No. or Agent"
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
        />
        <DataTable
          caption="Certificates received"
          columns={columns(can('TAX_MANAGE'), setCancelling)}
          rows={list.data?.content ?? []}
          rowKey={(c) => c.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
        />
        <PageFooter data={list.data} noun="certificates" onPage={setPage} />
      </Card>
      {recording && (
        <RecordCertificateDialog
          onClose={() => setRecording(false)}
          onDone={(c) => {
            setRecording(false);
            toast.success(
              `Certificate ${c.certificateNo} recorded (journal ${c.journalBatchNo ?? '—'})`,
            );
            void queryClient.invalidateQueries({ queryKey: ['tax', 'received'] });
          }}
        />
      )}
      {cancelling && (
        <ActionDialog
          title={`Cancel Certificate · ${cancelling.certificateNo}`}
          confirmLabel="Cancel Certificate"
          busy={cancel.isPending}
          error={cancel.error}
          onConfirm={(note) =>
            cancel.mutate({ id: cancelling.id, reason: note.comment ?? 'Cancelled' })
          }
          onClose={() => setCancelling(undefined)}
        />
      )}
    </div>
  );
}
