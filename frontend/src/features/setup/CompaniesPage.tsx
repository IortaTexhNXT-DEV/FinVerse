import { useMutation, useQueryClient } from '@tanstack/react-query';
import { organizationApi } from '@/api/organization';
import type { Company } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useWorkspace } from '@/context/workspaceContext';
import { awaitsOtherChecker } from '@/utils/makerChecker';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Legal entities keeping their own books (base currency, fiscal year, posting windows). */
export default function CompaniesPage() {
  const { companies } = useWorkspace();
  const { can, user } = useAuth();
  const queryClient = useQueryClient();
  const authorize = useMutation({
    mutationFn: (id: number) => organizationApi.authorizeCompany(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies'] }),
  });

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Companies"
        description="Each company keeps its own books and can be consolidated at group level."
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Company>
          rows={companies}
          rowKey={(c) => c.id}
          columns={[
            { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
            { key: 'n', header: 'Legal name', render: (c) => c.name },
            { key: 't', header: 'TIN', render: (c) => c.taxId ?? '' },
            { key: 'b', header: 'Base currency', render: (c) => c.baseCurrency },
            {
              key: 'f',
              header: 'Fiscal year starts',
              render: (c) => MONTHS[c.fiscalYearStartMonth - 1] ?? '',
            },
            {
              key: 'w',
              header: 'Back / forward value days',
              render: (c) => `${c.backValueDays} / ${c.forwardValueDays}`,
            },
            {
              key: 'r',
              header: 'Retained earnings a/c',
              render: (c) => c.retainedEarningsAccount ?? '',
            },
            { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (c) =>
                awaitsOtherChecker(c, user?.username) &&
                can('MASTER_AUTHORIZE') && (
                  <Button size="sm" variant="secondary" onClick={() => authorize.mutate(c.id)}>
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
