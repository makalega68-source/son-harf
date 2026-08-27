-- Align Günün Şifresi guesses with the authoritative Son Harf dictionary.

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
  if not public.sonharf_word_allowed(v_lang, public.normalize_game_word(v_lang,p_guess)) then
    raise exception 'cipher_word_not_in_dictionary';
  end if;

  insert into public.daily_cipher_sessions(user_id,challenge_date,language)
  values(v_uid,current_date,v_lang)
  on conflict on constraint daily_cipher_sessions_pkey do nothing;

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

  update public.daily_cipher_sessions as s
  set attempts=v_attempts,guesses=v_guesses,feedbacks=v_feedbacks,won=v_won,
      finished=v_finished,reward_coins=v_reward,updated_at=now()
  where s.user_id=v_uid and s.challenge_date=current_date and s.language=v_lang;

  return query
    select current_date,v_lang,v_attempts,6,v_guesses,v_feedbacks,v_won,v_finished,
           case when v_finished then v_answer_display else null end,v_reward;
end;
$$;

revoke all on function public.submit_daily_cipher_guess_v1(text,text) from public,anon;
grant execute on function public.submit_daily_cipher_guess_v1(text,text) to authenticated;
