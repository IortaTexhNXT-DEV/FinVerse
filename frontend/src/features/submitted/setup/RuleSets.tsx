import { useMutation, useQuery } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';
import type { RuleSetView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { SBM_LOV, STEP_LABELS } from '../common/submittedCodes';

/** The rule sets of the processing steps with their rules, submitted and approved. */
export function RuleSets({ onDone }: Readonly<{ onDone: () => void }>) {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const sets = useQuery({
    queryKey: ['submitted', 'rule-sets', companyId],
    queryFn: () => submittedApi.ruleSets(companyId),
    enabled: companyId > 0,
  });
  const decide = useMutation({
    mutationFn: ({ id, d }: { id: number; d: 'submit' | 'approve' | 'reject' }) =>
      submittedApi.ruleSetDecision(id, d),
    onSuccess: onDone,
  });
  return (
    <Card flush>
      <ErrorAlert error={sets.error ?? decide.error} />
      <DataTable<RuleSetView>
        loading={sets.isLoading}
        rows={sets.data ?? []}
        rowKey={(s) => s.id}
        emptyMessage="No rule set"
        renderExpanded={(s) => (
          <ol className="plain-list">
            {s.rules.map((r) => (
              <li key={r.id}>
                <strong>{r.priority}</strong> {r.name}:{' '}
                {r.conditions
                  .map((c) => `${c.field} ${c.operator.toLowerCase()} ${c.value ?? ''}`)
                  .join(' and ')}{' '}
                →{' '}
                {[r.outcome.bucket, r.outcome.classification, r.outcome.flag, r.outcome.tag]
                  .filter(Boolean)
                  .map((v) => humanize(String(v)))
                  .join(', ') || 'pass'}
              </li>
            ))}
          </ol>
        )}
        columns={[
          { key: 'code', header: 'Rule Set', kind: 'code', render: (s) => s.code },
          { key: 'step', header: 'Step', render: (s) => STEP_LABELS[s.step] ?? s.step },
          {
            key: 'segment',
            header: 'Segment',
            render: (s) => <LovLabel type={SBM_LOV.segment} code={s.segment} empty="All" />,
          },
          { key: 'version', header: 'Version', kind: 'amount', render: (s) => s.versionNo },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (s) => <StatusBadge status={s.status} />,
          },
          {
            key: 'from',
            header: 'Effective From',
            kind: 'date',
            render: (s) => formatDate(s.effectiveFrom),
          },
          { key: 'rules', header: 'Rules', kind: 'amount', render: (s) => s.rules.length },
          { key: 'maker', header: 'Maker', render: (s) => <UserName login={s.maker} /> },
          {
            key: 'actions',
            header: 'Actions',
            render: (s) => (
              <span className="form-actions">
                {can('SBM_RULE_MAINTAIN') && s.status === 'DRAFT' && (
                  <Button variant="ghost" onClick={() => decide.mutate({ id: s.id, d: 'submit' })}>
                    Submit
                  </Button>
                )}
                {can('SBM_RULE_APPROVE') &&
                  s.status === 'SUBMITTED' &&
                  user?.username !== s.maker && (
                    <>
                      <Button
                        variant="ghost"
                        onClick={() => decide.mutate({ id: s.id, d: 'approve' })}
                      >
                        Approve
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => decide.mutate({ id: s.id, d: 'reject' })}
                      >
                        Reject
                      </Button>
                    </>
                  )}
              </span>
            ),
          },
        ]}
      />
    </Card>
  );
}
