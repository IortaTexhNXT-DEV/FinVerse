import { StatusBadge } from '@/components/ui/StatusBadge';
import { migLabel } from './migrationCodes';

/** Status badge of a migration record with its readable label. */
export function MigStatus({ status }: Readonly<{ status: string | undefined }>) {
  if (status === undefined || status === '') {
    return null;
  }
  return <StatusBadge status={status} label={migLabel(status)} />;
}
