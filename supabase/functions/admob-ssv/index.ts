import { verifyCallback } from "./ssv.mjs";

let keys: Array<{keyId: number; pem: string}> = [];
let refreshedAt = 0;
async function getKeys(force = false) {
  const age = Date.now() - refreshedAt;
  if (keys.length && age < 3_600_000 && (!force || age < 60_000)) return keys;
  const response = await fetch("https://www.gstatic.com/admob/reward/verifier-keys.json", { signal: AbortSignal.timeout(8_000) });
  if (!response.ok) throw new Error("key_server_unavailable");
  const data = await response.json();
  if (!Array.isArray(data.keys) || !data.keys.length) throw new Error("invalid_keys");
  keys = data.keys.filter((k: {keyId: number; pem: string}) => k.keyId != null && typeof k.pem === "string");
  if (!keys.length) throw new Error("invalid_keys");
  refreshedAt = Date.now();
  return keys;
}
function reply(status: number, result: string) {
  return new Response(JSON.stringify({result}), {status, headers: {"Content-Type":"application/json","Cache-Control":"no-store"}});
}
Deno.serve(async (req: Request) => {
  if (req.method !== "GET") return reply(405,"method_not_allowed");
  let reward;
  try {
    try { reward = verifyCallback(req.url, await getKeys()); }
    catch (error) {
      if (error instanceof Error && error.message === "unknown_key") reward = verifyCallback(req.url, await getKeys(true));
      else throw error;
    }
  } catch { return reply(400,"invalid_callback"); }
  const endpoint = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!endpoint || !serviceKey) return reply(503,"unavailable");
  try {
    const response = await fetch(endpoint + "/rest/v1/rpc/fulfil_store_ad_v1", {
      method:"POST",
      headers:{"Content-Type":"application/json",apikey:serviceKey,Authorization:"Bearer "+serviceKey},
      body:JSON.stringify({p_intent_id:reward.intentId,p_transaction_id:reward.transactionId,p_user_id:reward.userId,p_ad_unit:reward.adUnit}),
      signal:AbortSignal.timeout(10_000),
    });
    if (!response.ok) return reply(409,"reward_not_accepted");
    return reply(200,"verified");
  } catch { return reply(503,"unavailable"); }
});
