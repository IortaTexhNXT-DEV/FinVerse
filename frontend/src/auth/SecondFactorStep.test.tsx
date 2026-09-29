import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mfaApi } from '@/api/mfa';
import type { LoginResponse } from '@/api/types';
import { SecondFactorStep } from './SecondFactorStep';

const SIGNED_IN: LoginResponse = {
  accessToken: 'token',
  expiresAt: '2026-09-28T18:00:00Z',
  accessTokenExpiresAt: '2026-09-28T10:15:00Z',
};

describe('SecondFactorStep', () => {
  afterEach(() => vi.restoreAllMocks());

  it('sends the code of the app with the challenge and opens the session', async () => {
    const verify = vi.spyOn(mfaApi, 'verify').mockResolvedValue(SIGNED_IN);
    const signedIn = vi.fn();
    render(
      <SecondFactorStep
        pending={{
          expiresAt: '',
          mfaStep: 'VERIFY',
          mfaChallenge: 'challenge-1',
          rememberDeviceDays: 7,
        }}
        onSignedIn={signedIn}
        onBack={vi.fn()}
      />,
    );
    await userEvent.type(screen.getByLabelText(/^Code/), '123456');
    await userEvent.click(screen.getByLabelText(/Do not ask again on this device for 7 days/));
    await userEvent.click(screen.getByRole('button', { name: 'Verify' }));
    await waitFor(() => expect(signedIn).toHaveBeenCalledWith(SIGNED_IN));
    expect(verify).toHaveBeenCalledWith('challenge-1', '123456', true);
  });

  it('accepts a recovery code instead and hides remember-device when it is off', async () => {
    const verify = vi.spyOn(mfaApi, 'verify').mockResolvedValue(SIGNED_IN);
    render(
      <SecondFactorStep
        pending={{ expiresAt: '', mfaStep: 'VERIFY', mfaChallenge: 'c', rememberDeviceDays: 0 }}
        onSignedIn={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    expect(screen.queryByText(/Do not ask again/)).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Use a recovery code' }));
    await userEvent.type(screen.getByLabelText(/^Recovery code/), 'abcde-fghij');
    await userEvent.click(screen.getByRole('button', { name: 'Verify' }));
    await waitFor(() => expect(verify).toHaveBeenCalledWith('c', 'abcde-fghij', false));
  });

  it('enrols the app at the first sign-in and shows the recovery codes once', async () => {
    vi.spyOn(mfaApi, 'startAtSignIn').mockResolvedValue({
      secret: 'ABCD EFGH',
      otpauthUri: 'otpauth://totp/x',
      qrCode: 'data:image/svg+xml;base64,PHN2Zy8+',
      issuer: 'x',
    });
    vi.spyOn(mfaApi, 'confirmAtSignIn').mockResolvedValue({
      recoveryCodes: ['aaaaa-bbbbb', 'ccccc-ddddd'],
      signIn: SIGNED_IN,
    });
    const signedIn = vi.fn();
    render(
      <SecondFactorStep
        pending={{ expiresAt: '', mfaStep: 'ENROL', mfaChallenge: 'c' }}
        onSignedIn={signedIn}
        onBack={vi.fn()}
      />,
    );
    expect(await screen.findByAltText(/QR code/)).toBeInTheDocument();
    expect(screen.getByText('ABCD EFGH')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/^Code from the app/), '654321');
    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    expect(await screen.findByText('aaaaa-bbbbb')).toBeInTheDocument();
    expect(signedIn).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }));
    expect(signedIn).toHaveBeenCalledWith(SIGNED_IN);
  });
});
