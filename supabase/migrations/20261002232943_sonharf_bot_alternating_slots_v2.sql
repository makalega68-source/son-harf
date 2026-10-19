-- Extend the existing ten-turn normal duel contract to AI rooms as well.
-- Preserve dictionary, difficulty, expert/final rules, reconnect grace and RPC privileges.
begin;
do $patch$
declare f text; signature text;
begin
 signature := 'private.sonharf_consume_missed_slot_v1(uuid,uuid)';
 f := pg_get_functiondef(signature::regprocedure);
 if position($needle$if r.is_bot or r.game_mode='expert' or r.status<>'playing' then return r; end if;$needle$ in f)=0 then
   raise exception 'missed_slot_helper_hook_missing';
 end if;
 f := replace(f,$needle$if r.is_bot or r.game_mode='expert' or r.status<>'playing' then return r; end if;$needle$,
                 $needle$if r.game_mode='expert' or r.status<>'playing' then return r; end if;$needle$);
 execute f;
 foreach signature in array array['public.switch_turn_after_failure(uuid,text)', 'public.claim_turn_timeout(uuid)'] loop
   f := pg_get_functiondef(signature::regprocedure);
   if position($needle$if not r.is_bot and r.game_mode<>'expert' and r.status='playing' then$needle$ in f)=0 then
     raise exception 'missed_slot_rpc_hook_missing:%',signature;
   end if;
   f := replace(f,$needle$if not r.is_bot and r.game_mode<>'expert' and r.status='playing' then$needle$,
                   $needle$if r.game_mode<>'expert' and r.status='playing' then$needle$);
   execute f;
 end loop;
end $patch$;
commit;
