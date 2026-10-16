import { useQuery } from '@tanstack/react-query';
import { changeHistoryApi } from '@/api/changeHistory';
import type { FieldChange } from '@/api/changeHistory';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { checkerState, fieldLabel } from './changeLabels';

interface ChangeHistoryProps {
  /** Record type of the change log, e.g. InsurerProfile, RiskProduct, Client. */
  entityType: string;
  entityId: number;
  /** False for records outside maker-checker (clients): no checker is expected. */
  makerChecker?: boolean;
}

/**
 * Field-level change history of a master record: field, old and new value, maker and time of the
 * change, checker and time of the authorization, latest first.
 */
export function ChangeHistory({
  entityType,
  entityId,
  makerChecker = true,
}: Readonly<ChangeHistoryProps>) {
  const history = useQuery({
    queryKey: ['change-history', entityType, entityId],
    queryFn: () => changeHistoryApi.history(entityType, entityId),
  });
  return (
    <Card title="Change History" flush>
      <ErrorAlert error={history.error} />
      <DataTable<FieldChange>
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(c) => c.id}
        emptyMessage="No change recorded yet."
        columns={[
          { key: 'f', header: 'Field', render: (c) => fieldLabel(c) },
          { key: 'o', header: 'Old value', render: (c) => c.oldValue ?? '' },
          { key: 'n', header: 'New value', render: (c) => c.newValue ?? '' },
          { key: 'm', header: 'Maker', render: (c) => <UserName login={c.maker} /> },
          { key: 'ma', header: 'Changed', render: (c) => formatDateTime(c.madeAt) },
          { key: 'c', header: 'Checker', render: (c) => checkerState(c, makerChecker) },
          { key: 'ca', header: 'Authorized', render: (c) => formatDateTime(c.checkedAt) },
        ]}
      />
    </Card>
  );
}
