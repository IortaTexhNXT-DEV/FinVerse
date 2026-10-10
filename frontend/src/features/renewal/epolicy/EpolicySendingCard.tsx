import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { addresses, renewalPlacementApi } from '@/api/renewalPlacement';
import type { EpolicyAccount, EpolicySummary } from '@/api/renewalPlacement';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';

function Summary({ s }: Readonly<{ s: EpolicySummary }>) {
  return (
    <div className="rnw-note" role="status">
      <strong>Total Accounts Selected:</strong> {s.selected} ·{' '}
      <strong>Successfully Submitted:</strong> {s.submitted} · <strong>Failed Submission:</strong>{' '}
      {s.failed} · <strong>Pending Submission:</strong> {s.pending}
      {Object.entries(s.messages).map(([ref, why]) => (
        <div key={ref}>
          {ref}: {why}
        </div>
      ))}
    </div>
  );
}

/**
 * E-policy sending through CCM (FRRN.033.04): the accounts For E-Policy Sending, those without an
 * attached e-policy, a policy number or a valid client e-mail address shown and not selectable;
 * one, several or all accounts sent with copy recipients, then the summary of the sending.
 */
export function EpolicySendingCard() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<string[]>([]);
  const [cc, setCc] = useState('');
  const accounts = useQuery({
    queryKey: ['renewal', 'epolicy', 'sending', companyId],
    queryFn: () => renewalPlacementApi.forSending(companyId),
  });
  const eligible = (accounts.data ?? []).filter((a) => a.problem === null).map((a) => a.renewalRef);
  const send = useMutation({
    mutationFn: () => renewalPlacementApi.sendEpolicies(companyId, selected, addresses(cc)),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'epolicy'] });
    },
  });
  const toggle = (ref: string) =>
    setSelected((old) => (old.includes(ref) ? old.filter((r) => r !== ref) : [...old, ref]));
  const all = eligible.length > 0 && eligible.every((r) => selected.includes(r));
  return (
    <Card title="For E-Policy Sending">
      <ErrorAlert error={accounts.error ?? send.error} />
      <span className="rnw-actions">
        <Button
          variant="ghost"
          onClick={() => setSelected(all ? [] : eligible)}
          disabled={eligible.length === 0}
        >
          {all ? 'Deselect All' : 'Select All'}
        </Button>
        <Field label="CC recipients" hint="Separate the addresses with commas">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
        <Button
          disabled={selected.length === 0}
          busy={send.isPending}
          onClick={() => send.mutate()}
        >
          Send E-Policy ({selected.length})
        </Button>
      </span>
      {send.data !== undefined && <Summary s={send.data} />}
      <DataTable<EpolicyAccount>
        loading={accounts.isLoading}
        rows={accounts.data ?? []}
        rowKey={(a) => a.renewalRef}
        emptyMessage="No account is For E-Policy Sending"
        columns={[
          {
            key: 'select',
            header: 'Select',
            render: (a) => (
              <input
                type="checkbox"
                aria-label={`Select ${a.renewalRef}`}
                disabled={a.problem !== null}
                checked={selected.includes(a.renewalRef)}
                onChange={() => toggle(a.renewalRef)}
              />
            ),
          },
          {
            key: 'ref',
            header: 'Renewal Reference Number',
            kind: 'code',
            render: (a) => a.renewalRef,
          },
          { key: 'client', header: 'Client', render: (a) => a.clientName },
          { key: 'policy', header: 'Policy Number', render: (a) => a.policyNo ?? '' },
          { key: 'email', header: 'Client Email', render: (a) => a.email ?? '' },
          { key: 'file', header: 'E-Policy', render: (a) => a.fileName ?? '' },
          { key: 'problem', header: 'Not Eligible', render: (a) => a.problem ?? '' },
        ]}
      />
    </Card>
  );
}
