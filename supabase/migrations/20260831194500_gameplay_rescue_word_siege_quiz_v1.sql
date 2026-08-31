create or replace function public.validate_word_siege_word_v1(p_word text, p_language text default 'tr')
returns boolean
language sql
stable
security definer
set search_path = public, private, pg_temp
as $$
  select case
    when auth.uid() is null then false
    else private.word_siege_word_allowed_v1(coalesce(p_word,''), coalesce(p_language,'tr'))
  end;
$$;
revoke all on function public.validate_word_siege_word_v1(text,text) from public, anon;
grant execute on function public.validate_word_siege_word_v1(text,text) to authenticated;

create or replace function private.capture_legacy_trivia_resume_v1()
returns trigger
language plpgsql
security definer
set search_path = public, private, pg_temp
as $$
declare r public.game_rooms;
begin
  if new.resume_current_player_id is null and coalesce(new.resume_bot_turn,false)=false then
    select * into r from public.game_rooms where id=new.room_id;
    if r.id is not null and r.status in ('playing','sudden_death') and (r.current_player_id is not null or r.bot_turn) then
      new.resume_status := r.status;
      new.resume_current_player_id := r.current_player_id;
      new.resume_bot_turn := r.bot_turn;
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists trivia_rounds_capture_legacy_resume_v1 on public.trivia_rounds;
create trigger trivia_rounds_capture_legacy_resume_v1
before insert on public.trivia_rounds
for each row execute function private.capture_legacy_trivia_resume_v1();

create or replace function public.finish_bilbakalim_result_v2(p_round_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public, private, pg_temp
as $$
declare
  q public.trivia_rounds;
  r public.game_rooms;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into q from public.trivia_rounds where id=p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id for update;
  if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;

  if q.resume_current_player_id is null
     and coalesce(q.resume_bot_turn,false)=false
     and (r.current_player_id is not null or r.bot_turn) then
    update public.trivia_rounds
    set resume_status=case when r.status='sudden_death' then 'sudden_death' else 'playing' end,
        resume_current_player_id=r.current_player_id,
        resume_bot_turn=r.bot_turn
    where id=q.id;
  end if;

  return public.finish_bilbakalim_result_v1(p_round_id);
end;
$$;
revoke all on function public.finish_bilbakalim_result_v2(uuid) from public, anon;
grant execute on function public.finish_bilbakalim_result_v2(uuid) to authenticated;
