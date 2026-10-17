/** Fields of a maker-checker master record that identify its maker. */
export interface MaintainedRecord {
  recordStatus?: string;
  createdBy?: string;
  updatedBy?: string;
  /** User who created or last maintained the record (unchanged by authorization). */
  maker?: string;
}

/** The maker of a record: the last maintainer, else the creator. */
export function makerOf(record: MaintainedRecord): string | undefined {
  return record.maker ?? record.updatedBy ?? record.createdBy;
}

/**
 * Whether the "Authorize" action is offered to a user: the record waits for authorization and the
 * user did not maintain it (four eyes; the server refuses the maker with MAKER_CHECKER_VIOLATION).
 * The permission check stays with the caller, since screens authorize under different permissions.
 */
export function awaitsOtherChecker(
  record: MaintainedRecord | undefined,
  username: string | undefined,
): boolean {
  return (
    record?.recordStatus === 'PENDING_AUTHORIZATION' &&
    username !== undefined &&
    makerOf(record) !== username
  );
}
