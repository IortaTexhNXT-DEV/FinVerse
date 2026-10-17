import { Pencil, Send } from 'lucide-react';
import type { InsurerResponse } from '@/api/productmaint';
import type { Column } from '@/components/ui/DataTable';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, formatRate } from '@/utils/format';

/** Columns of the insurer responses of a negotiation round. */
export function responseColumns(
  canEdit: boolean,
  onEdit: (r: InsurerResponse) => void,
  onResend: (r: InsurerResponse) => void,
): Column<InsurerResponse>[] {
  return [
    { key: 'insurer', header: 'Insurer', render: (r) => <strong>{r.insurerName}</strong> },
    { key: 'outcome', header: 'Outcome', render: (r) => <StatusBadge status={r.outcome} /> },
    { key: 'rate', header: 'Rate %', numeric: true, render: (r) => formatRate(r.rate, '—') },
    {
      key: 'min',
      header: 'Minimum',
      numeric: true,
      render: (r) => (r.minimumPremium === undefined ? '—' : formatAmount(r.minimumPremium)),
    },
    { key: 'conditions', header: 'Conditions', render: (r) => r.conditions ?? '—' },
    { key: 'valid', header: 'Valid Until', render: (r) => formatDate(r.validUntil) },
    { key: 'rev', header: 'Rev.', numeric: true, render: (r) => r.revision },
    {
      // Key In and Resend in the row action menu (screen standard), never buttons in the row.
      key: 'actions',
      header: '',
      kind: 'actions',
      render: (r) => (
        <RowActions
          record={r.insurerName}
          actions={[
            {
              label: 'Key In',
              icon: <Pencil size={14} />,
              hidden: !canEdit,
              onSelect: () => onEdit(r),
            },
            {
              label: 'Resend',
              icon: <Send size={14} />,
              hidden: !canEdit,
              onSelect: () => onResend(r),
            },
          ]}
        />
      ),
    },
  ];
}
