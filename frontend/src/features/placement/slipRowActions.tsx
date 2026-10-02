import { FileSpreadsheet, FileText, RefreshCw, Send } from 'lucide-react';
import type { Slip } from '@/api/placement';
import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row actions of a placement slip: the PDF and Excel downloads for every user; Send (Resend
 * once sent) and, for a sent slip, Regenerate for a user who manages placement while the slip is
 * not superseded.
 */
export function slipActions(
  slip: Slip,
  manage: boolean,
  on: { download: (format: 'pdf' | 'xlsx') => void; send: () => void; regenerate: () => void },
): RowAction[] {
  const sent = slip.status === 'SENT';
  return [
    { label: 'Download PDF', icon: <FileText size={14} />, onSelect: () => on.download('pdf') },
    {
      label: 'Download Excel',
      icon: <FileSpreadsheet size={14} />,
      onSelect: () => on.download('xlsx'),
    },
    {
      label: sent ? 'Resend' : 'Send',
      icon: <Send size={14} />,
      hidden: !manage,
      onSelect: on.send,
    },
    {
      label: 'Regenerate',
      icon: <RefreshCw size={14} />,
      hidden: !manage || !sent,
      onSelect: on.regenerate,
    },
  ];
}
