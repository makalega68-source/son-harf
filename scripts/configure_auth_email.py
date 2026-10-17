"""Apply the reviewed signup branding through Supabase's Management API.

By default this only prints the non-secret proposal. --apply requires an explicit
project ref and SUPABASE_ACCESS_TOKEN supplied by the operator. Never log config
responses: they can contain SMTP credentials and auth provider secrets.
"""
import argparse
import json
import os
from pathlib import Path
import sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parents[1]

def proposed_config():
    return {
        'smtp_sender_name': 'Kelime Tahtı',
        'mailer_subjects_confirmation': 'Kelime Tahtı · E-posta adresini doğrula',
        'mailer_templates_confirmation_content': (ROOT / 'supabase/templates/confirm-signup.html').read_text(),
        # Supabase accepts 6..10, never truncate .Token to fake a four-digit code.
        'mailer_otp_length': 6,
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project-ref')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    proposal = proposed_config()
    if not args.apply:
        print(json.dumps(proposal, ensure_ascii=False, indent=2))
        return
    token = os.environ.get('SUPABASE_ACCESS_TOKEN')
    if not token or not args.project_ref or not args.project_ref.isalnum():
        parser.error('--apply requires --project-ref and SUPABASE_ACCESS_TOKEN')
    url = f'https://api.supabase.com/v1/projects/{args.project_ref}/config/auth'
    headers = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json'}
    def request(method, data=None):
        req = Request(url, data=json.dumps(data).encode() if data else None, headers=headers, method=method)
        with urlopen(req, timeout=30) as response:
            return json.load(response)
    try:
        before = request('GET')
        # Preserve the existing SMTP transport and redirect URLs. Only replace the
        # requested brand, template and supported OTP length.
        request('PATCH', proposal)
        after = request('GET')
        mismatches = [key for key, value in proposal.items() if after.get(key) != value]
        if mismatches:
            raise ValueError('Config verification failed: ' + ', '.join(mismatches))
        print('Verified Kelime Tahtı sender, signup subject/template and 6-digit email OTP.')
        print('Custom SMTP configured:', bool(before.get('smtp_host')))
        print('Verify SPF/DKIM/DMARC with the existing sender domain; inbox placement is decided by recipients.')
    except HTTPError as error:
        sys.exit(f'Management API rejected the operation: HTTP {error.code}')
    except (URLError, ValueError):
        sys.exit('Unable to complete or verify the requested auth email configuration.')

if __name__ == '__main__':
    main()
