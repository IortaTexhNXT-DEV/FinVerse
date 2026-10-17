import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { GateRow } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { GATES, MIG_SECTION, migLabel } from '../common/migrationCodes';
import '../migration.css';

function gateCell(row: GateRow, gate: string) {
  const cell = row.gates[gate];
  if (cell === undefined) {
    return <span className="muted">—</span>;
  }
  return (
    <span className="mig-gate">
      <MigStatus status={cell.decision} />
      <span className="muted">
        {cell.signedBy}, {formatDate(cell.signedAt)}
      </span>
    </span>
  );
}

/**
 * Sign-off (DATA_MIGRATION_DESIGN section 13): the gates G1 to G7 of every object and of its latest
 * batch - decision, mapping, validation, load approval, reconciliation, acceptance and go-live -
 * with who signed and when; the business owner signs the mapping of an object here. Batch gates
 * are signed on the batch page.
 */
export default function SignoffPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [action, setAction] = useState<MigAction>();
  const matrix = useQuery({
    queryKey: ['migration', 'matrix', companyId],
    queryFn: () => migrationApi.gateMatrix(companyId),
    enabled: companyId > 0,
  });
  const columns: Column<GateRow>[] = [
    {
      key: 'object',
      header: 'Object',
      render: (r) => (
        <CellStack main={`${r.objectCode} ${r.objectName}`} sub={migLabel(r.objectClass)} />
      ),
    },
    {
      key: 'batch',
      header: 'Latest batch',
      render: (r) =>
        r.batchNo ? (
          <CellStack
            main={<Link to={`/migration/batches/${r.batchNo}`}>{r.batchNo}</Link>}
            sub={migLabel(r.batchStatus)}
          />
        ) : (
          ''
        ),
    },
    ...GATES.map((g) => ({
      key: g.id,
      header: `${g.id} ${g.label}`,
      render: (r: GateRow) => gateCell(r, g.id),
    })),
  ];
  if (can('MIG_MAPPING_APPROVE')) {
    columns.push({
      key: 'actions',
      header: '',
      render: (r) =>
        r.gates.G1?.decision === 'APPROVED' && r.gates.G2?.decision !== 'APPROVED' ? (
          <Button
            variant="secondary"
            size="sm"
            onClick={() =>
              setAction({
                title: `Sign the mapping of ${r.objectCode}`,
                record: r.objectCode,
                effect:
                  'Gate G2: the layouts of the object are frozen and every code map its columns use has an approved version.',
                confirmLabel: 'Sign Mapping',
                reason: 'optional',
                done: 'Mapping signed',
                run: (comment) =>
                  migrationApi.signMapping(companyId, r.objectCode, { approve: true, comment }),
              })
            }
          >
            Sign Mapping
          </Button>
        ) : null,
    });
  }
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Sign-off"
        description="Gates per object and batch, from the class decision to the go-live."
      />
      <Card flush>
        <ErrorAlert error={matrix.error} onRetry={() => void matrix.refetch()} />
        <DataTable<GateRow>
          loading={matrix.isLoading}
          rows={matrix.data ?? []}
          rowKey={(r) => r.objectCode}
          emptyMessage="No data object"
          columns={columns}
        />
      </Card>
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}
