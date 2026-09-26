import type { Proposal } from '@/api/proposals';
import {
  formOfProposal,
  newProposalForm,
  proposalErrors,
  SECTION_HEADINGS,
  toggleInsurer,
  toProposalInput,
} from './proposalForm';
import {
  PROPOSAL_TAB_STATUSES,
  PROPOSAL_TABS,
  proposalCriteria,
  RESPONSE_STAGES,
  SLIP_EDIT_STAGES,
  slipSent,
} from './proposalList';

describe('proposal form', () => {
  it('starts with the suggested risk sections', () => {
    const f = newProposalForm(8);
    expect(f.clientId).toBe(8);
    expect(f.sections.map((s) => s.heading)).toEqual(SECTION_HEADINGS);
  });

  it('leaves out empty sections and blank fields', () => {
    const f = {
      ...newProposalForm(8),
      productCode: 'FIRE-IAR',
      periodFrom: ' ',
      items: [{ sumInsured: 10 }, { sumInsured: 20 }],
      groups: [2],
      insurers: ['INS-1'],
    };
    f.sections[0] = { heading: 'Description of the risk', text: 'Warehouse' };
    const body = toProposalInput(f, 1);
    expect(body.sections).toHaveLength(1);
    expect(body.periodFrom).toBeUndefined();
    expect(body.sourceChannel).toBe('EMAIL');
    expect(body.items.map((i) => i.riskGroup)).toEqual([2, 1]);
  });

  it('reads a saved PRF back into the form', () => {
    const p = {
      id: 4,
      prfNo: 'PRF-2026-000001',
      clientId: 8,
      productCode: 'FIRE-IAR',
      sections: [{ heading: 'Loss history' }],
      items: [{ itemNo: 1, riskGroup: 3, data: { sumInsured: 5 } }],
      insurers: ['INS-1'],
    } as unknown as Proposal;
    const f = formOfProposal(p);
    expect(f.sections).toEqual([{ heading: 'Loss history', text: '' }]);
    expect(f.groups).toEqual([3]);
    expect(f.marketSegment).toBe('');
    expect(f.insurers).not.toBe(p.insurers);
  });

  it('needs the client and product and an ordered period', () => {
    expect(Object.keys(proposalErrors(newProposalForm()))).toEqual(['clientId', 'productCode']);
    const f = {
      ...newProposalForm(1),
      productCode: 'X',
      periodFrom: '2026-02-01',
      periodTo: '2026-01-01',
    };
    expect(proposalErrors(f)).toHaveProperty('periodTo');
    expect(proposalErrors({ ...f, periodTo: '2027-02-01' })).toEqual({});
  });

  it('toggles requested insurers', () => {
    expect(toggleInsurer(['A'], 'B')).toEqual(['A', 'B']);
    expect(toggleInsurer(['A', 'B'], 'A')).toEqual(['B']);
  });
});

describe('proposal work list', () => {
  it('covers every tab and filters by stage', () => {
    PROPOSAL_TABS.forEach((t) => expect(PROPOSAL_TAB_STATUSES[t.id].length).toBeGreaterThan(0));
    expect(proposalCriteria('tsu', ' PRF ', true)).toEqual({
      text: 'PRF',
      status: PROPOSAL_TAB_STATUSES.tsu,
      mine: true,
    });
    expect(proposalCriteria('drafts', '', false).mine).toBeUndefined();
  });

  it('knows when the slips and responses can be worked', () => {
    expect(slipSent('QS_PREPARATION')).toBe(false);
    expect(slipSent('QS_SENT')).toBe(true);
    expect(slipSent('ACCEPTED')).toBe(true);
    expect(RESPONSE_STAGES.has('TERMS_RECEIVED')).toBe(true);
    expect(SLIP_EDIT_STAGES.has('QS_SENT')).toBe(false);
  });
});
