import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Plus, UserCheck } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { glApi } from '@/api/gl';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { AssignJournalsDialog } from './AssignJournalsDialog';
import { ConfirmPostingDialog } from './ConfirmPostingDialog';
import { JournalFiltersCard } from './JournalFiltersCard';
import type { ListFilters } from './JournalFiltersCard';
import { glPlatformApi } from './glPlatformApi';
import type { BulkPostOutcome, FrbsJournal } from './glPlatformApi';

type Dialog = 'assign' | 'post' | null;

/**
 * Journal inquiry and authorization queue (FRBS 2.5.x): filter on "Pending approval" or on the
 * entries assigned to me, assign entries to a poster (TL) and post several at once, each on its
 * own with the full controls.
 */
export default function JournalsPage() {
  const companyId = useCompanyId();
  const { branchId } = useWorkspace();
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [filters, setFilters] = useState<ListFilters>({ page: 0 });
  const [dialog, setDialog] = useState<Dialog>(null);
  const [outcomes, setOutcomes] = useState<BulkPostOutcome[]>([]);

  const query = useQuery({
    queryKey: ['journals', companyId, branchId, filters],
    queryFn: () => glApi.journals({ ...filters, companyId, branchId, size: 25 }),
    enabled: companyId > 0,
  });
  const set = (patch: Partial<ListFilters>) => {
    selection.clear();
    setFilters((f) => ({ ...f, ...patch, page: 0 }));
  };
  const rows = (query.data?.content ?? []) as FrbsJournal[];
  const selected = rows.filter((j) => selection.has(String(j.id)));
  const pending = selected.filter((j) => j.status === 'PENDING_APPROVAL');
  const done = async (message: string) => {
    setDialog(null);
    selection.clear();
    await queryClient.invalidateQueries({ queryKey: ['journals'] });
    toast.success(message);
  };
  const post = useMutation({
    mutationFn: () => glPlatformApi.bulkApprove(pending.map((j) => j.id)),
    onSuccess: async (result) => {
      setOutcomes(result.filter((o) => !o.posted));
      await done(`${result.filter((o) => o.posted).length} of ${result.length} journal(s) posted`);
    },
  });
  const assign = useMutation({
    mutationFn: (assignee: string) =>
      glPlatformApi.assign(
        selected.map((j) => j.id),
        assignee,
      ),
    onSuccess: (result) => done(`${result.length} journal(s) assigned`),
  });

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Journals"
        description="Search vouchers, follow their approval status, assign entries for posting and post several at once."
        actions={
          can('JOURNAL_CREATE') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/gl/journals/new')}
            >
              New Journal
            </Button>
          )
        }
      />
      <JournalFiltersCard
        filters={filters}
        username={user?.username ?? ''}
        onChange={set}
        actions={
          <>
            {can('JOURNAL_ASSIGN') && (
              <Button
                variant="secondary"
                icon={<UserCheck size={16} />}
                disabled={selected.length === 0}
                onClick={() => setDialog('assign')}
              >
                Assign ({selected.length})
              </Button>
            )}
            {can('JOURNAL_AUTHORIZE') && (
              <Button
                variant="accent"
                icon={<CheckCircle2 size={16} />}
                disabled={pending.length === 0}
                onClick={() => setDialog('post')}
              >
                Post Selected ({pending.length})
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={query.error} />
      {outcomes.length > 0 && (
        <div className="alert warning" role="status">
          Not posted:
          <ul>
            {outcomes.map((o) => (
              <li key={o.id}>
                Journal {o.id}: {o.message}
              </li>
            ))}
          </ul>
        </div>
      )}
      <Card flush>
        <DataTable<FrbsJournal>
          loading={query.isLoading}
          rows={rows}
          rowKey={(j) => j.id}
          onRowClick={(j) => void navigate(`/gl/journals/${j.id}`)}
          caption="Journals"
          emptyMessage="No journal matches these filters"
          columns={[
            selectionColumn(
              rows,
              (j) => String(j.id),
              selection,
              (j) => j.batchNo,
            ),
            { key: 'no', header: 'Batch No.', render: (j) => <strong>{j.batchNo}</strong> },
            { key: 'date', header: 'Value Date', render: (j) => formatDate(j.valueDate) },
            { key: 'type', header: 'Type', render: (j) => j.journalType },
            { key: 'nar', header: 'Narration', render: (j) => j.narration },
            { key: 'by', header: 'Inputter', render: (j) => j.createdBy },
            { key: 'to', header: 'Assigned To', render: (j) => j.assignedTo ?? '' },
            { key: 'auth', header: 'Authorizer', render: (j) => j.authorizedBy ?? '' },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (j) => <Amount value={j.totalDebit} />,
            },
            { key: 'st', header: 'Status', render: (j) => <StatusBadge status={j.status} /> },
          ]}
        />
        <PageFooter
          data={query.data}
          onPage={(p) => {
            selection.clear();
            setFilters((f) => ({ ...f, page: p }));
          }}
        />
      </Card>
      <AssignJournalsDialog
        open={dialog === 'assign'}
        count={selected.length}
        busy={assign.isPending}
        error={assign.error}
        onAssign={(assignee) => assign.mutate(assignee)}
        onClose={() => setDialog(null)}
      />
      <ConfirmPostingDialog
        open={dialog === 'post'}
        title="Post selected journals"
        intro="Each journal is checked and posted on its own; a journal that fails its checks is left pending and reported."
        facts={[
          { label: 'Journals to post', value: pending.length },
          {
            label: 'Total debit',
            value: <Amount value={pending.reduce((sum, j) => sum + j.totalDebit, 0)} />,
          },
          { label: 'Selected but not pending', value: selected.length - pending.length },
        ]}
        confirmLabel="Post Journals"
        busy={post.isPending}
        error={post.error}
        onConfirm={() => post.mutate()}
        onClose={() => setDialog(null)}
      />
    </div>
  );
}
