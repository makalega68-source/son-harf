-- Daily Arena v1
-- One official 60-second run per language/day. Same letters for everyone.
-- Letters stay hidden until the official run starts.
-- Completion reward is capped to once per user/day across languages.

create table if not exists public.daily_arena_challenges (
  challenge_date date not null,
  language text not null check(language in ('tr','en')),
  letters text not null,
  created_at timestamptz not null default now(),
  primary key(challenge_date,language)
);

create table if not exists public.daily_arena_runs (
  id uuid primary key default gen_random_uuid(),
  challenge_date date not null,
  language text not null check(language in ('tr','en')),
  user_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'playing' check(status in ('playing','finished')),
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  score integer not null default 0 check(score>=0),
  word_count integer not null default 0 check(word_count>=0),
  longest_word text not null default '',
  best_combo integer not null default 0 check(best_combo>=0),
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  unique(challenge_date,language,user_id),
  foreign key(challenge_date,language)
    references public.daily_arena_challenges(challenge_date,language)
    on delete cascade
);

create table if not exists public.daily_arena_words (
  id bigint generated always as identity primary key,
  run_id uuid not null references public.daily_arena_runs(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  word text not null,
  normalized_word text not null,
  base_points integer not null check(base_points>=0),
  combo integer not null default 1 check(combo>=1),
  created_at timestamptz not null default now(),
  unique(run_id,normalized_word)
);

create table if not exists public.daily_arena_rewards (
  user_id uuid not null references public.profiles(id) on delete cascade,
  challenge_date date not null,
  reward_coins integer not null default 8 check(reward_coins>=0),
  claimed_at timestamptz not null default now(),
  primary key(user_id,challenge_date)
);

alter table public.daily_arena_challenges enable row level security;
alter table public.daily_arena_runs enable row level security;
alter table public.daily_arena_words enable row level security;
alter table public.daily_arena_rewards enable row level security;

revoke all on public.daily_arena_challenges from anon,authenticated;
revoke all on public.daily_arena_runs from anon,authenticated;
revoke all on public.daily_arena_words from anon,authenticated;
revoke all on public.daily_arena_rewards from anon,authenticated;

create index if not exists daily_arena_runs_user_date_idx
  on public.daily_arena_runs(user_id,challenge_date desc);

create index if not exists daily_arena_runs_board_idx
  on public.daily_arena_runs(challenge_date,language,status,score desc,word_count desc,best_combo desc);

create index if not exists daily_arena_words_run_created_idx
  on public.daily_arena_words(run_id,created_at,id);

create index if not exists daily_arena_words_user_idx
  on public.daily_arena_words(user_id);

create or replace function public.daily_arena_letter_set_v1(
  p_date date,
  p_language text
)
returns text
language plpgsql
immutable
set search_path=''
as $$
declare
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_sets text[];
  v_idx int;
begin
  if v_lang='en' then
    v_sets:=array[
      'aeimnorst','aelnrstio','aelrstion',
      'aerlnstic','aerstlinc','aeglnrstu'
    ];
  else
    v_sets:=array[
      'aelrmtskin','aerltmison','aeilmnorst','aelrktsuin',
      'aeiklnrst','aerlnmikt','aeılnrstok','aegilnrstu'
    ];
  end if;

  v_idx:=1 + (
    get_byte(decode(md5(p_date::text||':'||v_lang),'hex'),0)
    % array_length(v_sets,1)
  );
  return v_sets[v_idx];
end
$$;

create or replace function public.ensure_daily_arena_challenge_v1(
  p_language text
)
returns public.daily_arena_challenges
language plpgsql
security definer
set search_path=''
as $$
declare
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_date date:=(clock_timestamp() at time zone 'Europe/Istanbul')::date;
  c public.daily_arena_challenges;
begin
  insert into public.daily_arena_challenges(challenge_date,language,letters)
  values(v_date,v_lang,public.daily_arena_letter_set_v1(v_date,v_lang))
  on conflict(challenge_date,language) do nothing;

  select dac.* into c
  from public.daily_arena_challenges dac
  where dac.challenge_date=v_date and dac.language=v_lang;

  return c;
end
$$;

create or replace function public.daily_arena_streaks_v1(
  p_user_id uuid
)
returns table(current_streak integer,best_streak integer)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_today date:=(clock_timestamp() at time zone 'Europe/Istanbul')::date;
  v_latest date;
  v_current int:=0;
  v_best int:=0;
begin
  with days as (
    select distinct dar.challenge_date
    from public.daily_arena_runs dar
    where dar.user_id=p_user_id
      and dar.status='finished'
      and dar.score>0
  ),
  seq as (
    select
      d.challenge_date,
      d.challenge_date - row_number() over(order by d.challenge_date)::int grp
    from days d
  ),
  groups as (
    select s.grp,count(*)::int cnt
    from seq s
    group by s.grp
  )
  select coalesce(max(g.cnt),0) into v_best
  from groups g;

  select max(dar.challenge_date) into v_latest
  from public.daily_arena_runs dar
  where dar.user_id=p_user_id
    and dar.status='finished'
    and dar.score>0;

  if v_latest is not null and v_latest>=v_today-1 then
    with days as (
      select distinct dar.challenge_date
      from public.daily_arena_runs dar
      where dar.user_id=p_user_id
        and dar.status='finished'
        and dar.score>0
        and dar.challenge_date<=v_latest
    ),
    ordered as (
      select
        d.challenge_date,
        row_number() over(order by d.challenge_date desc)::int rn
      from days d
    )
    select count(*)::int into v_current
    from ordered o
    where o.challenge_date=v_latest-(o.rn-1);
  end if;

  return query select v_current,v_best;
end
$$;

create or replace function public.finish_daily_arena_run_internal_v1(
  p_run_id uuid
)
returns public.daily_arena_runs
language plpgsql
security definer
set search_path=''
as $$
declare
  r public.daily_arena_runs;
  v_score int:=0;
  v_count int:=0;
  v_longest text:='';
  v_combo int:=0;
  v_reward int;
  v_balance int;
begin
  select dar.* into r
  from public.daily_arena_runs dar
  where dar.id=p_run_id
  for update;

  if r.id is null then raise exception 'daily_arena_run_not_found'; end if;
  if r.status='finished' then return r; end if;
  if clock_timestamp()<r.ends_at then return r; end if;

  select
    coalesce(sum(daw.base_points),0)::int,
    count(*)::int,
    coalesce(
      (
        select daw2.word
        from public.daily_arena_words daw2
        where daw2.run_id=r.id
        order by char_length(daw2.normalized_word) desc,daw2.created_at asc,daw2.id asc
        limit 1
      ),
      ''
    ),
    coalesce(max(daw.combo),0)::int
  into v_score,v_count,v_longest,v_combo
  from public.daily_arena_words daw
  where daw.run_id=r.id;

  update public.daily_arena_runs dar
  set status='finished',
      score=v_score,
      word_count=v_count,
      longest_word=v_longest,
      best_combo=v_combo,
      finished_at=clock_timestamp()
  where dar.id=r.id
  returning dar.* into r;

  if v_score>0 then
    insert into public.daily_arena_rewards(user_id,challenge_date,reward_coins)
    values(r.user_id,r.challenge_date,8)
    on conflict(user_id,challenge_date) do nothing
    returning reward_coins into v_reward;

    if v_reward is not null then
      update public.profiles p
      set diamonds=coalesce(p.diamonds,0)+v_reward,
          updated_at=clock_timestamp()
      where p.id=r.user_id
      returning p.diamonds into v_balance;

      insert into public.diamond_ledger(user_id,delta,reason)
      values(r.user_id,v_reward,'daily_arena_completion:'||r.challenge_date::text);
    end if;
  end if;

  return r;
end
$$;

create or replace function public.start_daily_arena_v1(
  p_language text default 'tr'
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_date date:=(clock_timestamp() at time zone 'Europe/Istanbul')::date;
  c public.daily_arena_challenges;
  r public.daily_arena_runs;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  c:=public.ensure_daily_arena_challenge_v1(v_lang);

  select dar.* into r
  from public.daily_arena_runs dar
  where dar.challenge_date=v_date
    and dar.language=v_lang
    and dar.user_id=v_uid
  for update;

  if r.id is not null then
    if r.status='playing' and clock_timestamp()>=r.ends_at then
      r:=public.finish_daily_arena_run_internal_v1(r.id);
    end if;

    return jsonb_build_object(
      'run_id',r.id,'status',r.status,'challenge_date',r.challenge_date,
      'language',r.language,'letters',c.letters,'starts_at',r.starts_at,
      'ends_at',r.ends_at,'score',r.score,'word_count',r.word_count,
      'longest_word',r.longest_word,'best_combo',r.best_combo
    );
  end if;

  if exists(
    select 1 from public.game_rooms g
    where g.status in ('playing','quiz','final','sudden_death')
      and (g.host_id=v_uid or g.guest_id=v_uid)
  ) or exists(
    select 1 from public.word_arena_rooms a
    where a.status='playing'
      and a.ends_at>clock_timestamp()
      and (a.host_id=v_uid or a.guest_id=v_uid)
  ) then
    raise exception 'player_already_in_game';
  end if;

  if exists(
    select 1 from public.daily_arena_runs x
    where x.user_id=v_uid
      and x.status='playing'
      and x.ends_at>clock_timestamp()
  ) then
    raise exception 'daily_arena_active';
  end if;

  insert into public.daily_arena_runs(
    challenge_date,language,user_id,status,starts_at,ends_at
  )
  values(
    v_date,v_lang,v_uid,'playing',
    clock_timestamp()+interval '3 seconds',
    clock_timestamp()+interval '63 seconds'
  )
  returning * into r;

  return jsonb_build_object(
    'run_id',r.id,'status',r.status,'challenge_date',r.challenge_date,
    'language',r.language,'letters',c.letters,'starts_at',r.starts_at,
    'ends_at',r.ends_at,'score',r.score,'word_count',r.word_count,
    'longest_word',r.longest_word,'best_combo',r.best_combo
  );
end
$$;

create or replace function public.submit_daily_arena_word_v1(
  p_run_id uuid,
  p_word text
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  r public.daily_arena_runs;
  c public.daily_arena_challenges;
  v_word text:=trim(coalesce(p_word,''));
  v_norm text;
  v_len int;
  v_combo int:=1;
  v_prev public.daily_arena_words;
  v_points int;
  v_score int;
  v_count int;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select dar.* into r
  from public.daily_arena_runs dar
  where dar.id=p_run_id
  for update;

  if r.id is null then raise exception 'daily_arena_run_not_found'; end if;
  if r.user_id<>v_uid then raise exception 'daily_arena_not_owner'; end if;
  if r.status<>'playing' then
    return jsonb_build_object('accepted',false,'status',r.status);
  end if;

  if clock_timestamp()>=r.ends_at then
    r:=public.finish_daily_arena_run_internal_v1(r.id);
    return jsonb_build_object('accepted',false,'status','finished');
  end if;

  if clock_timestamp()<r.starts_at then raise exception 'daily_arena_not_started'; end if;

  select dac.* into c
  from public.daily_arena_challenges dac
  where dac.challenge_date=r.challenge_date
    and dac.language=r.language;

  v_norm:=public.normalize_game_word(r.language,v_word);
  v_len:=char_length(v_norm);

  if v_len<3 or v_len>10 then raise exception 'daily_arena_word_length'; end if;
  if not public.arena_word_fits_letters_v1(v_norm,c.letters) then
    raise exception 'daily_arena_letters_mismatch';
  end if;

  if not exists(
    select 1
    from public.dictionary_words d
    where d.language=r.language
      and d.active
      and d.normalized_word=v_norm
  ) then
    raise exception 'daily_arena_invalid_word';
  end if;

  if exists(
    select 1
    from public.daily_arena_words daw
    where daw.run_id=r.id
      and daw.normalized_word=v_norm
  ) then
    raise exception 'daily_arena_duplicate_word';
  end if;

  select daw.* into v_prev
  from public.daily_arena_words daw
  where daw.run_id=r.id
  order by daw.created_at desc,daw.id desc
  limit 1;

  if v_prev.id is not null
     and clock_timestamp()-v_prev.created_at<=interval '8 seconds'
  then
    v_combo:=least(v_prev.combo+1,99);
  end if;

  v_points:=
    v_len
    +greatest(0,v_len-4)*2
    +least(greatest(v_combo-1,0),4);

  insert into public.daily_arena_words(
    run_id,user_id,word,normalized_word,base_points,combo
  )
  values(r.id,v_uid,v_word,v_norm,v_points,v_combo);

  select
    coalesce(sum(daw.base_points),0)::int,
    count(*)::int
  into v_score,v_count
  from public.daily_arena_words daw
  where daw.run_id=r.id;

  update public.daily_arena_runs dar
  set score=v_score,
      word_count=v_count,
      best_combo=greatest(dar.best_combo,v_combo),
      longest_word=case
        when char_length(v_norm)>char_length(coalesce(dar.longest_word,''))
        then v_word
        else dar.longest_word
      end
  where dar.id=r.id;

  return jsonb_build_object(
    'accepted',true,'status','playing','word',v_word,
    'normalized_word',v_norm,'base_points',v_points,'combo',v_combo,
    'score',v_score,'word_count',v_count
  );
end
$$;

create or replace function public.get_daily_arena_status_v1(
  p_language text default 'tr'
)
returns table(
  challenge_date date,
  language text,
  run_id uuid,
  status text,
  letters text,
  starts_at timestamptz,
  ends_at timestamptz,
  score integer,
  word_count integer,
  longest_word text,
  best_combo integer,
  reward_coins integer,
  current_streak integer,
  best_streak integer,
  my_rank bigint,
  player_count bigint
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_date date:=(clock_timestamp() at time zone 'Europe/Istanbul')::date;
  c public.daily_arena_challenges;
  r public.daily_arena_runs;
  v_reward int:=0;
  v_current int:=0;
  v_best int:=0;
  v_rank bigint:=0;
  v_count bigint:=0;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  c:=public.ensure_daily_arena_challenge_v1(v_lang);

  select dar.* into r
  from public.daily_arena_runs dar
  where dar.challenge_date=v_date
    and dar.language=v_lang
    and dar.user_id=v_uid;

  if r.id is not null
     and r.status='playing'
     and clock_timestamp()>=r.ends_at
  then
    r:=public.finish_daily_arena_run_internal_v1(r.id);
  end if;

  select coalesce(darw.reward_coins,0) into v_reward
  from public.daily_arena_rewards darw
  where darw.user_id=v_uid
    and darw.challenge_date=v_date;

  select s.current_streak,s.best_streak
  into v_current,v_best
  from public.daily_arena_streaks_v1(v_uid) s;

  select count(*)::bigint into v_count
  from public.daily_arena_runs x
  where x.challenge_date=v_date
    and x.language=v_lang
    and x.status='finished'
    and x.score>0;

  if r.id is not null and r.status='finished' and r.score>0 then
    with ranked as (
      select
        x.user_id,
        row_number() over(
          order by x.score desc,x.word_count desc,x.best_combo desc,x.finished_at asc,x.user_id
        )::bigint rnk
      from public.daily_arena_runs x
      where x.challenge_date=v_date
        and x.language=v_lang
        and x.status='finished'
        and x.score>0
    )
    select rr.rnk into v_rank
    from ranked rr
    where rr.user_id=v_uid;
  end if;

  return query
  select
    v_date,v_lang,r.id,coalesce(r.status,'not_started'),
    case when r.id is null then null else c.letters end,
    r.starts_at,r.ends_at,coalesce(r.score,0),coalesce(r.word_count,0),
    coalesce(r.longest_word,''),coalesce(r.best_combo,0),
    coalesce(v_reward,0),coalesce(v_current,0),coalesce(v_best,0),
    coalesce(v_rank,0),coalesce(v_count,0);
end
$$;

create or replace function public.get_daily_arena_words_v1(
  p_run_id uuid
)
returns table(
  word text,
  normalized_word text,
  base_points integer,
  combo integer,
  created_at timestamptz
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  if not exists(
    select 1
    from public.daily_arena_runs dar
    where dar.id=p_run_id and dar.user_id=v_uid
  ) then
    raise exception 'daily_arena_not_owner';
  end if;

  return query
  select daw.word,daw.normalized_word,daw.base_points,daw.combo,daw.created_at
  from public.daily_arena_words daw
  where daw.run_id=p_run_id
  order by daw.created_at,daw.id;
end
$$;

create or replace function public.get_daily_arena_leaderboard_v1(
  p_language text default 'tr',
  p_limit integer default 50
)
returns table(
  user_id uuid,
  display_name text,
  score integer,
  word_count integer,
  best_combo integer,
  longest_word text,
  rank bigint,
  is_me boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_date date:=(clock_timestamp() at time zone 'Europe/Istanbul')::date;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  perform public.ensure_daily_arena_challenge_v1(v_lang);

  return query
  with ranked as (
    select
      dar.user_id,
      dar.score,
      dar.word_count,
      dar.best_combo,
      dar.longest_word,
      row_number() over(
        order by dar.score desc,dar.word_count desc,dar.best_combo desc,dar.finished_at asc,dar.user_id
      )::bigint rnk
    from public.daily_arena_runs dar
    where dar.challenge_date=v_date
      and dar.language=v_lang
      and dar.status='finished'
      and dar.score>0
  )
  select
    r.user_id,p.display_name,r.score,r.word_count,r.best_combo,
    r.longest_word,r.rnk,r.user_id=v_uid
  from ranked r
  join public.profiles p on p.id=r.user_id
  order by r.rnk
  limit greatest(1,least(coalesce(p_limit,50),100));
end
$$;

revoke all on function public.daily_arena_letter_set_v1(date,text) from public,anon,authenticated;
revoke all on function public.ensure_daily_arena_challenge_v1(text) from public,anon,authenticated;
revoke all on function public.daily_arena_streaks_v1(uuid) from public,anon,authenticated;
revoke all on function public.finish_daily_arena_run_internal_v1(uuid) from public,anon,authenticated;

revoke all on function public.start_daily_arena_v1(text) from public,anon;
revoke all on function public.submit_daily_arena_word_v1(uuid,text) from public,anon;
revoke all on function public.get_daily_arena_status_v1(text) from public,anon;
revoke all on function public.get_daily_arena_words_v1(uuid) from public,anon;
revoke all on function public.get_daily_arena_leaderboard_v1(text,integer) from public,anon;

grant execute on function public.start_daily_arena_v1(text) to authenticated;
grant execute on function public.submit_daily_arena_word_v1(uuid,text) to authenticated;
grant execute on function public.get_daily_arena_status_v1(text) to authenticated;
grant execute on function public.get_daily_arena_words_v1(uuid) to authenticated;
grant execute on function public.get_daily_arena_leaderboard_v1(text,integer) to authenticated;


-- Prevent a timed Daily Arena run from overlapping other competitive modes.
create or replace function public.prevent_queue_during_daily_arena_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if new.status='waiting'
     and exists(
       select 1
       from public.daily_arena_runs dar
       where dar.user_id=new.user_id
         and dar.status='playing'
         and dar.ends_at>clock_timestamp()
     )
  then
    raise exception 'daily_arena_active';
  end if;

  return new;
end
$$;

create or replace function public.prevent_room_during_daily_arena_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if exists(
    select 1
    from public.daily_arena_runs dar
    where dar.status='playing'
      and dar.ends_at>clock_timestamp()
      and (
        dar.user_id=new.host_id
        or (new.guest_id is not null and dar.user_id=new.guest_id)
      )
  ) then
    raise exception 'daily_arena_active';
  end if;

  return new;
end
$$;

drop trigger if exists trg_prevent_matchmaking_queue_during_daily_arena_v1
  on public.matchmaking_queue;
create trigger trg_prevent_matchmaking_queue_during_daily_arena_v1
before insert or update of status
on public.matchmaking_queue
for each row
execute function public.prevent_queue_during_daily_arena_v1();

drop trigger if exists trg_prevent_word_arena_queue_during_daily_arena_v1
  on public.word_arena_queue;
create trigger trg_prevent_word_arena_queue_during_daily_arena_v1
before insert or update of status
on public.word_arena_queue
for each row
execute function public.prevent_queue_during_daily_arena_v1();

drop trigger if exists trg_prevent_game_room_during_daily_arena_v1
  on public.game_rooms;
create trigger trg_prevent_game_room_during_daily_arena_v1
before insert
on public.game_rooms
for each row
execute function public.prevent_room_during_daily_arena_v1();

drop trigger if exists trg_prevent_word_arena_room_during_daily_arena_v1
  on public.word_arena_rooms;
create trigger trg_prevent_word_arena_room_during_daily_arena_v1
before insert
on public.word_arena_rooms
for each row
execute function public.prevent_room_during_daily_arena_v1();

revoke all on function public.prevent_queue_during_daily_arena_v1()
  from public,anon,authenticated;
revoke all on function public.prevent_room_during_daily_arena_v1()
  from public,anon,authenticated;
