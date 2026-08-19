-- Prevent repeated clicks/reports by the same reporter from creating repeated penalties.
-- A target receives a new chat strike only when the number of DISTINCT reporters
-- reaches a new multiple of three: 3, 6, 9, ...

create or replace function public.report_player(p_reported_id uuid, p_reason text, p_room_id uuid default null, p_message_id bigint default null)
returns integer
language plpgsql security definer set search_path = public as $$
declare
  report_count int;
  new_level int;
  suspend_for interval;
  was_existing boolean;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_reported_id = auth.uid() then raise exception 'cannot_report_self'; end if;
  if char_length(trim(p_reason)) < 3 then raise exception 'invalid_reason'; end if;

  select exists(
    select 1 from public.player_reports
    where reporter_id=auth.uid() and reported_id=p_reported_id
  ) into was_existing;

  insert into public.player_reports(reporter_id, reported_id, room_id, message_id, reason)
  values (auth.uid(), p_reported_id, p_room_id, p_message_id, trim(p_reason))
  on conflict (reporter_id, reported_id) do update
    set reason = excluded.reason,
        room_id = coalesce(excluded.room_id, public.player_reports.room_id),
        message_id = coalesce(excluded.message_id, public.player_reports.message_id);

  select count(distinct reporter_id) into report_count
  from public.player_reports where reported_id=p_reported_id;

  -- Existing reporter never creates a new strike. A new reporter creates a strike
  -- only at 3, 6, 9... distinct reporters.
  if not was_existing and report_count >= 3 and report_count % 3 = 0 then
    select chat_strike_level + 1 into new_level
    from public.profiles where id=p_reported_id for update;

    suspend_for := case
      when new_level <= 1 then interval '24 hours'
      when new_level = 2 then interval '3 days'
      else interval '7 days'
    end;

    update public.profiles
       set chat_strike_level=new_level,
           chat_suspended_until=greatest(coalesce(chat_suspended_until, now()), now()) + suspend_for
     where id=p_reported_id;
  end if;

  return report_count;
end;
$$;

grant execute on function public.report_player(uuid,text,uuid,bigint) to authenticated;
