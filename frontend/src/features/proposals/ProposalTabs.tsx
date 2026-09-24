import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileSpreadsheet, FileText, Save } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { PROPOSAL_ENTITY, proposalsApi } from '@/api/proposals';
import type { ComparativeRow, Proposal } from '@/api/proposals';
import { workflowApi } from '@/api/workflow';
import { StageTimeline } from '@/components/broking/StageTimeline';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useAuth } from '@/auth/authContext';
import { DetailList } from '@/features/catalog/DetailList';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { DocumentChecklist, InsurerChoices } from './ProposalFormParts';
import { SLIP_EDIT_STAGES, slipSent } from './proposalList';

function useDownload() {
  return useMutation({
    mutationFn: (fetch: () => Promise<DownloadedFile>) => fetch(),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
}

function itemLabel(data: Proposal['items'][number]['data']): string {
  return (
    data.vehicle?.plateNo ??
    data.location?.address ??
    data.person?.name ??
    data.description ??
    'Risk item'
  );
}

/** Details: risk sections, items, TSU routing, requested insurers and mandatory documents. */
export function DetailsTab({ proposal: p }: Readonly<{ proposal: Proposal }>) {
  return (
    <div className="stack">
      <Card title="Risk details">
        <div className="stack">
          {p.sections.length === 0 && <p className="muted">No risk section.</p>}
          {p.sections.map((s, i) => (
            <div key={`s-${i + 1}`} className="risk-section">
              <h3>{s.heading ?? 'Details'}</h3>
              <p>{s.text}</p>
            </div>
          ))}
        </div>
      </Card>
      <Card title="Risk items" flush>
        <DataTable
          rows={p.items}
          rowKey={(i) => i.itemNo}
          emptyMessage="No risk item."
          columns={[
            { key: 'n', header: '#', numeric: true, render: (i) => i.itemNo },
            { key: 'g', header: 'Risk group', numeric: true, render: (i) => i.riskGroup },
            { key: 'l', header: 'Risk', render: (i) => <strong>{itemLabel(i.data)}</strong> },
            {
              key: 's',
              header: 'Sum insured',
              numeric: true,
              render: (i) => <Amount value={i.data.sumInsured} />,
            },
          ]}
        />
      </Card>
      <Card title="Routing and insurers">
        <DetailList
          rows={[
            ['TSU routing', p.tsuReason ?? 'Evaluated at submission'],
            ['Requested insurers', p.insurers.join(', ') || '—'],
            ['Submitted', p.slips.submittedBy ?? '—'],
            ['Approved (Marketing)', p.slips.approvedBy ?? '—'],
            ['Accounts', p.accountArns.join(', ') || '—'],
          ]}
        />
      </Card>
      <DocumentChecklist proposalId={p.id} />
    </div>
  );
}

/** Quotation slip (BRNB.008): insurers approached, numbers, approval and the PDF. */
export function QuotationSlipTab({ proposal: p }: Readonly<{ proposal: Proposal }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurers, setInsurers] = useState(p.insurers);
  const download = useDownload();
  const editable = SLIP_EDIT_STAGES.has(p.status) && can('TSU_PROCESS');
  const select = useMutation({
    mutationFn: () => proposalsApi.selectInsurers(p.id, insurers),
    onSuccess: async (saved) => {
      queryClient.setQueryData(['proposal', p.id], saved);
      toast.success(`${saved.insurers.length} insurer(s) selected for the quotation slip`);
      await queryClient.invalidateQueries({ queryKey: ['proposals'] });
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={select.error ?? download.error} />
      <Card
        title="Insurers approached"
        actions={
          editable && (
            <Button
              size="sm"
              variant="primary"
              icon={<Save size={14} />}
              busy={select.isPending}
              onClick={() => select.mutate()}
            >
              Save Selection
            </Button>
          )
        }
      >
        <InsurerChoices selected={insurers} onChange={setInsurers} disabled={!editable} />
      </Card>
      <Card
        title="Quotation slip"
        actions={
          p.slips.qsNo && (
            <Button
              size="sm"
              variant="secondary"
              icon={<FileText size={14} />}
              onClick={() => download.mutate(() => proposalsApi.quotationSlipPdf(p.id))}
            >
              PDF
            </Button>
          )
        }
      >
        <DetailList
          rows={[
            ['QS number', p.slips.qsNo ?? 'Numbered when submitted'],
            ['Template', p.slips.qsTemplate],
            ['Reply by', formatDate(p.slips.qsReplyBy) || '—'],
            ['Prepared by', p.slips.qsSubmittedBy],
            ['Approved by', p.slips.qsApprovedBy],
            ['Sent to insurers', formatDateTime(p.slips.qsSentAt) || '—'],
          ]}
        />
      </Card>
    </div>
  );
}

const COMPARE_COLUMNS = [
  {
    key: 'i',
    header: 'Insurer',
    render: (r: ComparativeRow) => (
      <span>
        <strong>{r.insurerName}</strong>
        {r.recommended && (
          <>
            {' '}
            <StatusBadge status="RECOMMENDED" />
          </>
        )}
      </span>
    ),
  },
  {
    key: 's',
    header: 'Response',
    render: (r: ComparativeRow) => <StatusBadge status={r.status} />,
  },
  {
    key: 'p',
    header: 'Premium',
    numeric: true,
    render: (r: ComparativeRow) => (
      <span className={r.lowest ? 'diff-added' : undefined}>
        <Amount value={r.premium} />
        {r.lowest && ' (lowest)'}
      </span>
    ),
  },
  { key: 'r', header: 'Rate %', numeric: true, render: (r: ComparativeRow) => r.rate ?? '' },
  { key: 'd', header: 'Deductibles', render: (r: ComparativeRow) => r.deductibles ?? '' },
  { key: 'c', header: 'Conditions', render: (r: ComparativeRow) => r.conditions ?? '' },
  { key: 'v', header: 'Valid until', render: (r: ComparativeRow) => formatDate(r.validUntil) },
  { key: 'm', header: 'Remarks', render: (r: ComparativeRow) => r.remarks ?? '' },
];

/** Comparative table (BRNB.010): insurers side by side, exported as PDF and Excel. */
export function ComparativeTab({ proposal }: Readonly<{ proposal: Proposal }>) {
  const table = useQuery({
    queryKey: ['proposal', proposal.id, 'comparative'],
    queryFn: () => proposalsApi.comparative(proposal.id),
    enabled: slipSent(proposal.status),
  });
  const download = useDownload();
  if (!slipSent(proposal.status)) {
    return (
      <Card title="Comparative table">
        <p className="muted">The table is compiled once the insurers answer the quotation slip.</p>
      </Card>
    );
  }
  return (
    <Card
      title="Comparative table"
      flush
      actions={
        <>
          <Button
            size="sm"
            variant="secondary"
            icon={<FileText size={14} />}
            onClick={() => download.mutate(() => proposalsApi.comparativePdf(proposal.id))}
          >
            PDF
          </Button>
          <Button
            size="sm"
            variant="secondary"
            icon={<FileSpreadsheet size={14} />}
            onClick={() => download.mutate(() => proposalsApi.comparativeXlsx(proposal.id))}
          >
            Excel
          </Button>
        </>
      }
    >
      <ErrorAlert error={table.error ?? download.error} />
      <DataTable<ComparativeRow>
        loading={table.isLoading}
        rows={table.data?.rows ?? []}
        rowKey={(r) => r.insurerCode}
        emptyMessage="No insurer response yet."
        columns={COMPARE_COLUMNS}
      />
    </Card>
  );
}

/** Proposal slip (BRNB.017): number, version, chosen insurer, approval and archived versions. */
export function ProposalSlipTab({ proposal: p }: Readonly<{ proposal: Proposal }>) {
  const download = useDownload();
  const archive = useQuery({
    queryKey: ['proposal', p.id, 'slips'],
    queryFn: () => attachmentsApi.list(PROPOSAL_ENTITY, String(p.id)),
  });
  const versions = (archive.data ?? []).filter((a) => a.documentType === 'PROPOSAL_SLIP');
  return (
    <div className="stack">
      <ErrorAlert error={download.error} />
      <Card
        title="Proposal slip"
        actions={
          p.slips.chosenInsurer && (
            <Button
              size="sm"
              variant="secondary"
              icon={<FileText size={14} />}
              onClick={() => download.mutate(() => proposalsApi.proposalSlipPdf(p.id))}
            >
              Current PDF
            </Button>
          )
        }
      >
        <DetailList
          rows={[
            ['PS number', p.slips.psNo ?? 'Numbered when submitted'],
            ['Version', p.slips.psVersion > 0 ? String(p.slips.psVersion) : '—'],
            ['Chosen insurer', p.slips.chosenInsurer],
            ['Prepared by', p.slips.psSubmittedBy],
            ['Approved by', p.slips.psApprovedBy],
            ['Sent to client', formatDateTime(p.slips.sentAt) || '—'],
            ['Accepted', formatDateTime(p.slips.acceptedAt) || '—'],
          ]}
        />
      </Card>
      <Card title="Archived versions" flush>
        <DataTable
          loading={archive.isLoading}
          rows={versions}
          rowKey={(a) => a.id}
          emptyMessage="No proposal slip archived yet."
          columns={[
            { key: 'f', header: 'File', render: (a) => a.fileName },
            { key: 'd', header: 'Description', render: (a) => a.description ?? '' },
            {
              key: 'u',
              header: 'Archived',
              render: (a) => `${a.uploadedBy} ${formatDateTime(a.uploadedAt)}`,
            },
            {
              key: 'x',
              header: 'Download',
              render: (a) => (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => download.mutate(() => attachmentsApi.download(a.id))}
                >
                  {humanize('DOWNLOAD')}
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

/** Status history of the PRF's work case. */
export function HistoryTab({ proposalId }: Readonly<{ proposalId: number }>) {
  const detail = useQuery({
    queryKey: workflowKey(PROPOSAL_ENTITY, proposalId),
    queryFn: () => workflowApi.byRecord(PROPOSAL_ENTITY, proposalId),
  });
  return (
    <Card title="History">
      <StageTimeline history={detail.data?.history ?? []} />
    </Card>
  );
}
