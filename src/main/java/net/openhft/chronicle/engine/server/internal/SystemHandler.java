package net.openhft.chronicle.engine.server.internal;

import net.openhft.chronicle.network.ClientClosedProvider;
import net.openhft.chronicle.network.api.session.SessionDetailsProvider;
import net.openhft.chronicle.network.connection.CoreFields;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireKey;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * @author Rob Austin.
 */
public class SystemHandler extends AbstractHandler implements ClientClosedProvider {
    private final StringBuilder eventName = new StringBuilder();
    private SessionDetailsProvider sessionDetails;
    private volatile boolean hasClientClosed;

    void process(@NotNull final WireIn inWire,
                 @NotNull final WireOut outWire, final long tid,
                 @NotNull final SessionDetailsProvider sessionDetails) {
        this.sessionDetails = sessionDetails;
        setOutWire(outWire);
        dataConsumer.accept(inWire, tid);
    }

    @NotNull
    private final BiConsumer<WireIn, Long> dataConsumer = (inWire, tid) -> {

        eventName.setLength(0);
        final ValueIn valueIn = inWire.readEventName(eventName);

        if (EventId.userid.contentEquals(eventName)) {
            this.sessionDetails.setUserId(valueIn.text());
            return;
        }

        //noinspection ConstantConditions
        outWire.writeDocument(true, wire -> outWire.writeEventName(CoreFields.tid).int64(tid));

        writeData(inWire.bytes(), out -> {

            if (EventId.heartbeat.contentEquals(eventName))
                outWire.write(EventId.heartbeatReply).int64(valueIn.int64());

            else if (EventId.onClientClosing.contentEquals(eventName)) {
                hasClientClosed = true;
                outWire.write(EventId.onClosingReply).text("");
            }
        });
    };

    public enum EventId implements WireKey {
        heartbeat,
        heartbeatReply,
        onClientClosing,
        onClosingReply,
        userid
    }

    /**
     * @return {@code true} if the client has intentionally closed
     */
    @Override
    public boolean hasClientClosed() {
        return hasClientClosed;
    }
}
