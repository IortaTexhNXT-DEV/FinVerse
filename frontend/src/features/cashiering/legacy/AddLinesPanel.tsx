import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import type { DpprCandidate, LegacyBatch, UnappliedCandidate } from '@/api/legacyBatches';
import { legacyBatchLines } from '@/api/legacyBatches';
import type { RecordOriginKind } from '@/api/types';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { OriginFilter } from '@/components/ui/OriginFilter';
import { Tag } from '@/components/ui/Tag';
import { useCompanyId } from '@/context/workspaceContext';
import type { BatchScreen } from './batchScreens';
import { StatusBadge } from '@/components/ui/StatusBadge';

function useAddLine<T>(run: (value: T) => Promise<unknown>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: run,
    onSuccess: () => client.invalidateQueries({ queryKey: ['legacy-batches'] }),
  });
}

function legacyTag(context: string | undefined) {
  return context === 'LEGACY' ? <Tag tone="info">LEGACY</Tag> : null;
}

/** Unapplied payments that may be taken to income: open, old enough, not on another batch. */
function UnappliedLines({ batch }: Readonly<{ batch: LegacyBatch }>) {
  const companyId = useCompanyId();
  const [minAge, setMinAge] = useState('365');
  const [origin, setOrigin] = useState<RecordOriginKind>();
  const [reason, setReason] = useState('');
  const days = Math.max(0, Number(minAge) || 0);
  const candidates = useQuery({
    queryKey: ['legacy-batches', 'unapplied-candidates', companyId, days, origin],
    queryFn: () => legacyBatchLines.unappliedCandidates(companyId, days, origin),
    enabled: companyId > 0,
  });
  const add = useAddLine((id: number) => legacyBatchLines.addUnapplied(batch.batchNo, id, reason));
  const rows = (candidates.data ?? []).filter((c) => c.currency === batch.currency);
  return (
    <Card title="Add Unapplied Payments">
      <div className="form-grid">
        <Field label="Received At Least (days ago)">
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="numeric"
              value={minAge}
              onChange={(e) => setMinAge(e.target.value)}
            />
          )}
        </Field>
        <OriginFilter value={origin} onChange={setOrigin} />
        <Field label="Reason for the Lines">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={250}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
      <ErrorAlert error={candidates.error ?? add.error} />
      <DataTable<UnappliedCandidate>
        loading={candidates.isLoading}
        rows={rows}
        rowKey={(c) => c.id}
        emptyMessage="No unapplied payment fits"
        columns={[
          {
            key: 'ref',
            header: 'Unapplied Payment',
            render: (c) => (
              <CellStack
                main={
                  <>
                    {c.reference} {legacyTag(c.ledgerContext)}
                  </>
                }
                sub={c.legacyArNo ? `Legacy AR ${c.legacyArNo}` : c.origin}
              />
            ),
          },
          {
            key: 'payor',
            header: 'Payor / Client',
            render: (c) => <CellStack main={c.payorName ?? ''} sub={c.clientCode ?? ''} />,
          },
          { key: 'age', header: 'Age (days)', numeric: true, render: (c) => String(c.ageDays) },
          {
            key: 'stage',
            header: 'Stage',
            kind: 'status',
            render: (c) => <StatusBadge status={c.stage} />,
          },
          {
            key: 'balance',
            header: 'Balance',
            kind: 'amount',
            render: (c) => <Amount value={c.balance} />,
          },
          {
            key: 'add',
            header: '',
            render: (c) => (
              <Button
                variant="secondary"
                size="sm"
                icon={<Plus size={14} />}
                busy={add.isPending}
                onClick={() => add.mutate(c.id)}
              >
                Add
              </Button>
            ),
          },
        ]}
      />
    </Card>
  );
}

/** A legacy invoice and the PR 2307 amount to reverse against the insurer. */
function Pr2307Lines({ batch }: Readonly<{ batch: LegacyBatch }>) {
  const [invoiceNo, setInvoiceNo] = useState('');
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const add = useAddLine(() =>
    legacyBatchLines.addPr2307(batch.batchNo, invoiceNo.trim(), Number(amount), reason),
  );
  const ready = invoiceNo.trim() !== '' && Number(amount) > 0;
  return (
    <Card title="Add a Legacy Invoice">
      <ErrorAlert error={add.error} />
      <div className="form-grid">
        <Field label="Invoice No." required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={invoiceNo}
              onChange={(e) => setInvoiceNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="Amount" required hint="Within the open PR 2307 and the due to insurer">
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="decimal"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          )}
        </Field>
        <Field label="Reason">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={250}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
      <Button
        variant="primary"
        icon={<Plus size={14} />}
        disabled={!ready}
        busy={add.isPending}
        onClick={() =>
          add.mutate(undefined, {
            onSuccess: () => {
              setInvoiceNo('');
              setAmount('');
            },
          })
        }
      >
        Add Invoice
      </Button>
    </Card>
  );
}

/** Legacy invoices with an open premium receivable, optionally only those Collections tagged. */
function DpprLines({ batch }: Readonly<{ batch: LegacyBatch }>) {
  const companyId = useCompanyId();
  const [taggedOnly, setTaggedOnly] = useState(true);
  const [reason, setReason] = useState('');
  const candidates = useQuery({
    queryKey: ['legacy-batches', 'dppr-candidates', companyId, taggedOnly],
    queryFn: () => legacyBatchLines.dpprCandidates(companyId, taggedOnly),
    enabled: companyId > 0,
  });
  const add = useAddLine((invoiceNo: string) =>
    legacyBatchLines.addDppr(batch.batchNo, invoiceNo, reason),
  );
  return (
    <Card title="Add Legacy Invoices">
      <div className="form-grid">
        <Field label="Show">
          {(id) => (
            <select
              id={id}
              className="select"
              value={taggedOnly ? 'tagged' : 'all'}
              onChange={(e) => setTaggedOnly(e.target.value === 'tagged')}
            >
              <option value="tagged">Tagged "DP PR for reversal" by Collections</option>
              <option value="all">Every legacy invoice with open premium</option>
            </select>
          )}
        </Field>
        <Field label="Reason for the Lines">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={250}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
      <ErrorAlert error={candidates.error ?? add.error} />
      <DataTable<DpprCandidate>
        loading={candidates.isLoading}
        rows={candidates.data ?? []}
        rowKey={(c) => c.invoiceNo}
        emptyMessage="No legacy invoice fits"
        columns={[
          {
            key: 'invoice',
            header: 'Invoice',
            render: (c) => (
              <CellStack
                main={
                  <>
                    {c.invoiceNo} <Tag tone="info">LEGACY</Tag>
                  </>
                }
                sub={[c.sourceSystem, c.legacyInvoiceNo].filter(Boolean).join(' ')}
              />
            ),
          },
          {
            key: 'parties',
            header: 'Client / Insurer',
            render: (c) => <CellStack main={c.clientCode} sub={c.insurerCode} />,
          },
          { key: 'policy', header: 'Policy', render: (c) => c.policyNo ?? '' },
          { key: 'tagged', header: 'Tagged', render: (c) => (c.tagged ? 'Yes' : 'No') },
          {
            key: 'open',
            header: 'Open Premium',
            kind: 'amount',
            render: (c) => <Amount value={c.openPremium} />,
          },
          {
            key: 'add',
            header: '',
            render: (c) => (
              <Button
                variant="secondary"
                size="sm"
                icon={<Plus size={14} />}
                busy={add.isPending}
                onClick={() => add.mutate(c.invoiceNo)}
              >
                Add
              </Button>
            ),
          },
        ]}
      />
    </Card>
  );
}

/** The way to add lines to a draft batch of a screen. */
export function AddLinesPanel({
  screen,
  batch,
}: Readonly<{ screen: BatchScreen; batch: LegacyBatch }>) {
  if (screen.id === 'income') {
    return <UnappliedLines batch={batch} />;
  }
  return screen.id === 'pr2307' ? <Pr2307Lines batch={batch} /> : <DpprLines batch={batch} />;
}
