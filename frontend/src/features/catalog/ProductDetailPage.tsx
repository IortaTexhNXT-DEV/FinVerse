import { useQuery } from '@tanstack/react-query';
import { CalendarDays, Layers, Package, Pencil, Percent, Plus, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { FieldRule, ProductDetail } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, humanize } from '@/utils/format';
import { DetailList } from './DetailList';
import type { DetailRow } from './DetailList';
import { ProductEditorModal } from './ProductEditorModal';
import { ProductVersionsTab } from './ProductVersionsTab';
import { productFormOf } from './productForm';
import { RecordActions } from './RecordActions';
import { RuleEditorModal } from './RuleEditorModal';
import type { RuleKind } from './RuleEditorModal';

const yes = (flag: boolean) => (flag ? 'Yes' : 'No');

const TABS = [
  { id: 'versions', label: 'Versions' },
  { id: 'features', label: 'Features' },
  { id: 'rules', label: 'Field Rules & Documents' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const PRODUCT_MAINTAINERS = ['MASTER_MAINTAIN', 'PRODUCT_MAINTAIN'] as const;
const PRODUCT_AUTHORIZERS = ['MASTER_AUTHORIZE', 'PRODUCT_AUTHORIZE'] as const;

function Attributes({ detail }: Readonly<{ detail: ProductDetail }>) {
  const p = detail.product;
  const rows: DetailRow[] = [
    ['Line', `${p.lineCode} – ${detail.lineName}`],
    ['Cover type', p.coverTypeCode ?? '–'],
    ['Risk items', humanize(detail.riskItemKind)],
    ['Rating', humanize(detail.ratingMethod)],
    ['Package', yes(p.packaged)],
    ['Package TSI limit', p.maxSumInsured ? <Amount value={p.maxSumInsured} /> : '–'],
    ['Fleet', yes(p.fleetCapable)],
    ['Market segments', p.marketSegments.length ? p.marketSegments.join(', ') : 'All'],
    ['Mortgage related', yes(p.mortgageApplicable)],
    ['Direct payment allowed', yes(p.directPaymentEligible)],
    ['Multi-year', p.multiYearAllowed ? `Up to ${p.maxTermYears} years` : 'No'],
    ['Free First Year', yes(p.ffyEligible)],
    ['Placement after', humanize(p.paymentGate)],
    ['Default rate %', p.defaultRate ?? '–'],
    ['Default commission %', p.defaultCommissionRate],
    ['Minimum premium', <Amount key="m" value={p.minimumPremium} />],
    ['TSU review', humanize(p.tsuInvolvement ?? 'BY_RULES')],
  ];
  return <DetailList rows={rows} />;
}

function FieldMatrix({ rules }: Readonly<{ rules: FieldRule[] }>) {
  return (
    <DataTable<FieldRule>
      rows={rules}
      rowKey={(r) => r.id}
      emptyMessage="No field rules apply."
      columns={[
        {
          key: 't',
          header: 'Level',
          render: (r) => (r.target === 'ITEM' ? 'Risk item' : 'Account'),
        },
        { key: 'l', header: 'Field', render: (r) => <strong>{r.label}</strong> },
        { key: 'k', header: 'Key', render: (r) => <code>{r.fieldKey}</code> },
        { key: 'r', header: 'Mandatory', render: (r) => (r.required ? 'Yes' : 'Optional') },
        {
          key: 'y',
          header: 'Check',
          render: (r) =>
            r.ruleType && r.ruleType !== 'REQUIRED' ? humanize(r.ruleType) : 'Presence',
        },
        {
          key: 's',
          header: 'From',
          render: (r) => (r.scope === 'ALL' ? 'All products' : `${humanize(r.scope)} rule`),
        },
      ]}
    />
  );
}

function Rules({ detail, maintain }: Readonly<{ detail: ProductDetail; maintain: boolean }>) {
  const [adding, setAdding] = useState<RuleKind | null>(null);
  const addButton = (kind: RuleKind) =>
    maintain && (
      <Button
        size="sm"
        variant="secondary"
        icon={<Plus size={14} />}
        onClick={() => setAdding(kind)}
      >
        Add Product Rule
      </Button>
    );
  return (
    <div className="stack">
      <Card title="Field matrix" flush actions={addButton('fields')}>
        <FieldMatrix rules={detail.fieldRules} />
      </Card>
      <Card title="Documents required before submission" actions={addButton('documents')}>
        {detail.requiredDocuments.length === 0 ? (
          <p className="muted">No document is required.</p>
        ) : (
          <ul>
            {detail.requiredDocuments.map((t) => (
              <li key={t}>{humanize(t)}</li>
            ))}
          </ul>
        )}
      </Card>
      {adding && (
        <RuleEditorModal
          kind={adding}
          scope={{ scope: 'PRODUCT', scopeCode: detail.product.code }}
          onClose={() => setAdding(null)}
        />
      )}
    </div>
  );
}

function Summary({ detail }: Readonly<{ detail: ProductDetail }>) {
  const p = detail.product;
  return (
    <RecordSummary
      title={p.name}
      chips={
        <>
          <ReferenceChip label="Risk code" value={p.code} />
          <StatusBadge status={p.lifecycleStatus ?? 'ACTIVE'} />
          <StatusBadge status={p.recordStatus} />
        </>
      }
      flags={
        (p.packaged || p.openVersionStatus) && (
          <>
            {p.packaged && <span className="tag">Package</span>}
            {p.openVersionStatus && (
              <span className="tag">
                Version {p.openVersionNo} {humanize(p.openVersionStatus)}
              </span>
            )}
          </>
        )
      }
      facts={[
        {
          icon: Layers,
          label: 'Line › Type',
          value: `${detail.lineName} › ${p.coverTypeCode ?? '–'}`,
        },
        {
          icon: Package,
          label: 'Current Version',
          value: p.currentVersionNo ? `v${p.currentVersionNo}` : '–',
        },
        { icon: Percent, label: 'Rate %', value: p.defaultRate ?? '–' },
        { icon: CalendarDays, label: 'Package End', value: formatDate(p.packageEndDate) },
        { icon: ShieldCheck, label: 'TSU Review', value: humanize(p.tsuInvolvement ?? 'BY_RULES') },
      ]}
    />
  );
}

/**
 * One product (BRNB.001, BRPM.006/007): summary with lifecycle and current version, the package
 * versions, the features, and the field matrix and documents an account needs.
 */
export default function ProductDetailPage() {
  const code = useParams().code ?? '';
  const { can } = useAuth();
  const [editing, setEditing] = useState(false);
  const [tab, setTab] = useState<TabId>('versions');
  const detail = useQuery({
    queryKey: ['catalog', 'product', code],
    queryFn: () => catalogApi.product(code),
  });
  if (detail.data === undefined) {
    return detail.error ? (
      <ErrorAlert error={detail.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = detail.data;
  const maintain = PRODUCT_MAINTAINERS.some((p) => can(p));
  return (
    <div className="stack">
      <PageHeader
        backTo="/catalog/products"
        section="Product Maintenance · Product"
        title={`${d.product.code} – ${d.product.name}`}
        description={
          d.product.packaged
            ? 'Package product: its commercial terms change through versions.'
            : 'Non-package product.'
        }
        actions={
          <>
            <RecordActions
              kind="PRODUCT"
              record={d.product}
              refresh={[['catalog']]}
              authorizers={PRODUCT_AUTHORIZERS}
              maintainers={PRODUCT_MAINTAINERS}
            />
            {maintain && (
              <Button
                variant="secondary"
                icon={<Pencil size={14} />}
                onClick={() => setEditing(true)}
              >
                Edit
              </Button>
            )}
          </>
        }
      />
      <Summary detail={d} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'versions' && <ProductVersionsTab product={d.product} />}
      {tab === 'features' && (
        <Card title="Product features">
          <Attributes detail={d} />
        </Card>
      )}
      {tab === 'rules' && <Rules detail={d} maintain={maintain} />}
      {editing && (
        <ProductEditorModal initial={productFormOf(d.product)} onClose={() => setEditing(false)} />
      )}
    </div>
  );
}
