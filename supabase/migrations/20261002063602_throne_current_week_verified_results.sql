do $$
declare r record; w timestamptz:=date_trunc('week',now() at time zone 'Europe/Istanbul') at time zone 'Europe/Istanbul';
begin
 for r in
 select id,host_id a,guest_id b,winner_id winner,finished_at from public.game_rooms
 where status='finished' and stats_applied and valid_word_count>=2 and finished_at>=w order by finished_at
 loop
 perform private.throne_award_xp(r.a,'match',r.id::text,'last_letter',35+case when r.a=r.winner then 85 else 0 end,1,case when r.a=r.winner then 1 else 0 end);
 perform private.throne_award_xp(r.b,'match',r.id::text,'last_letter',35+case when r.b=r.winner then 85 else 0 end,1,case when r.b=r.winner then 1 else 0 end);
 update public.throne_xp_events set earned_at=r.finished_at where source='match' and source_id=r.id::text and game='last_letter';
 end loop;
 for r in select id,player_one_id a,player_two_id b,winner_id winner,finished_at from public.word_siege_games
 where status='finished' and result_applied and move_count>=2 and player_one_word_score+player_two_word_score>0 and finished_at>=w order by finished_at
 loop
 perform private.throne_award_xp(r.a,'match',r.id::text,'siege',35+case when r.a=r.winner then 85 else 0 end,1,case when r.a=r.winner then 1 else 0 end);
 perform private.throne_award_xp(r.b,'match',r.id::text,'siege',35+case when r.b=r.winner then 85 else 0 end,1,case when r.b=r.winner then 1 else 0 end);
 update public.throne_xp_events set earned_at=r.finished_at where source='match' and source_id=r.id::text and game='siege';
 end loop;
 for r in select * from public.atelier_daily_runs where finished_at>=w order by finished_at
 loop
 perform private.throne_award_xp(r.user_id,'daily_atelier',r.day||':'||r.language||':'||r.duration_seconds,'atelier',least(120,35+r.score/greatest(20,r.duration_seconds/60*20)),1,0,r.tasks);
 update public.throne_xp_events set earned_at=r.finished_at where source='daily_atelier' and source_id=r.day||':'||r.language||':'||r.duration_seconds and user_id=r.user_id;
 end loop;
end $$;
