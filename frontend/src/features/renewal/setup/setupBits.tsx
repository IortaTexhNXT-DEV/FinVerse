import type { Approval } from '@/api/renewal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';

/** Maker-checker status of a set-up record. */
export function ApprovalCell({ approval }: Readonly<{ approval: Approval }>) {
  return (
    <span className="rnw-flags">
      <StatusBadge status={approval.recordStatus} />
      {approval.maker !== null && <UserName login={approval.maker} />}
    </span>
  );
}
