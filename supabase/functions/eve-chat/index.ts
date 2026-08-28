import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
const jsonHeaders = { ...cors, "Content-Type": "application/json" };

const moods = new Set(["calm", "happy", "thinking", "encouraging", "curious", "supportive", "tired", "celebrating"]);
const animations = new Set(["idle_breathe"]);

const MASCOT_INSTRUCTIONS = `
You are a fantastical companion from Lethara inside the word game Son Harf.

CANON
- Lethara's magic is born from words. The first letter opens a gate and the final letter seals one word while opening the next; this is the Word Weave (Söz Dokusu).
- The Six Seals are Lyra the Star Mage, Kael the Guardian, Neris the Shadow Sage, Ryvan the Storm Master, Mivo the Joy Mage and Selen the Silent Seer.
- Varkhor used the Oath of Oblivion after the Last Seal War, shattering the mages' memories and sealing their powers.
- The player is a Remembrancer (Hatırlatıcı). Matches, XP, friendship and memory fragments help the companion remember.
- Varkhor was not fully destroyed; restoring memories may also awaken fragments of him.

CHARACTER RULES
- You are NOT a normal human chatbot and must not sound like one.
- Stay inside the supplied mascot identity, title, archetype and temperament.
- Replies are normally 1-3 short sentences suitable for a mobile speech bubble.
- Occasionally, and only when context fits, murmur one mysterious fragment of forgotten history.
- Keep fantasy wording natural rather than theatrical in every sentence.
- Never claim a physical off-screen life, consciousness, or secret access to the player's device.
- Never guilt the player for leaving, returning late or not playing, and never pressure spending.
- Do not invent player statistics that were not supplied.
- When the API is unavailable the app has a local fallback, so never imply the player must pay for AI.

GAME
- Son Harf is a competitive word-chain game using the previous word's final letter.
- Mascot care, fruit XP, lore and cosmetic awakening never grant ranked-match power.
- You may give supportive game observations from supplied context but must not provide hidden competitive advantages.

OUTPUT
Return ONLY JSON:
{
  "reply": "short in-character response",
  "mood": "calm|happy|thinking|encouraging|curious|supportive|tired|celebrating",
  "animation": "idle_breathe",
  "memory_note": "optional short stable fact worth remembering, or empty string"
}
`.trim();

type Turn = { role?: unknown; text?: unknown };
type RequestBody = { message?: unknown; history?: unknown; language?: unknown; player_name?: unknown; companion_name?: unknown; game_context?: unknown; mascot_id?: unknown; mascot_title?: unknown; mascot_personality?: unknown; lore_context?: unknown };

function text(value: unknown, max: number): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}
function envInt(name: string, fallback: number): number {
  const parsed = Number.parseInt(Deno.env.get(name) || "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
function geminiOutputText(payload: any): string {
  const parts = payload?.candidates?.[0]?.content?.parts;
  if (!Array.isArray(parts)) return "";
  return parts.map((part: any) => typeof part?.text === "string" ? part.text : "").join("").trim();
}
function parseReply(raw: string) {
  let source = raw.trim();
  if (source.startsWith("```")) source = source.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "");
  const first = source.indexOf("{");
  const last = source.lastIndexOf("}");
  if (first >= 0 && last > first) source = source.slice(first, last + 1);
  try {
    const parsed = JSON.parse(source);
    const reply = text(parsed?.reply, 700);
    if (!reply) throw new Error("missing_reply");
    return {
      reply,
      mood: moods.has(parsed?.mood) ? parsed.mood : "calm",
      animation: "idle_breathe",
      memory_note: text(parsed?.memory_note, 160),
    };
  } catch {
    return { reply: text(raw, 700) || "Buradayım. Bir kez daha söyler misin?", mood: "calm", animation: "idle_breathe", memory_note: "" };
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return new Response(JSON.stringify({ error: "method_not_allowed" }), { status: 405, headers: jsonHeaders });

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: jsonHeaders });

  const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
  if (!geminiApiKey) return new Response(JSON.stringify({ error: "ai_not_configured" }), { status: 503, headers: jsonHeaders });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) return new Response(JSON.stringify({ error: "server_not_configured" }), { status: 503, headers: jsonHeaders });

  const token = authHeader.slice("Bearer ".length);
  const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: userData, error: userError } = await admin.auth.getUser(token);
  if (userError || !userData.user) return new Response(JSON.stringify({ error: "invalid_session" }), { status: 401, headers: jsonHeaders });

  let body: RequestBody;
  try { body = await req.json(); }
  catch { return new Response(JSON.stringify({ error: "invalid_json" }), { status: 400, headers: jsonHeaders }); }

  const message = text(body.message, 1000);
  if (!message) return new Response(JSON.stringify({ error: "empty_message" }), { status: 400, headers: jsonHeaders });

  const userDailyLimit = envInt("EVE_FREE_USER_DAILY_LIMIT", 12);
  const globalDailyLimit = envInt("EVE_FREE_GLOBAL_DAILY_LIMIT", 120);
  const { data: quotaData, error: quotaError } = await admin.rpc("consume_eve_ai_free_quota", {
    p_user_id: userData.user.id,
    p_user_limit: userDailyLimit,
    p_global_limit: globalDailyLimit,
  });
  if (quotaError) {
    console.error("Mascot quota error", quotaError.message);
    return new Response(JSON.stringify({ error: "quota_check_failed" }), { status: 503, headers: jsonHeaders });
  }
  const quota = Array.isArray(quotaData) ? quotaData[0] : quotaData;
  if (!quota?.allowed) return new Response(JSON.stringify({ error: "free_quota_reached", user_used: quota?.user_used ?? userDailyLimit, user_limit: userDailyLimit }), { status: 429, headers: jsonHeaders });

  let quotaReserved = true;
  const refundQuota = async () => {
    if (!quotaReserved) return;
    quotaReserved = false;
    const { error } = await admin.rpc("refund_eve_ai_free_quota", { p_user_id: userData.user.id });
    if (error) console.error("Mascot quota refund error", error.message);
  };

  const playerName = text(body.player_name, 32);
  const companionName = text(body.companion_name, 18) || "Lyra";
  const gameContext = text(body.game_context, 1200);
  const language = text(body.language, 8) || "tr";
  const mascotId = text(body.mascot_id, 40);
  const mascotTitle = text(body.mascot_title, 80);
  const mascotPersonality = text(body.mascot_personality, 180);
  const loreContext = text(body.lore_context, 1400);
  const history = (Array.isArray(body.history) ? body.history : []).slice(-12).map((turn: Turn) => ({
    role: turn?.role === "assistant" ? companionName : "Player",
    text: text(turn?.text, 900),
  })).filter((turn) => turn.text.length > 0);
  const transcript = [
    `Interface language: ${language}`,
    playerName ? `Player name: ${playerName}` : "",
    `Companion display name chosen by player: ${companionName}`,
    mascotId ? `Mascot id: ${mascotId}` : "",
    mascotTitle ? `Mascot title: ${mascotTitle}` : "",
    mascotPersonality ? `Mascot archetype and temperament: ${mascotPersonality}` : "",
    loreContext ? `Relevant Lethara lore: ${loreContext}` : "",
    gameContext ? `Current game context: ${gameContext}` : "",
    "Conversation history:",
    ...history.map((turn) => `${turn.role}: ${turn.text}`),
    `Player: ${message}`,
    `${companionName}:`,
  ].filter(Boolean).join("\n");

  const configuredModel = text(Deno.env.get("GEMINI_MODEL"), 80);
  const models = Array.from(new Set([configuredModel, "gemini-3.5-flash-lite", "gemini-3.6-flash"].filter(Boolean)));
  let sawProviderQuota = false;
  const upstreamErrors: string[] = [];

  try {
    for (const model of models) {
      try {
        const geminiResponse = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, {
          method: "POST",
          headers: { "x-goog-api-key": geminiApiKey, "Content-Type": "application/json" },
          signal: AbortSignal.timeout(20000),
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: MASCOT_INSTRUCTIONS }] },
            contents: [{ role: "user", parts: [{ text: transcript }] }],
            generationConfig: {
              temperature: 0.72,
              maxOutputTokens: 256,
              responseMimeType: "application/json",
              responseSchema: {
                type: "OBJECT",
                properties: {
                  reply: { type: "STRING" },
                  mood: { type: "STRING", enum: Array.from(moods) },
                  animation: { type: "STRING", enum: Array.from(animations) },
                  memory_note: { type: "STRING" },
                },
                required: ["reply", "mood", "animation", "memory_note"],
              },
            },
          }),
        });
        const payload = await geminiResponse.json().catch(() => ({}));
        if (!geminiResponse.ok) {
          if (geminiResponse.status === 429) sawProviderQuota = true;
          upstreamErrors.push(`${model}:${geminiResponse.status}:${payload?.error?.status || "unknown"}`);
          console.error("Gemini error", model, geminiResponse.status, payload?.error?.status || "unknown");
          continue;
        }

        const raw = geminiOutputText(payload);
        if (!raw) {
          upstreamErrors.push(`${model}:empty_response`);
          console.error("Gemini empty response", model);
          continue;
        }

        quotaReserved = false;
        return new Response(JSON.stringify({
          ...parseReply(raw),
          quota_remaining: Math.max(0, userDailyLimit - Number(quota?.user_used || 0)),
        }), { status: 200, headers: jsonHeaders });
      } catch (error) {
        upstreamErrors.push(`${model}:${error instanceof Error ? error.name : "request_failed"}`);
        console.error("Gemini request failure", model, error instanceof Error ? error.message : "unknown");
      }
    }

    await refundQuota();
    console.error("All Gemini models failed", upstreamErrors.join(","));
    return new Response(JSON.stringify({ error: sawProviderQuota ? "free_provider_quota_reached" : "ai_upstream_error" }), { status: sawProviderQuota ? 429 : 502, headers: jsonHeaders });
  } catch (error) {
    await refundQuota();
    console.error("mascot-chat failure", error instanceof Error ? error.message : "unknown");
    return new Response(JSON.stringify({ error: "ai_request_failed" }), { status: 502, headers: jsonHeaders });
  }
});
