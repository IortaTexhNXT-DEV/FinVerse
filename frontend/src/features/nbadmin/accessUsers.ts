import type { UserAccess } from '@/api/nbadmin';
import type { AccessRequestForm } from './accessRequest';

/** Status shown for a user (FR-UA-052): Active, Disabled or Locked. */
export function userStatus(user: Pick<UserAccess, 'enabled' | 'locked'>): string {
  if (user.locked) {
    return 'LOCKED';
  }
  return user.enabled ? 'ACTIVE' : 'DISABLED';
}

/** Users offered for a request type: enabled users to deactivate, disabled or locked to reactivate. */
export function usersFor(form: AccessRequestForm, users: UserAccess[]): UserAccess[] {
  if (form.type === 'DISABLE_USER') {
    return users.filter((u) => u.enabled);
  }
  if (form.type === 'ENABLE_USER') {
    return users.filter((u) => !u.enabled || u.locked);
  }
  return users;
}
