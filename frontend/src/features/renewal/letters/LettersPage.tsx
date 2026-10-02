import { FileText, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf } from '@/utils/format';
import { CandidateList } from '../common/CandidateList';
import { GenerateRaDialog } from '../common/MoreDialogs';
import { LETTER_TABS, RENEWAL_SECTION, tabOf } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

type Open = 'generate' | 'send' | 'closing';

/**
 * Letters (FR-RN-080-084): Generate Renewal Advice (first or second notice) for the renewals with
 * their terms, Send them protected to the client, send the No Advice (NAL) and Not for
 * Renewal (NFR) letters, and follow the renewals with no reply (NRNS). Client acceptances are
 * recorded on the renewal or uploaded here.
 */
export default function LettersPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [params] = useSearchParams();
  const [upload, setUpload] = useState(false);
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const generate = useBatchAction<{ notice: 'FIRST' | 'SECOND'; late: boolean }>(
    'Generate Renewal Advice',
    'generated',
    ({ notice, late }) => renewalApi.generateRa(companyId, refs, notice, late),
    done,
  );
  const send = useBatchAction<null>('Send', 'sent', () => renewalApi.sendRa(companyId, refs), done);
  const closing = useBatchAction<null>(
    'Send closing letters',
    'sent',
    () => renewalApi.closingLetters(companyId, refs),
    done,
  );
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Letters"
        description="Renewal Advices, No Advice and Not for Renewal letters, and the replies."
        actions={
          can('RNW_ACCEPT') && (
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload Acceptances
            </Button>
          )
        }
      />
      {upload && (
        <UploadPanel
          label="Upload Acceptances"
          handler="RNW_ACCEPTANCE"
          onClose={() => setUpload(false)}
        />
      )}
      <CandidateList
        tabs={LETTER_TABS}
        initialTab={tabOf(LETTER_TABS, params.get('tab'))}
        actions={(selected, selection, tab) => {
          const none = selected.length === 0;
          return (
            <span className="rnw-actions">
              {can('RNW_RA_GENERATE') &&
                (tab === 'RA_READY' || tab === 'RA_SENT' || tab === 'NRNS') && (
                  <Button
                    icon={<FileText size={16} />}
                    disabled={none}
                    onClick={() => show('generate', selected, selection.clear)}
                  >
                    Generate RA
                  </Button>
                )}
              {can('RNW_RA_SEND') && tab === 'RA_GENERATED' && (
                <Button
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('send', selected, selection.clear)}
                >
                  Send
                </Button>
              )}
              {can('RNW_RA_GENERATE') && can('RNW_RA_SEND') && tab === 'LETTER_PENDING' && (
                <Button
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('closing', selected, selection.clear)}
                >
                  Send Letters
                </Button>
              )}
            </span>
          );
        }}
      />
      {open === 'generate' && (
        <GenerateRaDialog
          count={refs.length}
          busy={generate.mutation.isPending}
          error={generate.mutation.error}
          onClose={close}
          onConfirm={(notice, late) => generate.mutation.mutate({ notice, late })}
        />
      )}
      {open === 'send' && (
        <ConfirmDialog
          title="Send Renewal Advice"
          effect={`The Renewal Advice of ${countOf(refs.length, 'renewal')} is e-mailed protected to the client.`}
          confirmLabel="Send"
          busy={send.mutation.isPending}
          error={send.mutation.error}
          onClose={close}
          onConfirm={() => send.mutation.mutate(null)}
        />
      )}
      {open === 'closing' && (
        <ConfirmDialog
          title="Send closing letters"
          effect={`The No Advice or Not for Renewal letter of ${countOf(refs.length, 'renewal')} is generated and e-mailed; the renewals are closed.`}
          confirmLabel="Send"
          busy={closing.mutation.isPending}
          error={closing.mutation.error}
          onClose={close}
          onConfirm={() => closing.mutation.mutate(null)}
        />
      )}
      {generate.dialog}
      {send.dialog}
      {closing.dialog}
    </div>
  );
}
