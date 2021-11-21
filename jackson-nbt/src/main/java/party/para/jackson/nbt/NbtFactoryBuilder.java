package party.para.jackson.nbt;

import com.fasterxml.jackson.core.TSFBuilder;

/**
 * NBT Factory Builder.
 *
 * This class is an implementation of {@link com.fasterxml.jackson.core.TSFBuilder}.
 * The detailed information about this class is available in {@link com.fasterxml.jackson.core.TSFBuilder}.
 */
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
