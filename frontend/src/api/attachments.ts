import { api, toQuery } from './client';

export interface AttachmentInfo {
  id: number;
  entityType: string;
  entityId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  description?: string;
  uploadedBy: string;
  uploadedAt: string;
  /** Document type code (list DOCUMENT_TYPE). */
  documentType?: string;
  /** Shown here because it is linked from another record (not uploaded to this one). */
  linked?: boolean;
}

export interface UploadOptions {
  description?: string;
  documentType?: string;
  /** INHERIT keeps the file name; NOMINATE names it <REFERENCE>_<DOCTYPE>_<n>. */
  naming?: 'INHERIT' | 'NOMINATE';
  reference?: string;
}

export interface LinkedRecord {
  entityType: string;
  entityId: string;
}

export interface UploadPolicy {
  maxSizeBytes: number;
  allowedExtensions: string;
  namingSyntax?: string;
  maxFiles?: number;
}

function uploadForm(entityType: string, entityId: string, options: UploadOptions): FormData {
  const form = new FormData();
  form.append('entityType', entityType);
  form.append('entityId', entityId);
  Object.entries(options).forEach(([key, value]) => {
    if (typeof value === 'string' && value !== '') {
      form.append(key, value);
    }
  });
  return form;
}

export const attachmentsApi = {
  list: (entityType: string, entityId: string) =>
    api.get<AttachmentInfo[]>(`/attachments${toQuery({ entityType, entityId })}`),
  policy: () => api.get<UploadPolicy>('/attachments/policy'),
  upload: (entityType: string, entityId: string, file: File, description?: string) => {
    const form = new FormData();
    form.append('entityType', entityType);
    form.append('entityId', entityId);
    if (description) {
      form.append('description', description);
    }
    form.append('file', file);
    return api.upload<AttachmentInfo>('/attachments', form);
  },
  /** Uploads several files at once with one document type and naming choice. */
  uploadMany: (entityType: string, entityId: string, files: File[], options: UploadOptions) => {
    const form = uploadForm(entityType, entityId, options);
    files.forEach((file) => form.append('files', file));
    return api.upload<AttachmentInfo[]>('/attachments/batch', form);
  },
  /** Links an uploaded file to more records (it is stored once). */
  link: (id: number, records: LinkedRecord[]) =>
    api.post<AttachmentInfo>(`/attachments/${id}/links`, { records }),
  /** Downloads the chosen files as one ZIP archive. */
  zip: (ids: number[], name: string) =>
    api.getFile(`/attachments/zip${toQuery({ ids: ids.join(','), name })}`),
  /** Removes a file, or only its link when it was linked from another record. */
  removeFrom: (id: number, entityType: string, entityId: string) =>
    api.delete(`/attachments/${id}${toQuery({ entityType, entityId })}`),
  download: (id: number) => api.getFile(`/attachments/${id}/content`),
  remove: (id: number) => api.delete(`/attachments/${id}`),
};
