"""Content of the User Access interface specification (BRD-11): SCIM 2.0 provisioning from UIDM-ISC and OpenID
Connect sign-in with Microsoft Entra ID. Read by build_brd11_interface_spec.py, which writes the Word document, the
OpenAPI description and the interface test pack from these data.

Every rule here follows the BIBS provisioning service as configured for BDOI (identity module, integration security,
single sign-on settings). Keep the examples fictitious.
"""

from __future__ import annotations

import json

VERSION = "1.1"
ISSUE_DATE = "16 Oct 2026"
DOC_NO = "ICD-BRD-11-UA"
STEM = "BIBS_ICD-BDOI_BRD-11"
DOCX = f"{STEM}_User_Access_Interface_Specification_v{VERSION}.docx"
OPENAPI = f"{STEM}_SCIM_OpenAPI_v{VERSION}.yaml"
XLSX = f"{STEM}_Interface_Test_Pack_v{VERSION}.xlsx"
TITLE = ("Interface Specification - User Access (SCIM 2.0 with UIDM-ISC, OpenID Connect with Microsoft Entra ID)")
HEADER_LEFT = "Interface Specification"
HEADER_RIGHT = "BIBS - User Access Interfaces"
AUTHOR = "iorta TechNXT Project Team"

BASE_PATH = "integration/v1/scim/v2"
REDIRECT_PATH = "api/v1/auth/sso/oidc/callback"

CORE = "urn:ietf:params:scim:schemas:core:2.0:User"
ENTERPRISE = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
BIBS = "urn:bibs:params:scim:schemas:extension:bdo:2.0:User"
GROUP = "urn:ietf:params:scim:schemas:core:2.0:Group"
LIST = "urn:ietf:params:scim:api:messages:2.0:ListResponse"
PATCHOP = "urn:ietf:params:scim:api:messages:2.0:PatchOp"
ERROR = "urn:ietf:params:scim:api:messages:2.0:Error"

# ----------------------------------------------------------------------------------------------- examples
JOINER = {
    "schemas": [CORE, ENTERPRISE, BIBS],
    "externalId": "BDOI\\mbsantos",
    "userName": "mbsantos",
    "name": {"givenName": "Maria", "familyName": "Santos"},
    "displayName": "Maria B. Santos",
    "emails": [{"value": "maria.santos@bdoinsure.example", "primary": True}],
    "active": True,
    ENTERPRISE: {"department": "Claims", "division": "Non-Life Claims"},
    BIBS: {
        "windowsId": "BDOI\\mbsantos",
        "adGroup": "GG-BIBS-CLAIMS",
        "adStatus": "ACTIVE",
        "teamLeaderName": "Jose Reyes",
        "teamHeadName": "Ana Cruz",
        "sectionHeadName": "Paolo Garcia",
        "unitHeadName": "Liza Mendoza",
        "unitSegment": "Non-Life Claims",
        "location": "Makati Head Office",
        "uidmRequestNo": "UIDM-2026-004512",
    },
    "groups": [{"value": "CLM_OFFICER"}],
}


def user_answer(groups=("CLM_OFFICER",), active=True, status="ACTIVE", request="UIDM-2026-004512",
                department="Claims", team_leader="Jose Reyes"):
    ext = dict(JOINER[BIBS])
    ext.update({"adStatus": status, "uidmRequestNo": request, "teamLeaderName": team_leader,
                "groupProfiles": list(groups)})
    return {
        "schemas": [CORE, ENTERPRISE, BIBS],
        "id": "mbsantos",
        "externalId": "BDOI\\mbsantos",
        "userName": "mbsantos",
        "displayName": "Maria B. Santos",
        "active": active,
        "name": {"givenName": "Maria", "familyName": "Santos"},
        "emails": [{"value": "maria.santos@bdoinsure.example", "primary": True}],
        ENTERPRISE: {"department": department, "division": "Non-Life Claims"},
        BIBS: ext,
        "groups": [{"value": g} for g in groups],
    }


MOVER = json.loads(json.dumps(JOINER))
MOVER[BIBS].update({"teamLeaderName": "Carla Lim", "uidmRequestNo": "UIDM-2026-004790"})
MOVER["groups"] = [{"value": "CLM_TL"}]

STATUS_PATCH = {"schemas": [PATCHOP], "Operations": [{"op": "replace", "path": "active", "value": False}]}
AD_STATUS_PATCH = {"schemas": [PATCHOP],
                   "Operations": [{"op": "replace", "path": f"{BIBS}:adStatus", "value": "LOCKED"}]}
REHIRE_PATCH = {"schemas": [PATCHOP], "Operations": [{"op": "replace", "path": "active", "value": True}]}

GROUP_ADD = {"schemas": [PATCHOP], "Operations": [
    {"op": "add", "path": "members", "value": [{"value": "mbsantos"}, {"value": "jdelacruz"}]}]}
GROUP_REMOVE = {"schemas": [PATCHOP], "Operations": [
    {"op": "remove", "path": "members[value eq \"jdelacruz\"]"}]}
GROUP_REPLACE = {"schemas": [PATCHOP], "Operations": [
    {"op": "replace", "path": "members", "value": [{"value": "mbsantos"}]}]}

GROUP_ANSWER = {
    "schemas": [GROUP],
    "id": "CLM_OFFICER",
    "displayName": "Claims Officer",
    "active": True,
    "members": [{"value": "mbsantos"}, {"value": "jdelacruz"}],
    "permissions": ["CLAIM_VIEW", "CLAIM_REGISTER", "CLAIM_DOCUMENT_UPLOAD"],
}


def list_of(items):
    return {"schemas": [LIST], "totalResults": len(items), "itemsPerPage": len(items), "startIndex": 1,
            "Resources": items}


def error(status, detail, scim_type=None):
    e = {"schemas": [ERROR], "status": str(status)}
    if scim_type:
        e["scimType"] = scim_type
    e["detail"] = detail
    return e


ERR_UNKNOWN_GROUP = error(404, "Event 18452: No group profile CLM_SUPERVISOR")
ERR_SOD = error(400, "Event 18460: One user may not hold both CLM_OFFICER and CLM_TL (separation-of-duties rule "
                     "SOD-004)", "invalidValue")
ERR_TAKEN = error(409, "Event 18471: Windows ID BDOI\\mbsantos belongs to another user", "uniqueness")
ERR_ATTRIBUTE = error(400, "Attribute nickName is not supported", "invalidValue")
ERR_PATH = error(400, "Only the members of a group profile change", "invalidPath")

# ----------------------------------------------------------------------------------------------- operations
# (key, method, path, purpose, success status, BIBS event type)
OPERATIONS = [
    ("create", "POST", "Users", "Joiner: create the user; groups, when present, become the user's group "
     "profiles", "201", "Joiner"),
    ("list", "GET", "Users", "Extract of every user with group profiles (access certification, "
     "reconciliation)", "200", "-"),
    ("read", "GET", "Users/{user-id}", "One user, by BIBS user ID or Windows ID", "200", "-"),
    ("replace", "PUT", "Users/{user-id}", "Mover: replace the user's details; groups, when present, replace "
     "the group profiles", "200", "Mover"),
    ("patch", "PATCH", "Users/{user-id}", "Status change: active false or true, or the AD status of the "
     "extension", "200", "Status change or Rehire"),
    ("delete", "DELETE", "Users/{user-id}", "Leaver: deactivate the user and end the open sessions; the user "
     "and the history stay", "204", "Leaver"),
    ("groups", "GET", "Groups", "Extract of every group profile with permissions and members", "200", "-"),
    ("group", "GET", "Groups/{group-id}", "One group profile, by group profile code", "200", "-"),
    ("membership", "PATCH", "Groups/{group-id}", "Add, remove or replace the members of a group profile",
     "200", "Membership (one per user changed)"),
]

# per-operation details: request notes, request example, answer example, refusals
OPERATION_DETAIL = {
    "create": {
        "notes": [
            "Send one User per call. userName is the BIBS user ID; externalId or the extension windowsId is the "
            "Windows ID (AD account). Both are unique in BIBS.",
            "The user is created active. The status follows the extension adStatus or, when absent, active.",
            "groups is optional. When present, every group profile is checked (exists, is active, no "
            "separation-of-duties conflict, no conflicting permissions) before anything is recorded. When one "
            "check fails, the user is not created.",
            "uidmRequestNo carries the UIDM request number. It is written to the access change log as the "
            "reference of every change.",
        ],
        "request": JOINER,
        "answer": ("201 Created", user_answer()),
        "refusals": ["409 uniqueness: the user ID already exists or the Windows ID belongs to another user",
                     "404: a group profile of groups does not exist",
                     "400 invalidValue: an attribute outside the agreed schema, a value too long or with "
                     "characters not allowed, an inactive group profile, a separation-of-duties conflict"],
    },
    "list": {
        "notes": ["Returns every user that holds a Windows ID, with the group profiles held in BIBS. "
                  "No filter, sort or paging parameters are read: the whole list comes back in one answer "
                  "(about 530 users).",
                  "active is true only for the AD status ACTIVE."],
        "request": None,
        "answer": ("200 OK", list_of([user_answer()])),
        "refusals": ["401 or 403: token or scope (Section 5)"],
    },
    "read": {
        "notes": ["{user-id} is the BIBS user ID (the id and userName of the answer) or the Windows ID."],
        "request": None,
        "answer": ("200 OK", user_answer()),
        "refusals": ["404: no user with that ID (detail: No user <id>)"],
    },
    "replace": {
        "notes": [
            "Send the whole User. Attributes left out are not cleared when BIBS cannot tell a removal from an "
            "omission; send every attribute UIDM-ISC holds.",
            "When the body has no Windows ID, BIBS keeps the one it holds for {user-id}.",
            "The group profiles stay as they are unless the body carries groups. When it does, they replace the "
            "group profiles at once, after the same checks as for a joiner. An empty list removes every group "
            "profile.",
            "A change of the AD status in the extension is applied with the other details.",
        ],
        "request": MOVER,
        "answer": ("200 OK", user_answer(groups=("CLM_TL",), request="UIDM-2026-004790", team_leader="Carla Lim")),
        "refusals": ["400: no user holds the Windows ID or user ID", "404 or 400 invalidValue: group checks as "
                     "for a joiner", "409 uniqueness: the Windows ID belongs to another user"],
    },
    "patch": {
        "notes": [
            "A SCIM PatchOp. BIBS reads the first operation that sets active (true or false) or the extension "
            "adStatus (ACTIVE, INACTIVE, LOCKED, DISABLED or DEACTIVATED). Other operations are ignored.",
            "active false, or any AD status other than ACTIVE, deactivates the user in BIBS and ends the open "
            "sessions at once.",
            "active true (or ACTIVE) is a rehire: the user is reactivated with the group profiles held before.",
            "At most 100 operations.",
        ],
        "request": STATUS_PATCH,
        "extra": [("Same call with the AD status", AD_STATUS_PATCH), ("Rehire", REHIRE_PATCH)],
        "answer": ("200 OK", user_answer(active=False, status="INACTIVE")),
        "refusals": ["400 invalidValue: the patch changes neither active nor the AD status, or an unknown AD "
                     "status", "400: no user holds the ID"],
    },
    "delete": {
        "notes": ["The user is deactivated (status DEACTIVATED), the open sessions end, the group profiles are "
                  "kept for the record and the history stays. BIBS never deletes a user.",
                  "No body is sent or returned."],
        "request": None,
        "answer": ("204 No Content", None),
        "refusals": ["400: no user holds the ID"],
    },
    "groups": {
        "notes": ["Returns every group profile with its name, status, permission codes and members (BIBS user "
                  "IDs). The group profiles themselves are maintained in BIBS through group-profile requests; "
                  "UIDM-ISC reads them here and maps them to its entitlements."],
        "request": None,
        "answer": ("200 OK", list_of([GROUP_ANSWER])),
        "refusals": ["401 or 403: token or scope (Section 5)"],
    },
    "group": {
        "notes": ["{group-id} is the group profile code."],
        "request": None,
        "answer": ("200 OK", GROUP_ANSWER),
        "refusals": ["404: no group profile with that code (detail: No group profile <code>)"],
    },
    "membership": {
        "notes": [
            "The only path accepted is members, or members[value eq \"<user ID>\"] to remove one member. "
            "A value of a member is a BIBS user ID as returned by the user extract.",
            "add gives the group profile to the users listed; remove takes it from the users listed (or from "
            "every holder when no value and no filter is given); replace makes the users listed the only "
            "holders. op is read without regard to case.",
            "Each user whose group profiles change gets one membership event. All of them are checked first. "
            "When one is refused, nothing is applied for anybody (all or nothing).",
            "Send the UIDM request number in the header X-UIDM-Request-No (up to 40 letters, digits and . _ / -).",
        ],
        "request": GROUP_ADD,
        "extra": [("Remove one member", GROUP_REMOVE), ("Replace all members", GROUP_REPLACE)],
        "answer": ("200 OK", GROUP_ANSWER),
        "refusals": ["404: the group profile does not exist",
                     "400 invalidPath: a path other than members", "400 invalidSyntax: an op other than add, "
                     "remove or replace", "400 invalidValue: a member that is not a BIBS user, an inactive group "
                     "profile, a separation-of-duties conflict, conflicting permissions"],
    },
}

# ----------------------------------------------------------------------------------------------- mapping
# (SCIM attribute, BIBS field, mandatory, format, validation)
CORE_MAP = [
    ("schemas", "-", "Yes", "List of schema URNs", "Core User; add the enterprise and BIBS extension URNs when "
     "they are sent"),
    ("id", "User ID (read only)", "No", "Text", "Ignored on input; BIBS returns the user ID"),
    ("externalId", "Windows ID", "Yes (or windowsId)", "Text, up to 50", "Letters, digits and . _ \\ @ -; "
     "unique; the extension windowsId wins when both are sent"),
    ("userName", "User ID", "Yes (joiner)", "Text, up to 50", "Letters, digits and . _ -; unique; must also "
     "match the BIBS parameter USER_ID_PATTERN"),
    ("name.givenName", "First name", "Yes", "Text, up to 60", "No control characters"),
    ("name.familyName", "Last name", "Yes", "Text, up to 60", "No control characters"),
    ("displayName", "Display name", "No", "Text, up to 120", "No control characters; BIBS returns the full "
     "name"),
    ("emails[].value", "E-mail", "Yes", "local@domain", "The primary address, or the first one when none is "
     "primary; up to 64 + 120 characters"),
    ("active", "Status", "No", "true / false", "Used when the extension adStatus is absent; false gives "
     "INACTIVE"),
    ("groups[].value", "Group profiles", "No", "List of codes, up to 50", "Each code up to 40 letters, digits "
     "and _ . -; absent: unchanged; present: replaces the group profiles"),
    ("meta", "-", "No", "Object", "Accepted and ignored"),
]
ENTERPRISE_MAP = [
    ("department", "Department", "No", "Text, up to 120", "No control characters"),
    ("division", "Unit / segment", "No", "Text, up to 120", "Used when the BIBS extension unitSegment is absent"),
    ("manager.displayName", "Unit head name", "No", "Text, up to 120", "Used when the BIBS extension "
     "unitHeadName is absent"),
]
BIBS_MAP = [
    ("windowsId", "Windows ID", "Yes (or externalId)", "Text, up to 50", "As externalId"),
    ("adGroup", "AD group", "No", "Text, up to 120", "No control characters"),
    ("adStatus", "Status", "No", "ACTIVE, INACTIVE, LOCKED, DISABLED, DEACTIVATED", "Any other value is "
     "refused; only ACTIVE lets the user sign in"),
    ("teamLeaderName", "Team leader", "No", "Text, up to 120", "No control characters"),
    ("teamHeadName", "Team head", "No", "Text, up to 120", "No control characters"),
    ("sectionHeadName", "Section head", "No", "Text, up to 120", "No control characters"),
    ("unitHeadName", "Unit head", "No", "Text, up to 120", "No control characters"),
    ("unitSegment", "Unit / segment", "No", "Text, up to 120", "No control characters"),
    ("location", "Location", "No", "Text, up to 120", "No control characters"),
    ("uidmRequestNo", "UIDM request number", "Yes (BDOI rule)", "Text, up to 40", "Letters, digits and . _ / -; "
     "reference of the access change log; BIBS uses IDE-<event no.> when absent"),
    ("groupProfiles", "Group profiles", "No", "List of codes", "Returned on every answer. On input it is read "
     "only when BDOI turns on the setting UAM_PROVISIONING_ROLES; send groups instead"),
]
STATUS_VALUES = [
    ("ACTIVE", "Yes", "User active, can sign in"),
    ("INACTIVE", "No", "User deactivated, sessions ended"),
    ("LOCKED", "No", "User deactivated, sessions ended"),
    ("DISABLED", "No", "User deactivated, sessions ended"),
    ("DEACTIVATED", "No", "User deactivated, sessions ended (also the result of a leaver)"),
]

# ----------------------------------------------------------------------------------------------- errors
# (status, scimType, when, detail, UIDM-ISC action)
ERRORS = [
    ("400", "invalidValue", "An attribute outside the agreed schema, a value too long, characters not allowed, "
     "more than 50 group profiles, more than 100 operations, an unknown AD status, a patch that changes "
     "nothing BIBS reads", "Names the attribute, never its value", "Correct the request in UIDM-ISC; do not "
     "retry unchanged"),
    ("400", "invalidValue", "Membership refused: inactive group profile, separation-of-duties conflict, "
     "conflicting permissions, member that is not a BIBS user", "Event <no.>: <reason>", "Return the request to "
     "the requester or approver; raise a group-profile request in BIBS when the profile must change"),
    ("400", "(none)", "Event refused for another business reason: no user holds the Windows ID or user ID, "
     "provisioning switched off (setting UAM_SSO_PROVISIONING)", "Event <no.>: <reason>", "Check the user "
     "exists (GET of Users/{user-id}); for a mover of an unknown user send a joiner"),
    ("400", "invalidSyntax", "The body is not valid JSON, or a group patch op other than add, remove, replace",
     "The body is not valid JSON / Unknown operation", "Fix the connector; do not retry unchanged"),
    ("400", "invalidPath", "A group patch path other than members", "Only the members of a group profile "
     "change", "Fix the connector"),
    ("401", "-", "No token, an expired token, a wrong issuer, audience or signature", "Bearer challenge in the "
     "WWW-Authenticate header", "Get a new token from the gateway and retry once"),
    ("403", "-", "Valid token without the scope identity.provision", "-", "Ask BDOI IT to grant the scope to "
     "the UIDM-ISC client"),
    ("404", "(none)", "Unknown user (GET), unknown group profile (GET or PATCH of a group, or a code in groups)",
     "No user <id> / No group profile <code>", "Check the code against the group extract; correct the "
     "entitlement mapping"),
    ("409", "uniqueness", "The user ID already exists, or the Windows ID belongs to another user",
     "Event <no.>: ... already exists / belongs to another user", "Use PUT for an existing user; resolve the "
     "duplicate identity with BDOI IT"),
    ("413", "-", "Body larger than 256 KB", "The request is larger than allowed", "Split the change; one user per "
     "call"),
    ("415", "-", "Content type other than application/scim+json or application/json", "Send "
     "application/scim+json or application/json", "Fix the connector"),
    ("429", "-", "More than 300 requests in one minute from one client address", "Too many requests; retry "
     "later (Retry-After: 60)", "Wait the Retry-After seconds, then resume"),
    ("500", "-", "Unexpected failure in BIBS", "The request could not be processed (reference <ref>)", "Retry "
     "with backoff; when it persists, raise an incident quoting the reference"),
]

# ----------------------------------------------------------------------------------------------- journey
# (no., actor, system, step, interface)
JOURNEY = [
    (1, "Requester", "UIDM-ISC", "Raises the user access request (joiner, mover, leaver, rehire or access "
     "change) with the BIBS group profiles needed.", "-"),
    (2, "Line approver", "UIDM-ISC", "Approves, rejects or returns the request.", "-"),
    (3, "Application owner / Information Security", "UIDM-ISC", "Gives the second approval for a privileged "
     "group profile.", "-"),
    (4, "UIDM-ISC", "Microsoft Entra ID", "Creates or updates the account (UPN, AD user ID, status) and the "
     "BIBS application assignment.", "Within BDOI"),
    (5, "UIDM-ISC", "BIBS", "Sends the joiner or mover with the group profiles and the UIDM request number.",
     "SCIM POST or PUT of Users"),
    (6, "BIBS", "BIBS", "Checks the request (token, scope, size, attributes, group profiles, separation of "
     "duties) and records the identity event.", "-"),
    (7, "BIBS", "BIBS", "Creates or updates the user, gives the group profiles, writes the access change log "
     "and the audit trail, and tells the user (notice Your access changed).", "-"),
    (8, "BIBS", "UIDM-ISC", "Answers with the user as held in BIBS, or with the SCIM error.", "SCIM answer"),
    (9, "UIDM-ISC", "UIDM-ISC", "Closes the request on success; on an error, routes it back for correction.",
     "-"),
    (10, "User", "Microsoft Entra ID / BIBS", "Opens BIBS and signs in with Microsoft Entra ID. BIBS links the "
     "identity to the active user.", "OpenID Connect"),
    (11, "UIDM-ISC", "BIBS", "Sends a later mover or access change (PUT of Users, PATCH of Groups).",
     "SCIM"),
    (12, "UIDM-ISC", "BIBS", "Sends a status change or a leaver. BIBS deactivates the user and ends the open "
     "sessions at once.", "SCIM PATCH or DELETE of Users"),
    (13, "User (leaver)", "Microsoft Entra ID / BIBS", "Any later sign-in is refused by BIBS, even when Entra "
     "ID still issues a token.", "OpenID Connect"),
    (14, "BIBS", "UIDM-ISC directory", "The daily reconciliation compares every BIBS user with the directory "
     "and reports mismatches to the System Administrators.", "Directory read"),
    (15, "Certifier", "UIDM-ISC", "Reads the user and group extracts for the periodic access certification; "
     "removals come back as step 11 or 12.", "SCIM GET of Users and Groups"),
]

ACTORS = [
    ("UIDM-ISC", "BDOI identity governance platform", "Source of truth for user access requests, approvals and "
     "group-profile membership. Client of the SCIM interface."),
    ("Microsoft Entra ID", "BDO enterprise identity provider (EIAM)", "Holds the accounts and passwords. "
     "OpenID Connect provider for the sign-in to BIBS."),
    ("API gateway", "BDOI API gateway (Apigee)", "Issues the client-credentials token to UIDM-ISC, applies its "
     "quota and forwards the calls to BIBS over TLS."),
    ("BIBS", "BDO Insure Brokering System", "SCIM service provider for Users and Groups; OpenID Connect "
     "client; keeps the identity event log, the access change log and the audit trail."),
    ("BIBS System Administrator", "BDOI user of BIBS", "Reviews refused events and reconciliation mismatches in "
     "Identity Synchronisation. Signs in through Microsoft Entra ID like every user."),
    ("Requester, approver, certifier", "BDOI users", "Work in UIDM-ISC only."),
]

ENVIRONMENTS = [
    ("SIT", "https://{gateway-host-sit}/" + BASE_PATH, "https://{bibs-host-sit}/", "BDOI IT to give"),
    ("UAT", "https://{gateway-host-uat}/" + BASE_PATH, "https://{bibs-host-uat}/", "BDOI IT to give"),
    ("Production", "https://{gateway-host-prod}/" + BASE_PATH, "https://{bibs-host-prod}/", "BDOI IT to give"),
]

SECURITY = [
    ("Grant", "OAuth 2.0 client credentials, token issued by the BDOI API gateway to the UIDM-ISC client"),
    ("Token type", "Signed JWT, sent as Authorization: Bearer <token>"),
    ("Signature", "RS256 only; keys read from the gateway key set (JWKS address set in BIBS)"),
    ("Issuer (iss)", "The gateway issuer, set in BIBS per environment (BDOI IT to give)"),
    ("Audience (aud)", "At least one of the audiences set in BIBS for the environment (BDOI IT to give)"),
    ("Scope", "identity.provision, read from the scope claim; every resource of the interface needs it"),
    ("Validity", "exp and nbf checked with a clock skew of 60 seconds"),
    ("Unknown paths", "Refused: only the configured path of the interface is open to the token"),
    ("Body size", "256 KB at most per request (one User is a few KB); larger gives status 413"),
    ("Rate", "300 requests per minute per client address and BIBS instance; above it status 429 with "
     "Retry-After: 60. The gateway quota applies in front"),
    ("Order of checks", "Size and rate first, then the token, then the scope, then the request"),
    ("Transport", "TLS 1.2 or higher from UIDM-ISC to the gateway and from the gateway to BIBS"),
    ("Data in answers", "Errors name the attribute, never its value; no stack trace; a failure carries a "
     "reference only"),
]

OIDC_ITEMS = [
    ("Tenant ID", "Directory (tenant) ID of BDO in Microsoft Entra ID", "BDOI IT"),
    ("Issuer", "https://login.microsoftonline.com/{tenant-id}/v2.0; BIBS reads the provider settings from "
     "its discovery document", "BDOI IT"),
    ("Application (client) ID", "One app registration per environment, or one with all redirect URIs", "BDOI IT"),
    ("Client secret", "Confidential client; the secret is sent with HTTP Basic authentication to the token "
     "address. Kept in the BIBS secret store, never in a file", "BDOI IT (to the BIBS secret store)"),
    ("Redirect URIs", "Web platform; one per environment (table below)", "BDOI IT registers"),
    ("Scopes", "openid profile email", "BDOI IT consents"),
    ("Signing", "ID token signed RS256", "Default of Entra ID"),
    ("User name claim", "A claim whose value equals the BIBS user ID (the AD user ID sent as userName by "
     "UIDM-ISC). BIBS reads the claim named in its setting BROKERVERSE_SSO_USERNAME_CLAIM", "BDOI IT and "
     "iorta TechNXT agree the claim (open point OP-03)"),
    ("Assignment", "Only users assigned to the BIBS application may get a token (assignment required: Yes)",
     "BDOI IT"),
    ("Sign-out return", "After sign-out Entra ID returns the user to the BIBS login page (<BIBS address>login); "
     "register it with the redirect URIs", "BDOI IT registers"),
]

CLAIMS = [
    ("iss", "Must equal the configured issuer", "Checked"),
    ("aud", "Must contain the BIBS client ID", "Checked"),
    ("exp / nbf / iat", "Valid time, with a clock skew of 2 minutes", "Checked"),
    ("nonce", "Must equal the nonce BIBS sent with the request", "Checked"),
    ("User name claim", "Links the sign-in to an existing BIBS user with that user ID", "Used for the link"),
    ("upn", "User principal name; informative unless chosen as the user name claim", "Not used by default"),
    ("oid / tid", "Object and tenant IDs", "Not used"),
    ("groups / roles", "Entra ID groups or app roles", "Not used for authorisation: access comes only from "
     "the BIBS group profiles given by UIDM-ISC"),
]

NFR = [
    ("Volume", "About 530 BIBS users; peak at go-live load (one call per user) and at re-organisations. Normal "
     "day: tens of events"),
    ("Response time", "Target: 95% of single-user calls under 2 seconds at the gateway; the user extract "
     "(about 530 users) under 10 seconds"),
    ("Availability", "The service levels of BIBS (programme NFR). Planned maintenance is announced to BDOI IT; "
     "during it the gateway answers 502 or 503 and UIDM-ISC retries"),
    ("Retries", "Retry only 429, 500, 502, 503, 504 and time-outs. Exponential backoff: 30 s, 2 min, 10 min, "
     "30 min, then hourly up to 24 hours; honour Retry-After. Never retry 400, 403, 404, 409, 413 or 415 "
     "unchanged"),
    ("Repeat safety", "A retry of the same call with the same UIDM request number gives the first answer and "
     "changes nothing twice (Section 8.2)"),
    ("Ordering", "Send the events of one user in order and wait for each answer; events of different users may "
     "run in parallel (up to 5 at a time is advised)"),
    ("Time-out", "UIDM-ISC client time-out of 30 seconds per call"),
    ("Logging", "Every call is recorded in the identity event log with its outcome; retained as the BIBS audit "
     "trail"),
    ("Security", "TLS 1.2+, signed tokens, least privilege scope, no personal data in error texts"),
]

OPEN_POINTS = [
    ("OP-01", "Gateway addresses and the public base path of the interface for SIT, UAT and production",
     "BDOI IT", "Before SIT connection"),
    ("OP-02", "Token issuer, audience values and key set address of the gateway; the UIDM-ISC client "
     "registration with the scope identity.provision", "BDOI IT", "Before SIT connection"),
    ("OP-03", "Entra ID claim carrying the BIBS user ID (AD user ID) for the sign-in link; UPN is not accepted "
     "as a BIBS user ID", "BDOI IT with iorta TechNXT", "Before SIT connection"),
    ("OP-04", "Tenant ID, application (client) ID, client secret and the redirect URIs registered per "
     "environment", "BDOI IT", "Before SIT connection"),
    ("OP-05", "Mapping of UIDM-ISC entitlements to BIBS group profile codes (from the group extract)",
     "BDOI IT / UIDM-ISC team", "Before SIT run"),
    ("OP-06", "Use of groups on the User (joiner, mover) or the Groups patch, or both; BIBS accepts both",
     "BDOI IT / UIDM-ISC team", "Before SIT run"),
    ("OP-07", "Address and token of the UIDM-ISC directory read by the daily reconciliation", "BDOI IT",
     "Before UAT"),
    ("OP-08", "Retry policy and alerting in UIDM-ISC for refused and failed calls (Section 12)",
     "UIDM-ISC team", "Before UAT"),
    ("OP-09", "Network path and allow-listing between the gateway and BIBS; TLS certificates", "BDOI IT",
     "Before SIT connection"),
]

GLOSSARY = [
    ("Access change log", "BIBS record of every changed user attribute and group profile, with the actor and "
     "the UIDM request number"),
    ("AD status", "Status of the account in the directory: ACTIVE, INACTIVE, LOCKED, DISABLED or DEACTIVATED"),
    ("API gateway", "BDOI component that authenticates UIDM-ISC, issues its token and forwards the calls"),
    ("Local password", "Password kept in BIBS; only for test accounts in DEV, SIT and UAT. Production refuses "
     "sign-in with a local password"),
    ("Entra ID", "Microsoft Entra ID, the BDO enterprise identity provider (EIAM)"),
    ("Group profile", "BIBS role: a named set of permissions given to users"),
    ("Identity event", "One call of UIDM-ISC as recorded in BIBS with its type and outcome"),
    ("JWT", "JSON Web Token, the signed token of the gateway"),
    ("Joiner, mover, leaver, rehire", "New user, change of details, exit, return of a former user"),
    ("OIDC", "OpenID Connect, the sign-in protocol used with Entra ID"),
    ("SCIM", "System for Cross-domain Identity Management 2.0 (RFC 7643, RFC 7644)"),
    ("Separation of duties", "BIBS rule naming group profiles one user may not hold together"),
    ("UIDM-ISC", "BDOI identity governance platform for user access requests and certification"),
    ("UIDM request number", "Number of the approved request in UIDM-ISC, carried on every change"),
    ("UPN", "User principal name of the Entra ID account"),
    ("Windows ID", "The AD account of the user, unique in BIBS"),
]

# ----------------------------------------------------------------------------------------------- test pack
# body sample references (sheet Samples of the workbook, same as the examples of Section 6)
SAMPLES = {
    "S-01": ("Joiner with one group profile", JOINER),
    "S-02": ("Mover with a new group profile", MOVER),
    "S-03": ("Status change: active false", STATUS_PATCH),
    "S-04": ("Status change: AD status LOCKED", AD_STATUS_PATCH),
    "S-05": ("Rehire: active true", REHIRE_PATCH),
    "S-06": ("Group patch: add two members", GROUP_ADD),
    "S-07": ("Group patch: remove one member", GROUP_REMOVE),
    "S-08": ("Group patch: replace members", GROUP_REPLACE),
}

U = "Users"
UID = "Users/{user-id}"
GID = "Groups/{group-id}"
EV = "Identity event "
ACL = "Access change log: "
T = [
    # id, group, scenario, preconditions, method, path, body, expected status, expected answer, event log,
    # access change log, audit / notice
    ("Joiner", "Joiner with one group profile", "User ID and Windows ID not in BIBS; CLM_OFFICER active",
     "POST", U, "S-01", "201", "User with active true and groups CLM_OFFICER", EV + "Joiner, Applied",
     ACL + "user created, group profile added, reference UIDM request no.",
     "Audit: user created; notice Your access changed to the user"),
    ("Joiner", "Joiner without groups", "As above; S-01 without groups", "POST", U, "S-01 (groups removed)",
     "201", "User with no group profile", EV + "Joiner, Applied", ACL + "user created",
     "Audit: user created"),
    ("Joiner", "Joiner for a user ID that exists", "TC-01 done", "POST", U, "S-01 with another UIDM no.",
     "409", "scimType uniqueness; detail ... already exists", EV + "Joiner, Refused", "No change",
     "Audit: event refused; notice Identity event to review to System Administrators"),
    ("Mover", "Mover with new team leader and group profile", "TC-01 done", "PUT", UID, "S-02", "200",
     "User with teamLeaderName Carla Lim and groups CLM_TL", EV + "Mover, Applied; " + EV + "Membership, Applied",
     ACL + "team leader changed; CLM_OFFICER removed, CLM_TL added", "Notice Your access changed"),
    ("Mover", "Mover without groups keeps group profiles", "TC-04 done", "PUT", UID, "S-02 without groups",
     "200", "groups unchanged (CLM_TL)", EV + "Mover, Applied", ACL + "changed details only", "-"),
    ("Status change", "Account disabled (active false)", "User active", "PATCH", UID, "S-03", "200",
     "User with active false, adStatus INACTIVE", EV + "Status change, Applied",
     ACL + "status changed; open sessions ended", "Audit: user deactivated"),
    ("Status change", "AD status LOCKED", "User active", "PATCH", UID, "S-04", "200",
     "active false, adStatus LOCKED", EV + "Status change, Applied", ACL + "status changed", "Sessions ended"),
    ("Status change", "Patch with no status operation", "User exists", "PATCH", UID,
     "PatchOp replacing displayName", "400", "scimType invalidValue; the patch changes neither active nor the AD "
     "status", "No event recorded", "No change", "-"),
    ("Leaver", "Leaver", "User active with sessions open", "DELETE", UID, "-", "204", "No body",
     EV + "Leaver, Applied", ACL + "status DEACTIVATED; sessions ended", "Audit: user deactivated"),
    ("Leaver", "Leaver for an unknown user", "No such user", "DELETE", "Users/nobody01", "-", "400",
     "detail Event <no.>: No user holds Windows ID nobody01", EV + "Leaver, Refused", "No change",
     "Notice Identity event to review"),
    ("Rehire", "Rehire of a leaver", "TC-09 done", "PATCH", UID, "S-05", "200",
     "active true with the group profiles held before", EV + "Rehire, Applied", ACL + "status ACTIVE",
     "Notice Your access changed"),
    ("Membership", "Add two members", "Users mbsantos and jdelacruz exist; CLM_OFFICER active", "PATCH",
     "Groups/CLM_OFFICER", "S-06 + header X-UIDM-Request-No", "200", "Group with both members",
     EV + "Membership, Applied (one per user changed)", ACL + "CLM_OFFICER added to each user, reference from "
     "the header", "Notice Your access changed to each user"),
    ("Membership", "Remove one member by filter", "TC-12 done", "PATCH", "Groups/CLM_OFFICER", "S-07", "200",
     "Group without jdelacruz", EV + "Membership, Applied", ACL + "CLM_OFFICER removed from jdelacruz",
     "Notice Your access changed"),
    ("Membership", "Replace members", "TC-12 done", "PATCH", "Groups/CLM_OFFICER", "S-08", "200",
     "Group with mbsantos only", EV + "Membership, Applied for each user changed", ACL + "removals and additions",
     "Notices to the users changed"),
    ("Membership", "PUT with groups replaces group profiles", "User holds CLM_OFFICER", "PUT", UID,
     "S-02 with groups CLM_OFFICER and CLM_TH", "200", "groups CLM_OFFICER, CLM_TH", EV + "Mover and "
     "Membership, Applied", ACL + "CLM_TH added", "Notice Your access changed"),
    ("Repeat", "Same joiner sent twice (retry after a time-out)", "TC-01 done", "POST", U,
     "S-01 unchanged, same UIDM no.", "201", "Same answer as the first call", "No new event: the first event is "
     "returned", "No change", "-"),
    ("Repeat", "Same group patch sent twice", "TC-12 done", "PATCH", "Groups/CLM_OFFICER",
     "S-06, same header", "200", "Same group", "No new change for users who already hold it", "No change", "-"),
    ("Refusal", "Unknown group profile on a joiner", "CLM_SUPERVISOR does not exist", "POST", U,
     "S-01 with groups CLM_SUPERVISOR", "404", "detail Event <no.>: No group profile CLM_SUPERVISOR",
     EV + "Membership, Refused; user not created", "No change", "Notice Identity event to review"),
    ("Refusal", "Inactive group profile", "Group profile deactivated in BIBS", "PATCH", GID, "S-06", "400",
     "scimType invalidValue; Group profile <code> is not active", EV + "Membership, Refused", "No change",
     "Notice Identity event to review"),
    ("Refusal", "Separation-of-duties conflict", "Active rule forbids CLM_OFFICER with CLM_TL", "PUT", UID,
     "S-02 with groups CLM_OFFICER and CLM_TL", "400", "scimType invalidValue; One user may not hold both ...",
     EV + "Membership, Refused; nothing applied", "No change", "Notice Identity event to review"),
    ("Refusal", "All or nothing: one of two members conflicts", "jdelacruz holds CLM_OFFICER; a rule forbids "
     "CLM_OFFICER with CLM_TL", "PATCH", "Groups/CLM_TL", "S-06 for CLM_TL", "400", "scimType invalidValue",
     EV + "Membership, Refused", "No change for either user", "Notice Identity event to review"),
    ("Refusal", "Unknown member", "nobody01 is not a BIBS user", "PATCH", "Groups/CLM_OFFICER",
     "S-06 with value nobody01", "400", "scimType invalidValue", EV + "Membership, Refused", "No change",
     "Notice Identity event to review"),
    ("Refusal", "Invalid attribute", "-", "POST", U, "S-01 with nickName added", "400",
     "scimType invalidValue; Attribute nickName is not supported", "No event recorded", "No change", "-"),
    ("Refusal", "Value too long", "-", "POST", U, "S-01 with givenName of 61 characters", "400",
     "scimType invalidValue; name.givenName is longer than 60 characters", "No event recorded", "No change", "-"),
    ("Refusal", "Invalid group patch path", "-", "PATCH", "Groups/CLM_OFFICER", "S-06 with path displayName",
     "400", "scimType invalidPath", "No event recorded", "No change", "-"),
    ("Limits", "Body over 256 KB", "-", "POST", U, "Padded body of 300 KB", "413",
     "The request is larger than allowed", "No event recorded", "No change", "-"),
    ("Limits", "Rate limit", "Rate 300 per minute", "GET", U, "-", "429",
     "Retry-After: 60 on the 301st call within the minute", "No event recorded", "No change", "-"),
    ("Security", "Token without the scope identity.provision", "Gateway client without the scope", "GET", U, "-",
     "403", "Access refused", "No event recorded", "No change", "-"),
    ("Security", "Token with the wrong audience", "Token for another API", "GET", U, "-", "401",
     "WWW-Authenticate: Bearer error", "No event recorded", "No change", "-"),
    ("Security", "No token", "-", "GET", U, "-", "401", "WWW-Authenticate: Bearer", "No event recorded",
     "No change", "-"),
    ("Extract", "User extract", "Users loaded", "GET", U, "-", "200",
     "ListResponse; totalResults equals the BIBS users with a Windows ID", "No event recorded", "No change", "-"),
    ("Extract", "Group extract", "Group profiles set up", "GET", "Groups", "-", "200",
     "ListResponse with permissions and members of each group profile", "No event recorded", "No change", "-"),
    ("Extract", "One group profile", "-", "GET", "Groups/CLM_OFFICER", "-", "200",
     "Group CLM_OFFICER", "No event recorded", "No change", "-"),
    ("Sign-in", "Single sign-on of an active user", "TC-01 done; user assigned to the BIBS app in Entra ID",
     "Browser", "BIBS login page, single sign-on button", "-", "Signed in",
     "Home page of BIBS with the menus of CLM_OFFICER", "-", "-", "Audit: sign-in by single sign-on; session log"),
    ("Sign-in", "Sign-in of a leaver is refused", "TC-09 done; Entra ID account still enabled", "Browser",
     "BIBS login page, single sign-on button", "-", "Refused", "Sign-in refused message; no session",
     "-", "-", "Audit: sign-in failed (deactivated user)"),
    ("Sign-in", "Sign-in of an Entra ID user unknown to BIBS", "Account not provisioned", "Browser",
     "BIBS login page, single sign-on button", "-", "Refused", "Sign-in refused message; no user created",
     "-", "-", "Audit: sign-in failed (unknown user)"),
    ("Reconciliation", "Daily reconciliation finds a mismatch", "Status changed in the directory without an "
     "event", "Job", "Identity reconciliation run", "-", "Completed", "Mismatch STATUS listed",
     "-", "-", "Notice reconciliation mismatch to System Administrators"),
]
TEST_CASES = [dict(zip(("group", "scenario", "pre", "method", "path", "body", "status", "answer", "event", "acl",
                        "audit"), row), id=f"TC-{i:02d}") for i, row in enumerate(T, start=1)]
