import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarRange, Lock, UserRound, Users, Wallet, Building2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';
import { DispositionsTab, HistoryTab } from './AccountHistoryTabs';
import { PaymentsTab, PolicyTab, SummaryTab, TimelineTab } from './AccountTabs';
import { collectionsApi } from './api';
import type { Account } from './api';
import { ACCOUNT_TABS } from './collectionsLogic';
import type { AccountTabId } from './collectionsLogic';
import { DetailsDialog, DispositionDialog, EffortDialog } from './WorkDialogs';
import './collections.css';

function flags(a: Account): string[] {
  const out: string[] = [];
  const l = a.ledger;
  if (a.item.dpFlag) {
    out.push('Direct Payment');
  }
  if (a.item.cwtFlag) {
    out.push('2% CWT');
  }
  if (l?.hold === true) {
    out.push('On Hold');
  }
  if (l?.pendingNegativeAdjustment === true) {
    out.push('Pending Negative Adjustment');
  }
  if (l?.lockOwner !== undefined) {
    out.push(`Locked by ${l.lockOwner}`);
  }
  if (a.item.taggingOwner !== undefined) {
    out.push(`${a.item.taggingOwner === 'OPERATIONS' ? 'Operations' : 'Marketing'} Action`);
  }
  return out;
}

function Summary({ account }: Readonly<{ account: Account }>) {
  const i = account.item;
  return (
    <RecordSummary
      title={i.assuredName}
      chips={
        <>
          <ReferenceChip label="Invoice" value={i.invoiceNo} />
          <ReferenceChip label="ARN" value={i.arn} />
          <StatusBadge status={i.status} />
          <StatusBadge status={account.ledger?.paymentStatus ?? i.paymentStatus} />
          {account.ledger !== undefined && <StatusBadge status={account.ledger.remittanceStatus} />}
        </>
      }
      flags={flags(account).map((f) => (
        <span key={f} className="tag">
          {f}
        </span>
      ))}
      facts={[
        {
          icon: Users,
          label: 'Client',
          value: (
            <Link to={`/collections/clients/${encodeURIComponent(i.clientCode)}`}>
              {i.clientCode}
            </Link>
          ),
        },
        {
          icon: Wallet,
          label: 'Outstanding / PR2307',
          value: `${i.currency} ${formatAmount(i.netOutstanding)} / ${formatAmount(i.outstandingPr2307)}`,
        },
        {
          icon: CalendarRange,
          label: 'Aging',
          value: `${i.agingDays} days (${i.agingBracket ?? '—'}) · booked ${formatDate(i.bookingDate)}`,
        },
        { icon: Building2, label: 'Insurer', value: i.insurerCode },
        {
          icon: UserRound,
          label: 'Handler / AO / UH',
          value: [i.currentHandler ?? 'Unassigned', i.aoUsername, i.unitHead]
            .filter(Boolean)
            .join(' · '),
        },
      ]}
    />
  );
}

function TabBody({ tab, account }: Readonly<{ tab: AccountTabId; account: Account }>) {
  const no = account.item.invoiceNo;
  switch (tab) {
    case 'payments':
      return <PaymentsTab invoiceNo={no} />;
    case 'timeline':
      return <TimelineTab invoiceNo={no} />;
    case 'policy':
      return <PolicyTab invoiceNo={no} />;
    case 'dispositions':
      return <DispositionsTab invoiceNo={no} />;
    case 'history':
      return <HistoryTab invoiceNo={no} />;
    default:
      return <SummaryTab account={account} />;
  }
}

/** Takes the edit lock while the collector works on the account and releases it on leaving. */
function useEditLock(invoiceNo: string, enabled: boolean) {
  const queryClient = useQueryClient();
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    void collectionsApi
      .lock(invoiceNo)
      .then(() =>
        queryClient.invalidateQueries({ queryKey: ['collections', 'account', invoiceNo] }),
      )
      .catch(() => undefined);
    return () => {
      void collectionsApi.unlock(invoiceNo).catch(() => undefined);
    };
  }, [invoiceNo, enabled, queryClient]);
}

type Dialog = 'disposition' | 'effort' | 'details';

/** The page actions of an account: Invoice 360, refresh and the collector's work. */
function AccountActions({
  invoiceNo,
  mayOpen360,
  mayRefresh,
  workable,
  refreshing,
  onRefresh,
  onDialog,
}: Readonly<{
  invoiceNo: string;
  mayOpen360: boolean;
  mayRefresh: boolean;
  workable: boolean;
  refreshing: boolean;
  onRefresh: () => void;
  onDialog: (d: Dialog) => void;
}>) {
  return (
    <>
      {mayOpen360 && (
        <Link
          className="btn btn-secondary"
          to={`/operations/invoices/${encodeURIComponent(invoiceNo)}`}
        >
          Open Invoice 360
        </Link>
      )}
      {mayRefresh && (
        <Button variant="secondary" busy={refreshing} onClick={onRefresh}>
          Refresh from Ledger
        </Button>
      )}
      {workable && (
        <>
          <Button variant="secondary" onClick={() => onDialog('details')}>
            Update Remarks
          </Button>
          <Button variant="secondary" onClick={() => onDialog('effort')}>
            Log Effort
          </Button>
          <Button onClick={() => onDialog('disposition')}>Record Disposition</Button>
        </>
      )}
    </>
  );
}

/**
 * Collection account (COLLECTIONS_DESIGN 11; BRCLXN.016-023, 043, 046, 052-057): the invoice's
 * summary with payment, remittance, hold, DP, CWT, lock and owner chips, the "is editing" lock,
 * the collector's actions and the tabs Summary, Payments, Timeline, Policy & Co-insurance,
 * Dispositions & Efforts and History.
 */
export default function AccountPage() {
  const invoiceNo = decodeURIComponent(useParams().invoiceNo ?? '');
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<AccountTabId>('summary');
  const [dialog, setDialog] = useState<Dialog>();
  const mayWork = can('CLX_WORK');
  const account = useQuery({
    queryKey: ['collections', 'account', invoiceNo],
    queryFn: () => collectionsApi.account(invoiceNo),
  });
  useEditLock(invoiceNo, mayWork);
  const done = async (message: string) => {
    setDialog(undefined);
    await queryClient.invalidateQueries({ queryKey: ['collections'] });
    toast.success(message);
  };
  const dispose = useMutation({
    mutationFn: (input: Parameters<typeof collectionsApi.dispose>[1]) =>
      collectionsApi.dispose(companyId, input),
    onSuccess: () => done('Disposition recorded'),
  });
  const effort = useMutation({
    mutationFn: (input: Parameters<typeof collectionsApi.effort>[1]) =>
      collectionsApi.effort(companyId, input),
    onSuccess: () => done('Effort logged'),
  });
  const details = useMutation({
    mutationFn: (v: { remarks: string; category: string }) =>
      collectionsApi.details(companyId, invoiceNo, v.remarks, v.category),
    onSuccess: () => done('Remarks and category saved'),
  });
  const refresh = useMutation({
    mutationFn: () => collectionsApi.refreshOne(invoiceNo),
    onSuccess: () => done('Account refreshed from the ledger'),
  });
  if (account.data === undefined) {
    return account.error ? (
      <ErrorAlert error={account.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const a = account.data;
  const lockedByOther = a.lock.editingBy !== undefined && !a.lock.mine;
  const workable =
    mayWork && !lockedByOther && (a.item.status === 'OPEN' || a.item.status === 'CREDIT');
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · PR Worklist"
        backTo="/collections/worklist"
        title={a.item.invoiceNo}
        description={[
          a.item.policyNo,
          a.item.segment,
          a.item.salesUnit,
          `refreshed ${formatDateTime(a.item.lastRefreshedAt)}`,
        ]
          .filter(Boolean)
          .join(' · ')}
        actions={
          <AccountActions
            invoiceNo={invoiceNo}
            mayOpen360={can('OPS_VIEW')}
            mayRefresh={mayWork}
            workable={workable}
            refreshing={refresh.isPending}
            onRefresh={() => refresh.mutate()}
            onDialog={setDialog}
          />
        }
      />
      {lockedByOther && (
        <div className="clx-lock-banner" role="status">
          <Lock size={16} aria-hidden="true" />
          {a.lock.editingBy} is editing since {formatDateTime(a.lock.since)}
        </div>
      )}
      <ErrorAlert error={refresh.error} />
      <Summary account={a} />
      <Tabs tabs={ACCOUNT_TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} account={a} />
      {dialog === 'disposition' && (
        <DispositionDialog
          invoiceNos={[invoiceNo]}
          busy={dispose.isPending}
          error={dispose.error}
          onClose={() => setDialog(undefined)}
          onSave={(input) => dispose.mutate(input)}
        />
      )}
      {dialog === 'effort' && (
        <EffortDialog
          invoiceNos={[invoiceNo]}
          busy={effort.isPending}
          error={effort.error}
          onClose={() => setDialog(undefined)}
          onSave={(input) => effort.mutate(input)}
        />
      )}
      {dialog === 'details' && (
        <DetailsDialog
          remarks={a.item.remarks}
          category={a.item.category}
          busy={details.isPending}
          error={details.error}
          onClose={() => setDialog(undefined)}
          onSave={(remarks, category) => details.mutate({ remarks, category })}
        />
      )}
    </div>
  );
}
