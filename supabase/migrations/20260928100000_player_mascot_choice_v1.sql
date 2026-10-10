-- Opponents see each other's mascot: the player's chosen mascot is kept on the server (only a
-- mascot they really own), and anyone signed in can read which mascot a player brings to a game.
-- Seeing a rival's mascot needs no mascot of your own.
create table if not exists public.user_mascot_choice (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  mascot_key text not null,
  updated_at timestamptz not null default now()
);
alter table public.user_mascot_choice enable row level security;
revoke all on public.user_mascot_choice from anon, authenticated;

-- p_key: the mascot product id (e.g. 'mascot_orb'); null clears the choice.
create or replace function public.set_my_mascot_v1(p_key text)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_key is null then
    delete from public.user_mascot_choice where user_id = v_uid;
    return jsonb_build_object('mascot', null);
  end if;
  if not exists (
    select 1 from public.store_entitlements e
    where e.user_id = v_uid and e.status = 'active' and e.entitlement_key = p_key
      and e.entitlement_key like 'mascot\_%' escape '\'
      and (e.expires_at is null or e.expires_at > now())
  ) then
    raise exception 'mascot_not_owned';
  end if;
  insert into public.user_mascot_choice(user_id, mascot_key) values (v_uid, p_key)
  on conflict (user_id) do update set mascot_key = excluded.mascot_key, updated_at = now();
  return jsonb_build_object('mascot', p_key);
end;
$$;

-- The mascot a player brings, only while they still own it; hidden between blocked players.
create or replace function public.get_player_mascot_v1(p_user_id uuid)
returns text
language sql stable security definer set search_path = '' as $$
  select c.mascot_key
  from public.user_mascot_choice c
  where auth.uid() is not null
    and c.user_id = p_user_id
    and exists (
      select 1 from public.store_entitlements e
      where e.user_id = c.user_id and e.status = 'active' and e.entitlement_key = c.mascot_key
        and (e.expires_at is null or e.expires_at > now())
    )
    and not exists (
      select 1 from public.user_blocks b
      where (b.blocker_id = auth.uid() and b.blocked_id = p_user_id)
         or (b.blocker_id = p_user_id and b.blocked_id = auth.uid())
    );
$$;

revoke all on function public.set_my_mascot_v1(text) from public, anon;
revoke all on function public.get_player_mascot_v1(uuid) from public, anon;
grant execute on function public.set_my_mascot_v1(text) to authenticated;
grant execute on function public.get_player_mascot_v1(uuid) to authenticated;
