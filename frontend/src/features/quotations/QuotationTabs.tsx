import { useQuery } from '@tanstack/react-query';
import { quotationsApi, QUOTATION_ENTITY } from '@/api/quotations';
import type {
  Quotation,
  QuotationContent,
  QuotationDiff,
  QuotationItemView,
} from '@/api/quotations';
import { workflowApi } from '@/api/workflow';
import { StageTimeline } from '@/components/broking/StageTimeline';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { DetailList } from '@/features/catalog/DetailList';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { PremiumCard } from './PremiumBreakdown';

/** Details: client, product, terms and the workflow facts of the quotation. */
export function DetailsTab({ quotation: q }: Readonly<{ quotation: Quotation }>) {
  const c = q.content;
  return (
    <div className="grid-2">
      <Card title="Quotation">
        <DetailList
          rows={[
            ['Proposal No.', q.quotationNo],
            ['ARN', q.arn],
            ['Client', `${q.clientCode} – ${q.clientName}`],
            ['Client e-mail', q.clientEmail],
            ['Product', `${q.productCode} (${q.lineCode})`],
            ['Market segment', q.marketSegment],
            ['Source channel', q.sourceChannel],
            [
              'Insurer',
              [c.insurerCode, c.insurerBranch].filter(Boolean).join(' / ') || 'To be advised',
            ],
            ['Period', `${formatDate(c.periodFrom)} to ${formatDate(c.periodTo)}`],
            ['Valid until', formatDate(c.validUntil)],
            ['Premium payment', c.directPayment ? 'Directly to the insurer' : 'Via BDOI'],
            ['Remarks', c.remarks],
          ]}
        />
      </Card>
      <Card title="Control">
        <DetailList
          rows={[
            ['Intake template', q.templateVersion],
            ['Version', `${q.currentVersion}${q.versionOpen ? ' (open)' : ' (submitted)'}`],
            ['Created', `${q.createdBy} ${formatDateTime(q.createdAt)}`],
            [
              'Submitted',
              q.submittedBy ? `${q.submittedBy} ${formatDateTime(q.submittedAt)}` : '—',
            ],
            ['Approved', q.approvedBy ? `${q.approvedBy} ${formatDateTime(q.approvedAt)}` : '—'],
            ['Sent to client', formatDateTime(q.sentAt) || '—'],
            ['Accepted', formatDateTime(q.acceptedAt) || '—'],
            ['Accepted risk groups', q.acceptedGroups.join(', ') || '—'],
            ['Accounts', q.accountArns.join(', ') || '—'],
          ]}
        />
      </Card>
    </div>
  );
}

const ITEM_COLUMNS = [
  { key: 'n', header: '#', numeric: true, render: (i: QuotationItemView) => i.itemNo },
  { key: 'g', header: 'Risk Group', numeric: true, render: (i: QuotationItemView) => i.riskGroup },
  { key: 'l', header: 'Risk', render: (i: QuotationItemView) => <strong>{i.label}</strong> },
  {
    key: 's',
    header: 'Sum Insured',
    numeric: true,
    render: (i: QuotationItemView) => <Amount value={i.sumInsured} />,
  },
  {
    key: 'r',
    header: 'Rate %',
    numeric: true,
    render: (i: QuotationItemView) => i.ratePercent ?? '',
  },
  {
    key: 'p',
    header: 'Premium',
    numeric: true,
    render: (i: QuotationItemView) => <Amount value={i.premium} />,
  },
];

/** Items and premium of one version. */
export function ItemsTab({
  content,
  currency,
  versionLabel,
}: Readonly<{ content: QuotationContent; currency: string; versionLabel: string }>) {
  return (
    <>
      <Card title={`Risk items – ${versionLabel}`} flush>
        <DataTable<QuotationItemView>
          rows={content.items}
          rowKey={(i) => i.itemNo}
          emptyMessage="No risk item yet."
          columns={ITEM_COLUMNS}
        />
      </Card>
      <PremiumCard premium={content.premium} currency={currency} />
    </>
  );
}

function DiffTable({ diff }: Readonly<{ diff: QuotationDiff }>) {
  const tone = { ADDED: 'diff-added', REMOVED: 'diff-removed', CHANGED: 'diff-changed' } as const;
  return (
    <div className="stack">
      <DataTable
        caption="Changed terms"
        rows={diff.fields}
        rowKey={(f) => f.field}
        emptyMessage="No term changed."
        columns={[
          { key: 'f', header: 'Term', render: (f) => f.field },
          { key: 'a', header: `Version ${diff.fromVersion}`, render: (f) => f.from || '—' },
          { key: 'b', header: `Version ${diff.toVersion}`, render: (f) => f.to || '—' },
        ]}
      />
      <DataTable
        caption="Item changes"
        rows={diff.items}
        rowKey={(i) => `${i.change}-${i.risk}`}
        emptyMessage="No item changed."
        columns={[
          {
            key: 'c',
            header: 'Change',
            render: (i) => <span className={tone[i.change]}>{humanize(i.change)}</span>,
          },
          { key: 'r', header: 'Risk', render: (i) => i.risk },
          {
            key: 's1',
            header: 'Sum Insured Before',
            numeric: true,
            render: (i) => <Amount value={i.fromSumInsured} />,
          },
          {
            key: 's2',
            header: 'Sum Insured After',
            numeric: true,
            render: (i) => <Amount value={i.toSumInsured} />,
          },
          {
            key: 'p1',
            header: 'Premium Before',
            numeric: true,
            render: (i) => <Amount value={i.fromPremium} />,
          },
          {
            key: 'p2',
            header: 'Premium After',
            numeric: true,
            render: (i) => <Amount value={i.toPremium} />,
          },
        ]}
      />
      <p>
        Gross premium: <Amount value={diff.grossFrom} /> → <Amount value={diff.grossTo} />{' '}
        <strong>
          (difference <Amount value={diff.grossDelta} />)
        </strong>
      </p>
    </div>
  );
}

/** Versions of the quotation (BRNB.020) and the differences between two of them. */
export function VersionsTab({
  quotation,
  from,
  to,
  onCompare,
}: Readonly<{
  quotation: Quotation;
  from: number;
  to: number;
  onCompare: (from: number, to: number) => void;
}>) {
  const versions = useQuery({
    queryKey: ['quotation', quotation.id, 'versions'],
    queryFn: () => quotationsApi.versions(quotation.id),
  });
  const diff = useQuery({
    queryKey: ['quotation', quotation.id, 'diff', from, to],
    queryFn: () => quotationsApi.diff(quotation.id, from, to),
    enabled: from !== to,
  });
  const numbers = (versions.data ?? []).map((v) => v.versionNo);
  return (
    <div className="stack">
      <ErrorAlert error={versions.error ?? diff.error} />
      <Card title="Versions" flush>
        <DataTable
          loading={versions.isLoading}
          rows={versions.data ?? []}
          rowKey={(v) => v.versionNo}
          columns={[
            { key: 'v', header: 'Version', render: (v) => <strong>v{v.versionNo}</strong> },
            {
              key: 's',
              header: 'State',
              render: (v) => <StatusBadge status={v.frozen ? 'SUBMITTED' : 'DRAFT'} />,
            },
            {
              key: 'i',
              header: 'Sum Insured',
              numeric: true,
              render: (v) => <Amount value={v.totalSumInsured} />,
            },
            {
              key: 'g',
              header: 'Gross Premium',
              numeric: true,
              render: (v) => <Amount value={v.grossPremium} />,
            },
            {
              key: 'b',
              header: 'Submitted',
              render: (v) => (v.frozenBy ? `${v.frozenBy} ${formatDateTime(v.frozenAt)}` : '—'),
            },
          ]}
        />
      </Card>
      {numbers.length > 1 && (
        <Card title="Compare versions">
          <div className="row">
            {[from, to].map((value, side) => (
              <label key={side === 0 ? 'from' : 'to'} className="checkbox">
                {side === 0 ? 'From' : 'To'}{' '}
                <select
                  className="select"
                  value={value}
                  onChange={(e) =>
                    side === 0
                      ? onCompare(Number(e.target.value), to)
                      : onCompare(from, Number(e.target.value))
                  }
                >
                  {numbers.map((n) => (
                    <option key={n} value={n}>
                      v{n}
                    </option>
                  ))}
                </select>
              </label>
            ))}
          </div>
          {diff.data && <DiffTable diff={diff.data} />}
        </Card>
      )}
    </div>
  );
}

/** Status history of the quotation's work case (who, when, reason, comment). */
export function HistoryTab({ entityId }: Readonly<{ entityId: number }>) {
  const detail = useQuery({
    queryKey: workflowKey(QUOTATION_ENTITY, entityId),
    queryFn: () => workflowApi.byRecord(QUOTATION_ENTITY, entityId),
  });
  return (
    <Card title="History">
      <StageTimeline history={detail.data?.history ?? []} />
    </Card>
  );
}
