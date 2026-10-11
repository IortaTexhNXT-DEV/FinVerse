import { FileSpreadsheet, Link2, Upload } from 'lucide-react';
import type { BillingBatch } from '@/api/placement';
import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row actions of a CLPC billing batch: download its billing file in Excel or OpenDocument,
 * and, until the batch is closed, upload CLPC's payment report against it.
 */
export function batchActions(
  batch: BillingBatch,
  on: { download: (format: 'XLSX' | 'ODS') => void; upload: () => void },
): RowAction[] {
  return [
    {
      label: 'Download Excel',
      icon: <FileSpreadsheet size={14} />,
      onSelect: () => on.download('XLSX'),
    },
    {
      label: 'Download ODS',
      icon: <FileSpreadsheet size={14} />,
      onSelect: () => on.download('ODS'),
    },
    {
      label: 'Upload Report',
      icon: <Upload size={14} />,
      hidden: batch.status === 'CLOSED',
      onSelect: on.upload,
    },
  ];
}

/** The row action of a payment report line while the report is reviewed: Match it to an account. */
export function lineActions(reviewable: boolean, onMatch: () => void): RowAction[] {
  return [{ label: 'Match', icon: <Link2 size={14} />, hidden: !reviewable, onSelect: onMatch }];
}
