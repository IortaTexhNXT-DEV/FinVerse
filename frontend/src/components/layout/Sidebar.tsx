import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { mayOpen } from '@/navigation/access';
import { activeMenuPath } from '@/navigation/activeMenu';
import { MODULES } from '@/navigation/modules';

/**
 * Module navigation; only screens the user is permitted to use are shown. Exactly one entry is
 * highlighted: the most specific one for the current page (see {@link activeMenuPath}).
 */
export function Sidebar() {
  const { can } = useAuth();
  const { pathname } = useLocation();
  const sections = MODULES.map((module) => ({
    module,
    screens: module.screens.filter((s) => s.hidden !== true && mayOpen(s, can)),
  })).filter((section) => section.screens.length > 0);
  const active = activeMenuPath(
    pathname,
    sections.flatMap((section) => section.screens.map((s) => s.path)),
  );

  return (
    <nav className="app-sidebar" aria-label="Main navigation">
      {sections.map(({ module, screens }) => (
        <div className="nav-section" key={module.id}>
          <div className="nav-section-title">{module.section}</div>
          {screens.map((screen) => {
            const Icon = screen.icon;
            const isActive = screen.path === active;
            return (
              <Link
                key={screen.path}
                to={screen.path}
                aria-current={isActive ? 'page' : undefined}
                className={isActive ? 'nav-link active' : 'nav-link'}
              >
                <Icon size={17} aria-hidden="true" />
                {screen.label}
              </Link>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
