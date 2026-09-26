import { useQuery } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { approvalsApi } from '@/api/approvals';
import type { PendingApproval } from '@/api/approvals';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { ageInDays, oldestAge } from './age';

/**
 * Universal approval inbox: everything waiting for the signed-in user's authorization across
 * modules (journals, master data and any module that registers an inbox source), oldest first.
 */
export default function MyApprovalsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [allCompanies, setAllCompanies] = useState(false);
  const scope = allCompanies ? undefined : companyId || undefined;
  const inbox = useQuery({
    queryKey: ['approvals', 'inbox', scope],
    queryFn: () => approvalsApi.inbox(scope),
  });
  const items = inbox.data ?? [];
  const modules = [...new Set(items.map((i) => i.module))].sort((a, b) => a.localeCompare(b));

  return (
    <div className="stack">
      <PageHeader
        section="Overview"
        title="My Approvals"
        description="Items waiting for your authorization. Your own submissions are never shown here (maker-checker)."
        actions={
          <>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={allCompanies}
                onChange={(e) => setAllCompanies(e.target.checked)}
              />
              All companies
            </label>
            <Button
              variant="secondary"
              icon={<RefreshCw size={16} />}
              onClick={() => void inbox.refetch()}
            >
              Refresh
            </Button>
          </>
        }
      />
      <ErrorAlert error={inbox.error} />
      <div className="grid-4">
        <Kpi label="Waiting for you" value={items.length} accent />
        {modules.map((m) => (
          <Kpi
            key={m}
            label={humanize(m)}
            value={items.filter((i) => i.module === m).length}
            hint={`Oldest ${oldestAge(items.filter((i) => i.module === m).map((i) => i.submittedAt))} day(s)`}
          />
        ))}
      </div>
      <Card flush>
        <DataTable<PendingApproval>
          loading={inbox.isLoading}
          rows={items}
          rowKey={(i) => `${i.module}:${i.type}:${i.reference}`}
          emptyMessage="Nothing is waiting for your approval."
          onRowClick={(i) => {
            if (i.link !== undefined) {
              void navigate(i.link);
            }
          }}
          columns={[
            { key: 'm', header: 'Module', render: (i) => humanize(i.module) },
            { key: 't', header: 'Type', render: (i) => i.type },
            { key: 'r', header: 'Reference', render: (i) => <strong>{i.reference}</strong> },
            { key: 'd', header: 'Description', render: (i) => i.description ?? '' },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (i) =>
                i.amount === undefined ? (
                  ''
                ) : (
                  <>
                    <Amount value={i.amount} /> <span className="muted">{i.currency}</span>
                  </>
                ),
            },
            { key: 'b', header: 'Submitted by', render: (i) => i.submittedBy ?? '' },
            { key: 's', header: 'Submitted', render: (i) => formatDateTime(i.submittedAt) },
            {
              key: 'g',
              header: 'Age (days)',
              numeric: true,
              render: (i) => ageInDays(i.submittedAt),
            },
          ]}
        />
      </Card>
    </div>
  );
}
