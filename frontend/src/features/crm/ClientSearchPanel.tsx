import { Search, X } from 'lucide-react';
import { useState } from 'react';
import type { ClientSearch } from '@/api/clients';
import type { ClientStatus, KycStatus } from '@/api/crm';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { humanize } from '@/utils/format';

export type SearchCriteria = Omit<ClientSearch, 'companyId' | 'page'>;

const STATUSES: ClientStatus[] = ['PROSPECT', 'CONFIRMED', 'INACTIVE'];
const KYC_STATUSES: KycStatus[] = ['NOT_STARTED', 'PENDING', 'VERIFIED', 'EXPIRED'];

interface TextCriterion {
  key: 'code' | 'name' | 'tin' | 'idNumber' | 'email' | 'mobile';
  label: string;
  placeholder: string;
}

const TEXT_CRITERIA: TextCriterion[] = [
  { key: 'code', label: 'Prospect or Client Code', placeholder: 'PR-2026- or CL-2026-' },
  { key: 'name', label: 'Name Contains', placeholder: 'e.g. Santos' },
  { key: 'tin', label: 'TIN', placeholder: '000-000-000-000' },
  { key: 'idNumber', label: 'ID Number', placeholder: 'Passport, UMID...' },
  { key: 'email', label: 'E-mail', placeholder: 'name@example.ph' },
  { key: 'mobile', label: 'Mobile', placeholder: '09xxxxxxxxx' },
];

const blank = (value: string) => (value.trim() === '' ? undefined : value.trim());

/**
 * Advanced filters of the client work list (BRNB.046 multi-criteria search): codes, name,
 * identifiers, status, KYC status, segment and bank relationship.
 */
export function ClientSearchPanel({
  value,
  onSearch,
}: Readonly<{ value: SearchCriteria; onSearch: (c: SearchCriteria) => void }>) {
  const [draft, setDraft] = useState<SearchCriteria>(value);
  const set = (patch: Partial<SearchCriteria>) => setDraft((d) => ({ ...d, ...patch }));
  return (
    <div className="worklist-filters">
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          onSearch(draft);
        }}
      >
        <div className="form-grid">
          {TEXT_CRITERIA.map((c) => (
            <Field key={c.key} label={c.label}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  placeholder={c.placeholder}
                  value={draft[c.key] ?? ''}
                  onChange={(e) => set({ [c.key]: blank(e.target.value) })}
                />
              )}
            </Field>
          ))}
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={draft.status ?? ''}
                onChange={(e) => set({ status: (e.target.value || undefined) as ClientStatus })}
              >
                <option value="">All</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="KYC Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={draft.kycStatus ?? ''}
                onChange={(e) => set({ kycStatus: (e.target.value || undefined) as KycStatus })}
              >
                <option value="">All</option>
                {KYC_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Market Segment">
            {(id) => (
              <LovSelect
                id={id}
                type="MARKET_SEGMENT"
                placeholder="All"
                value={draft.marketSegment ?? ''}
                onChange={(code) => set({ marketSegment: code || undefined })}
              />
            )}
          </Field>
          <Field label="BDO Bank Client">
            {(id) => (
              <select
                id={id}
                className="select"
                value={draft.bankClient === undefined ? '' : String(draft.bankClient)}
                onChange={(e) =>
                  set({ bankClient: e.target.value === '' ? undefined : e.target.value === 'true' })
                }
              >
                <option value="">All</option>
                <option value="true">Bank clients</option>
                <option value="false">Non-bank clients</option>
              </select>
            )}
          </Field>
        </div>
        <div className="row">
          <div className="spacer" />
          <Button
            variant="ghost"
            icon={<X size={16} />}
            onClick={() => {
              setDraft({});
              onSearch({});
            }}
          >
            Clear Filters
          </Button>
          <Button type="submit" variant="secondary" icon={<Search size={16} />}>
            Apply Filters
          </Button>
        </div>
      </form>
    </div>
  );
}
