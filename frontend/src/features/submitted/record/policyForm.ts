import type { PolicyData } from '@/api/submitted';

/** The default segment of a policy created by hand (FR-SP-003: Non-CBG Corporate). */
const DEFAULT_SEGMENT = 'NONCBG_CORPORATE';

/**
 * An empty policy for New Policy.
 *
 * @param currency base currency of the company
 */
export function emptyPolicy(currency: string): PolicyData {
  return {
    segment: DEFAULT_SEGMENT,
    businessType: 'NB',
    loan: {
      pnNo: null,
      loanApplicationNo: null,
      cif: null,
      valueDate: null,
      maturityDate: null,
      referringBranch: null,
      originatingUnit: null,
      borrowerName: null,
    },
    assured: {
      assuredName: '',
      mailingAddress: null,
      telephone: null,
      mobile: null,
      email: null,
      bankCounterpartEmail: null,
    },
    terms: {
      insurerCode: null,
      policyNo: null,
      inceptionDate: null,
      expiryDate: '',
      coverageDays: null,
      sumInsured: null,
      totalPremium: null,
      currency,
    },
    risk: {
      unitDescription: null,
      serialNo: null,
      motorNo: null,
      colour: null,
      plateNo: null,
      vehicleType: null,
      vehicleYear: null,
      propertyLocation: null,
      occupancy: null,
      mortgagee: null,
    },
    marks: { ffy: false, employeeAccount: false, noTouch: false },
  };
}

/** A typed text, null when blank. */
export const textOrNull = (v: string): string | null => (v.trim() === '' ? null : v);

/** A nullable text or number as the value of an input. */
export const shown = <T>(v: T | null): T | undefined => v ?? undefined;
