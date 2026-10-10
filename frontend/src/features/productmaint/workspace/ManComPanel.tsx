import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pmRoutingApi } from '@/api/pmRouting';
import type { ManComDecision, ManComStep, RoutingView } from '@/api/pmRouting';
import type { PackageRequest } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { formatDateTime } from '@/utils/format';

const STATUS: Readonly<Record<ManComStep['status'], string>> = {
  WAITING: 'Waiting for the others',
  PENDING: 'Pending Approval',
  APPROVED: 'Approved',
  RETURNED: 'Returned for Revision',
  REJECTED: 'Rejected',
  CLOSED: 'Closed',
};

const COLUMNS: Column<ManComStep>[] = [
  { key: 'r', header: 'Round', numeric: true, render: (t) => String(t.round) },
  {
    key: 'a',
    header: 'Approver',
    render: (t) => (
      <>
        <UserName login={t.approver} />
        {t.president && <div className="muted">President</div>}
      </>
    ),
  },
  {
    key: 's',
    header: 'Approval Progress',
    render: (t) => <StatusBadge status={t.status} label={STATUS[t.status]} full />,
  },
  { key: 'm', header: 'Remarks', render: (t) => t.remarks ?? '' },
  { key: 'd', header: 'Decided', render: (t) => formatDateTime(t.decidedAt) },
];

/**
 * ManCom Approver Selection and Approval Routing (BDOI FRS FRPM.014.01): TSU selects the approvers
 * from the ManCom list; each sees the request in the approval queue and approves, returns for
 * revision or rejects with remarks; the President approves after every other approver; the
 * progress of each approver is shown.
 */
export function ManComPanel({
  request,
  routing,
}: Readonly<{ request: PackageRequest; routing: RoutingView }>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const name = useDisplayName();
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<string[]>([]);
  const [remarks, setRemarks] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['package-request', request.id] });
    await queryClient.invalidateQueries({ queryKey: ['package-requests'] });
  };
  const select = useMutation({
    mutationFn: () => pmRoutingApi.selectManCom(request.id, chosen),
    onSuccess: async () => {
      toast.success('ManCom approvers selected and told');
      await refresh();
    },
  });
  const decide = useMutation({
    mutationFn: (d: ManComDecision) => pmRoutingApi.decideManCom(request.id, d, remarks),
    onSuccess: async () => {
      toast.success('Decision recorded');
      await refresh();
    },
  });
  const m = routing.mancom;
  const mine = m.tasks.find(
    (t) => t.status === 'PENDING' && t.approver.toLowerCase() === user?.username.toLowerCase(),
  );
  const run = (d: ManComDecision) => {
    if (d !== 'APPROVE' && remarks.trim() === '') {
      setProblem('Enter the remarks of the rejection or the return');
      return;
    }
    setProblem(null);
    decide.mutate(d);
  };
  const selecting =
    request.status === 'FOR_MANCOM' && (can('PKG_NEGOTIATE') || can('PKG_TSU_APPROVE'));
  return (
    <Card title="ManCom Approval" flush>
      <ErrorAlert error={select.error ?? decide.error} />
      {selecting && (
        <div className="stack card-pad">
          <fieldset className="field">
            <legend>Select ManCom Approvers</legend>
            <div className="insurer-choices">
              {m.members.map((u) => (
                <label key={u} className="checkbox">
                  <input
                    type="checkbox"
                    checked={chosen.includes(u)}
                    onChange={(e) =>
                      setChosen(e.target.checked ? [...chosen, u] : chosen.filter((x) => x !== u))
                    }
                  />
                  {name(u)}
                  {u === m.president ? ' (President, approves last)' : ''}
                </label>
              ))}
            </div>
          </fieldset>
          <div>
            <Button
              disabled={chosen.length === 0}
              busy={select.isPending}
              onClick={() => select.mutate()}
            >
              Send for ManCom Approval
            </Button>
          </div>
        </div>
      )}
      <DataTable<ManComStep>
        rows={m.tasks}
        rowKey={(t) => `${String(t.round)}-${t.approver}`}
        columns={COLUMNS}
        emptyMessage="No ManCom approvers selected yet."
      />
      {mine !== undefined && (
        <div className="stack card-pad">
          <Field label="Remarks" error={problem ?? undefined}>
            {(id) => (
              <textarea
                id={id}
                className="input"
                rows={3}
                maxLength={1000}
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            )}
          </Field>
          <div className="row-actions">
            <Button busy={decide.isPending} onClick={() => run('APPROVE')}>
              Approve
            </Button>
            <Button variant="secondary" busy={decide.isPending} onClick={() => run('RETURN')}>
              Return for Revision
            </Button>
            <Button variant="danger" busy={decide.isPending} onClick={() => run('REJECT')}>
              Reject
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
}
