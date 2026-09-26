import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { formatAmount, humanize } from '@/utils/format';
import type { Recompute } from './api';
import { ChangesTable, RecomputeNotes, SharesTable } from './RequestParts';
import type { RequestForm } from './requestForm';

interface PreviewProps {
  recompute: Recompute;
  /** The form, to enter the duplicate and baseline justifications; omit for a read-only view. */
  form?: RequestForm;
  onChange?: (form: RequestForm) => void;
}

function Justifications({
  recompute,
  form,
  onChange,
}: Readonly<{ recompute: Recompute; form: RequestForm; onChange: (f: RequestForm) => void }>) {
  const exceeded = recompute.baseline?.exceeded ?? false;
  if (recompute.duplicates.length === 0 && !exceeded) {
    return null;
  }
  return (
    <div className="alert warning" role="alert">
      <div className="stack">
        {recompute.duplicates.length > 0 && (
          <Field
            label={`Possible duplicate of ${recompute.duplicates.join(', ')}`}
            required
            hint="Same invoice, request type, reason and endorsement reference (ADJID.023). Give the justification to proceed."
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={500}
                value={form.duplicateOverride}
                onChange={(e) => onChange({ ...form, duplicateOverride: e.target.value })}
              />
            )}
          </Field>
        )}
        {exceeded && recompute.baseline && (
          <Field
            label={`Cumulative adjustments ${formatAmount(recompute.baseline.adjustedAfter)} exceed ${String(recompute.baseline.limitPercent)}% of the original premium ${formatAmount(recompute.baseline.originalPremium)}`}
            required
            hint="Review the previous adjustments of the invoice (ADJID.028) and justify the request."
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={500}
                value={form.baselineOverride}
                onChange={(e) => onChange({ ...form, baselineOverride: e.target.value })}
              />
            )}
          </Field>
        )}
      </div>
    </div>
  );
}

/**
 * Recompute of a request (ADJID.014/027/028): the premium, commission and VAT changes, the before /
 * after of every component, the change per insurer, the service invoice, payment and remittance
 * effects, and the duplicate and baseline justifications.
 */
export function RecomputePreview({ recompute, form, onChange }: Readonly<PreviewProps>) {
  return (
    <div className="stack">
      <div className="grid-4">
        <Kpi label="Class" value={humanize(recompute.requestClass)} />
        <Kpi label="Premium Change" value={formatAmount(recompute.premiumChange)} accent />
        <Kpi label="Commission Change" value={formatAmount(recompute.commissionChange)} />
        <Kpi label="VAT on Commission" value={formatAmount(recompute.vatChange)} />
      </div>
      <RecomputeNotes recompute={recompute} />
      {form && onChange && <Justifications recompute={recompute} form={form} onChange={onChange} />}
      <Card title="Before and After">
        <ChangesTable changes={recompute.changes} />
      </Card>
      {recompute.shares.length > 0 && (
        <Card title="Per Insurer">
          <SharesTable shares={recompute.shares} />
        </Card>
      )}
    </div>
  );
}
