import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { CheckCheck, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { remittanceApi } from './api';
import { BATCH_COLUMNS } from './batchColumns';
import { BATCH_TABS, batchTabOf, stagesOf, TYPE_LABELS } from './remittanceLabels';
import type { BatchTab } from './remittanceLabels';
import './remittance.css';
import { ConfirmButton } from '@/components/ui/ConfirmButton';

function Filters({
  insurer,
  type,
  onChange,
}: Readonly<{
  insurer: string;
  type: string;
  onChange: (insurer: string, type: string) => void;
}>) {
  return (
    <div className="worklist-filters remit-form">
      <Field label="Insurer">
        {(id) => (
          <InsurerSelect
            id={id}
            value={insurer}
            placeholder="All insurers"
            onChange={(code) => onChange(code, type)}
          />
        )}
      </Field>
      <Field label="Remittance Type">
        {(id) => (
          <select
            id={id}
            className="select"
            value={type}
            onChange={(e) => onChange(insurer, e.target.value)}
          >
            <option value="">All types</option>
            {Object.entries(TYPE_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

/** The bulk action of a tab for the user: submit in review, approve for approval. */
function bulkActionOf(
  tab: BatchTab,
  can: (permission: string) => boolean,
): 'submit' | 'approve' | undefined {
  if (tab === 'REVIEW' && can('REMIT_PROCESS')) {
    return 'submit';
  }
  return tab === 'APPROVAL' && can('REMIT_APPROVE') ? 'approve' : undefined;
}

/** Submits or approves the selected batches one by one (RMTID.010). */
function useBulk(reset: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ ids, approve }: { ids: number[]; approve: boolean }) => {
      for (const id of ids) {
        await (approve ? remittanceApi.approve(id) : remittanceApi.submit(id));
      }
      return { count: ids.length, approve };
    },
    onSuccess: async ({ count, approve }) => {
      reset();
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${count} batch(es) ${approve ? 'approved' : 'submitted for approval'}`);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['remittance', 'batches'] }),
  });
}

/**
 * Remittance batches (RMTID.007-011/024/027): the Process Remittance queues by stage, from review
 * to the insurer OR. Processors submit the batches they reviewed; team leaders approve those for
 * approval and push them to Disbursement. Open a batch to exclude accounts and see its documents.
 */
export default function BatchesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<BatchTab>(() => batchTabOf(params.get('tab')));
  const [query, setQuery] = useState('');
  const [insurer, setInsurer] = useState('');
  const [type, setType] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const selection = useRowSelection();
  const bulk = useBulk(selection.clear);
  const rows = useQuery({
    queryKey: ['remittance', 'batches', companyId, tab, query, insurer, type, page],
    queryFn: () =>
      remittanceApi.batches(
        companyId,
        stagesOf(BATCH_TABS, tab),
        { q: query, insurer, type },
        page,
      ),
    enabled: companyId > 0,
  });
  const content = rows.data?.content ?? [];
  const bulkAction = bulkActionOf(tab, can);
  const ids = content.filter((b) => selection.has(String(b.id))).map((b) => b.id);
  const columns =
    bulkAction !== undefined
      ? [
          selectionColumn(
            content,
            (b) => String(b.id),
            selection,
            (b) => b.batchNo,
          ),
          ...BATCH_COLUMNS,
        ]
      : BATCH_COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Remittance Batches"
        description="Process Remittance: review, submit, approve and follow the batches to Disbursement."
      />
      <ErrorAlert error={rows.error ?? bulk.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={BATCH_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Batch or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
            filters={{ open: filtersOpen, onToggle: () => setFiltersOpen(!filtersOpen) }}
          >
            {bulkAction !== undefined && (
              <ConfirmButton
                icon={bulkAction === 'approve' ? <CheckCheck size={16} /> : <Send size={16} />}
                disabled={ids.length === 0}
                busy={bulk.isPending}
                confirm={{
                  title:
                    bulkAction === 'approve'
                      ? `Approve ${String(ids.length)} Batch(es)`
                      : `Submit ${String(ids.length)} Batch(es)`,
                  effect:
                    'Each selected batch is processed on its own; a batch that cannot be processed is reported.',
                }}
                onConfirm={() => bulk.mutateAsync({ ids, approve: bulkAction === 'approve' })}
              >
                {bulkAction === 'approve' ? 'Approve Selected' : 'Submit Selected'}
              </ConfirmButton>
            )}
          </WorklistToolbar>
          {filtersOpen && (
            <Filters
              insurer={insurer}
              type={type}
              onChange={(i, t) => {
                setInsurer(i);
                setType(t);
                setPage(0);
              }}
            />
          )}
          <DataTable
            caption="Remittance batches"
            columns={columns}
            rows={content}
            rowKey={(b) => b.id}
            loading={rows.isLoading}
            onRowClick={(b) => void navigate(`/remittance/batches/${b.id}`)}
          />
          <PageFooter data={rows.data} noun="batches" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
