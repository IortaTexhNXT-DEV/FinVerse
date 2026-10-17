import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseQueue } from '@/api/csfCases';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { CSF_SECTION } from '../csfCodes';
import { queuesFor } from './caseCodes';
import { CASE_COLUMNS } from './caseColumns';
import { LogCaseDialog } from './LogCaseDialog';

/**
 * Cases (FR-CSF-052, 056, 060): the work queues of the contact centre - My Cases, Returned to Me,
 * CSR Action Required, the Team Queue of the supervisors, the Fulfilment Unit Queue, All Open and
 * Closed - with the status, the due date and the SLA indicator of each case. A caller who is not a
 * client is logged from here.
 */
export default function CasesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queues = queuesFor(can);
  const [params, setParams] = useSearchParams();
  const wanted = params.get('queue') as CaseQueue | null;
  const active = queues.find((q) => q.id === wanted)?.id ?? queues[0]?.id ?? 'OPEN';
  const [logging, setLogging] = useState(false);
  const list = useQuery({
    queryKey: ['csf-cases', 'queue', companyId, active],
    queryFn: () => csfCaseApi.queue(companyId, active),
  });
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        title="Cases"
        description="Inquiries, requests, problems and complaints of the clients, from logging to closure."
        actions={
          can('CSF_CASE_CREATE') && (
            <Button
              variant="accent"
              icon={<Plus size={16} aria-hidden="true" />}
              onClick={() => setLogging(true)}
            >
              Log Case
            </Button>
          )
        }
      />
      <Tabs
        tabs={queues.map((q) => ({ id: q.id, label: q.label }))}
        active={active}
        onChange={(id) => setParams({ queue: id })}
      />
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable
          columns={CASE_COLUMNS}
          rows={list.data ?? []}
          rowKey={(c) => c.id}
          loading={list.isLoading}
          emptyMessage="No cases in this queue"
          caption="Cases"
        />
      </Card>
      {logging && <LogCaseDialog companyId={companyId} onClose={() => setLogging(false)} />}
    </div>
  );
}
