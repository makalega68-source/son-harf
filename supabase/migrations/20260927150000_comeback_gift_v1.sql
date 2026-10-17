-- Retention: presence + comeback gift. The app touches presence on every start; a player who
-- returns after 3+ days away gets a small Son Coin gift from Obi (at most once every 14 days).
create table if not exists public.user_presence (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  last_seen_at timestamptz not null default now(),
  last_gift_at timestamptz
);
alter table public.user_presence enable row level security;
revoke all on public.user_presence from anon, authenticated;

create or replace function public.touch_presence_v1()
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_row public.user_presence%rowtype;
  v_gift integer := 0;
  v_days integer := 0;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_row from public.user_presence where user_id = v_uid for update;
  if not found then
    insert into public.user_presence(user_id) values (v_uid);
    return jsonb_build_object('gift', 0, 'days_away', 0);
  end if;
  v_days := floor(extract(epoch from (now() - v_row.last_seen_at)) / 86400)::int;
  if v_days >= 3 and (v_row.last_gift_at is null or v_row.last_gift_at < now() - interval '14 days') then
    v_gift := case when v_days >= 7 then 100 else 60 end;
    update public.profiles set diamonds = coalesce(diamonds, 0) + v_gift, updated_at = now() where id = v_uid;
    insert into public.diamond_ledger(user_id, delta, reason) values (v_uid, v_gift, 'comeback_gift');
    update public.user_presence set last_seen_at = now(), last_gift_at = now() where user_id = v_uid;
  else
    update public.user_presence set last_seen_at = now() where user_id = v_uid;
  end if;
  return jsonb_build_object('gift', v_gift, 'days_away', v_days);
end;
$$;

revoke all on function public.touch_presence_v1() from public, anon;
grant execute on function public.touch_presence_v1() to authenticated;
