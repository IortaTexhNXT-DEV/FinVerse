import '@fontsource/nunito/400.css';
import '@fontsource/nunito/600.css';
import '@fontsource/nunito/700.css';
import '@fontsource/nunito/800.css';
import './tokens.css';
import clientLogo from './client-logo.png';
import loginPhoto from './login-photo.jpg';
import theme from './theme.json';
import type { ThemePack } from '../../themePack';

/**
 * Theme pack "bdoi": BIBS, the BDOI Broker System, under the BDO Insure logo. The logo and the
 * sign-in photo come from the BDOI UX design pack; replace them with the master files from BDO
 * Marketing Communications (never redrawn or altered) when they are supplied.
 */
const pack: ThemePack = { ...theme, clientLogo, loginPhoto };

export default pack;
