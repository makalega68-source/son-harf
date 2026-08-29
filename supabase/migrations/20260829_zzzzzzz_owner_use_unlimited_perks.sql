-- Remove legacy admin free-purchase flag from the owner gameplay account.
-- Unlimited purchases are controlled only by owner_game_accounts.

update public.admin_users a
set free_test_purchases=false
from auth.users u
where a.user_id=u.id
  and lower(u.email)=lower('makalega68@gmail.com');
