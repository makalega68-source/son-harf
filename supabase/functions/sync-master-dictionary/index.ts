import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const TDK_URL = "https://sozluk.gov.tr/autocomplete.json";
const EN_URL = "https://raw.githubusercontent.com/en-wl/wordlist-diff/71d7dd07676edb60ade43552e10b41314b7e9287/en_US.txt";
const EN_VERSION = "SCOWL/ESDB 2026.02.25";
const EN_COMMIT = "71d7dd07676edb60ade43552e10b41314b7e9287";
const BATCH_SIZE = 2500;
const MIN_SOURCE_WORDS = 20_000;

const headers = { "Content-Type": "application/json" };

type StageWord = {
  job_id: string;
  language: "tr" | "en";
  word: string;
  normalized_word: string;
  source_text: string;
};

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), { status, headers });
}

async function sha256Hex(bytes: Uint8Array): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function normalizeTurkish(raw: string): { word: string; source: string } | null {
  const source = raw.normalize("NFC").trim();
  if (!source || source !== source.toLocaleLowerCase("tr-TR")) return null;

  const word = source
    .replaceAll("â", "a")
    .replaceAll("î", "i")
    .replaceAll("û", "u")
    .normalize("NFC");

  if (word.length < 2 || word.length > 30) return null;
  if (!/^[abcçdefgğhıijklmnoöprsştuüvyz]+$/u.test(word)) return null;
  return { word, source };
}

function normalizeEnglish(raw: string): { word: string; source: string } | null {
  const source = raw.normalize("NFC").trim();
  if (!source || source !== source.toLowerCase()) return null;
  if (source.length < 2 || source.length > 30) return null;
  if (!/^[a-z]+$/.test(source)) return null;
  return { word: source, source };
}

function uniqueWords(
  entries: Iterable<{ word: string; source: string }>,
  language: "tr" | "en",
  jobId: string,
): StageWord[] {
  const seen = new Map<string, string>();
  for (const entry of entries) {
    if (!seen.has(entry.word)) seen.set(entry.word, entry.source);
  }
  return Array.from(seen, ([word, source]) => ({
    job_id: jobId,
    language,
    word,
    normalized_word: word,
    source_text: source,
  })).sort((a, b) => a.word.localeCompare(b.word, language === "tr" ? "tr" : "en"));
}

async function fetchBytes(url: string): Promise<Uint8Array> {
  const response = await fetch(url, {
    redirect: "follow",
    headers: { "User-Agent": "Kelime-Tahti-Dictionary-Sync/1.0" },
  });
  if (!response.ok) throw new Error(`source_fetch_failed:${response.status}:${url}`);
  return new Uint8Array(await response.arrayBuffer());
}

async function stageBatches(admin: ReturnType<typeof createClient>, rows: StageWord[]) {
  for (let i = 0; i < rows.length; i += BATCH_SIZE) {
    const batch = rows.slice(i, i + BATCH_SIZE);
    const { error } = await admin
      .from("dictionary_stage_words")
      .upsert(batch, {
        onConflict: "job_id,language,normalized_word",
        ignoreDuplicates: true,
      });
    if (error) throw new Error(`stage_upsert_failed:${error.message}`);
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRole) return json(500, { error: "server_not_configured" });

  const admin = createClient(supabaseUrl, serviceRole, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  let jobId = "";
  try {
    const body = await req.json() as { job_id?: string; token?: string };
    jobId = body.job_id?.trim() ?? "";
    const token = body.token?.trim() ?? "";
    if (!jobId || token.length < 32) return json(401, { error: "invalid_sync_token" });

    const tokenHash = await sha256Hex(new TextEncoder().encode(token));
    const { data: job, error: jobError } = await admin
      .from("dictionary_sync_jobs")
      .select("id,token_hash,token_used_at,status")
      .eq("id", jobId)
      .maybeSingle();

    if (jobError || !job || job.token_hash !== tokenHash || job.token_used_at || job.status !== "pending") {
      return json(401, { error: "invalid_or_used_sync_job" });
    }

    const { error: lockError } = await admin
      .from("dictionary_sync_jobs")
      .update({ token_used_at: new Date().toISOString(), status: "fetching", started_at: new Date().toISOString(), error_message: null })
      .eq("id", jobId)
      .eq("status", "pending")
      .is("token_used_at", null);
    if (lockError) throw new Error(`job_lock_failed:${lockError.message}`);

    const [tdkBytes, enBytes] = await Promise.all([fetchBytes(TDK_URL), fetchBytes(EN_URL)]);
    const [tdkSha, enSha] = await Promise.all([sha256Hex(tdkBytes), sha256Hex(enBytes)]);

    const tdkJson = JSON.parse(new TextDecoder("utf-8").decode(tdkBytes)) as Array<{ madde?: unknown }>;
    if (!Array.isArray(tdkJson)) throw new Error("tdk_payload_not_array");
    const trEntries: Array<{ word: string; source: string }> = [];
    for (const item of tdkJson) {
      if (typeof item?.madde !== "string") continue;
      const normalized = normalizeTurkish(item.madde);
      if (normalized) trEntries.push(normalized);
    }

    const enText = new TextDecoder("utf-8").decode(enBytes);
    const enEntries: Array<{ word: string; source: string }> = [];
    for (const line of enText.split(/\r?\n/)) {
      const normalized = normalizeEnglish(line);
      if (normalized) enEntries.push(normalized);
    }

    const trRows = uniqueWords(trEntries, "tr", jobId);
    const enRows = uniqueWords(enEntries, "en", jobId);
    if (trRows.length < MIN_SOURCE_WORDS) throw new Error(`turkish_source_too_small:${trRows.length}`);
    if (enRows.length < MIN_SOURCE_WORDS) throw new Error(`english_source_too_small:${enRows.length}`);

    await admin.from("dictionary_stage_words").delete().eq("job_id", jobId);
    await stageBatches(admin, trRows);
    await stageBatches(admin, enRows);

    const now = new Date().toISOString();
    const { error: stagedError } = await admin
      .from("dictionary_sync_jobs")
      .update({
        status: "staged",
        tr_source_url: TDK_URL,
        tr_source_version: `TDK GTS live ${now.slice(0, 10)}`,
        tr_source_sha256: tdkSha,
        tr_count: trRows.length,
        en_source_url: EN_URL,
        en_source_version: EN_VERSION,
        en_source_commit: EN_COMMIT,
        en_source_sha256: enSha,
        en_count: enRows.length,
      })
      .eq("id", jobId);
    if (stagedError) throw new Error(`job_stage_update_failed:${stagedError.message}`);

    const { data: activated, error: activateError } = await admin.rpc("activate_dictionary_sync_v1", { p_job_id: jobId });
    if (activateError) throw new Error(`activation_failed:${activateError.message}`);

    return json(200, {
      ok: true,
      job_id: jobId,
      turkish: { count: trRows.length, sha256: tdkSha, source: TDK_URL },
      english: { count: enRows.length, sha256: enSha, source: EN_URL, version: EN_VERSION, commit: EN_COMMIT },
      activation: activated,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (jobId) {
      await admin
        .from("dictionary_sync_jobs")
        .update({ status: "error", error_message: message.slice(0, 1000), completed_at: new Date().toISOString() })
        .eq("id", jobId);
    }
    return json(500, { error: "dictionary_sync_failed", detail: message });
  }
});
