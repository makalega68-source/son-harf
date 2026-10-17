-- Son Harf: lengthen the break between rounds from 7 to 10 seconds. The app first shows the
-- "round over" result alone for 4 seconds, then the break screen counts down the rest.
do $migration$
declare
  target record;
  def text;
  patched text;
begin
  for target in
    select n.nspname as ns, p.proname as fn, p.oid
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where (n.nspname, p.proname) in (
      ('public', 'submit_word_normal_v3'), ('public', 'submit_word_expert_v1'),
      ('public', 'bot_take_turn_normal_v1'), ('public', 'bot_take_turn_expert_v1'),
      ('private', 'sonharf_consume_missed_slot_v1'))
  loop
    def := pg_get_functiondef(target.oid);
    if def like '%sonharf_turn_deadline_for_round_v1(next_round)+interval ''10 seconds''%' then continue; end if;
    patched := replace(def, 'sonharf_turn_deadline_for_round_v1(next_round)+interval ''7 seconds''',
      'sonharf_turn_deadline_for_round_v1(next_round)+interval ''10 seconds''');
    if patched = def then raise exception 'round break not found in %.%', target.ns, target.fn; end if;
    execute patched;
  end loop;
end
$migration$;
