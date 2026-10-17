import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { SoaFilters } from '@/api/ebMarket';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { EB_SECTION } from '../EbPlaceholder';
import '../eb.css';
import { SOA_STATUSES } from '../common/ebStatuses';
import { InsurerSelect } from '../common/EbSelects';
import { SoaTable } from './SoaTable';

/**
 * SOA Register (design 10.1): the insurer statements of account received on the programmes,
 * validated by Processing and released to the client and Collection, with the payment status of
 * the linked invoices. SOAs are received on the programme's Billing & SOA tab.
 */
export default function SoaRegisterPage() {
  const [params] = useSearchParams();
  const soa = Number(params.get('soa') ?? '');
  const [filters, setFilters] = useState<SoaFilters>({ status: params.get('status') ?? undefined });
  const [showFilters, setShowFilters] = useState(true);
  const set = (next: Partial<SoaFilters>) => setFilters((f) => ({ ...f, ...next }));
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="SOA Register"
        description="Insurer statements of account received, validated by Processing and released to the client and Collection."
      />
      <Card flush>
        <WorklistToolbar
          placeholder="Search SOA, programme or client"
          onSearch={(q) => set({ q })}
          filters={{ open: showFilters, onToggle: () => setShowFilters((f) => !f) }}
        />
        {showFilters && (
          <div className="worklist-filters form-grid">
            <Field label="Status">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={filters.status ?? ''}
                  onChange={(e) => set({ status: e.target.value || undefined })}
                >
                  <option value="">All statuses</option>
                  {SOA_STATUSES.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Insurer">
              {(id) => (
                <InsurerSelect
                  id={id}
                  value={filters.insurer ?? ''}
                  placeholder="All insurers"
                  onChange={(v) => set({ insurer: v || undefined })}
                />
              )}
            </Field>
          </div>
        )}
        <SoaTable filters={filters} showProgramme openId={soa > 0 ? soa : undefined} />
      </Card>
    </div>
  );
}
