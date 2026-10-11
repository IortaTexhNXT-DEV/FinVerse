import type { ItemInput } from '@/api/accounts';
import type { Proposal, ProposalInput, RiskSection } from '@/api/proposals';

/** A PRF as entered on the form (text fields stay '' while blank). */
export interface ProposalForm {
  id?: number;
  prfNo?: string;
  clientId?: number;
  productCode: string;
  marketSegment: string;
  sourceChannel: string;
  periodFrom: string;
  periodTo: string;
  sections: RiskSection[];
  items: ItemInput[];
  groups: number[];
  insurers: string[];
}

/** Suggested headings of the free-form risk sections (BRNB.005 complete risk details). */
export const SECTION_HEADINGS = [
  'Description of the risk',
  'Occupancy / operations',
  'Loss history',
  'Requested cover and limits',
];

export function newProposalForm(clientId?: number): ProposalForm {
  return {
    clientId,
    productCode: '',
    marketSegment: '',
    sourceChannel: 'EMAIL',
    periodFrom: '',
    periodTo: '',
    sections: SECTION_HEADINGS.map((heading) => ({ heading, text: '' })),
    items: [],
    groups: [],
    insurers: [],
  };
}

/** The form of a saved PRF. */
export function formOfProposal(p: Proposal): ProposalForm {
  return {
    id: p.id,
    prfNo: p.prfNo,
    clientId: p.clientId,
    productCode: p.productCode,
    marketSegment: p.marketSegment ?? '',
    sourceChannel: p.sourceChannel ?? '',
    periodFrom: p.periodFrom ?? '',
    periodTo: p.periodTo ?? '',
    sections: p.sections.map((s) => ({ heading: s.heading ?? '', text: s.text ?? '' })),
    items: p.items.map((i) => i.data),
    groups: p.items.map((i) => i.riskGroup),
    insurers: [...p.insurers],
  };
}

const blank = (value: string) => (value.trim() === '' ? undefined : value.trim());

/** The request body of a form; empty sections are left out. */
export function toProposalInput(f: ProposalForm, companyId: number): ProposalInput {
  return {
    companyId,
    clientId: f.clientId,
    productCode: f.productCode,
    marketSegment: blank(f.marketSegment),
    sourceChannel: blank(f.sourceChannel),
    periodFrom: blank(f.periodFrom),
    periodTo: blank(f.periodTo),
    sections: f.sections.filter((s) => (s.text ?? '').trim() !== ''),
    items: f.items.map((risk, i) => ({ riskGroup: f.groups[i] ?? 1, risk })),
    insurers: f.insurers,
  };
}

/** Field errors of the PRF form. */
export function proposalErrors(f: ProposalForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (f.clientId === undefined) {
    errors.clientId = 'Select the client or prospect';
  }
  if (f.productCode === '') {
    errors.productCode = 'Select the product';
  }
  if (f.periodFrom !== '' && f.periodTo !== '' && f.periodTo <= f.periodFrom) {
    errors.periodTo = 'The period end must be after the period start';
  }
  return errors;
}

/** Adds or removes an insurer from the requested list. */
export function toggleInsurer(insurers: string[], code: string): string[] {
  return insurers.includes(code) ? insurers.filter((c) => c !== code) : [...insurers, code];
}
