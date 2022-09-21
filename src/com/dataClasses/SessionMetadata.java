package com.dataClasses;

import java.sql.Timestamp;

public record SessionMetadata(
        boolean isNew,
        Timestamp rsaTimestamp
)
        implements Message
{}
