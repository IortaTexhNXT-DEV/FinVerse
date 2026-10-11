import { useQuery } from '@tanstack/react-query';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { api } from '@/api/client';
import type { CollectionItem } from './api';
import { CellStack } from '@/components/ui/CellStack';

/** A follow-up row carried from legacy (disposition, promise, installment or collector). */
export interface LegacyFollowUp {
  legacyInvoiceNo: string;
  sourceSystem: string;
  migrationBatch: string;
  recordType: 'DISPOSITION' | 'PROMISE' | 'INSTALLMENT' | 'ASSIGNMENT';
  seqNo: number;
  dispositionCode?: string;
  dispositionDate?: string;
  promiseDate?: string;
  promiseAmount?: number;
  installmentNo?: number;
  installmentDue?: string;
  installmentAmount?: number;
  collector?: string;
  remarks?: string;
}

const followUpOf = (companyId: number, invoiceNo: string) =>
  api.get<LegacyFollowUp[]>(
    `/collections/items/${encodeURIComponent(invoiceNo)}/legacy-follow-up?companyId=${String(companyId)}`,
  );

function detail(r: LegacyFollowUp): string {
  switch (r.recordType) {
    case 'DISPOSITION':
      return [humanize(r.dispositionCode ?? ''), formatDate(r.dispositionDate)]
        .filter(Boolean)
        .join(' on ');
    case 'PROMISE':
      return `Promised for ${formatDate(r.promiseDate)}`;
    case 'INSTALLMENT':
      return `Installment ${String(r.installmentNo ?? '')} due ${formatDate(r.installmentDue)}`;
    default:
      return `Collector ${r.collector ?? ''}`;
  }
}

/**
 * The collection follow-up carried from legacy for a legacy invoice migrated at cut-over: the
 * latest disposition, promises to pay, installments and the collector, as they stood in legacy.
 */
export function LegacyFollowUpCard({ item }: Readonly<{ item: CollectionItem }>) {
  const companyId = useCompanyId();
  const migrated = item.origin === 'MIGRATED';
  const rows = useQuery({
    queryKey: ['collections', 'legacy-follow-up', item.invoiceNo],
    queryFn: () => followUpOf(companyId, item.invoiceNo),
    enabled: companyId > 0 && migrated,
  });
  if (!migrated) {
    return null;
  }
  return (
    <Card title="Follow-up Carried from Legacy">
      <ErrorAlert error={rows.error} />
      <DataTable<LegacyFollowUp>
        caption="Legacy follow-up"
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => `${r.recordType}-${String(r.seqNo)}`}
        emptyMessage="No follow-up was carried from legacy"
        columns={[
          { key: 'type', header: 'Kind', render: (r) => humanize(r.recordType) },
          { key: 'detail', header: 'Follow-up', render: detail },
          {
            key: 'amount',
            header: 'Amount',
            numeric: true,
            render: (r) => {
              const value = r.promiseAmount ?? r.installmentAmount;
              return value === undefined ? '' : <Amount value={value} />;
            },
          },
          { key: 'remarks', header: 'Remarks', render: (r) => r.remarks ?? '' },
          {
            key: 'source',
            header: 'Source',
            render: (r) => <CellStack main={r.sourceSystem} sub={r.legacyInvoiceNo} />,
          },
        ]}
      />
    </Card>
  );
}
