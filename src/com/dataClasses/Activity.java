package com.dataClasses;

import java.sql.Timestamp;

public record Activity(
        String user,
        String label,
        Timestamp ts,
        String file,
        String comment
)
        implements Message
{
    public Activity setUser(String user){
        return new Activity(user,label(),ts(),file(),comment());
    }

    public Activity setTs(Timestamp timestamp) {
        return new Activity(user(),label(),timestamp,file(),comment());
    }

    public Activity setComment(String file) {
        return new Activity(user(),label(),ts(),file,comment());
    }
}
