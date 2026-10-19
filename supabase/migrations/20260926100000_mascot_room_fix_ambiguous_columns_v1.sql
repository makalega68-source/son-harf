-- The mascot room functions failed with "column reference mascot_id is ambiguous" (the
-- RETURNS TABLE columns shadow table columns). Prefer the table column inside them.
do $$
declare r record; d text;
begin
  for r in select p.oid, p.proname from pg_proc p join pg_namespace n on n.oid=p.pronamespace
           where n.nspname='public' and p.proname in ('care_mascot_v2','feed_mascot_v1','get_mascot_progress_v1','get_mascot_room_state_v2','set_mascot_room_item_v2','rename_mascot_v1','buy_mascot_fruit_v1')
             and p.prolang=(select oid from pg_language where lanname='plpgsql')
  loop
    d := pg_get_functiondef(r.oid);
    if position('#variable_conflict' in d) = 0 then
      d := regexp_replace(d, 'AS \$function\$', E'AS $function$\n#variable_conflict use_column');
      execute d;
    end if;
  end loop;
end $$;
