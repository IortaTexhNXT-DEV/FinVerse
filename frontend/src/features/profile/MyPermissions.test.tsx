import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { permissionLabel } from '@/utils/permissionLabel';
import { MyPermissions } from './MyPermissions';

describe('MyPermissions', () => {
  it('shows the permissions in words, grouped by area with counts', () => {
    const { container } = render(
      <MyPermissions
        permissions={['BCL_VIEW', 'CLX_SETUP', 'MFA_RESET', 'PRQ_VIEW', 'SCR_VIEW']}
      />,
    );
    expect(container.querySelectorAll('details').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/\(\d+\)$/)).toHaveLength(
      container.querySelectorAll('details').length,
    );
    const text = container.textContent;
    for (const code of ['Bcl View', 'Clx Setup', 'Mfa Reset', 'Prq View', 'Scr View']) {
      expect(text).not.toContain(code);
    }
    expect(text).toContain('Collections setup');
    expect(text).toContain('Second factor reset');
  });

  it('names every module prefix in words', () => {
    expect(permissionLabel('DISB_VIEW')).toBe('Disbursement view');
    expect(permissionLabel('SBM_VIEW')).toBe('Submitted policies view');
    expect(permissionLabel('PRQ_HR_APPROVE')).toBe('Package requests HR approve');
  });
});
