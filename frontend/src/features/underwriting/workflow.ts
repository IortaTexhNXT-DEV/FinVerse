import type { SourceType } from '@/api/underwriting';

/** Maker-checker rules shared by policies, endorsements and quotations (mirrors the server). */

export interface WorkflowFacts {
  status: string;
  createdBy?: string;
  submittedBy?: string;
}

export interface AllowedActions {
  edit: boolean;
  submit: boolean;
  discard: boolean;
  approve: boolean;
  reject: boolean;
}

export function allowedActions(
  doc: WorkflowFacts,
  username: string | undefined,
  can: (permission: string) => boolean,
): AllowedActions {
  const maintain = can('POLICY_MAINTAIN');
  const draft = doc.status === 'DRAFT';
  const checker =
    doc.status === 'PENDING_APPROVAL' &&
    can('POLICY_AUTHORIZE') &&
    username !== doc.createdBy &&
    username !== doc.submittedBy;
  return {
    edit: draft && maintain,
    submit: draft && maintain,
    discard: draft && maintain,
    approve: checker,
    reject: checker,
  };
}

/** Human label of an endorsement number (0 = original policy). */
export function endorsementLabel(no: number): string {
  return no === 0 ? 'Policy' : `E${String(no).padStart(2, '0')}`;
}

/** Whether a quotation can take a new negotiation iteration. */
export function canIterate(status: string): boolean {
  return status === 'DRAFT' || status === 'APPROVED' || status === 'REJECTED';
}

/** Channel implied by an intermediary code (A- agents, others brokers; blank = direct). */
export function sourceForIntermediary(code: string): SourceType {
  if (code.trim() === '') {
    return 'DIRECT';
  }
  return code.startsWith('A-') ? 'AGENT' : 'BROKER';
}
