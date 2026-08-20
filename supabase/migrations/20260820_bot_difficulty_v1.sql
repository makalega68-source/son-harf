alter table public.profiles add column if not exists bot_difficulty text not null default 'normal';
do $$ begin
  if not exists (select 1 from pg_constraint where conname='profiles_bot_difficulty_check') then
    alter table public.profiles add constraint profiles_bot_difficulty_check check (bot_difficulty in ('easy','normal','hard'));
  end if;
end $$;

create or replace function public.set_bot_difficulty_v1(p_difficulty text)
returns text language plpgsql security definer set search_path=public as $$
declare v text;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  v := case when lower(p_difficulty) in ('easy','hard') then lower(p_difficulty) else 'normal' end;
  update public.profiles set bot_difficulty=v,updated_at=now() where id=auth.uid();
  return v;
end $$;
grant execute on function public.set_bot_difficulty_v1(text) to authenticated;

create or replace function public.bot_take_turn_normal_v1(p_room_id uuid)
 returns game_rooms language plpgsql security definer set search_path to 'public', 'pg_temp'
as $function$
declare r public.game_rooms; previous_word text; expected text; chosen public.dictionary_words; streak_value int; add_points int:=3; next_round int; qid bigint; difficulty text:='normal';
begin
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
 if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
 if r.status not in ('playing','sudden_death') or not r.bot_turn then return r; end if;
 select coalesce(bot_difficulty,'normal') into difficulty from public.profiles where id=r.host_id;
 select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
 expected:=case when previous_word is null then null else right(previous_word,1) end;
 select d.* into chosen from public.dictionary_words d
 where d.language=r.language and d.active and public.sonharf_word_allowed(d.language,d.normalized_word)
 and (expected is null or left(d.normalized_word,1)=expected)
 and not exists(select 1 from public.game_words w where w.room_id=r.id and w.normalized_word=d.normalized_word)
 order by
   case when difficulty='easy' then random() else 0 end,
   case when difficulty='hard' then (select count(*) from public.dictionary_words n where n.language=r.language and n.active and public.sonharf_word_allowed(n.language,n.normalized_word) and left(n.normalized_word,1)=right(d.normalized_word,1) and not exists(select 1 from public.game_words uw where uw.room_id=r.id and uw.normalized_word=n.normalized_word)) else 0 end desc,
   case when difficulty='normal' then char_length(d.normalized_word) else 0 end desc,
   random()
 limit 1;
 if chosen.id is null then return public.sonharf_finish_room(r.id,r.host_id,false,'bot_no_word'); end if;
 insert into public.game_words(room_id,player_id,word,normalized_word,is_bot) values(r.id,null,chosen.word,chosen.normalized_word,true);
 if r.status='sudden_death' then return public.sonharf_finish_room(r.id,null,true,'sudden_death_word'); end if;
 streak_value:=r.guest_streak+1; if streak_value%5=0 then add_points:=6; end if;
 update public.game_rooms set guest_score=guest_score+add_points,guest_round_score=guest_round_score+add_points,guest_streak=streak_value,valid_word_count=valid_word_count+1,round_word_count=round_word_count+1,current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',last_event=case when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,last_event_player_id=null where id=r.id returning * into r;
 if r.round_word_count=5 then
   select id into qid from public.trivia_questions where language=r.language and active order by random() limit 1;
   if qid is not null then insert into public.trivia_rounds(room_id,milestone,bonus_points,question_id,reveal_at) values(r.id,(r.round_no-1)*10+5,3,qid,now()+interval '3 seconds') on conflict(room_id,milestone) do nothing; update public.game_rooms set status='quiz',turn_deadline=null,last_event='quiz_started' where id=r.id returning * into r; return r; end if;
 end if;
 if r.round_word_count>=10 then
   update public.profiles set total_rounds=total_rounds+1,rounds_won=rounds_won+case when r.host_round_score>r.guest_round_score then 1 else 0 end where id=r.host_id;
   update public.game_rooms set host_rounds=host_rounds+case when host_round_score>guest_round_score then 1 else 0 end,guest_rounds=guest_rounds+case when guest_round_score>host_round_score then 1 else 0 end where id=r.id returning * into r;
   if r.round_no>=3 then
     if r.host_rounds>r.guest_rounds then return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
     elsif r.guest_rounds>r.host_rounds then return public.sonharf_finish_room(r.id,null,true,'match_finished');
     else update public.game_rooms set status='sudden_death',round_word_count=0,host_round_score=0,guest_round_score=0,current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',last_event='sudden_death_started' where id=r.id returning * into r; return r; end if;
   else
     next_round:=r.round_no+1;
     update public.game_rooms set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,host_streak=0,guest_streak=0,current_player_id=case when next_round%2=1 then host_id else null end,bot_turn=(next_round%2=0),turn_deadline=case when next_round%2=1 then now()+interval '45 seconds' else null end,last_event='round_started' where id=r.id returning * into r;
   end if;
 end if;
 return r;
end $function$;
