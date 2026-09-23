import { useState } from 'react';
import type { AccountClass, AccountLevel, GlAccountRequest, SubLedgerType } from '@/api/gl';
import { Field } from '@/components/ui/Field';

const CLASSES: AccountClass[] = ['ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE', 'MEMORANDUM'];
const LEVELS: AccountLevel[] = ['GROUP', 'MAIN', 'SUB', 'MICRO'];
const SUB_LEDGERS: SubLedgerType[] = [
  'NONE',
  'POLICYHOLDER',
  'INTERMEDIARY',
  'REINSURER',
  'COINSURER',
  'BANK',
  'VENDOR',
];

type Flag =
  | 'controlAccount'
  | 'allowManualPosting'
  | 'costCenterRequired'
  | 'businessLineRequired'
  | 'revaluationRequired'
  | 'reconcilable'
  | 'interBranch';

const FLAGS: { key: Flag; label: string }[] = [
  { key: 'allowManualPosting', label: 'Manual posting allowed' },
  { key: 'controlAccount', label: 'Control account (sub-ledger)' },
  { key: 'costCenterRequired', label: 'Cost centre mandatory' },
  { key: 'businessLineRequired', label: 'Line of business mandatory' },
  { key: 'revaluationRequired', label: 'FX revaluation' },
  { key: 'reconcilable', label: 'Reconcilable (nominal)' },
  { key: 'interBranch', label: 'Inter-branch account' },
];

interface Props {
  value: GlAccountRequest;
  editing: boolean;
  onChange: (value: GlAccountRequest) => void;
}

/** GL Heads maintenance form (Main / Sub / Micro GL with posting controls). */
export function AccountForm({ value, editing, onChange }: Readonly<Props>) {
  const [currencies, setCurrencies] = useState(value.allowedCurrencies.join(', '));
  const set = <K extends keyof GlAccountRequest>(key: K, v: GlAccountRequest[K]) =>
    onChange({ ...value, [key]: v });

  return (
    <div className="stack">
      <div className="form-grid">
        <Field label="Account code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={editing}
              value={value.code}
              onChange={(e) => set('code', e.target.value.toUpperCase())}
            />
          )}
        </Field>
        <Field label="Account name" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={value.name}
              onChange={(e) => set('name', e.target.value)}
            />
          )}
        </Field>
        <Field label="Short name">
          {(id) => (
            <input
              id={id}
              className="input"
              value={value.shortName ?? ''}
              onChange={(e) => set('shortName', e.target.value)}
            />
          )}
        </Field>
        <Field label="Class" required>
          {(id) => (
            <select
              id={id}
              className="select"
              disabled={editing}
              value={value.accountClass}
              onChange={(e) => set('accountClass', e.target.value as AccountClass)}
            >
              {CLASSES.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Tier" required>
          {(id) => (
            <select
              id={id}
              className="select"
              disabled={editing}
              value={value.level}
              onChange={(e) => set('level', e.target.value as AccountLevel)}
            >
              {LEVELS.map((l) => (
                <option key={l}>{l}</option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Parent account">
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={editing}
              value={value.parentCode ?? ''}
              onChange={(e) => set('parentCode', e.target.value)}
            />
          )}
        </Field>
        <Field label="Sub-ledger">
          {(id) => (
            <select
              id={id}
              className="select"
              value={value.subLedgerType}
              onChange={(e) => set('subLedgerType', e.target.value as SubLedgerType)}
            >
              {SUB_LEDGERS.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Statement line">
          {(id) => (
            <input
              id={id}
              className="input"
              value={value.reportGroup ?? ''}
              onChange={(e) => set('reportGroup', e.target.value)}
            />
          )}
        </Field>
        <Field label="Opened on" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              disabled={editing}
              value={value.openedOn}
              onChange={(e) => set('openedOn', e.target.value)}
            />
          )}
        </Field>
        <Field label="Allowed currencies" hint="Comma separated; blank = all">
          {(id) => (
            <input
              id={id}
              className="input"
              value={currencies}
              onChange={(e) => {
                setCurrencies(e.target.value);
                set(
                  'allowedCurrencies',
                  e.target.value
                    .split(',')
                    .map((c) => c.trim().toUpperCase())
                    .filter(Boolean),
                );
              }}
            />
          )}
        </Field>
      </div>
      <fieldset className="row" style={{ border: 'none', padding: 0 }}>
        <legend className="kpi-label">Posting controls</legend>
        {FLAGS.map((f) => (
          <label key={f.key} className="checkbox">
            <input
              type="checkbox"
              checked={value[f.key]}
              onChange={(e) => set(f.key, e.target.checked)}
            />
            {f.label}
          </label>
        ))}
      </fieldset>
    </div>
  );
}
