import { Pencil, Trash2 } from 'lucide-react';
import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row actions of an account queued for the batch booking: Edit (booking date and cost
 * center), then Remove from the queue (a reversing action: last, in red).
 */
export function queuedActions(onEdit: () => void, onRemove: () => void): RowAction[] {
  return [
    { label: 'Edit', icon: <Pencil size={14} />, onSelect: onEdit },
    { label: 'Remove', icon: <Trash2 size={14} />, danger: true, onSelect: onRemove },
  ];
}
