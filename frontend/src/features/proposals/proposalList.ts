import type { ProposalSearch, ProposalStatus } from '@/api/proposals';

/** Status tabs of the PRF work list. */
export const PROPOSAL_TABS = [
  { id: 'drafts', label: 'Drafts' },
  { id: 'approval', label: 'For Approval' },
  { id: 'tsu', label: 'With TSU' },
  { id: 'client', label: 'With Client' },
  { id: 'accepted', label: 'Accepted' },
  { id: 'closed', label: 'Not Proceeded' },
] as const;

export type ProposalTab = (typeof PROPOSAL_TABS)[number]['id'];

export const PROPOSAL_TAB_STATUSES: Record<ProposalTab, ProposalStatus[]> = {
  drafts: ['DRAFT'],
  approval: ['FOR_MKT_APPROVAL'],
  tsu: [
    'WITH_TSU',
    'QS_PREPARATION',
    'QS_FOR_APPROVAL',
    'QS_SENT',
    'TERMS_RECEIVED',
    'PS_FOR_APPROVAL',
  ],
  client: ['PS_RELEASED', 'SENT_TO_CLIENT'],
  accepted: ['ACCEPTED', 'CONVERTED'],
  closed: ['NOT_PROCEEDED', 'VOIDED'],
};

/** The list criteria of a tab and the search box. */
export function proposalCriteria(tab: ProposalTab, text: string, mine: boolean): ProposalSearch {
  return {
    text: text.trim() === '' ? undefined : text.trim(),
    status: PROPOSAL_TAB_STATUSES[tab],
    mine: mine ? true : undefined,
  };
}

/** Stages in which the TSU keys in and compares insurer terms (BRNB.009). */
export const RESPONSE_STAGES = new Set<ProposalStatus>(['QS_SENT', 'TERMS_RECEIVED']);

/** Stages in which TSU still chooses the insurers of the quotation slip. */
export const SLIP_EDIT_STAGES = new Set<ProposalStatus>(['WITH_TSU', 'QS_PREPARATION']);

/** Stages after the quotation slip was sent (responses and comparison exist). */
export function slipSent(status: ProposalStatus): boolean {
  return ![
    'DRAFT',
    'FOR_MKT_APPROVAL',
    'WITH_TSU',
    'QS_PREPARATION',
    'QS_FOR_APPROVAL',
    'VOIDED',
  ].includes(status);
}
