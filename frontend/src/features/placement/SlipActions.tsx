import { useMutation, useQuery } from '@tanstack/react-query';
import { FileSpreadsheet, FileText, RefreshCw, Send } from 'lucide-react';
import { useState } from 'react';
import { placementApi } from '@/api/placement';
import type { Slip, SlipEmail } from '@/api/placement';
import { useAuth } from '@/auth/authContext';
import { SendEmailDialog } from '@/components/broking/SendEmailDialog';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';

function SendSlipDialog({
  slip,
  onClose,
  onSent,
}: Readonly<{ slip: Slip; onClose: () => void; onSent: () => void }>) {
  const toast = useToast();
  const draft = useQuery({
    queryKey: ['placement', 'slip-draft', slip.id],
    queryFn: () => placementApi.slipDraft(slip.id),
  });
  const send = useMutation({
    mutationFn: (email: SlipEmail) => placementApi.sendSlip(slip.id, email),
    onSuccess: (sent) => {
      toast.success(`${sent.displayNo} sent to the insurer`);
      onSent();
    },
  });
  if (draft.data === undefined) {
    return <ErrorAlert error={draft.error} />;
  }
  return (
    <SendEmailDialog
      title={`${slip.status === 'SENT' ? 'Resend' : 'Send'} ${slip.displayNo} to the Insurer`}
      initial={{ ...draft.data, to: draft.data.to.join(', '), cc: draft.data.cc.join(', ') }}
      attachmentNames={[`${slip.displayNo}.pdf`, `${slip.displayNo}.xlsx`]}
      protectionOptional
      busy={send.isPending}
      error={send.error}
      onClose={onClose}
      onSend={(d) =>
        send.mutate({ to: d.to, cc: d.cc, subject: d.subject, body: d.body, protect: d.protect })
      }
    />
  );
}

/**
 * Actions on a placement slip (BRNB.069/071): download the PDF and Excel files, send or resend it
 * to the insurer's placement mailbox (optionally password protected) and regenerate it after a
 * return (new version, the old one kept).
 */
export function SlipActions({ slip, onChanged }: Readonly<{ slip: Slip; onChanged: () => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const [sending, setSending] = useState(false);
  const download = useFileDownload();
  const regenerate = useMutation({
    mutationFn: () => placementApi.regenerateSlip(slip.id),
    onSuccess: (next) => {
      toast.success(`${next.displayNo} generated`);
      onChanged();
    },
  });
  const manage = can('PLACEMENT_MANAGE') && slip.status !== 'SUPERSEDED';
  return (
    <span className="row">
      <Button
        size="sm"
        variant="ghost"
        icon={<FileText size={14} />}
        onClick={() => download.mutate(() => placementApi.slipFile(slip.id, 'pdf'))}
      >
        PDF
      </Button>
      <Button
        size="sm"
        variant="ghost"
        icon={<FileSpreadsheet size={14} />}
        onClick={() => download.mutate(() => placementApi.slipFile(slip.id, 'xlsx'))}
      >
        Excel
      </Button>
      {manage && (
        <Button
          size="sm"
          variant="secondary"
          icon={<Send size={14} />}
          onClick={() => setSending(true)}
        >
          {slip.status === 'SENT' ? 'Resend' : 'Send'}
        </Button>
      )}
      {manage && slip.status === 'SENT' && (
        <Button
          size="sm"
          variant="ghost"
          icon={<RefreshCw size={14} />}
          busy={regenerate.isPending}
          onClick={() => regenerate.mutate()}
        >
          Regenerate
        </Button>
      )}
      <ErrorAlert error={download.error ?? regenerate.error} />
      {sending && (
        <SendSlipDialog
          slip={slip}
          onClose={() => setSending(false)}
          onSent={() => {
            setSending(false);
            onChanged();
          }}
        />
      )}
    </span>
  );
}
