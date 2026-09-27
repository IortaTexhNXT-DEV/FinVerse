import { useQuery } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';
import type { Controlled, InsurerRule, LetterRule, LimitRule, MatrixRow } from '@/api/submitted';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { formatAmount, humanize } from '@/utils/format';
import { SBM_LOV, SOURCE_LABELS, SUBMITTED_SECTION } from '../common/submittedCodes';
import { ControlledTable } from './ControlledTable';
import { RuleSets } from './RuleSets';
import { Scopes } from './Scopes';

const TABS = [
  { id: 'rules', label: 'Rule Sets' },
  { id: 'limits', label: 'Insurer Limits' },
  { id: 'insurers', label: 'Insurer Assignment' },
  { id: 'letters', label: 'Letter Rules' },
  { id: 'matrix', label: 'Approval Matrix' },
  { id: 'sources', label: 'Sources' },
  { id: 'scopes', label: 'User Scopes' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const money = (v: number | null) => (v === null ? '—' : formatAmount(v));

const limitOf = (r: Controlled<LimitRule>) => r.limits ?? r.row;
const rowOf = <T,>(r: Controlled<T>) => r.row ?? r.limits;

function Body({ tab }: Readonly<{ tab: TabId }>) {
  const sources = useQuery({
    queryKey: ['submitted', 'sources'],
    queryFn: () => submittedApi.sources(),
    enabled: tab === 'sources',
  });
  switch (tab) {
    case 'limits':
      return (
        <ControlledTable<LimitRule>
          kind="limits"
          title="New Insurer Limit"
          load={submittedApi.limits}
          columns={[
            {
              key: 'insurer',
              header: 'Insurer',
              render: (r) => <InsurerName code={limitOf(r)?.insurerCode} />,
            },
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => (
                <LovLabel type={SBM_LOV.segment} code={limitOf(r)?.segment} empty="All" />
              ),
            },
            {
              key: 'si',
              header: 'Maximum Sum Insured',
              kind: 'amount',
              render: (r) => money(limitOf(r)?.maxSumInsured ?? null),
            },
            {
              key: 'age',
              header: 'Maximum Vehicle Age',
              kind: 'amount',
              render: (r) => limitOf(r)?.maxVehicleAge ?? '—',
            },
            { key: 'desc', header: 'Description', render: (r) => limitOf(r)?.description ?? '—' },
          ]}
        />
      );
    case 'insurers':
      return (
        <ControlledTable<InsurerRule>
          kind="insurers"
          title="New Insurer Assignment Rule"
          load={submittedApi.insurerRules}
          columns={[
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => <LovLabel type={SBM_LOV.segment} code={rowOf(r)?.segment} />,
            },
            {
              key: 'vehicle',
              header: 'Vehicle Type',
              render: (r) => rowOf(r)?.vehicleType ?? 'Any',
            },
            { key: 'occupancy', header: 'Occupancy', render: (r) => rowOf(r)?.occupancy ?? 'Any' },
            {
              key: 'insurer',
              header: 'Insurer',
              render: (r) => <InsurerName code={rowOf(r)?.insurerCode} />,
            },
            {
              key: 'priority',
              header: 'Priority',
              kind: 'amount',
              render: (r) => rowOf(r)?.priority ?? '—',
            },
            {
              key: 'exclude',
              header: 'Never the Expiring Insurer',
              render: (r) => (rowOf(r)?.excludeExpiring ? 'Yes' : 'No'),
            },
          ]}
        />
      );
    case 'letters':
      return (
        <ControlledTable<LetterRule>
          kind="letters"
          title="New Letter Rule"
          load={submittedApi.letterRules}
          columns={[
            {
              key: 'type',
              header: 'Letter',
              render: (r) => <LovLabel type={SBM_LOV.letterType} code={rowOf(r)?.letterType} />,
            },
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => (
                <LovLabel type={SBM_LOV.segment} code={rowOf(r)?.segment} empty="All" />
              ),
            },
            {
              key: 'days',
              header: 'Days Before Expiry',
              kind: 'amount',
              render: (r) => rowOf(r)?.daysFromExpiry ?? '—',
            },
            { key: 'channel', header: 'Channel', render: (r) => humanize(rowOf(r)?.channel ?? '') },
            { key: 'desc', header: 'Description', render: (r) => rowOf(r)?.description ?? '—' },
          ]}
        />
      );
    case 'matrix':
      return (
        <ControlledTable<MatrixRow>
          kind="matrix"
          title="New Approval Level"
          load={submittedApi.matrix}
          columns={[
            { key: 'doc', header: 'Document', render: (r) => rowOf(r)?.document ?? '—' },
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => (
                <LovLabel type={SBM_LOV.segment} code={rowOf(r)?.segment} empty="All" />
              ),
            },
            {
              key: 'from',
              header: 'Sum Insured From',
              kind: 'amount',
              render: (r) => money(rowOf(r)?.tsiFrom ?? null),
            },
            {
              key: 'to',
              header: 'Sum Insured To',
              kind: 'amount',
              render: (r) => money(rowOf(r)?.tsiTo ?? null),
            },
            {
              key: 'level',
              header: 'Level',
              kind: 'amount',
              render: (r) => rowOf(r)?.level ?? '—',
            },
            { key: 'title', header: 'Signatory', render: (r) => rowOf(r)?.signatoryTitle ?? '—' },
            {
              key: 'approver',
              header: 'Named Approver',
              render: (r) =>
                rowOf(r)?.approverUsername ? (
                  <UserName login={rowOf(r)?.approverUsername ?? ''} />
                ) : (
                  '—'
                ),
            },
          ]}
        />
      );
    case 'sources':
      return (
        <Card flush>
          <ErrorAlert error={sources.error} />
          <DataTable
            loading={sources.isLoading}
            rows={sources.data ?? []}
            rowKey={(s) => s.id}
            columns={[
              { key: 'name', header: 'Source', render: (s) => SOURCE_LABELS[s.code] ?? s.name },
              {
                key: 'segment',
                header: 'Segment',
                render: (s) => <LovLabel type={SBM_LOV.segment} code={s.segment} empty="Per row" />,
              },
              { key: 'format', header: 'Format', render: (s) => humanize(s.format) },
              { key: 'active', header: 'Active', render: (s) => (s.active ? 'Yes' : 'No') },
            ]}
          />
        </Card>
      );
    case 'scopes':
      return <Scopes />;
    default:
      return <RuleSets />;
  }
}

/**
 * Submitted Policies Setup (FR-SP-080 to 084): the rule sets of the processing steps, the insurer
 * limits and assignment rules, the letter rules and the IAAF / TOR approval matrix, each changed
 * by a maker and approved by a checker; the source register and the user scopes.
 */
export default function SetupPage() {
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'rules',
  );
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Submitted Policies Setup"
        description="Rules, limits, insurer assignment, letters, approval matrix, sources and scopes."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Body tab={tab} />
    </div>
  );
}
