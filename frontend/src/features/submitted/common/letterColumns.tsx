import { LovLabel } from '@/components/broking/LovLabel';
import type { LetterView } from '@/api/submitted';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { SBM_LOV } from './submittedCodes';

const CHANNELS: Record<string, string> = {
  EMAIL: 'E-mail',
  PRINT: 'Print',
  BANK_COUNTERPART: 'Bank counterpart',
};

/**
 * Columns of a letter list.
 *
 * @param onOpen downloads a letter
 */
export function letterColumns(onOpen: (id: number) => void): Column<LetterView>[] {
  return [
    { key: 'no', header: 'Letter No.', kind: 'code', render: (l: LetterView) => l.letterNo },
    {
      key: 'type',
      header: 'Letter',
      render: (l: LetterView) => <LovLabel type={SBM_LOV.letterType} code={l.letterType} />,
    },
    {
      key: 'channel',
      header: 'Channel',
      render: (l: LetterView) => CHANNELS[l.channel] ?? l.channel,
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (l: LetterView) => <StatusBadge status={l.status} />,
    },
    { key: 'to', header: 'Recipient', render: (l: LetterView) => l.recipient ?? '—' },
    { key: 'error', header: 'Error', render: (l: LetterView) => l.error ?? '—' },
    {
      key: 'sent',
      header: 'Sent',
      kind: 'datetime',
      render: (l: LetterView) => formatDateTime(l.sentAt),
    },
    {
      key: 'pdf',
      header: 'Letter',
      render: (l: LetterView) =>
        l.storedFileId === null ? (
          '—'
        ) : (
          <Button variant="ghost" onClick={() => onOpen(l.id)}>
            Download
          </Button>
        ),
    },
  ];
}
