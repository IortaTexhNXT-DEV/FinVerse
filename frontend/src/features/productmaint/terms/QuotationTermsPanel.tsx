import { useQueryClient } from '@tanstack/react-query';
import type { Proposal } from '@/api/proposals';
import { TermsTablePanel } from './TermsTablePanel';

/**
 * The comparative table of a quotation request of a non-package product (BDOI FRS FRPM.006.02,
 * FRPM.008.01, FRPM.009.01, FRPM.009.02): the requestor selects the insurers while the terms come
 * in, TSU generates the proposal once the terms are agreed.
 */
export function QuotationTermsPanel({ proposal }: Readonly<{ proposal: Proposal }>) {
  const queryClient = useQueryClient();
  return (
    <TermsTablePanel
      type="quotation"
      id={proposal.id}
      keyInPermission="TSU_PROCESS"
      selectPermissions={['PROPOSAL_REQUEST', 'TSU_PROCESS']}
      selectable={proposal.status === 'QS_SENT' || proposal.status === 'TERMS_RECEIVED'}
      generatable={proposal.status === 'TERMS_RECEIVED'}
      onChanged={() => void queryClient.invalidateQueries({ queryKey: ['proposal', proposal.id] })}
    />
  );
}
