import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarRange, Coins, FileCheck, ReceiptText, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';
import { CertificateDialog } from './CertificateDialog';
import { CERTIFICATE_ENTITY, commissionApi } from './commissionApi';
import type { Certificate, OrLink } from './commissionApi';
import { receiptsTotal } from './commissionLogic';
import { ReasonDialog } from './DpItemDialogs';

const TABS = [
  { id: 'receipts', label: 'Official Receipts' },
  { id: 'documents', label: 'Documents' },
] as const;

type TabId = (typeof TABS)[number]['id'];
type DialogKind = 'reject' | 'resubmit';

const OR_COLUMNS: Column<OrLink>[] = [
  { key: 'or', header: 'OR No.', render: (r) => <strong>{r.orNo}</strong> },
  {
    key: 'amount',
    header: 'Commission Amount',
    numeric: true,
    render: (r) => <Amount value={r.amount} />,
  },
];

function Summary({ cert: c }: Readonly<{ cert: Certificate }>) {
  return (
    <RecordSummary
      title={c.insurerCode}
      chips={
        <>
          <ReferenceChip label="Submission" value={c.submissionNo} />
          <StatusBadge status={c.stage} />
        </>
      }
      facts={[
        {
          icon: FileCheck,
          label: 'Certificate',
          value: `${c.certificate.form} ${c.certificate.number}`,
        },
        {
          icon: CalendarRange,
          label: 'Period',
          value: `${formatDate(c.certificate.periodFrom)} – ${formatDate(c.certificate.periodTo)}`,
        },
        { icon: Coins, label: 'Tax Withheld', value: formatAmount(c.certificate.taxWithheld) },
        {
          icon: ReceiptText,
          label: 'ORs Covered',
          value: `${String(c.certificate.receipts.length)} (${formatAmount(receiptsTotal(c.certificate))})`,
        },
        {
          icon: UserRound,
          label: 'Submitted',
          value: `${formatDateTime(c.createdAt)} · ${c.createdBy}`,
        },
        {
          icon: Building2,
          label: 'Decision',
          value: c.decidedBy ? `${c.decidedBy} · ${formatDateTime(c.decidedAt)}` : 'Pending',
        },
      ]}
    />
  );
}

/**
 * A BIR certificate submission (CMRID.015): the certificate, the ORs it covers and its scanned
 * copy; Comptrollership acknowledges or rejects it, and a rejected one is corrected and resubmitted.
 */
export default function CertificatePage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('receipts');
  const [dialog, setDialog] = useState<DialogKind>();
  const cert = useQuery({
    queryKey: ['commission', 'certificate', id],
    queryFn: () => commissionApi.certificate(id),
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<Certificate>) => fn(),
    onSuccess: async (c) => {
      setDialog(undefined);
      await queryClient.invalidateQueries({ queryKey: ['commission'] });
      toast.success(`${c.submissionNo} ${c.stage.toLowerCase()}`);
    },
  });
  if (cert.data === undefined) {
    return cert.error ? (
      <ErrorAlert error={cert.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = cert.data;
  const decide = can('BIR_CERT_ACK') && c.stage === 'SUBMITTED';
  const resubmit = can('BIR_CERT_SUBMIT') && c.stage === 'REJECTED';
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables · BIR Certificates"
        backTo="/commission/certificates"
        title={c.submissionNo}
        description={c.rejectReason ? `Rejected: ${c.rejectReason}` : c.insurerCode}
        actions={
          <>
            {decide && (
              <>
                <Button variant="secondary" onClick={() => setDialog('reject')}>
                  Reject
                </Button>
                <Button
                  busy={act.isPending}
                  onClick={() => act.mutate(() => commissionApi.acknowledge(id, ''))}
                >
                  Acknowledge
                </Button>
              </>
            )}
            {resubmit && (
              <Button onClick={() => setDialog('resubmit')}>Correct and Resubmit</Button>
            )}
          </>
        }
      />
      <ErrorAlert error={dialog === undefined ? act.error : undefined} />
      <Summary cert={c} />
      <WorkflowPanel
        entityType={CERTIFICATE_ENTITY}
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['commission'] })}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'receipts' ? (
        <Card>
          <DataTable
            caption="Official receipts covered"
            columns={OR_COLUMNS}
            rows={c.certificate.receipts}
            rowKey={(r) => r.orNo}
            emptyMessage="No items to display"
          />
        </Card>
      ) : (
        <Attachments entityType={CERTIFICATE_ENTITY} entityId={c.id} reference={c.submissionNo} />
      )}
      {dialog === 'reject' && (
        <ReasonDialog
          title={`Reject ${c.submissionNo}`}
          action="Reject Certificate"
          label="Reason"
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(reason) => act.mutate(() => commissionApi.reject(id, reason))}
        />
      )}
      {dialog === 'resubmit' && (
        <CertificateDialog
          companyId={companyId}
          certificate={c}
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onSave={(input) => act.mutate(() => commissionApi.resubmitCertificate(id, input))}
        />
      )}
    </div>
  );
}
