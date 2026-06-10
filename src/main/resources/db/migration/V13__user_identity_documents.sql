CREATE TABLE user_identity_documents (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    side            VARCHAR(16) NOT NULL,
    storage_path    TEXT NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, side),
    CONSTRAINT chk_identity_side CHECK (side IN ('front', 'back', 'selfie'))
);

CREATE INDEX idx_user_identity_documents_user ON user_identity_documents (user_id);
