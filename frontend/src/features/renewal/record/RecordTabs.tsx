import { useQuery } from '@tanstack/react-query';
import { renewalApi } from '@/api/renewal';
import type { CandidateDetail, CheckResultView, LetterView, Terms } from '@/api/renewal';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { RowActions } from '@/components/ui/RowActions';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import {
  acceptanceMethodLabel,
  bucketCauseLabel,
  letterTypeLabel,
  rateText,
  responseLabel,
} from '../common/presentation';
import { BucketPill } from '../common/RenewalBits';
import { severityLabel } from '../setup/setupCodes';

type Props = Readonly<{ detail: CandidateDetail }>;

const yes = (v: boolean) => (v ? 'Yes' : 'No');

/** Checks and Classification (FR-RN-020, 022). */
export function ChecksTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const checks = useQuery({
    queryKey: ['renewal', 'checks', companyId, ref],
    queryFn: () => renewalApi.checks(companyId, ref),
  });
  const d = checks.data;
  return (
    <div className="stack">
      <ErrorAlert error={checks.error} />
      <Card title="Check results" flush>
        <DataTable<CheckResultView>
          loading={checks.isLoading}
          rows={d?.results ?? []}
          rowKey={(r) => r.checkCode}
          emptyMessage="The checks have not run yet"
          columns={[
            { key: 'name', header: 'Check', render: (r) => r.checkName },
            {
              key: 'outcome',
              header: 'Result',
              kind: 'status',
              render: (r) => <StatusBadge status={r.outcome} />,
            },
            { key: 'sev', header: 'Severity', render: (r) => severityLabel(r.severity) },
            { key: 'msg', header: 'Message', render: (r) => r.message },
          ]}
        />
      </Card>
      <Card title="Classification history" flush>
        <DataTable
          rows={d?.buckets ?? []}
          rowKey={(b) => `${b.at}-${b.to}`}
          emptyMessage="No classification yet"
          columns={[
            { key: 'at', header: 'When', kind: 'datetime', render: (b) => formatDateTime(b.at) },
            { key: 'from', header: 'From', render: (b) => <BucketPill bucket={b.from} /> },
            { key: 'to', header: 'To', render: (b) => <BucketPill bucket={b.to} /> },
            { key: 'cause', header: 'Cause', render: (b) => bucketCauseLabel(b.cause) },
            { key: 'remarks', header: 'Remarks', render: (b) => b.remarks ?? '' },
            { key: 'by', header: 'By', render: (b) => <UserName login={b.by} /> },
          ]}
        />
      </Card>
      <Card title="Endorsements linked" flush>
        <DataTable
          rows={d?.endorsements ?? []}
          rowKey={(e) => e.reference}
          emptyMessage="No endorsements in the term"
          columns={[
            { key: 'ref', header: 'Reference', kind: 'code', render: (e) => e.reference },
            { key: 'src', header: 'Source', render: (e) => (e.source ? humanize(e.source) : '') },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (e) => (e.status ? <StatusBadge status={e.status} /> : ''),
            },
            {
              key: 'at',
              header: 'Linked',
              kind: 'datetime',
              render: (e) => formatDateTime(e.linkedAt),
            },
          ]}
        />
      </Card>
    </div>
  );
}

function TermsGrid({ title, t }: Readonly<{ title: string; t: Terms }>) {
  return (
    <Card title={title}>
      <DefinitionGrid
        items={[
          { label: 'Currency', value: t.currency ?? '' },
          { label: 'Net Premium', value: <Amount value={t.netPremium} /> },
          { label: 'Charges', value: <Amount value={t.charges} /> },
          { label: 'Gross Premium', value: <Amount value={t.grossPremium} /> },
          { label: 'Sum Insured', value: <Amount value={t.sumInsured} /> },
          { label: 'Commission Rate', value: rateText(t.commissionRate) },
          { label: 'Commission', value: <Amount value={t.commission} /> },
        ]}
      />
    </Card>
  );
}

/** Computations (FR-RN-064): the expiring and renewal terms side by side. */
export function ComputationsTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const c = useQuery({
    queryKey: ['renewal', 'computations', companyId, ref],
    queryFn: () => renewalApi.computations(companyId, ref),
  });
  return (
    <div className="stack">
      <ErrorAlert error={c.error} />
      {c.data && (
        <div className="rnw-grid">
          <TermsGrid title="Expiring" t={c.data.expiring} />
          <TermsGrid
            title={
              c.data.renewalArn
                ? `Renewal (${c.data.renewalArn})`
                : 'Renewal (no renewal account yet)'
            }
            t={c.data.renewal}
          />
        </div>
      )}
    </div>
  );
}

function usedLabel(latestValid: boolean, late: boolean): string {
  if (latestValid) return 'Latest valid';
  return late ? 'Late' : '';
}

/** Insurer responses (FR-RN-071): append-only, the latest valid one drives the renewal. */
export function InsurerTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const rows = useQuery({
    queryKey: ['renewal', 'responses', companyId, ref],
    queryFn: () => renewalApi.responses(companyId, ref),
  });
  return (
    <Card title="Insurer responses" flush>
      <ErrorAlert error={rows.error} />
      <DataTable
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => `${r.at}-${r.response}`}
        emptyMessage="No response from the insurer"
        columns={[
          { key: 'on', header: 'Received', kind: 'date', render: (r) => formatDate(r.receivedOn) },
          { key: 'resp', header: 'Response', render: (r) => responseLabel(r.response) },
          { key: 'ref', header: 'Insurer Ref', render: (r) => r.insurerRef ?? '' },
          {
            key: 'gp',
            header: 'Revised Premium',
            kind: 'amount',
            render: (r) => <Amount value={r.revisedPremium} />,
          },
          {
            key: 'si',
            header: 'Revised Sum Insured',
            kind: 'amount',
            render: (r) => <Amount value={r.revisedSumInsured} />,
          },
          {
            key: 'match',
            header: 'Match',
            kind: 'status',
            render: (r) => <StatusBadge status={r.match} />,
          },
          {
            key: 'valid',
            header: 'Used',
            render: (r) => usedLabel(r.latestValid, r.late),
          },
          {
            key: 'src',
            header: 'Source',
            render: (r) => (r.jobNo ? `Upload ${r.jobNo}` : 'Manual'),
          },
          { key: 'by', header: 'By', render: (r) => <UserName login={r.by} /> },
        ]}
      />
    </Card>
  );
}

/** Letters and acceptances (FR-RN-080-084). */
export function LettersTab({ detail }: Props) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const download = useFileDownload();
  const letters = useQuery({
    queryKey: ['renewal', 'letters', companyId, ref],
    queryFn: () => renewalApi.letters(companyId, ref),
  });
  const acceptances = useQuery({
    queryKey: ['renewal', 'acceptances', companyId, ref],
    queryFn: () => renewalApi.acceptances(companyId, ref),
  });
  return (
    <div className="stack">
      <ErrorAlert error={letters.error ?? acceptances.error ?? download.error} />
      <Card title="Letters" flush>
        <DataTable<LetterView>
          loading={letters.isLoading}
          rows={letters.data ?? []}
          rowKey={(l) => l.letterNo}
          emptyMessage="No letters"
          columns={[
            { key: 'no', header: 'Letter', kind: 'code', render: (l) => l.letterNo },
            {
              key: 'type',
              header: 'Type',
              render: (l) => letterTypeLabel(l.type, l.notice),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (l) => <StatusBadge status={l.status} />,
            },
            {
              key: 'gen',
              header: 'Generated',
              kind: 'datetime',
              render: (l) => formatDateTime(l.generatedAt),
            },
            {
              key: 'sent',
              header: 'Sent',
              kind: 'datetime',
              render: (l) => formatDateTime(l.sentAt),
            },
            { key: 'to', header: 'Recipients', render: (l) => l.recipients ?? l.failure ?? '' },
            {
              key: 'file',
              header: '',
              render: (l) => (
                <RowActions
                  record={l.letterNo}
                  actions={[
                    {
                      label: 'Download PDF',
                      hidden: l.attachmentId === null,
                      onSelect: () =>
                        download.mutate(() => renewalApi.letterFile(companyId, l.letterNo)),
                    },
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      <Card title="Acceptance" flush>
        <DataTable
          rows={acceptances.data ?? []}
          rowKey={(a) => a.at}
          emptyMessage="No acceptance recorded"
          columns={[
            {
              key: 'on',
              header: 'Accepted on',
              kind: 'date',
              render: (a) => formatDate(a.acceptedOn),
            },
            { key: 'method', header: 'Method', render: (a) => acceptanceMethodLabel(a.method) },
            { key: 'ref', header: 'Reference', render: (a) => a.reference ?? '' },
            {
              key: 'ack',
              header: 'Financial impact acknowledged',
              render: (a) => yes(a.financialImpactAck),
            },
            { key: 'by', header: 'By', render: (a) => <UserName login={a.by} /> },
          ]}
        />
      </Card>
    </div>
  );
}
