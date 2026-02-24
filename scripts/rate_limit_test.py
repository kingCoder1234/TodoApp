# rate_limit_test.py
#
# Usage:
#   pip install requests
#   python rate_limit_test.py --base http://localhost:8080 --endpoint /api/auth/login --method POST \
#       --json '{"email":"x@y.com","password":"password"}' --count 15
#
#   python rate_limit_test.py --base http://localhost:8080 --endpoint /api/todos --method GET \
#       --token "<JWT>" --count 220
#
# Notes:
# - Auth endpoints are usually limited by IP (e.g., 10 / 15 min).
# - Authenticated endpoints are usually limited per user (e.g., 200 / 1 min).
# - Script prints status, key rate-limit headers, and stops when it hits 429.

import argparse
import json
import sys
import time
from typing import Optional, Dict, Any

import requests


def parse_json_arg(s: Optional[str]) -> Optional[Dict[str, Any]]:
    if not s:
        return None
    try:
        return json.loads(s)
    except json.JSONDecodeError as e:
        print(f"Invalid --json payload: {e}", file=sys.stderr)
        sys.exit(2)


def main() -> None:
    p = argparse.ArgumentParser(description="Rate limiter tester (repeated requests until 429).")
    p.add_argument("--base", default="http://localhost:8080", help="Base URL, e.g. http://localhost:8080")
    p.add_argument("--endpoint", required=True, help="Endpoint path, e.g. /api/auth/login or /api/todos")
    p.add_argument("--method", choices=["GET", "POST", "PUT", "DELETE"], default="GET")
    p.add_argument("--token", default=None, help="Bearer token for authenticated endpoints")
    p.add_argument("--json", default=None, help='JSON body as string, e.g. \'{"email":"a@a.com","password":"x"}\'')
    p.add_argument("--count", type=int, default=50, help="How many requests to send")
    p.add_argument("--delay", type=float, default=0.0, help="Delay between requests in seconds")
    p.add_argument("--timeout", type=float, default=10.0, help="Request timeout seconds")
    args = p.parse_args()

    url = args.base.rstrip("/") + "/" + args.endpoint.lstrip("/")
    payload = parse_json_arg(args.json)

    headers: Dict[str, str] = {"Accept": "application/json"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    if args.token:
        headers["Authorization"] = f"Bearer {args.token}"

    sess = requests.Session()

    print(f"Target: {url}")
    print(f"Method: {args.method} | Requests: {args.count} | Delay: {args.delay}s")
    if args.token:
        print("Auth: Bearer <provided>")
    else:
        print("Auth: none")
    if payload is not None:
        print(f"Body: {json.dumps(payload)}")
    print("-" * 90)

    for i in range(1, args.count + 1):
        try:
            if args.method == "GET":
                r = sess.get(url, headers=headers, timeout=args.timeout)
            elif args.method == "POST":
                r = sess.post(url, headers=headers, json=payload, timeout=args.timeout)
            elif args.method == "PUT":
                r = sess.put(url, headers=headers, json=payload, timeout=args.timeout)
            else:
                r = sess.delete(url, headers=headers, timeout=args.timeout)
        except requests.RequestException as e:
            print(f"{i:03d} -> REQUEST ERROR: {e}")
            break

        limit = r.headers.get("X-RateLimit-Limit", "-")
        remaining = r.headers.get("X-RateLimit-Remaining", "-")
        reset = r.headers.get("X-RateLimit-Reset", "-")
        retry_after = r.headers.get("Retry-After", "-")

        print(
            f"{i:03d} -> {r.status_code} | "
            f"Limit={limit} Remaining={remaining} Reset={reset} Retry-After={retry_after}"
        )

        if r.status_code == 429:
            # show response body for debugging
            body = r.text.strip()
            if body:
                print("\n429 body:")
                print(body)
            print("\nHit rate limit. Stopping.")
            break

        if args.delay > 0:
            time.sleep(args.delay)


if __name__ == "__main__":
    main()