// Run with Node 24+. No network calls, accounts or purchase writes.
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import { createContext, runInContext } from 'node:vm';
import assert from 'node:assert/strict';

const source = readFileSync(new URL('../functions/verify-play-purchase/index.ts', import.meta.url), 'utf8');
const executable = stripTypeScriptTypes(source.replace(/^import .*;\n/gm, ''), { mode: 'strip' });
async function probe(method, account, authorization = 'Bearer readiness-test') {
  let handler;
  const environment = {
    SUPABASE_URL: 'https://example.invalid', SUPABASE_ANON_KEY: 'public-test',
    SUPABASE_SERVICE_ROLE_KEY: 'fixture-only', GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: account,
  };
  const context = createContext({
    Request, Response, Headers, URL, console,
    Deno: { env: { get: key => environment[key] }, serve: callback => { handler = callback; } },
    createClient: () => { throw new Error('Readiness must not access account/purchase data'); },
    GoogleAuth: class { constructor() { throw new Error('Readiness must not perform billing'); } },
  });
  runInContext(executable, context);
  const response = await handler(new Request('https://example.invalid/verify-play-purchase', {
    method, headers: authorization ? { Authorization: authorization } : {},
  }));
  return { status: response.status, body: await response.json() };
}
assert.deepEqual(await probe('GET', undefined), { status: 503, body: { error: 'google_play_not_configured' } });
assert.deepEqual(await probe('GET', 'invalid'), { status: 503, body: { error: 'invalid_google_service_account_json' } });
assert.deepEqual(await probe('GET', '{}'), { status: 503, body: { error: 'invalid_google_service_account_json' } });
assert.deepEqual(await probe('GET', JSON.stringify({ client_email: 'fixture@example.invalid', private_key: 'fixture' })), { status: 200, body: { available: true } });
assert.equal((await probe('GET', undefined, null)).status, 401);
assert.equal((await probe('PUT', undefined)).status, 405);
console.log('PASS: 6 payment readiness cases; no account or billing access');
