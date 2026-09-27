/** True when the location is the menu path itself or one of its sub-pages (detail, edit…). */
function covers(menuPath: string, pathname: string): boolean {
  if (menuPath === '/') {
    return pathname === '/';
  }
  return pathname === menuPath || pathname.startsWith(`${menuPath}/`);
}

/**
 * The one menu entry to highlight for a location: the most specific entry that covers it. On
 * `/gl/journals/new` that is "New Journal" and not "Journals" as well, while a journal's detail
 * page `/gl/journals/12` still highlights "Journals".
 */
export function activeMenuPath(pathname: string, menuPaths: readonly string[]): string | undefined {
  let best: string | undefined;
  menuPaths.forEach((p) => {
    if (covers(p, pathname) && (best === undefined || p.length > best.length)) {
      best = p;
    }
  });
  return best;
}
