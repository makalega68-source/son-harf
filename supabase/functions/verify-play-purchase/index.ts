import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library@9";

const jsonHeaders = { "Content-Type": "application/json" };
const subscriptionProducts = new Set(["vip_monthly", "vip_yearly", "season_pass_monthly"]);
const oneTimeProducts = new Set(["coins_500", "coins_1500", "coins_3500", "coins_8000", "starter_style_pack", "theme_neon"]);
const consumableProducts = new Set(["coins_500", "coins_1500", "coins_3500", "coins_8000"]);
const entitlementStates = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED",
]);

function response(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return response(405, { error: "method_not_allowed" });

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return response(401, { error: "unauthorized" });

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const serviceAccountJson = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
  const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME") || "com.sonharf.game";
  if (!url || !anonKey || !serviceRole) return response(500, { error: "supabase_not_configured" });
  if (!serviceAccountJson) return response(503, { error: "google_play_not_configured" });

  let input: { productId?: string; purchaseToken?: string };
  try { input = await req.json(); } catch { return response(400, { error: "invalid_json" }); }

  const productId = input.productId?.trim() || "";
  const purchaseToken = input.purchaseToken?.trim() || "";
  const isSubscription = subscriptionProducts.has(productId);
  if (!isSubscription && !oneTimeProducts.has(productId)) return response(400, { error: "unsupported_product" });
  if (purchaseToken.length < 8) return response(400, { error: "invalid_purchase_token" });

  const userClient = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return response(401, { error: "invalid_session" });

  let credentials: Record<string, unknown>;
  try { credentials = JSON.parse(serviceAccountJson); } catch { return response(500, { error: "invalid_google_service_account_json" }); }

  const googleAuth = new GoogleAuth({ credentials, scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
  const authClient = await googleAuth.getClient();
  const access = await authClient.getAccessToken();
  const accessToken = access.token;
  if (!accessToken) return response(502, { error: "google_oauth_failed" });

  const encodedToken = encodeURIComponent(purchaseToken);
  const verifyUrl = isSubscription
    ? `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodedToken}`
    : `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/productsv2/tokens/${encodedToken}`;

  const playResponse = await fetch(verifyUrl, { headers: { Authorization: `Bearer ${accessToken}` } });
  const playBody = await playResponse.json().catch(() => ({}));
  if (!playResponse.ok) return response(502, { error: "google_play_verification_failed", status: playResponse.status });

  let orderId: string | null = null;
  let expiresAt: string | null = null;
  let acknowledgementState: string | null = null;

  if (isSubscription) {
    const state = String(playBody.subscriptionState || "");
    const lineItems = Array.isArray(playBody.lineItems) ? playBody.lineItems : [];
    if (!lineItems.some((item: any) => item?.productId === productId)) return response(409, { error: "product_mismatch" });
    if (!entitlementStates.has(state)) return response(409, { error: "subscription_not_entitled", state });
    const expiries = lineItems.map((item: any) => item?.expiryTime).filter(Boolean).sort();
    expiresAt = expiries.at(-1) || null;
    if (!expiresAt || Date.parse(expiresAt) <= Date.now()) return response(409, { error: "subscription_expired" });
    orderId = playBody.latestOrderId || lineItems.map((item: any) => item?.latestSuccessfulOrderId).find(Boolean) || null;
    acknowledgementState = playBody.acknowledgementState || null;
  } else {
    const state = String(playBody.purchaseStateContext?.purchaseState || "");
    const lineItems = Array.isArray(playBody.productLineItem) ? playBody.productLineItem : [];
    if (!lineItems.some((item: any) => item?.productId === productId)) return response(409, { error: "product_mismatch" });
    if (state !== "PURCHASED") return response(409, { error: "purchase_not_completed", state });
    orderId = playBody.orderId || null;
    acknowledgementState = playBody.acknowledgementState || null;
  }

  const admin = createClient(url, serviceRole, { auth: { autoRefreshToken: false, persistSession: false } });
  const { data: grantData, error: grantError } = await admin.rpc("apply_verified_play_purchase_v1", {
    p_user_id: userData.user.id, p_product_id: productId, p_purchase_token: purchaseToken, p_order_id: orderId, p_expires_at: expiresAt,
  });
  if (grantError) return response(500, { error: "entitlement_grant_failed" });

  let acknowledged = acknowledgementState?.includes("ACKNOWLEDGED") === true;
  let consumed = false;

  if (consumableProducts.has(productId)) {
    const consumeUrl =
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodedToken}:consume`;
    const consumeResponse = await fetch(consumeUrl, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    consumed = consumeResponse.ok;
    acknowledged = acknowledged || consumed;
    if (!consumed) {
      return response(502, {
        error: "google_play_consume_failed",
        verified: true,
        productId,
        grant: grantData,
      });
    }
  } else if (!acknowledged) {
    const ackUrl = isSubscription
      ? `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptions/${encodeURIComponent(productId)}/tokens/${encodedToken}:acknowledge`
      : `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodedToken}:acknowledge`;
    const ackResponse = await fetch(ackUrl, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    acknowledged = ackResponse.ok;
  }

  return response(200, { verified: true, productId, acknowledged, consumed, grant: grantData });
});
