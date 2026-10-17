import { useLovLabel } from '@/components/broking/useLabels';
import { endorsementTypeLabel } from './requestForm';

/** Labels of a request's codes: endorsement type, request type, reason and return reason. */
export function useRequestLabels() {
  const type = useLovLabel('ENDORSEMENT_TYPE');
  const requestType = useLovLabel('ENDORSEMENT_REQUEST_TYPE');
  const reason = useLovLabel('CANCELLATION_REASON');
  const returnReason = useLovLabel('ADJ_RETURN_REASON');
  return {
    type: (code: string | undefined) => (code ? endorsementTypeLabel(code, type(code)) : ''),
    requestType: (code: string | undefined) => (code ? requestType(code) : ''),
    reason: (code: string | undefined) => (code ? reason(code) : ''),
    returnReason: (code: string | undefined) => (code ? returnReason(code) : ''),
  };
}
