import { Plus, Trash2 } from 'lucide-react';
import type { CoverageTerm, PackageTerms } from '@/api/productmaint';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import { InsurerChoices } from '@/features/proposals/ProposalFormParts';
import { newCoverage } from './packageRequest';

interface TermsEditorProps {
  terms: PackageTerms;
  onChange: (terms: PackageTerms) => void;
  errors: Record<string, string>;
  /** Hides the target insurers (retirement requests). */
  withInsurers?: boolean;
}

function SectionsCard({ terms, onChange }: Readonly<Omit<TermsEditorProps, 'errors'>>) {
  const sections = terms.sections;
  const set = (index: number, patch: { heading?: string; text?: string }) =>
    onChange({
      ...terms,
      sections: sections.map((s, i) => (i === index ? { ...s, ...patch } : s)),
    });
  return (
    <Card
      title="Requested terms"
      actions={
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange({ ...terms, sections: [...sections, { heading: '', text: '' }] })}
        >
          Add Section
        </Button>
      }
    >
      <div className="stack">
        {sections.map((s, index) => (
          <div key={`section-${index + 1}`} className="risk-section">
            <div className="row">
              <TextInput
                label="Heading"
                value={s.heading}
                onChange={(heading) => set(index, { heading })}
              />
              <div className="spacer" />
              <Button
                size="sm"
                variant="ghost"
                icon={<Trash2 size={14} />}
                aria-label={`Remove section ${index + 1}`}
                onClick={() =>
                  onChange({ ...terms, sections: sections.filter((_, i) => i !== index) })
                }
              />
            </div>
            <Field label="Details">
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  rows={3}
                  maxLength={4000}
                  value={s.text ?? ''}
                  onChange={(e) => set(index, { text: e.target.value })}
                />
              )}
            </Field>
          </div>
        ))}
      </div>
    </Card>
  );
}

function CoverageRow({
  c,
  index,
  onChange,
  onRemove,
}: Readonly<{
  c: CoverageTerm;
  index: number;
  onChange: (patch: Partial<CoverageTerm>) => void;
  onRemove: () => void;
}>) {
  const n = index + 1;
  return (
    <tr>
      <td>
        <input
          className="input"
          aria-label={`Coverage ${n} code`}
          value={c.coverageCode}
          onChange={(e) => onChange({ coverageCode: e.target.value.toUpperCase() })}
        />
      </td>
      <td>
        <input
          type="checkbox"
          aria-label={`Coverage ${n} included`}
          checked={c.included}
          onChange={(e) => onChange({ included: e.target.checked })}
        />
      </td>
      <td>
        <input
          className="input num"
          type="number"
          aria-label={`Coverage ${n} limit`}
          value={c.limitAmount ?? ''}
          onChange={(e) =>
            onChange({ limitAmount: e.target.value === '' ? undefined : Number(e.target.value) })
          }
        />
      </td>
      <td>
        <input
          className="input"
          aria-label={`Coverage ${n} deductible`}
          value={c.deductibleText ?? ''}
          onChange={(e) => onChange({ deductibleText: e.target.value || undefined })}
        />
      </td>
      <td>
        <Button
          size="sm"
          variant="ghost"
          icon={<Trash2 size={14} />}
          aria-label={`Remove coverage ${n}`}
          onClick={onRemove}
        />
      </td>
    </tr>
  );
}

function CoveragesCard({ terms, onChange, errors }: Readonly<TermsEditorProps>) {
  const coverages = terms.coverages;
  const set = (index: number, patch: Partial<CoverageTerm>) =>
    onChange({
      ...terms,
      coverages: coverages.map((c, i) => (i === index ? { ...c, ...patch } : c)),
    });
  return (
    <Card
      title="Coverages and perils"
      actions={
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange({ ...terms, coverages: [...coverages, newCoverage()] })}
        >
          Add Coverage
        </Button>
      }
    >
      {errors.coverages && (
        <p className="field-error" role="alert">
          {errors.coverages}
        </p>
      )}
      {coverages.length === 0 ? (
        <p className="muted">
          No coverage yet. Add the coverages, limits and deductibles asked for.
        </p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Coverage code</th>
                <th>Included</th>
                <th className="num">Limit</th>
                <th>Deductible</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {coverages.map((c, index) => (
                <CoverageRow
                  key={`coverage-${index + 1}`}
                  c={c}
                  index={index}
                  onChange={(patch) => set(index, patch)}
                  onRemove={() =>
                    onChange({ ...terms, coverages: coverages.filter((_, i) => i !== index) })
                  }
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function SchemeCard({ terms, onChange, errors }: Readonly<TermsEditorProps>) {
  const s = terms.scheme;
  const d = terms.dates;
  const scheme = (patch: Partial<PackageTerms['scheme']>) =>
    onChange({ ...terms, scheme: { ...s, ...patch } });
  const dates = (patch: Partial<PackageTerms['dates']>) =>
    onChange({ ...terms, dates: { ...d, ...patch } });
  const date = (v: string) => (v === '' ? undefined : v);
  return (
    <Card title="Rate scheme and package term">
      <div className="form-grid">
        <NumberInput
          label="Requested rate %"
          value={s.defaultRate}
          onChange={(defaultRate) => scheme({ defaultRate })}
          step="0.0001"
        />
        <NumberInput
          label="Minimum premium"
          value={s.minimumPremium}
          onChange={(minimumPremium) => scheme({ minimumPremium })}
        />
        <NumberInput
          label="Commission %"
          value={s.commissionRate}
          onChange={(commissionRate) => scheme({ commissionRate })}
        />
        <NumberInput
          label="Package TSI limit"
          value={s.maxSumInsured}
          onChange={(maxSumInsured) => scheme({ maxSumInsured })}
        />
        <TextInput
          label="Effective from"
          type="date"
          value={d.effectiveFrom}
          onChange={(v) => dates({ effectiveFrom: date(v) })}
        />
        <TextInput
          label="Package start"
          type="date"
          value={d.packageStartDate}
          onChange={(v) => dates({ packageStartDate: date(v) })}
        />
        <TextInput
          label="Package end"
          type="date"
          error={errors.packageEndDate}
          hint="Monitored for expiry and renewal (BRPM.017)."
          value={d.packageEndDate}
          onChange={(v) => dates({ packageEndDate: date(v) })}
        />
        <TextInput
          label="Anniversary"
          type="date"
          value={d.anniversaryDate}
          onChange={(v) => dates({ anniversaryDate: date(v) })}
        />
      </div>
    </Card>
  );
}

/**
 * The requested terms of a package request (BRPM.008, PMADD01/02): free-form sections, coverages
 * with limits and deductibles, the requested rate scheme and package term, and the target
 * insurers of the negotiation.
 */
export function TermsEditor({
  terms,
  onChange,
  errors,
  withInsurers = true,
}: Readonly<TermsEditorProps>) {
  return (
    <>
      <SectionsCard terms={terms} onChange={onChange} />
      <CoveragesCard terms={terms} onChange={onChange} errors={errors} />
      <SchemeCard terms={terms} onChange={onChange} errors={errors} />
      {withInsurers && (
        <Card title="Target insurers">
          <InsurerChoices
            selected={terms.insurers.map((i) => i.insurerCode)}
            onChange={(codes) =>
              onChange({
                ...terms,
                insurers: codes.map(
                  (code) =>
                    terms.insurers.find((i) => i.insurerCode === code) ?? {
                      insurerCode: code,
                      terms: [],
                    },
                ),
              })
            }
          />
        </Card>
      )}
    </>
  );
}
