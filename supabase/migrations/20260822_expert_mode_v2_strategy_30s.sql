-- Expert Mode v2: authoritative 30-second turns and strategic bot selection.

create or replace function public.sonharf_turn_deadline(p_mode text)
returns timestamptz language sql volatile set search_path=public as $$
  select now() + case when lower(coalesce(p_mode,'normal'))='expert' then interval '30 seconds' else interval '45 seconds' end
$$;
grant execute on function public.sonharf_turn_deadline(text) to authenticated;

create or replace function public.switch_turn_after_failure(p_room_id uuid, p_reason text)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; next_player uuid;
begin
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null then raise exception 'room_not_found'; end if;
 if r.is_bot then if auth.uid()<>r.host_id or r.bot_turn then raise exception 'not_your_turn'; end if;
 else if auth.uid()<>r.current_player_id then raise exception 'not_your_turn'; end if; end if;
 if r.status='sudden_death' then
   return public.sonharf_finish_room(r.id,case when r.is_bot then null else case when auth.uid()=r.host_id then r.guest_id else r.host_id end end,r.is_bot and auth.uid()=r.host_id,p_reason);
 end if;
 if auth.uid()=r.host_id then update public.game_rooms set host_score=host_score-1,host_round_score=host_round_score-1,host_streak=0 where id=r.id;
 else update public.game_rooms set guest_score=guest_score-1,guest_round_score=guest_round_score-1,guest_streak=0 where id=r.id; end if;
 next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
 update public.game_rooms set current_player_id=case when r.is_bot and auth.uid()=r.host_id then null else next_player end,
   bot_turn=(r.is_bot and auth.uid()=r.host_id),
   turn_deadline=case when r.is_bot and auth.uid()=r.host_id then null else public.sonharf_turn_deadline(r.game_mode) end,
   last_event=p_reason,last_event_player_id=auth.uid() where id=r.id returning * into r;
 return r;
end $$;

create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; timed uuid;
begin
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null then raise exception 'room_not_found'; end if;
 if r.status not in ('playing','sudden_death') or r.turn_deadline is null or r.turn_deadline>=now() then return r; end if;
 timed:=r.current_player_id; if timed is null then return r; end if;
 if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
 if r.status='sudden_death' then return public.sonharf_finish_room(r.id,case when r.is_bot then null else case when timed=r.host_id then r.guest_id else r.host_id end end,r.is_bot and timed=r.host_id,'turn_expired'); end if;
 if timed=r.host_id then
   update public.game_rooms set host_score=host_score-1,host_round_score=host_round_score-1,host_streak=0,
     current_player_id=case when r.is_bot then null else r.guest_id end,bot_turn=r.is_bot,
     turn_deadline=case when r.is_bot then null else public.sonharf_turn_deadline(r.game_mode) end,
     last_event='turn_expired',last_event_player_id=timed where id=r.id returning * into r;
 else
   update public.game_rooms set guest_score=guest_score-1,guest_round_score=guest_round_score-1,guest_streak=0,current_player_id=r.host_id,bot_turn=false,
     turn_deadline=public.sonharf_turn_deadline(r.game_mode),last_event='turn_expired',last_event_player_id=timed where id=r.id returning * into r;
 end if;
 return r;
end $$;

create or replace function public.join_random_matchmaking_v2(p_language text, p_mode text default 'normal')
returns public.game_rooms language plpgsql security definer set search_path=public,pg_temp as $$
declare opponent uuid; r public.game_rooms; generated_code text; i int;
begin
 if auth.uid() is null then raise exception 'not_authenticated'; end if;
 p_language:=lower(trim(p_language)); p_mode:=lower(trim(p_mode));
 if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;
 if p_mode not in ('normal','expert') then raise exception 'invalid_game_mode'; end if;
 perform pg_advisory_xact_lock(77110051);
 update public.game_rooms set status='cancelled',turn_deadline=null,last_event='stale_match_cleaned'
 where status in ('playing','quiz','final','sudden_death') and (host_id=auth.uid() or guest_id=auth.uid()) and greatest(coalesce(host_last_seen_at,created_at),coalesce(guest_last_seen_at,created_at))<now()-interval '2 minutes';
 if exists(select 1 from public.game_rooms where status in ('playing','quiz','final','sudden_death') and (host_id=auth.uid() or guest_id=auth.uid())) then raise exception 'player_already_in_game'; end if;
 update public.matchmaking_queue set status='cancelled' where status='waiting' and heartbeat_at<now()-interval '90 seconds';
 select q.user_id into opponent from public.matchmaking_queue q
 where q.status='waiting' and q.language=p_language and q.game_mode=p_mode and q.user_id<>auth.uid() and q.heartbeat_at>=now()-interval '90 seconds'
 and not exists(select 1 from public.user_blocks b where (b.blocker_id=auth.uid() and b.blocked_id=q.user_id) or (b.blocker_id=q.user_id and b.blocked_id=auth.uid()))
 and not exists(select 1 from public.game_rooms ar where ar.status in ('playing','quiz','final','sudden_death') and (ar.host_id=q.user_id or ar.guest_id=q.user_id))
 order by q.queued_at for update skip locked limit 1;
 if opponent is null then
   insert into public.matchmaking_queue(user_id,language,game_mode,status,queued_at,heartbeat_at)
   values(auth.uid(),p_language,p_mode,'waiting',now(),now())
   on conflict(user_id) do update set language=excluded.language,game_mode=excluded.game_mode,status='waiting',room_id=null,
     queued_at=case when public.matchmaking_queue.status='waiting' and public.matchmaking_queue.language=excluded.language and public.matchmaking_queue.game_mode=excluded.game_mode then public.matchmaking_queue.queued_at else now() end,heartbeat_at=now();
   update public.profiles set presence_status='online',last_seen_at=now() where id=auth.uid(); return null;
 end if;
 for i in 1..8 loop
   generated_code:=upper(substr(encode(gen_random_bytes(8),'hex'),1,6));
   begin
     insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,language,room_type,game_mode,host_last_seen_at,guest_last_seen_at)
     values(generated_code,opponent,auth.uid(),'playing',opponent,public.sonharf_turn_deadline(p_mode),p_language,'random',p_mode,now(),now()) returning * into r; exit;
   exception when unique_violation then null; end;
 end loop;
 if r.id is null then raise exception 'room_code_generation_failed'; end if;
 update public.matchmaking_queue set status='matched',room_id=r.id,heartbeat_at=now() where user_id in(opponent,auth.uid());
 insert into public.matchmaking_queue(user_id,language,game_mode,status,room_id,queued_at,heartbeat_at)
 values(auth.uid(),p_language,p_mode,'matched',r.id,now(),now()) on conflict(user_id) do update set game_mode=excluded.game_mode,status='matched',room_id=r.id,heartbeat_at=now();
 update public.profiles set presence_status='in_game',last_seen_at=now() where id in(opponent,auth.uid()); return r;
end $$;

create or replace function public.poll_random_matchmaking_v2()
returns public.game_rooms language plpgsql security definer set search_path=public,pg_temp as $$
declare q public.matchmaking_queue; r public.game_rooms; generated_code text;
begin
 select * into q from public.matchmaking_queue where user_id=auth.uid() for update;
 if q.user_id is null then return null; end if;
 if q.status='matched' and q.room_id is not null then select * into r from public.game_rooms where id=q.room_id; return r; end if;
 if q.status='waiting' then
   update public.matchmaking_queue set heartbeat_at=now() where user_id=auth.uid();
   if q.queued_at<=now()-interval '10 seconds' then
     generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
     insert into public.game_rooms(code,host_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,bot_turn,room_type,game_mode,host_last_seen_at)
     values(generated_code,auth.uid(),'playing',auth.uid(),public.sonharf_turn_deadline(q.game_mode),q.language,true,case when q.language='tr' then 'KelimeBot' else 'WordBot' end,false,'bot',q.game_mode,now()) returning * into r;
     update public.matchmaking_queue set status='matched',room_id=r.id,heartbeat_at=now() where user_id=auth.uid();
     update public.profiles set presence_status='in_game',last_seen_at=now() where id=auth.uid(); return r;
   end if;
 end if;
 return null;
end $$;

create or replace function public.restart_bot_match_v2(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public,pg_temp as $$
declare old public.game_rooms; r public.game_rooms; c text; i int;
begin
 if auth.uid() is null then raise exception 'not_authenticated'; end if;
 select * into old from public.game_rooms where id=p_room_id for update;
 if old.id is null or not old.is_bot or auth.uid()<>old.host_id or old.status<>'finished' then raise exception 'invalid_rematch'; end if;
 if exists(select 1 from public.game_rooms where status in ('playing','quiz','final','sudden_death') and (host_id=auth.uid() or guest_id=auth.uid())) then raise exception 'player_already_in_game'; end if;
 for i in 1..8 loop
  c:=upper(substr(encode(gen_random_bytes(8),'hex'),1,6));
  begin insert into public.game_rooms(code,host_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,room_type,rematch_of,game_mode,host_last_seen_at)
  values(c,auth.uid(),'playing',auth.uid(),public.sonharf_turn_deadline(old.game_mode),old.language,true,old.bot_name,'bot',old.id,old.game_mode,now()) returning * into r; exit; exception when unique_violation then null; end;
 end loop;
 if r.id is null then raise exception 'room_code_generation_failed'; end if;
 update public.profiles set presence_status='in_game',last_seen_at=now() where id=auth.uid(); return r;
end $$;

create or replace function public.submit_word_expert_v1(p_room_id uuid, p_word text)
returns public.game_rooms language plpgsql security definer set search_path=public,pg_temp as $$
declare r public.game_rooms; clean_word text; previous_word text; expected text; next_player uuid; streak_value int; multiplier int; add_points int; round_winner uuid; next_round int; next_starter uuid; dictionary_known boolean:=false;
begin
 if auth.uid() is null then raise exception 'not_authenticated'; end if;
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null then raise exception 'room_not_found'; end if;
 if r.game_mode<>'expert' then raise exception 'not_expert_room'; end if;
 if r.status not in ('playing','sudden_death') then raise exception 'room_not_playing'; end if;
 if r.is_bot then if auth.uid()<>r.host_id or r.bot_turn or r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
 else if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if; if r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if; end if;
 if r.turn_deadline is not null and r.turn_deadline<now() then return public.claim_turn_timeout(p_room_id); end if;
 clean_word:=public.normalize_game_word(r.language,p_word);
 if char_length(clean_word)<2 or char_length(clean_word)>40 then return public.switch_turn_after_failure(p_room_id,'invalid_word'); end if;
 if (r.language='tr' and clean_word !~ '^[a-zçğıöşü]+$') or (r.language='en' and clean_word !~ '^[a-z]+$') then return public.switch_turn_after_failure(p_room_id,'invalid_word'); end if;
 if not public.sonharf_word_allowed(r.language,clean_word) then return public.switch_turn_after_failure(p_room_id,'invalid_word'); end if;
 select exists(select 1 from public.dictionary_words d where d.language=r.language and d.normalized_word=clean_word and d.active) into dictionary_known;
 if not dictionary_known then
   if char_length(clean_word)<3 then return public.switch_turn_after_failure(p_room_id,'not_in_dictionary'); end if;
   insert into public.dictionary_review_queue(language,normalized_word,raw_word,first_seen_user_id,first_seen_room_id) values(r.language,clean_word,trim(p_word),auth.uid(),r.id)
   on conflict(language,normalized_word) do update set use_count=public.dictionary_review_queue.use_count+1,updated_at=now();
 end if;
 select normalized_word into previous_word from public.game_words where room_id=p_room_id order by id desc limit 1;
 if previous_word is not null then expected:=right(previous_word,least(3,greatest(1,r.round_no))); if left(clean_word,char_length(expected))<>expected then return public.switch_turn_after_failure(p_room_id,'wrong_start_letter'); end if; end if;
 if exists(select 1 from public.game_words where room_id=p_room_id and normalized_word=clean_word) then return public.switch_turn_after_failure(p_room_id,'word_already_used'); end if;
 insert into public.game_words(room_id,player_id,word,normalized_word,is_bot) values(p_room_id,auth.uid(),trim(p_word),clean_word,false);
 if r.status='sudden_death' then update public.profiles set valid_words=valid_words+1 where id=auth.uid(); return public.sonharf_finish_room(r.id,auth.uid(),false,'sudden_death_word'); end if;
 multiplier:=least(3,greatest(1,r.round_no)); streak_value:=case when auth.uid()=r.host_id then r.host_streak+1 else r.guest_streak+1 end; add_points:=3*multiplier+case when streak_value%5=0 then 3 else 0 end;
 if auth.uid()=r.host_id then update public.game_rooms set host_score=host_score+add_points,host_round_score=host_round_score+add_points,host_streak=streak_value where id=r.id;
 else update public.game_rooms set guest_score=guest_score+add_points,guest_round_score=guest_round_score+add_points,guest_streak=streak_value where id=r.id; end if;
 update public.profiles set valid_words=valid_words+1,best_streak=greatest(best_streak,streak_value),word_storms=word_storms+case when streak_value%5=0 then 1 else 0 end where id=auth.uid();
 next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
 update public.game_rooms set valid_word_count=valid_word_count+1,round_word_count=round_word_count+1,
   current_player_id=case when r.is_bot and auth.uid()=r.host_id then null else next_player end,bot_turn=case when r.is_bot and auth.uid()=r.host_id then true else false end,
   turn_deadline=case when r.is_bot and auth.uid()=r.host_id then null else public.sonharf_turn_deadline('expert') end,
   last_event=case when multiplier=3 then 'expert_x3' when multiplier=2 then 'expert_x2' when not dictionary_known then 'provisional_word' when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,last_event_player_id=auth.uid() where id=r.id returning * into r;
 if r.round_word_count>=15 then
   round_winner:=case when r.host_round_score>r.guest_round_score then r.host_id when r.guest_round_score>r.host_round_score then r.guest_id else null end;
   update public.profiles set total_rounds=total_rounds+1,rounds_won=rounds_won+case when id=round_winner then 1 else 0 end where id=r.host_id or (not r.is_bot and id=r.guest_id);
   update public.game_rooms set host_rounds=host_rounds+case when round_winner=host_id then 1 else 0 end,guest_rounds=guest_rounds+case when (not is_bot and round_winner=guest_id) or (is_bot and guest_round_score>host_round_score) then 1 else 0 end where id=r.id returning * into r;
   if r.round_no>=3 then
     if r.host_rounds>r.guest_rounds then return public.sonharf_finish_room(r.id,r.host_id,false,'expert_finished');
     elsif r.guest_rounds>r.host_rounds then return public.sonharf_finish_room(r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'expert_finished');
     elsif r.host_score>r.guest_score then return public.sonharf_finish_room(r.id,r.host_id,false,'expert_score_tiebreak');
     elsif r.guest_score>r.host_score then return public.sonharf_finish_room(r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'expert_score_tiebreak');
     else update public.game_rooms set status='sudden_death',round_word_count=0,host_round_score=0,guest_round_score=0,current_player_id=host_id,bot_turn=false,turn_deadline=public.sonharf_turn_deadline('expert'),last_event='sudden_death_started' where id=r.id returning * into r; return r; end if;
   else
     next_round:=r.round_no+1;
     if r.is_bot then update public.game_rooms set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,host_streak=0,guest_streak=0,current_player_id=case when next_round%2=1 then host_id else null end,bot_turn=(next_round%2=0),turn_deadline=case when next_round%2=1 then public.sonharf_turn_deadline('expert') else null end,last_event='round_started' where id=r.id returning * into r;
     else next_starter:=case when next_round%2=1 then r.host_id else r.guest_id end; update public.game_rooms set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,host_streak=0,guest_streak=0,current_player_id=next_starter,turn_deadline=public.sonharf_turn_deadline('expert'),last_event='round_started' where id=r.id returning * into r; end if;
   end if;
 end if;
 return r;
end $$;

create or replace function public.bot_take_turn_expert_v1(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public,pg_temp as $$
declare r public.game_rooms; previous_word text; expected text; chosen public.dictionary_words; streak_value int; multiplier int; add_points int; round_winner uuid; next_round int;
begin
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null or not r.is_bot or r.game_mode<>'expert' then raise exception 'not_expert_bot_room'; end if;
 if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
 if r.status not in ('playing','sudden_death') or not r.bot_turn then return r; end if;
 select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
 expected:=case when previous_word is null then null else right(previous_word,least(3,greatest(1,r.round_no))) end;
 with candidates as (
   select d.id,d.normalized_word,right(d.normalized_word,least(3,greatest(1,r.round_no))) as next_prefix from public.dictionary_words d
   where d.language=r.language and d.active and public.sonharf_word_allowed(d.language,d.normalized_word)
     and (expected is null or left(d.normalized_word,char_length(expected))=expected)
     and not exists(select 1 from public.game_words w where w.room_id=r.id and w.normalized_word=d.normalized_word)
   order by random() limit 48
 ), ranked as (
   select c.id,(select count(*) from public.dictionary_words reply where reply.language=r.language and reply.active and reply.id<>c.id
     and public.sonharf_word_allowed(reply.language,reply.normalized_word)
     and left(reply.normalized_word,char_length(c.next_prefix))=c.next_prefix
     and not exists(select 1 from public.game_words used where used.room_id=r.id and used.normalized_word=reply.normalized_word)) as reply_count from candidates c
 )
 select d.* into chosen from ranked x join public.dictionary_words d on d.id=x.id order by x.reply_count asc,char_length(d.normalized_word) desc,random() limit 1;
 if chosen.id is null then return public.sonharf_finish_room(r.id,r.host_id,false,'bot_no_word'); end if;
 insert into public.game_words(room_id,player_id,word,normalized_word,is_bot) values(r.id,null,chosen.word,chosen.normalized_word,true);
 if r.status='sudden_death' then return public.sonharf_finish_room(r.id,null,true,'sudden_death_word'); end if;
 multiplier:=least(3,greatest(1,r.round_no)); streak_value:=r.guest_streak+1; add_points:=3*multiplier+case when streak_value%5=0 then 3 else 0 end;
 update public.game_rooms set guest_score=guest_score+add_points,guest_round_score=guest_round_score+add_points,guest_streak=streak_value,valid_word_count=valid_word_count+1,round_word_count=round_word_count+1,current_player_id=host_id,bot_turn=false,turn_deadline=public.sonharf_turn_deadline('expert'),last_event=case when multiplier=3 then 'expert_x3' when multiplier=2 then 'expert_x2' when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,last_event_player_id=null where id=r.id returning * into r;
 if r.round_word_count>=15 then
   round_winner:=case when r.host_round_score>r.guest_round_score then r.host_id when r.guest_round_score>r.host_round_score then null else null end;
   update public.profiles set total_rounds=total_rounds+1,rounds_won=rounds_won+case when r.host_round_score>r.guest_round_score then 1 else 0 end where id=r.host_id;
   update public.game_rooms set host_rounds=host_rounds+case when host_round_score>guest_round_score then 1 else 0 end,guest_rounds=guest_rounds+case when guest_round_score>host_round_score then 1 else 0 end where id=r.id returning * into r;
   if r.round_no>=3 then
     if r.host_rounds>r.guest_rounds then return public.sonharf_finish_room(r.id,r.host_id,false,'expert_finished');
     elsif r.guest_rounds>r.host_rounds then return public.sonharf_finish_room(r.id,null,true,'expert_finished');
     elsif r.host_score>r.guest_score then return public.sonharf_finish_room(r.id,r.host_id,false,'expert_score_tiebreak');
     elsif r.guest_score>r.host_score then return public.sonharf_finish_room(r.id,null,true,'expert_score_tiebreak');
     else update public.game_rooms set status='sudden_death',round_word_count=0,host_round_score=0,guest_round_score=0,current_player_id=host_id,bot_turn=false,turn_deadline=public.sonharf_turn_deadline('expert'),last_event='sudden_death_started' where id=r.id returning * into r; return r; end if;
   else
     next_round:=r.round_no+1;
     update public.game_rooms set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,host_streak=0,guest_streak=0,current_player_id=case when next_round%2=1 then host_id else null end,bot_turn=(next_round%2=0),turn_deadline=case when next_round%2=1 then public.sonharf_turn_deadline('expert') else null end,last_event='round_started' where id=r.id returning * into r;
   end if;
 end if;
 return r;
end $$;
