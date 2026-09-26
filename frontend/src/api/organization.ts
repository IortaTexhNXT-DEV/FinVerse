import { api, toQuery } from './client';
import type { Branch, Company } from './types';

export const organizationApi = {
  companies: () => api.get<Company[]>('/organization/companies'),
  createCompany: (body: Partial<Company>) => api.post<Company>('/organization/companies', body),
  updateCompany: (id: number, body: Partial<Company>) =>
    api.put<Company>(`/organization/companies/${id}`, body),
  authorizeCompany: (id: number) => api.post<Company>(`/organization/companies/${id}/authorize`),
  branches: (companyId: number) =>
    api.get<Branch[]>(`/organization/branches${toQuery({ companyId })}`),
  createBranch: (body: Partial<Branch>) => api.post<Branch>('/organization/branches', body),
  updateBranch: (id: number, body: Partial<Branch>) =>
    api.put<Branch>(`/organization/branches/${id}`, body),
  authorizeBranch: (id: number) => api.post<Branch>(`/organization/branches/${id}/authorize`),
  deactivateBranch: (id: number) => api.post<Branch>(`/organization/branches/${id}/deactivate`),
};
