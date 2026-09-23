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
}

export interface UploadPolicy {
  maxSizeBytes: number;
  allowedExtensions: string;
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
  download: (id: number) => api.getFile(`/attachments/${id}/content`),
  remove: (id: number) => api.delete(`/attachments/${id}`),
};
