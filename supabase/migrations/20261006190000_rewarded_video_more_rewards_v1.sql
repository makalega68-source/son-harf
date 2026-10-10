-- More rewarded-video rewards: Gold videos 3 -> 6 a day, a theme 2 times a week (never daily),
-- keyboard 3 a week, each game's hint pack 3 a day.
do $$
declare def text;
begin
  def := pg_get_functiondef('public.grant_store_ad_internal_v1(text,text,text)'::regprocedure);
  if position('then 3 else 1 end' in def) > 0 then
    execute replace(def, 'then 3 else 1 end', 'then 6 else 1 end');
  end if;

  def := pg_get_functiondef('public.get_store_reward_status_v1()'::regprocedure);
  if position('''coin_ads_limit'',3' in def) > 0 then
    execute replace(def, '''coin_ads_limit'',3', '''coin_ads_limit'',6');
  end if;

  def := pg_get_functiondef('public.reward_ad_rule_v1(text)'::regprocedure);
  def := replace(def, '(''keyboard_day'',   2, 7, 0, 24)', '(''keyboard_day'',   3, 7, 0, 24)');
  def := replace(def, '(''theme_day'',      1, 7, 0, 24)', '(''theme_day'',      2, 7, 0, 24)');
  def := replace(def, '(''hints_son_harf'', 2, 1, 2, 168)', '(''hints_son_harf'', 3, 1, 2, 168)');
  def := replace(def, '(''hints_siege'',    2, 1, 2, 168)', '(''hints_siege'',    3, 1, 2, 168)');
  def := replace(def, '(''hints_workshop'', 2, 1, 2, 168)', '(''hints_workshop'', 3, 1, 2, 168)');
  execute def;
end $$;
