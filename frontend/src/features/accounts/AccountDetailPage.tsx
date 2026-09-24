import { useQuery } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { accountsApi, ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { PolicyPanel } from '@/features/issuance/PolicyPanel';
import { PlacementPanel } from '@/features/placement/PlacementPanel';
import { formatAmount } from '@/utils/format';
import { AccountActions } from './AccountActions';
import { AccountCheckPanel } from './AccountCheckPanel';
import { DetailsPanel, HistoryPanel, ItemsPanel } from './AccountPanels';
import { AccountTagsCard } from './AccountTagsCard';
import { PremiumSummary } from './PremiumSummary';
import { useAccountRefresh } from './useAccountRefresh';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'items', label: 'Risk items' },
  { id: 'premium', label: 'Premium' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
  // Placement and issuance tabs: mounted here until the account page offers an extension point.
  { id: 'placement', label: 'Placement' },
  { id: 'policy', label: 'Policy' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const EDITABLE = new Set(['DRAFT', 'RETURNED_TO_MARKETING']);

function TabBody({ tab, account }: Readonly<{ tab: TabId; account: Account }>) {
  switch (tab) {
    case 'details':
      return (
        <>
          <AccountTagsCard account={account} />
          <DetailsPanel account={account} />
        </>
      );
    case 'items':
      return <ItemsPanel account={account} />;
    case 'premium':
      return <PremiumSummary accountId={account.id} />;
    case 'documents':
      return (
        <Attachments
          entityType={ACCOUNT_ENTITY}
          entityId={account.id}
          title="Documents"
          reference={account.arn}
        />
      );
    case 'emails':
      return (
        <Card title="E-mails" flush>
          <SentMessages entityType={ACCOUNT_ENTITY} entityId={account.id} />
        </Card>
      );
    case 'placement':
      return <PlacementPanel arn={account.arn} />;
    case 'policy':
      return <PolicyPanel arn={account.arn} />;
    default:
      return <HistoryPanel accountId={account.id} />;
  }
}

/**
 * One account (BRNB.050): status and work case with the actions the user may take, readiness
 * check while it is open for changes, and tabs for details, items, premium, documents, e-mails
 * and history.
 */
export default function AccountDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('details');
  const refresh = useAccountRefresh(id);
  const account = useQuery({ queryKey: ['account', id], queryFn: () => accountsApi.get(id) });
  if (account.data === undefined) {
    return account.error ? (
      <ErrorAlert error={account.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const a = account.data;
  const editable = EDITABLE.has(a.status) && can('ACCOUNT_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement · Account"
        title={a.clientName}
        description={`${a.productCode} · ${a.currency} ${formatAmount(a.premium.grossPremium)} gross premium`}
        actions={
          <>
            <ReferenceChip label="ARN" value={a.arn} />
            <StatusBadge status={a.status} />
            {a.freeFirstYear.active && <StatusBadge status="FFY" />}
            {a.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
            {editable && (
              <Link className="btn btn-secondary" to={`/accounts/${a.id}/edit`}>
                <Pencil size={14} aria-hidden="true" /> Edit
              </Link>
            )}
          </>
        }
      />
      <WorkflowPanel
        entityType={ACCOUNT_ENTITY}
        entityId={a.id}
        showHistory={false}
        onChanged={() => void refresh()}
        renderBusinessActions={(actions) => <AccountActions account={a} actions={actions} />}
      />
      {EDITABLE.has(a.status) && <AccountCheckPanel accountId={a.id} />}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} account={a} />
    </div>
  );
}
