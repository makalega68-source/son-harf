-- Ownership restrictions also exclude anonymous sessions from social tools.
create index social_activity_actor_idx on public.social_activity(actor_id);
create or replace function public.can_use_pro_friend_list_v1(p_user_id uuid)
returns boolean language sql stable security invoker set search_path='' as $$
 select p_user_id is not null and p_user_id=(select auth.uid()) and not coalesce(((select auth.jwt())->>'is_anonymous')::boolean,false);
$$;
alter policy "own invite code" on public.player_invite_codes using(user_id=(select auth.uid()) and not coalesce(((select auth.jwt())->>'is_anonymous')::boolean,false));
alter policy "activity owner read" on public.social_activity using(user_id=(select auth.uid()) and not coalesce(((select auth.jwt())->>'is_anonymous')::boolean,false));
alter policy "activity owner mark" on public.social_activity using(user_id=(select auth.uid()) and not coalesce(((select auth.jwt())->>'is_anonymous')::boolean,false)) with check(user_id=(select auth.uid()) and not coalesce(((select auth.jwt())->>'is_anonymous')::boolean,false));
do $$
declare definition text; name text;
begin
 foreach name in array array['public.get_my_invite_code_v1()','public.use_player_invite_code_v1(text)','public.request_word_siege_rematch_v1(uuid)'] loop
   select pg_get_functiondef(name::regprocedure) into definition;
   definition:=replace(definition,'if u is null then','if u is null or coalesce((auth.jwt()->>''is_anonymous'')::boolean,false) then');
   definition:=replace(definition,'if me is null then','if me is null or coalesce((auth.jwt()->>''is_anonymous'')::boolean,false) then');
   execute definition;
 end loop;
 select pg_get_functiondef('private.social_activity_trigger_v1()'::regprocedure) into definition;
 definition:=replace(definition,'n->>''status'' in (''playing'',''sudden_death'')','n->>''status'' in (''playing'',''quiz'',''final'',''sudden_death'')');
 definition:=replace(definition,'new.receiver_id,new.sender_id,''challenge'',''activity''','new.receiver_id,new.sender_id,case when n->>''rematch_of'' is not null then ''rematch'' else ''challenge'' end,''activity''');
 execute definition;
end $$;
