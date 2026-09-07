import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { OAuth2Client } from "npm:google-auth-library@9";

const jsonHeaders = { "Content-Type": "application/json", "Cache-Control": "no-store" };

function response(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

function decodePubSubData(data: string): Record<string, any> {
  const binary = atob(data);
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return response(405, { error: "method_not_allowed" });

  const url = Deno.env.get("SUPABASE_URL");
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const expectedAudience = Deno.env.get("GOOGLE_PLAY_RTDN_AUDIENCE");
  const expectedPushServiceAccount = Deno.env.get("GOOGLE_PLAY_RTDN_PUSH_SERVICE_ACCOUNT");
  const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME") || "com.sonharf.game";
  if (!url || !serviceRole || !expectedAudience || !expectedPushServiceAccount) {
    return response(503, { error: "rtdn_not_configured" });
  }

  const authorization = req.headers.get("Authorization") || "";
  if (!authorization.startsWith("Bearer ")) return response(401, { error: "missing_push_identity" });
  const idToken = authorization.slice("Bearer ".length).trim();
  try {
    const ticket = await new OAuth2Client().verifyIdToken({ idToken, audience: expectedAudience });
    const payload = ticket.getPayload();
    if (!payload?.email_verified || payload.email !== expectedPushServiceAccount) {
      return response(403, { error: "invalid_push_identity" });
    }
  } catch {
    return response(401, { error: "invalid_push_token" });
  }

  let envelope: any;
  let notification: Record<string, any>;
  try {
    envelope = await req.json();
    notification = decodePubSubData(String(envelope?.message?.data || ""));
  } catch {
    return response(400, { error: "invalid_pubsub_payload" });
  }

  const messageId = String(envelope?.message?.messageId || "").trim();
  if (!messageId) return response(400, { error: "missing_message_id" });
  if (String(notification.packageName || "") !== packageName) return response(403, { error: "package_mismatch" });

  const voided = notification.voidedPurchaseNotification;
  const pendingRefund = notification.pendingRefundReviewNotification;
  const subscription = notification.subscriptionNotification;
  const oneTime = notification.oneTimeProductNotification;
  const eventType = voided ? "voided_purchase" : pendingRefund ? "pending_refund_review" : subscription ? "subscription" : oneTime ? "one_time" : notification.testNotification ? "test" : "unknown";
  const purchaseToken = String(voided?.purchaseToken || subscription?.purchaseToken || oneTime?.purchaseToken || "").trim() || null;
  const orderId = String(voided?.orderId || pendingRefund?.orderId || "").trim() || null;
  const eventMillis = Number(notification.eventTimeMillis || 0);
  const eventTime = Number.isFinite(eventMillis) && eventMillis > 0 ? new Date(eventMillis).toISOString() : null;

  const admin = createClient(url, serviceRole, { auth: { autoRefreshToken: false, persistSession: false } });
  const { data: claimed, error: claimError } = await admin.rpc("claim_play_rtdn_event_v1", {
    p_message_id: messageId,
    p_event_type: eventType,
    p_purchase_token: purchaseToken,
    p_order_id: orderId,
    p_event_time: eventTime,
  });
  if (claimError) return response(500, { error: "event_claim_failed" });
  if (claimed !== true) return response(200, { ok: true, duplicate: true });

  let processingError: string | null = null;
  try {
    if (voided && purchaseToken) {
      const { error } = await admin.rpc("reconcile_play_entitlement_v2", {
        p_purchase_token: purchaseToken,
        p_play_state: `VOIDED:${String(voided.refundType || "unknown")}`,
        p_expires_at: null,
        p_revoke: true,
      });
      if (error) throw error;
    }
    // PendingRefundReviewNotification is deliberately not a clawback signal: Google has not yet
    // decided the chargeback. Subscription/one-time lifecycle events are receipts that trigger
    // authoritative verification through the normal Play API path rather than trusting RTDN data.
  } catch (error) {
    processingError = error instanceof Error ? error.message : String(error);
  }

  await admin.rpc("finish_play_rtdn_event_v1", {
    p_message_id: messageId,
    p_error: processingError,
  });

  if (processingError) return response(500, { error: "rtdn_processing_failed" });
  return response(200, { ok: true, eventType });
});
