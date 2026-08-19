-- Son Harf: 3 x 10-word rounds + mutual rematch.
-- Apply after the existing 20260819 migrations.

alter table public.game_rooms
  add column if not exists round_no integer not null default 1,
  add column if not exists round_word_count integer not null default 0,
  add column if not exists host_rounds integer not null default 0,
  add column if not exists guest_rounds integer not null default 0,
  add column if not exists rematch_of uuid references public.game_rooms(id),
  add column if not exists host_rematch boolean not null default false,
  add column if not exists guest_rematch boolean not null default false;

create or replace function public.request_rematch(p_room_id uuid)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; me uuid:=auth.uid(); new_room public.game_rooms;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or me not in (r.host_id,r.guest_id) then raise exception 'room_not_found'; end if;
  if r.status <> 'finished' then raise exception 'match_not_finished'; end if;
  if me=r.host_id then update public.game_rooms set host_rematch=true where id=r.id returning * into r;
  else update public.game_rooms set guest_rematch=true where id=r.id returning * into r; end if;
  if r.host_rematch and r.guest_rematch then
    insert into public.game_rooms(code,host_id,guest_id,status,language,current_player_id,turn_deadline,rematch_of)
    values (upper(substr(md5(random()::text),1,6)),r.host_id,r.guest_id,'playing',r.language,r.host_id,now()+interval '45 seconds',r.id)
    returning * into new_room;
    update public.profiles set presence_status='in_game' where id in (r.host_id,r.guest_id);
    return new_room;
  end if;
  return r;
end $$;

grant execute on function public.request_rematch(uuid) to authenticated;

-- Round accounting is deliberately isolated so submit_word can call it after every valid word.
create or replace function public.advance_round_if_needed(p_room_id uuid)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; round_winner uuid;
begin
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null then raise exception 'room_not_found'; end if;
 update public.game_rooms set round_word_count=(valid_word_count % 10) where id=r.id returning * into r;
 if r.valid_word_count>0 and r.valid_word_count % 10=0 and r.valid_word_count<=30 then
   if r.host_score>r.guest_score then round_winner:=r.host_id;
   elsif r.guest_score>r.host_score then round_winner:=r.guest_id; else round_winner:=null; end if;
   update public.game_rooms set
     host_rounds=host_rounds+case when round_winner=host_id then 1 else 0 end,
     guest_rounds=guest_rounds+case when round_winner=guest_id then 1 else 0 end,
     round_no=least(3,round_no+case when valid_word_count<30 then 1 else 0 end),
     round_word_count=0
   where id=r.id returning * into r;
 end if;
 return r;
end $$;

grant execute on function public.advance_round_if_needed(uuid) to authenticated;
