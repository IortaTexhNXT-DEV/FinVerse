import { showsInsurerWidgets } from './insurerWidgets';

describe('insurer dashboard widgets', () => {
  it('are hidden for every BDOI role', () => {
    expect(showsInsurerWidgets(['MKT_TL'])).toBe(false);
    expect(showsInsurerWidgets(['PROCESSING_TL', 'NB_APPROVER'])).toBe(false);
    expect(showsInsurerWidgets(['FINANCE_MANAGER'])).toBe(false);
    expect(showsInsurerWidgets(undefined)).toBe(false);
  });

  it('are kept only for the insurer roles', () => {
    expect(showsInsurerWidgets(['UNDERWRITER'])).toBe(true);
  });
});
