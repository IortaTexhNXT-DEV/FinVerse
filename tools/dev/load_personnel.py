#!/usr/bin/env python3
"""Loads the client's personnel list into a DEV / SIT environment of BIBS.

Each person of the list (an .xlsx with the columns of the BDOI personnel template: Windows log-in,
name, work e-mail, team leader / team head / section head / unit head, Unit/Segment, Department,
Location, Role) becomes a user of the environment the way UIDM-ISC provisions one in production:

  1. the account is added to the Enterprise SSO simulator (user ID = Windows log-in);
  2. the JOINER event creates the user;
  3. the MEMBERSHIP event gives the user the BDOI target-state group profile its role maps to
     (docs/modules/BDOI_TARGET_STATE_PROFILES.md);
  4. the local test password is set through the reset of the test account and changed once in the
     user's own session, so the user is not asked to change it at the first sign-in.

The list holds personal data: it is read from the path given and never copied into the repository.
The plan (--plan) and the report are written where the caller says, outside the repository.

  Dry run (no environment needed):
    load_personnel.py personnel.xlsx --plan /tmp/plan.csv

  Load:
    load_personnel.py personnel.xlsx --base-url https://dev.example --admin Brokerverse \\
        --plan /tmp/plan.csv --user-password-env LOAD_USER_PASSWORD
    (the administrator password is asked for, or read from --admin-password-env; with a second
    factor, pass --mfa-code or a signed-in session token with --token-env)

Requires openpyxl. Runs with the identity simulator on (brokerverse.identity.simulator, seed profile).
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
import getpass
import json
import os
import re
import secrets
import string
import sys
import time
import urllib.error
import urllib.request
from collections import Counter
from dataclasses import dataclass, field

# ------------------------------------------------------------------------------------- mapping
# Role title (with the unit / segment and the department as context) -> group profile. The rules
# are tried in order; the first match wins. Status AMBIGUOUS marks a mapping to be confirmed with
# BDOI before the live load; UNMAPPED rows are not loaded.

HEAD = r"unit head|section head|seciton head|group head|team head|\bth\b"
LEAD = r"team leader|team lead|\btl\b|-team leader"
SENIOR = HEAD + "|" + LEAD


@dataclass(frozen=True)
class Rule:
    name: str
    where: str  # regex on "title | unit | department" (lower case)
    profile: str | None
    title: str | None = None  # regex on the title alone
    status: str = "MAPPED"
    note: str = ""


def tiered(prefix: str, head: str, lead: str, officer: str) -> list[tuple[str, str]]:
    return [(HEAD, f"{prefix}_{head}"), (LEAD, f"{prefix}_{lead}"), (r".", f"{prefix}_{officer}")]


def area(name: str, where: str, prefix: str, head: str, lead: str, officer: str) -> list[Rule]:
    return [Rule(f"{name}/{p.split('_', 2)[-1].lower()}", where, f"BDOI_{p}", title=t)
            for t, p in tiered(prefix, head, lead, officer)]


RULES: list[Rule] = [
    Rule("no business role", r".", None, title=r"executive assistant", status="UNMAPPED",
         note="no matrix role; give a profile by hand if needed"),
    Rule("claims/unit head", r"claims", "BDOI_CLM_UNIT_HEAD", title=r"unit head"),
    *area("claims", r"claims", "CLM", "TEAM_LEAD", "TEAM_LEAD", "OFFICER"),
    Rule("tsu", r"\| tsu$|^tsu |underwriting", "BDOI_TSU_TEAM_HEAD", title=r"team head"),
    Rule("tsu", r"\| tsu$|^tsu |underwriting", "BDOI_TSU_TEAM_LEAD", title=LEAD),
    Rule("tsu", r"\| tsu$|^tsu |underwriting", "BDOI_TSU_OFFICER"),
    Rule("reinsurance", r"reinsurance", "BDOI_RI_HEAD", title=SENIOR),
    Rule("reinsurance", r"reinsurance", "BDOI_RI_OFFICER"),
    Rule("risk management", r"risk", "BDOI_RISK_MGMT"),
    Rule("analytics", r"analytics", "BDOI_ANALYTICS"),
    Rule("tsru head", r"\| tsru \|", "BDOI_TSRU_UNIT_HEAD", title=r"unit head"),
    Rule("trade processing", r"trade process", "BDOI_TRADE_PROCESSOR"),
    Rule("policy review", r"policy review", "BDOI_POLICY_REVIEW"),
    Rule("employee benefits", r"\(eb\)|corporate 6$", "BDOI_EB_MARKETING", title=SENIOR,
         status="AMBIGUOUS", note="EB heads take EB Marketing (checker of the comparative analysis)"),
    Rule("employee benefits", r"\(eb\)|corporate 6$", "BDOI_EB_ACCOUNT_OFFICER"),
    *area("collections", r"collection", "CLX", "TEAM_HEAD", "TEAM_LEAD", "OFFICER"),
    Rule("operations services head", r".", None, title=r"operations services - section head",
         status="UNMAPPED", note="heads cashiering, remittance and adjustment: choose one"),
    Rule("cashiering", r"cashiering", "BDOI_CASH_TEAM_HEAD", title=r"cashiering and remittance",
         status="AMBIGUOUS", note="heads cashiering and remittance; also give BDOI_REMIT_TEAM_HEAD"),
    *area("cashiering", r"cashiering", "CASH", "TEAM_HEAD", "TEAM_LEAD", "OFFICER"),
    *area("remittance", r"remittance", "REMIT", "TEAM_HEAD", "TEAM_LEAD", "OFFICER"),
    *area("adjustment", r"adjustment|reconciliation", "ADJ", "TEAM_HEAD", "TEAM_LEAD", "OFFICER"),
    Rule("mbs", r"\(mbs\) \||business services", "BDOI_MBS"),
    Rule("renewal operations", r"operations . renewal|renewal$|operations renewal|sanitation",
         "BDOI_RENEWAL_HO", title=r"processing|team leader|operations renewal|sanitation"),
    Rule("processing", r"processing|placement & booking|operations ho|\| operations$", "BDOI_PROC_UNIT_HEAD",
         title=SENIOR + r"|operations - unit head|operations team head"),
    Rule("processing", r"processing|placement & booking|operations ho|\| operations$", "BDOI_PROC_OFFICER",
         title=r"processing|operations"),
    Rule("business support", r"business support", "BDOI_MKT_ADMIN", title=r"business support|^admin",
         status="AMBIGUOUS", note="Business Support (Corporate) taken as Marketing Admin"),
    Rule("marketing admin", r".", "BDOI_MKT_ADMIN", title=r"admin"),
    Rule("marketing admin", r"\| admin$", "BDOI_MKT_ADMIN", title=LEAD),
    Rule("marketing head", r"and mbs", "BDOI_MKT_UNIT_HEAD", title=r"unit head", status="AMBIGUOUS",
         note="unit head of marketing and MBS: also give BDOI_MBS if needed"),
    Rule("marketing head", r".", "BDOI_MKT_UNIT_HEAD", title=SENIOR),
    Rule("marketing officer", r".", "BDOI_MKT_OFFICER",
         title=r"account officer|account broker|marketing|\bao\b|support|affinity|digital"),
]


def map_role(title: str, unit: str, department: str) -> tuple[str | None, str, str, str]:
    """(profile, status, rule, note) of a person."""
    t = clean(title).lower()
    where = f"{t} | {clean(unit).lower()} | {clean(department).lower()}"
    for r in RULES:
        if re.search(r.where, where) and (r.title is None or re.search(r.title, t)):
            return r.profile, r.status if r.profile or r.status != "MAPPED" else "UNMAPPED", r.name, r.note
    return None, "UNMAPPED", "", "no rule for this role title"


# ------------------------------------------------------------------------------------- input
COLUMNS = {
    "windows_id": r"windows",
    "host": r"host name",
    "name": r"^name",
    "email": r"e-?mail",
    "team_leader": r"team leader",
    "team_head": r"team head",
    "section_head": r"section head",
    "unit_head": r"unit head",
    "unit": r"unit/segment",
    "department": r"^department",
    "location": r"^location",
    "role": r"^role",
}


def clean(v) -> str:
    return "" if v is None else re.sub(r"\s+", " ", str(v)).strip()


@dataclass
class Person:
    windows_id: str
    name: str
    email: str
    unit: str
    department: str
    location: str
    role: str
    team_leader: str = ""
    team_head: str = ""
    section_head: str = ""
    unit_head: str = ""
    profile: str | None = None
    status: str = ""
    rule: str = ""
    notes: list[str] = field(default_factory=list)

    def names(self) -> tuple[str, str]:
        """(first name, last name) from "Last, Given Middle" or "Given M. Last"."""
        if "," in self.name:
            last, given = (p.strip() for p in self.name.split(",", 1))
            return given.title()[:60], last.title()[:60]
        parts = self.name.split()
        suffix = {"jr", "jr.", "sr", "sr.", "ii", "iii", "iv", "v", "vi"}
        cut = len(parts) - 1
        if cut > 0 and parts[cut].lower() in suffix:
            cut -= 1
        return " ".join(parts[:cut]).title()[:60] or parts[0].title(), " ".join(parts[cut:]).title()[:60]


def read_people(path: str) -> list[Person]:
    try:
        import openpyxl
    except ImportError:
        sys.exit("openpyxl is required: pip install openpyxl")
    ws = openpyxl.load_workbook(path, read_only=True, data_only=True).active
    rows = ws.iter_rows(values_only=True)
    header = [clean(c).lower() for c in next(rows)]
    index = {}
    for key, pattern in COLUMNS.items():
        hits = [i for i, h in enumerate(header) if re.search(pattern, h)]
        if not hits and key in ("windows_id", "name", "email", "role"):
            sys.exit(f"column for {key} not found in {header}")
        index[key] = hits[0] if hits else None
    people = []
    for row in rows:
        if not any(row):
            continue
        get = {k: clean(row[i]) if i is not None and i < len(row) else "" for k, i in index.items()}
        if not get["windows_id"]:
            continue
        get.pop("host", None)
        people.append(Person(**get))
    return people


USER_ID = re.compile(r"^[a-zA-Z][0-9]{9}$")
PASSWORD_COMPLEXITY = re.compile(r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$")
MIN_PASSWORD_LENGTH = 10


def plan(people: list[Person]) -> None:
    seen: Counter = Counter(p.windows_id.lower() for p in people)
    emails: Counter = Counter(p.email.lower() for p in people if p.email)
    for p in people:
        p.profile, p.status, p.rule, note = map_role(p.role, p.unit, p.department)
        if note:
            p.notes.append(note)
        if not USER_ID.match(p.windows_id):
            p.notes.append("Windows log-in does not follow USER_ID_PATTERN (^[a-zA-Z][0-9]{9}$): the joiner is refused")
            p.status = "INVALID"
        if seen[p.windows_id.lower()] > 1:
            p.notes.append("Windows log-in appears more than once")
            p.status = "INVALID"
        if not p.email or "@" not in p.email:
            p.notes.append("no work e-mail")
            p.status = "INVALID"
        elif emails[p.email.lower()] > 1:
            p.notes.append("work e-mail appears more than once")
            p.status = "INVALID"


def write_plan(people: list[Person], path: str) -> None:
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["windows_id", "name", "email", "unit_segment", "department", "location", "role_title",
                    "profile", "status", "rule", "notes"])
        for p in people:
            w.writerow([p.windows_id, p.name, p.email, p.unit, p.department, p.location, p.role,
                        p.profile or "", p.status, p.rule, "; ".join(p.notes)])


def summary(people: list[Person]) -> None:
    by_profile = Counter(p.profile for p in people if p.status in ("MAPPED", "AMBIGUOUS"))
    ambiguous = sum(1 for p in people if p.status == "AMBIGUOUS")
    print(f"{len(people)} people; {sum(by_profile.values())} mapped to a profile"
          f" ({ambiguous} ambiguous, loaded only with --include-ambiguous)")
    for code, n in sorted(by_profile.items()):
        print(f"  {n:4}  {code}")
    by_status = Counter(p.status for p in people)
    print("status: " + ", ".join(f"{k} {v}" for k, v in sorted(by_status.items())))
    for status in ("AMBIGUOUS", "UNMAPPED", "INVALID"):
        titles = Counter((p.role, p.profile or "-", "; ".join(p.notes)) for p in people if p.status == status)
        if titles:
            print(f"{status} role titles:")
            for (title, profile, note), n in sorted(titles.items()):
                print(f"  {n:4}  {title} -> {profile}  ({note})")


# ------------------------------------------------------------------------------------- API
class Api:
    def __init__(self, base: str, token: str | None = None):
        self.base = base.rstrip("/")
        self.token = token
        self.relogin = None  # signs the administrator in again when the session has expired

    def call(self, method: str, path: str, body=None, token: str | None = None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Accept", "application/json")
        if data is not None:
            req.add_header("Content-Type", "application/json")
        bearer = token or self.token
        if bearer:
            req.add_header("Authorization", "Bearer " + bearer)
        for attempt in range(4):
            try:
                status, answer = self._send(req)
                if status == 401 and token is None and self.relogin is not None and attempt == 0:
                    self.token = self.relogin()
                    req.add_header("Authorization", "Bearer " + self.token)
                    continue
                return status, answer
            except (urllib.error.URLError, TimeoutError, ConnectionError) as e:
                if isinstance(e, urllib.error.HTTPError) or attempt == 3:
                    raise
                time.sleep(5 * (attempt + 1))  # dropped connection: wait and send again
        raise RuntimeError("unreachable")

    @staticmethod
    def _send(req):
        try:
            with urllib.request.urlopen(req, timeout=300) as resp:
                raw = resp.read()
                return resp.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                return e.code, json.loads(raw) if raw else None
            except ValueError:
                return e.code, {"message": raw.decode(errors="replace")[:300]}

    def sign_in(self, username: str, password: str, mfa_code: str | None = None) -> dict:
        status, body = self.call("POST", "/api/v1/auth/login", {"username": username, "password": password})
        if status != 200:
            raise RuntimeError(f"sign-in of {username} refused ({status}): {message(body)}")
        if body.get("mfaStep"):
            if not mfa_code:
                raise NeedsSecondFactor(body.get("mfaStep"))
            status, body = self.call("POST", "/api/v1/auth/mfa/verify",
                                     {"challenge": body["mfaChallenge"], "code": mfa_code, "rememberDevice": False})
            if status != 200:
                raise RuntimeError(f"second factor of {username} refused ({status}): {message(body)}")
        return body


class NeedsSecondFactor(RuntimeError):
    pass


def message(body) -> str:
    if isinstance(body, dict):
        return str(body.get("message") or body.get("detail") or body.get("title") or body)
    return str(body)


def temporary_password() -> str:
    alphabet = string.ascii_letters + string.digits
    return "Tmp#" + "".join(secrets.choice(alphabet) for _ in range(14)) + "a1"


def load(people: list[Person], api: Api, user_password: str, request_no: str, reset_existing: bool) -> None:
    status, users = api.call("GET", "/api/v1/admin/users")
    if status != 200:
        raise RuntimeError(f"users not readable ({status}): {message(users)}")
    existing = {u["username"].lower(): u for u in users}
    status, accounts = api.call("GET", "/api/v1/admin/identity/simulator/accounts")
    if status != 200:
        raise RuntimeError(f"identity simulator not available ({status}): {message(accounts)}")
    in_simulator = {a["windowsId"].lower() for a in accounts}

    for p in people:
        if p.status not in ("MAPPED", "AMBIGUOUS"):
            continue
        first, last = p.names()
        account = {
            "windowsId": p.windows_id, "userId": p.windows_id, "email": p.email,
            "firstName": first, "lastName": last, "displayName": f"{first} {last}"[:120],
            "adGroup": "BIBS Users", "status": "ACTIVE",
            "teamLeaderName": p.team_leader[:120] or None, "teamHeadName": p.team_head[:120] or None,
            "sectionHeadName": p.section_head[:120] or None, "unitHeadName": p.unit_head[:120] or None,
            "unitSegment": p.unit[:120] or None, "department": p.department[:120] or None,
            "location": p.location[:120] or None, "uidmRequestNo": request_no,
        }
        q = "?windowsId=" + urllib.request.quote(p.windows_id)
        if p.windows_id.lower() in in_simulator:
            status, body = api.call("PUT", "/api/v1/admin/identity/simulator/accounts" + q, account)
        else:
            status, body = api.call("POST", "/api/v1/admin/identity/simulator/accounts", account)
        if status not in (200, 201):
            p.status, p.notes = "FAILED", p.notes + [f"simulator account: {message(body)}"]
            continue
        created = p.windows_id.lower() not in existing
        if created:
            status, body = api.call("POST", "/api/v1/admin/identity/simulator/accounts/events/JOINER" + q)
            if not applied(status, body):
                p.status, p.notes = "FAILED", p.notes + [f"joiner: {message(body)}"]
                continue
        status, body = api.call("POST", "/api/v1/admin/identity/simulator/accounts/events/MEMBERSHIP" + q,
                                {"groups": [p.profile], "uidmRequestNo": request_no})
        if not applied(status, body):
            p.status, p.notes = "FAILED", p.notes + [f"membership: {message(body)}"]
            continue
        if created or reset_existing:
            set_password(api, p, user_password)
        p.status = "LOADED" if created else "UPDATED"


def applied(status: int, event) -> bool:
    """Whether an identity event was applied (APPLIED, or NO_CHANGE when nothing differed)."""
    return status == 200 and isinstance(event, dict) and event.get("status") in ("APPLIED", "NO_CHANGE")


def set_password(api: Api, p: Person, user_password: str) -> None:
    status, users = api.call("GET", "/api/v1/admin/users")
    user = next((u for u in users or [] if u["username"].lower() == p.windows_id.lower()), None)
    if user is None:
        p.notes.append("user not found after the joiner; password not set")
        return
    temp = temporary_password()
    status, body = api.call("POST", f"/api/v1/admin/users/{user['id']}/reset-password", {"newPassword": temp})
    if status not in (200, 204):
        p.notes.append(f"password reset refused: {message(body)}")
        return
    try:
        session = api.sign_in(p.windows_id, temp)
    except NeedsSecondFactor:
        p.notes.append("second factor asked: password left at the temporary value with a change at first sign-in")
        return
    except RuntimeError as e:
        p.notes.append(str(e))
        return
    status, body = api.call("POST", "/api/v1/auth/change-password",
                            {"currentPassword": temp, "newPassword": user_password}, token=session["accessToken"])
    if status not in (200, 204):
        p.notes.append(f"password change refused: {message(body)}")


# ------------------------------------------------------------------------------------- main
def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("personnel", help="personnel list (.xlsx)")
    ap.add_argument("--plan", required=True, help="CSV of the plan (and of the outcome after a load); keep it outside the repository")
    ap.add_argument("--base-url", help="environment, e.g. https://dev.example (omit for a dry run)")
    ap.add_argument("--admin", default="Brokerverse", help="administrator user ID (USER_MANAGE)")
    ap.add_argument("--admin-password-env", help="environment variable holding the administrator password (else asked)")
    ap.add_argument("--token-env", help="environment variable holding a signed-in access token of the administrator")
    ap.add_argument("--mfa-code", help="current code of the administrator's authenticator app")
    ap.add_argument("--user-password-env", help="environment variable holding the local test password of the loaded users")
    ap.add_argument("--request-no", default="DEV-LOAD-" + dt.date.today().strftime("%Y%m%d"), help="UIDM request number recorded on the events")
    ap.add_argument("--include-ambiguous", action="store_true", help="also load the AMBIGUOUS mappings (default: plan only)")
    ap.add_argument("--reset-existing", action="store_true", help="also set the password of users that already exist")
    args = ap.parse_args()

    people = read_people(args.personnel)
    plan(people)
    summary(people)
    if not args.base_url:
        write_plan(people, args.plan)
        print(f"dry run: plan written to {args.plan}")
        return

    user_password = os.environ.get(args.user_password_env or "", "") or getpass.getpass("Password of the loaded users: ")
    if len(user_password) < MIN_PASSWORD_LENGTH or not PASSWORD_COMPLEXITY.match(user_password):
        sys.exit(f"the password of the users must have {MIN_PASSWORD_LENGTH}+ characters with upper and lower case"
                 " letters, a digit and a symbol (password rules of BIBS); the server would refuse it")
    if not args.include_ambiguous:
        for p in people:
            if p.status == "AMBIGUOUS":
                p.status = "SKIPPED"
                p.notes.append("ambiguous mapping; rerun with --include-ambiguous once confirmed")
    api = Api(args.base_url)
    token = os.environ.get(args.token_env or "", "")
    if token:
        api.token = token
    else:
        password = os.environ.get(args.admin_password_env or "", "") or getpass.getpass(f"Password of {args.admin}: ")
        try:
            api.token = api.sign_in(args.admin, password, args.mfa_code)["accessToken"]
            api.relogin = lambda: api.sign_in(args.admin, password, args.mfa_code)["accessToken"]
        except NeedsSecondFactor:
            sys.exit("the administrator is asked for a second factor: pass --mfa-code or --token-env")
    load(people, api, user_password, args.request_no, args.reset_existing)
    write_plan(people, args.plan)
    outcome = Counter(p.status for p in people)
    print("outcome: " + ", ".join(f"{k} {v}" for k, v in sorted(outcome.items())) + f"; details in {args.plan}")


if __name__ == "__main__":
    main()
