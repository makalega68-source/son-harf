create or replace function public.protect_locked_profile_identity()
returns trigger
language plpgsql
set search_path=public
as $$
begin
  if old.identity_locked and (
    new.display_name is distinct from old.display_name or
    new.gender is distinct from old.gender or
    new.account_email is distinct from old.account_email or
    new.identity_locked is distinct from old.identity_locked
  ) then
    raise exception 'identity_locked';
  end if;
  return new;
end $$;

drop trigger if exists trg_protect_locked_profile_identity on public.profiles;
create trigger trg_protect_locked_profile_identity
before update on public.profiles
for each row execute function public.protect_locked_profile_identity();
