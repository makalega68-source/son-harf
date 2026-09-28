-- Coin economy rebalance: saving up for an item should take days of play, not one sitting.
--
--   source                      before                      after
--   Piggy bank (8 matches)      200 / 400 / 600 / 800       20 / 40 / 60 / 80
--   Daily login (PRO)           40 (80)                     15 (30)
--   Weekly Cup (1/2/3/4-10/rest) 1000/600/400/150/50        250/150/100/40/10
--   Word Workshop weekly (1/2/3/4-10) 200/120/80/30         100/60/40/15
--   Comeback gift (3+/7+ days)  60 / 100                    30 / 50
--
-- Match rewards (8 win / 3 loss), missions and the 10-coin rewarded video are unchanged.
-- Each function is rewritten from its live definition; the migration stops if any expected
-- text is missing, so nothing is changed half-way.
do $migration$
declare
  v_def text;
  v_new text;
  r record;
begin
  for r in
    select * from (values
      ('public.get_store_reward_status_v1()'::text,
       'v_bonus := case v_tier when 1 then 200 when 2 then 400 when 3 then 600 when 4 then 800 else 0 end;',
       'v_bonus := case v_tier when 1 then 20 when 2 then 40 when 3 then 60 when 4 then 80 else 0 end;'),
      ('public.open_piggy_bank_v2()',
       'if v_bonus not in (200,400,600,800) then',
       'if v_bonus not in (20,40,60,80) then'),
      ('public.claim_daily_checkin_v1()',
       'v_reward := case when v_vip then 80 else 40 end;',
       'v_reward := case when v_vip then 30 else 15 end;'),
      ('public.get_storefront_v1()',
       '''daily_reward'',case when coalesce(v_vip,false) then 80 else 40 end',
       '''daily_reward'',case when coalesce(v_vip,false) then 30 else 15 end'),
      ('public.touch_presence_v1()',
       'v_gift := case when v_days >= 7 then 100 else 60 end;',
       'v_gift := case when v_days >= 7 then 50 else 30 end;')
    ) t(fn, old_text, new_text)
  loop
    v_def := pg_get_functiondef(r.fn::regprocedure);
    if position(r.old_text in v_def) = 0 then
      raise exception 'economy rebalance: expected text not found in %', r.fn;
    end if;
    v_new := replace(v_def, r.old_text, r.new_text);
    execute v_new;
  end loop;

  -- Weekly Cup and Word Workshop prizes (multi-line case expressions).
  v_def := pg_get_functiondef('public.claim_previous_weekly_tournament_reward_v1()'::regprocedure);
  v_new := regexp_replace(v_def,
    'when v_rank=1 then 1000\s+when v_rank=2 then 600\s+when v_rank=3 then 400\s+when v_rank between 4 and 10 then 150\s+else 50',
    'when v_rank=1 then 250 when v_rank=2 then 150 when v_rank=3 then 100 when v_rank between 4 and 10 then 40 else 10');
  if v_new = v_def then raise exception 'economy rebalance: weekly cup prizes not found'; end if;
  execute v_new;

  select pg_get_functiondef(p.oid) into v_def
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'claim_atelier_weekly_reward_v1'
   limit 1;
  v_new := regexp_replace(v_def,
    'when v_rank = 1 then 200 when v_rank = 2 then 120 when v_rank = 3 then 80\s+when v_rank between 4 and 10 then 30',
    'when v_rank = 1 then 100 when v_rank = 2 then 60 when v_rank = 3 then 40 when v_rank between 4 and 10 then 15');
  if v_new = v_def then raise exception 'economy rebalance: workshop prizes not found'; end if;
  execute v_new;

  -- Piggy banks already filled at the old rate keep their progress; the shown bonus follows the
  -- new table the next time the status is read.
  update public.piggy_banks set bonus_sc = case tier when 1 then 20 when 2 then 40 when 3 then 60 when 4 then 80 else 0 end;
end
$migration$;
