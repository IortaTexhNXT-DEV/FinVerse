import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { searchHelp } from './helpContent';
import type { HelpScreen } from './helpContent';

function ScreenHelp({ screen }: Readonly<{ screen: HelpScreen }>) {
  return (
    <article className="stack" style={{ gap: 'var(--space-2)' }}>
      <h3 style={{ margin: 0 }}>
        {screen.name}{' '}
        {screen.path !== undefined && (
          <Link to={screen.path} className="muted" style={{ fontSize: 13, fontWeight: 400 }}>
            Open
          </Link>
        )}
      </h3>
      <p style={{ margin: 0 }}>{screen.summary}</p>
      {screen.workflow !== undefined && (
        <div>
          <strong>How it works</strong>
          <ol>
            {screen.workflow.map((w) => (
              <li key={w}>{w}</li>
            ))}
          </ol>
        </div>
      )}
      {screen.controls !== undefined && (
        <div>
          <strong>Controls</strong>
          <ul>
            {screen.controls.map((c) => (
              <li key={c}>{c}</li>
            ))}
          </ul>
        </div>
      )}
    </article>
  );
}

/** In-app help: purpose, workflow and controls of every screen, searchable. */
export default function HelpPage() {
  const [term, setTerm] = useState('');
  const sections = searchHelp(term);
  return (
    <div className="stack">
      <PageHeader
        section="Help"
        title="Help Center"
        description="What each screen is for, how the workflow runs and which controls apply."
        actions={
          <>
            <label className="visually-hidden" htmlFor="help-search">
              Search help
            </label>
            <input
              id="help-search"
              className="input"
              placeholder="Search help…"
              value={term}
              onChange={(e) => setTerm(e.target.value)}
            />
          </>
        }
      />
      {sections.length === 0 && <div className="alert info">No help topic matches “{term}”.</div>}
      {sections.map((section) => (
        <Card key={section.id} title={section.module}>
          <div className="stack">
            <p className="muted" style={{ margin: 0 }}>
              {section.intro}
            </p>
            {section.screens.map((s) => (
              <ScreenHelp key={s.name} screen={s} />
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
