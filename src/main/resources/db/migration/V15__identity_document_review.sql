ALTER TABLE user_identity_documents
    ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE user_identity_documents
    ADD CONSTRAINT chk_identity_doc_review_status
        CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED'));

UPDATE user_identity_documents d
SET review_status = 'APPROVED'
FROM seller_verifications v
WHERE v.user_id = d.user_id
  AND v.tier = 'identity_kyc'
  AND v.status = 'APPROVED';

UPDATE user_identity_documents d
SET review_status = 'REJECTED',
    rejection_reason = v.admin_note
FROM seller_verifications v
WHERE v.user_id = d.user_id
  AND v.tier = 'identity_kyc'
  AND v.status = 'REJECTED';
