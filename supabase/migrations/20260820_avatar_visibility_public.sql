-- Allow the profile privacy toggle to persist the visible/public state.
-- The client RPC set_avatar_visibility(false) writes 'public', which was previously
-- rejected by profiles_avatar_visibility_check.

alter table public.profiles
    drop constraint if exists profiles_avatar_visibility_check;

alter table public.profiles
    add constraint profiles_avatar_visibility_check
    check (
        avatar_visibility = any (
            array['hidden'::text, 'public'::text, 'vip'::text, 'match'::text, 'selected'::text]
        )
    );
