import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { DownloadButton } from './ExtractParts';
import type { ReconExtract } from './prodreconApi';

/** Columns of an extract list. */
export function extractColumns(
  onSend: ((e: ReconExtract) => void) | undefined,
): Column<ReconExtract>[] {
  return [
    {
      key: 'no',
      header: 'Extract No.',
      render: (e) => (
        <>
          <strong>{e.extractNo}</strong>
          <div className="muted">{e.fileName}</div>
        </>
      ),
    },
    { key: 'trigger', header: 'Trigger', render: (e) => humanize(e.trigger) },
    {
      key: 'period',
      header: 'Booking Period',
      render: (e) => `${formatDate(e.bookingFrom)} – ${formatDate(e.bookingTo)}`,
    },
    { key: 'rows', header: 'Accounts', numeric: true, render: (e) => e.rowCount },
    { key: 'new', header: 'New', numeric: true, render: (e) => e.newCount },
    { key: 'created', header: 'Extracted', render: (e) => formatDateTime(e.createdAt) },
    {
      key: 'sent',
      header: 'Sent',
      render: (e) => (e.sentAt ? `${formatDateTime(e.sentAt)} · ${e.sentBy ?? ''}` : 'Not sent'),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (e) => (
        <div className="row">
          <DownloadButton extract={e} />
          {onSend !== undefined && (
            <Button size="sm" variant="secondary" onClick={() => onSend(e)}>
              {e.sentAt ? 'Resend' : 'Send'}
            </Button>
          )}
        </div>
      ),
    },
  ];
}
