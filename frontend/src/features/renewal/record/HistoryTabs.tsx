import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { History } from 'lucide-react';
import { renewalApi } from '@/api/renewal';
import type { CandidateDetail } from '@/api/renewal';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { dispositionSourceLabel, overrideChange, overrideKindLabel } from '../common/presentation';
import { RNW_LOV, dispositionLabel } from '../common/renewalCodes';
import { LovLabel } from '@/components/broking/LovLabel';

type Props = Readonly<{ detail: CandidateDetail }>;

/** Account History (FR-RN-042): opening it is recorded; the disposition needs it. */
export function AccountHistoryTab({ detail }: Props) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const ref = detail.row.renewalRef;
  const history = useMutation({
    mutationFn: () => renewalApi.accountHistory(companyId, ref),
    onSuccess: () =>
      void queryClient.invalidateQueries({ queryKey: ['renewal', 'candidate', companyId, ref] }),
  });
  const h = history.data;
  if (h === undefined) {
    return (
      <Card title="Account History">
        <ErrorAlert error={history.error} />
        <p className="muted">
          Prior renewals, endorsements, payments and claims of the account. Viewing it is recorded
          and is required before the disposition.
        </p>
        <Button
          icon={<History size={16} />}
          busy={history.isPending}
          onClick={() => history.mutate()}
        >
          View Account History
        </Button>
      </Card>
    );
  }
  return (
    <div className="stack">
      <Card title="Prior renewals" flush>
        <DataTable
          rows={h.priorRenewals}
          rowKey={(r) => r.renewalRef}
          emptyMessage="No prior renewals"
          columns={[
            { key: 'ref', header: 'Renewal', kind: 'code', render: (r) => r.renewalRef },
            { key: 'exp', header: 'Expiry', kind: 'date', render: (r) => formatDate(r.expiry) },
            {
              key: 'arn',
              header: 'Expiring / Renewal ARN',
              render: (r) => <CellStack main={r.expiringArn ?? ''} sub={r.renewalArn ?? ''} />,
            },
            {
              key: 'out',
              header: 'Outcome',
              render: (r) => (r.outcome ? humanize(r.outcome) : ''),
            },
          ]}
        />
      </Card>
      <Card title="Endorsements" flush>
        <DataTable
          rows={h.endorsements}
          rowKey={(e) => e.invoiceNo}
          emptyMessage="No endorsements"
          columns={[
            { key: 'inv', header: 'Invoice', kind: 'code', render: (e) => e.invoiceNo },
            { key: 'kind', header: 'Kind', render: (e) => (e.kind ? humanize(e.kind) : '') },
            { key: 'no', header: 'Endorsement', render: (e) => e.endorsementNo ?? '' },
            { key: 'on', header: 'Booked', kind: 'date', render: (e) => formatDate(e.bookedOn) },
            {
              key: 'gp',
              header: 'Gross Premium',
              kind: 'amount',
              render: (e) => <Amount value={e.grossPremium} />,
            },
          ]}
        />
      </Card>
      <Card title="Payments" flush>
        <DataTable
          rows={h.payments}
          rowKey={(p) =>
            `${p.invoiceNo}-${p.valueDate ?? ''}-${p.orNo ?? ''}-${p.type}-${String(p.amount)}`
          }
          emptyMessage="No payments"
          columns={[
            { key: 'inv', header: 'Invoice', kind: 'code', render: (p) => p.invoiceNo },
            {
              key: 'date',
              header: 'Value date',
              kind: 'date',
              render: (p) => formatDate(p.valueDate),
            },
            { key: 'type', header: 'Type', render: (p) => humanize(p.type) },
            {
              key: 'comp',
              header: 'Component',
              render: (p) => (p.component ? humanize(p.component) : ''),
            },
            { key: 'or', header: 'OR No.', render: (p) => p.orNo ?? '' },
            {
              key: 'amt',
              header: 'Amount',
              kind: 'amount',
              render: (p) => <Amount value={p.amount} />,
            },
          ]}
        />
      </Card>
      <Card title="Claims">
        <p>
          {h.claims.connected
            ? `${String(h.claims.count)} claim(s), ${String(h.claims.open)} open`
            : 'Claims are not connected yet; the claims shown on the renewal come from the extraction.'}
        </p>
      </Card>
    </div>
  );
}

/** Remarks and follow-ups (FR-RN-044, 085). */
export function NotesTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const remarks = useQuery({
    queryKey: ['renewal', 'remarks', companyId, ref],
    queryFn: () => renewalApi.remarks(companyId, ref),
  });
  const followups = useQuery({
    queryKey: ['renewal', 'followups', companyId, ref],
    queryFn: () => renewalApi.followups(companyId, ref),
  });
  return (
    <div className="stack">
      <ErrorAlert error={remarks.error ?? followups.error} />
      <Card title="Remarks" flush>
        <DataTable
          rows={remarks.data ?? []}
          rowKey={(r) => `${r.at}-${r.by}`}
          emptyMessage="No remarks"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (r) => formatDateTime(r.at) },
            { key: 'by', header: 'By', render: (r) => <UserName login={r.by} /> },
            {
              key: 'stage',
              header: 'Stage',
              kind: 'status',
              render: (r) => <StatusBadge status={r.stage} />,
            },
            { key: 'text', header: 'Remark', render: (r) => r.text },
          ]}
        />
      </Card>
      <Card title="Follow-ups" flush>
        <DataTable
          rows={followups.data ?? []}
          rowKey={(f) => `${f.at}-${f.by}`}
          emptyMessage="No follow-ups"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (f) => formatDateTime(f.at) },
            {
              key: 'ch',
              header: 'Channel',
              render: (f) => <LovLabel type={RNW_LOV.followupChannel} code={f.channel} />,
            },
            {
              key: 'out',
              header: 'Outcome',
              render: (f) => <LovLabel type={RNW_LOV.followupOutcome} code={f.outcome} />,
            },
            { key: 'rem', header: 'Remarks', render: (f) => f.remarks },
            {
              key: 'next',
              header: 'Next action',
              kind: 'date',
              render: (f) => formatDate(f.nextActionDate),
            },
            { key: 'by', header: 'By', render: (f) => <UserName login={f.by} /> },
          ]}
        />
      </Card>
    </div>
  );
}

/** History (FR-RN-004): dispositions, assignments and overrides. */
export function HistoryTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const h = useQuery({
    queryKey: ['renewal', 'history', companyId, ref],
    queryFn: () => renewalApi.history(companyId, ref),
  });
  return (
    <div className="stack">
      <ErrorAlert error={h.error} />
      <Card title="Dispositions" flush>
        <DataTable
          rows={h.data?.dispositions ?? []}
          rowKey={(d) => `${d.at}-${d.code}`}
          emptyMessage="No dispositions"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (d) => formatDateTime(d.at) },
            { key: 'code', header: 'Disposition', render: (d) => dispositionLabel(d.code) },
            {
              key: 'reason',
              header: 'Reason',
              render: (d) =>
                d.reason ? <LovLabel type={RNW_LOV.nonRenewalReason} code={d.reason} /> : '',
            },
            { key: 'src', header: 'Source', render: (d) => dispositionSourceLabel(d.source) },
            { key: 'rem', header: 'Remarks', render: (d) => d.remarks ?? '' },
            { key: 'cur', header: 'Current', render: (d) => (d.superseded ? '' : 'Yes') },
            { key: 'by', header: 'By', render: (d) => <UserName login={d.by} /> },
          ]}
        />
      </Card>
      <Card title="Assignments" flush>
        <DataTable
          rows={h.data?.assignments ?? []}
          rowKey={(a) => `${a.at}-${a.role}-${a.username}`}
          emptyMessage="No assignments"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (a) => formatDateTime(a.at) },
            {
              key: 'role',
              header: 'Role',
              render: (a) => (a.role === 'PO' ? 'Processing Officer' : 'Account Officer'),
            },
            { key: 'user', header: 'Assigned to', render: (a) => <UserName login={a.username} /> },
            {
              key: 'prev',
              header: 'Previous',
              render: (a) => (a.previous ? <UserName login={a.previous} /> : ''),
            },
            { key: 'by', header: 'By', render: (a) => <UserName login={a.by} /> },
          ]}
        />
      </Card>
      <Card title="Overrides" flush>
        <DataTable
          rows={h.data?.overrides ?? []}
          rowKey={(o) => `${o.at}-${o.kind}`}
          emptyMessage="No overrides"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (o) => formatDateTime(o.at) },
            { key: 'kind', header: 'Override', render: (o) => overrideKindLabel(o.kind) },
            { key: 'chg', header: 'Change', render: (o) => overrideChange(o.from, o.to) },
            {
              key: 'reason',
              header: 'Reason',
              render: (o) => <LovLabel type={RNW_LOV.overrideReason} code={o.reason} />,
            },
            { key: 'rem', header: 'Remarks', render: (o) => o.remarks },
            { key: 'by', header: 'By', render: (o) => <UserName login={o.by} /> },
          ]}
        />
      </Card>
    </div>
  );
}
