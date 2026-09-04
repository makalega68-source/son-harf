-- Word Siege authoritative area scoring.
-- Product invariant: each currently owned cube is worth exactly 2 area points.
-- Word/letter points are cumulative and are never clawed back when territory changes owner.

create or replace function private.word_siege_sync_area_scores_v1()
returns trigger
language plpgsql
security invoker
set search_path=pg_catalog,public,private,pg_temp
as $$
declare
  v_one integer := 0;
  v_two integer := 0;
begin
  select
    count(*) filter (where coalesce((cell ->> 'owner')::integer,0)=1)::integer,
    count(*) filter (where coalesce((cell ->> 'owner')::integer,0)=2)::integer
  into v_one,v_two
  from jsonb_array_elements(coalesce(new.board,'[]'::jsonb)) cell;

  new.player_one_area := coalesce(v_one,0);
  new.player_two_area := coalesce(v_two,0);
  new.player_one_area_score := coalesce(v_one,0) * 2;
  new.player_two_area_score := coalesce(v_two,0) * 2;
  return new;
end
$$;

revoke all on function private.word_siege_sync_area_scores_v1() from public,anon,authenticated;

drop trigger if exists trg_word_siege_sync_area_scores_v1 on public.word_siege_games;
create trigger trg_word_siege_sync_area_scores_v1
before insert or update of board on public.word_siege_games
for each row execute function private.word_siege_sync_area_scores_v1();

-- Bring existing games onto the same invariant without changing accumulated word scores.
update public.word_siege_games
set board=board
where board is not null;

-- Preview uses the exact same area-delta engine as the committed move.
create or replace function private.word_siege_preview_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean
)
returns jsonb
language plpgsql
security definer
set search_path=pg_catalog,public,private,pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_owner integer;
  v_rack text;
  v_board jsonb;
  v_before_board jsonb;
  v_indices integer[];
  v_anchor integer;
  v_main_cells integer[];
  v_cross_cells integer[];
  v_words text[] := array[]::text[];
  v_word text;
  v_index integer;
  v_i integer;
  v_cell jsonb;
  v_word_score integer := 0;
  v_base_score integer := 0;
  v_bonus_score integer := 0;
  v_area_score integer := 0;
  v_neutral_captured integer := 0;
  v_opponent_captured integer := 0;
  v_captured integer[] := array[]::integer[];
  v_new_area integer[] := array[]::integer[];
  v_bonus_cells integer[] := array[]::integer[];
  v_letter text;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  perform private.word_siege_prevalidate_move_v2(p_game_id,p_placements,p_horizontal);

  select * into r from public.word_siege_games where id=p_game_id;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id,r.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  v_owner := case when v_uid=r.player_one_id then 1 else 2 end;
  v_rack := case when v_owner=1 then r.player_one_rack else r.player_two_rack end;
  v_board := r.board;
  v_before_board := r.board;

  select array_agg((e ->> 'index')::integer order by ordinality)
  into v_indices
  from jsonb_array_elements(p_placements) with ordinality as x(e,ordinality);
  v_anchor := v_indices[1];

  v_main_cells := private.word_siege_collect_cells_v1(
    v_board,p_placements,v_rack,v_anchor,case when p_horizontal then 1 else 9 end
  );

  if cardinality(v_main_cells)>1 then
    v_word := private.word_siege_word_from_cells_v1(v_board,p_placements,v_rack,v_main_cells);
    v_words := array_append(v_words,v_word);
    v_word_score := v_word_score + private.word_siege_score_word_v1(v_board,p_placements,v_rack,v_main_cells);
    v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board,p_placements,v_rack,v_main_cells);
    foreach v_index in array v_main_cells loop
      v_cell := v_board -> v_index;
      if (v_cell ->> 'letter') is not null
         and coalesce((v_cell ->> 'owner')::integer,0) not in (0,v_owner) then
        v_board := jsonb_set(v_board,array[v_index::text],v_cell || jsonb_build_object('owner',v_owner),false);
        if not (v_index=any(v_captured)) then v_captured:=array_append(v_captured,v_index); end if;
      end if;
    end loop;
  end if;

  foreach v_index in array v_indices loop
    v_cross_cells := private.word_siege_collect_cells_v1(
      v_board,p_placements,v_rack,v_index,case when p_horizontal then 9 else 1 end
    );
    if cardinality(v_cross_cells)>1 then
      v_word := private.word_siege_word_from_cells_v1(v_board,p_placements,v_rack,v_cross_cells);
      v_words := array_append(v_words,v_word);
      v_word_score := v_word_score + private.word_siege_score_word_v1(v_board,p_placements,v_rack,v_cross_cells);
      v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board,p_placements,v_rack,v_cross_cells);
      foreach v_i in array v_cross_cells loop
        v_cell := v_board -> v_i;
        if (v_cell ->> 'letter') is not null
           and coalesce((v_cell ->> 'owner')::integer,0) not in (0,v_owner)
           and not (v_i=any(v_captured)) then
          v_board := jsonb_set(v_board,array[v_i::text],v_cell || jsonb_build_object('owner',v_owner),false);
          v_captured:=array_append(v_captured,v_i);
        end if;
      end loop;
    end if;
  end loop;

  foreach v_index in array v_indices loop
    v_cell := v_board -> v_index;
    v_letter := private.word_siege_letter_at_v1(r.board,p_placements,v_rack,v_index);
    if not coalesce((r.board -> v_index ->> 'bonus_used')::boolean,false)
       and (r.board -> v_index ->> 'bonus') is not null then
      v_bonus_cells:=array_append(v_bonus_cells,v_index);
    end if;
    v_board := jsonb_set(
      v_board,array[v_index::text],
      v_cell || jsonb_build_object('letter',v_letter,'owner',v_owner,'bonus_used',true),false
    );
  end loop;

  select d.neutral_captured,d.opponent_captured,d.area_score
  into v_neutral_captured,v_opponent_captured,v_area_score
  from private.word_siege_area_delta_v1(v_before_board,v_board,v_owner) d;

  v_bonus_score := greatest(0,v_word_score-v_base_score);

  select coalesce(array_agg(i order by i),array[]::integer[])
  into v_new_area
  from generate_series(0,80) i
  where coalesce((r.board -> i ->> 'owner')::integer,0)<>v_owner
    and coalesce((v_board -> i ->> 'owner')::integer,0)=v_owner;

  return jsonb_build_object(
    'valid',true,
    'formed_words',to_jsonb(v_words),
    'base_word_score',v_base_score,
    'word_score',v_word_score,
    'bonus_score',v_bonus_score,
    'area_score',v_area_score,
    'area_cells',v_neutral_captured+v_opponent_captured,
    'neutral_captured',v_neutral_captured,
    'opponent_captured',v_opponent_captured,
    'captured_cells',cardinality(v_captured),
    'bonus_cells',cardinality(v_bonus_cells),
    'preview_cells',to_jsonb(v_new_area),
    'total_score',v_word_score+v_area_score
  );
exception
  when others then
    return jsonb_build_object('valid',false,'reason',sqlerrm);
end
$$;

revoke all on function private.word_siege_preview_move_v1(uuid,jsonb,boolean) from public,anon,authenticated;
