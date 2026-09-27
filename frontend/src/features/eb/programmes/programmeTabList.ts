export type TabId =
  | 'cycle'
  | 'lines'
  | 'contacts'
  | 'documents'
  | 'bor'
  | 'franchise'
  | 'market'
  | 'proposals'
  | 'comparative'
  | 'confirmation'
  | 'submissions'
  | 'accounts'
  | 'members'
  | 'changes'
  | 'billing'
  | 'pending'
  | 'history';

/** The tabs of the programme page in display order. */
export const PROGRAMME_TABS: readonly { id: TabId; label: string }[] = [
  { id: 'cycle', label: 'Cycle' },
  { id: 'lines', label: 'Lines' },
  { id: 'contacts', label: 'Contacts' },
  { id: 'documents', label: 'Documents' },
  { id: 'bor', label: 'BOR' },
  { id: 'franchise', label: 'Franchise' },
  { id: 'market', label: 'TOR & Requests' },
  { id: 'proposals', label: 'Proposals' },
  { id: 'comparative', label: 'Comparative' },
  { id: 'confirmation', label: 'Confirmation' },
  { id: 'submissions', label: 'Submissions' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'members', label: 'Members' },
  { id: 'changes', label: 'Member Changes' },
  { id: 'billing', label: 'Billing & SOA' },
  { id: 'pending', label: 'Pending Items' },
  { id: 'history', label: 'History' },
];
