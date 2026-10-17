import { AuditTrailView } from './AuditTrailView';

/** Audit trail inquiry: originator, modifier and authorizer activity with timestamps. */
export default function AuditTrailPage() {
  return (
    <AuditTrailView
      section="Administration"
      title="Audit Trail"
      description="Every financial and non-financial action, who performed it, when and from where."
    />
  );
}
