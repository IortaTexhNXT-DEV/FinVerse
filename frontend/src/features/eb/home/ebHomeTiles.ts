/**
 * The work tiles of EB Home (design 10.1; FR-EB-062) in display order, each with the list it opens.
 * The counts come with waves E1-B (marketing tiles) and E1-C (servicing tiles). Without the partner
 * portal (BDOI Drop 2) there is no "portal uploads to review" tile.
 */
export interface EbHomeTile {
  id: string;
  label: string;
  /** Route of the list the tile opens. */
  to: string;
  /** Wave that fills the count. */
  wave: 'E1-B' | 'E1-C';
}

export const EB_HOME_TILES: readonly EbHomeTile[] = [
  { id: 'raDue', label: 'RA due', to: '/eb/programmes?tab=RENEWAL_DUE', wave: 'E1-B' },
  {
    id: 'awaitingFeedback',
    label: 'Awaiting feedback',
    to: '/eb/programmes?tab=IN_PROGRESS',
    wave: 'E1-B',
  },
  {
    id: 'franchisePending',
    label: 'Franchise pending',
    to: '/eb/programmes?tab=IN_PROGRESS',
    wave: 'E1-B',
  },
  {
    id: 'proposalsOutstanding',
    label: 'Proposals outstanding',
    to: '/eb/programmes?tab=IN_PROGRESS',
    wave: 'E1-B',
  },
  {
    id: 'comparativesToSignOff',
    label: 'Comparatives to sign off',
    to: '/eb/programmes?tab=IN_PROGRESS',
    wave: 'E1-B',
  },
  {
    id: 'thresholdApprovals',
    label: 'Threshold approvals',
    to: '/eb/programmes?tab=IN_PROGRESS',
    wave: 'E1-B',
  },
  { id: 'withClient', label: 'With client', to: '/eb/programmes?tab=WITH_CLIENT', wave: 'E1-B' },
  {
    id: 'memberChangesOpen',
    label: 'Member changes open',
    to: '/eb/member-changes',
    wave: 'E1-C',
  },
  {
    id: 'pendingItemsOverdue',
    label: 'Pending items overdue',
    to: '/eb/pending-items?overdue=true',
    wave: 'E1-C',
  },
];
