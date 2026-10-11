import type { RowAction } from '@/components/ui/RowActions';

/** The row actions of a retention rule: Records (the eligible records) and, to its maintainer, Edit. */
export function retentionActions(
  maintain: boolean,
  onRecords: () => void,
  onEdit: () => void,
): RowAction[] {
  return [
    { label: 'Records', onSelect: onRecords },
    { label: 'Edit', hidden: !maintain, onSelect: onEdit },
  ];
}
