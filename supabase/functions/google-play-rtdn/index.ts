import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library@9";

const jsonHeaders = { "Content-Type": "application/json", "Cache-Control": "no-store" };

function out(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

function decodeBase64Json(value: string): any {
  const bytes = Uint8Array.from(atob(value), (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return out(405, { error: "method_not_allowed" });

  // Pub/Sub push configuration must attach this secret header. It prevents unauthenticated
  // callers from causing reconciliation; every resulting state is still re-fetched from Play.
  const expectedSecret = Deno.env.get("GOOGLE_PLAY_RTDN_SECRET");
  const providedSecret = req.headers.get("X-Son-Harf-RTDN-Secret");
  if (!expectedSecret || providedSecret !== expectedSecret) return out(401, { error: "unauthorized" });

  const url = Deno.env.get("SUPABASE_URL");
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const serviceAccountJson = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
  const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME") || "com.sonharf.game";
  if (!url || !serviceRole || !serviceAccountJson) return out(500, { error: "server_not_configured" });

  let envelope: any;
  try { envelope = await req.json(); } catch { return out(400, { error: "invalid_json" }); }
  const encodedData = envelope?.message?.data;
  if (typeof encodedData !== "string") return out(400, { error: "missing_pubsub_data" });

  let event: any;
  try { event = decodeBase64Json(encodedData); } catch { return out(400, { error: "invalid_pubsub_data" }); }
  if (event?.packageName && event.packageName !== packageName) return out(204, {});

  let credentials: Record<string, unknown>;
  try { credentials = JSON.parse(serviceAccountJson); } catch { return out(500, { error: "invalid_google_service_account_json" }); }
  const googleAuth = new GoogleAuth({ credentials, scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
  const authClient = await googleAuth.getClient();
  const access = await authClient.getAccessToken();
  if (!access.token) return out(502, { error: "google_oauth_failed" });

  const admin = createClient(url, serviceRole, { auth: { autoRefreshToken: false, persistSession: false } });
  const sub = event?.subscriptionNotification;
  const oneTime = event?.oneTimeProductNotification;

  if (sub?.purchaseToken) {
    const token = String(sub.purchaseToken);
    const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`;
    const playResponse = await fetch(verifyUrl, { headers: { Authorization: `Bearer ${access.token}` } });
    if (!playResponse.ok) return out(502, { error: "google_play_verification_failed", status: playResponse.status });
    const playBody = await playResponse.json();
    const state = String(playBody.subscriptionState || "");
    const expiries = (Array.isArray(playBody.lineItems) ? playBody.lineItems : [])
      .map((item: any) => item?.expiryTime).filter(Boolean).sort();
    const expiresAt = expiries.at(-1) || null;
    const notificationType = Number(sub.notificationType || 0);
    // REVOKED notifications must close access immediately even if an old expiry remains.
    const revoke = notificationType === 12;

    const { error } = await admin.rpc("reconcile_play_entitlement_v1", {
      p_purchase_token: token,
      p_play_state: state,
      p_expires_at: expiresAt,
      p_revoke: revoke,
    });
    if (error) return out(500, { error: "reconciliation_failed" });
    return out(204, {});
  }

  if (oneTime?.purchaseToken) {
    // One-time revocation/refund is recorded for audit. Previously granted ordinary Style is not
    // silently deleted here; Son Coin clawback requires an explicit support/reconciliation policy.
    const token = String(oneTime.purchaseToken);
    const notificationType = Number(oneTime.notificationType || 0);
    const revoke = notificationType === 2; // ONE_TIME_PRODUCT_CANCELED
    const { error } = await admin.rpc("reconcile_play_entitlement_v1", {
      p_purchase_token: token,
      p_play_state: revoke ? "ONE_TIME_PRODUCT_CANCELED" : "PURCHASED",
      p_expires_at: null,
      p_revoke: revoke,
    });
    if (error) return out(500, { error: "reconciliation_failed" });
    return out(204, {});
  }

  // Test notifications and unsupported message variants are acknowledged without state changes.
  return out(204, {});
});
