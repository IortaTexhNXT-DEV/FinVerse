import { PeriodCell } from '@/components/ui/PeriodCell';
import { CellStack } from '@/components/ui/CellStack';
import { UserName } from '@/components/ui/UserName';
import type { Column } from '@/components/ui/DataTable';
import { formatDateTime, humanize } from '@/utils/format';
import { ExtractActions } from './ExtractParts';
import type { ReconExtract } from './prodreconApi';

/** Columns of an extract list. */
export function extractColumns(
  onSend: ((e: ReconExtract) => void) | undefined,
): Column<ReconExtract>[] {
  return [
    {
      key: 'no',
      header: 'Extract No.',
      render: (e) => <strong>{e.extractNo}</strong>,
    },
    { key: 'trigger', header: 'Trigger', render: (e) => humanize(e.trigger) },
    {
      key: 'period',
      header: 'Booking Period',
      kind: 'period',
      render: (e) => <PeriodCell from={e.bookingFrom} to={e.bookingTo} />,
    },
    { key: 'rows', header: 'Accounts', numeric: true, render: (e) => e.rowCount },
    { key: 'new', header: 'New', numeric: true, render: (e) => e.newCount },
    { key: 'created', header: 'Extracted', render: (e) => formatDateTime(e.createdAt) },
    {
      key: 'sent',
      header: 'Sent',
      render: (e) =>
        e.sentAt ? (
          <CellStack main={formatDateTime(e.sentAt)} sub={<UserName login={e.sentBy} />} />
        ) : (
          'Not sent'
        ),
    },
    {
      key: 'actions',
      header: '',
      render: (e) => <ExtractActions extract={e} onSend={onSend} />,
    },
  ];
}
