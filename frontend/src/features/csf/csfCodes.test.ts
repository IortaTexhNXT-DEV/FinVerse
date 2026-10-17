import {
  contactError,
  contactFieldLabel,
  csfStatusTone,
  isEmail,
  isMobile,
  minutesLeft,
  searchKeyLabel,
  searchRefusal,
} from './csfCodes';

describe('CSF rules of the screens', () => {
  it('refuses a blank search and a short name', () => {
    expect(searchRefusal('NAME', '')).toBe('Enter the value to search');
    expect(searchRefusal('NAME', 'Sa')).toBe('Enter at least 3 characters');
    expect(searchRefusal('PN_NO', 'P1')).toBeUndefined();
    expect(searchKeyLabel('APPLICATION_NO')).toBe('Application No.');
  });

  it('colours the CSF statuses', () => {
    expect(csfStatusTone('OPEN')).toBe('success');
    expect(csfStatusTone('AWAITING')).toBe('info');
    expect(csfStatusTone('CLOSED')).toBe('neutral');
    expect(csfStatusTone('SOMETHING')).toBe('neutral');
    expect(csfStatusTone(null)).toBe('neutral');
  });

  it('checks the contact values like the client master', () => {
    expect(isEmail('a@b.ph')).toBe(true);
    expect(isEmail('a@b')).toBe(false);
    expect(isEmail('a b@c.ph')).toBe(false);
    expect(isMobile('09171234567')).toBe(true);
    expect(isMobile('+639171234567')).toBe(true);
    expect(isMobile('0917123')).toBe(false);
    expect(contactError('EMAIL', 'x')).toBe('Enter a valid e-mail address');
    expect(contactError('PHONE', '(02) 8888-1234')).toBeUndefined();
    expect(contactError('PHONE', 'call me')).toBe(
      'Enter the phone number with digits and separators',
    );
    expect(contactError('MOBILE', '')).toBeUndefined();
    expect(contactError('CITY', 'Makati')).toBeUndefined();
    expect(contactFieldLabel('ADDRESS_LINE')).toBe('Address Line');
    expect(contactFieldLabel('CIVIL_STATUS')).toBeUndefined();
  });

  it('counts the minutes left of a verification', () => {
    const now = new Date('2026-09-27T01:00:00Z');
    expect(minutesLeft('2026-09-27T01:29:30Z', now)).toBe(30);
    expect(minutesLeft('2026-09-27T00:59:00Z', now)).toBe(0);
  });
});
