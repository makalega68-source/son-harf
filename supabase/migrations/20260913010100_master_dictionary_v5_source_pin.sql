-- Correct the generated-output commit used by the 2026.02.25 SCOWL/ESDB release.
-- The upstream wordlist release commit is 7e99edab8e32f9f9ea2b15f249ca8d4d67237410.
-- The matching generated wordlist-diff commit is 71d7dd07676edb60ade43552e10b41314b7e9287.

alter table public.dictionary_sync_jobs
    alter column en_source_url set default 'https://raw.githubusercontent.com/en-wl/wordlist-diff/71d7dd07676edb60ade43552e10b41314b7e9287/en_US.txt';

alter table public.dictionary_sync_jobs
    alter column en_source_commit set default '71d7dd07676edb60ade43552e10b41314b7e9287';
