import { FileText, Send } from 'lucide-react';
import type { RowAction } from '@/components/ui/RowActions';

/** The row action of an e-policy ready to dispatch: Send it to the client (E-policy Sender only). */
export function dispatchActions(sender: boolean, onSend: () => void): RowAction[] {
  return [{ label: 'Send', icon: <Send size={14} />, hidden: !sender, onSelect: onSend }];
}

/** The row action of an Insurance Advice: download its PDF. */
export function adviceActions(onDownload: () => void): RowAction[] {
  return [{ label: 'Download PDF', icon: <FileText size={14} />, onSelect: onDownload }];
}
