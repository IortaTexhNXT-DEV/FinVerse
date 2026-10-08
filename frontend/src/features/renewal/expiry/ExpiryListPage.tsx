import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarPlus, Send, UserPlus } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { ExtractionRunView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf, formatDateTime, formatPeriod } from '@/utils/format';
import { AssignDialog, TransferDialog } from '../common/ActionDialogs';
import { CandidateList } from '../common/CandidateList';
import { ExtractDialog } from '../common/MoreDialogs';
import { EXPIRY_TABS, RENEWAL_SECTION, tabOf } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

type Open = 'assign' | 'transfer' | 'extract';

function RunsCard() {
  const companyId = useCompanyId();
  const runs = useQuery({
    queryKey: ['renewal', 'runs', companyId],
    queryFn: () => renewalApi.runs(companyId),
    enabled: companyId > 0,
  });
  return (
    <Card title="Recent extractions" flush>
      <DataTable<ExtractionRunView>
        loading={runs.isLoading}
        rows={(runs.data?.content ?? []).slice(0, 5)}
        rowKey={(r) => r.runNo}
        emptyMessage="No extraction yet"
        columns={[
          { key: 'no', header: 'Run', kind: 'code', render: (r) => r.runNo },
          {
            key: 'range',
            header: 'Expiry range',
            render: (r) => (r.expiryFrom ? formatPeriod(r.expiryFrom, r.expiryTo) : 'Daily run'),
          },
          { key: 'by', header: 'By', render: (r) => <UserName login={r.requestedBy} /> },
          { key: 'read', header: 'Read', kind: 'amount', render: (r) => r.counts.read },
          { key: 'new', header: 'New', kind: 'amount', render: (r) => r.counts.created },
          { key: 'urgent', header: 'Urgent', kind: 'amount', render: (r) => r.counts.urgent },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (r) => <StatusBadge status={r.status} />,
          },
          {
            key: 'at',
            header: 'Started',
            kind: 'datetime',
            render: (r) => formatDateTime(r.startedAt),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Expiry List (FR-RN-011-015, 030, 031): the expiring accounts by tab, Generate Expiry
 * List for a range, Initiate the extracted renewals, Assign Disposition to an officer, Push the
 * dispositioned renewals and Transfer to another unit.
 */
export default function ExpiryListPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const initiate = useBatchAction<string[]>(
    'Initiate renewals',
    'initiated',
    async (r) => {
      const result = await renewalApi.initiate(companyId, r);
      return { done: result.initiated, refused: result.refused };
    },
    done,
  );
  const assign = useBatchAction<{ ao: string; reason: string }>(
    'Assign Disposition',
    'assigned',
    ({ ao, reason }) => renewalApi.assign(companyId, refs, ao, reason || undefined),
    done,
  );
  const push = useBatchAction<string[]>(
    'Push',
    'pushed',
    (r) => renewalApi.push(companyId, r),
    done,
  );
  const transfer = useMutation({
    mutationFn: (input: { toUnit: string; reasonCode: string; remarks: string }) =>
      renewalApi.requestTransfer(companyId, {
        renewalRef: refs[0] ?? '',
        toUnit: input.toUnit,
        reasonCode: input.reasonCode || undefined,
        remarks: input.remarks,
      }),
    onSuccess: async () => {
      done();
      toast.success('Transfer requested');
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const extract = useMutation({
    mutationFn: (range: { from: string; to: string }) =>
      renewalApi.extract(companyId, range.from, range.to),
    onSuccess: async (run) => {
      close();
      toast.success(`${countOf(run.counts.created, 'renewal')} extracted`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Expiry List"
        description="Accounts expiring within the renewal horizon, from extraction to disposition."
        actions={
          can('RNW_EXTRACT') && (
            <Button icon={<CalendarPlus size={16} />} onClick={() => show('extract')}>
              Generate Expiry List
            </Button>
          )
        }
      />
      <CandidateList
        tabs={EXPIRY_TABS}
        initialTab={tabOf(EXPIRY_TABS, params.get('tab'))}
        actions={(selected, selection, tab) => (
          <span className="rnw-actions">
            {tab === 'EXTRACTED' && (can('RNW_EXTRACT') || can('RNW_ASSIGN')) && (
              <Button
                disabled={selected.length === 0}
                busy={initiate.mutation.isPending}
                onClick={() => initiate.mutation.mutate(selected, { onSuccess: selection.clear })}
              >
                Initiate
              </Button>
            )}
            {can('RNW_ASSIGN') && (
              <Button
                variant="secondary"
                icon={<UserPlus size={16} />}
                disabled={selected.length === 0}
                onClick={() => show('assign', selected, selection.clear)}
              >
                Assign Disposition
              </Button>
            )}
            {can('RNW_DISPOSE') && (
              <Button
                variant="secondary"
                icon={<Send size={16} />}
                disabled={selected.length === 0}
                busy={push.mutation.isPending}
                onClick={() => push.mutation.mutate(selected, { onSuccess: selection.clear })}
              >
                Push
              </Button>
            )}
            {can('RNW_ASSIGN') && (
              <Button
                variant="secondary"
                disabled={selected.length !== 1}
                title="Select one renewal"
                onClick={() => show('transfer', selected, selection.clear)}
              >
                Transfer
              </Button>
            )}
          </span>
        )}
      />
      {can('RNW_EXTRACT') && <RunsCard />}
      {open === 'extract' && (
        <ExtractDialog
          busy={extract.isPending}
          error={extract.error}
          onClose={close}
          onConfirm={(from, to) => extract.mutate({ from, to })}
        />
      )}
      {open === 'assign' && (
        <AssignDialog
          count={refs.length}
          busy={assign.mutation.isPending}
          error={assign.mutation.error}
          onClose={close}
          onConfirm={(ao, reason) => assign.mutation.mutate({ ao, reason })}
        />
      )}
      {open === 'transfer' && (
        <TransferDialog
          busy={transfer.isPending}
          error={transfer.error}
          onClose={close}
          onConfirm={(toUnit, reasonCode, remarks) =>
            transfer.mutate({ toUnit, reasonCode, remarks })
          }
        />
      )}
      {initiate.dialog}
      {assign.dialog}
      {push.dialog}
    </div>
  );
}
