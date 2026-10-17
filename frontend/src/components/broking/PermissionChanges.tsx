import { X } from 'lucide-react';
import { countOf } from '@/utils/format';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';
import { byArea, permissionDiff } from './permissionAreas';

/** One permission as a chip (its name; the description in the tooltip), optionally removable. */
export function PermissionChip({
  code,
  tone,
  onRemove,
  action = 'Remove',
}: Readonly<{
  code: string;
  tone?: 'added' | 'removed';
  onRemove?: () => void;
  action?: string;
}>) {
  const name = permissionLabel(code);
  return (
    <span
      className={['permission-chip', tone ?? ''].join(' ').trim()}
      title={permissionDescription(code)}
    >
      {tone === 'added' && <span className="visually-hidden">Added: </span>}
      {tone === 'removed' && <span className="visually-hidden">Removed: </span>}
      <span className="permission-chip-name">{name}</span>
      {onRemove && (
        <button
          type="button"
          className="permission-chip-remove"
          aria-label={`${action} ${name}`}
          onClick={onRemove}
        >
          <X size={12} aria-hidden="true" />
        </button>
      )}
    </span>
  );
}

/**
 * The change of a profile's permissions: added (green) and removed (red), by area. Shown in the
 * picker of a change request and on the request and its approval, read only.
 */
export function PermissionChanges({
  current,
  selected,
  onRestore,
}: Readonly<{
  current: readonly string[];
  selected: readonly string[];
  onRestore?: (code: string) => void;
}>) {
  const { added, removed } = permissionDiff(current, selected);
  if (added.length === 0 && removed.length === 0) {
    return <p className="muted profile-picker-note">No change to the permissions.</p>;
  }
  return (
    <div className="permission-changes">
      <p className="profile-picker-note">
        <strong className="permission-added-text">
          {countOf(added.length, 'permission')} added
        </strong>
        {', '}
        <strong className="permission-removed-text">
          {countOf(removed.length, 'permission')} removed
        </strong>
      </p>
      {byArea([...added, ...removed]).map(([area, codes]) => (
        <div key={area} className="permission-chip-area">
          <div className="permission-chip-area-name">{area}</div>
          <span className="permission-chips">
            {codes.map((c) => (
              <PermissionChip
                key={c}
                code={c}
                tone={added.includes(c) ? 'added' : 'removed'}
                action="Undo change of"
                onRemove={
                  onRestore === undefined
                    ? undefined
                    : () => {
                        onRestore(c);
                      }
                }
              />
            ))}
          </span>
        </div>
      ))}
    </div>
  );
}
