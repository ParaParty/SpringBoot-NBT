package com.superexercisebook.jackson.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public class CompoundTagWriter extends CompoundTag {
    private String pendingFieldName = null;

    public boolean addPendingFieldName(String name) {
        if (pendingFieldName == null) {
            pendingFieldName = name;
            return true;
        }
        return false;
    }

    public boolean finishField(Tag<?> value) {
        if (pendingFieldName == null) {
            return false;
        }
        this.put(pendingFieldName, value);
        pendingFieldName = null;
        return true;
    }

    public boolean cleanFieldName() {
        if (pendingFieldName == null) {
            return false;
        }
        pendingFieldName = null;
        return true;
    }
}