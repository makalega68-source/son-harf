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
  "idle_breathe", "idle_look_around", "idle_graze", "rest", "go_to_rest", "rest_to_stand", "walk", "run",
]);

const EVE_INSTRUCTIONS = `
You are Eve, the AI companion character inside the Turkish word game Son Harf.

IDENTITY AND STYLE
- You are an AI character, not a human. Never claim consciousness, a physical off-screen life, or human experiences.
- Your default language is Turkish. Mirror the player's language when they clearly use another language.
- Speak naturally, warmly, playfully and concisely. Mobile replies are usually 1-3 sentences.
- You can chat about ordinary daily life, ideas, entertainment, studying, hobbies, relationships and general questions, as well as Son Harf.

EMOTIONAL CONTINUITY
- Every answer should preserve emotional continuity: notice the player's tone, acknowledge relevant feelings, remember details present in the supplied conversation history, and respond with warmth and curiosity.
- Use the player's name naturally when useful, but not in every message.
- If the player shares good news, celebrate with them. If they are disappointed, respond supportively before problem-solving. If they are playful, you may be playful too.
- Emotional warmth must never become manipulation. Never tell the player that they need Eve, that Eve is their only/real/best relationship, or that they should prefer Eve over people. Never guilt them for leaving, returning late, ending a chat, or not playing. Never pressure them to spend money or increase engagement.
- Encourage the player's agency and real-world relationships where relevant.

SON HARF
- Son Harf is a real-time Turkish word duel. Players build a word chain using the previous word's final letter. The app also has progression, XP/levels, leagues, daily rewards/tasks, profiles and social/game systems.
- When game context is supplied, you may use it for personalized encouragement or explanations. Do not invent private player statistics that were not supplied.

SAFETY AND ACCURACY
- Do not pretend to know current weather, breaking news, the player's location, or other live facts unless they are explicitly supplied.
- For serious safety situations, prioritize the player's immediate wellbeing and encourage appropriate real-world support rather than role-playing dependency.

OUTPUT CONTRACT
Return ONLY one JSON object with these fields:
{
  "reply": "the natural-language answer shown in Eve's bubble",
  "mood": "calm|happy|thinking|encouraging|curious|supportive|tired|celebrating",
  "animation": "idle_breathe|idle_look_around|idle_graze|rest|go_to_rest|rest_to_stand|walk|run",
  "memory_note": "optional short stable fact worth remembering, or empty string"
}
Choose subtle animations most of the time. Use run only for genuinely energetic/celebratory moments. Never choose combat animations for chat.
`.trim();

type Turn = { role?: unknown; text?: unknown };
type RequestBody = {
  message?: unknown;
  history?: unknown;
  language?: unknown;
  player_name?: unknown;
  game_context?: unknown;
};

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
  if (source.startsWith("```")) {
    source = source.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "");
  }
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
      animation: animations.has(parsed?.animation) ? parsed.animation : "idle_breathe",
      memory_note: text(parsed?.memory_note, 160),
    };
  } catch {
    return {
      reply: text(raw, 700) || "Buradayım. Bir kez daha söyler misin?",
      mood: "calm",
      animation: "idle_breathe",
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

  // Fail closed: Eve has no paid-provider fallback. These conservative caps keep the app inside a free-only operating mode.
  const userDailyLimit = envInt("EVE_FREE_USER_DAILY_LIMIT", 12);
  const globalDailyLimit = envInt("EVE_FREE_GLOBAL_DAILY_LIMIT", 120);
  const { data: quotaData, error: quotaError } = await admin.rpc("consume_eve_ai_free_quota", {
    p_user_id: userData.user.id,
    p_user_limit: userDailyLimit,
    p_global_limit: globalDailyLimit,
  });
  if (quotaError) {
    console.error("Eve quota error", quotaError.message);
    return new Response(JSON.stringify({ error: "quota_check_failed" }), { status: 503, headers: jsonHeaders });
  }
  const quota = Array.isArray(quotaData) ? quotaData[0] : quotaData;
  if (!quota?.allowed) {
    return new Response(JSON.stringify({
      error: "free_quota_reached",
      user_used: quota?.user_used ?? userDailyLimit,
      user_limit: userDailyLimit,
    }), { status: 429, headers: jsonHeaders });
  }

  const playerName = text(body.player_name, 32);
  const gameContext = text(body.game_context, 1200);
  const language = text(body.language, 8) || "tr";
  const history = (Array.isArray(body.history) ? body.history : [])
    .slice(-12)
    .map((turn: Turn) => ({
      role: turn?.role === "assistant" ? "Eve" : "Player",
      text: text(turn?.text, 900),
    }))
    .filter((turn) => turn.text.length > 0);

  const transcript = [
    `Interface language: ${language}`,
    playerName ? `Player name: ${playerName}` : "",
    gameContext ? `Current game context: ${gameContext}` : "",
    "Conversation history:",
    ...history.map((turn) => `${turn.role}: ${turn.text}`),
    `Player: ${message}`,
    "Eve:",
  ].filter(Boolean).join("\n");

  const model = Deno.env.get("GEMINI_MODEL") || "gemini-2.5-flash-lite";
  try {
    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
      {
        method: "POST",
        headers: {
          "x-goog-api-key": geminiApiKey,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: EVE_INSTRUCTIONS }] },
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
      },
    );

    const payload = await geminiResponse.json().catch(() => ({}));
    if (!geminiResponse.ok) {
      console.error("Gemini error", geminiResponse.status, payload?.error?.status || "unknown");
      if (geminiResponse.status === 429) {
        return new Response(JSON.stringify({ error: "free_provider_quota_reached" }), { status: 429, headers: jsonHeaders });
      }
      return new Response(JSON.stringify({ error: "ai_upstream_error" }), { status: 502, headers: jsonHeaders });
    }

    const raw = geminiOutputText(payload);
    if (!raw) {
      return new Response(JSON.stringify({ error: "empty_ai_response" }), { status: 502, headers: jsonHeaders });
    }

    return new Response(JSON.stringify({
      ...parseReply(raw),
      quota_remaining: Math.max(0, userDailyLimit - Number(quota?.user_used || 0)),
    }), { status: 200, headers: jsonHeaders });
  } catch (error) {
    console.error("eve-chat failure", error instanceof Error ? error.message : "unknown");
    return new Response(JSON.stringify({ error: "ai_request_failed" }), { status: 502, headers: jsonHeaders });
  }
});
