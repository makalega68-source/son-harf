-- Unified Pro booster-state FK lookup hardening.
create index if not exists premier_booster_state_user_idx
  on public.premier_booster_state(user_id);
