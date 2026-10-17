import { Download, Printer } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { DownloadedFile } from '@/api/client';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';

/**
 * Print Preview of a print batch (FRS.CSH.02.04.10 to 02.04.15): every AR or OR of the batch in
 * one document, printed with the user's printer set-up or downloaded; when the printer reports an
 * error the user retries or skips to the next batch.
 */
export function PrintPreview({
  file,
  title,
  onClose,
  onSkip,
}: Readonly<{
  file: DownloadedFile;
  title: string;
  onClose: () => void;
  onSkip?: () => void;
}>) {
  const url = useMemo(() => URL.createObjectURL(file.blob), [file]);
  useEffect(() => () => URL.revokeObjectURL(url), [url]);
  const frame = useRef<HTMLIFrameElement>(null);
  const [error, setError] = useState<string>();
  const print = () => {
    setError(undefined);
    try {
      const view = frame.current?.contentWindow;
      if (!view) {
        throw new Error('The printer is not available');
      }
      view.focus();
      view.print();
    } catch (e) {
      setError(`Printer error: ${e instanceof Error ? e.message : String(e)}`);
    }
  };
  return (
    <Modal
      open
      size="lg"
      title={`Print Preview ${title}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            onClick={() => saveFile(file.blob, file.fileName)}
          >
            Download
          </Button>
          <Button variant="accent" icon={<Printer size={16} />} onClick={print}>
            Print
          </Button>
        </>
      }
    >
      {error && (
        <div className="stack" role="alert">
          <p className="field-error">{error}</p>
          <div className="worklist-actions">
            <Button variant="secondary" onClick={print}>
              Retry
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                setError(undefined);
                onSkip?.();
                onClose();
              }}
            >
              Skip
            </Button>
          </div>
        </div>
      )}
      <iframe
        ref={frame}
        title={`Print preview ${title}`}
        src={url}
        className="csh-print-preview"
      />
    </Modal>
  );
}
