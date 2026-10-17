import type { UserProfile } from '@/api/types';

/** Status of a user on the User Access screens. */
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED';

export const USER_STATUSES: readonly UserStatus[] = ['ACTIVE', 'INACTIVE', 'LOCKED'];

export const USER_STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  LOCKED: 'Locked',
};

/** Inactive when deactivated (by UIDM-ISC or the dormancy job), Locked after failed sign-ins. */
export function statusOf(u: Pick<UserProfile, 'enabled' | 'locked'>): UserStatus {
  if (!u.enabled) {
    return 'INACTIVE';
  }
  return u.locked ? 'LOCKED' : 'ACTIVE';
}

/** Filters of the Users list. */
export interface UserFilter {
  text: string;
  status: UserStatus | '';
  profile: string;
  businessUnit: string;
}

export const NO_USER_FILTER: UserFilter = { text: '', status: '', profile: '', businessUnit: '' };

/** Whether a user matches the search text (User ID, name or Windows ID) and the filters. */
export function matchesUser(u: UserProfile, f: UserFilter): boolean {
  const needle = f.text.trim().toLocaleLowerCase();
  const found =
    needle === '' ||
    [u.username, u.fullName, u.windowsId ?? ''].some((v) => v.toLocaleLowerCase().includes(needle));
  return (
    found &&
    (f.status === '' || statusOf(u) === f.status) &&
    (f.profile === '' || u.roles.includes(f.profile)) &&
    (f.businessUnit === '' || u.businessUnitCode === f.businessUnit)
  );
}
