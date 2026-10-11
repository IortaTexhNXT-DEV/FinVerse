import { useMutation, useQuery } from '@tanstack/react-query';
import { authApi } from '@/api/auth';
import type { PasswordChangeReason } from '@/api/auth';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { PasswordChangeForm } from '@/features/profile/PasswordChangeForm';
import { changeReasonText, policyHint } from '@/features/profile/passwordRules';
import { useAuth } from './authContext';
import { SignInFrame } from './SignInFrame';

/**
 * Shown instead of the home page while the password must be changed (UAM-NFR-36; FR-UA-005): after
 * an administrator reset or on first use, or once the password is older than the maximum age.
 */
export function ForcedPasswordChange({ reason }: Readonly<{ reason: PasswordChangeReason }>) {
  const { logout, passwordChanged } = useAuth();
  const toast = useToast();
  const status = useQuery({ queryKey: ['password-status'], queryFn: authApi.passwordStatus });
  const change = useMutation({
    mutationFn: ({ current, next }: { current: string; next: string }) =>
      authApi.changePassword(current, next),
    onSuccess: () => {
      toast.success('Password changed');
      passwordChanged();
    },
  });
  return (
    <SignInFrame
      title="Change Your Password"
      subtitle={changeReasonText(reason, status.data?.maxAgeDays)}
    >
      <PasswordChangeForm
        requireCurrent
        submitLabel="Change Password"
        busy={change.isPending}
        error={change.error}
        hint={status.data === undefined ? undefined : policyHint(status.data) || undefined}
        onSubmit={(current, next) => change.mutateAsync({ current, next })}
      />
      <Button variant="secondary" onClick={() => logout()}>
        Sign Out
      </Button>
    </SignInFrame>
  );
}
