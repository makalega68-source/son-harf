-- Club challenge records are available only through membership-checked RPCs.
-- Keep an explicit deny policy as defense in depth for the exposed public schema.

drop policy if exists club_challenges_no_direct_access on public.club_challenges;
create policy club_challenges_no_direct_access
on public.club_challenges
for all to authenticated
using (false)
with check (false);
