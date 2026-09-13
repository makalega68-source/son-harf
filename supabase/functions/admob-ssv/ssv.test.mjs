import { generateKeyPairSync, sign } from "node:crypto";
import assert from "node:assert/strict";
import test from "node:test";
import { verifyCallback } from "./ssv.mjs";
const {privateKey, publicKey} = generateKeyPairSync("ec", {namedCurve:"prime256v1"});
const keys = [{keyId:123,pem:publicKey.export({type:"spki",format:"pem"})}];
const raw = "ad_unit=test-unit&custom_data=aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa&reward_amount=10&transaction_id=transaction-123&user_id=bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb";
function signed(value = raw) {
  const signature = sign("sha256", Buffer.from(value), privateKey).toString("base64url");
  return "https://example.invalid/admob-ssv?" + value + "&signature=" + signature + "&key_id=123";
}
test("valid DER ECDSA callback resolves the signed account and intent", () => {
  assert.deepEqual(verifyCallback(signed(),keys), {
    userId:"bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb",intentId:"aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa",
    transactionId:"transaction-123",adUnit:"test-unit",
  });
});
test("tampering with account, amount or ad unit fails", () => {
  for (const [a,b] of [["reward_amount=10","reward_amount=999"],["test-unit","other-unit"],["bbbbbbbb-bbbb","cccccccc-cccc"]]) {
    assert.throws(() => verifyCallback(signed().replace(a,b),keys),/invalid_signature/);
  }
});
test("untrusted signing key is rejected", () => {
  assert.throws(() => verifyCallback(signed().replace("key_id=123","key_id=999"),keys),/unknown_key/);
});
test("signed duplicate parameters are rejected", () => {
  assert.throws(() => verifyCallback(signed(raw + "&user_id=cccccccc-cccc-4ccc-cccc-cccccccccccc"),keys),/duplicate_parameter/);
});
test("unsigned fields after the signature are rejected", () => {
  assert.throws(() => verifyCallback(signed()+"&user_id=attacker",keys),/invalid_signature_fields/);
});
test("missing signature and invalid account cannot authorize a reward", () => {
  assert.throws(() => verifyCallback("https://example.invalid/?"+raw,keys),/missing_signature/);
  assert.throws(() => verifyCallback(signed(raw.replace("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb","attacker")),keys),/invalid_reward_identity/);
});
