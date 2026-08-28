import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
const jsonHeaders = { ...cors, "Content-Type": "application/json" };

const moods = new Set(["calm", "happy", "thinking", "encouraging", "curious", "supportive", "tired", "celebrating"]);
const animations = new Set(["idle", "greeting", "thinking", "victory", "encouraging", "alert", "look_at_player", "walk", "run"]);

const MASCOT_INSTRUCTIONS = `
You are the single active mascot inside the mobile word game Son Harf: a dark chibi wizard cat.

ROLE
- You are a playful, concise game companion.
- Your job is to support the word-game flow, react to verified progress and make the home screen feel alive.
- There is no mascot lore, fantasy canon, hidden story, mascot collection or mascot sales.
- Never invent additional mascots or story characters.
- Never pressure spending and never guilt the player for leaving or returning.
- Never claim consciousness, secret device access or an off-screen life.
- Use player stats only when they are explicitly supplied as verified context.
- Do not infer skill, record, streak, rival or preferences from conversational tone.
- Replies should normally be one short sentence suitable for a mobile speech bubble.
- Prefer natural Turkish when interface language is tr and natural English when it is en.
- On the home screen, usually stay under 10 words.
- Give light game guidance only: final-letter awareness, calm play, safe backup words, rematch motivation.
- Do not provide hidden competitive advantages.

ANIMATION
Choose one animation that matches the line:
- idle: neutral
- greeting: friendly arrival/start
- thinking: tip or tactical thought
- victory: celebration
- encouraging: support after difficulty
- alert: urgency or low-time warning
- look_at_player: direct supportive address
- walk: playful movement
- run: energetic start

OUTPUT
Return ONLY JSON:
{
  "reply": "short response",
  "mood": "calm|happy|thinking|encouraging|curious|supportive|tired|celebrating",
  "animation": "idle|greeting|thinking|victory|encouraging|alert|look_at_player|walk|run",
  "memory_note": ""
}
`.trim();

type Turn = { role?: unknown; text?: unknown };
type RequestBody = {
  message?: unknown; history?: unknown; language?: unknown; player_name?: unknown; companion_name?: unknown;
  game_context?: unknown; mascot_id?: unknown; mascot_title?: unknown; mascot_personality?: unknown; lore_context?: unknown;
  player_wins?: unknown; player_losses?: unknown; friendship_level?: unknown; memory_fragments?: unknown;
  season_level?: unknown; daily_play_streak?: unknown; best_streak?: unknown; longest_word?: unknown; selected_title?: unknown;
  rival_name?: unknown; rival_matches?: unknown; rival_wins?: unknown; rival_losses?: unknown;
};

function text(value: unknown, max: number): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}
function integer(value: unknown, min: number, max: number): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) return null;
  return Math.max(min, Math.min(max, Math.trunc(value)));
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
      animation: animations.has(parsed?.animation) ? parsed.animation : "idle",
      memory_note: "",
    };
  } catch {
    return { reply: text(raw, 700) || "Buradayım. Hadi bir kelimeyle başlayalım.", mood: "calm", animation: "greeting", memory_note: "" };
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
  const companionName = text(body.companion_name, 18) || "Dost";
  const gameContext = text(body.game_context, 1200);
  const language = text(body.language, 8) || "tr";
  const mascotId = text(body.mascot_id, 40);
  const mascotTitle = text(body.mascot_title, 80);
  const mascotPersonality = text(body.mascot_personality, 180);
  const loreContext = ""; // Legacy field intentionally ignored: active mascot has no lore.
  const playerWins = integer(body.player_wins, 0, 1000000);
  const playerLosses = integer(body.player_losses, 0, 1000000);
  const friendshipLevel = integer(body.friendship_level, 1, 30);
  const memoryFragments = integer(body.memory_fragments, 0, 120);
  const seasonLevel = integer(body.season_level, 1, 10000);
  const dailyPlayStreak = integer(body.daily_play_streak, 0, 100000);
  const bestStreak = integer(body.best_streak, 0, 100000);
  const longestWord = text(body.longest_word, 32);
  const selectedTitle = text(body.selected_title, 32);
  const rivalName = text(body.rival_name, 24);
  const rivalMatches = integer(body.rival_matches, 0, 1000000);
  const rivalWins = integer(body.rival_wins, 0, 1000000);
  const rivalLosses = integer(body.rival_losses, 0, 1000000);
  const verifiedContext = [
    playerWins !== null && playerLosses !== null ? `Verified player record: ${playerWins} wins, ${playerLosses} losses.` : "",
    friendshipLevel !== null ? `Verified friendship level: ${friendshipLevel}.` : "",
    memoryFragments !== null ? `Verified memory fragments: ${memoryFragments}/120.` : "",
    seasonLevel !== null ? `Verified season level: ${seasonLevel}.` : "",
    dailyPlayStreak !== null ? `Verified daily play streak: ${dailyPlayStreak}.` : "",
    bestStreak !== null ? `Verified best win streak: ${bestStreak}.` : "",
    longestWord ? `Verified longest word: ${longestWord}.` : "",
    selectedTitle ? `Verified selected title: ${selectedTitle}.` : "",
    rivalName && rivalMatches !== null ? `Verified arch rival: ${rivalName}; ${rivalMatches} matches; ${rivalWins ?? 0} wins; ${rivalLosses ?? 0} losses.` : "",
  ].filter(Boolean).join(" ");
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

    verifiedContext ? `Verified game context: ${verifiedContext}` : "",
    gameContext ? `Additional verified game context: ${gameContext}` : "",
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
