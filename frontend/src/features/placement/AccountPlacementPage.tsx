import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarClock, FileStack, ShieldCheck, Wallet } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ACCOUNT_ENTITY, accountsApi } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { placementApi } from '@/api/placement';
import type { InsurerReturn, PlacementView, Slip } from '@/api/placement';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDateTime, humanize } from '@/utils/format';
import { GatePanel } from './GatePanel';
import { HoldCoverPanel } from './HoldCoverPanel';
import { PlacementActions } from './PlacementActions';
import { SlipActions } from './SlipActions';

const TABS = [
  { id: 'gate', label: 'Payment Gate' },
  { id: 'slips', label: 'Placement Slips' },
  { id: 'hold', label: 'Hold Cover' },
  { id: 'returns', label: 'Insurer Returns' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function Fact({
  icon,
  label,
  children,
}: Readonly<{ icon: ReactNode; label: string; children: ReactNode }>) {
  return (
    <div className="row">
      {icon}
      <span className="muted">{label}</span>
      <strong>{children}</strong>
    </div>
  );
}

function Summary({ account, view }: Readonly<{ account: Account; view: PlacementView }>) {
  const slip = view.slips.find((s) => s.status !== 'SUPERSEDED');
  const hold = view.holdCovers[0];
  return (
    <Card>
      <div className="grid-2">
        <Fact icon={<FileStack size={16} aria-hidden="true" />} label="Product">
          {account.productCode} · {humanize(account.lineCode)}
        </Fact>
        <Fact icon={<Building2 size={16} aria-hidden="true" />} label="Insurer">
          {[account.insurerCode, account.insurerBranch].filter(Boolean).join(' / ') || '—'}
        </Fact>
        <Fact icon={<Wallet size={16} aria-hidden="true" />} label="Gross premium">
          {account.currency} {formatAmount(account.premium.grossPremium)}
        </Fact>
        <Fact icon={<ShieldCheck size={16} aria-hidden="true" />} label="Payment gate">
          {view.gate.open ? 'Open' : 'Waiting'} ({humanize(view.gate.rule)})
        </Fact>
        <Fact icon={<FileStack size={16} aria-hidden="true" />} label="Placement slip">
          {slip ? `${slip.displayNo} (${humanize(slip.status)})` : 'Not generated'}
        </Fact>
        <Fact icon={<CalendarClock size={16} aria-hidden="true" />} label="Hold cover">
          {hold ? `${humanize(hold.status)} until ${hold.expiryDate}` : 'None'}
        </Fact>
      </div>
    </Card>
  );
}

function SlipsTable({ slips, onChanged }: Readonly<{ slips: Slip[]; onChanged: () => void }>) {
  return (
    <Card title="Placement Slips" flush>
      <DataTable<Slip>
        caption="Placement slips"
        rows={slips}
        rowKey={(s) => s.id}
        emptyMessage="No placement slip yet."
        columns={[
          { key: 'no', header: 'Slip', render: (s) => <code>{s.displayNo}</code> },
          { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
          {
            key: 'insurer',
            header: 'Insurer',
            render: (s) => `${s.insurerCode} / ${s.branchCode}`,
          },
          {
            key: 'sent',
            header: 'Sent',
            render: (s) => (s.sentAt ? `${formatDateTime(s.sentAt)} (${s.sendCount})` : '—'),
          },
          {
            key: 'actions',
            header: '',
            render: (s) => <SlipActions slip={s} onChanged={onChanged} />,
          },
        ]}
      />
    </Card>
  );
}

function ReturnsTable({ returns }: Readonly<{ returns: InsurerReturn[] }>) {
  return (
    <Card title="Insurer Returns" flush>
      <DataTable<InsurerReturn>
        caption="Insurer returns"
        rows={returns}
        rowKey={(r) => r.id}
        emptyMessage="Never returned by the insurer."
        columns={[
          {
            key: 'when',
            header: 'Returned',
            render: (r) => `${formatDateTime(r.createdAt)} by ${r.createdBy}`,
          },
          { key: 'reason', header: 'Reason', render: (r) => humanize(r.reasonCode) },
          { key: 'remarks', header: 'Insurer Remarks', render: (r) => r.remarks ?? '—' },
          { key: 'slip', header: 'Slip', render: (r) => r.slipNo ?? '—' },
          {
            key: 'resolution',
            header: 'Resolution',
            render: (r) => (r.resolution ? humanize(r.resolution) : 'Open'),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Placement of one account: summary, workflow status with the placement actions, and tabs for the
 * payment gate (rule and evidence), placement slips, hold cover and insurer returns.
 */
export default function AccountPlacementPage() {
  const arn = useParams().arn ?? '';
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('gate');
  const account = useQuery({
    queryKey: ['account', 'arn', arn],
    queryFn: () => accountsApi.byArn(arn),
  });
  const view = useQuery({
    queryKey: ['placement', 'account', arn],
    queryFn: () => placementApi.account(arn),
  });
  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['placement'] });
    void queryClient.invalidateQueries({ queryKey: ['account'] });
    if (account.data) {
      void queryClient.invalidateQueries({
        queryKey: workflowKey(ACCOUNT_ENTITY, account.data.id),
      });
    }
  };
  if (account.data === undefined || view.data === undefined) {
    const error = account.error ?? view.error;
    return error ? <ErrorAlert error={error} /> : <span className="spinner" aria-label="Loading" />;
  }
  const a = account.data;
  const v = view.data;
  return (
    <div className="stack">
      <PageHeader
        backTo="/placement"
        section="Placement & Booking · Account Placement"
        title={a.clientName}
        description={`${a.clientCode} · ${a.productCode} · placement, hold cover and insurer returns`}
        actions={
          <>
            <ReferenceChip label="ARN" value={a.arn} />
            <StatusBadge status={a.status} />
            <Link className="btn btn-secondary" to={`/accounts/${a.id}`}>
              Open Account
            </Link>
          </>
        }
      />
      <Summary account={a} view={v} />
      <WorkflowPanel
        entityType={ACCOUNT_ENTITY}
        entityId={a.id}
        onChanged={refresh}
        renderBusinessActions={(actions) => (
          <PlacementActions arn={a.arn} status={a.status} actions={actions} onChanged={refresh} />
        )}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'gate' && <GatePanel gate={v.gate} onChanged={refresh} />}
      {tab === 'slips' && <SlipsTable slips={v.slips} onChanged={refresh} />}
      {tab === 'hold' && (
        <HoldCoverPanel
          arn={a.arn}
          status={a.status}
          holdCovers={v.holdCovers}
          onChanged={refresh}
        />
      )}
      {tab === 'returns' && <ReturnsTable returns={v.returns} />}
    </div>
  );
}
