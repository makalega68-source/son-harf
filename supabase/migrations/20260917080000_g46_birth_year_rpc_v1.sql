-- G4.6 — Doğum yılı yakalama RPC'si.
-- Kolon 20260917060000_g46_minor_gate_v1.sql'da eklendi (profiles.birth_year).
-- Bu RPC istemcinin kendi kaydını güncellemesine izin verir.

set search_path = public, pg_temp;

create or replace function public.update_my_birth_year(p_birth_year int)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_year int;
begin
    if v_user is null then raise exception 'not_authenticated'; end if;
    -- Boş verilirse temizle (kullanıcı geri çekmek isterse).
    if p_birth_year is null then
        update public.profiles set birth_year = null where id = v_user;
        return;
    end if;
    -- Yıl aralığı sanity: 1900..bu yıl.
    v_year := p_birth_year;
    if v_year < 1900 or
       v_year > extract(year from (now() at time zone 'Europe/Istanbul'))::int then
        raise exception 'invalid_birth_year';
    end if;
    update public.profiles set birth_year = v_year where id = v_user;
end;
$$;

grant execute on function public.update_my_birth_year(int) to authenticated;

select pg_notify('pgrst', 'reload schema');
