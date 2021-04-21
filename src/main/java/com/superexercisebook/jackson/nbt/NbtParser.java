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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NbtParser extends ParserMinimalBase {
    private final IOContext ioContext;
    private final InputStream inputStream;
    private final byte[] inputBuffer;
    private final int inputStart;
    private final int inputLength;
    private final boolean bufferRecyclable;
    private final TextBuffer textBuffer;
    private final int tokenInputRow;
    private final int tokenInputCol;

    private int objectCounter;
    private int inputPtr;
    private ObjectCodec objectCodec;
    private JsonReadContext parsingContext;
    private boolean closed;

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
                     InputStream in, byte[] inputBuffer, int start, int length,
                     boolean bufferRecyclable){
        super(parserFeatures);
        ioContext = ctxt;
        objectCodec = codec;

        inputStream = in;
        this.inputBuffer = inputBuffer;
        inputPtr = start;
        inputStart = start;
        inputLength = length;
        this.bufferRecyclable = bufferRecyclable;
        textBuffer = ctxt.constructTextBuffer();

        tokenInputRow = -1;
        tokenInputCol = -1;

        objectCounter = 0;
        closed = false;
    }

    @Override
    public String getCurrentName() throws IOException {
        return getText();
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
        long value = ((long) (inputBuffer[inputPtr] & 0xFF) << 56)
                | ((long) (inputBuffer[inputPtr + 1] & 0xFF) << 48)
                | ((long) (inputBuffer[inputPtr + 2] & 0xFF) << 40)
                | ((long) (inputBuffer[inputPtr + 3] & 0xFF) << 32)
                | ((long) (inputBuffer[inputPtr + 4] & 0xFF) << 24)
                | ((inputBuffer[inputPtr + 5] & 0xFF) << 16)
                | ((inputBuffer[inputPtr + 6] & 0xFF) << 8)
                | (inputBuffer[inputPtr + 7] & 0xFF);
        inputPtr += 8;
        return value;
    }

    /**
     * @return A single, big endian IEEE-754 single-precision floating point number (NaN possible)
     */
    @Override
    public float getFloatValue() throws IOException {
        int asFloat = getSignedInt();
        return Float.intBitsToFloat(asFloat);
    }

    /**
     * @return A single signed byte
     */
    @Override
    public byte getByteValue() {
        return inputBuffer[inputPtr++];
    }

    /**
     * @return A single signed, big endian 16 bit integer
     */
    @Override
    public short getShortValue() {
        short value = (short) (((inputBuffer[inputPtr] & 0xFF) << 8) | (inputBuffer[inputPtr + 1] & 0xFF));
        inputPtr += 2;
        return value;
    }

    /**
     * @return A single, big endian IEEE-754 double-precision floating point number (NaN possible)
     */
    @Override
    public double getDoubleValue() throws IOException {
        long asDouble = getLongValue();
        return Double.longBitsToDouble(asDouble);
    }

    /**
     * The prefix is a signed integer (thus 4 bytes)
     *
     * @return A length-prefixed array of signed bytes.
     */
    public byte[] getByteArrayValue() {
        int length = getSignedInt();
        byte[] value = Arrays.copyOfRange(inputBuffer, inputPtr, inputPtr + length);
        inputPtr += length;
        return value;
    }

    /**
     * The prefix is an unsigned short (thus 2 bytes)
     * signifying the length of the string in bytes.
     *
     * @return A length-prefixed modified UTF-8 string.
     */
    @Override
    public String getText() {
        StringBuilder value = new StringBuilder();
        int length = getUnsignedShort();
        for (int i = 0; i < length; i++) {
            value.append((char) inputBuffer[inputPtr]);
            inputPtr++;
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
                    stringList.add(getText());
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

    /**
     * Effectively a list of a named tags. Order is not guaranteed.
     *
     * @return a nested object
     */
    public Object getCompoundValue() throws IOException {
        int curTypeId = getNbtTypeId();
        objectCounter++;
        String curObjectName = getCurrentName();
        parseObjectBody();
        return new Object();
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

    @Override
    public JsonToken nextToken() throws IOException {
        return null;
    }

    @Override
    protected void _handleEOF() throws JsonParseException {
        _throwInternal();
    }

    @Override
    public ObjectCodec getCodec() {
        return objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        objectCodec = c;
    }

    @Override
    public Version version() {
        return objectCodec.version();
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        if (inputPtr >= inputStart + inputLength) {
            // eof
            closed = true;
        } else {
            closed = objectCounter == 0;
        }
        return closed;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return parsingContext;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return null;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void overrideCurrentName(String name) {

    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return new char[0];
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    @Override
    public Number getNumberValue() throws IOException {
        return null;
    }

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

    /**
     * used to read an unsigned short number, 2 bytes long
     *
     * @return a int converted by an unsigned short
     */
    private int getUnsignedShort() {
        int value = ((inputBuffer[inputPtr] & 0xFF) << 8) | (inputBuffer[inputPtr + 1] & 0xFF);
        inputPtr += 2;
        return value;
    }

    /**
     * used to read a signed int, 4 bytes long
     *
     * @return a 4 bytes long integer
     */
    private int getSignedInt() {
        int value = ((inputBuffer[inputPtr] & 0xFF) << 24)
                | ((inputBuffer[inputPtr + 1] & 0xFF) << 16)
                | ((inputBuffer[inputPtr + 2] & 0xFF) << 8)
                | (inputBuffer[inputPtr + 3] & 0xFF);
        inputPtr += 4;
        return value;
    }

    /**
     * used to get the NBT type id, 1 byte long
     *
     * @return nbt type id {@link NbtTag#getNbtTypeId()}
     */
    private int getNbtTypeId() {
        return inputBuffer[inputPtr++] & 0xFF;
    }

    /**
     * object header include TAG_COMPOUND and field name with its length,
     * the residue is object body
     */
    private void parseObjectBody() throws IOException {
        while (!isClosed()) {
            NbtTag subLevelTag = NbtTag.getTagByTypeId(getNbtTypeId());
            String curFieldName;
            switch (subLevelTag) {
                case TAG_BYTE:
                    curFieldName = getCurrentName();
                    byte curByte = getByteValue();
                    break;
                case TAG_SHORT:
                    curFieldName = getCurrentName();
                    short curShort = getShortValue();
                    break;
                case TAG_INT:
                    curFieldName = getCurrentName();
                    int curInt = getIntValue();
                    break;
                case TAG_LONG:
                    curFieldName = getCurrentName();
                    long curLong = getLongValue();
                    break;
                case TAG_FLOAT:
                    curFieldName = getCurrentName();
                    float curFloat = getFloatValue();
                    break;
                case TAG_DOUBLE:
                    curFieldName = getCurrentName();
                    double curDouble = getDoubleValue();
                    break;
                case TAG_BYTE_ARRAY:
                    curFieldName = getCurrentName();
                    byte[] curByteArray = getByteArrayValue();
                    break;
                case TAG_STRING:
                    curFieldName = getCurrentName();
                    String curString = getText();
                    break;
                case TAG_LIST:
                    curFieldName = getCurrentName();
                    List<?> curList = getListValue();
                    break;
                case TAG_COMPOUND:
                    curFieldName = getCurrentName();
                    objectCounter++;
                    parseObjectBody();  // head has been parsed
                    break;
                case TAG_INT_ARRAY:
                    curFieldName = getCurrentName();
                    int[] curIntArray = getIntArrayValue();
                    break;
                case TAG_LONG_ARRAY:
                    curFieldName = getCurrentName();
                    long[] curLongArray = getLongArrayValue();
                    break;
                case TAG_END:
                    objectCounter--;
                    break;
                default:
                    // todo, handle illegal input
                    break;
            }
        }
    }
}
