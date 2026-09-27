import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { LetterView, PolicyDetail, RenewalRow } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';
import { letterColumns } from '../common/letterColumns';
import { SBM_LOV } from '../common/submittedCodes';
import { ReassignDialog } from '../renewals/ReassignDialog';

const template = (t: string) => (t === 'FFY' ? 'Free First Year' : 'Generic');

function RenewalFacts({ r }: Readonly<{ r: RenewalRow }>) {
  return (
    <DefinitionGrid
      columns={2}
      items={[
        { label: 'Hand-off', value: <StatusBadge status={r.handoffStatus} /> },
        { label: 'Started', value: r.manual ? 'Renew with BDOI' : 'Expiry scan' },
        { label: 'Handed Over', value: formatDateTime(r.handedOffAt) },
        { label: 'Renewal Reference', value: r.renewalRef },
        { label: 'Renewal Account', value: r.arn },
        { label: 'Insurer Assigned', value: <InsurerName code={r.insurerAssigned} /> },
        {
          label: 'RA Template',
          value: template(r.raTemplate),
        },
        { label: 'Hold Cover', value: formatDate(r.holdCoverOn) },
        { label: 'Insurer Accepted', value: formatDate(r.insurerAcceptedOn) },
        { label: 'Re-assignments', value: r.reassignCount },
        { label: 'Outcome', value: r.outcome ? <StatusBadge status={r.outcome} /> : null },
        {
          label: 'Decline Reason',
          value: <LovLabel type={SBM_LOV.decline} code={r.declineReason} />,
        },
        { label: 'Message', value: r.message, wide: true },
      ]}
    />
  );
}

/** The hand-off of a record to Renewal (FR-SP-060 to 064). */
export function RenewalTab({ detail }: Readonly<{ detail: PolicyDetail }>) {
  const policyId = detail.row.id;
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [reassigning, setReassigning] = useState(false);
  const renewal = useQuery({
    queryKey: ['submitted', 'renewal-of', policyId],
    queryFn: () => submittedApi.renewalOf(policyId),
  });
  const renew = useMutation({
    mutationFn: () => submittedApi.renew(policyId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['submitted'] }),
  });
  const r = renewal.data?.[0];
  return (
    <div className="stack">
      <ErrorAlert error={renewal.error ?? renew.error} />
      {r === undefined ? (
        <Card title="Renewal">
          <EmptyState message="Not handed to Renewal yet" />
          {can('SBM_PROCESS') && detail.row.status === 'FOR_RENEWAL' && (
            <Button disabled={renew.isPending} onClick={() => renew.mutate()}>
              Renew with BDOI
            </Button>
          )}
        </Card>
      ) : (
        <Card
          title="Renewal"
          actions={
            can('SBM_PROCESS') &&
            r.insurerAcceptedOn === null && (
              <Button variant="secondary" onClick={() => setReassigning(true)}>
                Re-assign Insurer
              </Button>
            )
          }
        >
          <RenewalFacts r={r} />
        </Card>
      )}
      {reassigning && r !== undefined && (
        <ReassignDialog row={r} onClose={() => setReassigning(false)} />
      )}
    </div>
  );
}

/** The letters of a record (FR-SP-065, 066). */
export function LettersTab({ policyId }: Readonly<{ policyId: number }>) {
  const letters = useQuery({
    queryKey: ['submitted', 'letters-of', policyId],
    queryFn: () => submittedApi.lettersOf(policyId),
  });
  const download = useFileDownload();
  return (
    <Card title="Letters" flush>
      <ErrorAlert error={letters.error ?? download.error} />
      <DataTable<LetterView>
        loading={letters.isLoading}
        rows={letters.data ?? []}
        rowKey={(l) => l.id}
        emptyMessage="No letter sent"
        columns={letterColumns((id) => download.mutate(() => submittedApi.letterPdf(id)))}
      />
    </Card>
  );
}
