import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, CircleAlert, Plus, Trash2 } from 'lucide-react';
import { catalogApi } from '@/api/catalog';
import { proposalsApi } from '@/api/proposals';
import type { RiskSection } from '@/api/proposals';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';
import { TextInput } from '@/features/assets/FormControls';
import { humanize } from '@/utils/format';
import { toggleInsurer } from './proposalForm';

/** Free-form sections of the risk details (BRNB.005). */
export function SectionsCard({
  sections,
  onChange,
}: Readonly<{ sections: RiskSection[]; onChange: (s: RiskSection[]) => void }>) {
  const set = (index: number, patch: Partial<RiskSection>) =>
    onChange(sections.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  return (
    <Card
      title="Risk details"
      actions={
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange([...sections, { heading: '', text: '' }])}
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
                onClick={() => onChange(sections.filter((_, i) => i !== index))}
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

/** Panel insurers to approach (requested by Marketing, confirmed by TSU). */
export function InsurerChoices({
  selected,
  onChange,
  disabled = false,
}: Readonly<{ selected: string[]; onChange: (codes: string[]) => void; disabled?: boolean }>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="insurer-choices" role="group" aria-label="Insurers">
      {(insurers.data ?? [])
        .filter((i) => i.recordStatus === 'ACTIVE')
        .map((i) => (
          <label key={i.partyCode} className="checkbox">
            <input
              type="checkbox"
              disabled={disabled}
              checked={selected.includes(i.partyCode)}
              onChange={() => onChange(toggleInsurer(selected, i.partyCode))}
            />
            {i.name}
          </label>
        ))}
    </div>
  );
}

/** The mandatory documents of the product and whether they are attached (BRNB.005). */
export function DocumentChecklist({ proposalId }: Readonly<{ proposalId: number }>) {
  const checklist = useQuery({
    queryKey: ['proposal', proposalId, 'checklist'],
    queryFn: () => proposalsApi.checklist(proposalId),
  });
  const items = checklist.data ?? [];
  return (
    <Card title="Mandatory documents">
      {items.length === 0 ? (
        <p className="muted">No mandatory document for this product.</p>
      ) : (
        <ul className="checklist">
          {items.map((c) => (
            <li key={c.documentType} className="checklist-item">
              {c.attached ? (
                <CheckCircle2 size={16} className="text-success" aria-label="Attached" />
              ) : (
                <CircleAlert size={16} className="text-danger" aria-label="Missing" />
              )}{' '}
              {humanize(c.documentType)}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
