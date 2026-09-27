import type { ReactNode } from 'react';
import { useState } from 'react';
import type { BulkJob } from '@/api/bulk';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

/** A bulk upload of a Renewal screen, shown under the header while open. */
export function UploadPanel({
  label,
  handler,
  parameters,
  parameterFields,
  parametersReady,
  onClose,
  after,
}: Readonly<{
  label: string;
  handler: string;
  parameters?: Record<string, string>;
  parameterFields?: ReactNode;
  parametersReady?: boolean;
  onClose: () => void;
  /** Shown under the wizard once an upload is committed. */
  after?: (job: BulkJob) => ReactNode;
}>) {
  const [job, setJob] = useState<BulkJob>();
  return (
    <Card
      title={label}
      actions={
        <Button variant="ghost" onClick={onClose}>
          Close
        </Button>
      }
    >
      <BulkUploadWizard
        handler={handler}
        parameters={parameters}
        parameterFields={parameterFields}
        parametersReady={parametersReady}
        onCommitted={setJob}
      />
      {job !== undefined && after?.(job)}
    </Card>
  );
}
