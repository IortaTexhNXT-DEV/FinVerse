import type { Approval, Claim, Movement, PolicyCover } from '@/api/claims';
import { newClaimForm, toClaimInput, validateClaim } from './claimForm';
import {
  currentEstimate,
  estimateTypeLabel,
  lpoNet,
  ourShare,
  outstandingOf,
  runningOutstanding,
  settlementNet,
  settlementSplit,
} from './claimMath';
import { canDecide, claimActions } from './claimWorkflow';

const cover: PolicyCover = {
  policyId: 7,
  policyNo: 'P-FIRE-HO-2026-000001',
  status: 'APPROVED',
  productCode: 'FIRE-COM',
  productName: 'Fire',
  businessLine: 'FIRE',
  customerCode: 'C-000201',
  customerName: 'Luzon Steel',
  insuredName: 'Luzon Steel',
  periodFrom: '2026-01-01',
  periodTo: '2026-12-31',
  currency: 'PHP',
  sharePct: 100,
  coinsuranceLeader: false,
  inForce: true,
  risks: [{ lineNo: 1, description: 'Plant', sumInsured: 1000000 }],
};

const totals = {
  estimateLoss: 1000,
  estimateExpense: 100,
  estimateRecovery: 50,
  paidLoss: 400,
  paidExpense: 0,
  recovered: 10,
  outstandingLoss: 600,
  outstandingExpense: 100,
  recoveryOutstanding: 40,
  ourEstimate: 1100,
  ourPaid: 400,
  ourOutstanding: 700,
  ourRecovered: 10,
};

describe('claim arithmetic', () => {
  it('computes settlement nets, shares and coinsurance splits', () => {
    expect(settlementNet(1000, 50, 25)).toBe(925);
    expect(settlementNet(100, 200)).toBe(0);
    expect(ourShare(333.33, 60)).toBe(200);
    expect(settlementSplit(1000, 60, true)).toEqual({ ours: 600, payable: 1000, coinsurers: 400 });
    expect(settlementSplit(1000, 60, false)).toEqual({ ours: 600, payable: 600, coinsurers: 0 });
    expect(settlementSplit(1000, 100, true)).toEqual({ ours: 1000, payable: 1000, coinsurers: 0 });
    expect(lpoNet(52000, 2000)).toBe(50000);
  });

  it('reads estimates, outstanding and running balances', () => {
    const claim = { totals } as Claim;
    expect(currentEstimate(claim, 'PAYMENT', 'LOSS')).toBe(1000);
    expect(currentEstimate(claim, 'PAYMENT', 'EXPENSE')).toBe(100);
    expect(currentEstimate(claim, 'RECOVERY', 'LOSS')).toBe(50);
    expect(outstandingOf(claim, 'PAYMENT', 'LOSS')).toBe(600);
    expect(outstandingOf(claim, 'PAYMENT', 'EXPENSE')).toBe(100);
    expect(outstandingOf(claim, 'RECOVERY', 'LOSS')).toBe(40);
    expect(estimateTypeLabel(3)).toBe('3 Reversal of Payment');
    expect(estimateTypeLabel(9)).toBe('9');
    const lines = [
      { side: 'PAYMENT', kind: 'ESTIMATE', amount: 1000 },
      { side: 'PAYMENT', kind: 'PAID', amount: 300 },
      { side: 'RECOVERY', kind: 'ESTIMATE', amount: 50 },
      { side: 'PAYMENT', kind: 'ESTIMATE', amount: -200 },
    ] as Movement[];
    expect(runningOutstanding(lines)).toEqual([1000, 700, 700, 500]);
  });
});

describe('claim workflow', () => {
  const all = () => true;
  const makerOnly = (p: string) => p === 'CLAIM_MAINTAIN';

  it('offers maker and checker actions by status', () => {
    const open = claimActions(
      { status: 'OPEN', createdBy: 'claims', ourPaid: 0 },
      true,
      'boss',
      all,
    );
    expect(open).toEqual({
      reserve: true,
      settle: true,
      recover: true,
      lpo: true,
      close: true,
      reopen: false,
      decline: true,
    });
    const closed = claimActions(
      { status: 'CLOSED', createdBy: 'claims', ourPaid: 10 },
      false,
      'boss',
      all,
    );
    expect(closed.reopen).toBe(true);
    expect(closed.recover).toBe(true);
    expect(closed.reserve).toBe(false);
    const own = claimActions(
      { status: 'OPEN', createdBy: 'claims', ourPaid: 0 },
      false,
      'claims',
      all,
    );
    expect(own.close).toBe(false);
    expect(own.lpo).toBe(false);
    const maker = claimActions(
      { status: 'PARTIALLY_SETTLED', createdBy: 'x', ourPaid: 5 },
      true,
      'claims',
      makerOnly,
    );
    expect(maker.settle).toBe(true);
    expect(maker.decline).toBe(false);
  });

  it('lets only another checker decide a pending document', () => {
    const pending = { status: 'PENDING_APPROVAL', submittedBy: 'claims' } as Approval;
    expect(canDecide(pending, 'fmanager', all)).toBe(true);
    expect(canDecide(pending, 'claims', all)).toBe(false);
    expect(canDecide(pending, undefined, all)).toBe(false);
    expect(canDecide({ ...pending, status: 'APPROVED' }, 'fmanager', all)).toBe(false);
    expect(canDecide(pending, 'fmanager', makerOnly)).toBe(false);
  });
});

describe('claim notification form', () => {
  it('validates dates, required fields and cover', () => {
    const form = newClaimForm('2026-05-10');
    expect(validateClaim(form, undefined, '2026-05-10')).toContain('Look up the policy first');
    const complete = {
      ...form,
      policyNo: cover.policyNo,
      natureOfLoss: 'Fire',
      causeOfLoss: 'Short circuit',
      lossLocation: 'Calamba',
      description: 'Warehouse fire',
    };
    expect(validateClaim(complete, cover, '2026-05-10')).toEqual([]);
    expect(
      validateClaim(
        { ...complete, reportedDate: '2026-05-01', lossDate: '2026-05-02' },
        cover,
        '2026-05-10',
      ),
    ).toContain('The notification cannot be before the loss');
    expect(
      validateClaim({ ...complete, reportedDate: '2026-06-01' }, cover, '2026-05-10'),
    ).toContain('The notification date cannot be in the future');
    expect(validateClaim({ ...complete, lossDate: '' }, cover, '2026-05-10')).toHaveLength(1);
    expect(validateClaim(complete, { ...cover, inForce: false }, '2026-05-10')).toHaveLength(1);
    expect(validateClaim({ ...complete, initialLossReserve: -1 }, cover, '2026-05-10')).toContain(
      'Reserves cannot be negative',
    );
    expect(validateClaim({ ...complete, description: ' ' }, cover, '2026-05-10')).toEqual([
      'Describe the loss',
    ]);
  });

  it('maps the form to a registration request', () => {
    const input = toClaimInput(
      {
        ...newClaimForm('2026-05-10'),
        policyNo: 'x',
        natureOfLoss: ' Fire ',
        causeOfLoss: 'Short circuit',
        lossLocation: 'Calamba',
        description: 'Fire',
        claimantCode: '',
        surveyorCode: 'SV-0001',
        garageCode: '',
        initialLossReserve: 1000,
        initialExpenseReserve: 0,
      },
      3,
      cover,
    );
    expect(input.policyNo).toBe(cover.policyNo);
    expect(input.currency).toBe('PHP');
    expect(input.natureOfLoss).toBe('Fire');
    expect(input.claimantCode).toBeUndefined();
    expect(input.parties).toEqual([{ role: 'SURVEYOR', partyCode: 'SV-0001' }]);
    expect(input.initialLossReserve).toBe(1000);
    expect(input.initialExpenseReserve).toBeUndefined();
  });
});
