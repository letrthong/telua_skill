#!/usr/bin/env python3
"""Encrypt / decrypt apk.config with a password; the encrypted file is base64 text.

Usage:
  ./config_crypto.py encrypt 12345      # apk.config      -> enc_apk.config (overwrites existing enc_apk.config)
  ./config_crypto.py decrypt 12345      # enc_apk.config  -> dec_apk.config (apk.config is never modified)
  ./config_crypto.py decrypt 12345 --force   # same, overwriting an existing dec_apk.config
  ./config_crypto.py encrypt            # omit the password to be prompted (not echoed)

Requires: python3 -m pip install cryptography
File format: base64( salt(16 bytes) || Fernet token ); key = PBKDF2-HMAC-SHA256(password, salt).
NOTE: a password given on the command line is visible in shell history and `ps`.
"""

import argparse
import base64
import binascii
import getpass
import os
import sys

try:
    from cryptography.fernet import Fernet, InvalidToken
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
except ImportError:
    sys.exit("ERROR: missing dependency. Install it with: python3 -m pip install cryptography")

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PLAIN_FILE = os.path.join(SCRIPT_DIR, "apk.config")
ENCRYPTED_FILE = os.path.join(SCRIPT_DIR, "enc_apk.config")
DECRYPTED_FILE = os.path.join(SCRIPT_DIR, "dec_apk.config")
SALT_SIZE = 16
KDF_ITERATIONS = 600_000


def derive_key(password: str, salt: bytes) -> bytes:
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(), length=32, salt=salt, iterations=KDF_ITERATIONS
    )
    return base64.urlsafe_b64encode(kdf.derive(password.encode("utf-8")))


def write_private(path: str, data: bytes) -> None:
    # 0600 so the file is not readable by other users.
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "wb") as f:
        f.write(data)


def check_paths(src: str, dst: str, force: bool) -> None:
    if not os.path.isfile(src):
        sys.exit(f"ERROR: input file not found: {src}")
    if os.path.abspath(src) == os.path.abspath(dst):
        sys.exit("ERROR: input and output must be different files")
    if os.path.exists(dst) and not force:
        sys.exit(f"ERROR: '{dst}' already exists (use --force to overwrite)")


def encrypt(args: argparse.Namespace) -> None:
    src = args.input or PLAIN_FILE
    dst = args.output or ENCRYPTED_FILE
    check_paths(src, dst, force=True)  # encrypt always overwrites enc_apk.config

    password = args.password
    if password is None:
        password = getpass.getpass("Password: ")
        if password != getpass.getpass("Confirm password: "):
            sys.exit("ERROR: passwords do not match")
    if not password:
        sys.exit("ERROR: password must not be empty")

    with open(src, "rb") as f:
        plaintext = f.read()

    salt = os.urandom(SALT_SIZE)
    token = Fernet(derive_key(password, salt)).encrypt(plaintext)
    write_private(dst, base64.b64encode(salt + token) + b"\n")
    print(f"Encrypted: {src} -> {dst}")


def decrypt(args: argparse.Namespace) -> None:
    src = args.input or ENCRYPTED_FILE
    dst = args.output or DECRYPTED_FILE
    check_paths(src, dst, args.force)

    with open(src, "rb") as f:
        try:
            payload = base64.b64decode(f.read().strip(), validate=True)
        except binascii.Error:
            sys.exit("ERROR: input is not valid base64")
    if len(payload) <= SALT_SIZE:
        sys.exit("ERROR: input is too short to be an encrypted config")
    salt, token = payload[:SALT_SIZE], payload[SALT_SIZE:]

    password = args.password
    if password is None:
        password = getpass.getpass("Password: ")

    try:
        plaintext = Fernet(derive_key(password, salt)).decrypt(token)
    except InvalidToken:
        sys.exit("ERROR: wrong password or corrupted file")

    write_private(dst, plaintext)
    print(f"Decrypted: {src} -> {dst}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest="command", required=True)

    for name, func in (("encrypt", encrypt), ("decrypt", decrypt)):
        p = sub.add_parser(name)
        p.add_argument("password", nargs="?", help="omit to be prompted")
        p.add_argument("-i", "--input", help="input file")
        p.add_argument("-o", "--output", help="output file")
        p.add_argument("--force", action="store_true", help="overwrite existing output")
        p.set_defaults(func=func)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
