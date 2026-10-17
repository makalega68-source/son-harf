-- Ayaz Kağan (Ayazkagann) joins the admins at the owner's request.
insert into public.admin_users(user_id, role) values ('2b316de9-6a8d-4d9f-95a5-e176ee252d4b','admin')
on conflict (user_id) do update set role='admin';
