-- Ticker: a purchase is news only while it is happening. Players who open the app later no
-- longer see it, so the feed keeps purchases of the last 3 minutes (the client polls every
-- minute and shows each one once per device). The stale announcement is switched off.
do $$
declare
  def text;
begin
  def := pg_get_functiondef('public.get_ticker_feed_v1(integer)'::regprocedure);
  if position('interval ''3 days''' in def) = 0 then
    raise notice 'get_ticker_feed_v1 already patched';
    return;
  end if;
  execute replace(def, 'interval ''3 days''', 'interval ''3 minutes''');
end $$;

update public.admin_announcement set enabled = false where singleton and enabled;
