import { X } from 'lucide-react';
import { Tag } from '@/components/ui/Tag';
import { humanize } from '@/utils/format';
import { permissionLabel } from '@/utils/permissionLabel';
import {
  areaOfPermission,
  areaOfProfile,
  isActing,
  keyPermissions,
  profileKind,
  profileSummary,
} from './profileAreas';
import type { ProfileInfo } from './profileAreas';

/** Most permissions listed per heading of the details (the rest are counted). */
const SHOWN = 8;

function PermissionList({ title, codes }: Readonly<{ title: string; codes: readonly string[] }>) {
  if (codes.length === 0) {
    return null;
  }
  const labels = [...new Set(codes.map(permissionLabel))].sort((a, b) => a.localeCompare(b));
  return (
    <div className="profile-details-group">
      <h4>
        {title} <span className="muted">({labels.length})</span>
      </h4>
      <ul>
        {labels.slice(0, SHOWN).map((l) => (
          <li key={l}>{l}</li>
        ))}
        {labels.length > SHOWN && <li className="muted">and {labels.length - SHOWN} more</li>}
      </ul>
    </div>
  );
}

/**
 * What a group profile grants, in business words: its area, kind (maker, approver, view only),
 * privilege level, the areas it works in and its key permissions (approves / does / sees). Approval
 * amounts are not on the profile: they come from the user's Authorisation Limit.
 */
export function ProfileDetails({
  profile,
  onClose,
}: Readonly<{ profile: ProfileInfo; onClose: () => void }>) {
  const { approves, does, sees } = keyPermissions(profile.permissions);
  const areas = [
    ...new Set(
      profile.permissions
        .filter(isActing)
        .map(areaOfPermission)
        .filter((a): a is string => a !== null),
    ),
  ];
  return (
    <section className="profile-details" aria-label={`Details of ${profile.name}`}>
      <header className="profile-details-head">
        <h3>{profile.name}</h3>
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          aria-label="Close the profile details"
          onClick={onClose}
        >
          <X size={16} aria-hidden="true" />
        </button>
      </header>
      <p className="muted">{profileSummary(profile)}</p>
      <dl className="profile-details-facts">
        <dt>Business Area</dt>
        <dd>{areaOfProfile(profile)}</dd>
        <dt>Kind</dt>
        <dd>{profileKind(profile)}</dd>
        <dt>Privilege Level</dt>
        <dd>
          {profile.privilegeLevel === undefined ? '—' : humanize(profile.privilegeLevel)}
          {(profile.privilegeLevel === 'HIGH' || profile.privilegeLevel === 'ADMIN') && (
            <>
              {' '}
              <Tag tone="danger">Privileged</Tag>
            </>
          )}
        </dd>
        <dt>Works In</dt>
        <dd>{areas.length === 0 ? 'Views only' : areas.join(', ')}</dd>
        <dt>Approval Limit</dt>
        <dd>{approves.length === 0 ? 'No approvals' : "Up to the user's Authorisation Limit"}</dd>
      </dl>
      <PermissionList title="Approves" codes={approves} />
      <PermissionList title="Does" codes={does} />
      <PermissionList title="Sees" codes={sees} />
    </section>
  );
}
