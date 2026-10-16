"""Local stand-in of the API gateway for the SCIM load test.

Writes an RSA key set (jwks.json, served to the backend) and prints a signed access token with the
identity.provision scope. Local runs only: the key is thrown away with the folder.

    python3 integration_token.py OUT_DIR [--hours 4]
"""

import argparse
import base64
import json
import pathlib
import time

import jwt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

ISSUER = "https://gateway.perf.local"
AUDIENCE = "bibs-perf"
KID = "perf-1"


def b64(number: int) -> str:
    raw = number.to_bytes((number.bit_length() + 7) // 8, "big")
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("out")
    parser.add_argument("--hours", type=float, default=4)
    args = parser.parse_args()
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    key_file = out / "key.pem"
    if key_file.exists():
        key = serialization.load_pem_private_key(key_file.read_bytes(), password=None)
    else:
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        key_file.write_bytes(
            key.private_bytes(
                serialization.Encoding.PEM,
                serialization.PrivateFormat.PKCS8,
                serialization.NoEncryption(),
            )
        )
    public = key.public_key().public_numbers()
    jwks = {"keys": [{"kty": "RSA", "kid": KID, "alg": "RS256", "use": "sig",
                      "n": b64(public.n), "e": b64(public.e)}]}
    (out / "jwks.json").write_text(json.dumps(jwks))
    now = int(time.time())
    token = jwt.encode(
        {"iss": ISSUER, "aud": [AUDIENCE], "sub": "uidm-isc", "scope": "identity.provision",
         "iat": now, "nbf": now, "exp": now + int(args.hours * 3600)},
        key, algorithm="RS256", headers={"kid": KID})
    print(token)


if __name__ == "__main__":
    main()
