import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { consolidationApi } from '@/api/consolidation';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { GroupForm } from './GroupForm';
import { GroupPanel } from './GroupPanel';
import { RunResults } from './RunResults';
import { RunsTable } from './RunsTable';
import { useConsolidationRuns } from './useConsolidationRuns';

/** Consolidation groups, runs (translation and eliminations) and consolidated results. */
export default function ConsolidationPage() {
  const companyId = useCompanyId();
  const { companies } = useWorkspace();
  const { can } = useAuth();
  const [groupId, setGroupId] = useState<number | undefined>();
  const [asOf, setAsOf] = useState(today());
  const [creating, setCreating] = useState(false);
  const code = (id?: number) => companies.find((c) => c.id === id)?.code ?? 'GROUP';
  const manage = can('CONSOLIDATION_RUN');

  const groups = useQuery({ queryKey: ['con-groups'], queryFn: consolidationApi.groups });
  const allGroups = groups.data ?? [];
  const group = allGroups.find((g) => g.id === groupId) ?? allGroups[0];
  const runs = useConsolidationRuns(group?.id ?? 0, asOf);

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="Consolidation"
        description="Translate members (balance sheet at closing, P&L at average rate, CTA to equity), eliminate inter-company balances and investment against equity."
        actions={
          <Button
            variant="secondary"
            icon={<Plus size={16} />}
            disabled={!manage}
            onClick={() => setCreating(true)}
          >
            New group
          </Button>
        }
      />
      <ErrorAlert error={groups.error ?? runs.error} />
      <GroupPanel
        groups={allGroups}
        group={group}
        asOf={asOf}
        canRun={manage}
        running={runs.running}
        companyCode={code}
        onGroup={(id) => {
          setGroupId(id);
          runs.select(undefined);
        }}
        onAsOf={setAsOf}
        onRun={runs.execute}
      />
      <RunsTable
        runs={runs.runs}
        loading={runs.loading}
        canFinalize={manage}
        onSelect={(r) => runs.select(r.id)}
        onFinalize={runs.finalize}
      />
      {runs.run && group && <RunResults run={runs.run} groupCode={group.code} companyCode={code} />}
      {creating && (
        <GroupForm
          open
          companies={companies}
          parentCompanyId={companyId}
          onClose={() => setCreating(false)}
        />
      )}
    </div>
  );
}
