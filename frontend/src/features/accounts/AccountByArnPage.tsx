import { useQuery } from '@tanstack/react-query';
import { Navigate, useParams } from 'react-router-dom';
import { accountsApi } from '@/api/accounts';
import { ErrorAlert } from '@/components/ui/ErrorAlert';

/** Opens an account from its ARN (links in duplicate findings, e-mails and other modules). */
export default function AccountByArnPage() {
  const arn = useParams().arn ?? '';
  const account = useQuery({
    queryKey: ['account', 'arn', arn],
    queryFn: () => accountsApi.byArn(arn),
  });
  if (account.data) {
    return <Navigate to={`/accounts/${account.data.id}`} replace />;
  }
  return account.error ? (
    <ErrorAlert error={account.error} />
  ) : (
    <span className="spinner" aria-label="Loading" />
  );
}
