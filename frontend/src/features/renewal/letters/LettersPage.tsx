import { FileText, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { renewalHoldCoverApi } from '@/api/renewalHoldCover';
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

type Open = 'generate' | 'generateSend' | 'send' | 'closing' | 'expiry';

/**
 * Letters (FR-RN-080-084): Generate Renewal Advice (first or second notice) for the renewals with
 * their terms, Send them protected to the client, send the No Advice (NAL) and Not for
 * Renewal (NFR) letters, send the closing letter of the renewals unrenewed at their effective
 * expiry date (NAL by Operations, NRL by the Account Officer), and follow the renewals with no
 * reply (NRNS). Client acceptances are
 * recorded on the renewal or uploaded here.
 */
/** The letter actions shown on a tab of the Letters screen for the user's permissions. */
function letterButtons(
  tab: string,
  can: (permission: string) => boolean,
): { generate: boolean; send: boolean; expiry: boolean; closing: boolean } {
  return {
    generate: can('RNW_RA_GENERATE') && ['RA_READY', 'RA_SENT', 'NRNS'].includes(tab),
    send: can('RNW_RA_SEND') && tab === 'RA_GENERATED',
    expiry: (tab === 'NAL_DUE' && can('RNW_RA_SEND')) || (tab === 'NRL_DUE' && can('RNW_DISPOSE')),
    closing: can('RNW_RA_GENERATE') && can('RNW_RA_SEND') && tab === 'LETTER_PENDING',
  };
}

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
  const generateSend = useBatchAction<{ notice: 'FIRST' | 'SECOND'; late: boolean }>(
    'Generate and Send RA',
    'generated and sent',
    ({ notice, late }) => renewalApi.generateAndSendRa(companyId, refs, notice, late),
    done,
  );
  const send = useBatchAction<null>('Send', 'sent', () => renewalApi.sendRa(companyId, refs), done);
  const closing = useBatchAction<null>(
    'Send closing letters',
    'sent',
    () => renewalApi.closingLetters(companyId, refs),
    done,
  );
  const atExpiry = useBatchAction<null>(
    'Send closing letters',
    'sent',
    () => renewalHoldCoverApi.closingAtExpiry(companyId, refs),
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
          const shown = letterButtons(tab, can);
          return (
            <span className="rnw-actions">
              {shown.generate && (
                <Button
                  icon={<FileText size={16} />}
                  disabled={none}
                  onClick={() => show('generate', selected, selection.clear)}
                >
                  Generate RA
                </Button>
              )}
              {shown.generate && can('RNW_RA_SEND') && (
                <Button
                  variant="secondary"
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('generateSend', selected, selection.clear)}
                >
                  Generate and Send RA
                </Button>
              )}
              {shown.send && (
                <Button
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('send', selected, selection.clear)}
                >
                  Send
                </Button>
              )}
              {shown.expiry && (
                <Button
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('expiry', selected, selection.clear)}
                >
                  Send Letters
                </Button>
              )}
              {shown.closing && (
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
      {open === 'generateSend' && (
        <GenerateRaDialog
          count={refs.length}
          busy={generateSend.mutation.isPending}
          error={generateSend.mutation.error}
          onClose={close}
          onConfirm={(notice, late) => generateSend.mutation.mutate({ notice, late })}
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
      {open === 'expiry' && (
        <ConfirmDialog
          title="Send closing letters"
          effect={`The closing letter of ${countOf(refs.length, 'unrenewed renewal')} is generated and e-mailed: the No Advice Letter where a Renewal Advice was sent, the Non-Renewal Letter otherwise. A renewal never receives both.`}
          confirmLabel="Send"
          busy={atExpiry.mutation.isPending}
          error={atExpiry.mutation.error}
          onClose={close}
          onConfirm={() => atExpiry.mutation.mutate(null)}
        />
      )}
      {atExpiry.dialog}
      {generate.dialog}
      {generateSend.dialog}
      {send.dialog}
      {closing.dialog}
    </div>
  );
}
