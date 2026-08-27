-- Son Harf Engagement V3
-- Adds the original daily word challenge "Günün Şifresi", Ustalık Yolu,
-- arch-rival summary and fair 20-player weekly pods.
-- Visible currency remains Son Coin; profiles.diamonds is retained as legacy storage.

create table if not exists public.daily_cipher_sessions (
  user_id uuid not null references public.profiles(id) on delete cascade,
  challenge_date date not null default current_date,
  language text not null check (language in ('tr','en')),
  attempts integer not null default 0 check (attempts between 0 and 6),
  guesses text[] not null default '{}'::text[],
  feedbacks text[] not null default '{}'::text[],
  won boolean not null default false,
  finished boolean not null default false,
  reward_coins integer not null default 0,
  updated_at timestamptz not null default now(),
  primary key (user_id, challenge_date, language)
);

alter table public.daily_cipher_sessions enable row level security;
drop policy if exists daily_cipher_self_select on public.daily_cipher_sessions;
create policy daily_cipher_self_select
on public.daily_cipher_sessions for select
to authenticated
using ((select auth.uid()) = user_id);

revoke all on table public.daily_cipher_sessions from anon;
grant select on table public.daily_cipher_sessions to authenticated;

create table if not exists public.mastery_reward_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  milestone_id text not null,
  reward_coins integer not null check (reward_coins >= 0),
  claimed_at timestamptz not null default now(),
  primary key (user_id, milestone_id)
);

alter table public.mastery_reward_claims enable row level security;
drop policy if exists mastery_reward_self_select on public.mastery_reward_claims;
create policy mastery_reward_self_select
on public.mastery_reward_claims for select
to authenticated
using ((select auth.uid()) = user_id);

revoke all on table public.mastery_reward_claims from anon;
grant select on table public.mastery_reward_claims to authenticated;

create or replace function public.normalize_cipher_word_v1(p_text text)
returns text
language sql
immutable
set search_path=public,pg_temp
as $$
  select replace(translate(lower(trim(coalesce(p_text,''))), 'çğıöşü', 'cgiosu'), 'ı', 'i');
$$;

revoke all on function public.normalize_cipher_word_v1(text) from public,anon,authenticated;

create or replace function public.daily_cipher_answer_v1(p_date date, p_language text)
returns text
language plpgsql
stable
set search_path=public,pg_temp
as $$
declare
  v_tr text[] := array[
    'KALEM','BULUT','GÜNEŞ','MASAL','BAHAR','YOLCU','ÇORAP','KUMRU','TABAK','KÖPEK',
    'ÇANTA','KAVUN','SARAY','KİRAZ','DENİZ','TOPRA','KARGA','ARMUT','KABAK','SABAH',
    'YAPRA','KÜREK','YEMEK','KAPAK','KUMSA','BALIK','ÇUBUK','KAZAN','KÖMÜR','UZMAN'
  ];
  v_en text[] := array[
    'APPLE','BRAIN','CLOUD','DREAM','EARTH','FLAME','GRAPE','HOUSE','LIGHT','MUSIC',
    'NIGHT','OCEAN','PLANT','QUEEN','RIVER','SMILE','TRAIN','WATER','WORLD','YOUTH',
    'BREAD','CHAIR','GREEN','HEART','MOUSE','STONE','TABLE','TIGER','WOMAN','WRITE'
  ];
  v_words text[];
  v_idx integer;
begin
  v_words := case when lower(coalesce(p_language,'tr'))='en' then v_en else v_tr end;
  v_idx := 1 + mod(abs(hashtext(p_date::text || ':' || lower(coalesce(p_language,'tr')))), array_length(v_words,1));
  return v_words[v_idx];
end;
$$;

revoke all on function public.daily_cipher_answer_v1(date,text) from public,anon,authenticated;

create or replace function public.get_daily_cipher_status_v1(p_language text default 'tr')
returns table(
  challenge_date date,
  language text,
  attempts integer,
  max_attempts integer,
  guesses text[],
  feedbacks text[],
  won boolean,
  finished boolean,
  answer text,
  reward_coins integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  return query
  select
    current_date,
    v_lang,
    coalesce(s.attempts,0),
    6,
    coalesce(s.guesses,'{}'::text[]),
    coalesce(s.feedbacks,'{}'::text[]),
    coalesce(s.won,false),
    coalesce(s.finished,false),
    case when coalesce(s.finished,false) then public.daily_cipher_answer_v1(current_date,v_lang) else null end,
    coalesce(s.reward_coins,0)
  from (select 1) seed
  left join public.daily_cipher_sessions s
    on s.user_id=v_uid and s.challenge_date=current_date and s.language=v_lang;
end;
$$;

revoke all on function public.get_daily_cipher_status_v1(text) from public,anon;
grant execute on function public.get_daily_cipher_status_v1(text) to authenticated;

create or replace function public.submit_daily_cipher_guess_v1(p_language text, p_guess text)
returns table(
  challenge_date date,
  language text,
  attempts integer,
  max_attempts integer,
  guesses text[],
  feedbacks text[],
  won boolean,
  finished boolean,
  answer text,
  reward_coins integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_guess_display text := upper(trim(coalesce(p_guess,'')));
  v_guess text := public.normalize_cipher_word_v1(p_guess);
  v_answer_display text := public.daily_cipher_answer_v1(current_date,v_lang);
  v_answer text := public.normalize_cipher_word_v1(v_answer_display);
  v_guess_chars text[];
  v_answer_chars text[];
  v_feedback text[] := array['X','X','X','X','X'];
  v_used boolean[] := array[false,false,false,false,false];
  v_feedback_text text;
  v_attempts integer;
  v_guesses text[];
  v_feedbacks text[];
  v_won boolean;
  v_finished boolean;
  v_reward integer;
  v_i integer;
  v_j integer;
  v_new_reward integer := 0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if v_guess !~ '^[a-z]{5}$' then raise exception 'invalid_five_letter_word'; end if;

  insert into public.daily_cipher_sessions(user_id,challenge_date,language)
  values(v_uid,current_date,v_lang)
  on conflict (user_id,challenge_date,language) do nothing;

  select s.attempts,s.guesses,s.feedbacks,s.won,s.finished,s.reward_coins
    into v_attempts,v_guesses,v_feedbacks,v_won,v_finished,v_reward
  from public.daily_cipher_sessions s
  where s.user_id=v_uid and s.challenge_date=current_date and s.language=v_lang
  for update;

  if v_finished then
    return query
      select current_date,v_lang,v_attempts,6,v_guesses,v_feedbacks,v_won,true,v_answer_display,v_reward;
    return;
  end if;

  if exists(select 1 from unnest(v_guesses) g where public.normalize_cipher_word_v1(g)=v_guess) then
    raise exception 'guess_already_used';
  end if;

  v_guess_chars := regexp_split_to_array(v_guess,'');
  v_answer_chars := regexp_split_to_array(v_answer,'');

  for v_i in 1..5 loop
    if v_guess_chars[v_i]=v_answer_chars[v_i] then
      v_feedback[v_i] := 'G';
      v_used[v_i] := true;
    end if;
  end loop;

  for v_i in 1..5 loop
    if v_feedback[v_i]='G' then continue; end if;
    for v_j in 1..5 loop
      if not v_used[v_j] and v_guess_chars[v_i]=v_answer_chars[v_j] then
        v_feedback[v_i] := 'Y';
        v_used[v_j] := true;
        exit;
      end if;
    end loop;
  end loop;

  v_feedback_text := array_to_string(v_feedback,'');
  v_attempts := v_attempts + 1;
  v_guesses := array_append(v_guesses,v_guess_display);
  v_feedbacks := array_append(v_feedbacks,v_feedback_text);
  v_won := (v_guess=v_answer);
  v_finished := v_won or v_attempts>=6;

  if v_won and v_reward=0 then
    v_new_reward := 35;
    v_reward := v_new_reward;
    update public.profiles
      set diamonds=coalesce(diamonds,0)+v_new_reward,updated_at=now()
      where id=v_uid;
    if to_regclass('public.diamond_ledger') is not null then
      insert into public.diamond_ledger(user_id,delta,reason)
      values(v_uid,v_new_reward,'daily_cipher_win');
    end if;
  end if;

  update public.daily_cipher_sessions
  set attempts=v_attempts,guesses=v_guesses,feedbacks=v_feedbacks,won=v_won,
      finished=v_finished,reward_coins=v_reward,updated_at=now()
  where user_id=v_uid and challenge_date=current_date and language=v_lang;

  return query
    select current_date,v_lang,v_attempts,6,v_guesses,v_feedbacks,v_won,v_finished,
           case when v_finished then v_answer_display else null end,v_reward;
end;
$$;

revoke all on function public.submit_daily_cipher_guess_v1(text,text) from public,anon;
grant execute on function public.submit_daily_cipher_guess_v1(text,text) to authenticated;

create or replace function public.get_mastery_path_v1()
returns table(
  id text,
  title_tr text,
  title_en text,
  description_tr text,
  description_en text,
  progress integer,
  target integer,
  reward_coins integer,
  unlocked boolean,
  claimed boolean
)
language sql
security definer
set search_path=public,pg_temp
as $$
with stats as (
  select
    coalesce(p.total_matches,p.wins+p.losses,0)::int matches,
    coalesce(p.wins,0)::int wins,
    coalesce(p.valid_words,0)::int valid_words,
    coalesce(p.best_streak,0)::int best_streak,
    coalesce(p.rating,1000)::int rating,
    coalesce((select count(distinct gw.normalized_word)::int
              from public.game_words gw
              where gw.player_id=(select auth.uid()) and coalesce(gw.is_bot,false)=false),0) unique_words
  from public.profiles p where p.id=(select auth.uid())
), milestones as (
  select * from (values
    ('ilk_duello','İLK DÜELLO','FIRST DUEL','İlk maçını tamamla.','Finish your first match.','matches',1,20),
    ('ilk_5_zafer','5 ZAFER','5 WINS','5 galibiyete ulaş.','Reach 5 wins.','wins',5,40),
    ('kelime_100','100 KELİME','100 WORDS','100 farklı kelime kullan.','Use 100 unique words.','unique_words',100,60),
    ('seri_10','SERİ USTASI','STREAK MASTER','10 doğru hamlelik en iyi seriye ulaş.','Reach a best streak of 10 correct moves.','best_streak',10,80),
    ('rating_1200','YÜKSELEN RAKİP','RISING RIVAL','1200 rating seviyesine ulaş.','Reach 1200 rating.','rating',1200,100),
    ('zafer_50','50 ZAFER','50 WINS','50 galibiyete ulaş.','Reach 50 wins.','wins',50,120),
    ('kelime_500','KELİME USTASI','WORD MASTER','500 farklı kelime kullan.','Use 500 unique words.','unique_words',500,160),
    ('rating_1500','ARENA USTASI','ARENA MASTER','1500 rating seviyesine ulaş.','Reach 1500 rating.','rating',1500,200),
    ('zafer_100','EFSANE','LEGEND','100 galibiyete ulaş.','Reach 100 wins.','wins',100,300)
  ) as v(id,title_tr,title_en,description_tr,description_en,metric,target,reward_coins)
), computed as (
  select m.*,
    case m.metric
      when 'matches' then s.matches
      when 'wins' then s.wins
      when 'valid_words' then s.valid_words
      when 'unique_words' then s.unique_words
      when 'best_streak' then s.best_streak
      when 'rating' then s.rating
      else 0
    end::int as progress
  from milestones m cross join stats s
)
select c.id,c.title_tr,c.title_en,c.description_tr,c.description_en,
       c.progress,c.target,c.reward_coins,(c.progress>=c.target),
       exists(select 1 from public.mastery_reward_claims r
              where r.user_id=(select auth.uid()) and r.milestone_id=c.id)
from computed c;
$$;

revoke all on function public.get_mastery_path_v1() from public,anon;
grant execute on function public.get_mastery_path_v1() to authenticated;

create or replace function public.claim_mastery_reward_v1(p_milestone_id text)
returns integer
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_reward integer;
  v_unlocked boolean;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select m.reward_coins,m.unlocked into v_reward,v_unlocked
  from public.get_mastery_path_v1() m
  where m.id=p_milestone_id;

  if v_reward is null then raise exception 'unknown_milestone'; end if;
  if not v_unlocked then raise exception 'milestone_locked'; end if;

  insert into public.mastery_reward_claims(user_id,milestone_id,reward_coins)
  values(v_uid,p_milestone_id,v_reward)
  on conflict (user_id,milestone_id) do nothing;
  if not found then return 0; end if;

  update public.profiles
  set diamonds=coalesce(diamonds,0)+v_reward,updated_at=now()
  where id=v_uid;

  if to_regclass('public.diamond_ledger') is not null then
    insert into public.diamond_ledger(user_id,delta,reason)
    values(v_uid,v_reward,'mastery_reward:' || p_milestone_id);
  end if;

  return v_reward;
end;
$$;

revoke all on function public.claim_mastery_reward_v1(text) from public,anon;
grant execute on function public.claim_mastery_reward_v1(text) to authenticated;

create or replace function public.get_arch_rival_v1()
returns table(
  opponent_id uuid,
  display_name text,
  matches integer,
  wins integer,
  losses integer,
  my_points integer,
  their_points integer,
  last_played_at timestamptz
)
language sql
security definer
set search_path=public,pg_temp
as $$
with duels as (
  select
    case when g.host_id=(select auth.uid()) then g.guest_id else g.host_id end opponent_id,
    (g.winner_id=(select auth.uid())) won,
    (g.winner_id is not null and g.winner_id<>(select auth.uid())) lost,
    case when g.host_id=(select auth.uid()) then g.host_score else g.guest_score end my_points,
    case when g.host_id=(select auth.uid()) then g.guest_score else g.host_score end their_points,
    coalesce(g.finished_at,g.created_at) played_at
  from public.game_rooms g
  where g.status='finished'
    and coalesce(g.is_bot,false)=false
    and g.host_id is not null and g.guest_id is not null
    and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
), agg as (
  select d.opponent_id,count(*)::int matches,
         count(*) filter(where d.won)::int wins,
         count(*) filter(where d.lost)::int losses,
         coalesce(sum(d.my_points),0)::int my_points,
         coalesce(sum(d.their_points),0)::int their_points,
         max(d.played_at) last_played_at
  from duels d
  where d.opponent_id is not null
  group by d.opponent_id
)
select a.opponent_id,p.display_name,a.matches,a.wins,a.losses,a.my_points,a.their_points,a.last_played_at
from agg a join public.profiles p on p.id=a.opponent_id
order by a.matches desc,a.last_played_at desc
limit 1;
$$;

revoke all on function public.get_arch_rival_v1() from public,anon;
grant execute on function public.get_arch_rival_v1() to authenticated;

create or replace function public.get_weekly_pod_v1(p_language text default 'tr')
returns table(
  global_rank integer,
  pod_rank integer,
  user_id uuid,
  display_name text,
  wins integer,
  losses integer,
  rating integer,
  is_me boolean
)
language sql
security definer
set search_path=public,pg_temp
as $$
with weekly as (
  select
    p.id user_id,p.display_name,coalesce(p.rating,1000)::int rating,
    count(g.id) filter(where g.winner_id=p.id)::int wins,
    count(g.id) filter(where g.winner_id is not null and g.winner_id<>p.id)::int losses
  from public.profiles p
  left join public.game_rooms g
    on (g.host_id=p.id or g.guest_id=p.id)
   and g.status='finished'
   and coalesce(g.is_bot,false)=false
   and lower(coalesce(g.language,'tr'))=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end
   and coalesce(g.finished_at,g.created_at)>=date_trunc('week',now())
  group by p.id,p.display_name,p.rating
), ranked as (
  select row_number() over(order by w.wins desc,w.rating desc,w.user_id)::int global_rank,w.*
  from weekly w
), mine as (
  select floor((r.global_rank-1)/20.0)::int pod_no from ranked r where r.user_id=(select auth.uid())
)
select r.global_rank,(((r.global_rank-1)%20)+1)::int,r.user_id,r.display_name,r.wins,r.losses,r.rating,
       r.user_id=(select auth.uid())
from ranked r cross join mine m
where floor((r.global_rank-1)/20.0)::int=m.pod_no
order by r.global_rank;
$$;

revoke all on function public.get_weekly_pod_v1(text) from public,anon;
grant execute on function public.get_weekly_pod_v1(text) to authenticated;

create index if not exists daily_cipher_user_date_idx
  on public.daily_cipher_sessions(user_id,challenge_date desc);
create index if not exists mastery_claim_user_idx
  on public.mastery_reward_claims(user_id,claimed_at desc);
