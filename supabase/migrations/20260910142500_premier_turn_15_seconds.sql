-- Premier duel turns are 15 seconds. Trivia/quiz answer windows remain unchanged.
create or replace function public.sonharf_turn_deadline(p_mode text)
returns timestamptz
language sql
volatile
set search_path = 'public'
as $$
  select clock_timestamp() + interval '15 seconds'
$$;

do $patch$
declare
  sig regprocedure;
  f text;
  x text;
  targets text[] := array[
    'public.claim_turn_timeout_v2(uuid,uuid,timestamptz)',
    'public.join_room_by_code(text)',
    'public.request_rematch_legacy_v1(uuid)',
    'public.respond_game_invite(uuid,boolean)',
    'public.submit_word_normal_v3(uuid,text)'
  ];
begin
  foreach x in array targets loop
    sig := x::regprocedure;
    f := pg_get_functiondef(sig);
    if position($q$interval '20 seconds'$q$ in f) = 0 then
      raise exception 'premier_20_second_hook_missing:%', x;
    end if;
    f := replace(f, $q$interval '20 seconds'$q$, $q$interval '15 seconds'$q$);
    execute f;
  end loop;
end $patch$;

select pg_notify('pgrst', 'reload schema');
