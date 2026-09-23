/** Types shared across modules, mirroring the backend DTOs. */

export type RecordStatus = 'PENDING_AUTHORIZATION' | 'ACTIVE' | 'INACTIVE';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserProfile {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  enabled: boolean;
  locked: boolean;
  homeBranchId?: number;
  authorizationLimit?: number;
  lastLoginAt?: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  expiresAt: string;
  user: UserProfile;
}

export interface Company {
  id: number;
  code: string;
  name: string;
  baseCurrency: string;
  taxId?: string;
  address?: string;
  fiscalYearStartMonth: number;
  backValueDays: number;
  forwardValueDays: number;
  retainedEarningsAccount?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export interface Branch {
  id: number;
  companyId: number;
  code: string;
  name: string;
  region?: string;
  address?: string;
  openingDate: string;
  headOffice: boolean;
  forexAuthorized: boolean;
  contactPhone?: string;
  contactEmail?: string;
  managerName?: string;
  weeklyHolidays?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}
