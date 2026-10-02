import { awaitsOtherChecker, makerOf } from './makerChecker';

describe('maker-checker', () => {
  const pending = { recordStatus: 'PENDING_AUTHORIZATION', createdBy: 'fmanager' };

  it('does not offer Authorize to the user who maintained the record', () => {
    expect(awaitsOtherChecker(pending, 'fmanager')).toBe(false);
    expect(awaitsOtherChecker(pending, 'checker')).toBe(true);
  });

  it('takes the maker from the last maintainer, then the creator', () => {
    const edited = { ...pending, updatedBy: 'accountant' };
    expect(makerOf(edited)).toBe('accountant');
    expect(awaitsOtherChecker(edited, 'fmanager')).toBe(true);
    expect(awaitsOtherChecker(edited, 'accountant')).toBe(false);
    expect(makerOf({ ...edited, maker: 'uw' })).toBe('uw');
  });

  it('offers nothing for records that are not pending or while no user is known', () => {
    expect(awaitsOtherChecker({ ...pending, recordStatus: 'ACTIVE' }, 'checker')).toBe(false);
    expect(awaitsOtherChecker(pending, undefined)).toBe(false);
    expect(awaitsOtherChecker(undefined, 'checker')).toBe(false);
  });
});
