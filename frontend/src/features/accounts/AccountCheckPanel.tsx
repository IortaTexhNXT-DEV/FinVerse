import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, CircleAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { accountsApi } from '@/api/accounts';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { humanize } from '@/utils/format';
import { checkLines } from './accountChecks';

/**
 * Readiness of an account (BRNB.013/016): missing data and documents, duplicates with the
 * existing ARN, premium rating and TSU routing.
 */
export function AccountCheckPanel({ accountId }: Readonly<{ accountId: number }>) {
  const check = useQuery({
    queryKey: ['account', accountId, 'check'],
    queryFn: () => accountsApi.check(accountId),
  });
  if (check.data === undefined) {
    return check.error ? (
      <ErrorAlert error={check.error} />
    ) : (
      <span className="spinner" aria-label="Checking" />
    );
  }
  const c = check.data;
  const lines = checkLines(c);
  return (
    <div className="stack">
      {c.readyToSubmit ? (
        <div className="alert success" role="status">
          <CheckCircle2 size={16} aria-hidden="true" /> Complete: the account can be submitted.
        </div>
      ) : (
        <div className="alert warning" role="alert">
          <CircleAlert size={16} aria-hidden="true" /> Not ready to submit:
          <ul>
            {lines.map((l) => (
              <li key={l}>{l}</li>
            ))}
          </ul>
        </div>
      )}
      {c.duplicates.length > 0 && (
        <div className="alert danger" role="alert">
          Duplicate risks:
          <ul>
            {c.duplicates.map((d) => (
              <li key={`${d.itemNo}-${d.field}`}>
                Item {d.itemNo} {humanize(d.field)} {d.value} is on{' '}
                <Link to={`/accounts/by-arn/${d.existingArn}`}>{d.existingArn}</Link> (
                {d.existingProduct})
              </li>
            ))}
          </ul>
        </div>
      )}
      {c.tsuRequired && (
        <p className="muted">
          TSU review: {c.tsuReason ?? c.tsuRule} {c.tsuCleared ? '— cleared' : '— pending'}
        </p>
      )}
    </div>
  );
}
