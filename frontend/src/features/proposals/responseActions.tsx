import { Pencil, Star } from 'lucide-react';
import type { InsurerResponse } from '@/api/proposals';
import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row actions of an insurer response while TSU collects the terms: Terms (key in or revise),
 * and Recommend for terms received that are not yet the recommendation.
 */
export function responseActions(
  r: InsurerResponse,
  editable: boolean,
  onTerms: (r: InsurerResponse) => void,
  onRecommend: (r: InsurerResponse) => void,
): RowAction[] {
  return [
    { label: 'Terms', icon: <Pencil size={14} />, hidden: !editable, onSelect: () => onTerms(r) },
    {
      label: 'Recommend',
      icon: <Star size={14} />,
      hidden: !editable || r.status !== 'RECEIVED' || r.recommended,
      onSelect: () => onRecommend(r),
    },
  ];
}
