import { render, screen } from '@testing-library/react';
import { setUserDirectory } from '@/api/users';
import { UserNames } from './UserNames';

describe('user names', () => {
  afterEach(() => setUserDirectory([]));

  it('names each user in order with the login in the tooltip and skips empty ones', () => {
    setUserDirectory([
      { username: 'migsteward', displayName: 'Sofia Steward' },
      { username: 'migowner', displayName: 'Oscar Owner' },
    ]);
    const { container } = render(<UserNames logins={['migsteward', undefined, 'migowner']} />);
    expect(container.textContent).toBe('Sofia Steward / Oscar Owner');
    expect(screen.getByText('Oscar Owner')).toHaveAttribute('title', 'migowner');
  });

  it('names the platform sign-ins by what they do, not by their login', () => {
    const { container } = render(<UserNames logins={['mig-loader', 'SYSTEM']} />);
    expect(container.textContent).toBe('Data Migration / System');
  });

  it('renders nothing without users', () => {
    const { container } = render(<UserNames logins={[null, '']} />);
    expect(container.textContent).toBe('');
  });
});
