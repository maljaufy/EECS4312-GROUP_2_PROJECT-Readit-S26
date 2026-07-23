DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'votes'
          AND column_name = 'value'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'votes'
          AND column_name = 'vote_value'
    ) THEN
        EXECUTE 'UPDATE votes SET vote_value = value WHERE vote_value IS NULL';
        EXECUTE 'ALTER TABLE votes DROP COLUMN value';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'votes'
          AND column_name = 'value'
    ) THEN
        EXECUTE 'ALTER TABLE votes RENAME COLUMN value TO vote_value';
    END IF;
END $$;
