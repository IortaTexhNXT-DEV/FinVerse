import { Plus, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ACCOUNT_STATUSES } from '@/api/accounts';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';
import { criteriaOf, EMPTY_PANEL as EMPTY, panelOf, QUICK_TABS } from './accountForm';
import type { QuickFilter, SearchPanelValues as Panel } from './accountForm';
import { AccountTable } from './AccountTable';

/** Business type filter values (BRNB.097, shared work item BT0). */
const BUSINESS_TYPE_OPTIONS = [
  { value: 'NEW_BUSINESS', label: 'New Business' },
  { value: 'RENEWAL', label: 'Renewal' },
];

/** The advanced filters of the account work list (BRNB.050 multi-criteria search). */
function FilterPanel({
  initial,
  onSearch,
}: Readonly<{ initial: Panel; onSearch: (p: Panel) => void }>) {
  const [panel, setPanel] = useState(initial);
  const set = (patch: Partial<Panel>) => setPanel((p) => ({ ...p, ...patch }));
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSearch(panel);
      }}
      className="worklist-filters stack"
    >
      <div className="form-grid">
        <TextInput label="PN Number" value={panel.pn} onChange={(pn) => set({ pn })} />
        <TextInput
          label="Plate, Conduction, Engine or Chassis"
          value={panel.vehicle}
          onChange={(vehicle) => set({ vehicle })}
        />
        <TextInput
          label="Location of Risk"
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
        <SelectInput
          label="Business Type"
          blank="New business and renewal"
          value={panel.businessType}
          options={BUSINESS_TYPE_OPTIONS}
          onChange={(businessType) => set({ businessType })}
        />
        <TextInput
          label="Starts On or After"
          type="date"
          value={panel.periodFrom}
          onChange={(periodFrom) => set({ periodFrom })}
        />
        <TextInput
          label="Starts On or Before"
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
          Include Voided
        </label>
        <div className="spacer" />
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            const cleared = { ...EMPTY, text: panel.text };
            setPanel(cleared);
            onSearch(cleared);
          }}
        >
          Clear Filters
        </Button>
        <Button type="submit" variant="secondary">
          Apply Filters
        </Button>
      </div>
    </form>
  );
}

/**
 * Accounts (BRNB.050), BDO work list: quick-filter tabs (all, my drafts, returned to me, awaiting
 * payment, FFY, direct payment), a search by ARN, client code or name, advanced filters (PN,
 * vehicle identifiers, location, product, insurer, status, period) and the paged list. A link
 * with `?status=` (NB dashboard drill-down) opens the list filtered on that status.
 */
export default function AccountsPage() {
  const { can } = useAuth();
  const [params] = useSearchParams();
  const [panel, setPanel] = useState<Panel>(() => panelOf(params.get('status')));
  const [filtersOpen, setFiltersOpen] = useState(params.get('status') !== null);
  const [quick, setQuick] = useState<QuickFilter>('all');
  const [page, setPage] = useState(0);
  const apply = (p: Panel) => {
    setPanel(p);
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
                  <Upload size={16} aria-hidden="true" /> Bulk Upload
                </Link>
              )}
              <Link className="btn btn-accent" to="/accounts/new">
                <Plus size={16} aria-hidden="true" /> New Account
              </Link>
            </>
          )
        }
      />
      <Card flush>
        <Tabs
          tabs={QUICK_TABS}
          active={quick}
          onChange={(q) => {
            setQuick(q);
            setPage(0);
          }}
        />
        <WorklistToolbar
          placeholder="Search ARN, client code or name"
          initial={panel.text}
          onSearch={(text) => apply({ ...panel, text })}
          filters={{ open: filtersOpen, onToggle: () => setFiltersOpen((o) => !o) }}
        />
        {filtersOpen && (
          <FilterPanel initial={panel} onSearch={(p) => apply({ ...p, text: panel.text })} />
        )}
        <AccountTable criteria={criteriaOf(panel, quick)} page={page} onPage={setPage} />
      </Card>
    </div>
  );
}
