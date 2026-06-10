ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS cv_viewed_at TIMESTAMPTZ;
