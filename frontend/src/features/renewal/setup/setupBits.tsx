import type { Approval } from '@/api/renewal';
import { CellStack } from '@/components/ui/CellStack';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';

/** Maker-checker status of a set-up record. */
export function ApprovalCell({ approval }: Readonly<{ approval: Approval }>) {
  return (
    <CellStack
      main={<StatusBadge status={approval.recordStatus} />}
      sub={approval.maker === null ? '' : <UserName login={approval.maker} />}
    />
  );
}
