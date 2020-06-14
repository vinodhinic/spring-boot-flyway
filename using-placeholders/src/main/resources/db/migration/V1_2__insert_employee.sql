DO $$
DECLARE
client VARCHAR(50) := '${client}';
BEGIN
    IF LOWER(client) like '%local%' THEN
        INSERT into employee values(1,'vino');
        RAISE NOTICE 'Inserting record for client : %', client;
    ELSE
        RAISE NOTICE 'Insert skipped. Condition not met for %', client;
   END IF;
END $$ LANGUAGE plpgsql;
