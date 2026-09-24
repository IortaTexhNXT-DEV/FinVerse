import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { RetentionRule } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { EligibleDialog, RuleDialog } from './RetentionDialogs';

function eligibleText(r: RetentionRule): string {
  if (!r.providerAvailable) {
    return 'Module not yet reporting';
  }
  return r.lastEligibleCount === undefined ? 'Not reviewed yet' : String(r.lastEligibleCount);
}

/**
 * Data Retention (BRNB.106, NFR 5 years online / 15 years archive): retention rules per record
 * type and status, and the records eligible at the latest monthly review. Nothing is archived or
 * deleted by the system yet: archiving waits for the archive storage decision.
 */
export default function RetentionPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [drill, setDrill] = useState<RetentionRule | null>(null);
  const [editing, setEditing] = useState<RetentionRule | null>(null);
  const rules = useQuery({
    queryKey: ['nbadmin', 'retention'],
    queryFn: nbadminApi.retentionRules,
  });
  const run = useMutation({
    mutationFn: nbadminApi.runRetentionReview,
    onSuccess: async () => {
      toast.success('Retention review completed');
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
    },
  });
  const maintain = can('MASTER_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Data Retention"
        description="Retention periods by record type and status and the records that have become eligible. The RETENTION_REVIEW job counts them every month."
        actions={
          maintain && (
            <Button
              variant="accent"
              icon={<Play size={16} />}
              busy={run.isPending}
              onClick={() => run.mutate()}
            >
              Run Review Now
            </Button>
          )
        }
      />
      <div className="alert warning" role="note">
        Records are only counted and listed. Archiving and purging are not performed until the
        archive storage and backup policy are decided; no data is ever deleted by this screen.
      </div>
      <ErrorAlert error={rules.error ?? run.error} />
      <Card flush>
        <DataTable<RetentionRule>
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          columns={[
            { key: 'type', header: 'Record Type', render: (r) => humanize(r.recordType) },
            {
              key: 'statuses',
              header: 'Statuses',
              render: (r) => r.statuses.split(',').map(humanize).join(', '),
            },
            { key: 'online', header: 'Years Online', numeric: true, render: (r) => r.yearsOnline },
            {
              key: 'archive',
              header: 'Years Archive',
              numeric: true,
              render: (r) => r.yearsArchive,
            },
            { key: 'action', header: 'Action', render: (r) => humanize(r.action) },
            {
              key: 'active',
              header: 'Rule',
              render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            { key: 'last', header: 'Last Review', render: (r) => formatDateTime(r.lastRunAt) },
            { key: 'eligible', header: 'Eligible Records', render: eligibleText },
            {
              key: 'actions',
              header: '',
              render: (r) => (
                <div className="row">
                  <Button size="sm" variant="ghost" onClick={() => setDrill(r)}>
                    Records
                  </Button>
                  {maintain && (
                    <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>
                      Edit
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
      {drill && <EligibleDialog rule={drill} onClose={() => setDrill(null)} />}
      {editing && <RuleDialog rule={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
