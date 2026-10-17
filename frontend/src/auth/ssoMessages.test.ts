import { describe, expect, it } from 'vitest';
import { singleSignOnMessage } from './ssoMessages';

describe('singleSignOnMessage', () => {
  it('explains each error code in words', () => {
    expect(singleSignOnMessage('SSO_NOT_LINKED')).toContain('not linked to an active user');
    expect(singleSignOnMessage('SSO_PROVIDER_ERROR')).toContain('could not complete');
    expect(singleSignOnMessage('SSO_NOT_CONFIGURED')).toContain('not available');
    expect(singleSignOnMessage('SSO_INVALID')).toContain('Start again');
    expect(singleSignOnMessage('')).toContain('Start again');
  });
});
