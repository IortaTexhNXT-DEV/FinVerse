import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { channelLabel } from '@/api/renewalChannels';
import { useState } from 'react';
import { HOLD_COVER_KINDS, renewalHoldCoverRequestsApi } from '@/api/renewalHoldCoverRequests';
import type { HoldCoverRequestView } from '@/api/renewalHoldCoverRequests';
import { useAuth } from '@/auth/authContext';
import { useInsurerName } from '@/components/broking/useLabels';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

/**
 * Hold cover requests per insurer (FRRN.036): the default duration of the account, one request per
 * insurer, the extension of 15 or 30 days, and the insurer's response.
 */
export function HoldCoverRequestsCard({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const insurerName = useInsurerName();
  const [extension, setExtension] = useState(15);
  const rows = useQuery({
    queryKey: ['renewal', 'hold-cover-requests', companyId, renewalRef],
    queryFn: () => renewalHoldCoverRequestsApi.list(companyId, renewalRef),
  });
  const act = useMutation({
    mutationFn: (input: { call: () => Promise<unknown>; done: string }) => input.call(),
    onSuccess: async (_r, input) => {
      toast.success(input.done);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const allowed = can('RNW_PROCESS') || can('RNW_DISPOSE');
  const days = rows.data?.defaultDays;
  return (
    <Card
      title="Hold Cover Requests"
      flush
      actions={
        allowed && (
          <span className="rnw-actions">
            <Button
              size="sm"
              busy={act.isPending}
              onClick={() =>
                act.mutate({
                  call: () => renewalHoldCoverRequestsApi.request(companyId, renewalRef),
                  done: `Hold cover of ${String(days ?? '')} days requested`,
                })
              }
            >
              Request Hold Cover ({String(days ?? '')} days)
            </Button>
            <select
              className="input"
              aria-label="Extension days"
              value={extension}
              onChange={(e) => setExtension(Number(e.target.value))}
            >
              <option value={15}>15 days</option>
              <option value={30}>30 days</option>
            </select>
            <Button
              size="sm"
              variant="secondary"
              onClick={() =>
                act.mutate({
                  call: () => renewalHoldCoverRequestsApi.extend(companyId, renewalRef, extension),
                  done: `Extension of ${String(extension)} days requested`,
                })
              }
            >
              Request Extension
            </Button>
          </span>
        )
      }
    >
      <ErrorAlert error={rows.error ?? act.error} />
      <DataTable<HoldCoverRequestView>
        loading={rows.isLoading}
        rows={rows.data?.requests ?? []}
        rowKey={(r) => r.requestNo}
        emptyMessage="No hold cover request"
        columns={[
          { key: 'no', header: 'Reference Number', kind: 'code', render: (r) => r.requestNo },
          { key: 'kind', header: 'Request', render: (r) => HOLD_COVER_KINDS[r.kind] ?? r.kind },
          { key: 'insurer', header: 'Insurer', render: (r) => insurerName(r.insurerCode) },
          { key: 'share', header: 'Share (%)', kind: 'amount', render: (r) => r.share ?? '' },
          { key: 'start', header: 'Start', kind: 'date', render: (r) => formatDate(r.start) },
          { key: 'end', header: 'End', kind: 'date', render: (r) => formatDate(r.end) },
          { key: 'sent', header: 'Sent By', render: (r) => channelLabel(r.channel) },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (r) => <StatusBadge status={r.status} />,
          },
          { key: 'remarks', header: 'Insurer Remarks', render: (r) => r.remarks ?? '' },
          {
            key: 'actions',
            header: 'Actions',
            render: (r) =>
              allowed && r.status === 'REQUESTED' ? (
                <RowActionMenu
                  label={r.requestNo}
                  actions={[
                    {
                      label: 'Record Approval',
                      onSelect: () =>
                        act.mutate({
                          call: () =>
                            renewalHoldCoverRequestsApi.respond(
                              companyId,
                              renewalRef,
                              r.requestNo,
                              {
                                approved: true,
                              },
                            ),
                          done: `${r.requestNo} approved`,
                        }),
                    },
                  ]}
                />
              ) : (
                ''
              ),
          },
        ]}
      />
    </Card>
  );
}
