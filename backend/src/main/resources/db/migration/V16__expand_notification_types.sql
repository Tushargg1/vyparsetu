-- Keep the persisted MySQL ENUM aligned with Enums.NotificationType.
-- Missing values caused order modification and operational alerts to fail at runtime.
ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'LOW_STOCK',
        'OFFER',
        'PAYMENT_REMINDER',
        'DELIVERY_UPDATE',
        'AI_RECOMMENDATION',
        'ORDER_UPDATE',
        'SYSTEM',
        'NEW_RETAILER',
        'ORDER_MODIFIED',
        'LARGE_ORDER',
        'CREDIT_EXCEEDED',
        'AI_EXTRACTION_FAILED',
        'REGISTRATION_APPROVED'
    ) NOT NULL;