import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import type { UploadBatch, UploadItem } from '@/api/issuance';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';

function ArnCell({
  batch,
  item,
  onChange,
}: Readonly<{ batch: UploadBatch; item: UploadItem; onChange: (b: UploadBatch) => void }>) {
  const [arn, setArn] = useState(item.arn ?? '');
  const choose = useMutation({
    mutationFn: (included: boolean) =>
      issuanceApi.chooseItem(batch.id, item.id, arn.trim() || undefined, included),
    onSuccess: onChange,
  });
  if (batch.status !== 'REVIEW') {
    return <code>{item.arn ?? '—'}</code>;
  }
  return (
    <span className="row">
      <input
        className="input"
        aria-label={`Account of ${item.fileName}`}
        placeholder="ARN-yyyy-nnnnnn"
        value={arn}
        onChange={(e) => setArn(e.target.value)}
        onBlur={() => arn.trim() !== (item.arn ?? '') && choose.mutate(true)}
      />
      <label className="checkbox">
        <input
          type="checkbox"
          checked={item.included}
          onChange={(e) => choose.mutate(e.target.checked)}
        />{' '}
        Include
      </label>
      <ErrorAlert error={choose.error} />
    </span>
  );
}

/**
 * Bulk e-policy upload (BRNB.073): many PDFs at once, each matched to its account by the ARN in
 * the file name (e.g. ARN-2026-000123_policy.pdf) or printed in the document; the user corrects
 * the matches, leaves files out and confirms.
 */
export function BulkEpolicyUpload({ companyId }: Readonly<{ companyId: number }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [files, setFiles] = useState<File[]>([]);
  const [batch, setBatch] = useState<UploadBatch | null>(null);
  const upload = useMutation({
    mutationFn: () => issuanceApi.bulkUpload(companyId, files),
    onSuccess: setBatch,
  });
  const confirm = useMutation({
    mutationFn: (id: number) => issuanceApi.confirmBulk(id),
    onSuccess: (b) => {
      setBatch(b);
      toast.success(`${b.items.filter((i) => i.epolicyId !== undefined).length} e-policies stored`);
      void queryClient.invalidateQueries({ queryKey: ['issuance'] });
    },
  });
  const discard = useMutation({
    mutationFn: (id: number) => issuanceApi.discardBulk(id),
    onSuccess: setBatch,
  });
  return (
    <Card title="Bulk Upload">
      <div className="stack">
        <ErrorAlert error={upload.error ?? confirm.error ?? discard.error} />
        <div className="row">
          <Field label="E-policy PDFs" hint="Up to 50 files; name each file after its ARN.">
            {(id) => (
              <input
                id={id}
                type="file"
                className="input"
                accept=".pdf"
                multiple
                onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
              />
            )}
          </Field>
          <Button
            variant="primary"
            icon={<Upload size={16} />}
            busy={upload.isPending}
            disabled={files.length === 0}
            onClick={() => upload.mutate()}
          >
            Upload and Match
          </Button>
        </div>
        {batch && (
          <>
            <div className="row">
              <StatusBadge status={batch.status} />
              <span className="muted">{batch.fileCount} file(s)</span>
              <span className="spacer" />
              {batch.status === 'REVIEW' && (
                <>
                  <Button
                    variant="secondary"
                    busy={discard.isPending}
                    onClick={() => discard.mutate(batch.id)}
                  >
                    Discard
                  </Button>
                  <Button
                    variant="primary"
                    busy={confirm.isPending}
                    onClick={() => confirm.mutate(batch.id)}
                  >
                    Confirm Upload
                  </Button>
                </>
              )}
            </div>
            <DataTable<UploadItem>
              caption="Match review"
              rows={batch.items}
              rowKey={(i) => i.id}
              columns={[
                { key: 'file', header: 'File', render: (i) => i.fileName },
                {
                  key: 'arn',
                  header: 'Account',
                  render: (i) => (
                    <ArnCell
                      key={`${i.id}-${i.arn ?? ''}`}
                      batch={batch}
                      item={i}
                      onChange={setBatch}
                    />
                  ),
                },
                {
                  key: 'how',
                  header: 'Matched By',
                  render: (i) => (i.matchMethod ? humanize(i.matchMethod) : '—'),
                },
                {
                  key: 'outcome',
                  header: 'Detail',
                  render: (i) =>
                    i.epolicyId === undefined ? (
                      (i.outcome ?? i.message ?? '')
                    ) : (
                      <Link to={`/issuance/epolicies/${i.epolicyId}`}>Review extraction</Link>
                    ),
                },
              ]}
            />
          </>
        )}
      </div>
    </Card>
  );
}
