import '@fontsource/nunito/400.css';
import '@fontsource/nunito/600.css';
import '@fontsource/nunito/700.css';
import '@fontsource/nunito/800.css';
import './tokens.css';
import clientLogo from './client-logo.png';
import theme from './theme.json';
import type { ThemePack } from '../../themePack';

/**
 * Theme pack "bdoi": BIBS, the BDOI Broker System, under the BDO Insure logo. The logo comes from
 * the BDOI UX design pack; replace it with the master file from BDO Marketing Communications (never
 * redrawn or altered) when it is supplied.
 */
const pack: ThemePack = { ...theme, clientLogo };

export default pack;
