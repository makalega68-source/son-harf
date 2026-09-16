-- Operational health view for the master dictionary sync worker.
-- Restricted to service_role so ops can spot a stalled TDK feed without
-- exposing the sync job internals to end users.

create or replace function public.dictionary_sync_health_v1()
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select jsonb_build_object(
        'last_active_at', (
            select max(completed_at) from dictionary_sync_jobs where status = 'active'
        ),
        'days_since_active', (
            select extract(day from now() - max(completed_at))::int
            from dictionary_sync_jobs where status = 'active'
        ),
        'last_error', (
            select error_message from dictionary_sync_jobs
            where status = 'error' order by created_at desc limit 1
        ),
        'last_error_at', (
            select completed_at from dictionary_sync_jobs
            where status = 'error' order by created_at desc limit 1
        ),
        'tr_words', (
            select count(*) from dictionary_words where language = 'tr' and active
        ),
        'en_words', (
            select count(*) from dictionary_words where language = 'en' and active
        )
    );
$$;

revoke all on function public.dictionary_sync_health_v1() from public, anon, authenticated;
grant execute on function public.dictionary_sync_health_v1() to service_role;
