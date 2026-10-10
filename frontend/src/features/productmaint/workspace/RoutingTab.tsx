import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { pmRoutingApi } from '@/api/pmRouting';
import type { MarketingStep, RoutingView } from '@/api/pmRouting';
import type { PackageRequest, RequestDetails } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { SOURCES } from '../requestDetails';
import { ManComPanel } from './ManComPanel';

const LEVELS = ['Marketing Team Leader', 'Marketing Team Head', 'Marketing Unit Head'];

const MARKETING_COLUMNS: Column<MarketingStep>[] = [
  { key: 'l', header: 'Level', render: (s) => LEVELS[s.level - 1] ?? 'Marketing' },
  { key: 'a', header: 'Approver', render: (s) => <UserName login={s.approver} /> },
  { key: 'd', header: 'Decision', render: () => 'Approved' },
  { key: 'r', header: 'Remarks', render: (s) => s.remarks ?? '' },
  { key: 't', header: 'Approved', render: (s) => formatDateTime(s.decidedAt) },
];

function money(value: number | undefined): string {
  return value === undefined ? '' : formatAmount(value);
}

type DetailField = readonly [label: string, value: (d: RequestDetails) => ReactNode];

const DETAIL_FIELDS: readonly DetailField[] = [
  ['Business Origin', (d) => <LovLabel type="PKG_BUSINESS_ORIGIN" code={d.businessOrigin} />],
  ['Account Officer', (d) => <UserName login={d.accountOfficer} />],
  ['Unit Head', (d) => <UserName login={d.unitHead} />],
  ["Insured's Name", (d) => d.insuredName],
  ['Estimated No. of Policies', (d) => d.estimatedPolicies?.toString()],
  ['Estimated Total Basic Premium', (d) => money(d.estimatedPremium)],
  ['Type of Cover', (d) => d.typeOfCover],
  ['Description of Cover', (d) => d.descriptionOfCover],
  ['Cover / Undertaking', (d) => d.cover],
  ['Extensions of Cover', (d) => d.extensions],
  ['Warranties, Clauses', (d) => d.warranties],
  ['Other Instructions', (d) => d.otherInstructions],
  ['Maximum Limit/s', (d) => d.maximumLimits],
  ['Premium', (d) => money(d.premium)],
  ['Incentive Eligible', (d) => (d.incentiveEligible === true ? 'Yes' : 'No')],
  ['Incentive Amount', (d) => money(d.incentiveAmount)],
  [
    'Incentive Commission Rate',
    (d) => (d.incentiveRate === undefined ? '' : `${String(d.incentiveRate)}%`),
  ],
];

function detailItems(source: string, d: RequestDetails | null) {
  const details = d ?? {};
  return [
    { label: 'Source', value: SOURCES.find((s) => s.value === source)?.label ?? source },
    ...DETAIL_FIELDS.map(([label, value]) => ({ label, value: value(details) ?? '' })),
  ];
}

function DeploymentCard({
  request,
  routing,
}: Readonly<{ request: PackageRequest; routing: RoutingView }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const deploy = useMutation({
    mutationFn: () => pmRoutingApi.requestDeployment(request.id),
    onSuccess: async (r) => {
      toast.success(`Package deployment request ${r.deployment.number ?? ''} sent to MBS`);
      await queryClient.invalidateQueries({ queryKey: ['package-request', request.id] });
    },
  });
  const d = routing.deployment;
  const ready =
    request.status === 'MANCOM_APPROVED' && (can('PKG_NEGOTIATE') || can('PKG_REQUEST'));
  return (
    <Card
      title="Package Deployment Request"
      actions={
        ready && (
          <Button busy={deploy.isPending} onClick={() => deploy.mutate()}>
            Request for Deployment
          </Button>
        )
      }
    >
      <ErrorAlert error={deploy.error} />
      <DefinitionGrid
        items={[
          {
            label: 'Forwarding to MBS',
            value: d.mode === 'MANUAL' ? 'By the Request for Deployment button' : 'Automatic',
          },
          { label: 'Deployment Request Number', value: d.number ?? '' },
          { label: 'Requested', value: formatDateTime(d.requestedAt) },
          { label: 'Requested By', value: <UserName login={d.requestedBy} /> },
        ]}
      />
    </Card>
  );
}

/**
 * The routing of a package request (BDOI FRS FRPM.011.02, FRPM.014.01 and FRPM.015.01): the Source
 * and package details, the Marketing approvals, the ManCom approval and the deployment request.
 */
export function RoutingTab({ request }: Readonly<{ request: PackageRequest }>) {
  const routing = useQuery({
    queryKey: ['package-request', request.id, 'routing'],
    queryFn: () => pmRoutingApi.routing(request.id),
  });
  if (routing.data === undefined) {
    return routing.error ? <ErrorAlert error={routing.error} /> : <LoadingPanel />;
  }
  const r = routing.data;
  return (
    <div className="stack">
      <Card title="Source and Package Details">
        <DefinitionGrid items={detailItems(r.source, r.details)} />
      </Card>
      <Card
        title="Marketing Approvals"
        actions={
          <span className="muted">
            {r.marketing.chained
              ? 'Team Leader, then Team Head, then Unit Head'
              : 'One approval by the Team Leader, Team Head or Unit Head'}
          </span>
        }
        flush
      >
        <DataTable<MarketingStep>
          rows={r.marketing.history}
          rowKey={(s) => `${String(s.level)}-${s.decidedAt}`}
          columns={MARKETING_COLUMNS}
          emptyMessage="No Marketing approval yet."
        />
      </Card>
      {r.mancom.selectedRouting && <ManComPanel request={request} routing={r} />}
      <DeploymentCard request={request} routing={r} />
    </div>
  );
}
