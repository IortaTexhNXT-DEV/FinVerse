import { permissionLabel } from '@/utils/permissionLabel';

/** A permission shown by its name, with the code as a muted second line for the administrators. */
export function PermissionName({ code }: Readonly<{ code: string }>) {
  return (
    <>
      <span>{permissionLabel(code)}</span>
      <span className="cell-sub mono">{code}</span>
    </>
  );
}
