create or replace function private.assign_training_bot_name_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
declare
  v_names text[]:=array[
    'Elif','Zeynep','Defne','Ece','Duru','İrem','Selin','Melis',
    'Mert','Emir','Kerem','Arda','Eren','Can','Kaan','Berk'
  ];
  v_previous text;
  v_candidate text;
begin
  if not coalesce(new.is_bot,false) then return new; end if;
  select g.bot_name into v_previous
  from public.game_rooms g
  where g.host_id=new.host_id and coalesce(g.is_bot,false)
  order by g.created_at desc
  limit 1;
  loop
    v_candidate:=v_names[1+floor(random()*array_length(v_names,1))::int];
    exit when v_previous is null or v_candidate<>v_previous;
  end loop;
  new.bot_name:=v_candidate;
  return new;
end
$$;
revoke all on function private.assign_training_bot_name_v1() from public,anon,authenticated;
