import { useState } from 'react';
import type { CompanyUnit, DataScopeValue } from '@/api/dataScope';
import type { Column } from '@/components/ui/DataTable';
import { TreeTable } from '@/components/ui/TreeTable';
import {
  accessNodes,
  allBranchesGranted,
  branchGranted,
  companyGranted,
  openedCompanies,
  setAllBranches,
  setAllCompanies,
  toggleBranch,
  toggleCompany,
} from './dataAccess';
import type { AccessRow } from './dataAccess';

interface DataAccessSectionProps {
  /** Companies and branches the viewer may grant. */
  units: readonly CompanyUnit[];
  scope: DataScopeValue;
  /** Change of the scope; absent for a read-only section. */
  onChange?: (scope: DataScopeValue) => void;
  loading?: boolean;
  /** Accessible name of the tree. */
  caption?: string;
}

function nameOf(row: AccessRow) {
  const unit = row.kind === 'company' ? row.company : row.branch;
  return (
    <span>
      {unit.name}
      {unit.code !== '' && <span className="muted"> · {unit.code}</span>}
    </span>
  );
}

function grantedOf(scope: DataScopeValue, row: AccessRow): boolean {
  return row.kind === 'company'
    ? companyGranted(scope, row.company.id)
    : branchGranted(scope, row.company.id, row.branch.id);
}

function accessColumn(
  scope: DataScopeValue,
  onChange: ((scope: DataScopeValue) => void) | undefined,
): Column<AccessRow> {
  return {
    key: 'access',
    header: 'Access',
    width: '120px',
    render: (row) => {
      const granted = grantedOf(scope, row);
      if (onChange === undefined) {
        return <span>{granted ? 'Yes' : 'No'}</span>;
      }
      const label = row.kind === 'company' ? row.company.name : row.branch.name;
      const locked =
        scope.allCompanies || (row.kind === 'branch' && allBranchesGranted(scope, row.company.id));
      return (
        <input
          type="checkbox"
          aria-label={`Access to ${label}`}
          checked={granted}
          disabled={locked}
          onChange={() =>
            onChange(
              row.kind === 'company'
                ? toggleCompany(scope, row.company.id)
                : toggleBranch(scope, row.company.id, row.branch.id),
            )
          }
        />
      );
    },
  };
}

function branchesColumn(
  scope: DataScopeValue,
  onChange: ((scope: DataScopeValue) => void) | undefined,
): Column<AccessRow> {
  return {
    key: 'branches',
    header: 'Branches',
    width: '160px',
    render: (row) => {
      if (row.kind === 'branch' || !companyGranted(scope, row.company.id)) {
        return <span className="muted">—</span>;
      }
      const all = allBranchesGranted(scope, row.company.id);
      if (onChange === undefined || scope.allCompanies) {
        return <span>{all ? 'All branches' : 'Selected branches'}</span>;
      }
      return (
        <label className="checkbox">
          <input
            type="checkbox"
            checked={all}
            onChange={(e) => onChange(setAllBranches(scope, row.company.id, e.target.checked))}
          />
          All branches
        </label>
      );
    },
  };
}

/**
 * The Data access section of a user record (DATA_SCOPE_DESIGN.md): the companies and branches the
 * user may act for, as a tree table of companies and their branches. "All companies" and "All
 * branches" are the default. Editable only with `onChange`; read-only otherwise.
 */
export function DataAccessSection({
  units,
  scope,
  onChange,
  loading = false,
  caption = 'Data access',
}: Readonly<DataAccessSectionProps>) {
  const [expanded, setExpanded] = useState<Set<string>>(() => openedCompanies(scope));
  const toggle = (key: string) =>
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  const columns: Column<AccessRow>[] = [
    { key: 'unit', header: 'Company / Branch', render: nameOf },
    accessColumn(scope, onChange),
    branchesColumn(scope, onChange),
  ];
  return (
    <div className="stack">
      {onChange === undefined ? (
        <p className="muted" style={{ margin: 0 }}>
          {scope.allCompanies
            ? 'All companies and branches, including those added later.'
            : 'Only the companies and branches marked below.'}
        </p>
      ) : (
        <label className="checkbox">
          <input
            type="checkbox"
            checked={scope.allCompanies}
            onChange={(e) => onChange(setAllCompanies(e.target.checked))}
          />
          All companies (including companies added later)
        </label>
      )}
      <TreeTable<AccessRow>
        caption={caption}
        columns={columns}
        nodes={accessNodes(units, scope)}
        expanded={expanded}
        onToggle={toggle}
        loading={loading}
        emptyMessage="No company to show"
      />
    </div>
  );
}
