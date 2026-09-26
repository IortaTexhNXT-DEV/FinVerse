import { useQueryClient } from '@tanstack/react-query';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { PageHeader } from '@/components/ui/PageHeader';

/**
 * Batch of endorsement and cancellation requests by upload (ADJID.006): one request per row,
 * validated like a request raised on screen, then raised and submitted for validation.
 */
export default function BatchUploadPage() {
  const queryClient = useQueryClient();
  return (
    <div className="stack">
      <PageHeader
        backTo="/adjustment"
        section="Client & Policy · Adjustment"
        title="Batch Request Upload"
        description="Upload cancellation and adjustment requests for many invoices at once; each valid row is raised and submitted for validation."
      />
      <BulkUploadWizard
        handler="ADJ_BATCH"
        onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['adjustment'] })}
      />
    </div>
  );
}
