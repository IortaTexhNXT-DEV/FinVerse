import { NavLink } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { MODULES } from '@/navigation/modules';

/** Module navigation; only screens the user is permitted to use are shown. */
export function Sidebar() {
  const { can } = useAuth();
  return (
    <nav className="app-sidebar" aria-label="Main navigation">
      {MODULES.map((module) => {
        const visible = module.screens.filter(
          (s) => s.hidden !== true && (s.permission === undefined || can(s.permission)),
        );
        if (visible.length === 0) {
          return null;
        }
        return (
          <div className="nav-section" key={module.id}>
            <div className="nav-section-title">{module.section}</div>
            {visible.map((screen) => {
              const Icon = screen.icon;
              return (
                <NavLink
                  key={screen.path}
                  to={screen.path}
                  end={screen.path === '/'}
                  className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                >
                  <Icon size={17} aria-hidden="true" />
                  {screen.label}
                </NavLink>
              );
            })}
          </div>
        );
      })}
    </nav>
  );
}
