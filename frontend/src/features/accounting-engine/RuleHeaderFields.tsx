import type { EventType, RuleInput } from '@/api/accounting';
import { Field } from '@/components/ui/Field';
import { useGlLookups } from '@/features/gl/useLookups';
import { components } from './ruleModel';

interface Props {
  rule: RuleInput;
  eventTypes: EventType[];
  lockEventType: boolean;
  readOnly: boolean;
  onChange: (rule: RuleInput) => void;
}

/** Rule header: event type, name and the selection conditions (line of business, currency, dates). */
export function RuleHeaderFields({
  rule,
  eventTypes,
  lockEventType,
  readOnly,
  onChange,
}: Readonly<Props>) {
  const { businessLines, currencies } = useGlLookups();
  const set = (patch: Partial<RuleInput>) => onChange({ ...rule, ...patch });
  const changeEventType = (code: string) => {
    const first = components(eventTypes.find((t) => t.code === code))[0] ?? '';
    set({ eventType: code, lines: rule.lines.map((l) => ({ ...l, amountComponent: first })) });
  };

  return (
    <div className="form-grid">
      <Field label="Event type" required>
        {(id) => (
          <select
            id={id}
            className="select"
            disabled={lockEventType || readOnly}
            value={rule.eventType}
            onChange={(e) => changeEventType(e.target.value)}
          >
            {eventTypes.map((t) => (
              <option key={t.code} value={t.code}>
                {t.code} – {t.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Rule name" required>
        {(id) => (
          <input
            id={id}
            className="input"
            disabled={readOnly}
            value={rule.name}
            onChange={(e) => set({ name: e.target.value })}
          />
        )}
      </Field>
      <Field label="Line of business" hint="Blank = any line of business">
        {(id) => (
          <select
            id={id}
            className="select"
            disabled={readOnly}
            value={rule.businessLine ?? ''}
            onChange={(e) => set({ businessLine: e.target.value || undefined })}
          >
            <option value="">Any</option>
            {businessLines.map((b) => (
              <option key={b.code} value={b.code}>
                {b.code} – {b.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Currency" hint="Blank = any currency">
        {(id) => (
          <select
            id={id}
            className="select"
            disabled={readOnly}
            value={rule.currency ?? ''}
            onChange={(e) => set({ currency: e.target.value || undefined })}
          >
            <option value="">Any</option>
            {currencies.map((c) => (
              <option key={c.code} value={c.code}>
                {c.code}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Priority" hint="Lower number wins when several rules match">
        {(id) => (
          <input
            id={id}
            className="input"
            type="number"
            min={1}
            disabled={readOnly}
            value={rule.priority}
            onChange={(e) => set({ priority: Number(e.target.value) })}
          />
        )}
      </Field>
      <Field label="Effective from" required>
        {(id) => (
          <input
            id={id}
            className="input"
            type="date"
            disabled={readOnly}
            value={rule.effectiveFrom}
            onChange={(e) => set({ effectiveFrom: e.target.value })}
          />
        )}
      </Field>
      <Field label="Effective to">
        {(id) => (
          <input
            id={id}
            className="input"
            type="date"
            disabled={readOnly}
            value={rule.effectiveTo ?? ''}
            onChange={(e) => set({ effectiveTo: e.target.value || undefined })}
          />
        )}
      </Field>
    </div>
  );
}
