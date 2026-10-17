import type { InsurerRow, RenewalDashboard } from '@/api/renewalDashboard';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { UserName } from '@/components/ui/UserName';
import { DrillCount } from './dashboardBits';
import { percent } from './dashboardFormat';
import type { Open } from './dashboardWidgets';

type OpenFn = (o: Open) => void;

const APPROVALS = [
  ['totalRenewAsIs', 'Total Renew As Is', 'TOTAL'],
  ['approved', 'Insurer Approved', 'APPROVED'],
  ['pending', 'Pending for Insurer Approval', 'PENDING'],
  ['returned', 'Return to Marketing', 'RETURNED'],
] as const;

/** Insurer Renewal Approval Monitoring and the pending accounts per insurer (FRRN.002.02.08). */
export function InsurerTables({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  const group = (g: string) => (g === 'ALL' ? 'Total' : g);
  return (
    <div className="rnw-grid">
      <Card title="Insurer Renewal Approval Monitoring" flush>
        <DataTable<InsurerRow>
          rows={d.insurer}
          rowKey={(r) => r.group}
          emptyMessage="No accounts For Renewal"
          columns={[
            {
              key: 'g',
              header: d.filter.segment === null ? 'Market Segment' : 'Unit',
              render: (r) => group(r.group),
            },
            ...APPROVALS.map(([k, label, what]) => ({
              key: k,
              header: label,
              kind: 'amount' as const,
              render: (r: InsurerRow) => (
                <DrillCount
                  value={r[k].toLocaleString()}
                  label={`${label} of ${group(r.group)}`}
                  onOpen={() =>
                    open({
                      metric: `INSURER|${r.group}|${what}`,
                      title: `${label} - ${group(r.group)}`,
                    })
                  }
                />
              ),
            })),
            {
              key: 'pct',
              header: '% of Approval',
              kind: 'amount',
              render: (r) => percent(r.approvalRatio),
            },
          ]}
        />
      </Card>
      <Card title="Pending for Insurer Approval by Insurer" flush>
        <DataTable
          rows={d.insurerPending}
          rowKey={(r) => r.key}
          emptyMessage="No pending accounts"
          columns={[
            { key: 'i', header: 'Insurer Name', render: (r) => r.key },
            {
              key: 'n',
              header: 'Number of Pending Accounts',
              kind: 'amount',
              render: (r) => (
                <DrillCount
                  value={r.count.toLocaleString()}
                  label={`Pending accounts of ${r.key}`}
                  onOpen={() =>
                    open({ metric: r.metric, title: `Pending for Insurer Approval - ${r.key}` })
                  }
                />
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

/** Open renewal accounts per officer of the scope (FRRN.002.02). */
export function WorkloadTable({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  return (
    <Card title="Open Renewal Accounts per Officer" flush>
      <DataTable
        rows={d.workload}
        rowKey={(r) => r.key}
        emptyMessage="No renewal accounts assigned"
        columns={[
          { key: 'u', header: 'Officer', render: (r) => <UserName login={r.key} /> },
          {
            key: 'n',
            header: 'Open Renewal Accounts',
            kind: 'amount',
            render: (r) => (
              <DrillCount
                value={r.count.toLocaleString()}
                label={`Accounts of ${r.key}`}
                onOpen={() =>
                  open({ metric: r.metric, title: 'Open Renewal Accounts of the Officer' })
                }
              />
            ),
          },
        ]}
      />
    </Card>
  );
}
