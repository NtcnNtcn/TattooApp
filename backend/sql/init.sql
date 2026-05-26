-- ===========================================================================
-- TattooStudio PostgreSQL Init Script
-- Triggers, stored procedures and performance indexes
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- EXTENSION
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- for ILIKE fast search on tags

-- ---------------------------------------------------------------------------
-- INDEXES
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tattoo_works_status   ON tattoo_works (status);
CREATE INDEX IF NOT EXISTS idx_tattoo_works_master   ON tattoo_works (master_id);
CREATE INDEX IF NOT EXISTS idx_tattoo_works_created  ON tattoo_works (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tattoo_works_likes    ON tattoo_works (like_count DESC);
CREATE INDEX IF NOT EXISTS idx_likes_work_id         ON likes (work_id);
CREATE INDEX IF NOT EXISTS idx_favorites_user        ON favorites (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_master  ON subscriptions (master_id);
CREATE INDEX IF NOT EXISTS idx_tags_name_trgm        ON tags USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_notifications_user    ON notifications (user_id, is_read);

-- ---------------------------------------------------------------------------
-- TRIGGER: log every status change into work_reviews
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_work_status_change()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO work_reviews (work_id, reviewer_id, old_status, new_status, created_at)
        VALUES (NEW.id, NULL, OLD.status, NEW.status, NOW());
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_work_status_change ON tattoo_works;
CREATE TRIGGER trg_work_status_change
    AFTER UPDATE OF status ON tattoo_works
    FOR EACH ROW EXECUTE FUNCTION trg_fn_work_status_change();

-- ---------------------------------------------------------------------------
-- TRIGGER: maintain like_count on tattoo_works for fast sorting
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_like_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE tattoo_works SET like_count = like_count + 1 WHERE id = NEW.work_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE tattoo_works SET like_count = GREATEST(like_count - 1, 0) WHERE id = OLD.work_id;
    END IF;
    RETURN NULL;  -- AFTER trigger, return value ignored
END;
$$;

DROP TRIGGER IF EXISTS trg_like_count ON likes;
CREATE TRIGGER trg_like_count
    AFTER INSERT OR DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION trg_fn_like_count();

-- ---------------------------------------------------------------------------
-- STORED PROCEDURE: approve_work(work_id, admin_id)
-- Changes status → approved, records reviewer, creates notification
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE approve_work(
    p_work_id  INT,
    p_admin_id INT
)
LANGUAGE plpgsql AS $$
DECLARE
    v_master_id INT;
BEGIN
    -- Fetch master
    SELECT master_id INTO v_master_id FROM tattoo_works WHERE id = p_work_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Work % not found', p_work_id;
    END IF;

    -- Update status
    UPDATE tattoo_works
    SET status = 'approved', updated_at = NOW()
    WHERE id = p_work_id;

    -- Update reviewer in the review log inserted by the trigger
    UPDATE work_reviews
    SET reviewer_id = p_admin_id
    WHERE id = (
        SELECT id FROM work_reviews 
        WHERE work_id = p_work_id AND new_status = 'approved'
        ORDER BY created_at DESC
        LIMIT 1
    );

    -- Create in-app notification for the master
    INSERT INTO notifications (user_id, type, title, message, is_read, created_at)
    VALUES (
        v_master_id,
        'work_approved',
        'Работа одобрена ✅',
        FORMAT('Ваша работа #%s была одобрена и опубликована.', p_work_id),
        false,
        NOW()
    );

    COMMIT;
END;
$$;

-- ---------------------------------------------------------------------------
-- STORED PROCEDURE: reject_work(work_id, admin_id, reason)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE reject_work(
    p_work_id  INT,
    p_admin_id INT,
    p_reason   TEXT DEFAULT ''
)
LANGUAGE plpgsql AS $$
DECLARE
    v_master_id INT;
BEGIN
    SELECT master_id INTO v_master_id FROM tattoo_works WHERE id = p_work_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Work % not found', p_work_id;
    END IF;

    UPDATE tattoo_works
    SET status = 'rejected', rejection_reason = p_reason, updated_at = NOW()
    WHERE id = p_work_id;

    UPDATE work_reviews
    SET reviewer_id = p_admin_id, comment = p_reason
    WHERE id = (
        SELECT id FROM work_reviews 
        WHERE work_id = p_work_id AND new_status = 'rejected'
        ORDER BY created_at DESC
        LIMIT 1
    );

    INSERT INTO notifications (user_id, type, title, message, is_read, created_at)
    VALUES (
        v_master_id,
        'work_rejected',
        'Работа отклонена ❌',
        FORMAT('Ваша работа #%s была отклонена. Причина: %s', p_work_id, COALESCE(NULLIF(p_reason,''),'не указана')),
        false,
        NOW()
    );

    COMMIT;
END;
$$;

-- ---------------------------------------------------------------------------
-- FUNCTION: generate_monthly_report(start_date, end_date)
-- Returns a refcursor with aggregated statistics (call from application layer)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION generate_monthly_report(
    p_start DATE,
    p_end   DATE
) RETURNS TABLE (
    metric TEXT,
    value  BIGINT
) LANGUAGE sql STABLE AS $$
    SELECT 'total_works_uploaded'::TEXT,  COUNT(*)::BIGINT
      FROM tattoo_works WHERE created_at::date BETWEEN p_start AND p_end
    UNION ALL
    SELECT 'total_approved',  COUNT(*)::BIGINT
      FROM tattoo_works WHERE status = 'approved' AND created_at::date BETWEEN p_start AND p_end
    UNION ALL
    SELECT 'total_rejected',  COUNT(*)::BIGINT
      FROM tattoo_works WHERE status = 'rejected' AND created_at::date BETWEEN p_start AND p_end
    UNION ALL
    SELECT 'total_pending',   COUNT(*)::BIGINT
      FROM tattoo_works WHERE status = 'pending'  AND created_at::date BETWEEN p_start AND p_end
    UNION ALL
    SELECT 'total_likes',     COUNT(*)::BIGINT
      FROM likes WHERE created_at::date BETWEEN p_start AND p_end
    UNION ALL
    SELECT 'new_users',       COUNT(*)::BIGINT
      FROM users WHERE created_at::date BETWEEN p_start AND p_end;
$$;

-- ---------------------------------------------------------------------------
-- USEFUL VIEW: top masters by likes (current month)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_top_masters AS
SELECT
    u.id              AS master_id,
    u.full_name,
    u.avatar_url,
    COALESCE(SUM(tw.like_count), 0) AS total_likes,
    COUNT(tw.id) FILTER (WHERE tw.status = 'approved') AS approved_works
FROM users u
JOIN roles r ON r.id = u.role_id AND r.level >= 2
LEFT JOIN tattoo_works tw ON tw.master_id = u.id
GROUP BY u.id, u.full_name, u.avatar_url
ORDER BY total_likes DESC;
