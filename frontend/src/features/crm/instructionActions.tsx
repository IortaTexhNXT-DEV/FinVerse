import { Pencil, XCircle } from 'lucide-react';
import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row actions of a special instruction in force, for a user who maintains the client: Change,
 * then End (a reversing action: last, in red).
 */
export function instructionActions(
  maintain: boolean,
  onChange: () => void,
  onEnd: () => void,
): RowAction[] {
  return [
    { label: 'Change', icon: <Pencil size={14} />, hidden: !maintain, onSelect: onChange },
    { label: 'End', icon: <XCircle size={14} />, hidden: !maintain, danger: true, onSelect: onEnd },
  ];
}
