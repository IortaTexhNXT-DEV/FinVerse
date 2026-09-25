import type {
  InvoiceComponentRow,
  InvoiceFlags,
  LedgerComponent,
  OpsInvoice,
  OpsSection,
  RelatedItem,
  RelatedSection,
  Severity,
} from '@/api/operations';

/** Display names of the Operations home sections, in display order (BRQID.003). */
export const SECTION_LABELS: Record<OpsSection, string> = {
  CASHIERING: 'Cashiering',
  REMITTANCE: 'Remittance',
  PRODRECON: 'Production Reconciliation',
  ADJUSTMENT: 'Adjustment',
  COMMISSION: 'Commission Receivables',
  DISBURSEMENT: 'Disbursement',
  INTERFACES: 'Interfaces',
};

/** Where each section's own workbench lives (O1 modules fill them in). */
export const SECTION_ROUTES: Record<OpsSection, string> = {
  CASHIERING: '/cashiering',
  REMITTANCE: '/remittance',
  PRODRECON: '/prodrecon',
  ADJUSTMENT: '/adjustment',
  COMMISSION: '/commission',
  DISBURSEMENT: '/operations/disbursements',
  INTERFACES: '/operations/interfaces',
};

const COMPONENT_LABELS: Record<LedgerComponent, string> = {
  BASIC: 'PR - Basic Premium',
  DST: 'PR - DST',
  PREMIUM_TAX_VAT: 'PR - Premium Tax / VAT',
  LGT: 'PR - LGT',
  FST: 'PR - Fire Service Tax',
  OTHER: 'PR - Other Charges',
  DTIP: 'Due to Insurer (DTIP)',
  COMMISSION: 'Commission Receivable',
  COMMISSION_VAT: 'VAT on Commission',
  WTAX: 'Withholding Tax',
  PR2307: 'PR - CWT 2307',
};

/** Order of the components on the invoice 360 (CSHID.022 hierarchy first). */
const COMPONENT_ORDER: LedgerComponent[] = [
  'DST',
  'PREMIUM_TAX_VAT',
  'LGT',
  'FST',
  'OTHER',
  'BASIC',
  'PR2307',
  'DTIP',
  'COMMISSION',
  'COMMISSION_VAT',
  'WTAX',
];

/** Label of an invoice component. */
export function componentLabel(component: LedgerComponent): string {
  return COMPONENT_LABELS[component];
}

/** Components in display order, without the rows that never moved (all zero). */
export function visibleComponents(rows: InvoiceComponentRow[]): InvoiceComponentRow[] {
  const moved = (r: InvoiceComponentRow) =>
    [r.booked, r.applied, r.reversed, r.remitted, r.adjusted, r.writtenOff, r.balance].some(
      (v) => v !== 0,
    );
  return [...rows]
    .filter(moved)
    .sort((a, b) => COMPONENT_ORDER.indexOf(a.component) - COMPONENT_ORDER.indexOf(b.component));
}

/** Totals of the premium receivable components. */
export function premiumTotals(rows: InvoiceComponentRow[]): {
  booked: number;
  applied: number;
  balance: number;
} {
  return rows
    .filter((r) => r.premiumReceivable)
    .reduce(
      (t, r) => ({
        booked: t.booked + r.booked + r.adjusted,
        applied: t.applied + r.applied - r.reversed,
        balance: t.balance + r.balance,
      }),
      { booked: 0, applied: 0, balance: 0 },
    );
}

/** The flag chips of an invoice (record flags go beside, never inside, the status pill). */
export function flagChips(flags: InvoiceFlags): string[] {
  const chips: [boolean, string][] = [
    [flags.directPayment, 'Direct Payment'],
    [flags.cwt2Percent, '2% CWT'],
    [flags.incentiveEligible, 'Incentive'],
    [flags.hold, 'On Hold'],
    [flags.pendingNegativeAdjustment, 'Pending Negative Adjustment'],
    [flags.writtenOff, 'Written Off'],
    [flags.cancelled, 'Cancelled'],
    [flags.estimated, 'Estimated'],
    [Boolean(flags.lockOwner), `Locked by ${flags.lockOwner ?? ''}`],
  ];
  return chips.filter(([on]) => on).map(([, label]) => label);
}

/** CSS modifier of a work count tile. */
export function tileTone(count: number, severity: Severity): string {
  if (count === 0) {
    return '';
  }
  switch (severity) {
    case 'ALERT':
      return 'ops-tile-alert';
    case 'WARNING':
      return 'ops-tile-warning';
    default:
      return '';
  }
}

/** An external link's address when it is a web address; anything else is shown as text only. */
export function safeUrl(url: string | undefined): string | undefined {
  return url !== undefined && /^https?:\/\//i.test(url) ? url : undefined;
}

/** Tabs of Invoice 360 that list the records of one Operations module. */
export const RELATED_TABS = [
  { id: 'receipts', section: 'RECEIPTS', label: 'Receipts' },
  { id: 'remittances', section: 'REMITTANCES', label: 'Remittances' },
  { id: 'adjustments', section: 'ADJUSTMENTS', label: 'Adjustments' },
  { id: 'reconciliation', section: 'RECONCILIATION', label: 'Reconciliation' },
  { id: 'commission', section: 'COMMISSION', label: 'Commission' },
] as const satisfies readonly { id: string; section: RelatedSection; label: string }[];

/** Id of an Invoice 360 tab. */
export type Invoice360TabId =
  | 'components'
  | 'movements'
  | 'family'
  | (typeof RELATED_TABS)[number]['id']
  | 'history'
  | 'documents';

/**
 * The Invoice 360 tabs in display order, each module tab labelled with its record count so the user
 * sees at a glance where the invoice has been (RMTID.026/038, ADJID.024).
 */
export function invoiceTabs(
  related: Partial<Record<RelatedSection, RelatedItem[]>>,
): { id: Invoice360TabId; label: string }[] {
  const counted = (label: string, section: RelatedSection) => {
    const n = related[section]?.length ?? 0;
    return n === 0 ? label : `${label} (${String(n)})`;
  };
  return [
    { id: 'components', label: 'Components & Balances' },
    { id: 'movements', label: 'Movements' },
    { id: 'family', label: 'Invoice Family' },
    ...RELATED_TABS.map((t) => ({ id: t.id, label: counted(t.label, t.section) })),
    { id: 'history', label: 'History' },
    { id: 'documents', label: counted('Documents', 'DOCUMENTS') },
  ];
}

/**
 * Totals of an invoice family (DIS 3.27.2, ACSL 2.16.0): the gross premium and the outstanding
 * premium of the original booking with its endorsements and cancellation.
 */
export function familyTotals(
  invoices: readonly Pick<OpsInvoice, 'grossPremium' | 'premiumBalance'>[],
): {
  gross: number;
  outstanding: number;
} {
  const round = (n: number) => Math.round(n * 100) / 100;
  return invoices.reduce(
    (t, i) => ({
      gross: round(t.gross + i.grossPremium),
      outstanding: round(t.outstanding + i.premiumBalance),
    }),
    { gross: 0, outstanding: 0 },
  );
}
