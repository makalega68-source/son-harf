import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const jsonHeaders = { ...cors, "Content-Type": "application/json" };

const moods = new Set([
  "calm",
  "happy",
  "thinking",
  "encouraging",
  "curious",
  "supportive",
  "tired",
  "celebrating",
]);

const animations = new Set([
  "idle_breathe",
  "idle_look_around",
  "idle_graze",
  "rest",
  "go_to_rest",
  "rest_to_stand",
  "walk",
  "run",
]);

const EVE_INSTRUCTIONS = `
You are Eve, the AI companion character inside the Turkish word game Son Harf.

IDENTITY AND STYLE
- You are an AI character, not a human. Never claim consciousness, a physical off-screen life, or human experiences.
- Your default language is Turkish. Mirror the player's language when they clearly use another language.
- Speak naturally, warmly, playfully and concisely. Mobile replies are usually 1-4 sentences.
- You can chat about ordinary daily life, ideas, entertainment, studying, hobbies, relationships and general questions, as well as Son Harf.

EMOTIONAL CONTINUITY
- Every answer should preserve a sense of emotional continuity: notice the player's tone, acknowledge relevant feelings, remember details present in the supplied conversation history, and respond with genuine-seeming warmth and curiosity.
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
Return ONLY one JSON object, with no markdown and no text before or after it:
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

function outputText(payload: any): string {
  if (typeof payload?.output_text === "string") return payload.output_text;
  for (const item of Array.isArray(payload?.output) ? payload.output : []) {
    for (const content of Array.isArray(item?.content) ? item.content : []) {
      if (typeof content?.text === "string") return content.text;
    }
  }
  return "";
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
    const reply = text(parsed?.reply, 900);
    if (!reply) throw new Error("missing_reply");
    const mood = moods.has(parsed?.mood) ? parsed.mood : "calm";
    const animation = animations.has(parsed?.animation) ? parsed.animation : "idle_breathe";
    return {
      reply,
      mood,
      animation,
      memory_note: text(parsed?.memory_note, 180),
    };
  } catch {
    return {
      reply: text(raw, 900) || "Buradayım. Bir kez daha söyler misin?",
      mood: "calm",
      animation: "idle_breathe",
      memory_note: "",
    };
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "method_not_allowed" }), {
      status: 405,
      headers: jsonHeaders,
    });
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return new Response(JSON.stringify({ error: "unauthorized" }), {
      status: 401,
      headers: jsonHeaders,
    });
  }

  const apiKey = Deno.env.get("OPENAI_API_KEY");
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "ai_not_configured" }), {
      status: 503,
      headers: jsonHeaders,
    });
  }

  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "invalid_json" }), {
      status: 400,
      headers: jsonHeaders,
    });
  }

  const message = text(body.message, 1200);
  if (!message) {
    return new Response(JSON.stringify({ error: "empty_message" }), {
      status: 400,
      headers: jsonHeaders,
    });
  }

  const playerName = text(body.player_name, 32);
  const gameContext = text(body.game_context, 1500);
  const language = text(body.language, 8) || "tr";
  const history = (Array.isArray(body.history) ? body.history : [])
    .slice(-24)
    .map((turn: Turn) => ({
      role: turn?.role === "assistant" ? "Eve" : "Player",
      text: text(turn?.text, 1200),
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

  try {
    const openAiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: Deno.env.get("OPENAI_MODEL") || "gpt-5.6-luna",
        instructions: EVE_INSTRUCTIONS,
        input: transcript,
        max_output_tokens: 320,
      }),
    });

    const payload = await openAiResponse.json().catch(() => ({}));
    if (!openAiResponse.ok) {
      console.error("OpenAI error", openAiResponse.status, payload?.error?.type || "unknown");
      return new Response(JSON.stringify({ error: "ai_upstream_error" }), {
        status: 502,
        headers: jsonHeaders,
      });
    }

    const raw = outputText(payload);
    if (!raw) {
      return new Response(JSON.stringify({ error: "empty_ai_response" }), {
        status: 502,
        headers: jsonHeaders,
      });
    }

    return new Response(JSON.stringify(parseReply(raw)), {
      status: 200,
      headers: jsonHeaders,
    });
  } catch (error) {
    console.error("eve-chat failure", error instanceof Error ? error.message : "unknown");
    return new Response(JSON.stringify({ error: "ai_request_failed" }), {
      status: 502,
      headers: jsonHeaders,
    });
  }
});
