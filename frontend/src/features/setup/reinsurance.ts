/**
 * The reinsurance functions are deferred to Phase 2 for BDOI (client decision of 17-Oct-2026): the
 * reinsurer and reinsurance broker parties, receipts from a reinsurer and reinsurance settlement
 * payments are offered only to a user holding RI_BROKING_TRANSACT, which no BDOI group profile
 * holds (the super user of DEV, SIT and UAT does). The server refuses them to anybody else.
 */
export const REINSURANCE_VALUES: readonly string[] = ['REINSURER', 'RI_BROKER', 'REINSURANCE'];

/** Whether the signed-in user may use the reinsurance functions. */
export function reinsuranceInUse(can: (permission: string) => boolean): boolean {
  return can('RI_BROKING_TRANSACT');
}

/** The values of a list without the reinsurance ones, unless the user may use them. */
export function withoutReinsurance<T extends string>(
  values: readonly T[],
  can: (permission: string) => boolean,
): T[] {
  return reinsuranceInUse(can)
    ? [...values]
    : values.filter((v) => !REINSURANCE_VALUES.includes(v));
}
