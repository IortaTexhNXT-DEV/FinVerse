import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { Cession, CessionLine } from '@/api/reinsurance';
import { underwritingApi } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { TextField } from '@/features/underwriting/FormFields';
import { formatDate, humanize } from '@/utils/format';

/** Per-policy cession view: the allocation of every transaction, and on-demand ceding. */
export function PolicyCessionsCard({ companyId }: Readonly<{ companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [input, setInput] = useState('');
  const [policyNo, setPolicyNo] = useState('');
  const [selected, setSelected] = useState<Cession | null>(null);
  const policy = useQuery({
    queryKey: ['ri-policy', companyId, policyNo],
    queryFn: async () =>
      (await underwritingApi.policies({ companyId, q: policyNo, size: 5 })).content.find(
        (p) => p.policyNo === policyNo,
      ) ?? null,
    enabled: policyNo !== '',
  });
  const cessions = useQuery({
    queryKey: ['ri-cessions', companyId, policyNo],
    queryFn: () => reinsuranceApi.policyCessions(companyId, policyNo),
    enabled: policyNo !== '',
  });
  const cede = useMutation({
    mutationFn: (policyId: number) => reinsuranceApi.cedePolicy(policyId),
    onSuccess: async (list) => {
      await queryClient.invalidateQueries({ queryKey: ['ri-cessions'] });
      toast.success(`${list.length} transaction(s) ceded`);
    },
  });
  const found = policy.data ?? null;

  return (
    <Card
      title="Policy cessions"
      actions={
        found !== null &&
        can('REINSURANCE_MAINTAIN') && (
          <Button
            size="sm"
            variant="secondary"
            busy={cede.isPending}
            onClick={() => cede.mutate(found.id)}
          >
            Cede Now
          </Button>
        )
      }
    >
      <ErrorAlert error={policy.error ?? cessions.error ?? cede.error} />
      <div className="row">
        <TextField label="Policy number" value={input} onChange={setInput} />
        <Button
          variant="secondary"
          icon={<Search size={16} />}
          onClick={() => {
            setSelected(null);
            setPolicyNo(input.trim());
          }}
        >
          Show
        </Button>
      </div>
      {policyNo !== '' && found === null && !policy.isLoading && (
        <p className="muted">No policy {policyNo} in this company.</p>
      )}
      <DataTable<Cession>
        rows={cessions.data ?? []}
        loading={cessions.isLoading && policyNo !== ''}
        rowKey={(c) => c.id}
        onRowClick={setSelected}
        emptyMessage="No cession yet."
        caption="Cessions of the policy"
        columns={[
          { key: 'n', header: 'Cession', render: (c) => <strong>{c.cessionNo}</strong> },
          { key: 'd', header: 'Document', render: (c) => `${c.documentNo} (${humanize(c.kind)})` },
          { key: 'b', header: 'Basis', render: (c) => humanize(c.basis) },
          { key: 'r', header: 'RI Date', render: (c) => formatDate(c.riDate) },
          {
            key: 'p',
            header: 'Our Premium',
            numeric: true,
            render: (c) => <Amount value={c.ourPremium} />,
          },
          {
            key: 't',
            header: 'Treaty',
            numeric: true,
            render: (c) => <Amount value={c.treatyPremium} />,
          },
          {
            key: 'f',
            header: 'FAC',
            numeric: true,
            render: (c) => <Amount value={c.facPremium} />,
          },
          {
            key: 'k',
            header: 'Retention',
            numeric: true,
            render: (c) => <Amount value={c.retention} />,
          },
        ]}
      />
      {selected !== null && (
        <DataTable<CessionLine>
          rows={selected.lines}
          rowKey={(l) =>
            `${l.riskLineNo}-${l.layer}-${l.reinsurerCode ?? ''}-${l.placementId ?? ''}`
          }
          caption={`Allocation lines of ${selected.cessionNo}`}
          columns={[
            { key: 'r', header: 'Risk', render: (l) => `${l.riskLineNo} ${l.riskDescription}` },
            { key: 'l', header: 'Layer', render: (l) => humanize(l.layer) },
            { key: 'w', header: 'Reinsurer', render: (l) => l.reinsurerCode ?? '—' },
            { key: 'pc', header: 'SI %', numeric: true, render: (l) => l.sharePct },
            {
              key: 's',
              header: 'Sum Insured',
              numeric: true,
              render: (l) => <Amount value={l.sumInsured} />,
            },
            {
              key: 'p',
              header: 'Premium',
              numeric: true,
              render: (l) => <Amount value={l.premium} />,
            },
            {
              key: 'c',
              header: 'Commission',
              numeric: true,
              render: (l) => <Amount value={l.commission} />,
            },
          ]}
        />
      )}
    </Card>
  );
}
