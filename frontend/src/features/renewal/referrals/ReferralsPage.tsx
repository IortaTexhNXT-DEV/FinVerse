import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { renewalReferralsApi } from '@/api/renewalReferrals';
import type { NewBusinessInput, ReferralOutcome, ReferralView } from '@/api/renewalReferrals';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';
import { NewBusinessDialog, ReferralDecisionDialog } from './ReferralDialogs';

type Open =
  | { kind: 'decide'; outcome: ReferralOutcome; row: ReferralView }
  | { kind: 'nb'; row: ReferralView };

function rowActions(
  r: ReferralView,
  user: string,
  canDecide: boolean,
  open: (o: Open) => void,
  run: (call: () => Promise<unknown>, done: string) => void,
  companyId: number,
): RowAction[] {
  const mine = r.requestedBy === user;
  const pending = r.status === 'PENDING_ACCEPTANCE';
  return [
    ...(canDecide && pending
      ? [
          {
            label: 'Accept',
            onSelect: () => open({ kind: 'decide', outcome: 'ACCEPTED', row: r }),
          },
          {
            label: 'Reject',
            onSelect: () => open({ kind: 'decide', outcome: 'REJECTED', row: r }),
          },
          {
            label: 'Return for Clarification',
            onSelect: () => open({ kind: 'decide', outcome: 'RETURNED', row: r }),
          },
        ]
      : []),
    ...(canDecide && r.status === 'ACCEPTED' && r.nbArn === null
      ? [{ label: 'Create New Business Account', onSelect: () => open({ kind: 'nb', row: r }) }]
      : []),
    ...(mine && (r.status === 'DRAFT' || r.status === 'RETURNED')
      ? [
          {
            label: 'Submit',
            onSelect: () =>
              run(
                () => renewalReferralsApi.submit(companyId, r.id, r.justification),
                `${r.referralNo} submitted`,
              ),
          },
        ]
      : []),
    ...(mine && ['DRAFT', 'RETURNED', 'PENDING_ACCEPTANCE'].includes(r.status)
      ? [
          {
            label: 'Cancel Request',
            onSelect: () =>
              run(() => renewalReferralsApi.cancel(companyId, r.id), `${r.referralNo} cancelled`),
          },
        ]
      : []),
  ];
}

/**
 * Transfer Request Monitoring (FRRN.011.04): the transfer requests for New Business opportunities
 * from and to the user's units with their status, the accepting user and the New Business account.
 */
export default function ReferralsPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<Open>();
  const rows = useQuery({
    queryKey: ['renewal', 'referrals', companyId],
    queryFn: () => renewalReferralsApi.list(companyId),
  });
  const act = useMutation({
    mutationFn: (input: { call: () => Promise<unknown>; done: string }) => input.call(),
    onSuccess: async (_r, input) => {
      setOpen(undefined);
      toast.success(input.done);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const run = (call: () => Promise<unknown>, done: string) => act.mutate({ call, done });
  const canDecide = can('RNW_ASSIGN');
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Transfer Request Monitoring"
        description="Transfer requests for New Business opportunities between Marketing units."
      />
      <ErrorAlert error={rows.error ?? (open === undefined ? act.error : null)} />
      <Card flush>
        <DataTable<ReferralView>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => String(r.id)}
          emptyMessage="No transfer request"
          columns={[
            {
              key: 'no',
              header: 'Transfer Request Number',
              kind: 'code',
              render: (r) => r.referralNo,
            },
            {
              key: 'ref',
              header: 'Renewal Account Number',
              kind: 'code',
              render: (r) => (
                <Link to={`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`}>
                  {r.renewalRef}
                </Link>
              ),
            },
            { key: 'assured', header: "Assured's Name", render: (r) => r.assuredName ?? '' },
            { key: 'from', header: 'Source Marketing Unit', render: (r) => r.fromUnit ?? '' },
            { key: 'to', header: 'Receiving Marketing Unit', render: (r) => r.toUnit },
            {
              key: 'at',
              header: 'Request Date',
              kind: 'datetime',
              render: (r) => formatDateTime(r.requestedAt),
            },
            {
              key: 'status',
              header: 'Transfer Status',
              kind: 'status',
              render: (r) => <StatusBadge status={r.status} label={r.statusLabel} />,
            },
            {
              key: 'by',
              header: 'Accepted By',
              render: (r) =>
                r.status === 'ACCEPTED' && r.decidedBy ? <UserName login={r.decidedBy} /> : '',
            },
            { key: 'remarks', header: 'Remarks', render: (r) => r.decisionRemarks ?? '' },
            {
              key: 'nb',
              header: 'New Business Account Number',
              kind: 'code',
              render: (r) => r.nbArn ?? '',
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (r) => (
                <RowActionMenu
                  label={r.referralNo}
                  actions={rowActions(r, user?.username ?? '', canDecide, setOpen, run, companyId)}
                />
              ),
            },
          ]}
        />
      </Card>
      {open?.kind === 'decide' && (
        <ReferralDecisionDialog
          referral={open.row}
          outcome={open.outcome}
          busy={act.isPending}
          error={act.error}
          onClose={() => setOpen(undefined)}
          onConfirm={(remarks) =>
            run(
              () => renewalReferralsApi.decide(companyId, open.row.id, open.outcome, remarks),
              `${open.row.referralNo} updated`,
            )
          }
        />
      )}
      {open?.kind === 'nb' && (
        <NewBusinessDialog
          referral={open.row}
          busy={act.isPending}
          error={act.error}
          onClose={() => setOpen(undefined)}
          onConfirm={(input: NewBusinessInput) =>
            run(
              () => renewalReferralsApi.createNewBusiness(companyId, open.row.id, input),
              'New Business account created',
            )
          }
        />
      )}
    </div>
  );
}
