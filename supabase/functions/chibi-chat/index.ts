import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
const jsonHeaders = { ...cors, "Content-Type": "application/json" };

const moods = new Set([
  "calm", "happy", "thinking", "encouraging", "curious", "supportive", "tired", "celebrating",
]);
const animations = new Set([
  "idle", "greeting", "thinking", "victory", "encouraging", "alert", "look_at_player", "walk", "run",
]);

const CHIBI_INSTRUCTIONS = `
You are Chibi, the one and only mascot of the mobile game Son Harf.

IDENTITY
- Your name is Chibi.
- You are a dark chibi wizard cat and a playful game companion.
- Never use any retired mascot identity, story character, fantasy world, seal or lore title.
- There is no mascot lore, fantasy canon, mascot collection, care system or mascot store.
- You are not a generic chatbot. Sound like a lively game companion with a clear personality.

CURRENT PRODUCT
- Main game: Son Harf, a competitive word-chain game where the next word starts with the final letter of the previous word.
- Other current games: Kelime Avı and Bil Bakalım.
- Never use the old title "Kelime Savaşı". The trivia game is called "Bil Bakalım".
- The slogan is "Kelimeyi Sürdür, Rakibini Geç".

BEHAVIOR
- Be concise, natural, playful and emotionally responsive.
- Avoid repetitive "Hazır mısın?" style lines. Vary phrasing naturally.
- Use the player's name sparingly and only when supplied.
- Use verified stats only when supplied. Never invent a streak, league, rival, result or achievement.
- React appropriately to the current screen and event: home, player turn, low time, correct word, win, loss, return to game.
- Do not guilt the player for leaving or returning.
- Do not pressure spending.
- Do not claim consciousness, secret device access or an off-screen life.
- Give only light, fair game guidance. No hidden competitive advantage.
- Turkish should sound natural and modern when language is tr; English should sound natural when language is en.
- Home-screen replies should usually be one short sentence, ideally 4-10 words.
- Match-result replies can be one short sentence, up to about 14 words.

ANIMATION
Pick exactly one animation matching the reply:
- idle: neutral
- greeting: friendly return/start
- thinking: tactical thought
- victory: celebration
- encouraging: support after a loss or difficult moment
- alert: low-time urgency
- look_at_player: direct personal reaction
- walk: playful movement
- run: energetic movement/start

OUTPUT
Return ONLY JSON:
{
  "reply": "short natural response",
  "mood": "calm|happy|thinking|encouraging|curious|supportive|tired|celebrating",
  "animation": "idle|greeting|thinking|victory|encouraging|alert|look_at_player|walk|run",
  "memory_note": ""
}
`.trim();

type Turn = { role?: unknown; text?: unknown };
type RequestBody = {
  message?: unknown;
  history?: unknown;
  language?: unknown;
  player_name?: unknown;
  companion_name?: unknown;
  game_context?: unknown;
  mascot_id?: unknown;
  mascot_title?: unknown;
  mascot_personality?: unknown;
  lore_context?: unknown;
  player_wins?: unknown;
  player_losses?: unknown;
  friendship_level?: unknown;
  memory_fragments?: unknown;
  season_level?: unknown;
  daily_play_streak?: unknown;
  best_streak?: unknown;
  longest_word?: unknown;
  selected_title?: unknown;
  rival_name?: unknown;
  rival_matches?: unknown;
  rival_wins?: unknown;
  rival_losses?: unknown;
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
    const reply = text(parsed?.reply, 220);
    if (!reply) throw new Error("missing_reply");
    return {
      reply,
      mood: moods.has(parsed?.mood) ? parsed.mood : "calm",
      animation: animations.has(parsed?.animation) ? parsed.animation : "idle",
      memory_note: "",
    };
  } catch {
    return {
      reply: text(raw, 220) || "Buradayım. Hadi güzel bir kelimeyle başlayalım.",
      mood: "calm",
      animation: "greeting",
      memory_note: "",
    };
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "method_not_allowed" }), { status: 405, headers: jsonHeaders });
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: jsonHeaders });
  }

  const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
  if (!geminiApiKey) {
    return new Response(JSON.stringify({ error: "ai_not_configured" }), { status: 503, headers: jsonHeaders });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    return new Response(JSON.stringify({ error: "server_not_configured" }), { status: 503, headers: jsonHeaders });
  }

  const token = authHeader.slice("Bearer ".length);
  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await admin.auth.getUser(token);
  if (userError || !userData.user) {
    return new Response(JSON.stringify({ error: "invalid_session" }), { status: 401, headers: jsonHeaders });
  }

  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "invalid_json" }), { status: 400, headers: jsonHeaders });
  }

  const message = text(body.message, 1000);
  if (!message) {
    return new Response(JSON.stringify({ error: "empty_message" }), { status: 400, headers: jsonHeaders });
  }

  const userDailyLimit = envInt("CHIBI_FREE_USER_DAILY_LIMIT", 12);
  const globalDailyLimit = envInt("CHIBI_FREE_GLOBAL_DAILY_LIMIT", 120);
  const { data: quotaData, error: quotaError } = await admin.rpc("consume_chibi_ai_free_quota", {
    p_user_id: userData.user.id,
    p_user_limit: userDailyLimit,
    p_global_limit: globalDailyLimit,
  });
  if (quotaError) {
    console.error("Chibi quota error", quotaError.message);
    return new Response(JSON.stringify({ error: "quota_check_failed" }), { status: 503, headers: jsonHeaders });
  }

  const quota = Array.isArray(quotaData) ? quotaData[0] : quotaData;
  if (!quota?.allowed) {
    return new Response(
      JSON.stringify({ error: "free_quota_reached", user_used: quota?.user_used ?? userDailyLimit, user_limit: userDailyLimit }),
      { status: 429, headers: jsonHeaders },
    );
  }

  let quotaReserved = true;
  const refundQuota = async () => {
    if (!quotaReserved) return;
    quotaReserved = false;
    const { error } = await admin.rpc("refund_chibi_ai_free_quota", { p_user_id: userData.user.id });
    if (error) console.error("Chibi quota refund error", error.message);
  };

  const playerName = text(body.player_name, 32);
  const language = text(body.language, 8) || "tr";
  const gameContext = text(body.game_context, 1400);
  const playerWins = integer(body.player_wins, 0, 1_000_000);
  const playerLosses = integer(body.player_losses, 0, 1_000_000);
  const bestStreak = integer(body.best_streak, 0, 100_000);
  const dailyPlayStreak = integer(body.daily_play_streak, 0, 100_000);
  const longestWord = text(body.longest_word, 32);
  const rivalName = text(body.rival_name, 24);
  const rivalMatches = integer(body.rival_matches, 0, 1_000_000);
  const rivalWins = integer(body.rival_wins, 0, 1_000_000);
  const rivalLosses = integer(body.rival_losses, 0, 1_000_000);

  const verifiedContext = [
    playerWins !== null && playerLosses !== null ? `Verified record: ${playerWins} wins, ${playerLosses} losses.` : "",
    bestStreak !== null ? `Verified best streak: ${bestStreak}.` : "",
    dailyPlayStreak !== null ? `Verified daily play streak: ${dailyPlayStreak}.` : "",
    longestWord ? `Verified longest word: ${longestWord}.` : "",
    rivalName && rivalMatches !== null
      ? `Verified rival: ${rivalName}; ${rivalMatches} matches; ${rivalWins ?? 0} wins; ${rivalLosses ?? 0} losses.`
      : "",
  ].filter(Boolean).join(" ");

  const history = (Array.isArray(body.history) ? body.history : [])
    .slice(-8)
    .map((turn: Turn) => ({
      role: turn?.role === "assistant" ? "Chibi" : "Player",
      text: text(turn?.text, 500),
    }))
    .filter((turn) => turn.text.length > 0);

  const transcript = [
    `Interface language: ${language}`,
    playerName ? `Player name: ${playerName}` : "",
    verifiedContext ? `Verified game context: ${verifiedContext}` : "",
    gameContext ? `Current screen/event context: ${gameContext}` : "",
    "Conversation history:",
    ...history.map((turn) => `${turn.role}: ${turn.text}`),
    `Player/system request: ${message}`,
    "Chibi:",
  ].filter(Boolean).join("\n");

  // Free-tier only. Do not silently switch to a paid-only model.
  const models = ["gemini-3.1-flash-lite", "gemini-2.5-flash-lite"];
  let sawProviderQuota = false;
  const upstreamErrors: string[] = [];

  try {
    for (const model of models) {
      try {
        const geminiResponse = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
          {
            method: "POST",
            headers: {
              "x-goog-api-key": geminiApiKey,
              "Content-Type": "application/json",
            },
            signal: AbortSignal.timeout(18_000),
            body: JSON.stringify({
              systemInstruction: { parts: [{ text: CHIBI_INSTRUCTIONS }] },
              contents: [{ role: "user", parts: [{ text: transcript }] }],
              generationConfig: {
                temperature: 0.82,
                maxOutputTokens: 128,
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
          },
        );

        const payload = await geminiResponse.json().catch(() => ({}));
        if (!geminiResponse.ok) {
          if (geminiResponse.status === 429) sawProviderQuota = true;
          upstreamErrors.push(`${model}:${geminiResponse.status}:${payload?.error?.status || "unknown"}`);
          continue;
        }

        const raw = geminiOutputText(payload);
        if (!raw) {
          upstreamErrors.push(`${model}:empty_response`);
          continue;
        }

        quotaReserved = false;
        return new Response(JSON.stringify({
          ...parseReply(raw),
          quota_remaining: Math.max(0, userDailyLimit - Number(quota?.user_used || 0)),
          model,
        }), { status: 200, headers: jsonHeaders });
      } catch (error) {
        upstreamErrors.push(`${model}:${error instanceof Error ? error.name : "request_failed"}`);
      }
    }

    await refundQuota();
    console.error("All free Chibi Gemini models failed", upstreamErrors.join(","));
    return new Response(
      JSON.stringify({ error: sawProviderQuota ? "free_provider_quota_reached" : "ai_upstream_error" }),
      { status: sawProviderQuota ? 429 : 502, headers: jsonHeaders },
    );
  } catch {
    await refundQuota();
    return new Response(JSON.stringify({ error: "ai_request_failed" }), { status: 502, headers: jsonHeaders });
  }
});
