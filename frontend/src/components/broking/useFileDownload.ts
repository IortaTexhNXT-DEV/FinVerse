import { useMutation } from '@tanstack/react-query';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { useToast } from '@/components/ui/toastContext';

/** Downloads a generated file (slip, billing file, advice) and saves it with its server name. */
export function useFileDownload() {
  const toast = useToast();
  return useMutation({
    mutationFn: (fetchFile: () => Promise<DownloadedFile>) => fetchFile(),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
}
