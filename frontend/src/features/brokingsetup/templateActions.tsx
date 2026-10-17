import { FileType2 } from 'lucide-react';
import type { RowAction } from '@/components/ui/RowActions';

/** The row action of a document template version: download it as a Word document. */
export function templateVersionActions(onWord: () => void): RowAction[] {
  return [{ label: 'Download Word', icon: <FileType2 size={14} />, onSelect: onWord }];
}
