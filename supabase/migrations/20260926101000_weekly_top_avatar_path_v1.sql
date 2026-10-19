-- Weekly top 3 returns the uploaded photo path (avatar_path), falling back to the legacy
-- avatar_url; hidden photos stay hidden.
create or replace function public.weekly_top(p_limit integer default 20)
 returns table(user_id text, username text, rp integer, avatar_url text)
 language plpgsql stable security definer set search_path to 'pg_catalog','public','pg_temp' as $function$
declare
  v_limit integer := least(greatest(coalesce(p_limit, 20), 1), 100);
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  return query
  select wr.user_id::text,
         coalesce(nullif(trim(p.display_name), ''), 'Oyuncu')::text,
         least(wr.rp, 2147483647)::integer,
         case when coalesce(p.avatar_visibility, 'public') = 'hidden' then null
              else coalesce(nullif(trim(p.avatar_path), ''), nullif(trim(p.avatar_url), '')) end::text
  from public.weekly_rp wr
  join public.profiles p on p.id = wr.user_id
  order by wr.rp desc, wr.user_id
  limit v_limit;
end
$function$;
