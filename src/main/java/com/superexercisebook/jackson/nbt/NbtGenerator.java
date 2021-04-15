package com.superexercisebook.jackson.nbt;

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

public class NbtGenerator extends GeneratorBase {
    private OutputStream writer;

    Tag<?> rootTag = null;

    Stack<Tag<?>> tagStack = new Stack<>();

    public NbtGenerator(IOContext ctxt, int stdFeat, ObjectCodec objectCodec, OutputStream out) {
        super(stdFeat, objectCodec);
        this.writer = out;
    }

    /**
     * Method for writing starting marker of a Array value
     * (for JSON this is character '['; plus possible white space decoration
     * if pretty-printing is enabled).
     * <p>
     * Array values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     */
    @Override
    public void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        ListTag<?> t = new ListTag<>(Tag.class);
        _writeHelper(t);
        tagStack.push(t);
    }

    /**
     * Method for writing closing marker of a JSON Array value
     * (character ']'; plus possible white space decoration
     * if pretty-printing is enabled).
     * <p>
     * Marker can be written if the innermost structured type
     * is Array.
     */
    @Override
    public void writeEndArray() throws IOException {
        if (tagStack.size() == 0 || !(tagStack.peek() instanceof ListTag)) {
            _reportError("Current context not Array but Object");
        }
        tagStack.pop();
    }

    /**
     * Method for writing starting marker of an Object value
     * (character '{'; plus possible white space decoration
     * if pretty-printing is enabled).
     * <p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     */
    @Override
    public void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        CompoundTagWriter t = new CompoundTagWriter();
        _writeHelper(t);
        tagStack.push(t);
    }

    /**
     * Method for writing closing marker of an Object value
     * (character '}'; plus possible white space decoration
     * if pretty-printing is enabled).
     * <p>
     * Marker can be written if the innermost structured type
     * is Object, and the last written event was either a
     * complete value, or START-OBJECT marker (see JSON specification
     * for more details).
     */
    @Override
    public void writeEndObject() throws IOException {
        if (tagStack.size() == 0 || !(tagStack.peek() instanceof CompoundTagWriter)) {
            _reportError("Current context not Object but Array");
        }
        tagStack.pop();
    }

    /**
     * Method for writing a field name (JSON String surrounded by
     * double quotes: syntactically identical to a JSON String value),
     * possibly decorated by white space if pretty-printing is enabled.
     * <p>
     * Field names can only be written in Object context (check out
     * JSON specification for details), when field name is expected
     * (field names alternate with values).
     *
     * @param name
     */
    @Override
    public void writeFieldName(String name) throws IOException {
        if (tagStack.size() == 0 || !(tagStack.peek() instanceof CompoundTagWriter)) {
            _reportError("Key value pair not allow here.");
        }

        CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
        if (!s.addPendingFieldName(name)) {
            _reportError("Field name is already exists.");
        };
    }

    /**
     * writerHelper
     *
     * @param v
     * @throws IOException
     */
    private void _writeHelper(Tag<?> v) throws IOException {
        if (tagStack.size() == 0) {
            rootTag = v;
        } else if (tagStack.peek() instanceof CompoundTagWriter) {
            CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
            if (!s.finishField(v)) {
                _reportError("No field name exists.");
            };
        }
        else if (tagStack.peek() instanceof ListTag) {
            ListTag<Tag<?>> s = (ListTag<Tag<?>>) tagStack.peek();
            s.add(v);
        }
        else rootTag = v;
    }

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param text
     */
    @Override
    public void writeString(String text) throws IOException {
        _writeHelper(new StringTag(text));
    }

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param text
     * @param offset
     * @param len
     */
    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        String str = new String(text, offset, len);
        writeString(str);
    }

    /**
     * Method similar to {@link #writeString(String)} but that takes as
     * its input a UTF-8 encoded String that is to be output as-is, without additional
     * escaping (type of which depends on data format; backslashes for JSON).
     * However, quoting that data format requires (like double-quotes for JSON) will be added
     * around the value if and as necessary.
     * <p>
     * Note that some backends may choose not to support this method: for
     * example, if underlying destination is a {@link Writer}
     * using this method would require UTF-8 decoding.
     * If so, implementation may instead choose to throw a
     * {@link UnsupportedOperationException} due to ineffectiveness
     * of having to decode input.
     *
     * @param text
     * @param offset
     * @param len
     */
    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method similar to {@link #writeString(String)} but that takes as its input
     * a UTF-8 encoded String which has <b>not</b> been escaped using whatever
     * escaping scheme data format requires (for JSON that is backslash-escaping
     * for control characters and double-quotes; for other formats something else).
     * This means that textual JSON backends need to check if value needs
     * JSON escaping, but otherwise can just be copied as is to output.
     * Also, quoting that data format requires (like double-quotes for JSON) will be added
     * around the value if and as necessary.
     * <p>
     * Note that some backends may choose not to support this method: for
     * example, if underlying destination is a {@link Writer}
     * using this method would require UTF-8 decoding.
     * In this case
     * generator implementation may instead choose to throw a
     * {@link UnsupportedOperationException} due to ineffectiveness
     * of having to decode input.
     *
     * @param text
     * @param offset
     * @param length
     */
    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     * <p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     *
     * @param text
     */
    @Override
    public void writeRaw(String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     * <p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     *
     * @param text
     * @param offset
     * @param len
     */
    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     * <p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     *
     * @param text
     * @param offset
     * @param len
     */
    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     * <p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     *
     * @param c
     */
    @Override
    public void writeRaw(char c) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that will output given chunk of binary data as base64
     * encoded, as a complete String value (surrounded by double quotes).
     * This method defaults
     * <p>
     * Note: because JSON Strings can not contain unescaped linefeeds,
     * if linefeeds are included (as per last argument), they must be
     * escaped. This adds overhead for decoding without improving
     * readability.
     * Alternatively if linefeeds are not included,
     * resulting String value may violate the requirement of base64
     * RFC which mandates line-length of 76 characters and use of
     * linefeeds. However, all {@link JsonParser} implementations
     * are required to accept such "long line base64"; as do
     * typical production-level base64 decoders.
     *
     * @param bv     Base64 variant to use: defines details such as
     *               whether padding is used (and if so, using which character);
     *               what is the maximum line length before adding linefeed,
     *               and also the underlying alphabet to use.
     * @param data
     * @param offset
     * @param len
     */
    @Override
    public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
        byte[] newData = new byte[len];
        for (int i = 0; i < len; i++) {
            newData[i] = data[i + offset];
        }
        ByteArrayTag result = new ByteArrayTag(newData);
        _writeHelper(result);
    }

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(int v) throws IOException {
        _writeHelper(new IntTag(v));
    }

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(long v) throws IOException {
        _writeHelper(new LongTag(v));
    }

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(BigInteger v) throws IOException {
        _writeHelper(new LongTag(v.longValue()));
    }

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(double v) throws IOException {
        _writeHelper(new DoubleTag(v));
    }

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(float v) throws IOException {
        _writeHelper(new FloatTag(v));
    }

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        _writeHelper(new DoubleTag(v.doubleValue()));
    }

    /**
     * Write method that can be used for custom numeric types that can
     * not be (easily?) converted to "standard" Java number types.
     * Because numbers are not surrounded by double quotes, regular
     * {@link #writeString} method can not be used; nor
     * {@link #writeRaw} because that does not properly handle
     * value separators needed in Array or Object contexts.
     * <p>
     * Note: because of lack of type safety, some generator
     * implementations may not be able to implement this
     * method. For example, if a binary JSON format is used,
     * it may require type information for encoding; similarly
     * for generator-wrappers around Java objects or JSON nodes.
     * If implementation does not implement this method,
     * it needs to throw {@link UnsupportedOperationException}.
     *
     * @param encodedValue
     * @throws UnsupportedOperationException If underlying data format does not
     *                                       support numbers serialized textually AND if generator is not allowed
     *                                       to just output a String instead (Schema-based formats may require actual
     *                                       number, for example)
     */
    @Override
    public void writeNumber(String encodedValue) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method for outputting literal JSON boolean value (one of
     * Strings 'true' and 'false').
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param state
     */
    @Override
    public void writeBoolean(boolean state) throws IOException {
        _writeHelper(new ByteTag(state ? (byte) 1 : (byte) 1));

    }

    /**
     * Method for outputting literal JSON null value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     */
    @Override
    public void writeNull() throws IOException {
        if (tagStack.peek() instanceof CompoundTagWriter) {
            CompoundTagWriter s = (CompoundTagWriter) tagStack.peek();
            if (!s.cleanFieldName()) {
                _reportError("No field name exists.");
            };
        }
        else if (tagStack.peek() instanceof ListTag) {

        }
        else
        rootTag = null;
    }

    private boolean isFlushed = false;

    @Override
    public void flush() throws IOException {
        if (isFlushed) return;
        isFlushed = true;

        if (tagStack.size() > 1) {
            _reportError("Can not flush");
        }
        else if (tagStack.size() == 1)
        {
            Tag<?> s = tagStack.peek();
            new NBTSerializer(false).toStream(new NamedTag(null, s), writer);
        } else if ( rootTag != null) {
            new NBTSerializer(false).toStream(new NamedTag(null, rootTag), writer);
        }
    }

    /**
     * Method called to release any buffers generator may be holding,
     * once generator is being closed.
     */
    @Override
    protected void _releaseBuffers() {

    }

    /**
     * Method called before trying to write a value (scalar or structured),
     * to verify that this is legal in current output state, as well as to
     * output separators if and as necessary.
     *
     * @param typeMsg Additional message used for generating exception message
     *                if value output is NOT legal in current generator output state.
     */
    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {

    }
}
