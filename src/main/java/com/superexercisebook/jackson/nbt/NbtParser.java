package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.TextBuffer;
import net.querz.nbt.tag.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Stack;

public class NbtParser extends ParserMinimalBase {
    private final IOContext _ioContext;
    private ObjectCodec _objectCodec;
    private final TextBuffer _textBuffer;

    private final ByteArrayInputStream inputStream;
    private final DataInputStream dataInputStream;
    private final int _totalByte;

    private ArrayDeque<JsonToken> tokenQueue = new ArrayDeque<JsonToken>();
    private ArrayDeque<Object> valueQueue = new ArrayDeque<Object>();
    private Object nowValue = null;

    private Stack<State> stateStack = new Stack<State>();

    static class State {
        final byte type;
        final int length;
        int nowIndex = 0;
        final byte containsType;

        State(byte type) {
            this.type = type;
            this.length = 0;
            this.containsType = (byte) 0;
        }

        State(byte type, int length) {
            this.type = type;
            this.length = length;
            this.containsType = (byte) 0;
        }

        State(byte type, int length, byte containsType) {
            this.type = type;
            this.length = length;
            this.containsType = containsType;
        }

        static State MAP() {
            return new State(CompoundTag.ID);
        }

        static State LIST(int length, byte containsType) {
            return new State(ListTag.ID, length, containsType);
        }

        static State LIST_BYTE(int length) {
            return new State(ByteArrayTag.ID, length);
        }

        static State LIST_INT(int length) {
            return new State(IntArrayTag.ID, length);
        }

        static State LONG_ARRAY(int length) {
            return new State(LongArrayTag.ID, length);
        }
    }

    private void pushState(State t) {
        stateStack.push(t);
    }

    private void popState() {
        stateStack.pop();
    }

    private State topState() {
        return stateStack.peek();
    }

    private boolean hasState() {
        return !stateStack.isEmpty();
    }

    public NbtParser(IOContext ctxt, int parserFeatures,
                     ObjectCodec codec,
                     byte[] inputBuffer, int start, int end) throws IOException {
        super(parserFeatures);
        _ioContext = ctxt;
        _objectCodec = codec;
        _textBuffer = ctxt.constructTextBuffer();

        // include start, exclude end
        inputStream = new ByteArrayInputStream(inputBuffer, start, end - start);
        dataInputStream = new DataInputStream(inputStream);
        _totalByte = end - start;


        if (inputBuffer[0] == CompoundTag.ID) {
            dataInputStream.readByte(); // 读掉 0x0A
            String key = dataInputStream.readUTF();  // 读掉第一层键-

            tokenQueue.addLast(JsonToken.START_OBJECT);
            valueQueue.addLast(key);
            pushState(State.MAP());
        } else if (inputBuffer[0] == ListTag.ID) {
            byte containsType = dataInputStream.readByte();
            int length = dataInputStream.readInt();
            tokenQueue.addLast(JsonToken.START_ARRAY);
            valueQueue.addLast("[");
            pushState(State.LIST(length, containsType));
        } else if (inputBuffer[0] == ByteArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readByte());
            }

//            tokenQueue.addLast(JsonToken.START_ARRAY);
//            pushState(State.LIST_BYTE);
        } else if (inputBuffer[0] == IntArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readInt());
            }

//            tokenQueue.addLast(JsonToken.START_ARRAY);
//            pushState(State.LIST_INT);
        } else if (inputBuffer[0] == LongArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readLong());
            }

//            tokenQueue.addLast(JsonToken.START_ARRAY);
//            pushState(State.LIST_ARRAY);
        } else {
            throw new IOException("Not support.");
        }
    }

    @Override
    public JsonToken nextToken() throws IOException {
        try {
            JsonToken ret = _nextToken();
            while (ret == null && hasState()) {
                ret = _nextToken();
            }
            return ret;
        } catch (EOFException e) {
            return null;
        }
    }

    private JsonToken _nextToken() throws IOException {
        if (!tokenQueue.isEmpty()) {
            nowValue = valueQueue.removeFirst();
            return _currToken = tokenQueue.removeFirst();
        }

        if (!hasState()) {
            throw new EOFException();
        }

        if (topState().type == CompoundTag.ID) {
            byte type = dataInputStream.readByte();
            if (type == EndTag.ID) {
                popState();
                tokenQueue.addLast(JsonToken.END_OBJECT);
                valueQueue.addLast("}");
            } else {

                String key = dataInputStream.readUTF();
                tokenQueue.addLast(JsonToken.FIELD_NAME);
                valueQueue.addLast(key);

                switch (type) {
                    case ByteTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readByte());
                        break;
                    case ShortTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readShort());
                        break;
                    case IntTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readInt());
                        break;
                    case LongTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readLong());
                        break;
                    case FloatTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                        valueQueue.addLast(dataInputStream.readFloat());
                        break;
                    case DoubleTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                        valueQueue.addLast(dataInputStream.readDouble());
                        break;
                    case ByteArrayTag.ID: {
                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readByte());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    }
                    break;
                    case StringTag.ID:
                        tokenQueue.addLast(JsonToken.VALUE_STRING);
                        valueQueue.addLast(dataInputStream.readUTF());
                        break;
                    case ListTag.ID: {
                        byte containsType = dataInputStream.readByte();
                        int length = dataInputStream.readInt();
                        tokenQueue.addLast(JsonToken.START_ARRAY);
                        valueQueue.addLast("[");
                        pushState(State.LIST(length, containsType));
                        break;
                    }
                    case CompoundTag.ID:
                        pushState(State.MAP());
                        break;
                    case IntArrayTag.ID: {
                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readInt());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    }
                    break;
                    case LongArrayTag.ID: {
                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readLong());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    }
                    break;
                    default:
                        _reportError("Invalid Type ID");
                }
            }
        } else if (topState().type == ListTag.ID) {
            State nowState = topState();
            byte nowContainsType = nowState.containsType;
            int nowLength = nowState.length;

            switch (nowContainsType) {
                case ByteTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readByte());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case ShortTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readShort());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case IntTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readInt());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case LongTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.addLast(dataInputStream.readLong());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case FloatTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                        valueQueue.addLast(dataInputStream.readFloat());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case DoubleTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                        valueQueue.addLast(dataInputStream.readDouble());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case ByteArrayTag.ID: {
                    if (nowState.nowIndex < nowState.length) {
                        nowState.nowIndex++;

                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readByte());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    } else {
                        tokenQueue.addLast(JsonToken.END_ARRAY);
                        valueQueue.addLast("]");
                        popState();
                    }
                }
                break;
                case StringTag.ID:
                    for (int i = 0; i < nowLength; i++) {
                        tokenQueue.addLast(JsonToken.VALUE_STRING);
                        valueQueue.addLast(dataInputStream.readUTF());
                    }
                    tokenQueue.addLast(JsonToken.END_ARRAY);
                    valueQueue.addLast("]");
                    popState();
                    break;
                case ListTag.ID: {
                    if (nowState.nowIndex < nowState.length) {
                        nowState.nowIndex++;

                        byte containsType = dataInputStream.readByte();
                        int length = dataInputStream.readInt();
                        tokenQueue.addLast(JsonToken.START_ARRAY);
                        valueQueue.addLast("[");
                        pushState(State.LIST(length, containsType));
                    } else {
                        tokenQueue.addLast(JsonToken.END_ARRAY);
                        valueQueue.addLast("]");
                        popState();
                    }
                }
                case CompoundTag.ID:
                    if (nowState.nowIndex < nowState.length) {
                        nowState.nowIndex++;

                        tokenQueue.addLast(JsonToken.START_OBJECT);
                        valueQueue.addLast("{");
                        pushState(State.MAP());
                    } else {
                        tokenQueue.addLast(JsonToken.END_ARRAY);
                        valueQueue.addLast("]");
                        popState();
                    }
                    break;
                case IntArrayTag.ID: {
                    if (nowState.nowIndex < nowState.length) {
                        nowState.nowIndex++;

                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readInt());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    } else {
                        tokenQueue.addLast(JsonToken.END_ARRAY);
                        valueQueue.addLast("]");
                        popState();
                    }
                }
                break;
                case LongArrayTag.ID: {
                    if (nowState.nowIndex < nowState.length) {
                        nowState.nowIndex++;

                        int length = dataInputStream.readInt();
                        tokenQueue.add(JsonToken.START_ARRAY);
                        valueQueue.add("[");
                        for (int i = 0; i < length; i++) {
                            tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                            valueQueue.add(dataInputStream.readLong());
                        }
                        tokenQueue.add(JsonToken.END_ARRAY);
                        valueQueue.add("]");
                    } else {
                        tokenQueue.addLast(JsonToken.END_ARRAY);
                        valueQueue.addLast("]");
                        popState();
                    }
                }
                break;
                default:
                    _reportError("Invalid Type ID");
            }

        }

        if (!tokenQueue.isEmpty()) {
            nowValue = valueQueue.removeFirst();
            return _currToken = tokenQueue.removeFirst();
        }
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
        if (nowValue instanceof String) {
            return (String) nowValue;
        }
        throw new IOException("");
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
        return new Version(1, 0, 0, "", "", "");
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
        return null;
    }

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    @Override
    public JsonLocation getTokenLocation() {
        try {
            return new JsonLocation(inputStream, _totalByte, 0, 1, _totalByte - dataInputStream.available());
        } catch (IOException e) {
            return null;
        }
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
        if (nowValue instanceof String) {
            return (String) nowValue;
        }
        throw new IOException("");
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        if (nowValue instanceof String) {
            return ((String) nowValue).toCharArray();
        }
        throw new IOException("");
    }

    @Override
    public boolean hasTextCharacters() {
        return nowValue instanceof String;
    }

    /**
     * Generic number value accessor method that will work for
     * all kinds of numeric values. It will return the optimal
     * (simplest/smallest possible) wrapper object that can
     * express the numeric value just parsed.
     */
    @Override
    public Number getNumberValue() throws IOException {
        if (nowValue instanceof Number) {
            return (Number) nowValue;
        }
        throw new IOException("");
    }

    /**
     * If current token is of type
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberType} constants; otherwise returns null.
     */
    @Override
    public NumberType getNumberType() throws IOException {
        if (nowValue instanceof Double) {
            return NumberType.DOUBLE;
        }
        if (nowValue instanceof Float) {
            return NumberType.FLOAT;
        }

        if (nowValue instanceof Byte || nowValue instanceof Short || nowValue instanceof Integer) {
            return NumberType.INT;
        }

        if (nowValue instanceof Long) {
            return NumberType.LONG;
        }

        return null;
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java int primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     * <p>
     * Note: if the resulting integer value falls outside range of
     * Java int, a {@link JsonParseException}
     * may be thrown to indicate numeric overflow/underflow.
     */
    @Override
    public int getIntValue() throws IOException {
        if (nowValue instanceof Byte || nowValue instanceof Short || nowValue instanceof Integer) {
            return ((Number) nowValue).intValue();
        }
        throw new IOException("");
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a Java long primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting to int; except for possible overflow/underflow
     * exception.
     * <p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java long, a {@link JsonParseException}
     * may be thrown to indicate numeric overflow/underflow.
     */
    @Override
    public long getLongValue() throws IOException {
        if (nowValue instanceof Byte || nowValue instanceof Short || nowValue instanceof Integer || nowValue instanceof Long) {
            return ((Number) nowValue).longValue();
        }
        throw new IOException("");
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can not be used as a Java long primitive type due to its
     * magnitude.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDecimalValue}
     * and then constructing a {@link BigInteger} from that value.
     */
    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        if (nowValue instanceof Number) {
            return new BigInteger(String.valueOf((Number) nowValue));
        }
        throw new IOException("");
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java float primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     * <p>
     * Note: if the value falls
     * outside of range of Java float, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    @Override
    public float getFloatValue() throws IOException {
        if (nowValue instanceof Float) {
            return (Float) nowValue;
        }
        throw new IOException("");
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java double primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     * <p>
     * Note: if the value falls
     * outside of range of Java double, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    @Override
    public double getDoubleValue() throws IOException {
        if (nowValue instanceof Double) {
            return (Double) nowValue;
        }
        throw new IOException("");
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} or
     * {@link JsonToken#VALUE_NUMBER_INT}. No under/overflow exceptions
     * are ever thrown.
     */
    @Override
    public BigDecimal getDecimalValue() throws IOException {
        if (nowValue instanceof Number) {
            return new BigDecimal(String.valueOf((Number) nowValue));
        }
        throw new IOException("");
    }

    @Override
    public int getTextLength() throws IOException {
        if (nowValue instanceof String) {
            return ((String) nowValue).length();
        }
        throw new IOException("");
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws IOException {
        if (!(nowValue instanceof String)) {
            _reportError("Current token (" + _currToken + ") not VALUE_STRING, can not access as binary");
        }
        ByteArrayBuilder builder = new ByteArrayBuilder();
        _decodeBase64(getText(), builder, variant);
        return builder.toByteArray();
    }
}
