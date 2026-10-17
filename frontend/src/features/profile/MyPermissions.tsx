import { byArea } from '@/components/broking/permissionAreas';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';

/**
 * The signed-in user's permissions in business words, grouped by the areas of the permission
 * picker, one collapsible group per area with its count.
 */
export function MyPermissions({ permissions }: Readonly<{ permissions: readonly string[] }>) {
  const groups = byArea([...new Set(permissions)]);
  if (groups.length === 0) {
    return <p className="muted">No permissions.</p>;
  }
  return (
    <div className="stack" aria-label="Permissions by area">
      {groups.map(([area, codes]) => (
        <details key={area}>
          <summary style={{ cursor: 'pointer', fontWeight: 600 }}>
            {area} ({codes.length})
          </summary>
          <table className="table" style={{ marginTop: 'var(--space-2)' }}>
            <thead>
              <tr>
                <th>Permission</th>
                <th>What it allows</th>
              </tr>
            </thead>
            <tbody>
              {[...codes]
                .sort((a, b) => permissionLabel(a).localeCompare(permissionLabel(b)))
                .map((code) => (
                  <tr key={code}>
                    <td>{permissionLabel(code)}</td>
                    <td>{permissionDescription(code)}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </details>
      ))}
    </div>
  );
}
