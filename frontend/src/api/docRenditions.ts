import { api } from './client';
import type { DownloadedFile } from './client';

/** Whether a downloaded PDF can also be downloaded as Word (client requirement 16). */
export interface WordAvailability {
  available: boolean;
  title?: string;
}

/**
 * SHA-256 of a file as lower-case hex, or undefined where the browser offers no digest (plain
 * HTTP outside localhost).
 *
 * @param blob file
 * @returns 64 hex characters
 */
export async function sha256Hex(blob: Blob): Promise<string | undefined> {
  const subtle = (globalThis as { crypto?: Partial<Crypto> }).crypto?.subtle;
  if (subtle === undefined) {
    return undefined;
  }
  const digest = await subtle.digest('SHA-256', await blob.arrayBuffer());
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
}

/** Word copies of generated documents: every composed PDF is also available as Word. */
export const docRenditionsApi = {
  availability: (sha256: string) => api.get<WordAvailability>(`/doc-renditions/${sha256}`),
  word: (pdf: Blob, fileName: string): Promise<DownloadedFile> => {
    const form = new FormData();
    form.append('file', pdf, fileName);
    return api.download('/doc-renditions/word', form);
  },
};
