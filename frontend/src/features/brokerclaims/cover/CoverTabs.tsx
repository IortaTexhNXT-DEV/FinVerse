import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import type { LocationRef } from '../location/api';
import type {
  CoverClaim,
  CoverDetail,
  CoverInvoice,
  CoverItem,
  Endorsement,
  PolicyYear,
} from './api';

type TabId = 'years' | 'items' | 'endorsements' | 'invoices' | 'claims' | 'refs';

const TABS: { id: TabId; label: string }[] = [
  { id: 'years', label: 'Policy Years' },
  { id: 'items', label: 'Items & Locations' },
  { id: 'endorsements', label: 'Endorsements' },
  { id: 'invoices', label: 'Invoices' },
  { id: 'claims', label: 'Claims' },
  { id: 'refs', label: 'Insurer Location Refs' },
];

function YearsTable({ rows }: Readonly<{ rows: PolicyYear[] }>) {
  return (
    <DataTable<PolicyYear>
      caption="Policy years"
      rows={rows}
      rowKey={(y) => y.year}
      columns={[
        { key: 'y', header: 'Policy Year', render: (y) => y.year },
        { key: 'p', header: 'Policy No.', render: (y) => y.policyNo ?? 'Policy number pending' },
        { key: 'f', header: 'From', render: (y) => formatDate(y.from) },
        { key: 't', header: 'To', render: (y) => formatDate(y.to) },
      ]}
    />
  );
}

function ItemsTable({ rows }: Readonly<{ rows: CoverItem[] }>) {
  return (
    <DataTable<CoverItem>
      caption="Risk items"
      rows={rows}
      rowKey={(i) => i.itemNo}
      emptyMessage="The cover has no risk items."
      columns={[
        { key: 'n', header: 'Item', render: (i) => i.itemNo },
        { key: 'k', header: 'Kind', render: (i) => humanize(i.kind) },
        { key: 'l', header: 'Description / Address', render: (i) => i.label },
        { key: 'c', header: 'City', render: (i) => i.city ?? '' },
        { key: 'p', header: 'Province', render: (i) => i.province ?? '' },
        {
          key: 's',
          header: 'Sum Insured',
          numeric: true,
          render: (i) => formatAmount(i.sumInsured),
        },
      ]}
    />
  );
}

function EndorsementsTable({ rows }: Readonly<{ rows: Endorsement[] }>) {
  return (
    <DataTable<Endorsement>
      caption="Endorsements"
      rows={rows}
      rowKey={(e) => e.endorsementNo}
      emptyMessage="No endorsement: the cover is at version 0."
      columns={[
        { key: 'n', header: 'Endorsement', render: (e) => e.endorsementNo },
        { key: 't', header: 'Type', render: (e) => humanize(e.type) },
        { key: 'y', header: 'Policy Year', render: (e) => e.policyYear },
        { key: 'd', header: 'Effective', render: (e) => formatDate(e.effectiveDate) },
        { key: 'i', header: 'Invoice', render: (e) => e.invoiceNo ?? '' },
        { key: 's', header: 'Description', render: (e) => e.description ?? '' },
      ]}
    />
  );
}

function InvoicesTable({ rows }: Readonly<{ rows: CoverInvoice[] }>) {
  return (
    <DataTable<CoverInvoice>
      caption="Invoices"
      rows={rows}
      rowKey={(i) => i.invoiceNo}
      emptyMessage="No invoice in the ledger for this cover."
      columns={[
        { key: 'n', header: 'Invoice', render: (i) => <span className="mono">{i.invoiceNo}</span> },
        { key: 'k', header: 'Kind', render: (i) => humanize(i.kind) },
        { key: 'y', header: 'Year', render: (i) => i.policyYear },
        {
          key: 'g',
          header: 'Gross Premium',
          numeric: true,
          render: (i) => `${i.currency} ${formatAmount(i.grossPremium)}`,
        },
        { key: 'b', header: 'Balance', numeric: true, render: (i) => formatAmount(i.balance) },
        {
          key: 'p',
          header: 'Payment',
          render: (i) => <StatusBadge status={i.cancelled ? 'CANCELLED' : i.paymentStatus} />,
        },
        { key: 'r', header: 'Remittance', render: (i) => humanize(i.remittanceStatus) },
        { key: 'd', header: 'DP', render: (i) => (i.directPayment ? 'Yes' : '') },
      ]}
    />
  );
}

function ClaimsTable({ rows }: Readonly<{ rows: CoverClaim[] }>) {
  const navigate = useNavigate();
  return (
    <DataTable<CoverClaim>
      caption="Claims of the cover"
      rows={rows}
      rowKey={(c) => c.id}
      onRowClick={(c) => void navigate(`/claims-handling/${c.id}`)}
      emptyMessage="No claim on this cover."
      columns={[
        { key: 'n', header: 'Claim No.', render: (c) => <span className="mono">{c.claimNo}</span> },
        { key: 'y', header: 'Year', render: (c) => c.policyYear },
        { key: 'l', header: 'Loss Date', render: (c) => formatDate(c.lossDate) },
        { key: 'r', header: 'Reported', render: (c) => formatDate(c.reportedDate) },
        {
          key: 's',
          header: 'Status',
          render: (c) => <StatusBadge status={c.statusCode ?? c.phase} />,
        },
      ]}
    />
  );
}

/** The insurer references of a cover, current and past (BRCLM.042). */
export function RefsTable({ rows }: Readonly<{ rows: LocationRef[] }>) {
  return (
    <DataTable<LocationRef>
      caption="Insurer location references"
      rows={rows}
      rowKey={(r) => r.id}
      emptyMessage="No insurer location reference yet."
      columns={[
        { key: 'a', header: 'ARN', render: (r) => r.arn },
        { key: 'i', header: 'Location Item', render: (r) => r.itemNo },
        { key: 'n', header: 'Insurer', render: (r) => r.insurerCode },
        { key: 'r', header: 'Insurer Reference', render: (r) => r.reference },
        { key: 'f', header: 'From', render: (r) => formatDate(r.effectiveFrom) },
        {
          key: 't',
          header: 'To',
          render: (r) =>
            r.effectiveTo ? formatDate(r.effectiveTo) : <StatusBadge status="CURRENT" />,
        },
      ]}
    />
  );
}

/** The read-only tabs of a cover (FR-CL-010). */
export function CoverTabs({ cover }: Readonly<{ cover: CoverDetail }>) {
  const [tab, setTab] = useState<TabId>('years');
  return (
    <Card>
      <div className="stack">
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'years' && <YearsTable rows={cover.years} />}
        {tab === 'items' && <ItemsTable rows={cover.items} />}
        {tab === 'endorsements' && <EndorsementsTable rows={cover.endorsements} />}
        {tab === 'invoices' && <InvoicesTable rows={cover.invoices} />}
        {tab === 'claims' && <ClaimsTable rows={cover.claims} />}
        {tab === 'refs' && <RefsTable rows={cover.locationRefs} />}
      </div>
    </Card>
  );
}
