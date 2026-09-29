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

/**
 * Sign-in answer: either the session is open (accessToken, user; the refresh token is an HttpOnly
 * cookie) or the second factor is asked for first (mfaStep with its challenge).
 */
export interface LoginResponse {
  accessToken?: string;
  /** End of the sign-in session (the access token is renewed until then). */
  expiresAt: string;
  user?: UserProfile;
  /** The password must be changed before the home page opens (RESET or EXPIRED). */
  mustChangePassword?: boolean;
  passwordChangeReason?: 'RESET' | 'EXPIRED';
  /** Expiry of the access token; the client renews it shortly before. */
  accessTokenExpiresAt?: string;
  /** VERIFY: enter the code of the authenticator app; ENROL: set up the app first. */
  mfaStep?: 'VERIFY' | 'ENROL';
  /** Proof of the password step, sent back with the code. */
  mfaChallenge?: string;
  /** Days this device may be remembered for the second factor (0 = never). */
  rememberDeviceDays?: number;
  /** Token of this device when it is remembered for the second factor. */
  deviceToken?: string;
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
  /** Client profile: short name in texts and labels. */
  shortName?: string;
  /** Client profile: group the company belongs to, used in labels of group concepts. */
  groupName?: string;
  /** Client profile: logo printed on documents (theme pack logo or file store reference). */
  logoRef?: string;
  /** Client profile: code of the head office in files and for records without a branch. */
  headOfficeCode?: string;
  /** Client profile: bank account code proposed by default. */
  defaultBankCode?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}

/** The client profile part of a company update. */
export interface ClientProfile {
  shortName?: string;
  groupName?: string;
  logoRef?: string;
  headOfficeCode?: string;
  defaultBankCode?: string;
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

/** Where a record comes from: created in BIBS or migrated from a legacy system (BRD-13). */
export type RecordOriginKind = 'BIBS' | 'MIGRATED';

/** Origin fields of a record that may be migrated (flat on the list and record DTOs). */
export interface RecordOriginFields {
  origin?: RecordOriginKind;
  sourceSystem?: string;
  legacyRef?: string;
  migrationBatch?: string;
}
