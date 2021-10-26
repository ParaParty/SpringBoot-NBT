package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import net.querz.nbt.tag.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Stack;

public class NbtParser extends ParserMinimalBase {
    private final IOContext _ioContext;
    private ObjectCodec _objectCodec;
    private final TextBuffer _textBuffer;

    private final ByteInputStream inputStream;
    private final DataInputStream dataInputStream;

    private ArrayDeque<JsonToken> tokenQueue = new ArrayDeque<JsonToken>();
    private ArrayDeque<Object> valueQueue = new ArrayDeque<Object>();
    private Object nowValue = null;

    private Stack<State> stateStack = new Stack<State>();

    enum State {
        MAP, LIST, LIST_BYTE, LIST_INT, LIST_ARRAY
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

    public NbtParser(IOContext ctxt, int parserFeatures,
                     ObjectCodec codec,
                     byte[] inputBuffer, int start, int end) throws IOException {
        super(parserFeatures);
        _ioContext = ctxt;
        _objectCodec = codec;
        _textBuffer = ctxt.constructTextBuffer();

        // include start, exclude end
        inputStream = new ByteInputStream(inputBuffer, start, end - start);
        dataInputStream = new DataInputStream(inputStream);

        if (inputBuffer[0] == CompoundTag.ID) {
            dataInputStream.readByte(); // 读掉 0x0A
            dataInputStream.readUTF();  // 读掉第一层键-

            tokenQueue.push(JsonToken.START_OBJECT);
            pushState(State.MAP);
        } else if (inputBuffer[0] == ListTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

//            tokenQueue.push(JsonToken.START_ARRAY);
//            pushState(State.LIST);
        } else if (inputBuffer[0] == ByteArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readByte());
            }

//            tokenQueue.push(JsonToken.START_ARRAY);
//            pushState(State.LIST_BYTE);
        } else if (inputBuffer[0] == IntArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readInt());
            }

//            tokenQueue.push(JsonToken.START_ARRAY);
//            pushState(State.LIST_INT);
        } else if (inputBuffer[0] == LongArrayTag.ID) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readLong());
            }

//            tokenQueue.push(JsonToken.START_ARRAY);
//            pushState(State.LIST_ARRAY);
        } else {
            throw new IOException("Not support.");
        }
    }

    @Override
    public JsonToken nextToken() throws IOException {
        if (!tokenQueue.isEmpty()) {
            nowValue = valueQueue.pop();
            return tokenQueue.pop();
        }

        if (topState() == State.MAP) {
            byte type = dataInputStream.readByte();
            if (type == EndTag.ID) {
                return null;
            }

            String key = dataInputStream.readUTF();
            tokenQueue.push(JsonToken.FIELD_NAME);
            valueQueue.push(key);

            switch (type) {
                case ByteTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.push(dataInputStream.readByte());
                    break;
                case ShortTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.push(dataInputStream.readShort());
                    break;
                case IntTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.push(dataInputStream.readInt());
                    break;
                case LongTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.push(dataInputStream.readLong());
                    break;
                case FloatTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.push(dataInputStream.readFloat());
                    break;
                case DoubleTag.ID:
                    tokenQueue.push(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.push(dataInputStream.readDouble());
                    break;
                case ByteArrayTag.ID:
                    //TODO
                    break;
                case StringTag.ID:
                    tokenQueue.push(JsonToken.VALUE_STRING);
                    valueQueue.push(dataInputStream.readUTF());
                    break;
                case ListTag.ID:
                    //TODO
                    break;
                case CompoundTag.ID:
                    pushState(State.MAP);
                    break;
                case IntArrayTag.ID:
                    //TODO
                    break;
                case LongArrayTag.ID:
                    //TODO
                    break;
            }
        }

        if (!tokenQueue.isEmpty()) {
            nowValue = valueQueue.pop();
            return tokenQueue.pop();
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
