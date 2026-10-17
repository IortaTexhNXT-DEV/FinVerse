import { useQueryClient } from '@tanstack/react-query';
import type { PackageRequest, RequestStage } from '@/api/productmaintTypes';
import { TermsTablePanel } from './TermsTablePanel';

const OPEN: readonly RequestStage[] = [
  'NEGOTIATION',
  'TERMS_REVIEW',
  'FOR_MKT_REVIEW',
  'REQUIREMENTS_PREP',
];

/**
 * The comparative table of a package request (BDOI FRS FRPM.012.02, FRPM.013.01): the latest
 * round's insurer terms, the Final Terms for Proposal, the insurers selected and the proposal
 * slips generated for them.
 */
export function PackageTermsPanel({ request }: Readonly<{ request: PackageRequest }>) {
  const queryClient = useQueryClient();
  const open = OPEN.includes(request.status);
  return (
    <TermsTablePanel
      type="package"
      id={request.id}
      keyInPermission="PKG_NEGOTIATE"
      selectPermissions={['PKG_NEGOTIATE', 'PKG_REQUEST']}
      selectable={open}
      generatable={open}
      onChanged={() =>
        void queryClient.invalidateQueries({ queryKey: ['package-request', request.id] })
      }
    />
  );
}
