import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import type { Advice, Epolicy } from '@/api/issuance';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';

/**
 * Policy tab of the account detail page: policy numbers and issue date, the e-policies received
 * with their review status and dispatch, and the Insurance Advices.
 */
export function PolicyPanel({ arn }: Readonly<{ arn: string }>) {
  const policy = useQuery({
    queryKey: ['issuance', 'policy', arn],
    queryFn: () => issuanceApi.policy(arn),
  });
  if (policy.data === undefined) {
    return policy.error ? (
      <ErrorAlert error={policy.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = policy.data;
  return (
    <Card title="Policy">
      <div className="stack">
        <dl className="detail-list">
          <dt>Policy number(s)</dt>
          <dd>{p.policyNumbers.length > 0 ? p.policyNumbers.join(', ') : 'Not issued yet'}</dd>
          <dt>Issue date</dt>
          <dd>{formatDate(p.issueDate) || '—'}</dd>
        </dl>
        <DataTable<Epolicy>
          caption="E-policies"
          rows={p.epolicies}
          rowKey={(e) => e.id}
          emptyMessage="No e-policy received."
          columns={[
            {
              key: 'file',
              header: 'E-policy',
              render: (e) => <Link to={`/issuance/epolicies/${e.id}`}>{e.fileName}</Link>,
            },
            { key: 'received', header: 'Received', render: (e) => formatDateTime(e.createdAt) },
            { key: 'status', header: 'Review', render: (e) => <StatusBadge status={e.status} /> },
            {
              key: 'sent',
              header: 'Sent to Client',
              render: (e) =>
                e.dispatchedAt
                  ? `${formatDateTime(e.dispatchedAt)} to ${e.dispatchedTo ?? ''}`
                  : '—',
            },
          ]}
        />
        <DataTable<Advice>
          caption="Insurance Advices"
          rows={p.advices}
          rowKey={(a) => a.id}
          emptyMessage="No Insurance Advice."
          columns={[
            {
              key: 'no',
              header: 'Insurance Advice',
              render: (a) => <Link to={`/issuance/insurance-advice?ia=${a.iaNo}`}>{a.iaNo}</Link>,
            },
            { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
            { key: 'when', header: 'Generated', render: (a) => formatDateTime(a.createdAt) },
          ]}
        />
      </div>
    </Card>
  );
}
