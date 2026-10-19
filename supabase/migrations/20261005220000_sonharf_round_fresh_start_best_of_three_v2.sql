-- Son Harf rounds:
--  * Each round starts fresh: before a round's first word there is no previous word, so its
--    opener plays a free word instead of chaining from the last round. (round_word_count is 0
--    exactly then.) Words still may not repeat within a match; the opener still alternates.
--  * Best of three: a player who wins two rounds wins the match; no third round is played.
do $migration$
declare
  fn text;
  def text;
  patched text;
  chain_old text;
begin
  foreach fn in array array['submit_word_normal_v3', 'submit_word_expert_v1', 'bot_take_turn_normal_v1', 'bot_take_turn_expert_v1'] loop
    select pg_get_functiondef(p.oid) into def
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = fn;
    if def is null then raise exception 'missing function %', fn; end if;

    chain_old := case fn
      when 'bot_take_turn_normal_v1' then E'where room_id = r.id\n  order by id desc\n  limit 1;'
      when 'bot_take_turn_expert_v1' then E'where room_id=r.id\n  order by id desc\n  limit 1;'
      else E'where room_id=p_room_id\n  order by id desc\n  limit 1;'
    end;
    patched := replace(def, chain_old, chain_old || E'\n  if r.round_word_count = 0 then previous_word := null; end if;');
    if patched = def then raise exception 'chain query not found in %', fn; end if;

    def := patched;
    patched := regexp_replace(def, 'if r\.round_no\s*>=\s*3 then',
      'if r.round_no>=3 or r.host_rounds>=2 or r.guest_rounds>=2 then');
    if patched = def then raise exception 'round end check not found in %', fn; end if;
    execute patched;
  end loop;

  select pg_get_functiondef(p.oid) into def
  from pg_proc p join pg_namespace n on n.oid = p.pronamespace
  where n.nspname = 'private' and p.proname = 'sonharf_consume_missed_slot_v1';
  patched := regexp_replace(def, 'if r\.round_no\s*>=\s*3 then',
    'if r.round_no>=3 or r.host_rounds>=2 or r.guest_rounds>=2 then');
  if patched = def then raise exception 'round end check not found in sonharf_consume_missed_slot_v1'; end if;
  execute patched;
end
$migration$;
