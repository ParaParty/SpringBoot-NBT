package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.TextBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

public class NbtParser extends ParserMinimalBase {
    private final IOContext _ioContext;
    private final ObjectCodec _objectCodec;
    private final InputStream _inputStream;
    private final byte[] _inputBuffer;
    private final int _inputPtr;
    private final int _inputEnd;
    private final boolean _bufferRecyclable;
    private final TextBuffer _textBuffer;
    private final int _tokenInputRow;
    private final int _tokenInputCol;

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
        return null;
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
        return 0;
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
        return 0;
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
        return null;
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
        return 0;
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
        return 0;
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} or
     * {@link JsonToken#VALUE_NUMBER_INT}. No under/overflow exceptions
     * are ever thrown.
     */
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
