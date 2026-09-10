-- Bot matches pause while the app is away. Resume without charging an offline timeout.
create or replace function public.resume_premier_bot_match_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  r public.game_rooms;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if not r.is_bot or auth.uid() <> r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing', 'final', 'sudden_death') then return r; end if;

  if r.bot_turn then
    return public.bot_take_turn(p_room_id);
  end if;

  update public.game_rooms
  set current_player_id = r.host_id,
      bot_turn = false,
      turn_deadline = public.sonharf_turn_deadline(r.game_mode)
  where id = r.id
  returning * into r;
  return r;
end;
$$;

revoke all on function public.resume_premier_bot_match_v1(uuid) from public, anon;
grant execute on function public.resume_premier_bot_match_v1(uuid) to authenticated, service_role;

-- Prefix filtering plus a bounded candidate pool prevents a full 420k-word random sort.
create index if not exists dictionary_words_bot_prefix_idx
on public.dictionary_words (language, (left(normalized_word, 1)), normalized_word)
where active and game_allowed and not is_abbreviation and not is_proper_noun;

create or replace function public.bot_take_turn_normal_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  r public.game_rooms; previous_word text; expected text; chosen public.dictionary_words;
  streak_value int; add_points int := 3; next_round int; qid bigint;
  difficulty text := 'normal'; candidate_offset int;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
  if auth.uid() <> r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') or not r.bot_turn then return r; end if;

  select coalesce(bot_difficulty,'normal') into difficulty from public.profiles where id=r.host_id;
  select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
  expected := case when previous_word is null then null else right(previous_word,1) end;
  candidate_offset := floor(random() * 192)::int;

  with pool as materialized (
    select d.*
    from public.dictionary_words d
    where d.language = r.language
      and d.active and d.game_allowed and not d.is_abbreviation and not d.is_proper_noun
      and (expected is null or left(d.normalized_word,1) = expected)
      and public.sonharf_bot_word_allowed(d.language,d.normalized_word)
      and not exists (
        select 1 from public.game_words w
        where w.room_id=r.id and w.normalized_word=d.normalized_word
      )
    order by d.normalized_word
    offset candidate_offset limit 128
  )
  select p.* into chosen
  from pool p
  order by
    case when difficulty='normal' then abs(char_length(p.normalized_word)-6) else 0 end,
    random()
  limit 1;

  if chosen.id is null then
    select d.* into chosen
    from public.dictionary_words d
    where d.language=r.language and d.active and d.game_allowed
      and not d.is_abbreviation and not d.is_proper_noun
      and (expected is null or left(d.normalized_word,1)=expected)
      and public.sonharf_bot_word_allowed(d.language,d.normalized_word)
      and not exists(select 1 from public.game_words w where w.room_id=r.id and w.normalized_word=d.normalized_word)
    order by d.normalized_word limit 1;
  end if;

  if chosen.id is null then return public.sonharf_finish_room(r.id,r.host_id,false,'bot_no_word'); end if;
  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot)
  values(r.id,null,chosen.word,chosen.normalized_word,true);
  if r.status='sudden_death' then return public.sonharf_finish_room(r.id,null,true,'sudden_death_word'); end if;

  streak_value := r.guest_streak+1;
  if streak_value%5=0 then add_points:=6; end if;
  update public.game_rooms set guest_score=guest_score+add_points,guest_round_score=guest_round_score+add_points,
    guest_streak=streak_value,valid_word_count=valid_word_count+1,round_word_count=round_word_count+1,
    current_player_id=host_id,bot_turn=false,turn_deadline=public.sonharf_turn_deadline(game_mode),
    last_event=case when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,last_event_player_id=null
  where id=r.id returning * into r;

  if r.round_word_count>=10 then
    update public.profiles set total_rounds=total_rounds+1,
      rounds_won=rounds_won+case when r.host_round_score>r.guest_round_score then 1 else 0 end where id=r.host_id;
    update public.game_rooms set
      host_rounds=host_rounds+case when host_round_score>guest_round_score then 1 else 0 end,
      guest_rounds=guest_rounds+case when guest_round_score>host_round_score then 1 else 0 end
    where id=r.id returning * into r;
    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
      elsif r.guest_rounds>r.host_rounds then return public.sonharf_finish_room(r.id,null,true,'match_finished');
      else update public.game_rooms set status='sudden_death',round_word_count=0,host_round_score=0,guest_round_score=0,
        current_player_id=host_id,bot_turn=false,turn_deadline=public.sonharf_turn_deadline(game_mode),last_event='sudden_death_started'
        where id=r.id returning * into r; return r; end if;
    else
      next_round:=r.round_no+1;
      update public.game_rooms set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,
        host_streak=0,guest_streak=0,current_player_id=case when next_round%2=1 then host_id else null end,
        bot_turn=(next_round%2=0),turn_deadline=case when next_round%2=1 then public.sonharf_turn_deadline(game_mode) else null end,
        last_event='round_started' where id=r.id returning * into r;
    end if;
  end if;
  return r;
end;
$$;

revoke all on function public.bot_take_turn_normal_v1(uuid) from public, anon;
grant execute on function public.bot_take_turn_normal_v1(uuid) to authenticated, service_role;

-- Refund only clearly-corrupted empty bot matches that accumulated timeout penalties.
update public.game_rooms r
set host_score = 0,
    host_round_score = 0,
    current_player_id = r.host_id,
    bot_turn = false,
    turn_deadline = public.sonharf_turn_deadline(r.game_mode),
    last_event = 'bot_match_repaired',
    last_event_player_id = null
where r.is_bot and r.status in ('playing','final') and r.host_score < 0
  and not exists (select 1 from public.game_words w where w.room_id = r.id);

select pg_notify('pgrst', 'reload schema');
