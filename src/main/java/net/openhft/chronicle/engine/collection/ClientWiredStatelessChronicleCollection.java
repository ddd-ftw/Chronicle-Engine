/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.engine.collection;

import net.openhft.chronicle.network.connection.AbstractStatelessClient;
import net.openhft.chronicle.network.connection.TcpChannelHub;
import net.openhft.chronicle.wire.ValueIn;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.openhft.chronicle.engine.collection.CollectionWireHandler.EventId;
import static net.openhft.chronicle.engine.collection.CollectionWireHandler.EventId.*;
import static net.openhft.chronicle.network.connection.CoreFields.reply;

public class ClientWiredStatelessChronicleCollection<U, E extends Collection<U>> extends
        AbstractStatelessClient<EventId> implements Collection<U> {

    @NotNull
    private final Function<ValueIn, U> consumer;
    @NotNull
    private final Supplier<E> factory;

    public ClientWiredStatelessChronicleCollection(@NotNull final TcpChannelHub hub,
                                                   @NotNull Supplier<E> factory,
                                                   @NotNull Function<ValueIn, U> wireToSet,
                                                   @NotNull String csp, long cid) {
        super(hub, cid, csp);
        this.consumer = wireToSet;
        this.factory = factory;
    }

    @Override
    public int size() {
        return proxyReturnInt(size);
    }

    @Override
    public boolean isEmpty() {
        return proxyReturnBoolean(isEmpty);
    }

    @Override
    public boolean contains(Object o) {
        return proxyReturnBooleanWithArgs(contains, o);
    }

    @Override
    @NotNull
    public Iterator<U> iterator() {
        // todo improve numberOfSegments
        final int numberOfSegments = proxyReturnUint16(EventId.numberOfSegments);

        // todo iterate the segments for the moment we just assume one segment
        return segmentSet(1).iterator();
    }

    /**
     * gets the iterator for a given segment
     *
     * @param segment the maps segment number
     * @return and iterator for the {@code segment}
     */
    @NotNull
    private E segmentSet(int segment) {

        return proxyReturnWireConsumerInOut(iterator, reply, valueOut -> valueOut.uint16(segment),
                read -> {
                    final E e = factory.get();
                    read.sequence(e, (e2, s) -> {
                        while (read.hasNextSequenceItem())
                            e2.add(consumer.apply(read));
                    });
                    return e;
                });
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return asCollection().toArray();
    }

    @NotNull
    private E asCollection() {
        final E e = factory.get();
        final int numberOfSegments = proxyReturnUint16(EventId.numberOfSegments);

        for (long j = 0; j < numberOfSegments; j++) {
            final long i = j;
            proxyReturnWireConsumerInOut(iterator, reply, valueOut -> valueOut.uint16(i),
                    read -> read.sequence(e, (e2, r) -> {
                        while (r.hasNextSequenceItem()) {
                            e2.add(consumer.apply(r));
                        }
                    }));
        }
        return e;
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] array) {
        return asCollection().toArray(array);
    }

    @Override
    public boolean add(U u) {
        return proxyReturnBoolean(add);
    }

    @Override
    public boolean remove(Object o) {
        return proxyReturnBooleanWithArgs(remove, o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return proxyReturnBooleanWithSequence(containsAll, c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends U> c) {
        return proxyReturnBooleanWithSequence(addAll, c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return proxyReturnBooleanWithSequence(retainAll, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return proxyReturnBooleanWithSequence(removeAll, c);
    }

    @Override
    public void clear() {
        proxyReturnVoid(clear);
    }
}