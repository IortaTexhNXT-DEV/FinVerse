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
  /** Windows ID, business unit group and user level (BRD 1.002.1.1.1; UAM-NFR-15). */
  windowsId?: string;
  businessUnitCode?: string;
  userLevel?: string;
  /** Password set by an administrator: to be changed at sign-in (UAM-NFR-36). */
  mustChangePassword?: boolean;
  lastLogoutAt?: string;
  /** Mobile number, maintained by the user on My Profile (UQ17). */
  mobileNo?: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  expiresAt: string;
  user: UserProfile;
  /** The password must be changed before the home page opens (RESET or EXPIRED). */
  mustChangePassword?: boolean;
  passwordChangeReason?: 'RESET' | 'EXPIRED';
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
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
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
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}
