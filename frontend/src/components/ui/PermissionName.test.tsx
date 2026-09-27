import { render, screen } from '@testing-library/react';
import { ebWrapper } from '@/features/eb/testWrapper';
import { PermissionName } from './PermissionName';

describe('permission name', () => {
  it('shows the name and a plain description, never the code', () => {
    render(ebWrapper(new Set())(<PermissionName code="CLIENT_VIEW" />));
    expect(screen.getByText('Client view')).toBeInTheDocument();
    expect(screen.getByText('Open and search client records')).toBeInTheDocument();
    expect(screen.queryByText('CLIENT_VIEW')).not.toBeInTheDocument();
    expect(screen.getByText('Client view').parentElement).not.toHaveAttribute('title');
  });

  it('gives administrators the code in the tooltip only', () => {
    render(ebWrapper(new Set(['ROLE_MANAGE']))(<PermissionName code="CLIENT_VIEW" />));
    expect(screen.queryByText('CLIENT_VIEW')).not.toBeInTheDocument();
    expect(screen.getByText('Client view').parentElement).toHaveAttribute('title', 'CLIENT_VIEW');
  });
});
