import { afterEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/auth';
import { signOutAtProvider } from './providerSignOut';

describe('signOutAtProvider', () => {
  afterEach(() => vi.restoreAllMocks());

  it("opens the identity provider's sign-out page with single sign-on", async () => {
    vi.spyOn(authApi, 'ssoSignOut').mockResolvedValue({
      redirectUrl:
        'https://eiam.example/logout?post_logout_redirect_uri=https%3A%2F%2Fbibs%2Flogin',
    });
    const open = vi.fn();
    await expect(signOutAtProvider(open)).resolves.toBe(true);
    expect(open).toHaveBeenCalledWith(
      'https://eiam.example/logout?post_logout_redirect_uri=https%3A%2F%2Fbibs%2Flogin',
    );
  });

  it('does nothing without single sign-on or when the server cannot say', async () => {
    const open = vi.fn();
    vi.spyOn(authApi, 'ssoSignOut').mockResolvedValue({ redirectUrl: null });
    await expect(signOutAtProvider(open)).resolves.toBe(false);
    vi.spyOn(authApi, 'ssoSignOut').mockRejectedValue(new Error('offline'));
    await expect(signOutAtProvider(open)).resolves.toBe(false);
    expect(open).not.toHaveBeenCalled();
  });
});
