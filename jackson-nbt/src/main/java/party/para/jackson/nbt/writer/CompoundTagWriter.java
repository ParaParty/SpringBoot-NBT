package party.para.jackson.nbt.writer;


import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import party.para.jackson.nbt.entity.MutableCompoundTagImpl;

/**
 * This class is used to write a CompoundTag from JSON token sequence.
 */
public class CompoundTagWriter {

    /**
     * Field name.
     * <p>
     * Because JSON token sequence is given token by token like {"KEY": "VALUE", "KEY": "VALUE"}
     * But {@link CompoundBinaryTag#put(String, BinaryTag)} need two arguments at a time,
     * so we need the hold the field name temporary.
     */
    private String pendingFieldName = null;

    /**
     * Result
     */
    public CompoundBinaryTag tag = new MutableCompoundTagImpl();

    public boolean isPendingFieldNameNull() {
        return pendingFieldName == null;
    }

    public void addPendingFieldName(String name) {
        pendingFieldName = name;
    }

    public void finishField(BinaryTag value) {
        tag = tag.put(pendingFieldName, value);
        pendingFieldName = null;
    }

    public void cleanFieldName() {
        pendingFieldName = null;
    }
}