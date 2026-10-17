import { ExternalLink, FilePlus2, UserPlus, XCircle } from 'lucide-react';
import type { QuotationRequest } from '@/api/quotations';
import type { RowAction } from '@/components/ui/RowActions';
import { quotationLinkOf } from './requestForm';

/**
 * The row actions of a quotation request: Open Quotation once quoted; while new, Create Prospect
 * (no client yet) or Create Quotation, and Close (with its reason, asked by the page's dialog).
 */
export function requestActions(
  request: QuotationRequest,
  canMaintain: boolean,
  on: { open: (to: string) => void; prospect: () => void; close: () => void },
): RowAction[] {
  if (request.status === 'QUOTED' && request.quotationId !== undefined) {
    const to = `/quotations/${String(request.quotationId)}`;
    return [
      { label: 'Open Quotation', icon: <ExternalLink size={14} />, onSelect: () => on.open(to) },
    ];
  }
  const maintain = request.status === 'NEW' && canMaintain;
  return [
    {
      label: 'Create Prospect',
      icon: <UserPlus size={14} />,
      hidden: !maintain || request.clientId !== undefined,
      onSelect: on.prospect,
    },
    {
      label: 'Create Quotation',
      icon: <FilePlus2 size={14} />,
      hidden: !maintain || request.clientId === undefined,
      onSelect: () => on.open(quotationLinkOf(request)),
    },
    {
      label: 'Close',
      icon: <XCircle size={14} />,
      hidden: !maintain,
      danger: true,
      onSelect: on.close,
    },
  ];
}
