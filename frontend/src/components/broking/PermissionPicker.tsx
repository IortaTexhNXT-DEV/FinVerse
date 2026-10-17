import { ChevronDown, ChevronRight, Search } from 'lucide-react';
import { useId, useState } from 'react';
import { Notice } from '@/components/ui/Notice';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';
import { byArea, groupPermissions, permissionConflicts, privilegeOf } from './permissionAreas';
import { PermissionChanges, PermissionChip } from './PermissionChanges';
import type { PermissionGroup } from './permissionAreas';
import type { ProfileConflictRule, ProfileInfo } from './profileAreas';

interface PermissionPickerProps {
  legend: string;
  required?: boolean;
  /** Every permission that may be granted. */
  permissions: readonly string[];
  /** The permissions that approve (their action class), for the privilege hint. */
  approving?: ReadonlySet<string>;
  selected: readonly string[];
  onChange: (permissions: string[]) => void;
  /** The profile's current permissions (a change request): added and removed are shown. */
  current?: readonly string[];
  /** The group profiles: read for the separation-of-duties rules and "Start from a profile". */
  profiles?: readonly ProfileInfo[];
  rules?: readonly ProfileConflictRule[];
  /** Offers to start from the permissions of an active profile (a new profile). */
  offerCopy?: boolean;
  error?: string;
}

interface AreaProps {
  group: PermissionGroup;
  open: boolean;
  onToggle: () => void;
  selected: readonly string[];
  current: readonly string[];
  onPick: (codes: readonly string[], on: boolean) => void;
}

function Area({ group, open, onToggle, selected, current, onPick }: Readonly<AreaProps>) {
  const id = useId();
  const picked = group.all.filter((p) => selected.includes(p)).length;
  const shownPicked = group.shown.filter((p) => selected.includes(p)).length;
  return (
    <div className="profile-group">
      <div className="permission-group-head">
        <button
          type="button"
          className="profile-group-head"
          aria-expanded={open}
          aria-controls={id}
          onClick={onToggle}
        >
          {open ? (
            <ChevronDown size={16} aria-hidden="true" />
          ) : (
            <ChevronRight size={16} aria-hidden="true" />
          )}
          <span className="profile-group-name">{group.area}</span>
          <span className="profile-group-count">
            {`${String(picked)} of ${String(group.all.length)} selected`}
          </span>
        </button>
        <span className="permission-group-actions">
          <button
            type="button"
            className="link-button"
            disabled={shownPicked === group.shown.length}
            aria-label={`Select all in ${group.area}`}
            onClick={() => onPick(group.shown, true)}
          >
            Select all
          </button>
          <button
            type="button"
            className="link-button"
            disabled={shownPicked === 0}
            aria-label={`Clear ${group.area}`}
            onClick={() => onPick(group.shown, false)}
          >
            Clear
          </button>
        </span>
      </div>
      {open && (
        <ul id={id} className="profile-options">
          {group.shown.map((p) => {
            const on = selected.includes(p);
            const box = `${id}-${p}`;
            let change = '';
            if (current.length > 0 && on !== current.includes(p)) {
              change = on ? ' added' : ' removed';
            }
            return (
              <li
                key={p}
                className={`profile-option permission-option${on ? ' selected' : ''}${change}`}
              >
                <input
                  id={box}
                  type="checkbox"
                  checked={on}
                  aria-describedby={`${box}-desc`}
                  onChange={(e) => onPick([p], e.target.checked)}
                />
                <span className="permission-option-text">
                  <label htmlFor={box} className="profile-option-name">
                    {permissionLabel(p)}
                  </label>
                  <span
                    id={`${box}-desc`}
                    className="profile-option-desc"
                    title={permissionDescription(p)}
                  >
                    {permissionDescription(p)}
                  </span>
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function CopyFrom({
  profiles,
  onCopy,
}: Readonly<{ profiles: readonly ProfileInfo[]; onCopy: (p: ProfileInfo) => void }>) {
  const id = useId();
  const active = profiles
    .filter((p) => p.active !== false)
    .sort((a, b) => a.name.localeCompare(b.name));
  return (
    <span className="permission-copy">
      <label htmlFor={id}>Start from an existing profile</label>
      <select
        id={id}
        className="select"
        value=""
        onChange={(e) => {
          const p = active.find((x) => x.code === e.target.value);
          if (p !== undefined) {
            onCopy(p);
          }
        }}
      >
        <option value="">Select a profile to copy…</option>
        {active.map((p) => (
          <option key={p.code} value={p.code}>
            {p.name}
          </option>
        ))}
      </select>
    </span>
  );
}

function Warnings({
  selected,
  profiles,
  rules,
  approving,
}: Readonly<{
  selected: readonly string[];
  profiles: readonly ProfileInfo[];
  rules: readonly ProfileConflictRule[];
  approving?: ReadonlySet<string>;
}>) {
  const conflicts = permissionConflicts(selected, rules, profiles);
  const privileged = selected
    .map((p) => privilegeOf(p, approving?.has(p)))
    .filter((x): x is 'Administration' | 'Approval' => x !== null);
  return (
    <>
      {conflicts.length > 0 && (
        <Notice tone="warning" title="Separation of duties">
          These permissions may not be held together:
          <ul>
            {conflicts.map((c) => (
              <li key={c.key}>
                {permissionLabel(c.first)} ({c.profileA}) and {permissionLabel(c.second)} (
                {c.profileB}): {c.reason}
              </li>
            ))}
          </ul>
        </Notice>
      )}
      {privileged.includes('Administration') && (
        <Notice tone="info" title="High privilege">
          The profile administers users, profiles or security: set the privilege level to Admin.
        </Notice>
      )}
      {!privileged.includes('Administration') && privileged.includes('Approval') && (
        <Notice tone="info" title="Approval rights">
          The profile approves work: consider the privilege level High.
        </Notice>
      )}
    </>
  );
}

function SelectedSummary({
  selected,
  onRemove,
}: Readonly<{ selected: readonly string[]; onRemove: (code: string) => void }>) {
  if (selected.length === 0) {
    return (
      <p className="muted profile-selected-empty">
        No permission selected yet. Tick the permissions in the list.
      </p>
    );
  }
  return (
    <div className="permission-selected">
      {byArea(selected).map(([area, codes]) => (
        <div key={area} className="permission-chip-area">
          <div className="permission-chip-area-name">
            {area} <span className="muted">({codes.length})</span>
          </div>
          <span className="permission-chips">
            {codes.map((c) => (
              <PermissionChip key={c} code={c} onRemove={() => onRemove(c)} />
            ))}
          </span>
        </div>
      ))}
    </div>
  );
}

/**
 * The permission picker of the group-profile requests: on the left the permissions by business
 * area in collapsible sections (closed at first, "n of m selected", select all and clear per
 * area), one search over names and descriptions that opens the matching areas; on the right the
 * selected permissions by area (removable), the total, separation-of-duties and privilege
 * warnings and, for a change, the permissions added and removed. Native checkboxes and buttons.
 */
export function PermissionPicker({
  legend,
  required = false,
  permissions,
  approving,
  selected,
  onChange,
  current = [],
  profiles = [],
  rules = [],
  offerCopy = false,
  error,
}: Readonly<PermissionPickerProps>) {
  const searchId = useId();
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set());
  const groups = groupPermissions([...permissions, ...selected, ...current], search);
  const searching = search.trim() !== '';
  const toggle = (area: string) => {
    const next = new Set(open);
    if (next.has(area)) {
      next.delete(area);
    } else {
      next.add(area);
    }
    setOpen(next);
  };
  const pick = (codes: readonly string[], on: boolean) => {
    const rest = selected.filter((c) => !codes.includes(c));
    onChange(on ? [...rest, ...codes] : rest);
  };
  const changing = current.length > 0;
  return (
    <fieldset className="profile-picker permission-picker">
      <legend className={required ? 'required' : undefined}>{legend}</legend>
      <div className="profile-picker-grid">
        <section className="profile-picker-list" aria-label="Permissions by business area">
          <div className="profile-picker-tools">
            <label className="visually-hidden" htmlFor={searchId}>
              Search permissions
            </label>
            <span className="profile-search">
              <Search size={16} aria-hidden="true" />
              <input
                id={searchId}
                type="search"
                className="input"
                placeholder="Search permission name or what it allows"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </span>
            <button
              type="button"
              className="link-button"
              onClick={() => setOpen(new Set(groups.map((g) => g.area)))}
            >
              Expand All
            </button>
            <button type="button" className="link-button" onClick={() => setOpen(new Set())}>
              Collapse All
            </button>
            {offerCopy && profiles.length > 0 && (
              <CopyFrom profiles={profiles} onCopy={(p) => onChange([...p.permissions])} />
            )}
          </div>
          {groups.length === 0 && <p className="muted permission-none">No permission matches.</p>}
          {groups.map((g) => (
            <Area
              key={g.area}
              group={g}
              open={searching || open.has(g.area)}
              onToggle={() => toggle(g.area)}
              selected={selected}
              current={current}
              onPick={pick}
            />
          ))}
        </section>
        <aside className="profile-picker-side" aria-label="Selected permissions">
          <h3 className="profile-picker-side-title">
            Selected Permissions <span className="tab-count">{selected.length}</span>
          </h3>
          <Warnings selected={selected} profiles={profiles} rules={rules} approving={approving} />
          {changing && (
            <PermissionChanges
              current={current}
              selected={selected}
              onRestore={(c) => pick([c], !selected.includes(c))}
            />
          )}
          <SelectedSummary selected={selected} onRemove={(c) => pick([c], false)} />
        </aside>
      </div>
      {error && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </fieldset>
  );
}
