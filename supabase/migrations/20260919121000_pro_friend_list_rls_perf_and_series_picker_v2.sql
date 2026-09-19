-- Narrow Series Game exception for the existing friend picker.
-- General accepted-friend access remains PRO-only. A Series-only account may see
-- only accepted friends who also own Series Game/PRO, which is exactly the set
-- the Series invite RPC can legally invite. Pending rows stay visible so
-- incoming friend requests remain usable.
--
-- Also wraps auth.uid() in SELECT so Postgres evaluates it once per statement.

alter table public.friendships enable row level security;

drop policy if exists "friendships pro accepted read v1" on public.friendships;
drop policy if exists "friendships pro and series scoped read v2" on public.friendships;

create policy "friendships pro and series scoped read v2"
on public.friendships
for select
to authenticated
using (
  (
    user_id = (select auth.uid())
    or friend_id = (select auth.uid())
  )
  and (
    status = 'pending'
    or public.can_use_pro_friend_list_v1((select auth.uid()))
    or (
      status = 'accepted'
      and public.has_series_game_access_v1((select auth.uid()))
      and public.has_series_game_access_v1(
        case
          when user_id = (select auth.uid()) then friend_id
          else user_id
        end
      )
    )
  )
);
