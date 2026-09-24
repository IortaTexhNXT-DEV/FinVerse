import { mayOpen } from './access';

describe('screen access', () => {
  const can = (p: string) => p === 'ACCESS_APPROVE';

  it('opens screens without permission, with the permission or with an alternative', () => {
    expect(mayOpen({}, can)).toBe(true);
    expect(mayOpen({ permission: 'ACCESS_APPROVE' }, can)).toBe(true);
    expect(mayOpen({ permission: 'ACCESS_REQUEST' }, can)).toBe(false);
    expect(
      mayOpen({ permission: 'ACCESS_REQUEST', alsoPermissions: ['ACCESS_APPROVE'] }, can),
    ).toBe(true);
  });
});
