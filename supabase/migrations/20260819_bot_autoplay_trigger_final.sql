-- Server-side bot automation for the Android client currently calling v3 RPCs.
-- This keeps bot logic authoritative even if a modified client skips bot callbacks.

create or replace function public.auto_bot_turn_after_flag()
returns trigger language plpgsql security definer set search_path=public as $$
begin
  if new.is_bot and new.bot_turn and new.status in ('playing','final','sudden_death') then
    perform public.bot_take_turn(new.id);
  end if;
  return new;
end $$;

drop trigger if exists game_rooms_auto_bot_turn on public.game_rooms;
create trigger game_rooms_auto_bot_turn
after update of bot_turn on public.game_rooms
for each row
when (new.bot_turn = true)
execute function public.auto_bot_turn_after_flag();

-- Preserve v3 public API but let the bot respond after a human trivia attempt.
create or replace function public.answer_trivia_v3(p_round_id uuid,p_answer_index int)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; tr public.trivia_rounds; human_wrong boolean;
begin
  r:=public.answer_trivia_v2(p_round_id,p_answer_index);
  if not r.is_bot or r.status<>'quiz' then return r; end if;
  select * into tr from public.trivia_rounds where id=p_round_id;
  if tr.resolved_at is not null then return r; end if;
  select exists(select 1 from public.trivia_answers where round_id=tr.id and player_id=r.host_id and not is_correct) into human_wrong;
  if tr.bot_attempted and human_wrong then
    return public.finish_bot_quiz_without_bonus(r.id,tr.id);
  end if;
  -- Human has acted and answer choices are unlocked; give the bot one server-side attempt.
  r:=public.bot_answer_trivia(r.id);
  return r;
end $$;
grant execute on function public.answer_trivia_v3(uuid,int) to authenticated;
