import { useQuery } from '@tanstack/react-query';
import { csfApi } from '@/api/csf';
import type { ChangeField, ContactChange } from '@/api/csf';
import { useLovLabel } from '@/components/broking/useLabels';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { CSF_LOV, contactFieldLabel } from '../csfCodes';
import { TAB_UNAVAILABLE } from './AccountsTab';
import { useChangeColumns } from './changeColumns';

/** Field rows of a change: the value before and after, or the value asked for. */
export function ChangeFields({ change }: Readonly<{ change: ContactChange }>) {
  const referral = useLovLabel(CSF_LOV.referral);
  const columns: Column<ChangeField>[] = [
    {
      key: 'field',
      header: 'Field',
      render: (f) => contactFieldLabel(f.field) ?? referral(f.field),
    },
    { key: 'old', header: 'Old Value', render: (f) => f.oldValue ?? '' },
    {
      key: 'new',
      header: change.status === 'APPLIED' ? 'New Value' : 'Asked For',
      render: (f) => f.newValue ?? '',
    },
  ];
  return (
    <DataTable
      columns={columns}
      rows={change.fields}
      rowKey={(f) => f.field}
      caption={`Fields of ${change.changeNo}`}
    />
  );
}

/**
 * The Contact History tab (FR-CSF-021, 022, 040): contact changes with the values before and
 * after, refused changes and referrals, newest first, with the verification, agent, reason and the
 * sending to the legacy systems.
 */
export function HistoryTab({
  companyId,
  clientId,
}: Readonly<{ companyId: number; clientId: number }>) {
  const columns = useChangeColumns(false);
  const list = useQuery({
    queryKey: ['csf', 'history', companyId, clientId],
    queryFn: () => csfApi.history(companyId, clientId),
  });
  const rows = list.data ?? [];
  return (
    <Card flush>
      <ErrorAlert error={list.error} title={TAB_UNAVAILABLE} onRetry={() => void list.refetch()} />
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(c) => c.id}
        loading={list.isLoading}
        expanded={new Set(rows.map((c) => c.id))}
        renderExpanded={(c) => <ChangeFields change={c} />}
        caption="Contact history"
      />
    </Card>
  );
}
