-- Link app_users to persons for member login
ALTER TABLE app_users ADD COLUMN person_id BIGINT REFERENCES persons(id);

-- Slip offer on queue entries
ALTER TABLE queue_entries ADD COLUMN offered_slip_id BIGINT REFERENCES slips(id);
