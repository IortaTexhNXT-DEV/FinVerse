import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PACKAGE_REQUEST_ENTITY, productMaintApi } from '@/api/productmaint';
import type { PackageRequest, ResponseHistory } from '@/api/productmaint';
import { workflowApi } from '@/api/workflow';
import { StageTimeline } from '@/components/broking/StageTimeline';
import { workflowKey } from '@/components/broking/workflowKey';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime, humanize } from '@/utils/format';
import { typeLabel } from './packageRequest';

function step(by: string | undefined, at: string | undefined): string {
  return by === undefined ? '—' : `${by} · ${formatDateTime(at)}`;
}

/** Request header, recommendation and who did what (BRPM.008/009/024). */
export function DetailsTab({ request: p }: Readonly<{ request: PackageRequest }>) {
  const m = p.milestones;
  return (
    <div className="split">
      <Card title="Request">
        <dl className="detail-list">
          <dt>Type</dt>
          <dd>{typeLabel(p.requestType)}</dd>
          <dt>Scope</dt>
          <dd>{p.scope === 'CLIENT_SPECIFIC' ? 'Client-specific package' : 'Generic programme'}</dd>
          <dt>Client</dt>
          <dd>{p.clientName === undefined ? '—' : `${p.clientCode ?? ''} – ${p.clientName}`}</dd>
          <dt>Line / cover type</dt>
          <dd>
            {p.lineCode} / {p.coverTypeCode ?? '—'}
          </dd>
          <dt>Market segments</dt>
          <dd>{p.marketSegments.join(', ') || '—'}</dd>
          <dt>Reason</dt>
          <dd>
            {humanize(p.reason)}
            {p.reasonNote && ` – ${p.reasonNote}`}
          </dd>
          <dt>Negotiation</dt>
          <dd>{p.negotiationRequired ? 'Insurers approached' : 'No negotiation'}</dd>
          <dt>TSU recommendation</dt>
          <dd>{p.recommendation ?? '—'}</dd>
        </dl>
      </Card>
      <Card title="Approvals and steps">
        <dl className="detail-list">
          <dt>Submitted</dt>
          <dd>{step(m.submittedBy, m.submittedAt)}</dd>
          <dt>Marketing approval</dt>
          <dd>{step(m.approvedBy, m.approvedAt)}</dd>
          <dt>TSU recommendation</dt>
          <dd>{step(m.recommendedBy, m.recommendedAt)}</dd>
          <dt>TSU Head approval</dt>
          <dd>{step(m.tsuApprovedBy, m.tsuApprovedAt)}</dd>
          <dt>Terms final</dt>
          <dd>{step(m.termsFinalBy, m.termsFinalAt)}</dd>
          <dt>Requirements to ManCom</dt>
          <dd>{step(m.requirementsBy, m.requirementsAt)}</dd>
          <dt>MBS set-up</dt>
          <dd>{step(m.setupBy, m.setupAt)}</dd>
          <dt>Released</dt>
          <dd>{p.releasedAt === undefined ? '—' : formatDateTime(p.releasedAt)}</dd>
        </dl>
      </Card>
    </div>
  );
}

/** The catalog version the MBS set-up produced and where to complete and validate it (PMADD06). */
export function SetupTab({ request: p }: Readonly<{ request: PackageRequest }>) {
  if (p.resultingVersionNo === undefined || p.productCode === undefined) {
    return (
      <Card>
        <EmptyState message="MBS sets the package up after the ManCom sign-off" />
      </Card>
    );
  }
  return (
    <Card title="Catalog version">
      <dl className="detail-list">
        <dt>Product</dt>
        <dd className="mono">{p.productCode}</dd>
        <dt>Version</dt>
        <dd>{p.resultingVersionNo}</dd>
        <dt>Request stage</dt>
        <dd>
          <StatusBadge status={p.status} />
        </dd>
      </dl>
      <p className="muted">
        MBS completes the draft version and submits it for validation; a PRODUCT_VALIDATE holder
        (never the maker) validates and releases it, which releases this request.
      </p>
      <Link
        className="btn btn-secondary"
        to={`/catalog/products/${p.productCode}/versions/${p.resultingVersionNo}`}
      >
        <ExternalLink size={16} aria-hidden="true" /> Open Version Editor
      </Link>
    </Card>
  );
}

const HISTORY_COLUMNS: Column<ResponseHistory>[] = [
  { key: 'at', header: 'Changed', render: (h) => formatDateTime(h.changedAt) },
  { key: 'by', header: 'By', render: (h) => h.changedBy },
  { key: 'rev', header: 'Rev.', numeric: true, render: (h) => h.revision },
  { key: 'outcome', header: 'Outcome', render: (h) => <StatusBadge status={h.outcome} /> },
  { key: 'rate', header: 'Rate %', numeric: true, render: (h) => h.rate ?? '—' },
  { key: 'remarks', header: 'Remarks', render: (h) => h.remarks ?? '—' },
];

/** Status history of the request and the revision history of the insurer responses (BRPM.024). */
export function HistoryTab({ requestId }: Readonly<{ requestId: number }>) {
  const detail = useQuery({
    queryKey: workflowKey(PACKAGE_REQUEST_ENTITY, requestId),
    queryFn: () => workflowApi.byRecord(PACKAGE_REQUEST_ENTITY, requestId),
  });
  const history = useQuery({
    queryKey: ['package-request-tab', requestId, 'history'],
    queryFn: () => productMaintApi.history(requestId),
  });
  return (
    <div className="stack">
      <ErrorAlert error={detail.error ?? history.error} />
      <Card title="Status history">
        <StageTimeline history={detail.data?.history ?? []} />
      </Card>
      <Card title="Insurer response revisions" flush>
        <DataTable<ResponseHistory>
          rows={history.data ?? []}
          rowKey={(h) => `${h.responseId}-${h.revision}`}
          emptyMessage="No insurer response keyed in yet"
          columns={HISTORY_COLUMNS}
        />
      </Card>
    </div>
  );
}
