import { useMutation } from '@tanstack/react-query';
import { FileType2, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { PDF_SAVED_EVENT, saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { docRenditionsApi, sha256Hex } from '@/api/docRenditions';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { wordFileName } from '@/features/reports/exportFormats';

const OFFER_MS = 20_000;

interface Offer extends DownloadedFile {
  title: string;
}

/**
 * Offers the Word copy of a generated document right after its PDF is downloaded, on every
 * screen (client requirement 16): slips, letters, statements, forms and advisories are composed
 * once and can be had as PDF or Word with the same content. PDFs that are not composed documents
 * (report exports, merged print batches, the BIR 2307 form) get no offer.
 */
export function WordCopyOffer() {
  const toast = useToast();
  const [offer, setOffer] = useState<Offer | null>(null);

  useEffect(() => {
    let current = 0;
    const onSaved = (event: Event) => {
      const file = (event as CustomEvent<DownloadedFile>).detail;
      const ticket = ++current;
      void (async () => {
        const hash = await sha256Hex(file.blob);
        if (hash === undefined) {
          return;
        }
        const availability = await docRenditionsApi.availability(hash).catch(() => undefined);
        if (ticket === current && availability?.available === true) {
          setOffer({ ...file, title: availability.title ?? file.fileName });
        }
      })();
    };
    globalThis.addEventListener(PDF_SAVED_EVENT, onSaved);
    return () => globalThis.removeEventListener(PDF_SAVED_EVENT, onSaved);
  }, []);

  useEffect(() => {
    if (offer === null) {
      return undefined;
    }
    const timer = setTimeout(() => setOffer(null), OFFER_MS);
    return () => clearTimeout(timer);
  }, [offer]);

  const word = useMutation({
    mutationFn: (o: Offer) => docRenditionsApi.word(o.blob, o.fileName),
    onSuccess: (file, o) => {
      const name = wordFileName(o.fileName);
      saveFile(file.blob, name);
      toast.success(`${name} downloaded`);
      setOffer(null);
    },
    onError: (error) => toast.error(error.message),
  });

  if (offer === null) {
    return null;
  }
  return (
    <div className="word-offer" role="status" aria-live="polite">
      <FileType2 size={20} aria-hidden="true" className="word-offer-icon" />
      <div className="word-offer-text">
        <strong>{offer.title}</strong>
        <span className="muted">{offer.fileName} is also available in Word.</span>
      </div>
      <Button size="sm" busy={word.isPending} onClick={() => word.mutate(offer)}>
        Download Word
      </Button>
      <Button size="sm" variant="ghost" aria-label="Dismiss" onClick={() => setOffer(null)}>
        <X size={14} aria-hidden="true" />
      </Button>
    </div>
  );
}
