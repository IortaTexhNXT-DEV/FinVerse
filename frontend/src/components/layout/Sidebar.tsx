import { ChevronDown, ChevronUp } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { mayOpen } from '@/navigation/access';
import { activeMenuPath } from '@/navigation/activeMenu';
import { NAV_GROUPS } from '@/navigation/modules';
import type { FeatureModule, ScreenDef } from '@/navigation/types';
import { Brand } from './Brand';
import { visibleGroups, type VisibleGroup } from './navGroups';

interface SectionProps {
  module: FeatureModule;
  screens: ScreenDef[];
  active: string | undefined;
  showTitle: boolean;
}

function NavSection({ module, screens, active, showTitle }: Readonly<SectionProps>) {
  return (
    <div className="nav-section">
      {showTitle && <div className="nav-section-title">{module.section}</div>}
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
            <Icon size={20} aria-hidden="true" />
            {screen.label}
          </Link>
        );
      })}
    </div>
  );
}

interface GroupProps {
  group: VisibleGroup;
  active: string | undefined;
  open: boolean;
  onToggle: () => void;
}

function NavGroupBlock({ group, active, open, onToggle }: Readonly<GroupProps>) {
  const sections = group.sections.map((s) => (
    <NavSection key={s.module.id} {...s} active={active} showTitle={group.sections.length > 1} />
  ));
  if (group.title === undefined) {
    return <div className="nav-group">{sections}</div>;
  }
  const bodyId = `nav-group-${group.id}`;
  return (
    <div className="nav-group">
      <button
        type="button"
        className="nav-group-toggle"
        aria-expanded={open}
        aria-controls={bodyId}
        onClick={onToggle}
      >
        {group.title}
        {open ? (
          <ChevronUp size={18} aria-hidden="true" />
        ) : (
          <ChevronDown size={18} aria-hidden="true" />
        )}
      </button>
      {open && <div id={bodyId}>{sections}</div>}
    </div>
  );
}

/**
 * Module navigation in collapsible BDOI groups (Client & Policy, Finance ...); only screens the
 * user is permitted to use are shown. The group holding the current page is always open, and
 * exactly one entry is highlighted: the most specific one for the page (see {@link activeMenuPath}).
 */
export function Sidebar() {
  const { can } = useAuth();
  const { pathname } = useLocation();
  const [toggled, setToggled] = useState<Record<string, boolean>>({});
  const groups = visibleGroups(NAV_GROUPS, (s) => mayOpen(s, can));
  const active = activeMenuPath(
    pathname,
    groups.flatMap((g) => g.sections.flatMap((s) => s.screens.map((screen) => screen.path))),
  );

  return (
    <nav className="app-sidebar" aria-label="Main navigation">
      <Brand />
      {groups.map((group) => {
        const holdsActive = group.paths.includes(active ?? '');
        const open = toggled[group.id] ?? holdsActive;
        return (
          <NavGroupBlock
            key={group.id}
            group={group}
            active={active}
            open={open}
            onToggle={() => setToggled((t) => ({ ...t, [group.id]: !open }))}
          />
        );
      })}
    </nav>
  );
}
