-- The budgie's display name is Çiçi (product and skin ids stay 'mascot_cikcik' / 'cikcik').
do $$
declare d text := pg_get_functiondef('public.ensure_mascot_progress_v1(text)'::regprocedure);
begin
  if position('''Cikcik''' in d) > 0 then
    execute replace(d, 'when ''mascot_cikcik'' then ''Cikcik''', 'when ''mascot_cikcik'' then ''Çiçi''');
  end if;
end $$;

-- Rows still carrying the old default name follow the rename; custom names are untouched.
update public.user_mascot_progress set pet_name = 'Çiçi'
where mascot_id = 'mascot_cikcik' and pet_name = 'Cikcik';
