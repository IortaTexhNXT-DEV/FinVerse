import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { ItemFilters } from './ItemFilters';
import type { ItemExtraFilters } from './ItemFilters';
import { ItemReviewDialog } from './ItemReviewDialog';
import { prodreconApi } from './prodreconApi';
import type { Bucket, ReconCycle, ReconFeedback, ReconItem } from './prodreconApi';
import { bucketTabs, mayPair, maySplit } from './prodreconLogic';

function BulkDialog({
  count,
  busy,
  onClose,
  onSave,
}: Readonly<{
  count: number;
  busy: boolean;
  onClose: () => void;
  onSave: (companyConcerned: string, disposition: string, forClosure: boolean) => void;
}>) {
  const [company, setCompany] = useState('');
  const [disposition, setDisposition] = useState('');
  const [forClosure, setForClosure] = useState(false);
  const [error, setError] = useState<string>();
  return (
    <Modal
      title={`Set Disposition of ${String(count)} Item(s)`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() =>
              disposition === ''
                ? setError('Disposition is required')
                : onSave(company, disposition, forClosure)
            }
          >
            Apply to Selected
          </Button>
        </>
      }
    >
      <div className="stack">
        <Field label="Company Concerned">
          {(id) => (
            <LovSelect
              id={id}
              type="RECON_COMPANY_CONCERNED"
              value={company}
              onChange={setCompany}
            />
          )}
        </Field>
        <Field label="Disposition" required error={error}>
          {(id) => (
            <LovSelect
              id={id}
              type="RECON_DISPOSITION"
              value={disposition}
              onChange={(v) => {
                setDisposition(v);
                setError(undefined);
              }}
            />
          )}
        </Field>
        <label className="row">
          <input
            type="checkbox"
            checked={forClosure}
            onChange={(e) => setForClosure(e.target.checked)}
          />{' '}
          Ready for Closure
        </label>
      </div>
    </Modal>
  );
}

function PairDialog({
  cycleId,
  item,
  busy,
  onClose,
  onPair,
}: Readonly<{
  cycleId: number;
  item: ReconItem;
  busy: boolean;
  onClose: () => void;
  onPair: (insurerItemId: number) => void;
}>) {
  const [choice, setChoice] = useState('');
  const candidates = useQuery({
    queryKey: ['prodrecon', 'items', cycleId, 'INSURER_ONLY', 'pair'],
    queryFn: () => prodreconApi.items(cycleId, { bucket: 'INSURER_ONLY', page: 0 }),
  });
  return (
    <Modal
      title={`Pair ${item.invoiceNo ?? ''} with an Insurer Line`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={choice === ''} onClick={() => onPair(Number(choice))}>
            Pair Items
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={candidates.error} />
        <Field
          label="Insurer Line"
          required
          hint="Lines of this cycle the insurer sent without a BDOI booking"
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={choice}
              onChange={(e) => setChoice(e.target.value)}
            >
              <option value="">Select…</option>
              {(candidates.data?.content ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {[c.insurer?.policyNo, c.insurer?.referenceNo, c.insurer?.assuredName]
                    .filter(Boolean)
                    .join(' · ')}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Columns of the items; the selection and Pair action only when the cycle can be worked. */
function itemColumns(
  rows: ReconItem[],
  selection: RowSelection | undefined,
  onPair: (item: ReconItem) => void,
): Column<ReconItem>[] {
  return [
    ...(selection === undefined
      ? []
      : [
          selectionColumn(
            rows,
            (r) => String(r.id),
            selection,
            (r) => r.invoiceNo ?? String(r.id),
          ),
        ]),
    {
      key: 'invoice',
      header: 'Invoice / Policy',
      render: (r) => (
        <>
          <strong>{r.invoiceNo ?? r.insurer?.referenceNo ?? '—'}</strong>
          <div className="muted">{r.bdoi?.policyNo ?? r.insurer?.policyNo}</div>
        </>
      ),
    },
    {
      key: 'assured',
      header: 'Assured',
      render: (r) => r.bdoi?.assuredName ?? r.insurer?.assuredName ?? '',
    },
    { key: 'ao', header: 'AO', render: (r) => r.aoUsername ?? '' },
    {
      key: 'gross',
      header: 'Gross Premium (BDOI / Insurer)',
      numeric: true,
      render: (r) => (
        <>
          <Amount value={r.bdoi?.grossPremium} />
          <div className="muted">
            <Amount value={r.insurer?.grossPremium} />
          </div>
        </>
      ),
    },
    {
      key: 'diff',
      header: 'Differences',
      render: (r) => r.discrepancies.map((d) => humanize(d)).join(', '),
    },
    {
      key: 'disposition',
      header: 'Disposition',
      render: (r) => (r.feedback?.disposition ? humanize(r.feedback.disposition) : ''),
    },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: 'Actions',
      render: (r) =>
        selection !== undefined && mayPair(r) ? (
          <Button
            size="sm"
            variant="secondary"
            onClick={(e) => {
              e.stopPropagation();
              onPair(r);
            }}
          >
            Pair
          </Button>
        ) : null,
    },
  ];
}

type Dialog = { kind: 'review' | 'pair'; item: ReconItem } | { kind: 'bulk' };

interface Act {
  mutate: (fn: () => Promise<unknown>) => void;
  isPending: boolean;
  error: unknown;
}

/** The review, pairing and bulk disposition dialogs of the items. */
function ItemDialogs({
  cycleId,
  dialog,
  editable,
  selected,
  act,
  onClose,
}: Readonly<{
  cycleId: number;
  dialog: Dialog | undefined;
  editable: boolean;
  selected: number[];
  act: Act;
  onClose: () => void;
}>) {
  const review = dialog?.kind === 'review' ? dialog.item : undefined;
  return (
    <>
      {review !== undefined && (
        <ItemReviewDialog
          item={review}
          editable={editable}
          busy={act.isPending}
          error={act.error}
          onClose={onClose}
          onSave={(feedback: ReconFeedback) =>
            act.mutate(() => prodreconApi.feedback(review.id, feedback))
          }
          onSplit={
            maySplit(review) ? () => act.mutate(() => prodreconApi.split(review.id)) : undefined
          }
        />
      )}
      {dialog?.kind === 'pair' && (
        <PairDialog
          cycleId={cycleId}
          item={dialog.item}
          busy={act.isPending}
          onClose={onClose}
          onPair={(insurerItemId) =>
            act.mutate(() => prodreconApi.pair(dialog.item.id, insurerItemId))
          }
        />
      )}
      {dialog?.kind === 'bulk' && (
        <BulkDialog
          count={selected.length}
          busy={act.isPending}
          onClose={onClose}
          onSave={(company, disposition, forClosure) =>
            act.mutate(() => prodreconApi.bulkFeedback(selected, company, disposition, forClosure))
          }
        />
      )}
    </>
  );
}

/**
 * The items of a cycle by bucket (matched, with discrepancy, BDOI only, insurer only) with the
 * side-by-side review, manual pairing and bulk disposition (PRCID.012-018).
 */
export function CycleItemsTab({
  cycle,
  editable,
}: Readonly<{ cycle: ReconCycle; editable: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [bucket, setBucket] = useState<Bucket>('ALL');
  const [q, setQ] = useState('');
  const [extra, setExtra] = useState<ItemExtraFilters>({});
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<Dialog>();
  const items = useQuery({
    queryKey: ['prodrecon', 'items', cycle.id, bucket, q, extra, page],
    queryFn: () => prodreconApi.items(cycle.id, { ...extra, bucket, q: q || undefined, page }),
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: async () => {
      setDialog(undefined);
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
      toast.success('Reconciliation updated');
    },
  });
  const rows = items.data?.content ?? [];
  const columns = itemColumns(rows, editable ? selection : undefined, (item) =>
    setDialog({ kind: 'pair', item }),
  );
  return (
    <Card>
      <div className="stack">
        <Tabs
          tabs={bucketTabs(cycle.counts)}
          active={bucket}
          onChange={(b) => {
            setBucket(b);
            setPage(0);
            selection.clear();
          }}
        />
        <WorklistToolbar
          placeholder="Search Invoice, Policy or Assured"
          filters={{ open: filtersOpen, onToggle: () => setFiltersOpen((o) => !o) }}
          onSearch={(text) => {
            setQ(text.trim());
            setPage(0);
          }}
        >
          {editable && (
            <Button
              variant="secondary"
              disabled={selection.keys.length === 0}
              onClick={() => setDialog({ kind: 'bulk' })}
            >
              Set Disposition
            </Button>
          )}
        </WorklistToolbar>
        {filtersOpen && (
          <ItemFilters
            initial={extra}
            onApply={(f) => {
              setExtra(f);
              setPage(0);
            }}
          />
        )}
        <ErrorAlert error={items.error ?? (dialog?.kind === 'review' ? undefined : act.error)} />
        <DataTable
          caption="Reconciliation items"
          columns={columns}
          rows={rows}
          rowKey={(r) => r.id}
          loading={items.isLoading}
          onRowClick={(r) => setDialog({ kind: 'review', item: r })}
          emptyMessage="No items to display"
        />
        <PageFooter data={items.data} noun="items" onPage={setPage} />
      </div>
      <ItemDialogs
        cycleId={cycle.id}
        dialog={dialog}
        editable={editable}
        selected={selection.keys.map(Number)}
        act={act}
        onClose={() => setDialog(undefined)}
      />
    </Card>
  );
}
