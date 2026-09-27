import type { Approval } from '@/api/submitted';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime, humanize } from '@/utils/format';

/** The approval of an IAAF or TOR: status, level, the levels of its band and the signatures. */
export function ApprovalPanel({ approval }: Readonly<{ approval: Approval }>) {
  const signed = new Map(approval.signatures.map((s) => [s.level, s]));
  return (
    <Card title="Approval" flush>
      <div className="card-body">
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'Status', value: <StatusBadge status={approval.status} /> },
            {
              label: 'Level',
              value:
                approval.totalLevels === 0
                  ? null
                  : `${String(approval.currentLevel)} of ${String(approval.totalLevels)}`,
            },
            { label: 'Prepared By', value: <UserName login={approval.preparedBy} /> },
            { label: 'Prepared', value: formatDateTime(approval.preparedAt) },
            { label: 'Submitted', value: formatDateTime(approval.submittedAt) },
            { label: 'Return Reason', value: approval.returnReason },
          ]}
        />
      </div>
      <DataTable
        rows={approval.levels}
        rowKey={(l) => l.level}
        emptyMessage="No approval level applies"
        columns={[
          { key: 'level', header: 'Level', kind: 'amount', render: (l) => l.level },
          { key: 'title', header: 'Signatory', render: (l) => l.signatoryTitle },
          {
            key: 'approver',
            header: 'Approver',
            render: (l) =>
              l.approverUsername ? <UserName login={l.approverUsername} /> : humanize(l.permission),
          },
          {
            key: 'signed',
            header: 'Signed',
            render: (l) => {
              const s = signed.get(l.level);
              return s ? `${s.signerName}, ${formatDateTime(s.signedAt)}` : '—';
            },
          },
        ]}
      />
    </Card>
  );
}
