import { api } from './client';
import type { DownloadedFile } from './client';

export interface DocTemplateVersion {
  id: number;
  code: string;
  versionNo: number;
  title: string;
  body: string;
  effectiveFrom: string;
  active: boolean;
  createdBy: string;
  createdAt: string;
}

export interface DocTemplateRequest {
  title: string;
  body: string;
  effectiveFrom: string;
}

/** A new version read from an edited Word file (nothing saved yet). */
export interface TemplateDraft {
  title: string;
  body: string;
  missingPlaceholders: string[];
  addedPlaceholders: string[];
}

/** Versioned document templates (BRNB.004), downloaded and uploaded as Word (requirement 16). */
export const docTemplatesApi = {
  all: () => api.get<DocTemplateVersion[]>('/doc-templates'),
  newVersion: (code: string, body: DocTemplateRequest) =>
    api.post<DocTemplateVersion>(`/doc-templates/${code}/versions`, body),
  word: (code: string, versionNo: number): Promise<DownloadedFile> =>
    api.getFile(`/doc-templates/${code}/versions/${versionNo}/docx`),
  readWord: (code: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<TemplateDraft>(`/doc-templates/${code}/docx`, form);
  },
};
