import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PACKAGE_REQUEST_ENTITY, productMaintApi } from '@/api/productmaint';
import type { PackageRequest, ResponseHistory } from '@/api/productmaint';
import { workflowApi } from '@/api/workflow';
import { HistoryTable } from '@/components/broking/HistoryTable';
import { workflowKey } from '@/components/broking/workflowKey';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { CoverTypeLabel, LineLabel, LovLabel, LovLabels } from '@/components/broking/LovLabel';
import { typeLabel } from './packageRequest';
import { UserName } from '@/components/ui/UserName';
import { displayNameOf } from '@/api/users';

function step(by: string | undefined, at: string | undefined): string {
  return by === undefined ? '—' : `${displayNameOf(by)} · ${formatDateTime(at)}`;
}

/** Request header, recommendation and who did what (BRPM.008/009/024). */
export function DetailsTab({ request: p }: Readonly<{ request: PackageRequest }>) {
  const m = p.milestones;
  return (
    <div className="grid-2">
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
            <LineLabel code={p.lineCode} /> /{' '}
            <CoverTypeLabel line={p.lineCode} code={p.coverTypeCode} />
          </dd>
          <dt>Market segments</dt>
          <dd>
            <LovLabels type="MARKET_SEGMENT" codes={p.marketSegments} />
          </dd>
          <dt>Reason</dt>
          <dd>
            <LovLabel type="PKG_REQUEST_REASON" code={p.reason} />
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
        <dd>
          <span className="mono">{p.productCode}</span>
          {` – ${p.title}`}
        </dd>
        <dt>Version</dt>
        <dd>{p.resultingVersionNo}</dd>
        <dt>Request stage</dt>
        <dd>
          <StatusBadge status={p.status} />
        </dd>
      </dl>
      <p className="muted">
        MBS completes the draft version and submits it for validation; a user who validates products
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
  { key: 'by', header: 'By', render: (h) => <UserName login={h.changedBy} /> },
  { key: 'rev', header: 'Rev.', numeric: true, render: (h) => h.revision },
  { key: 'outcome', header: 'Outcome', render: (h) => <StatusBadge full status={h.outcome} /> },
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
        <HistoryTable history={detail.data?.history ?? []} terminal={detail.data?.stageTerminal} />
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
