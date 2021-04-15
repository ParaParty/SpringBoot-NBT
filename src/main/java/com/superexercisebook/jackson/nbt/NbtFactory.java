package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.PackageVersion;

import java.io.*;
import java.net.URL;

public class NbtFactory extends JsonFactory {
    public NbtFactory() { }

    public NbtFactory(ObjectCodec codec) {
        super(codec);
    }

    protected NbtFactory(NbtFactory src, ObjectCodec oc)
    {
        super(src, oc);
    }

    protected NbtFactory(NbtFactoryBuilder b) {
        super(b, false);
    }


    @Override
    public NbtFactoryBuilder rebuild() {
        return new NbtFactoryBuilder(this);
    }


    public static NbtFactoryBuilder builder() {
        return new NbtFactoryBuilder();
    }


    @Override
    public NbtFactory copy()
    {
        _checkInvalidCopy(NbtFactory.class);
        return new NbtFactory(this, null);
    }



    @Override
    protected Object readResolve() {
        return new NbtFactory(this, _objectCodec);
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public String getFormatName() {
        return "NBT";
    }

    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // TODO
        return MatchStrength.INCONCLUSIVE;
    }


    // Nbt is not positional
    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    // Nbt can embed raw binary data natively
    @Override
    public boolean canHandleBinaryNatively() {
        return true;
    }

    @Override
    public boolean canUseCharArrays() { return false; }



    @SuppressWarnings("resource")
    @Override
    public NbtParser createParser(File f) throws IOException {
        final IOContext ctxt = _createContext(f, true);
        return _createParser(_decorate(new FileInputStream(f), ctxt), ctxt);
    }

    @Override
    public NbtParser createParser(URL url) throws IOException {
        final IOContext ctxt = _createContext(url, true);
        return _createParser(_decorate(_optimizedStreamFromURL(url), ctxt), ctxt);
    }

    @Override
    public NbtParser createParser(InputStream in) throws IOException {
        final IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public NbtParser createParser(byte[] data) throws IOException {
        return _createParser(data, 0, data.length, _createContext(data, true));
    }

    @SuppressWarnings("resource")
    @Override
    public NbtParser createParser(byte[] data, int offset, int len) throws IOException {
        IOContext ctxt = _createContext(data, true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods
    /**********************************************************
     */

    @Override
    public NbtGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        return _createNbtGenerator(ctxt, _generatorFeatures, _objectCodec, _decorate(out, ctxt));
    }

    /**
     * Method for constructing {@link JsonGenerator} for generating
     * Nbt-encoded output.
     *<p>
     * Since Nbt format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    @Override
    public NbtGenerator createGenerator(OutputStream out) throws IOException {
        IOContext ctxt = _createContext(out, false);
        return _createNbtGenerator(ctxt, _generatorFeatures, _objectCodec, _decorate(out, ctxt));
    }

    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return super._createContext(srcRef, resourceManaged);
    }

    @Override
    protected NbtParser _createParser(InputStream in, IOContext ctxt) throws IOException
    {
        // TODO
//        byte[] buf = ctxt.allocReadIOBuffer();
//        return new NbtParser(ctxt, _parserFeatures,
//                _objectCodec, in, buf, 0, 0, true);
        return null;
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return _nonByteSource();
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt,
                                       boolean recyclable) throws IOException {
        return _nonByteSource();
    }

    @Override
    protected NbtParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException
    {
        // TODO
//        return new NbtParser(ctxt, _parserFeatures,
//                _objectCodec, null, data, offset, len, false);
        return null;
    }

    @Override
    protected NbtGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return _nonByteTarget();
    }

    @Override
    protected NbtGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return _createNbtGenerator(ctxt, _generatorFeatures, _objectCodec, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException {
        return _nonByteTarget();
    }

    private final NbtGenerator _createNbtGenerator(IOContext ctxt, int stdFeat, ObjectCodec codec, OutputStream out) throws IOException
    {
        return new NbtGenerator(ctxt, stdFeat, _objectCodec, out);
    }


    protected <T> T _nonByteSource() {
        throw new UnsupportedOperationException("Can not create parser for non-byte-based source");
    }

    protected <T> T _nonByteTarget() {
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }
}
