package com.superexercisebook.jackson.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public class CompoundTagWriter {
    private String pendingFieldName = null;

    public CompoundTag tag = new CompoundTag();

    public boolean isPendingFieldNameNull() {
        return pendingFieldName == null;
    }

    public void addPendingFieldName(String name) {
        pendingFieldName = name;
    }

    public void finishField(Tag<?> value) {
        tag.put(pendingFieldName, value);
        pendingFieldName = null;
    }

    public void cleanFieldName() {
        pendingFieldName = null;
    }
}