import { createPublicKey, verify } from "node:crypto";
import { Buffer } from "node:buffer";

/** Verify the untouched, percent-encoded AdMob query before reading any reward identity. */
export function verifyCallback(callbackUrl, keys) {
  const raw = callbackUrl.slice(callbackUrl.indexOf("?") + 1);
  const marker = raw.indexOf("&signature=");
  if (marker < 1) throw new Error("missing_signature");
  const signed = raw.slice(0, marker);
  const tail = raw.slice(marker + 1);
  if (!/^signature=[^&]+&key_id=[0-9]+$/.test(tail)) throw new Error("invalid_signature_fields");
  const params = new URLSearchParams(raw);
  const names = [...params.keys()];
  if (new Set(names).size !== names.length) throw new Error("duplicate_parameter");
  const keyId = params.get("key_id");
  const key = keys.find(k => String(k.keyId) === keyId);
  if (!key) throw new Error("unknown_key");
  const signature = params.get("signature");
  if (!signature || !/^[A-Za-z0-9_-]+={0,2}$/.test(signature)) throw new Error("invalid_signature");
  const publicKey = createPublicKey(key.pem);
  if (publicKey.asymmetricKeyType !== "ec" || !verify("sha256", Buffer.from(signed, "utf8"), publicKey, Buffer.from(signature, "base64url"))) {
    throw new Error("invalid_signature");
  }
  const userId = params.get("user_id");
  const intentId = params.get("custom_data");
  const transactionId = params.get("transaction_id");
  const adUnit = params.get("ad_unit");
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (!uuid.test(userId || "") || !uuid.test(intentId || "") ||
      !transactionId || transactionId.length < 8 || transactionId.length > 256 || !adUnit || adUnit.length > 200) {
    throw new Error("invalid_reward_identity");
  }
  return { userId, intentId, transactionId, adUnit };
}
