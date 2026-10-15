import { AuditTrailView } from '@/features/admin/AuditTrailView';
import { UAM_AUDIT_SCOPE } from './labels';

/**
 * Audit Log of User Access: the audit trail preset to the user access events (users, sessions,
 * group profiles and their changes, separation of duties, identity synchronisation, certification
 * extracts and the security settings).
 */
export default function AuditLogPage() {
  return (
    <AuditTrailView
      section="User Access"
      title="Audit Log"
      description="Every user access action, who performed it, when and from where."
      scope={UAM_AUDIT_SCOPE}
    />
  );
}
