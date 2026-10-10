import { authApi } from '@/api/auth';

/**
 * Ends the session at the identity provider too (BDOI FRS FRUM.001.05: with single sign-on, Log Out
 * also ends the BDO session, so the next access asks for the EIAM sign-in again). Opens the
 * provider's sign-out page, which returns to the Login page; without single sign-on nothing
 * happens.
 *
 * @param open opens an address (the browser's location by default)
 * @returns whether the provider's page was opened
 */
export async function signOutAtProvider(
  open: (url: string) => void = (url) => window.location.assign(url),
): Promise<boolean> {
  try {
    const answer = await authApi.ssoSignOut();
    if (answer.redirectUrl) {
      open(answer.redirectUrl);
      return true;
    }
  } catch {
    // The provider's page is not known: the system session is ended all the same.
  }
  return false;
}
