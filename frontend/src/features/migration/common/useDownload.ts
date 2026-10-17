import type { DownloadedFile } from '@/api/client';
import { useFileDownload } from '@/components/broking/useFileDownload';

/** Downloads a file of the migration console (templates, rejects, extracts, evidence). */
export function useDownload() {
  const download = useFileDownload();
  return {
    busy: download.isPending,
    error: download.error,
    run: (fetchFile: () => Promise<DownloadedFile>) => download.mutate(fetchFile),
  };
}
