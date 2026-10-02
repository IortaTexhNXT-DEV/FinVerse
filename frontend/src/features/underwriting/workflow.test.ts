import { allowedActions, canIterate, endorsementLabel, sourceForIntermediary } from './workflow';

const everything = () => true;

describe('underwriting workflow', () => {
  it('lets the maker edit and submit a draft', () => {
    const a = allowedActions({ status: 'DRAFT', createdBy: 'uw' }, 'uw', everything);
    expect(a).toMatchObject({ edit: true, submit: true, discard: true, approve: false });
  });

  it('lets only a different user approve', () => {
    const doc = { status: 'PENDING_APPROVAL', createdBy: 'uw', submittedBy: 'uw' };
    expect(allowedActions(doc, 'uw', everything).approve).toBe(false);
    expect(allowedActions(doc, 'fmanager', everything).approve).toBe(true);
    expect(allowedActions(doc, 'fmanager', (p) => p !== 'POLICY_AUTHORIZE').reject).toBe(false);
  });

  it('labels endorsements and iteration rules', () => {
    expect(endorsementLabel(0)).toBe('Policy');
    expect(endorsementLabel(3)).toBe('E03');
    expect(canIterate('APPROVED')).toBe(true);
    expect(canIterate('CONVERTED')).toBe(false);
    expect(sourceForIntermediary('')).toBe('DIRECT');
    expect(sourceForIntermediary('A-0001')).toBe('AGENT');
    expect(sourceForIntermediary('B-0002')).toBe('BROKER');
  });
});
