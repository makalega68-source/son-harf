-- Son Harf played-word history is a PRO entitlement.
-- Non-PRO participants may read only the latest word because it is required to know
-- the next starting letter. Older played words remain inaccessible through RLS.

create or replace function public.can_view_sonharf_word_history_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path=''
as $$
  select exists(select 1 from public.profiles p where p.id=p_user_id and coalesce(p.is_vip,false))
      or public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$$;

create or replace function public.latest_game_word_id_v1(p_room_id uuid)
returns bigint
language sql
stable
security definer
set search_path=''
as $$
  select max(w.id) from public.game_words w where w.room_id=p_room_id
$$;

revoke all on function public.can_view_sonharf_word_history_v1(uuid) from public,anon,authenticated;
revoke all on function public.latest_game_word_id_v1(uuid) from public,anon,authenticated;
grant execute on function public.can_view_sonharf_word_history_v1(uuid) to authenticated,service_role;
grant execute on function public.latest_game_word_id_v1(uuid) to authenticated,service_role;

drop policy if exists "words participants read" on public.game_words;
drop policy if exists game_words_participant_history_v1 on public.game_words;
create policy game_words_participant_history_v1
on public.game_words
for select
to authenticated
using (
  exists(
    select 1 from public.game_rooms r
    where r.id=game_words.room_id
      and auth.uid() in (r.host_id,r.guest_id)
  )
  and (
    public.can_view_sonharf_word_history_v1(auth.uid())
    or game_words.id=public.latest_game_word_id_v1(game_words.room_id)
  )
);

select pg_notify('pgrst','reload schema');
