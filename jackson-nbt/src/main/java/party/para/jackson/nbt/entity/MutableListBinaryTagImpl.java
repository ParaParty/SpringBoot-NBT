package party.para.jackson.nbt.entity;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *  Mutable implementation of {@link ListBinaryTag}
 */
public class MutableListBinaryTagImpl implements ListBinaryTag {
    private final ArrayList<BinaryTag> tags;
    private BinaryTagType<? extends BinaryTag> elementType;

    public MutableListBinaryTagImpl() {
        this.tags = new ArrayList<>();
        this.elementType = BinaryTagTypes.END;
    }

    public MutableListBinaryTagImpl(final BinaryTagType<? extends BinaryTag> elementType, final List<BinaryTag> tags) {
        this.tags = new ArrayList<>(tags);
        this.elementType = elementType;
    }

    private void updateElementType() {
        if (elementType == BinaryTagTypes.END && !tags.isEmpty()) {
            this.elementType = tags.get(0).type();
        }
    }

    @Override
    public @NotNull BinaryTagType<? extends BinaryTag> elementType() {
        updateElementType();
        return this.elementType;
    }

    @Override
    public int size() {
        return this.tags.size();
    }

    @Override
    public @NotNull BinaryTag get(@Range(from = 0, to = Integer.MAX_VALUE) final int index) {
        return this.tags.get(index);
    }

    @Override
    public @NotNull ListBinaryTag set(final int index, final @NotNull BinaryTag newTag, final @Nullable Consumer<? super BinaryTag> removed) {
        final BinaryTag oldTag = tags.set(index, newTag);
        if (removed != null) {
            removed.accept(oldTag);
        }
        return this;
    }

    @Override
    public @NotNull ListBinaryTag remove(final int index, final @Nullable Consumer<? super BinaryTag> removed) {
        final BinaryTag oldTag = tags.remove(index);
        if (removed != null) {
            removed.accept(oldTag);
        }
        return this;
    }

    @Override
    public @NotNull ListBinaryTag add(final BinaryTag tag) {
        noAddEnd(tag);
        updateElementType();
        if (this.elementType != BinaryTagTypes.END) {
            mustBeSameType(tag, this.elementType);
        }
        tags.add(tag);
        return this;
    }

    @Override
    public @NotNull ListBinaryTag add(final Iterable<? extends BinaryTag> tagsToAdd) {
        if (tagsToAdd instanceof Collection<?> && ((Collection<?>) tagsToAdd).isEmpty()) {
            return this;
        }
        for (final BinaryTag tag : tagsToAdd) {
            add(tag);
        }
        return this;
    }

    // An end tag cannot be an element in a list tag
    static void noAddEnd(final BinaryTag tag) {
        if (tag.type() == BinaryTagTypes.END) {
            throw new IllegalArgumentException(String.format("Cannot add a %s to a %s", BinaryTagTypes.END, BinaryTagTypes.LIST));
        }
    }

    // Cannot have different element types in a list tag
    static BinaryTagType<?> mustBeSameType(final Iterable<? extends BinaryTag> tags) {
        BinaryTagType<?> type = null;
        for (final BinaryTag tag : tags) {
            if (type == null) {
                type = tag.type();
            } else {
                mustBeSameType(tag, type);
            }
        }
        return type;
    }

    // Cannot have different element types in a list tag
    static void mustBeSameType(final BinaryTag tag, final BinaryTagType<? extends BinaryTag> type) {
        if (tag.type() != type) {
            throw new IllegalArgumentException(String.format("Trying to add tag of type %s to list of %s", tag.type(), type));
        }
    }

    @Override
    public @NotNull Stream<BinaryTag> stream() {
        return this.tags.stream();
    }

    @Override
    public Iterator<BinaryTag> iterator() {
        final Iterator<BinaryTag> iterator = this.tags.iterator();
        return new Iterator<BinaryTag>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public BinaryTag next() {
                return iterator.next();
            }

            @Override
            public void forEachRemaining(final Consumer<? super BinaryTag> action) {
                iterator.forEachRemaining(action);
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super BinaryTag> action) {
        this.tags.forEach(action);
    }

    @Override
    public Spliterator<BinaryTag> spliterator() {
        return Spliterators.spliterator(this.tags, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    @Override
    public boolean equals(final Object that) {
        return this == that || (that instanceof MutableListBinaryTagImpl && this.tags.equals(((MutableListBinaryTagImpl) that).tags));
    }

    @Override
    public int hashCode() {
        return tags.hashCode();
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(
                ExaminableProperty.of("tags", this.tags),
                ExaminableProperty.of("type", this.elementType)
        );
    }
}
