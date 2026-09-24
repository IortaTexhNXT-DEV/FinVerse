import { useQuery } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { FieldRule, ProductDetail } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { humanize } from '@/utils/format';
import { DetailList } from './DetailList';
import type { DetailRow } from './DetailList';
import { ProductEditorModal } from './ProductEditorModal';
import { productFormOf } from './productForm';
import { RecordActions } from './RecordActions';
import { RuleEditorModal } from './RuleEditorModal';
import type { RuleKind } from './RuleEditorModal';

const yes = (flag: boolean) => (flag ? 'Yes' : 'No');

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
          key: 's',
          header: 'From',
          render: (r) => (r.scope === 'ALL' ? 'All products' : `${humanize(r.scope)} rule`),
        },
      ]}
    />
  );
}

/**
 * One product: its features, the field matrix (data an account needs, from rules for all
 * products, its line and itself) and the documents required before submission.
 */
export default function ProductDetailPage() {
  const code = useParams().code ?? '';
  const { can } = useAuth();
  const [editing, setEditing] = useState(false);
  const [adding, setAdding] = useState<RuleKind | null>(null);
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
  const maintain = can('MASTER_MAINTAIN');
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
      <PageHeader
        section="Products & Insurers · Product"
        title={`${d.product.code} – ${d.product.name}`}
        actions={
          <>
            <StatusBadge status={d.product.recordStatus} />
            <RecordActions kind="PRODUCT" record={d.product} refresh={[['catalog']]} />
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
      <Card title="Product features">
        <Attributes detail={d} />
      </Card>
      <Card title="Field matrix" flush actions={addButton('fields')}>
        <FieldMatrix rules={d.fieldRules} />
      </Card>
      <Card title="Documents required before submission" actions={addButton('documents')}>
        {d.requiredDocuments.length === 0 ? (
          <p className="muted">No document is required.</p>
        ) : (
          <ul>
            {d.requiredDocuments.map((t) => (
              <li key={t}>{humanize(t)}</li>
            ))}
          </ul>
        )}
      </Card>
      {editing && (
        <ProductEditorModal initial={productFormOf(d.product)} onClose={() => setEditing(false)} />
      )}
      {adding && (
        <RuleEditorModal
          kind={adding}
          scope={{ scope: 'PRODUCT', scopeCode: d.product.code }}
          onClose={() => setAdding(null)}
        />
      )}
    </div>
  );
}
