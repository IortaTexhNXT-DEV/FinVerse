import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * My Diary (BRCLM.022): diary entries and follow-ups across the handler's claims. Placeholder of
 * the foundation; built by CL1-B.
 */
export default function DiaryPage() {
  return (
    <ClaimsPlaceholder
      title="My Diary"
      description="Your calls, e-mails, meetings, notes and follow-ups across claims, with what is due today and overdue."
      cardTitle="Diary"
      emptyMessage="No diary entries to display"
    />
  );
}
