import { LovLabel } from '@/components/broking/LovLabel';
import { useLovLabel } from '@/components/broking/useLabels';

/** List of the legacy statuses of archive records (maintained by the Data Steward). */
export const LEGACY_STATUS_LIST = 'LEGACY_RECORD_STATUS';

/** List of the legacy record types. */
export const LEGACY_TYPE_LIST = 'LEGACY_RECORD_TYPE';

/**
 * The legacy status of an archive record in words (e.g. "Fully paid"), with the value stored in
 * legacy in the tooltip; `withValue` also shows the stored value after the label (record detail).
 */
export function LegacyStatus({
  status,
  withValue = false,
}: Readonly<{ status: string | undefined; withValue?: boolean }>) {
  const label = useLovLabel(LEGACY_STATUS_LIST);
  if (!status) {
    return null;
  }
  return (
    <span title={`Stored in legacy as ${status}`}>
      {label(status)}
      {withValue && <span className="muted"> · stored in legacy as {status}</span>}
    </span>
  );
}

/** The kind of a legacy archive record in words (e.g. "GL journal"). */
export function LegacyRecordType({ type }: Readonly<{ type: string }>) {
  return <LovLabel type={LEGACY_TYPE_LIST} code={type} />;
}
