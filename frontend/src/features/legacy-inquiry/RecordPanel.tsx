import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { legacyInquiryApi } from '@/api/legacyInquiry';
import type { AccessReason, LegacyDocument } from '@/api/legacyInquiry';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { OriginBadge } from '@/components/ui/OriginBadge';
import { useDownload } from '@/features/migration/common/useDownload';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';

/** A legacy archive record: its keys, all its legacy columns and its documents (read-only). */
export function RecordPanel({
  id,
  reason,
  onClose,
}: Readonly<{ id: number; reason: AccessReason; onClose: () => void }>) {
  const download = useDownload();
  const detail = useQuery({
    queryKey: ['legacy-inquiry', 'record', id, reason],
    queryFn: () => legacyInquiryApi.view(id, reason),
  });
  const d = detail.data;
  const r = d?.record;
  return (
    <Card
      title={
        r === undefined ? (
          'Legacy record'
        ) : (
          <span>
            {r.recordType.replaceAll('_', ' ').toLowerCase()} {r.legacyKey}{' '}
            <OriginBadge
              record={{ origin: 'MIGRATED', sourceSystem: r.sourceSystem, legacyRef: r.legacyKey }}
            />
          </span>
        )
      }
      actions={
        <Button variant="ghost" size="sm" onClick={onClose}>
          Close
        </Button>
      }
    >
      <ErrorAlert error={detail.error ?? download.error} />
      {d !== undefined && r !== undefined && (
        <>
          <DefinitionGrid
            columns={2}
            items={[
              { label: 'Legacy system', value: r.sourceSystem },
              { label: 'Client', value: [r.clientName, r.clientKey].filter(Boolean).join(', ') },
              { label: 'Policy / cover', value: r.policyNo },
              { label: 'Invoice', value: r.invoiceNo },
              { label: 'Receipt', value: r.receiptNo },
              { label: 'Claim', value: r.claimNo },
              { label: 'Date', value: formatDate(r.documentDate) },
              {
                label: 'Period',
                value:
                  d.periodFrom === undefined
                    ? ''
                    : `${formatDate(d.periodFrom)} to ${formatDate(d.periodTo)}`,
              },
              {
                label: 'Amount',
                value:
                  r.amount === undefined ? '' : `${r.currency ?? ''} ${formatAmount(r.amount)}`,
              },
              { label: 'Legacy status', value: r.status },
              { label: 'Archived', value: formatDateTime(d.loadedAt) },
            ]}
          />
          <h3>Legacy details</h3>
          <DefinitionGrid
            columns={2}
            items={Object.entries(d.details).map(([label, value]) => ({ label, value }))}
          />
          <h3>Documents</h3>
          <DataTable<LegacyDocument>
            rows={d.documents}
            rowKey={(x) => String(x.id)}
            emptyMessage="No document archived with this record"
            columns={[
              { key: 'file', header: 'File', render: (x) => x.fileName },
              { key: 'desc', header: 'Document', render: (x) => x.description ?? '' },
              {
                key: 'size',
                header: 'Size',
                numeric: true,
                render: (x) => `${String(Math.ceil(x.sizeBytes / 1024))} KB`,
              },
              {
                key: 'get',
                header: '',
                render: (x) => (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Download ${x.fileName}`}
                    icon={<Download size={14} />}
                    onClick={() =>
                      download.run(() => legacyInquiryApi.document(r.id, x.id, reason))
                    }
                  />
                ),
              },
            ]}
          />
        </>
      )}
    </Card>
  );
}
