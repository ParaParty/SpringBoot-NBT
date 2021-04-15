package com.superexercisebook.jackson.nbt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

public class NbtMapper extends ObjectMapper {
    public static class Builder extends MapperBuilder<NbtMapper, Builder>
    {
        protected Builder(NbtMapper mapper) {
            super(mapper);
        }
    }

    public NbtMapper()
    {
        this(new NbtFactory());
    }

    public NbtMapper(NbtFactory s)
    {
        super(s);
    }

    public NbtMapper(NbtMapper src)
    {
        super(src);
    }

    @Override
    public NbtMapper copy()
    {
        _checkInvalidCopy(NbtMapper.class);
        return new NbtMapper(this);
    }

    @Override
    public NbtFactory getFactory() {
        return (NbtFactory) _jsonFactory;
    }

}
