import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { GoogleAuth, OAuth2Client } from "npm:google-auth-library@9";

const jsonHeaders = { "Content-Type": "application/json", "Cache-Control": "no-store" };

function out(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

function decodeBase64Json(value: string): Record<string, any> {
  const bytes = Uint8Array.from(atob(value), (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return out(405, { error: "method_not_allowed" });

  const url = Deno.env.get("SUPABASE_URL");
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const serviceAccountJson = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
  const expectedAudience = Deno.env.get("GOOGLE_PLAY_RTDN_AUDIENCE");
  const expectedPushServiceAccount = Deno.env.get("GOOGLE_PLAY_RTDN_PUSH_SERVICE_ACCOUNT");
  const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME") || "com.sonharf.game";
  if (!url || !serviceRole || !serviceAccountJson || !expectedAudience || !expectedPushServiceAccount) {
    return out(503, { error: "rtdn_not_configured" });
  }

  // Pub/Sub authenticated push identity is the perimeter. A shared secret is deliberately not
  // accepted as a substitute for a Google-signed ID token with the configured audience/email.
  const authorization = req.headers.get("Authorization") || "";
  if (!authorization.startsWith("Bearer ")) return out(401, { error: "missing_push_identity" });
  const idToken = authorization.slice("Bearer ".length).trim();
  try {
    const ticket = await new OAuth2Client().verifyIdToken({ idToken, audience: expectedAudience });
    const payload = ticket.getPayload();
    if (payload?.email_verified !== true || payload.email !== expectedPushServiceAccount) {
      return out(403, { error: "invalid_push_identity" });
    }
  } catch {
    return out(401, { error: "invalid_push_token" });
  }

  let envelope: any;
  let event: Record<string, any>;
  try {
    envelope = await req.json();
    const encodedData = envelope?.message?.data;
    if (typeof encodedData !== "string" || !encodedData) return out(400, { error: "missing_pubsub_data" });
    event = decodeBase64Json(encodedData);
  } catch {
    return out(400, { error: "invalid_pubsub_payload" });
  }

  const messageId = String(envelope?.message?.messageId || "").trim();
  if (!messageId) return out(400, { error: "missing_message_id" });
  if (String(event.packageName || "") !== packageName) return out(403, { error: "package_mismatch" });

  const voided = event.voidedPurchaseNotification;
  const pendingRefund = event.pendingRefundReviewNotification;
  const subscription = event.subscriptionNotification;
  const oneTime = event.oneTimeProductNotification;
  const eventType = voided
    ? "voided_purchase"
    : pendingRefund
      ? "pending_refund_review"
      : subscription
        ? "subscription"
        : oneTime
          ? "one_time"
          : event.testNotification
            ? "test"
            : "unknown";
  const purchaseToken = String(
    voided?.purchaseToken || subscription?.purchaseToken || oneTime?.purchaseToken || "",
  ).trim() || null;
  const orderId = String(voided?.orderId || pendingRefund?.orderId || "").trim() || null;
  const eventMillis = Number(event.eventTimeMillis || 0);
  const eventTime = Number.isFinite(eventMillis) && eventMillis > 0
    ? new Date(eventMillis).toISOString()
    : null;

  const admin = createClient(url, serviceRole, { auth: { autoRefreshToken: false, persistSession: false } });
  const { data: claimState, error: claimError } = await admin.rpc("claim_play_rtdn_event_v2", {
    p_message_id: messageId,
    p_event_type: eventType,
    p_purchase_token: purchaseToken,
    p_order_id: orderId,
    p_event_time: eventTime,
  });
  if (claimError) return out(500, { error: "event_claim_failed" });

  if (claimState !== true) {
    // Distinguish a completed duplicate from a concurrently leased/unfinished delivery. Ack only
    // the former; retry the latter so a failed worker cannot permanently swallow the event.
    const { data: rows, error: statusError } = await admin
      .from("play_rtdn_events")
      .select("processed_at,processing_error")
      .eq("message_id", messageId)
      .limit(1);
    if (statusError || !rows?.length) return out(503, { error: "event_status_unavailable" });
    if (rows[0]?.processed_at && !rows[0]?.processing_error) return out(204, {});
    return out(503, { error: "event_processing_busy" });
  }

  let credentials: Record<string, unknown>;
  try {
    credentials = JSON.parse(serviceAccountJson);
  } catch {
    await admin.rpc("finish_play_rtdn_event_v2", {
      p_message_id: messageId,
      p_error: "invalid_google_service_account_json",
    });
    return out(500, { error: "invalid_google_service_account_json" });
  }

  let publisherToken: string | null = null;
  async function getPublisherToken(): Promise<string> {
    if (publisherToken) return publisherToken;
    const googleAuth = new GoogleAuth({
      credentials,
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const authClient = await googleAuth.getClient();
    const access = await authClient.getAccessToken();
    if (!access.token) throw new Error("google_oauth_failed");
    publisherToken = access.token;
    return publisherToken;
  }

  let processingError: string | null = null;
  try {
    if (voided && purchaseToken) {
      // VoidedPurchaseNotification is the authoritative clawback signal. The database RPC is
      // provenance-aware and delegates subscription tokens to the existing subscription flow.
      const { error } = await admin.rpc("reconcile_play_entitlement_v2", {
        p_purchase_token: purchaseToken,
        p_play_state: `VOIDED:${String(voided.refundType ?? "unknown")}`,
        p_expires_at: null,
        p_revoke: true,
      });
      if (error) throw error;
    } else if (pendingRefund) {
      // A pending refund review is not a completed refund. Record/ack it, but do not claw back.
    } else if (subscription?.purchaseToken) {
      const token = String(subscription.purchaseToken);
      const accessToken = await getPublisherToken();
      const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`;
      const playResponse = await fetch(verifyUrl, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (!playResponse.ok) throw new Error(`google_play_subscription_verification_failed:${playResponse.status}`);
      const playBody = await playResponse.json();
      const state = String(playBody.subscriptionState || "");
      const expiries = (Array.isArray(playBody.lineItems) ? playBody.lineItems : [])
        .map((item: any) => item?.expiryTime)
        .filter(Boolean)
        .sort();
      const expiresAt = expiries.at(-1) || null;
      const notificationType = Number(subscription.notificationType || 0);
      const revoke = notificationType === 12;
      const { error } = await admin.rpc("reconcile_play_entitlement_v2", {
        p_purchase_token: token,
        p_play_state: state,
        p_expires_at: expiresAt,
        p_revoke: revoke,
      });
      if (error) throw error;
    } else if (oneTime?.purchaseToken) {
      // PURCHASED/CANCELED lifecycle notifications are state signals, not refund clawbacks.
      // Query Google authoritatively and update a known purchase, but never create a grant here
      // because RTDN does not bind the event to an app user. User verification remains the grant path.
      const token = String(oneTime.purchaseToken);
      const accessToken = await getPublisherToken();
      const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/productsv2/tokens/${encodeURIComponent(token)}`;
      const playResponse = await fetch(verifyUrl, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (!playResponse.ok) throw new Error(`google_play_product_verification_failed:${playResponse.status}`);
      const playBody = await playResponse.json();
      const state = String(
        playBody.purchaseStateContext?.purchaseState ||
          `ONE_TIME_NOTIFICATION_${String(oneTime.notificationType ?? "unknown")}`,
      );
      const { error } = await admin.rpc("reconcile_play_entitlement_v2", {
        p_purchase_token: token,
        p_play_state: state,
        p_expires_at: null,
        p_revoke: false,
      });
      if (error) throw error;
    }
  } catch (error) {
    processingError = error instanceof Error ? error.message : String(error);
  }

  const { error: finishError } = await admin.rpc("finish_play_rtdn_event_v2", {
    p_message_id: messageId,
    p_error: processingError,
  });
  if (finishError) return out(500, { error: "event_finish_failed" });
  if (processingError) return out(500, { error: "rtdn_processing_failed" });
  return out(204, {});
});
