import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { accountsApi } from '@/api/accounts';
import { useLovLabel } from '@/components/broking/useLabels';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { humanize } from '@/utils/format';
import { checkLines } from './accountChecks';
import { Notice } from '@/components/ui/Notice';
import { DataTable } from '@/components/ui/DataTable';

/**
 * Readiness of an account (BRNB.013/016): missing data and documents, duplicates with the
 * existing ARN, premium rating and TSU routing.
 */
export function AccountCheckPanel({ accountId }: Readonly<{ accountId: number }>) {
  const documentLabel = useLovLabel('DOCUMENT_TYPE');
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
  const lines = checkLines(c, documentLabel);
  return (
    <div className="stack">
      {c.readyToSubmit ? (
        <Notice tone="success" title="Complete">
          The account can be submitted.
        </Notice>
      ) : (
        <Notice tone="warning" role="alert" title="Not ready to submit" items={lines} />
      )}
      {c.duplicates.length > 0 && (
        <>
          <Notice tone="error" title="Duplicate risks">
            These risk items are already insured on other accounts.
          </Notice>
          <DataTable
            caption="Duplicate risks"
            rows={c.duplicates}
            rowKey={(d) => `${d.itemNo}-${d.field}`}
            columns={[
              { key: 'item', header: 'Item', kind: 'amount', render: (d) => d.itemNo },
              { key: 'field', header: 'Field', render: (d) => humanize(d.field) },
              { key: 'value', header: 'Value', render: (d) => d.value },
              {
                key: 'arn',
                header: 'Existing Account',
                kind: 'code',
                render: (d) => (
                  <Link to={`/accounts/by-arn/${d.existingArn}`}>{d.existingArn}</Link>
                ),
              },
              { key: 'product', header: 'Product', render: (d) => d.existingProduct },
            ]}
          />
        </>
      )}
      {c.tsuRequired && (
        <p className="muted">
          TSU review: {c.tsuReason ?? c.tsuRule} {c.tsuCleared ? '— cleared' : '— pending'}
        </p>
      )}
    </div>
  );
}
