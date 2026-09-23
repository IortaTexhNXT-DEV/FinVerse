import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Underwriting screens (listed in the help centre). */
export const UNDERWRITING_HELP: HelpSection = {
  id: 'underwriting',
  module: 'Underwriting',
  intro:
    'Products, quotations, policies, endorsements and marine open covers. Approving a policy or endorsement books the premium, taxes, commission and coinsurance share and issues the debit or credit note.',
  screens: [
    {
      name: 'Policies',
      path: '/underwriting/policies',
      summary:
        'Register of policies and marine certificates with their status, premium and debit note. Open a policy to see its terms, risks, premium, debit note and endorsement history.',
      workflow: [
        'The underwriter saves the policy as a draft, then submits it for approval.',
        'A checker approves it (optionally on another accounting date) or rejects it back to draft with a reason.',
        'Approval posts the premium and commission journals, records the open items and issues the debit note (DN) to the client and the credit note (CN) to the intermediary.',
        'Changes after approval are made by endorsement (Endorse button on the approved policy).',
      ],
      controls: [
        'Maker-checker: the user who created or submitted a policy cannot approve it. Underwriting applies no authorization limit.',
        'Approval and accounting happen in one transaction: either both succeed or nothing changes.',
        'The exchange rate is the SPOT rate of the accounting date; it is stored on the policy.',
        'Every creation, submission, approval, rejection and discard is recorded in the audit trail.',
      ],
    },
    {
      name: 'New Policy',
      path: '/underwriting/policies/new',
      summary:
        'Enter the policy terms and risks, preview the premium, then save the draft and submit it for approval.',
      workflow: [
        'Choose the product, client, channel (direct, agent or broker) and cover period; the underwriting year is the year the cover starts.',
        'Add one or more risks with sum insured and premium at 100 %; enter discount and loading as a percentage of gross.',
        'For coinsured business enter the company share, the coinsurer and whether the company leads (bills the client 100 %).',
        'Preview shows gross, net, our share, DST, VAT, LGT, FST, premium tax, policy fee, total due and commission.',
      ],
      controls: [
        'Only authorized products can be used. A policy needs at least one risk.',
        'Agent business needs an agent, broker business a broker, and direct business no intermediary.',
        'Commission: the rate entered on the policy (0–100 %), else the intermediary rate, else the product default.',
        'Direct business is written at 100 %; coinsured business needs a coinsurer and a share below 100 %.',
      ],
    },
    {
      name: 'Endorsements',
      summary:
        'Changes to an approved policy, entered from the policy screen and approved like the policy itself. Each endorsement has its own number (policy/E01, E02…) and its own debit or credit note.',
      workflow: [
        'Additional: extra gross premium (e.g. higher sum insured). Refund: return premium, issued with a credit note.',
        'Renewal: new period starting after the current one; premium defaults to the original gross, plus the policy fee.',
        'Cancellation: pro-rata return premium (1/365) from the effective date to the end of the period; approval cancels the policy.',
        'NIL: non-financial change such as an address or description; no journal.',
      ],
      controls: [
        'Only one endorsement per policy may be in draft or pending at a time.',
        'The effective date must fall within the policy period; a renewal’s new period must start after the current one.',
        'Endorsements use the policy terms: share, discount and loading rates, product taxes and the original commission rate.',
      ],
    },
    {
      name: 'Quotations',
      path: '/underwriting/quotations',
      summary:
        'Offers to prospective clients with negotiation iterations, approval and conversion into a draft policy.',
      workflow: [
        'Create the quotation with its first iteration: sum insured, gross premium, discount, loading and charges.',
        'Submit it; a checker approves or rejects it. A new iteration returns it to draft for renegotiation.',
        'Convert an approved quotation while still valid: a draft policy is created with one risk carrying the quoted figures.',
        'Expire lapsed quotations (underwriting maintain permission) marks every open quotation of the company whose validity has passed as EXPIRED now and shows how many were expired; the daily QUOTATION_EXPIRY job does the same for every company.',
      ],
      controls: [
        'The user who prepared or submitted a quotation cannot approve or reject it.',
        'Validity = issue date + validity days (1 to 365); conversion after expiry is refused, and the expiry run (daily job or the Expire lapsed quotations button) marks lapsed quotations EXPIRED. Every run is recorded in the job monitor (Scheduled Jobs).',
        'The discount cannot exceed the gross premium.',
      ],
    },
    {
      name: 'Open Covers',
      path: '/underwriting/open-covers',
      summary:
        'Marine cargo open covers with a limit per shipment, an annual limit and a rate. Open a cover to declare shipments; each declaration creates a marine certificate (MC-…) as a draft policy.',
      workflow: [
        'Create the open cover for a marine product; another user authorizes it.',
        'Declare each shipment (vessel, voyage, sailing date, B/L, L/C, sum insured); the premium defaults to sum insured × cover rate.',
        'Submit the certificate; its approval posts the premium like any policy.',
      ],
      controls: [
        'Only products that allow open covers can be used; only authorized covers accept declarations.',
        'The sailing date must fall in the cover period; each shipment within the limit per shipment.',
        'Draft, pending and approved certificates together may not exceed the annual limit.',
      ],
    },
    {
      name: 'Products',
      path: '/underwriting/products',
      summary:
        'Classes of business: line of business, default commission, UPR basis (1/365, 1/24 or 1/8), tax rates (DST, VAT, LGT, FST, premium tax), policy fee and whether open covers are allowed.',
      controls: [
        'Maker-checker: a new or changed product is pending until another user authorizes it, and cannot be used before.',
        'Tax rates apply to the company’s net premium; the policy fee is charged on new and renewed policies only.',
      ],
    },
  ],
};
