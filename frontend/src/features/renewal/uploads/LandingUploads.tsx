import { Download, Upload } from 'lucide-react';
import { useState } from 'react';
import { bulkApi } from '@/api/bulk';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { useCompanyId } from '@/context/workspaceContext';
import { RenewalUploadDialog } from './RenewalUploadDialog';
import { UploadResults } from './UploadResults';
import { UPLOADS } from './uploadMessages';
import type { UploadDefinition } from './uploadMessages';

/**
 * The uploads of the Renewal landing page (FRRN.002.01): LAMD, BDOFC/SOLD, insurer disposition and
 * Renewal Update, with the Renewal Update template, and their outcome summaries.
 */
export function LandingUploads() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const [open, setOpen] = useState<UploadDefinition>();
  const allowed = UPLOADS.filter((u) => can(u.permission));
  if (allowed.length === 0) return null;
  return (
    <>
      <Card title="Uploads">
        <div className="rnw-actions">
          {allowed.map((u) => (
            <Button
              key={u.kind}
              variant="secondary"
              icon={<Upload size={16} />}
              onClick={() => setOpen(u)}
            >
              {u.button}
            </Button>
          ))}
          {can('RNW_UPLOAD') && (
            <Button
              variant="ghost"
              icon={<Download size={16} />}
              onClick={() =>
                download.mutate(() => bulkApi.template('RNW_RENEWAL_UPDATE', companyId))
              }
            >
              Download Template
            </Button>
          )}
        </div>
      </Card>
      <UploadResults uploads={allowed} />
      {open && <RenewalUploadDialog upload={open} onClose={() => setOpen(undefined)} />}
    </>
  );
}
