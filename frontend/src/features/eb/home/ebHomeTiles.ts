/**
 * The work tiles of EB Home (design 10.1; FR-EB-062) in display order, each with the list it opens
 * and the key of its count in the EB Home API. Without the partner portal (BDOI Drop 2) there is no
 * "portal uploads to review" tile.
 */
export interface EbHomeTile {
  id: string;
  label: string;
  /** Route of the list the tile opens. */
  to: string;
  /** Draws attention to a non-zero count. */
  alert?: boolean;
}

const stage = (code: string) => `/eb/programmes?tab=IN_PROGRESS&stage=${code}`;

export const EB_HOME_TILES: readonly EbHomeTile[] = [
  { id: 'raDue', label: 'Renewal Advice Due', to: '/eb/programmes?tab=RENEWAL_DUE', alert: true },
  { id: 'awaitingFeedback', label: 'Awaiting Feedback', to: stage('RA_SENT') },
  { id: 'franchisePending', label: 'Franchise Pending', to: stage('FRANCHISE') },
  { id: 'proposalsOutstanding', label: 'Proposals Outstanding', to: stage('PROPOSALS') },
  { id: 'comparativesToSignOff', label: 'Comparatives to Sign Off', to: stage('FOR_SIGNOFF') },
  { id: 'thresholdApprovals', label: 'Threshold Approvals', to: stage('THRESHOLD_APPROVAL') },
  { id: 'withClient', label: 'With Client', to: '/eb/programmes?tab=WITH_CLIENT' },
  { id: 'memberChangesOpen', label: 'Member Changes Open', to: '/eb/member-changes' },
  {
    id: 'soaToValidate',
    label: 'Statements of Account to Validate',
    to: '/eb/soa?status=RECEIVED',
  },
  {
    id: 'pendingItemsOverdue',
    label: 'Pending Items Overdue',
    to: '/eb/pending-items?overdue=true',
    alert: true,
  },
];

/** The count of a tile, or a dash when the API has none for it. */
export function tileValue(counts: Record<string, number> | undefined, id: string): string {
  const value = counts?.[id];
  return value === undefined ? '–' : String(value);
}
