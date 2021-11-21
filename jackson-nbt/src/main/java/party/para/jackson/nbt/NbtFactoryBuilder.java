package party.para.jackson.nbt;

import com.fasterxml.jackson.core.TSFBuilder;

public class NbtFactoryBuilder extends TSFBuilder<NbtFactory, NbtFactoryBuilder>
{
    public NbtFactoryBuilder() {
        super();
    }

    public NbtFactoryBuilder(NbtFactory base) {
        super(base);
    }

    @Override
    public NbtFactory build() {
        return new NbtFactory(this);
    }
}
