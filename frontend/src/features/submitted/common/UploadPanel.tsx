import type { ReactNode } from 'react';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

/** A bulk upload of a Submitted Policies screen, shown under the header while open. */
export function UploadPanel({
  label,
  handler,
  parameters,
  parameterFields,
  parametersReady,
  onClose,
  onCommitted,
}: Readonly<{
  label: string;
  handler: string;
  parameters?: Record<string, string>;
  parameterFields?: ReactNode;
  parametersReady?: boolean;
  onClose: () => void;
  onCommitted?: () => void;
}>) {
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
        onCommitted={() => onCommitted?.()}
      />
    </Card>
  );
}
