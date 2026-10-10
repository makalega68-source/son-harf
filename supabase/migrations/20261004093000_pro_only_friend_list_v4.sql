-- Friend list is a PRO-member feature; only the list, nothing else.
-- * Accepted friendships are readable by participants who have PRO (is_vip or pro_lifetime).
-- * Pending rows stay readable by both participants, so anyone can still accept requests.
-- * Quick Duel (Series) owners keep the narrow v2 exception: accepted friends who can also
--   play Series, which is exactly the set the Series invite RPC accepts.
-- Sending friend requests, game invitations and rematches is not gated here: those RPCs are
-- SECURITY DEFINER and the can_use_pro_friend_list_v1 helper their triggers use is unchanged.

drop policy if exists "friendships participant read v3" on public.friendships;
drop policy if exists "friendships pro and series scoped read v2" on public.friendships;
drop policy if exists "friendships pro list read v4" on public.friendships;

create policy "friendships pro list read v4"
on public.friendships
for select
to authenticated
using (
  (user_id = (select auth.uid()) or friend_id = (select auth.uid()))
  and (
    status = 'pending'
    or public.has_pro_access_v1((select auth.uid()))
    or (
      status = 'accepted'
      and public.has_series_game_access_v1((select auth.uid()))
      and public.has_series_game_access_v1(case when user_id = (select auth.uid()) then friend_id else user_id end)
    )
  )
);

-- The entitlement response tells the client the truth again.
do $$ declare d text; begin
  select pg_get_functiondef('public.get_vip_entitlements_v7()'::regprocedure) into d;
  d := replace(d, '''saved_friend_list'', true', '''saved_friend_list'', pro');
  execute d;
end $$;
