package party.para.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.TextBuffer;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Stack;

/**
 * NBT Parser
 * <p>
 * Parser a NBT data from NBT stream.
 */
public class NbtParser extends ParserMinimalBase {
    private final IOContext _ioContext;
    private ObjectCodec _objectCodec;
    private final TextBuffer _textBuffer;

    private final ByteArrayInputStream inputStream;
    private final DataInputStream dataInputStream;
    private final int _totalByte;

    private final ArrayDeque<JsonToken> tokenQueue = new ArrayDeque<JsonToken>();
    private final ArrayDeque<Object> valueQueue = new ArrayDeque<Object>();
    private Object nowValue = null;

    private final Stack<State> stateStack = new Stack<State>();

    static class State {
        final byte type;
        final int length;
        int nowIndex = 0;
        final byte containsType;

        State(BinaryTagType<?> type) {
            this.type = type.id();
            this.length = 0;
            this.containsType = (byte) 0;
        }

        State(BinaryTagType<?> type, int length) {
            this.type = type.id();
            this.length = length;
            this.containsType = (byte) 0;
        }

        State(BinaryTagType<?> type, int length, byte containsType) {
            this.type = type.id();
            this.length = length;
            this.containsType = containsType;
        }

        static State MAP() {
            return new State(BinaryTagTypes.COMPOUND);
        }

        static State LIST(int length, byte containsType) {
            return new State(BinaryTagTypes.LIST, length, containsType);
        }

        static State LIST_BYTE(int length) {
            return new State(BinaryTagTypes.BYTE_ARRAY, length);
        }

        static State LIST_INT(int length) {
            return new State(BinaryTagTypes.INT_ARRAY, length);
        }

        static State LONG_ARRAY(int length) {
            return new State(BinaryTagTypes.LONG_ARRAY, length);
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


        if (inputBuffer[0] == BinaryTagTypes.COMPOUND.id()) {
            dataInputStream.readByte(); // 读掉 0x0A
            String key = dataInputStream.readUTF();  // 读掉第一层键-

            tokenQueue.addLast(JsonToken.START_OBJECT);
            valueQueue.addLast(key);
            pushState(State.MAP());
        } else if (inputBuffer[0] == BinaryTagTypes.LIST.id()) {
            byte containsType = dataInputStream.readByte();
            int length = dataInputStream.readInt();
            tokenQueue.addLast(JsonToken.START_ARRAY);
            valueQueue.addLast("[");
            pushState(State.LIST(length, containsType));
        } else if (inputBuffer[0] == BinaryTagTypes.BYTE_ARRAY.id()) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readByte());
            }

//            tokenQueue.addLast(JsonToken.START_ARRAY);
//            pushState(State.LIST_BYTE);
        } else if (inputBuffer[0] == BinaryTagTypes.INT_ARRAY.id()) {
            dataInputStream.readByte();
            int length = dataInputStream.readInt();

            for (int i = 0; i < length; i++) {
                tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                valueQueue.add(dataInputStream.readInt());
            }

//            tokenQueue.addLast(JsonToken.START_ARRAY);
//            pushState(State.LIST_INT);
        } else if (inputBuffer[0] == BinaryTagTypes.LONG_ARRAY.id()) {
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

        if (topState().type == BinaryTagTypes.COMPOUND.id()) {
            byte type = dataInputStream.readByte();
            if (type == BinaryTagTypes.END.id()) {
                popState();
                tokenQueue.addLast(JsonToken.END_OBJECT);
                valueQueue.addLast("}");
            } else {
                String key = dataInputStream.readUTF();
                tokenQueue.addLast(JsonToken.FIELD_NAME);
                valueQueue.addLast(key);

                if (type == BinaryTagTypes.BYTE.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readByte());
                } else if (type == BinaryTagTypes.SHORT.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readShort());
                } else if (type == BinaryTagTypes.INT.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readInt());
                } else if (type == BinaryTagTypes.LONG.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readLong());
                } else if (type == BinaryTagTypes.FLOAT.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.addLast(dataInputStream.readFloat());
                } else if (type == BinaryTagTypes.DOUBLE.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.addLast(dataInputStream.readDouble());
                } else if (type == BinaryTagTypes.BYTE_ARRAY.id()) {
                    int length = dataInputStream.readInt();
                    tokenQueue.add(JsonToken.START_ARRAY);
                    valueQueue.add("[");
                    for (int i = 0; i < length; i++) {
                        tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.add(dataInputStream.readByte());
                    }
                    tokenQueue.add(JsonToken.END_ARRAY);
                    valueQueue.add("]");
                } else if (type == BinaryTagTypes.STRING.id()) {
                    tokenQueue.addLast(JsonToken.VALUE_STRING);
                    valueQueue.addLast(dataInputStream.readUTF());
                } else if (type == BinaryTagTypes.LIST.id()) {
                    byte containsType = dataInputStream.readByte();
                    int length = dataInputStream.readInt();
                    tokenQueue.addLast(JsonToken.START_ARRAY);
                    valueQueue.addLast("[");
                    pushState(State.LIST(length, containsType));
                } else if (type == BinaryTagTypes.COMPOUND.id()) {
                    pushState(State.MAP());
                } else if (type == BinaryTagTypes.INT_ARRAY.id()) {
                    int length = dataInputStream.readInt();
                    tokenQueue.add(JsonToken.START_ARRAY);
                    valueQueue.add("[");
                    for (int i = 0; i < length; i++) {
                        tokenQueue.add(JsonToken.VALUE_NUMBER_INT);
                        valueQueue.add(dataInputStream.readInt());
                    }
                    tokenQueue.add(JsonToken.END_ARRAY);
                    valueQueue.add("]");
                } else if (type == BinaryTagTypes.LONG_ARRAY.id()) {
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
                    _reportError("Invalid Type ID");
                }
            }
        } else if (topState().type == BinaryTagTypes.LIST.id()) {
            State nowState = topState();
            byte nowContainsType = nowState.containsType;
            int nowLength = nowState.length;

            if (nowContainsType == BinaryTagTypes.END.id()) {
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.BYTE.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readByte());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.SHORT.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readShort());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.INT.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readInt());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.LONG.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_INT);
                    valueQueue.addLast(dataInputStream.readLong());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.FLOAT.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.addLast(dataInputStream.readFloat());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.DOUBLE.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_NUMBER_FLOAT);
                    valueQueue.addLast(dataInputStream.readDouble());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.BYTE_ARRAY.id()) {
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
            } else if (nowContainsType == BinaryTagTypes.STRING.id()) {
                for (int i = 0; i < nowLength; i++) {
                    tokenQueue.addLast(JsonToken.VALUE_STRING);
                    valueQueue.addLast(dataInputStream.readUTF());
                }
                tokenQueue.addLast(JsonToken.END_ARRAY);
                valueQueue.addLast("]");
                popState();
            } else if (nowContainsType == BinaryTagTypes.LIST.id()) {
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
            } else if (nowContainsType == BinaryTagTypes.COMPOUND.id()) {
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
            } else if (nowContainsType == BinaryTagTypes.INT_ARRAY.id()) {
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
            } else if (nowContainsType == BinaryTagTypes.LONG_ARRAY.id()) {
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
            } else {
                _reportError("Invalid Type ID");
            }

        }

        if (!tokenQueue.isEmpty()) {
            nowValue = valueQueue.removeFirst();
            return _currToken = tokenQueue.removeFirst();
        }
        return null;
    }

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

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

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

    @Override
    public JsonLocation getTokenLocation() {
        try {
            return new JsonLocation(inputStream, _totalByte, 0, 1, _totalByte - dataInputStream.available());
        } catch (IOException e) {
            return null;
        }
    }

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

    @Override
    public Number getNumberValue() throws IOException {
        if (nowValue instanceof Number) {
            return (Number) nowValue;
        }
        throw new IOException("");
    }

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

    @Override
    public int getIntValue() throws IOException {
        if (nowValue instanceof Byte || nowValue instanceof Short || nowValue instanceof Integer) {
            return ((Number) nowValue).intValue();
        }
        throw new IOException("");
    }

    @Override
    public long getLongValue() throws IOException {
        if (nowValue instanceof Byte || nowValue instanceof Short || nowValue instanceof Integer || nowValue instanceof Long) {
            return ((Number) nowValue).longValue();
        }
        throw new IOException("");
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        if (nowValue instanceof Number) {
            return new BigInteger(String.valueOf((Number) nowValue));
        }
        throw new IOException("");
    }

    @Override
    public float getFloatValue() throws IOException {
        if (nowValue instanceof Float) {
            return (Float) nowValue;
        }
        throw new IOException("");
    }

    @Override
    public double getDoubleValue() throws IOException {
        if (nowValue instanceof Double) {
            return (Double) nowValue;
        }
        throw new IOException("");
    }

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
