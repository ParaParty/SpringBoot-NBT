package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.util.TextBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class NbtParser extends ParserMinimalBase {
    private final IOContext _ioContext;
    private ObjectCodec _objectCodec;
    private final InputStream _inputStream;
    private final byte[] _inputBuffer;
    private final int _inputPtr;
    private final int _inputEnd;
    private final boolean _bufferRecyclable;
    private final TextBuffer _textBuffer;
    private final int _tokenInputRow;
    private final int _tokenInputCol;

    private JsonReadContext _parsingContext;

    /* nbt format tag */
    private enum NbtTag {
        TAG_END(0, "TAG_End", 0),
        TAG_BYTE(1, "TAG_Byte", 1),
        TAG_SHORT(2, "TAG_Short", 2),
        TAG_INT(3, "TAG_Int", 4),
        TAG_LONG(4, "TAG_Long", 8),
        TAG_FLOAT(5, "TAG_Float", 4),
        TAG_DOUBLE(6, "TAG_Double", 8),
        TAG_BYTE_ARRAY(7, "TAG_Byte_Array", -1),
        TAG_STRING(8, "TAG_String", -1),
        TAG_LIST(9, "TAG_List", -1),
        TAG_COMPOUND(10, "TAG_Compound", -1),
        TAG_INT_ARRAY(11, "TAG_Int_Array", -1),
        TAG_LONG_ARRAY(12, "TAG_Long_Array", -1)
        ;

        final int TYPE_ID;
        final String TYPE_TAME;
        final int PAYLOAD_SIZE;  // unit is Bytes, -1 means unlimited size

        NbtTag(int typeId, String typeName, int payloadSize) {
            TYPE_ID = typeId;
            TYPE_TAME = typeName;
            PAYLOAD_SIZE = payloadSize;
        }

        static NbtTag getTagByTypeId(int typeId) {
            switch (typeId) {
                case 1:
                    return TAG_BYTE;
                case 2:
                    return TAG_SHORT;
                case 3:
                    return TAG_INT;
                case 4:
                    return TAG_LONG;
                case 5:
                    return TAG_FLOAT;
                case 6:
                    return TAG_DOUBLE;
                case 7:
                    return TAG_BYTE_ARRAY;
                case 8:
                    return TAG_STRING;
                case 9:
                    return TAG_LIST;
                case 10:
                    return TAG_COMPOUND;
                case 11:
                    return TAG_INT_ARRAY;
                case 12:
                    return TAG_LONG_ARRAY;
                default:
                    return TAG_END;
            }
        }
    }

    public NbtParser(IOContext ctxt, int parserFeatures,
                     ObjectCodec codec,
                     InputStream in, byte[] inputBuffer, int start, int end,
                     boolean bufferRecyclable){
        super(parserFeatures);
        _ioContext = ctxt;
        _objectCodec = codec;

        _inputStream = in;
        _inputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _bufferRecyclable = bufferRecyclable;
        _textBuffer = ctxt.constructTextBuffer();

        _tokenInputRow = -1;
        _tokenInputCol = -1;
    }

    private byte[] fakeInputBuffer;
    private int ptr;
    private int parserFlag;

    public void setInput(byte[] inputBuffer) {
        fakeInputBuffer = inputBuffer;
        ptr = 0;
        parserFlag = 0;
    }

    private int getUnsignedShort() {
        int value = ((fakeInputBuffer[ptr] & 0xFF) << 8) | (fakeInputBuffer[ptr + 1] & 0xFF);
        ptr += Short.BYTES;
        return value;
    }

    private int getSignedInt() {
        int value = ((fakeInputBuffer[ptr] & 0xFF) << 24)
                | ((fakeInputBuffer[ptr + 1] & 0xFF) << 16)
                | ((fakeInputBuffer[ptr + 2] & 0xFF) << 8)
                | (fakeInputBuffer[ptr + 3] & 0xFF);
        ptr += 4;
        return value;
    }

    public int getNbtTypeId() {
        return fakeInputBuffer[ptr++] & 0xFF;
    }

    public String getFieldName() {
        return getStringValue();
    }

    /**
     * @return 	A single signed, big endian 32 bit integer
     */
    @Override
    public int getIntValue() throws IOException {
        return getSignedInt();
    }

    /**
     * @return A single signed, big endian 64 bit integer
     */
    @Override
    public long getLongValue() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(fakeInputBuffer, ptr, Long.BYTES);
        ptr += Long.BYTES;
        long value = buffer.getLong();
        buffer.clear();
        return value;
    }

    /**
     * @return A single, big endian IEEE-754 single-precision floating point number (NaN possible)
     */
    @Override
    public float getFloatValue() throws IOException {
        int asFloat = ((fakeInputBuffer[ptr] & 0xFF) << 24)
                | ((fakeInputBuffer[ptr + 1] & 0xFF) << 16)
                | ((fakeInputBuffer[ptr + 2] & 0xFF) << 8)
                | (fakeInputBuffer[ptr + 3] & 0xFF);
        ptr += 4;
        return Float.intBitsToFloat(asFloat);
    }

    /**
     * @return A single signed byte
     */
    public byte getByteValue() {
        return fakeInputBuffer[ptr++];
    }

    /**
     * @return A single signed, big endian 16 bit integer
     */
    public short getShortValue() {
        short value = (short) (((fakeInputBuffer[ptr] & 0xFF) << 8) | (fakeInputBuffer[ptr + 1] & 0xFF));
        ptr += 2;
        return value;
    }

    /**
     * @return A single, big endian IEEE-754 double-precision floating point number (NaN possible)
     */
    @Override
    public double getDoubleValue() throws IOException {
        long asDouble = ((long) (fakeInputBuffer[ptr] & 0xFF) << 56)
                | ((long) (fakeInputBuffer[ptr + 1] & 0xFF) << 48)
                | ((long) (fakeInputBuffer[ptr + 2] & 0xFF) << 40)
                | ((long) (fakeInputBuffer[ptr + 3] & 0xFF) << 32)
                | ((long) (fakeInputBuffer[ptr + 4] & 0xFF) << 24)
                | ((fakeInputBuffer[ptr + 5] & 0xFF) << 16)
                | ((fakeInputBuffer[ptr + 6] & 0xFF) << 8)
                | (fakeInputBuffer[ptr + 7] & 0xFF);
        ptr += 8;
        return Double.longBitsToDouble(asDouble);
    }

    /**
     * The prefix is a signed integer (thus 4 bytes)
     *
     * @return A length-prefixed array of signed bytes.
     */
    public byte[] getByteArrayValue() {
        int length = getSignedInt();
        byte[] value = Arrays.copyOfRange(fakeInputBuffer, ptr, ptr + length);
        ptr += length;
        return value;
    }

    /**
     * The prefix is an unsigned short (thus 2 bytes)
     * signifying the length of the string in bytes.
     *
     * @return A length-prefixed modified UTF-8 string.
     */
    public String getStringValue() {
        StringBuilder value = new StringBuilder();
        int length = getUnsignedShort();
        for (int i = 0; i < length; i++) {
            value.append((char) fakeInputBuffer[ptr]);
            ptr++;
        }
        return value.toString();
    }

    /**
     * The list is prefixed with the Type ID of the items
     * it contains (thus 1 byte), and the length of the
     * list as a signed integer (a further 4 bytes). If
     * the length of the list is 0 or negative, the type
     * may be 0 (TAG_End) but otherwise it must be any
     * other type. (The notchian implementation uses
     * TAG_End in that situation, but another reference
     * implementation by Mojang uses 1 instead; parsers
     * should accept any type if the length is <= 0).
     *
     * @return A list of nameless tags, all of the same type.
     */
    public List<?> getListValue() throws IOException {
        int typeId = getNbtTypeId();
        int length = getSignedInt();
        switch (NbtTag.getTagByTypeId(typeId)) {
            case TAG_BYTE:
                List<Byte> byteList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    byteList.add(getByteValue());
                }
                return byteList;
            case TAG_SHORT:
                List<Short> shortList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    shortList.add(getShortValue());
                }
                return shortList;
            case TAG_INT:
                List<Integer> intList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    intList.add(getIntValue());
                }
                return intList;
            case TAG_LONG:
                List<Long> longList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    longList.add(getLongValue());
                }
                return longList;
            case TAG_FLOAT:
                List<Float> floatList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    floatList.add(getFloatValue());
                }
                return floatList;
            case TAG_DOUBLE:
                List<Double> doubleList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    doubleList.add(getDoubleValue());
                }
                return doubleList;
            case TAG_BYTE_ARRAY:
                List<byte[]> byteArrayList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    byteArrayList.add(getByteArrayValue());
                }
                return byteArrayList;
            case TAG_STRING:
                List<String> stringList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    stringList.add(getStringValue());
                }
                return stringList;
            case TAG_COMPOUND:
                List<Object> objectList = new ArrayList<>();
                // todo
                return objectList;
            case TAG_INT_ARRAY:
                List<int[]> intArrayList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    intArrayList.add(getIntArrayValue());
                }
                return intArrayList;
            case TAG_LONG_ARRAY:
                List<long[]> longArrayList = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    longArrayList.add(getLongArrayValue());
                }
                return longArrayList;
            default:
                return new ArrayList<>();
        }
    }

    private boolean checkIsParserFinished() {
        if (ptr >= fakeInputBuffer.length) {
            // eof
            return true;
        }
        return parserFlag == 0;
    }

    /**
     * Effectively a list of a named tags. Order is not guaranteed.
     *
     * @return a nested object
     */
    public Object getCompoundValue() throws IOException {
        int curTypeId = getNbtTypeId();
        parserFlag++;
        String curObjectName = getFieldName();
        parseObjectBody();
        return new Object();
    }

    private void parseObjectBody() throws IOException {
        while (!checkIsParserFinished()) {
            NbtTag subLevelTag = NbtTag.getTagByTypeId(getNbtTypeId());
            String curFieldName;
            switch (subLevelTag) {
                case TAG_BYTE:
                    curFieldName = getFieldName();
                    byte curByte = getByteValue();
                    break;
                case TAG_SHORT:
                    curFieldName = getFieldName();
                    short curShort = getShortValue();
                    break;
                case TAG_INT:
                    curFieldName = getFieldName();
                    int curInt = getIntValue();
                    break;
                case TAG_LONG:
                    curFieldName = getFieldName();
                    long curLong = getLongValue();
                    break;
                case TAG_FLOAT:
                    curFieldName = getFieldName();
                    float curFloat = getFloatValue();
                    break;
                case TAG_DOUBLE:
                    curFieldName = getFieldName();
                    double curDouble = getDoubleValue();
                    break;
                case TAG_BYTE_ARRAY:
                    curFieldName = getFieldName();
                    byte[] curByteArray = getByteArrayValue();
                    break;
                case TAG_STRING:
                    curFieldName = getFieldName();
                    String curString = getStringValue();
                    break;
                case TAG_LIST:
                    curFieldName = getFieldName();
                    List<?> curList = getListValue();
                    break;
                case TAG_COMPOUND:
                    curFieldName = getFieldName();
                    parserFlag++;
                    parseObjectBody();  // head has been parsed
                    break;
                case TAG_INT_ARRAY:
                    curFieldName = getFieldName();
                    int[] curIntArray = getIntArrayValue();
                    break;
                case TAG_LONG_ARRAY:
                    curFieldName = getFieldName();
                    long[] curLongArray = getLongArrayValue();
                    break;
                case TAG_END:
                    parserFlag--;
                    break;
                default:
                    // todo, handle illegal input
                    break;
            }
        }
    }

    /**
     * The prefix is a signed integer (thus 4 bytes) and
     * indicates the number of 4 byte integers.
     *
     * @return A length-prefixed array of signed integers.
     */
    public int[] getIntArrayValue() throws IOException {
        int length = getSignedInt();
        int[] value = new int[length];
        for (int i = 0; i < length; i++) {
            value[i] = getIntValue();
        }
        return value;
    }

    /**
     * The prefix is a signed integer (thus 4 bytes) and
     * indicates the number of 8 byte longs.
     *
     * @return A length-prefixed array of signed longs.
     */
    public long[] getLongArrayValue() throws IOException {
        int length = getSignedInt();
        long[] value = new long[length];
        for (int i = 0; i < length; i++) {
            value[i] = getLongValue();
        }
        return value;
    }

    // *********************************

    @Override
    public JsonToken nextToken() throws IOException {
        return null;
    }

    /**
     * Method sub-classes need to implement
     */
    @Override
    protected void _handleEOF() throws JsonParseException {

    }

    @Override
    public String getCurrentName() throws IOException {
        return null;
    }

    /**
     * Accessor for {@link ObjectCodec} associated with this
     * parser, if any. Codec is used by {@link #readValueAs(Class)}
     * method (and its variants).
     */
    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    /**
     * Setter that allows defining {@link ObjectCodec} associated with this
     * parser, if any. Codec is used by {@link #readValueAs(Class)}
     * method (and its variants).
     *
     * @param c
     */
    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    /**
     * Accessor for getting version of the core package, given a parser instance.
     * Left for sub-classes to implement.
     */
    @Override
    public Version version() {
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return _parsingContext;
    }

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    @Override
    public JsonLocation getTokenLocation() {
        return null;
    }

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes.
     */
    @Override
    public JsonLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void overrideCurrentName(String name) {

    }

    @Override
    public String getText() throws IOException {
        return null;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return new char[0];
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    /**
     * Generic number value accessor method that will work for
     * all kinds of numeric values. It will return the optimal
     * (simplest/smallest possible) wrapper object that can
     * express the numeric value just parsed.
     */
    @Override
    public Number getNumberValue() throws IOException {
        return null;
    }

    /**
     * If current token is of type
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberType} constants; otherwise returns null.
     */
    @Override
    public NumberType getNumberType() throws IOException {
        return null;
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return null;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return null;
    }

    @Override
    public int getTextLength() throws IOException {
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        return new byte[0];
    }
}
