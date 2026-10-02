import { render, screen, within } from '@testing-library/react';
import { StageStepper } from './StageStepper';
import { stageSteps } from './stageSteps';
import type { StageDef } from './stageSteps';

const QUOTATION: StageDef[] = [
  { code: 'DRAFT', name: 'Draft', initial: true, terminal: false },
  { code: 'FOR_REVIEW', name: 'For review', terminal: false },
  { code: 'APPROVED', name: 'Approved', terminal: false },
  { code: 'SENT_TO_CLIENT', name: 'Sent to client', terminal: false },
  { code: 'ACCEPTED', name: 'Accepted by client', terminal: false },
  { code: 'CONVERTED', name: 'Account created', terminal: true },
  { code: 'NOT_PROCEEDED', name: 'Not proceeded', terminal: true },
  { code: 'VOIDED', name: 'Voided', terminal: true },
];

const states = (code: string, history: { fromStage?: string; toStage: string }[] = []) =>
  stageSteps(QUOTATION, code, history).map((s) => `${s.code}:${s.state}`);

describe('stage steps', () => {
  it('builds the normal path: done, current, then the stages ahead, without side ends', () => {
    expect(states('APPROVED', [{ fromStage: 'FOR_REVIEW', toStage: 'APPROVED' }])).toEqual([
      'DRAFT:done',
      'FOR_REVIEW:done',
      'APPROVED:current',
      'SENT_TO_CLIENT:upcoming',
      'ACCEPTED:upcoming',
      'CONVERTED:upcoming',
    ]);
  });

  it('marks a stage the record was sent back to as returned', () => {
    expect(states('DRAFT', [{ fromStage: 'FOR_REVIEW', toStage: 'DRAFT' }])[0]).toBe(
      'DRAFT:returned',
    );
  });

  it('ends the path at a rejected, voided or not proceeded stage, after where it came from', () => {
    expect(states('VOIDED', [{ fromStage: 'FOR_REVIEW', toStage: 'VOIDED' }])).toEqual([
      'DRAFT:done',
      'FOR_REVIEW:done',
      'VOIDED:ended',
    ]);
  });

  it('completes the path at the normal end', () => {
    expect(states('CONVERTED').at(-1)).toBe('CONVERTED:complete');
    expect(
      states('CONVERTED')
        .slice(0, -1)
        .every((s) => s.endsWith(':done')),
    ).toBe(true);
  });

  it('keeps the first, last, current and neighbouring stages on narrow widths', () => {
    const near = stageSteps(QUOTATION, 'SENT_TO_CLIENT').map((s) => s.near);
    expect(near).toEqual([true, false, true, true, true, true]);
  });
});

describe('optional detour stages', () => {
  const ENDORSEMENT: StageDef[] = [
    { code: 'DRAFT', name: 'Draft', initial: true, terminal: false },
    { code: 'FOR_POSTING', name: 'For Posting', terminal: false },
    { code: 'AWAITING_REAPPLICATION', name: 'Posted - payments to re-apply', terminal: false },
    { code: 'POSTED', name: 'Posted', terminal: true },
  ];
  it('leaves the re-application stage out unless the record is in it', () => {
    expect(stageSteps(ENDORSEMENT, 'FOR_POSTING').map((s) => s.code)).toEqual([
      'DRAFT',
      'FOR_POSTING',
      'POSTED',
    ]);
    const detour = stageSteps(ENDORSEMENT, 'AWAITING_REAPPLICATION', [
      { fromStage: 'FOR_POSTING', toStage: 'AWAITING_REAPPLICATION' },
    ]);
    expect(detour.map((s) => `${s.code}:${s.state}`)).toEqual([
      'DRAFT:done',
      'FOR_POSTING:done',
      'AWAITING_REAPPLICATION:current',
      'POSTED:upcoming',
    ]);
  });
});

describe('StageStepper', () => {
  it('shows every stage, checks the passed ones and highlights the current one', () => {
    render(
      <StageStepper
        steps={stageSteps(QUOTATION, 'FOR_REVIEW', [{ fromStage: 'DRAFT', toStage: 'FOR_REVIEW' }])}
      />,
    );
    const list = screen.getByRole('list', { name: 'Workflow stages' });
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(6);
    expect(items[0]).toHaveClass('is-done');
    expect(items[0]?.querySelector('svg')).not.toBeNull();
    expect(items[1]).toHaveClass('is-current');
    expect(items[1]).toHaveAttribute('aria-current', 'step');
    expect(items[1]).toHaveTextContent('For Review');
    expect(items[2]).toHaveClass('is-upcoming');
    expect(items[2]).toHaveTextContent('3');
  });

  it('collapses the far stages behind an ellipsis marker for narrow widths', () => {
    const { container } = render(<StageStepper steps={stageSteps(QUOTATION, 'ACCEPTED')} />);
    expect(container.querySelectorAll('.stage-step.is-far')).toHaveLength(2);
    expect(container.querySelectorAll('.stage-gap')).toHaveLength(1);
  });

  it('renders nothing without stages', () => {
    const { container } = render(<StageStepper steps={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
