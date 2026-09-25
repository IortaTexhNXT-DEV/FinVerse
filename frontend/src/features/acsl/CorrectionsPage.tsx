import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { CORRECTION_TABS, tabParam, optionalText } from './acsl';
import type { CorrectionTab } from './acsl';
import { acslApi } from './api';
import type { CorrectionStage, CorrectionSummary, Counts } from './api';
import { FormDialog } from './FormDialog';

const KINDS = [
  { value: 'WRONG_ACCOUNT', label: 'Posting to a wrong GL account' },
  { value: 'AMOUNT', label: 'Wrong amount' },
  { value: 'RECLASS', label: 'Reclassification' },
  { value: 'OTHER', label: 'Other correction' },
];

const COLUMNS: Column<CorrectionSummary>[] = [
  {
    key: 'no',
    header: 'Correction No.',
    render: (c) => (
      <>
        <strong>{c.correctionNo}</strong>
        <span className="cell-sub">{formatDate(c.createdAt)}</span>
      </>
    ),
  },
  { key: 'kind', header: 'Kind', render: (c) => humanize(c.kind) },
  { key: 'invoice', header: 'Invoice', render: (c) => c.invoiceNo ?? '—' },
  { key: 'description', header: 'Description', render: (c) => c.description },
  { key: 'journal', header: 'Journal', render: (c) => c.journalBatchNo ?? '—' },
  { key: 'by', header: 'Raised By', render: (c) => c.createdBy },
  { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.stage} /> },
];

function tabsWithCounts(counts: Counts<CorrectionStage> | undefined) {
  return CORRECTION_TABS.map((t) => {
    const count = t.id === 'ALL' ? undefined : counts?.[t.id];
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

/**
 * Correction entries (ACSL 2.7-2.15, 2.9.1): the list by stage, searchable by correction or
 * invoice, and the team leader's New Correction for errors found outside a case.
 */
export default function CorrectionsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = tabParam<CorrectionTab>(params.get('stage'), CORRECTION_TABS, 'ALL');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const stage = tab === 'ALL' ? undefined : tab;
  const list = useQuery({
    queryKey: ['acsl', 'corrections', companyId, stage, q, page],
    queryFn: () => acslApi.corrections(companyId, { stage, q, page }),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['acsl', 'correctionCounts', companyId],
    queryFn: () => acslApi.correctionCounts(companyId),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (v: Record<string, string>) =>
      acslApi.createCorrection(companyId, {
        kind: v.kind ?? 'OTHER',
        invoiceNo: optionalText(v.invoiceNo),
        originalBatchNo: optionalText(v.originalBatchNo),
        description: (v.description ?? '').trim(),
      }),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      toast.success(`${c.correctionNo} created`);
      void navigate(`/acsl/corrections/${String(c.id)}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · ACSL"
        title="Correction Entries"
        description="Linked correction entries that reverse a wrong posting and post the right one, reviewed and approved before they reach the ledger."
        actions={
          can('ACSL_ASSIGN') && (
            <Button
              variant="primary"
              icon={<FilePlus2 size={16} />}
              onClick={() => setCreating(true)}
            >
              New Correction
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<CorrectionTab>
            tabs={tabsWithCounts(counts.data)}
            active={tab}
            onChange={(next) => {
              setPage(0);
              setParams(next === 'ALL' ? {} : { stage: next });
            }}
          />
        </div>
        <WorklistToolbar
          placeholder="Search Correction or Invoice No."
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
        />
        <DataTable
          caption="Correction entries"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(c) => c.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
          onRowClick={(c) => void navigate(`/acsl/corrections/${String(c.id)}`)}
        />
        <PageFooter data={list.data} noun="corrections" onPage={setPage} />
      </Card>
      {creating && (
        <FormDialog
          title="New Correction"
          confirmLabel="Create Correction"
          fields={[
            { key: 'kind', label: 'Correction Kind', required: true, options: KINDS },
            { key: 'invoiceNo', label: 'Invoice No.', required: true },
            {
              key: 'originalBatchNo',
              label: 'Journal to Correct',
              hint: 'Journal batch no., if known',
            },
            { key: 'description', label: 'Description', required: true },
          ]}
          busy={create.isPending}
          error={create.error}
          onConfirm={(v) => create.mutate(v)}
          onClose={() => setCreating(false)}
        />
      )}
    </div>
  );
}
