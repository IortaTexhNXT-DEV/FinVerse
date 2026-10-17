import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { userAccessApi } from '@/api/userAccess';
import type { SodViolation } from '@/api/userAccess';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatDateTime } from '@/utils/format';
import { permissionLabel } from '@/utils/permissionLabel';
import { saveCsv } from './csv';

const pairOf = (v: SodViolation) =>
  v.kind === 'PERMISSIONS'
    ? `${permissionLabel(v.first)} + ${permissionLabel(v.second)}`
    : `${v.first} + ${v.second}`;

/**
 * The active users who hold access an active rule keeps apart, by rule. UIDM-ISC assigns the group
 * profiles, so each breach is corrected there; the daily check tells Information Security of new
 * ones.
 */
export function SodViolationsTab() {
  const violations = useQuery({
    queryKey: ['nbadmin', 'sod-violations'],
    queryFn: userAccessApi.sodViolations,
  });
  const rows = violations.data ?? [];
  const exportRows = () =>
    saveCsv(
      'separation_of_duties_violations.csv',
      ['Rule', 'Kept Apart', 'Reason', 'User ID', 'Name', 'Group Profiles', 'First Found'],
      rows.map((v) => [
        v.ruleCode,
        pairOf(v),
        v.description,
        v.username,
        v.fullName,
        v.groupProfiles,
        v.foundAt ?? '',
      ]),
    );
  return (
    <div className="stack">
      <div className="row" style={{ justifyContent: 'flex-end' }}>
        <Button
          variant="secondary"
          icon={<Download size={16} />}
          disabled={rows.length === 0}
          onClick={exportRows}
        >
          Export
        </Button>
      </div>
      <ErrorAlert error={violations.error} />
      <Card flush>
        <DataTable<SodViolation>
          caption="Users who breach an active rule"
          loading={violations.isLoading}
          rows={rows}
          rowKey={(v) => `${v.ruleCode}-${v.username}`}
          emptyMessage="No user breaches an active rule"
          columns={[
            { key: 'r', header: 'Rule', kind: 'code', render: (v) => v.ruleCode },
            { key: 'p', header: 'Kept Apart', render: pairOf },
            { key: 'u', header: 'User ID', kind: 'code', render: (v) => v.username },
            { key: 'n', header: 'Name', render: (v) => v.fullName },
            { key: 'g', header: 'Group Profiles', render: (v) => v.groupProfiles },
            {
              key: 'f',
              header: 'First Found',
              kind: 'datetime',
              render: (v) => formatDateTime(v.foundAt) || 'At the next daily check',
            },
          ]}
        />
      </Card>
    </div>
  );
}
