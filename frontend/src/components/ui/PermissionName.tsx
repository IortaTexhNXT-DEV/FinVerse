import { useAuth } from '@/auth/authContext';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';

/** Permissions of the administrators, who may see a permission's code in its tooltip. */
const ADMINISTRATOR = ['ROLE_MANAGE', 'USER_MANAGE'];

/**
 * A permission shown by its name with a short plain description on a muted second line. The
 * code never shows on the screen; administrators find it in the tooltip.
 */
export function PermissionName({ code }: Readonly<{ code: string }>) {
  const { can } = useAuth();
  const administrator = ADMINISTRATOR.some((p) => can(p));
  return (
    <span className="cell-stack permission-name" title={administrator ? code : undefined}>
      <span>{permissionLabel(code)}</span>
      <span className="muted">{permissionDescription(code)}</span>
    </span>
  );
}
