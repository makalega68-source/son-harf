# Admin Match Replay / Audit — staging gate

Issue: #341

This package is intentionally staging-only. `supabase/staging-migrations/20260912_admin_match_replay_v1.sql` must not be copied into `supabase/migrations/` or applied to production until the isolated Supabase branch test passes.

## Security contract

- RPC: `public.admin_match_replay_v1(uuid,text,integer,integer)`
- `SECURITY DEFINER`, empty `search_path`.
- Requires both `auth.uid()` and `public.is_admin()`.
- `PUBLIC`, `anon`, and `service_role` have no execute grant.
- Only `authenticated` can call the RPC, and the function itself rejects non-admin users.
- Payload contains player UUIDs needed for support/audit correlation, but no account email, auth metadata, IP address, device identifier, or other unnecessary PII.

## Replay contract

Supported modes:
- `classic` / Son Harf: paginated `game_words` audit trail plus terminal room metadata.
- `siege` / Kelime Kuşatması: paginated authoritative `word_siege_moves` events plus final 225-cell board snapshot.

For Siege:
- word points are permanent;
- each currently owned cube is worth exactly 2 territory points;
- final territory points are recomputed from `final_board[*].owner`;
- cumulative/legacy area-score columns are not used as final score sources;
- per-move owned-cube counts are deterministically reconstructed from `neutral_captured` and `opponent_captured` deltas before pagination;
- exact placed-tile indices are available from `placed_tiles`;
- exact historical captured-cell indices are **not** persisted by the current move schema and therefore are explicitly reported as unavailable instead of being inferred or fabricated.

The last limitation is important for support/audit accuracy. If exact historical capture-cell visualization becomes a product requirement, add a separately reviewed server-authoritative persistence field for future matches; do not backfill guessed indices for old matches.

## Payload bound

`p_limit` is clamped to `1..100`; `p_offset` is non-negative. The response includes total/returned/has_more metadata. The final board is fixed at 225 cells, so move history is the only unbounded component and is paginated.

## Staging validation

1. Create an isolated Supabase development/staging branch only after cost approval.
2. Apply `supabase/staging-migrations/20260912_admin_match_replay_v1.sql` to that branch.
3. Run `supabase/staging-tests/admin_match_replay_v1.sql`.
4. Confirm the test transaction rolls back and leaves no fixture users/matches.
5. Confirm ACLs: anon=false, service_role=false, authenticated=true.
6. Exercise a real staging admin session through PostgREST/RPC and verify a non-admin authenticated session receives `admin_required`.
7. Verify a staged completed Siege match against the game UI score: permanent word points + current board ownership × 2.
8. Only then promote the reviewed SQL into a timestamped production migration, run normal CI, back up the live function definition/ACL state, and apply production DDL.

## Rollback

Before production promotion, capture whether the RPC already exists and its `pg_get_functiondef`/ACLs. For the first deployment, rollback is `drop function public.admin_match_replay_v1(uuid,text,integer,integer);`. No gameplay/economy table mutation is part of this feature.
