import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';
import type { Claim } from '../record/api';
import type { InsurerLine, ReserveChange } from './api';
import { insurerApi } from './api';
import { ReserveDialog } from './InsurerDialogs';
import { totalReserve } from './insurerLogic';

function Settlement({ claim }: Readonly<{ claim: Claim }>) {
  const p = claim.progress;
  return (
    <dl className="detail-list">
      <dt>Requested type of settlement</dt>
      <dd>{p.settlementTypeLabel ?? '—'}</dd>
      <dt>Settlement amount</dt>
      <dd>
        {p.settlementAmount === undefined
          ? '—'
          : `${claim.cover.currency} ${formatAmount(p.settlementAmount)}`}
      </dd>
      <dt>Date settled</dt>
      <dd>{formatDate(p.dateSettled)}</dd>
    </dl>
  );
}

/**
 * Reserve & Settlement tab of a claim (BRCLM.023/024/029; FR-CM-032): the insurer reserve and
 * settled amount per insurer line with the claim total, Amend Reserve (BCL_RESERVE_AMEND) and the
 * reserve history; the settlement recorded on the claim. Information only: nothing is posted.
 */
export function ReserveTab({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [amending, setAmending] = useState<InsurerLine>();
  const lines = useQuery({
    queryKey: ['broker-claims', 'insurers', claim.id],
    queryFn: () => insurerApi.lines(companyId, claim.id),
  });
  const history = useQuery({
    queryKey: ['broker-claims', 'reserve-history', claim.id],
    queryFn: () => insurerApi.reserveHistory(companyId, claim.id),
  });
  const amend = useMutation({
    mutationFn: (v: { lineId: number; amount: number; reason: string }) =>
      insurerApi.reserve(companyId, claim.id, v.lineId, v.amount, v.reason),
    onSuccess: async () => {
      toast.success('Reserve amended');
      setAmending(undefined);
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
    },
  });
  const rows = lines.data ?? [];
  const canAmend = claim.progress.phase !== 'CLOSED' && can('BCL_RESERVE_AMEND');
  const currency = claim.cover.currency;
  const names = new Map(rows.map((l) => [l.id, l.insurerName ?? l.insurerCode]));
  return (
    <div className="stack">
      <ErrorAlert error={lines.error ?? history.error} />
      <DataTable<InsurerLine>
        caption={`Insurer reserve (${currency}) - total ${formatAmount(totalReserve(rows))}`}
        loading={lines.isLoading}
        rows={rows}
        rowKey={(l) => l.id}
        emptyMessage="No insurer on this claim yet."
        columns={[
          { key: 'i', header: 'Insurer', render: (l) => l.insurerName ?? l.insurerCode },
          { key: 'n', header: 'Insurer Claim No.', render: (l) => l.insurerClaimNo ?? '' },
          {
            key: 'r',
            header: 'Reserve',
            numeric: true,
            render: (l) => formatAmount(l.reserveAmount),
          },
          {
            key: 's',
            header: 'Settled',
            numeric: true,
            render: (l) => formatAmount(l.settledAmount),
          },
          {
            key: 'x',
            header: '',
            render: (l) =>
              canAmend ? (
                <Button size="sm" variant="ghost" onClick={() => setAmending(l)}>
                  Amend Reserve
                </Button>
              ) : null,
          },
        ]}
      />
      <DataTable<ReserveChange>
        caption="Reserve history"
        rows={history.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="The reserve has not been amended."
        columns={[
          { key: 'w', header: 'Changed', render: (r) => formatDateTime(r.changedAt) },
          { key: 'i', header: 'Insurer', render: (r) => names.get(r.insurerClaimId) ?? '' },
          {
            key: 'p',
            header: 'Previous',
            numeric: true,
            render: (r) => formatAmount(r.previousAmount),
          },
          { key: 'n', header: 'New', numeric: true, render: (r) => formatAmount(r.newAmount) },
          { key: 'r', header: 'Reason', render: (r) => r.reason },
          { key: 'u', header: 'By', render: (r) => r.changedBy },
        ]}
      />
      <Settlement claim={claim} />
      {amending !== undefined && (
        <ReserveDialog
          line={amending}
          currency={currency}
          busy={amend.isPending}
          error={amend.error}
          onClose={() => setAmending(undefined)}
          onSave={(amount, reason) => amend.mutate({ lineId: amending.id, amount, reason })}
        />
      )}
    </div>
  );
}
