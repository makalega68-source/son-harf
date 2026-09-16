-- Frames retired in 20260916074500 / 20260916075000 were bought with diamonds.
-- Give every affected owner 250 diamonds per frame they held (one time only),
-- tagged with a stable reason so a re-run of this migration is idempotent.

do $$
declare
    v_per_frame integer := 250;
    v_reason text := 'frame_retirement_compensation_v1';
begin
    -- Award any players who own a retired profile-frame shop item.
    with owned_frames as (
        select ui.user_id, count(*) as frame_count
        from public.user_inventory ui
        join public.shop_items si on si.id = ui.item_id
        where si.kind = 'profile_frame'
        group by ui.user_id
    ),
    payable as (
        select o.user_id, o.frame_count * v_per_frame as delta
        from owned_frames o
        where not exists (
            select 1 from public.diamond_ledger dl
            where dl.user_id = o.user_id and dl.reason = v_reason
        )
    ),
    inserted as (
        insert into public.diamond_ledger(user_id, delta, reason)
        select p.user_id, p.delta, v_reason
        from payable p
        returning user_id, delta
    )
    update public.profiles pr
    set diamonds = coalesce(pr.diamonds, 0) + i.delta
    from inserted i
    where pr.id = i.user_id;
end $$;
