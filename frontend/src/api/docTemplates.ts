import { api } from './client';

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

/** Versioned document templates (BRNB.004). */
export const docTemplatesApi = {
  all: () => api.get<DocTemplateVersion[]>('/doc-templates'),
  newVersion: (code: string, body: DocTemplateRequest) =>
    api.post<DocTemplateVersion>(`/doc-templates/${code}/versions`, body),
};
