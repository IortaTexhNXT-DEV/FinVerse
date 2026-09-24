import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { bulkApi } from '@/api/bulk';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';

/** One bulk upload type: template, upload, review and processing (generic wizard). */
export default function BulkUploadPage() {
  const code = useParams().handler ?? '';
  const handler = useQuery({
    queryKey: ['bulk', 'handler', code],
    queryFn: () => bulkApi.handler(code),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Bulk Processing"
        title={handler.data?.title ?? 'Bulk upload'}
        description={describe(handler.data?.instructions)}
        actions={<Link to="/bulk">All upload types</Link>}
      />
      <ErrorAlert error={handler.error} />
      {handler.data && <BulkUploadWizard handler={code} />}
    </div>
  );
}

function describe(instructions: string | undefined): string {
  return instructions === undefined || instructions === ''
    ? 'Download the template, fill it in and upload it.'
    : instructions;
}
