package party.para.jackson.nbt;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

/**
 * NBT Generator.
 *
 * Serializer a NBT data by given JSON sequence.
 */
public class NbtGenerator extends GeneratorBase {
    private final OutputStream writer;

    private Tag<?> rootTag = null;

    private final Stack<Object> tagStack = new Stack<>();

    public NbtGenerator(IOContext ctxt, int stdFeat, ObjectCodec objectCodec, OutputStream out) {
        super(stdFeat, objectCodec);
        this.writer = out;
    }

    private boolean checkTagStackIsNonnullAndIsPeekSpecifyType(Class clazz) {
        return tagStack.size() != 0 && (clazz.isInstance(tagStack.peek()));
    }

    @Override
    public void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        ListTag<?> t = ListTag.createUnchecked(EndTag.class);
        _writeHelper(t);
        tagStack.push(t);
    }

    @Override
    public void writeEndArray() throws IOException {
        if (!checkTagStackIsNonnullAndIsPeekSpecifyType(ListTag.class)) {
            _reportError("Current context not Array but Object");
        }
        tagStack.pop();
    }

    @Override
    public void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        CompoundTagWriter t = new CompoundTagWriter();
        _writeHelper(t.tag);
        tagStack.push(t);
    }

    @Override
    public void writeEndObject() throws IOException {
        if (!checkTagStackIsNonnullAndIsPeekSpecifyType(CompoundTagWriter.class)) {
            _reportError("Current context not Object but Array");
        }
        tagStack.pop();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        if (!checkTagStackIsNonnullAndIsPeekSpecifyType(CompoundTagWriter.class)) {
            _reportError("Key value pair not allow here.");
        }

        CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
        if (s.isPendingFieldNameNull()) {
            s.addPendingFieldName(name);
        } else {
            _reportError("Field name is already exists.");
        }
    }

    private void _writeHelper(Tag<?> v) throws IOException {
        if (checkTagStackIsNonnullAndIsPeekSpecifyType(CompoundTagWriter.class)) {
            CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
            if (s.isPendingFieldNameNull()) {
                _reportError("No field name exists.");
            } else {
                s.finishField(v);
            }
        } else if (checkTagStackIsNonnullAndIsPeekSpecifyType(ListTag.class)) {
            //noinspection unchecked
            ListTag<Tag<?>> s = (ListTag<Tag<?>>) tagStack.peek();
            s.add(v);
        } else {
            rootTag = v;
        }
    }

    @Override
    public void writeString(String text) throws IOException {
        _writeHelper(new StringTag(text));
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        String str = new String(text, offset, len);
        writeString(str);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws IOException {
        _reportError("writeRawUTF8String not supported");
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportError("writeUTF8String not supported");
    }

    @Override
    public void writeRaw(String text) throws IOException {
        _reportError("writeRaw not supported");
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _reportError("writeRaw not supported");
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _reportError("writeRaw not supported");
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _reportError("writeRaw not supported");
    }

    @Override
    public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
        byte[] newData = new byte[len];
        for (int i = 0; i < len; i++) {
            newData[i] = data[i + offset];
        }
        ByteArrayTag result = new ByteArrayTag(newData);
        _writeHelper(result);
    }

    @Override
    public void writeNumber(int v) throws IOException {
        _writeHelper(new IntTag(v));
    }

    @Override
    public void writeNumber(long v) throws IOException {
        _writeHelper(new LongTag(v));
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        _writeHelper(new LongTag(v.longValue()));
    }

    @Override
    public void writeNumber(double v) throws IOException {
        _writeHelper(new DoubleTag(v));
    }

    @Override
    public void writeNumber(float v) throws IOException {
        _writeHelper(new FloatTag(v));
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        _writeHelper(new DoubleTag(v.doubleValue()));
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        _writeHelper(new ByteTag(state ? (byte) 1 : (byte) 0));

    }

    @Override
    public void writeNull() throws IOException {
        if (checkTagStackIsNonnullAndIsPeekSpecifyType(CompoundTagWriter.class)) {
            CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
            if (s.isPendingFieldNameNull()) {
                _reportError("No field name exists.");
            } else {
                s.cleanFieldName();
            }
        } else if (checkTagStackIsNonnullAndIsPeekSpecifyType(ListTag.class)) {
            _reportError("Null is not in list.");
        } else {
            rootTag = null;
        }
    }

    private boolean isFlushed = false;

    @Override
    public void flush() throws IOException {
        if (isFlushed) return;
        isFlushed = true;

        if (!tagStack.empty()) {
            _reportError("Can not flush");
        }
        else if (rootTag != null) {
            new NBTSerializer(false).toStream(new NamedTag(null, rootTag), writer);
        }
    }

    @Override
    protected void _releaseBuffers() {

    }

    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {

    }
}
