begin;

-- Keep progression/title state server-authoritative. The profile table allows
-- owners to update their own row for legitimate preferences, so the existing
-- guard must also absorb direct client writes to fields that have dedicated
-- server-side award/validation paths.
create or replace function public.protect_profile_server_fields()
returns trigger
language plpgsql
set search_path to 'public', 'pg_temp'
as $function$
begin
  if current_user in ('authenticated','anon') then
    new.is_vip := old.is_vip;
    new.diamonds := old.diamonds;
    new.wins := old.wins;
    new.losses := old.losses;
    new.total_matches := old.total_matches;
    new.total_rounds := old.total_rounds;
    new.rounds_won := old.rounds_won;
    new.valid_words := old.valid_words;
    new.best_streak := old.best_streak;
    new.word_storms := old.word_storms;
    new.rating := old.rating;
    new.chat_suspended_until := old.chat_suspended_until;
    new.chat_strike_level := old.chat_strike_level;
    new.account_email := old.account_email;
    new.gender := old.gender;
    new.identity_locked := old.identity_locked;
    new.vip_xp_bonus := old.vip_xp_bonus;
    new.selected_title := old.selected_title;
  end if;
  return new;
end;
$function$;

commit;
