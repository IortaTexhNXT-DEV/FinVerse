import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize } from '@/utils/format';
import { optionalText } from './acsl';
import { acslApi } from './api';
import type { Correction, OriginalLine } from './api';
import { FormDialog } from './FormDialog';

/**
 * The posted lines of the invoice family (ACSL 2.9.1): the preparer picks the wrong line and names
 * the right account; the server adds the reversal and the re-post to the draft.
 */
export function OriginalLinesCard({
  correction,
  editable,
}: Readonly<{ correction: Correction; editable: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [picked, setPicked] = useState<OriginalLine | null>(null);
  const lines = useQuery({
    queryKey: ['acsl', 'originalLines', correction.id],
    queryFn: () => acslApi.originalLines(correction.id),
  });
  const propose = useMutation({
    mutationFn: ({ o, v }: { o: OriginalLine; v: Record<string, string> }) =>
      acslApi.propose(correction.id, {
        batchNo: o.batchNo,
        lineNo: o.lineNo,
        targetAccountCode: (v.target ?? '').trim(),
        targetPartyCode: optionalText(v.party),
        component: optionalText(v.component),
        targetComponent: optionalText(v.targetComponent),
      }),
    onSuccess: async () => {
      setPicked(null);
      await queryClient.invalidateQueries({ queryKey: ['acsl', 'correction', correction.id] });
      toast.success('Reversal and re-post added');
    },
  });
  const columns: Column<OriginalLine>[] = [
    {
      key: 'batch',
      header: 'Journal',
      render: (o) => (
        <>
          <strong>{o.batchNo}</strong>
          <span className="cell-sub">
            {humanize(o.journalType)} · {formatDate(o.valueDate)}
          </span>
        </>
      ),
    },
    {
      key: 'account',
      header: 'Account',
      render: (o) => (
        <>
          {o.accountCode}
          <span className="cell-sub">{o.accountName}</span>
        </>
      ),
    },
    { key: 'party', header: 'Party', render: (o) => o.partyCode ?? '—' },
    { key: 'side', header: 'Dr / Cr', render: (o) => (o.side === 'DEBIT' ? 'Dr' : 'Cr') },
    { key: 'amount', header: 'Amount', numeric: true, render: (o) => <Amount value={o.amount} /> },
  ];
  if (editable) {
    columns.push({
      key: 'act',
      header: 'Action',
      render: (o) => (
        <Button variant="secondary" size="sm" onClick={() => setPicked(o)}>
          Correct
        </Button>
      ),
    });
  }
  return (
    <Card title="Posted Lines of the Invoice Family" flush>
      <ErrorAlert error={lines.error} />
      <DataTable
        caption="Posted lines"
        columns={columns}
        rows={lines.data ?? []}
        rowKey={(o) => `${o.batchNo}:${String(o.lineNo)}`}
        loading={lines.isLoading}
        emptyMessage="No items to display"
      />
      {picked && (
        <FormDialog
          title={`Correct ${picked.batchNo} line ${String(picked.lineNo)}`}
          confirmLabel="Add Correction Lines"
          fields={[
            { key: 'target', label: 'Right GL Account', required: true },
            { key: 'party', label: 'Right Party', initial: picked.partyCode },
            {
              key: 'component',
              label: 'Ledger Component',
              hint: 'e.g. BASIC, when the ledger moves',
            },
            { key: 'targetComponent', label: 'Right Ledger Component' },
          ]}
          busy={propose.isPending}
          error={propose.error}
          onConfirm={(v) => propose.mutate({ o: picked, v })}
          onClose={() => setPicked(null)}
        />
      )}
    </Card>
  );
}
