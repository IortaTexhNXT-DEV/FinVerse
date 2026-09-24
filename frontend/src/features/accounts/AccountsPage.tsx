import { Plus, Search, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ACCOUNT_STATUSES } from '@/api/accounts';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';
import { criteriaOf, EMPTY_PANEL as EMPTY, QUICK_FILTERS } from './accountForm';
import type { QuickFilter, SearchPanelValues as Panel } from './accountForm';
import { AccountTable } from './AccountTable';

function SearchPanel({ onSearch }: Readonly<{ onSearch: (p: Panel) => void }>) {
  const [panel, setPanel] = useState(EMPTY);
  const set = (patch: Partial<Panel>) => setPanel((p) => ({ ...p, ...patch }));
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSearch(panel);
      }}
      className="stack"
      style={{ padding: 'var(--space-4)' }}
    >
      <div className="form-grid">
        <TextInput
          label="ARN, client code or name"
          value={panel.text}
          onChange={(text) => set({ text })}
        />
        <TextInput label="PN number" value={panel.pn} onChange={(pn) => set({ pn })} />
        <TextInput
          label="Plate, conduction, engine or chassis"
          value={panel.vehicle}
          onChange={(vehicle) => set({ vehicle })}
        />
        <TextInput
          label="Location of risk"
          value={panel.location}
          onChange={(location) => set({ location })}
        />
        <TextInput
          label="Product"
          upper
          value={panel.product}
          onChange={(product) => set({ product })}
        />
        <TextInput
          label="Insurer"
          upper
          value={panel.insurer}
          onChange={(insurer) => set({ insurer })}
        />
        <SelectInput
          label="Status"
          blank="Any status"
          value={panel.status}
          options={enumOptions(ACCOUNT_STATUSES)}
          onChange={(status) => set({ status })}
        />
        <TextInput
          label="Starts on or after"
          type="date"
          value={panel.periodFrom}
          onChange={(periodFrom) => set({ periodFrom })}
        />
        <TextInput
          label="Starts on or before"
          type="date"
          value={panel.periodTo}
          onChange={(periodTo) => set({ periodTo })}
        />
      </div>
      <div className="row">
        <label className="checkbox">
          <input
            type="checkbox"
            checked={panel.includeVoided}
            onChange={(e) => set({ includeVoided: e.target.checked })}
          />
          Include voided
        </label>
        <div className="spacer" />
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            setPanel(EMPTY);
            onSearch(EMPTY);
          }}
        >
          Clear
        </Button>
        <Button type="submit" variant="secondary" icon={<Search size={14} />}>
          Search
        </Button>
      </div>
    </form>
  );
}

/**
 * Accounts (BRNB.050): search by ARN, client, PN, vehicle identifiers or location, quick filters
 * for the user's drafts and returned accounts, awaiting payment, FFY and direct payment.
 */
export default function AccountsPage() {
  const { can } = useAuth();
  const [panel, setPanel] = useState(EMPTY);
  const [quick, setQuick] = useState<QuickFilter>('all');
  const [page, setPage] = useState(0);
  const choose = (q: QuickFilter) => {
    setQuick(q);
    setPage(0);
  };
  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement"
        title="Accounts"
        description="New Business accounts from draft to booking."
        actions={
          can('ACCOUNT_MAINTAIN') && (
            <>
              {can('BULK_PROCESS') && (
                <Link className="btn btn-secondary" to="/bulk/ACCOUNT_CREATE">
                  <Upload size={16} aria-hidden="true" /> Bulk upload
                </Link>
              )}
              <Link className="btn btn-accent" to="/accounts/new">
                <Plus size={16} aria-hidden="true" /> New account
              </Link>
            </>
          )
        }
      />
      <div className="row" role="group" aria-label="Quick filters">
        {(Object.keys(QUICK_FILTERS) as QuickFilter[]).map((q) => (
          <Button
            key={q}
            size="sm"
            variant={quick === q ? 'primary' : 'secondary'}
            aria-pressed={quick === q}
            onClick={() => choose(q)}
          >
            {QUICK_FILTERS[q].label}
          </Button>
        ))}
      </div>
      <Card flush>
        <SearchPanel
          onSearch={(p) => {
            setPanel(p);
            setPage(0);
          }}
        />
        <AccountTable criteria={criteriaOf(panel, quick)} page={page} onPage={setPage} />
      </Card>
    </div>
  );
}
