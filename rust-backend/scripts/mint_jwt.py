#!/usr/bin/env python3
"""Mint an RS256 OWGE *game* JWT trusted by the Java backend (and the Rust port).

The Rust `decode_token` only checks signature + expiry and reads the principal
from the nested `data` claim, so the minimal payload is:

    { "sub": <id>, "iat": <now>, "exp": <now+ttl>, "data": {id, username, email} }

Usage:
    python3 mint_jwt.py --id 1 --username tester [--email t@e.com] [--ttl 86400]
    # signs with /root/keys/private.key (RSA PKCS#1), prints the token to stdout
"""
import argparse
import time

import jwt  # PyJWT

DEFAULT_KEY = "/root/keys/private.key"


def mint(user_id: int, username: str, email, ttl: int, key_path: str) -> str:
    with open(key_path) as fh:
        private_key = fh.read()
    now = int(time.time())
    payload = {
        "sub": user_id,
        "iat": now,
        "exp": now + ttl,
        "data": {"id": user_id, "username": username, "email": email},
    }
    return jwt.encode(payload, private_key, algorithm="RS256")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", type=int, required=True)
    ap.add_argument("--username", required=True)
    ap.add_argument("--email", default=None)
    ap.add_argument("--ttl", type=int, default=86400)
    ap.add_argument("--key", default=DEFAULT_KEY)
    ap.add_argument("--verify-with", default=None,
                    help="public key PEM to self-verify the minted token")
    args = ap.parse_args()
    token = mint(args.id, args.username, args.email, args.ttl, args.key)
    if args.verify_with:
        with open(args.verify_with) as fh:
            pub = fh.read()
        decoded = jwt.decode(token, pub, algorithms=["RS256"])
        assert decoded["data"]["id"] == args.id
    print(token)


if __name__ == "__main__":
    main()
