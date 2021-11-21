package party.para.jackson.nbt;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

/**
 * This class is used to write a CompoundTag from JSON token sequence.
 */
public class CompoundTagWriter {

    /**
     * Field name.
     *
     * Because JSON token sequence is given token by token like {"KEY": "VALUE", "KEY": "VALUE"}
     * But {@link net.querz.nbt.tag.CompoundTag#put(String, Tag)} need two arguments at a time,
     * so we need the hold the field name temporary.
     */
    private String pendingFieldName = null;

    /**
     * Result
     */
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