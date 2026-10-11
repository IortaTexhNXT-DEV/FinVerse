import { Download } from 'lucide-react';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';

/**
 * The recovery codes of the second factor, shown once after an enrolment: each works once when
 * the phone is not at hand. The user keeps them somewhere safe (download as a text file).
 */
export function RecoveryCodes({ codes }: Readonly<{ codes: string[] }>) {
  const download = () =>
    saveFile(new Blob([`${codes.join('\n')}\n`], { type: 'text/plain' }), 'recovery-codes.txt');
  return (
    <div className="stack">
      <Notice tone="warning">
        Keep these recovery codes in a safe place. Each code signs you in once when your phone is
        not at hand. They are shown only now.
      </Notice>
      <ol className="recovery-codes" aria-label="Recovery codes">
        {codes.map((code) => (
          <li key={code}>
            <code>{code}</code>
          </li>
        ))}
      </ol>
      <Button variant="secondary" icon={<Download size={14} />} onClick={download}>
        Download Codes
      </Button>
    </div>
  );
}
