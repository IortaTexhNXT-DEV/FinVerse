/** The message of an error code of the single sign-on, in words. */
export function singleSignOnMessage(code: string): string {
  switch (code) {
    case 'SSO_NOT_LINKED':
      return 'Your sign-in was accepted by your organisation, but it is not linked to an active user of this system. Contact your administrator.';
    case 'SSO_PROVIDER_ERROR':
      return 'Your organisation’s sign-in service could not complete the sign-in. Try again in a moment.';
    case 'SSO_NOT_CONFIGURED':
      return 'Single sign-on is not available. Contact your administrator.';
    default:
      return 'The sign-in could not be completed. Start again from the sign-in page.';
  }
}
