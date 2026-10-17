import { User } from 'lucide-react';
import { CellStack } from '@/components/ui/CellStack';
import { useDisplayName, useRoleName } from '@/components/ui/useDisplayName';
import type { OrgRow } from './salesTree';

/** Unit column: code in bold and name; an officer by display name with the role under it. */
export function UnitCell({ row }: Readonly<{ row: OrgRow }>) {
  const name = useDisplayName();
  const role = useRoleName();
  if (row.kind === 'officer') {
    return (
      <>
        <User size={16} aria-hidden="true" />
        <CellStack
          main={<span title={row.officer.username}>{name(row.officer.username)}</span>}
          sub={role(row.officer.username)}
        />
      </>
    );
  }
  const u = row.node.unit;
  return (
    <span>
      <span className="tree-code">{u.code}</span> {u.name}
    </span>
  );
}

export function CostCenterCell({ row }: Readonly<{ row: OrgRow }>) {
  if (row.kind === 'officer') {
    return null;
  }
  const n = row.node;
  if (n.unit.costCenter) {
    return <span className="nowrap">{n.unit.costCenter}</span>;
  }
  if (!n.costCenter) {
    return null;
  }
  return (
    <CellStack
      main={<span className="nowrap">{n.costCenter}</span>}
      sub={`Inherited from ${n.costCenterFrom ?? ''}`}
    />
  );
}
