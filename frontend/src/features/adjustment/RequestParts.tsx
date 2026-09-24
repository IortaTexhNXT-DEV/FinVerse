import { AlertTriangle, Info } from 'lucide-react';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { formatAmount } from '@/utils/format';
import type { ChangeView, Recompute, RequestStage, ShareView } from './api';
import { componentLabel, stageLabel, stageTone } from './requestForm';
import './adjustment.css';

/** Outlined status pill of a request stage (BDO: yellow review, blue in process, green done). */
export function RequestStatus({ stage }: Readonly<{ stage: RequestStage }>) {
  return <span className={`badge ${stageTone(stage)}`}>{stageLabel(stage)}</span>;
}

/** Record flags of a request as gold chips, apart from the status pill. */
export function RequestFlags({
  negative,
  quotationRequired,
  duplicateOverride,
}: Readonly<{ negative: boolean; quotationRequired: boolean; duplicateOverride: boolean }>) {
  const chips = [
    negative ? 'Negative Adjustment' : undefined,
    quotationRequired ? 'Quotation Required' : undefined,
    duplicateOverride ? 'Duplicate Override' : undefined,
  ].filter((c): c is string => c !== undefined);
  if (chips.length === 0) {
    return null;
  }
  return (
    <span className="tag-list">
      {chips.map((c) => (
        <span key={c} className="tag">
          {c}
        </span>
      ))}
    </span>
  );
}

const CHANGE_COLUMNS: Column<ChangeView>[] = [
  { key: 'component', header: 'Component', render: (c) => componentLabel(c.component) },
  { key: 'before', header: 'Before', numeric: true, render: (c) => <Amount value={c.before} /> },
  {
    key: 'delta',
    header: 'Change',
    numeric: true,
    render: (c) => <strong>{formatAmount(c.delta)}</strong>,
  },
  { key: 'after', header: 'After', numeric: true, render: (c) => <Amount value={c.after} /> },
];

/** Before / after per invoice component (ADJID.014/022). */
export function ChangesTable({ changes }: Readonly<{ changes: ChangeView[] }>) {
  const shown = changes.filter((c) => c.delta !== 0 || c.before !== 0);
  return (
    <DataTable
      caption="Before and after per component"
      columns={CHANGE_COLUMNS}
      rows={shown}
      rowKey={(c) => c.component}
      emptyMessage="No financial effect"
    />
  );
}

const SHARE_COLUMNS: Column<ShareView>[] = [
  {
    key: 'insurer',
    header: 'Insurer',
    render: (s) => (
      <>
        {s.insurerCode} {s.lead && <span className="tag">Lead</span>}
      </>
    ),
  },
  { key: 'share', header: 'Share', numeric: true, render: (s) => `${String(s.sharePct)}%` },
  {
    key: 'premium',
    header: 'Premium / Refund',
    numeric: true,
    render: (s) => <Amount value={s.premiumDelta} />,
  },
  {
    key: 'commission',
    header: 'Commission',
    numeric: true,
    render: (s) => <Amount value={s.commissionDelta} />,
  },
  { key: 'vat', header: 'VAT', numeric: true, render: (s) => <Amount value={s.vatDelta} /> },
];

/** Change per insurer share (ADJID.027). */
export function SharesTable({ shares }: Readonly<{ shares: ShareView[] }>) {
  return (
    <DataTable
      caption="Change per insurer"
      columns={SHARE_COLUMNS}
      rows={shares}
      rowKey={(s) => s.insurerCode}
      emptyMessage="No insurer change"
    />
  );
}

function serviceInvoiceText(r: Recompute): string | undefined {
  const si = r.serviceInvoice;
  if (si.action === 'NONE') {
    return undefined;
  }
  const verb =
    si.action === 'ISSUE'
      ? 'A service invoice will be issued'
      : 'The service invoice will be credited';
  return `${verb} for commission ${formatAmount(si.commission)} and VAT ${formatAmount(si.vat)} (ADJID.014).`;
}

/** What the request does to the service invoice, the payments and the remittance of the invoice. */
export function RecomputeNotes({ recompute }: Readonly<{ recompute: Recompute }>) {
  const notes: { text: string; warn: boolean }[] = [];
  const si = serviceInvoiceText(recompute);
  if (si !== undefined) {
    notes.push({ text: si, warn: false });
  }
  if (recompute.settlement.reapplication) {
    notes.push({
      text: 'The invoice is paid: its payments are re-applied by Cashiering and the excess becomes an unapplied item.',
      warn: true,
    });
  }
  if (recompute.settlement.arInsurer > 0) {
    notes.push({
      text: `Already remitted: ${formatAmount(recompute.settlement.arInsurer)} is set up as AR Insurer.`,
      warn: true,
    });
  }
  if (recompute.quotationRequired) {
    notes.push({
      text: 'The new sum insured exceeds the package limit: Marketing prepares a quotation (ADJID.008).',
      warn: true,
    });
  }
  if (notes.length === 0) {
    return null;
  }
  return (
    <ul className="adj-notes">
      {notes.map((n) => (
        <li key={n.text} className={n.warn ? 'adj-note-warn' : undefined}>
          {n.warn ? (
            <AlertTriangle size={16} aria-hidden="true" />
          ) : (
            <Info size={16} aria-hidden="true" />
          )}
          <span>{n.text}</span>
        </li>
      ))}
    </ul>
  );
}
