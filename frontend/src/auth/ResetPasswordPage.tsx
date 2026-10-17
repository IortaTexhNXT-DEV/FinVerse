import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api/auth';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PasswordChangeForm } from '@/features/profile/PasswordChangeForm';
import { formatDateTime } from '@/utils/format';
import { BRAND } from '@/branding';
import { SignInFrame } from './SignInFrame';
import { Notice } from '@/components/ui/Notice';

/**
 * The page of a "Forgot password?" link (UAM-NFR-37; FR-UA-005): the link is checked first, then
 * the new password is set. A link works once and expires after 30 minutes.
 */
export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const link = useQuery({
    queryKey: ['password-reset-link', token],
    queryFn: () => authApi.checkReset(token),
    enabled: token !== '',
    retry: false,
  });
  const reset = useMutation({
    mutationFn: (next: string) => authApi.confirmReset(token, next),
  });

  let content;
  if (token === '') {
    content = <Notice tone="warning">This page needs the link from your e-mail.</Notice>;
  } else if (reset.isSuccess) {
    content = (
      <Notice tone="success">Your password was changed. Sign in with the new password.</Notice>
    );
  } else if (link.isError) {
    content = <ErrorAlert error={link.error} />;
  } else if (link.data === undefined) {
    content = <span className="spinner" aria-label="Checking the link" />;
  } else {
    content = (
      <>
        <p className="login-notice">
          Choose a new password. The link expires at {formatDateTime(link.data.expiresAt)}.
        </p>
        <PasswordChangeForm
          requireCurrent={false}
          submitLabel="Set Password"
          busy={reset.isPending}
          error={reset.error}
          onSubmit={(_, next) => reset.mutateAsync(next)}
        />
      </>
    );
  }

  return (
    <SignInFrame title="Set a New Password" subtitle={BRAND.productName}>
      {content}
      <Link to="/login" className="login-notice">
        Back to Login
      </Link>
    </SignInFrame>
  );
}
