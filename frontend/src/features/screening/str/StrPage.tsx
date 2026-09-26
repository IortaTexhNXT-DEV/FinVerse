import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileOutput } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { strApi } from './api';
import type { StrExtraction, StrRow, StrStatus } from './api';
import { ExtractDialog } from './StrDialogs';

type RegisterTab = StrStatus | 'ALL';

const TABS: readonly { id: RegisterTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'DRAFT', label: 'Draft' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'APPROVED', label: 'Approved' },
  { id: 'EXTRACTED', label: 'Extracted' },
  { id: 'FILED', label: 'Filed' },
];

function Extractions({ canDownload }: Readonly<{ canDownload: boolean }>) {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const [page, setPage] = useState(0);
  const extractions = useQuery({
    queryKey: ['screening', 'str', 'extractions', companyId, page],
    queryFn: () => strApi.extractions(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <Card flush title="Extractions">
      <ErrorAlert error={extractions.error ?? download.error} />
      <DataTable<StrExtraction>
        caption="STR extractions"
        rows={extractions.data?.content ?? []}
        rowKey={(x) => x.id}
        loading={extractions.isLoading}
        emptyMessage="No extraction yet"
        columns={[
          {
            key: 'batch',
            header: 'Batch',
            render: (x) => <span className="mono">{x.batchNo}</span>,
          },
          {
            key: 'period',
            header: 'Period',
            render: (x) => `${formatDate(x.periodFrom)} – ${formatDate(x.periodTo)}`,
          },
          { key: 'count', header: 'STRs', numeric: true, render: (x) => x.strCount },
          {
            key: 'file',
            header: 'File',
            render: (x) => (
              <>
                {x.fileName}
                <span className="cell-sub mono">SHA-256 {x.sha256.slice(0, 16)}…</span>
              </>
            ),
          },
          {
            key: 'by',
            header: 'Extracted',
            render: (x) => (
              <>
                {formatDateTime(x.extractedAt)}
                <span className="cell-sub">
                  {x.extractedBy}
                  {x.reExtraction ? ` · re-extraction: ${x.reason ?? ''}` : ''}
                </span>
              </>
            ),
          },
          {
            key: 'download',
            header: 'Download',
            render: (x) =>
              canDownload ? (
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Download ${x.fileName}`}
                  icon={<Download size={14} />}
                  onClick={() => download.mutate(() => strApi.file(x.id))}
                />
              ) : (
                '—'
              ),
          },
        ]}
      />
      <PageFooter data={extractions.data} noun="extractions" onPage={setPage} />
    </Card>
  );
}

/**
 * STR register (SNSRP-705, 706; FR-SS-071, 072): the STRs by status with their committee
 * decision, extraction and AMLC reference, "Extract Approved STRs" for a period and the extraction
 * files. Filing references are recorded on the STR tab of the case.
 */
export default function StrPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [tab, setTab] = useState<RegisterTab>('ALL');
  const [page, setPage] = useState(0);
  const [extracting, setExtracting] = useState(false);
  const register = useQuery({
    queryKey: ['screening', 'str', 'register', companyId, tab, page],
    queryFn: () => strApi.register({ companyId, status: tab === 'ALL' ? undefined : tab, page }),
    enabled: companyId > 0,
  });
  const canExtract = can('SCR_STR_EXTRACT');
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Sanction Screening"
        title="STR"
        description="Suspicious transaction reports prepared from the screening cases: extract the AML Committee-approved STRs in the AMLC format and record the AMLC reference after filing on the portal."
        actions={
          canExtract ? (
            <Button
              variant="accent"
              icon={<FileOutput size={16} />}
              onClick={() => setExtracting(true)}
            >
              Extract Approved STRs
            </Button>
          ) : undefined
        }
      />
      <Tabs
        tabs={TABS}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setPage(0);
        }}
      />
      <Card flush>
        <ErrorAlert error={register.error} />
        <DataTable<StrRow>
          caption="STR register"
          rows={register.data?.content ?? []}
          rowKey={(s) => s.id}
          loading={register.isLoading}
          onRowClick={(s) => void navigate(`/screening/cases/${s.caseId}?tab=str`)}
          emptyMessage="No STR in this status"
          columns={[
            {
              key: 'no',
              header: 'STR No.',
              render: (s) => <span className="mono">{s.strNo}</span>,
            },
            {
              key: 'subject',
              header: 'Subject',
              render: (s) => (
                <>
                  <strong>{s.subjectName}</strong>
                  <span className="cell-sub mono">{s.subjectCode}</span>
                </>
              ),
            },
            { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
            {
              key: 'committee',
              header: 'Committee Decision',
              render: (s) => formatDateTime(s.committeeDecidedAt),
            },
            { key: 'extracted', header: 'Extracted', render: (s) => formatDate(s.extractedAt) },
            {
              key: 'amlc',
              header: 'AMLC Reference / Filed On',
              render: (s) => (
                <>
                  {s.amlcReference ?? '—'}
                  <span className="cell-sub">{formatDate(s.filedOn)}</span>
                </>
              ),
            },
            { key: 'by', header: 'Prepared By', render: (s) => s.createdBy },
          ]}
        />
        <PageFooter data={register.data} noun="STRs" onPage={setPage} />
      </Card>
      <Extractions canDownload={canExtract} />
      {extracting && (
        <ExtractDialog
          onClose={() => setExtracting(false)}
          onDone={(x) => {
            setExtracting(false);
            toast.success(`${x.batchNo}: ${x.strCount} STR(s) extracted to ${x.fileName}`);
            void queryClient.invalidateQueries({ queryKey: ['screening', 'str'] });
          }}
        />
      )}
    </div>
  );
}
