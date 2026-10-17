import { ChevronDown, ChevronRight, Info, Search, X } from 'lucide-react';
import { useId, useState } from 'react';
import { Notice } from '@/components/ui/Notice';
import { Tag } from '@/components/ui/Tag';
import { countOf } from '@/utils/format';
import { ProfileDetails } from './ProfileDetails';
import {
  areaOfProfile,
  groupProfiles,
  profileChange,
  profileConflicts,
  profileSummary,
} from './profileAreas';
import type { ProfileConflictRule, ProfileGroup, ProfileInfo } from './profileAreas';

interface ProfilePickerProps<P extends ProfileInfo> {
  legend: string;
  required?: boolean;
  profiles: readonly P[];
  selected: readonly string[];
  onChange: (codes: string[]) => void;
  /** The user's current profiles (a change request): marked current, added or removed. */
  current?: readonly string[];
  /** Separation-of-duties rules: two selected profiles of a rule are flagged. */
  rules?: readonly ProfileConflictRule[];
  error?: string;
}

const PRIVILEGED = new Set(['HIGH', 'ADMIN']);

function ChangeTag({ change }: Readonly<{ change: string }>) {
  if (change === 'current') {
    return <Tag tone="neutral">Current</Tag>;
  }
  if (change === 'added') {
    return <Tag tone="info">Added</Tag>;
  }
  return change === 'removed' ? <Tag tone="danger">Removed</Tag> : null;
}

interface GroupProps<P extends ProfileInfo> {
  group: ProfileGroup<P>;
  open: boolean;
  onToggle: () => void;
  selected: readonly string[];
  current: readonly string[];
  shown?: string;
  onPick: (code: string, on: boolean) => void;
  onShow: (code: string) => void;
}

function Group<P extends ProfileInfo>(g: Readonly<GroupProps<P>>) {
  const id = useId();
  const picked = g.group.profiles.filter((p) => g.selected.includes(p.code)).length;
  return (
    <div className="profile-group">
      <button
        type="button"
        className="profile-group-head"
        aria-expanded={g.open}
        aria-controls={id}
        onClick={g.onToggle}
      >
        {g.open ? (
          <ChevronDown size={16} aria-hidden="true" />
        ) : (
          <ChevronRight size={16} aria-hidden="true" />
        )}
        <span className="profile-group-name">{g.group.area}</span>
        <span className="profile-group-count">
          {picked > 0
            ? `${String(picked)} of ${String(g.group.profiles.length)}`
            : g.group.profiles.length}
        </span>
      </button>
      {g.open && (
        <ul id={id} className="profile-options">
          {g.group.profiles.map((p) => {
            const on = g.selected.includes(p.code);
            const box = `${id}-${p.code}`;
            return (
              <li key={p.code} className={on ? 'profile-option selected' : 'profile-option'}>
                <input
                  id={box}
                  type="checkbox"
                  checked={on}
                  aria-describedby={`${box}-desc`}
                  onChange={(e) => g.onPick(p.code, e.target.checked)}
                />
                <span className="profile-option-text">
                  <label htmlFor={box} className="profile-option-name">
                    {p.name}
                  </label>
                  <span
                    id={`${box}-desc`}
                    className="profile-option-desc"
                    title={profileSummary(p)}
                  >
                    {profileSummary(p)}
                  </span>
                </span>
                <span className="profile-option-tags">
                  {g.current.includes(p.code) && <Tag tone="neutral">Current</Tag>}
                  {PRIVILEGED.has(p.privilegeLevel ?? '') && <Tag tone="danger">Privileged</Tag>}
                </span>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm profile-option-info"
                  aria-label={`Details of ${p.name}`}
                  aria-pressed={g.shown === p.code}
                  onClick={() => g.onShow(p.code)}
                >
                  <Info size={16} aria-hidden="true" />
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function SelectedPanel({
  profiles,
  selected,
  current,
  conflicted,
  onPick,
  onShow,
}: Readonly<{
  profiles: readonly ProfileInfo[];
  selected: readonly string[];
  current: readonly string[];
  conflicted: ReadonlySet<string>;
  onPick: (code: string, on: boolean) => void;
  onShow: (code: string) => void;
}>) {
  const byCode = new Map(profiles.map((p) => [p.code, p]));
  const removed = current.filter((c) => !selected.includes(c));
  const rows = [...selected, ...removed];
  if (rows.length === 0) {
    return (
      <p className="muted profile-selected-empty">
        No profile selected yet. Tick the profiles in the list.
      </p>
    );
  }
  return (
    <ul className="profile-selected">
      {rows.map((code) => {
        const p = byCode.get(code);
        const change = profileChange(code, selected, current);
        const name = p?.name ?? code;
        return (
          <li
            key={code}
            className={[
              'profile-selected-item',
              change === 'removed' ? 'removed' : '',
              conflicted.has(code) ? 'conflict' : '',
            ]
              .filter(Boolean)
              .join(' ')}
          >
            <button type="button" className="link-button inline" onClick={() => onShow(code)}>
              {name}
            </button>
            <span className="muted profile-selected-area">
              {p === undefined ? '' : areaOfProfile(p)}
            </span>
            {current.length > 0 && <ChangeTag change={change} />}
            {change === 'removed' ? (
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => onPick(code, true)}
              >
                Keep
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                aria-label={`Remove ${name}`}
                onClick={() => onPick(code, false)}
              >
                <X size={16} aria-hidden="true" />
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * The group-profile picker of the user access screens: on the left the profiles grouped by
 * business area in collapsible groups with counts, searchable, each with a one-line description of
 * what it does and a details button; on the right the selected profiles (current, added and
 * removed ones for a change request, each removable), separation-of-duties warnings when two
 * selected profiles may not be held together, and the details of the profile in view (key
 * permissions, kind, level). Native checkboxes and buttons: fully keyboard operable.
 */
export function ProfilePicker<P extends ProfileInfo>({
  legend,
  required = false,
  profiles,
  selected,
  onChange,
  current = [],
  rules = [],
  error,
}: Readonly<ProfilePickerProps<P>>) {
  const searchId = useId();
  const [search, setSearch] = useState('');
  const [opened, setOpened] = useState<ReadonlySet<string> | null>(null);
  const [shown, setShown] = useState<string>();
  const groups = groupProfiles(profiles, search, [...selected, ...current]);
  // Groups holding a selected profile start open; the others start closed.
  const open =
    opened ??
    new Set(
      groups.filter((g) => g.profiles.some((p) => selected.includes(p.code))).map((g) => g.area),
    );
  const toggle = (area: string) => {
    const next = new Set(open);
    if (next.has(area)) {
      next.delete(area);
    } else {
      next.add(area);
    }
    setOpened(next);
  };
  const pick = (code: string, on: boolean) =>
    onChange(
      on ? [...selected.filter((c) => c !== code), code] : selected.filter((c) => c !== code),
    );
  const conflicts = profileConflicts(selected, rules);
  const conflicted = new Set(conflicts.flatMap((c) => [c.a, c.b]));
  const nameOf = (code: string) => profiles.find((p) => p.code === code)?.name ?? code;
  const shownProfile = profiles.find((p) => p.code === shown);
  return (
    <fieldset className="profile-picker">
      <legend className={required ? 'required' : undefined}>{legend}</legend>
      <div className="profile-picker-grid">
        <section className="profile-picker-list" aria-label="Group profiles by business area">
          <div className="profile-picker-tools">
            <label className="visually-hidden" htmlFor={searchId}>
              Search group profiles
            </label>
            <span className="profile-search">
              <Search size={16} aria-hidden="true" />
              <input
                id={searchId}
                type="search"
                className="input"
                placeholder="Search profile, area or what it does"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </span>
            <button
              type="button"
              className="link-button"
              onClick={() => setOpened(new Set(groups.map((g) => g.area)))}
            >
              Expand All
            </button>
            <button type="button" className="link-button" onClick={() => setOpened(new Set())}>
              Collapse All
            </button>
          </div>
          {groups.length === 0 && <p className="muted">No group profile matches the search.</p>}
          {groups.map((g) => (
            <Group
              key={g.area}
              group={g}
              open={search.trim() !== '' || open.has(g.area)}
              onToggle={() => toggle(g.area)}
              selected={selected}
              current={current}
              shown={shown}
              onPick={pick}
              onShow={setShown}
            />
          ))}
        </section>
        <aside className="profile-picker-side" aria-label="Selected group profiles">
          <h3 className="profile-picker-side-title">
            Selected Profiles <span className="tab-count">{selected.length}</span>
          </h3>
          {conflicts.length > 0 && (
            <Notice tone="warning" title="Separation of duties">
              One user may not hold these profiles together:
              <ul>
                {conflicts.map((c) => (
                  <li key={`${c.a}-${c.b}`}>
                    {nameOf(c.a)} and {nameOf(c.b)}: {c.reason}
                  </li>
                ))}
              </ul>
            </Notice>
          )}
          <SelectedPanel
            profiles={profiles}
            selected={selected}
            current={current}
            conflicted={conflicted}
            onPick={pick}
            onShow={setShown}
          />
          {current.length > 0 && (
            <p className="muted profile-picker-note">
              {countOf(selected.filter((c) => !current.includes(c)).length, 'profile')} added,{' '}
              {countOf(current.filter((c) => !selected.includes(c)).length, 'profile')} removed.
            </p>
          )}
          {shownProfile !== undefined && (
            <ProfileDetails profile={shownProfile} onClose={() => setShown(undefined)} />
          )}
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
