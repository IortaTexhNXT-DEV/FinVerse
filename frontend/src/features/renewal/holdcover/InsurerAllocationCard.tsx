import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalHoldCoverRequestsApi } from '@/api/renewalHoldCoverRequests';
import type { InsurerShare } from '@/api/renewalHoldCoverRequests';
import { useAuth } from '@/auth/authContext';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { useInsurerName } from '@/components/broking/useLabels';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';

interface Line {
  insurerCode: string;
  value: string;
}

/**
 * Multiple insurer allocation (FRRN.014.03): the insurers of the renewal by percentage (totalling
 * 100%) or coverage amount (totalling the total sum insured), with each insurer's premium.
 */
export function InsurerAllocationCard({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const insurerName = useInsurerName();
  const [byAmount, setByAmount] = useState(false);
  const [lines, setLines] = useState<Line[]>([]);
  const shares = useQuery({
    queryKey: ['renewal', 'insurers', companyId, renewalRef],
    queryFn: () => renewalHoldCoverRequestsApi.insurers(companyId, renewalRef),
  });
  const save = useMutation({
    mutationFn: () =>
      renewalHoldCoverRequestsApi.allocate(
        companyId,
        renewalRef,
        lines.map((l) =>
          byAmount
            ? { insurerCode: l.insurerCode, amount: Number(l.value) }
            : { insurerCode: l.insurerCode, percent: Number(l.value) },
        ),
      ),
    onSuccess: async () => {
      setLines([]);
      toast.success('Insurer allocation saved');
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'insurers'] });
    },
  });
  const set = (i: number, patch: Partial<Line>) =>
    setLines((old) => old.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  return (
    <Card title="Insurers" flush>
      <ErrorAlert error={shares.error ?? save.error} />
      <DataTable<InsurerShare>
        loading={shares.isLoading}
        rows={shares.data ?? []}
        rowKey={(s) => s.insurerCode}
        emptyMessage="No insurer"
        columns={[
          { key: 'insurer', header: 'Insurer', render: (s) => insurerName(s.insurerCode) },
          { key: 'share', header: 'Share (%)', kind: 'amount', render: (s) => s.percent },
          {
            key: 'amount',
            header: 'Coverage',
            kind: 'amount',
            render: (s) => <Amount value={s.amount} />,
          },
          {
            key: 'premium',
            header: 'Premium',
            kind: 'amount',
            render: (s) => <Amount value={s.premium} />,
          },
        ]}
      />
      {(can('RNW_DISPOSE') || can('RNW_PROCESS')) && (
        <div className="stack rnw-note">
          <label className="checkbox">
            <input
              type="checkbox"
              checked={byAmount}
              onChange={(e) => setByAmount(e.target.checked)}
            />{' '}
            Allocate by coverage amount
          </label>
          {lines.map((l, i) => (
            <span key={i} className="rnw-actions">
              <InsurerSelect
                id={`rnw-insurer-${String(i)}`}
                aria-label={`Insurer ${String(i + 1)}`}
                value={l.insurerCode}
                onChange={(code) => set(i, { insurerCode: code })}
              />
              <input
                className="input"
                aria-label={`${byAmount ? 'Amount' : 'Share'} ${String(i + 1)}`}
                inputMode="decimal"
                value={l.value}
                onChange={(e) => set(i, { value: e.target.value })}
              />
            </span>
          ))}
          <span className="rnw-actions">
            <Button
              size="sm"
              variant="secondary"
              onClick={() => setLines((old) => [...old, { insurerCode: '', value: '' }])}
            >
              Add Insurer
            </Button>
            <Button
              size="sm"
              disabled={lines.length === 0}
              busy={save.isPending}
              onClick={() => save.mutate()}
            >
              Save Allocation
            </Button>
          </span>
        </div>
      )}
    </Card>
  );
}
