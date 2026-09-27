import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { Issue } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { Tag } from '@/components/ui/Tag';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { migLabel } from '../common/migrationCodes';

function resolver(i: Issue): string | undefined {
  if (i.resolvedBy === undefined) {
    return undefined;
  }
  return i.resolutionNote ? i.resolvedBy + ': ' + i.resolutionNote : i.resolvedBy;
}

/** The data-quality issues of a batch with the Data Steward's resolution. */
export function BatchIssues({ batchNo }: Readonly<{ batchNo: string }>) {
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<MigAction>();
  const issues = useQuery({
    queryKey: ['migration', 'batch', batchNo, 'issues', page],
    queryFn: () => migrationApi.batchIssues(batchNo, page),
  });
  return (
    <Card flush>
      <ErrorAlert error={issues.error} onRetry={() => void issues.refetch()} />
      <DataTable<Issue>
        loading={issues.isLoading}
        rows={issues.data?.content ?? []}
        rowKey={(i) => i.id}
        emptyMessage="No data-quality issue"
        columns={[
          { key: 'rule', header: 'Rule', kind: 'code', render: (i) => i.ruleCode },
          {
            key: 'severity',
            header: 'Severity',
            render: (i) => (
              <Tag tone={i.severity === 'ERROR' ? 'danger' : 'neutral'}>{migLabel(i.severity)}</Tag>
            ),
          },
          {
            key: 'field',
            header: 'Field',
            render: (i) => <CellStack main={i.field ?? ''} sub={i.value ?? undefined} />,
          },
          { key: 'message', header: 'Message', render: (i) => i.message },
          {
            key: 'resolution',
            header: 'Resolution',
            render: (i) => <CellStack main={migLabel(i.resolution)} sub={resolver(i)} />,
          },
          {
            key: 'actions',
            header: '',
            render: (i) =>
              can('MIG_DQ_RESOLVE') && i.resolution === 'OPEN' ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() =>
                    setAction({
                      title: `Record the fix of issue ${i.ruleCode}`,
                      effect:
                        'The legacy source is corrected; the next extract carries the fixed value.',
                      confirmLabel: 'Fixed at Source',
                      reason: 'required',
                      done: 'Resolution recorded',
                      run: (note) => migrationApi.resolveIssue(i.id, 'FIXED_AT_SOURCE', note),
                    })
                  }
                >
                  Fixed at Source
                </Button>
              ) : null,
          },
        ]}
      />
      <PageFooter data={issues.data} noun="issues" onPage={setPage} />
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </Card>
  );
}
