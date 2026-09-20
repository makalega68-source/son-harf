begin;

-- Shared history should survive an account deletion without retaining a hard user FK.
alter table public.club_challenges
  alter column created_by drop not null;

alter table public.club_challenges
  drop constraint if exists club_challenges_created_by_fkey;

alter table public.club_challenges
  add constraint club_challenges_created_by_fkey
  foreign key (created_by) references public.profiles(id) on delete set null;

alter table public.trivia_rounds
  drop constraint if exists trivia_rounds_winner_id_fkey;

alter table public.trivia_rounds
  add constraint trivia_rounds_winner_id_fkey
  foreign key (winner_id) references public.profiles(id) on delete set null;

-- A club owner is the one remaining RESTRICT relation to profiles. Resolve it inside the same
-- transaction that deletes the profile, so auth.admin.deleteUser cannot leave a half-transferred
-- club if a later step fails. Multi-member clubs go to the longest-standing remaining member;
-- an owner-only club is deleted, matching leave_club_v1 semantics.
create or replace function private.prepare_profile_delete_relations_v1()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_club_id uuid;
  v_successor uuid;
begin
  for v_club_id in
    select c.id
      from public.clubs c
     where c.owner_id = old.id
     order by c.created_at, c.id
     for update
  loop
    select cm.user_id
      into v_successor
      from public.club_members cm
     where cm.club_id = v_club_id
       and cm.user_id <> old.id
     order by cm.joined_at, cm.user_id
     limit 1;

    if v_successor is null then
      delete from public.clubs where id = v_club_id;
    else
      update public.club_members
         set role = 'owner'
       where club_id = v_club_id
         and user_id = v_successor;

      update public.clubs
         set owner_id = v_successor,
             updated_at = now()
       where id = v_club_id;
    end if;
  end loop;

  return old;
end
$function$;

revoke all on function private.prepare_profile_delete_relations_v1() from public;

drop trigger if exists profiles_prepare_delete_relations_v1 on public.profiles;
create trigger profiles_prepare_delete_relations_v1
before delete on public.profiles
for each row
execute function private.prepare_profile_delete_relations_v1();

commit;
