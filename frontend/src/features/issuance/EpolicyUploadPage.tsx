import { useMutation } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { BulkEpolicyUpload } from './BulkEpolicyUpload';

function SingleEpolicyUpload({
  companyId,
  initialArn,
}: Readonly<{ companyId: number; initialArn: string }>) {
  const toast = useToast();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [arn, setArn] = useState(initialArn);
  const [policyNo, setPolicyNo] = useState('');
  const receive = useMutation({
    mutationFn: (chosen: File) =>
      issuanceApi.receive(companyId, chosen, arn.trim() || undefined, policyNo.trim() || undefined),
    onSuccess: (e) => {
      toast.success(`${e.fileName} stored on ${e.arn}`);
      void navigate(`/issuance/epolicies/${e.id}`);
    },
  });
  return (
    <Card title="Single E-policy">
      <div className="stack">
        <ErrorAlert error={receive.error} />
        <div className="form-grid">
          <Field label="E-policy PDF" required>
            {(id) => (
              <input
                id={id}
                type="file"
                className="input"
                accept=".pdf"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            )}
          </Field>
          <Field
            label="Account (ARN)"
            hint="Optional: found from the file name or the document when empty."
          >
            {(id) => (
              <input
                id={id}
                className="input"
                value={arn}
                onChange={(e) => setArn(e.target.value)}
              />
            )}
          </Field>
          <Field label="Policy number" hint="Optional: finds an account already issued.">
            {(id) => (
              <input
                id={id}
                className="input"
                value={policyNo}
                onChange={(e) => setPolicyNo(e.target.value)}
              />
            )}
          </Field>
        </div>
        <div className="row">
          <span className="spacer" />
          <Button
            variant="primary"
            icon={<Upload size={16} />}
            busy={receive.isPending}
            disabled={file === null}
            onClick={() => file && receive.mutate(file)}
          >
            Upload and Review
          </Button>
        </div>
      </div>
    </Card>
  );
}

/**
 * E-policy upload (BRNB.073): one e-policy matched by the ARN chosen, the policy number or the ARN
 * in the file, or many at once with a match review. Each file is stored as the account's EPOLICY
 * document and opens an extraction review (document trigger, BRNB.105). Reading the insurers'
 * mailbox or SFTP is not built yet (Q31).
 */
export default function EpolicyUploadPage() {
  const companyId = useCompanyId();
  const [params] = useSearchParams();
  return (
    <div className="stack">
      <PageHeader
        backTo="/issuance"
        section="Policy Issuance"
        title="E-policy Upload"
        description="Store the e-policies received from the insurers on their accounts; the policy data is extracted for review."
      />
      <SingleEpolicyUpload companyId={companyId} initialArn={params.get('arn') ?? ''} />
      <BulkEpolicyUpload companyId={companyId} />
    </div>
  );
}
