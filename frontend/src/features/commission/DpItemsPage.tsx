import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
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
import { commissionApi } from './commissionApi';
import type { DpItem, DpTag } from './commissionApi';
import { bulkActions, failedRules, TAG_TABS, withCount } from './commissionLogic';
import { DpItemDialog, ReasonDialog } from './DpItemDialogs';

function columnsOf(rows: DpItem[], selection: RowSelection | undefined): Column<DpItem>[] {
  return [
    ...(selection === undefined
      ? []
      : [
          selectionColumn(
            rows,
            (r) => String(r.id),
            selection,
            (r) => r.invoiceNo,
          ),
        ]),
    {
      key: 'invoice',
      header: 'Invoice No.',
      render: (r) => (
        <>
          <strong>{r.invoiceNo}</strong>
          <div className="muted">{r.policyNo}</div>
        </>
      ),
    },
    { key: 'insurer', header: 'Insurer', render: (r) => r.insurerCode ?? '' },
    { key: 'assured', header: 'Assured', render: (r) => r.assuredName ?? '' },
    { key: 'branch', header: 'Branch', render: (r) => r.branchCode ?? '' },
    {
      key: 'premium',
      header: 'Premium',
      numeric: true,
      render: (r) => <Amount value={r.amounts.premium} />,
    },
    {
      key: 'net',
      header: 'Net Commission',
      numeric: true,
      render: (r) => <Amount value={r.amounts.net} />,
    },
    { key: 'issues', header: 'Validation', render: (r) => failedRules(r) || 'All rules passed' },
    { key: 'sanitation', header: 'Result', render: (r) => <StatusBadge status={r.sanitation} /> },
  ];
}

function listIdOf(value: string | null): number | undefined {
  return value === null ? undefined : Number(value);
}

function describe(listId: number | undefined): string {
  return listId === undefined
    ? 'Direct payment accounts from confirmation to billing, collection and PR reversal.'
    : `Accounts of DP list ${String(listId)}.`;
}

function BulkButtons({
  tag,
  count,
  busy,
  onExclude,
  onConfirm,
  onBill,
}: Readonly<{
  tag: DpTag;
  count: number;
  busy: boolean;
  onExclude: () => void;
  onConfirm: () => void;
  onBill: () => void;
}>) {
  const allowed = bulkActions(tag);
  const none = count === 0;
  return (
    <>
      {allowed.exclude && (
        <Button variant="secondary" disabled={none} onClick={onExclude}>
          Exclude
        </Button>
      )}
      {allowed.confirm && (
        <Button disabled={none} busy={busy} onClick={onConfirm}>
          Confirm Paid to Insurer
        </Button>
      )}
      {allowed.bill && (
        <Button disabled={none} busy={busy} onClick={onBill}>
          Prepare Billing
        </Button>
      )}
    </>
  );
}

type Dialog = { kind: 'item'; item: DpItem } | { kind: 'exclude' };

interface Act {
  mutate: (v: { run: () => Promise<unknown>; done: string }) => void;
  isPending: boolean;
  error: unknown;
}

function ItemsDialogs({
  dialog,
  ids,
  editable,
  act,
  onClose,
}: Readonly<{
  dialog: Dialog | undefined;
  ids: number[];
  editable: boolean;
  act: Act;
  onClose: () => void;
}>) {
  const item = dialog?.kind === 'item' ? dialog.item : undefined;
  return (
    <>
      {item !== undefined && (
        <DpItemDialog
          item={item}
          editable={editable}
          busy={act.isPending}
          error={act.error}
          onClose={onClose}
          onRevalidate={() =>
            act.mutate({ run: () => commissionApi.revalidate(item.id), done: 'Validated again' })
          }
          onReverse={() =>
            act.mutate({
              run: () => commissionApi.reverse(item.id),
              done: 'Premium receivable reversed',
            })
          }
          onReinstate={(reason, comment) =>
            act.mutate({
              run: () => commissionApi.reinstate(item.id, reason, comment),
              done: 'Premium receivable reinstated',
            })
          }
        />
      )}
      {dialog?.kind === 'exclude' && (
        <ReasonDialog
          title={`Exclude ${String(ids.length)} Account(s)`}
          action="Exclude"
          label="Reason"
          busy={act.isPending}
          error={act.error}
          onClose={onClose}
          onConfirm={(reason) =>
            act.mutate({
              run: () => commissionApi.exclude(ids, reason),
              done: `${String(ids.length)} account(s) excluded`,
            })
          }
        />
      )}
    </>
  );
}

/**
 * Direct payment accounts (CMRID.007-013): each account of the lists by stage, from confirmation
 * that the client paid the insurer to billing, the insurer answer and collection.
 */
export default function DpItemsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const listId = listIdOf(params.get('listId'));
  const selection = useRowSelection();
  const [tag, setTag] = useState<DpTag>('DP_FOR_CONFIRMATION');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<Dialog>();
  const counts = useQuery({
    queryKey: ['commission', 'counts', companyId],
    queryFn: () => commissionApi.counts(companyId),
    enabled: companyId > 0,
  });
  const items = useQuery({
    queryKey: ['commission', 'items', companyId, tag, q, listId, page],
    queryFn: () => commissionApi.items(companyId, { tag: [tag], q: q || undefined, listId }, page),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (v: { run: () => Promise<unknown>; done: string }) => v.run().then(() => v.done),
    onSuccess: async (done) => {
      setDialog(undefined);
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['commission'] });
      toast.success(done);
    },
  });
  const editable = can('COMMREC_PROCESS');
  const ids = selection.keys.map(Number);
  const prepare = useMutation({
    mutationFn: () => commissionApi.prepare(companyId, ids),
    onSuccess: async (billings) => {
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['commission'] });
      toast.success(`${String(billings.length)} billing(s) prepared`);
      if (billings.length === 1 && billings[0] !== undefined) {
        void navigate(`/commission/dp/billings/${String(billings[0].id)}`);
      }
    },
  });
  const rows = items.data?.content ?? [];
  const tabs = TAG_TABS.map((t) => ({ id: t.id, label: withCount(t.label, counts.data?.[t.id]) }));
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="DP Accounts"
        description={describe(listId)}
      />
      <ErrorAlert
        error={items.error ?? prepare.error ?? (dialog?.kind === 'item' ? undefined : act.error)}
      />
      <Card>
        <div className="stack">
          <Tabs
            tabs={tabs}
            active={tag}
            onChange={(t) => {
              setTag(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Invoice, Policy or Assured"
            onSearch={(text) => {
              setQ(text.trim());
              setPage(0);
            }}
          >
            {editable && (
              <BulkButtons
                tag={tag}
                count={ids.length}
                busy={act.isPending || prepare.isPending}
                onExclude={() => setDialog({ kind: 'exclude' })}
                onConfirm={() =>
                  act.mutate({
                    run: () => commissionApi.confirm(ids),
                    done: `${String(ids.length)} account(s) confirmed for billing`,
                  })
                }
                onBill={() => prepare.mutate()}
              />
            )}
          </WorklistToolbar>
          <DataTable
            caption="Direct payment accounts"
            columns={columnsOf(rows, editable ? selection : undefined)}
            rows={rows}
            rowKey={(r) => r.id}
            loading={items.isLoading}
            onRowClick={(r) => setDialog({ kind: 'item', item: r })}
            emptyMessage="No items to display"
          />
          <PageFooter data={items.data} noun="accounts" onPage={setPage} />
        </div>
      </Card>
      <ItemsDialogs
        dialog={dialog}
        ids={ids}
        editable={editable}
        act={act}
        onClose={() => setDialog(undefined)}
      />
    </div>
  );
}
