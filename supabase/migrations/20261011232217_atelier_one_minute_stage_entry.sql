-- Each tournament stage now stays open for entry for one minute (instead of ~10 seconds), so a
-- player who opens the game a little late still joins. Applies to events from 2026-10-12 00:00 UTC:
-- stage 1 until +125 s, stage 2 until +310 s, stage 3 until +555 s (60 s entry + play time + 5 s).
CREATE OR REPLACE FUNCTION private.atelier_stage_at(p_event timestamp with time zone, p_now timestamp with time zone)
 RETURNS integer
 LANGUAGE sql
 IMMUTABLE
 SET search_path TO ''
AS $function$
  select case
    when p_event < timestamptz '2026-10-10 11:00:00+00'
      then floor(extract(epoch from (p_now - p_event)) / 600)::integer + 1
    when p_event < timestamptz '2026-10-12 00:00:00+00' then case
      when p_now < p_event + interval '75 seconds' then 1
      when p_now < p_event + interval '210 seconds' then 2
      when p_now < p_event + interval '405 seconds' then 3
      else 4 end
    when p_now < p_event + interval '125 seconds' then 1
    when p_now < p_event + interval '310 seconds' then 2
    when p_now < p_event + interval '555 seconds' then 3
    else 4
  end
$function$;

CREATE OR REPLACE FUNCTION private.atelier_stage_end(p_event timestamp with time zone, p_stage integer)
 RETURNS timestamp with time zone
 LANGUAGE sql
 IMMUTABLE
 SET search_path TO ''
AS $function$
  select p_event + make_interval(secs =>
    case
      when p_event < timestamptz '2026-10-10 11:00:00+00' then least(greatest(p_stage, 1), 3) * 600
      when p_event < timestamptz '2026-10-12 00:00:00+00' then
        case when p_stage <= 1 then 75 when p_stage = 2 then 210 else 405 end
      when p_stage <= 1 then 125
      when p_stage = 2 then 310
      else 555
    end)
$function$;
