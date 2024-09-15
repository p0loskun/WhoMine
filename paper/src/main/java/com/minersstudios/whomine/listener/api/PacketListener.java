package com.minersstudios.whomine.listener.api;

import com.google.common.base.Joiner;
import com.minersstudios.whomine.WhoMine;
import com.minersstudios.whomine.api.packet.PacketType;
import com.minersstudios.whomine.packet.PaperPacketEvent;
import com.minersstudios.whomine.plugin.AbstractPluginComponent;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Set;

public abstract class PacketListener extends AbstractPluginComponent {
    private final Set<PacketType> sendWhiteList;
    private final Set<PacketType> receiveWhiteList;

    /**
     * Packet listener constructor
     *
     * @param plugin The plugin instance
     * @param first  The first packet type to listen to
     * @param rest   The other packet types to listen to (optional)
     * @see PacketType
     */
    protected PacketListener(
            final @NotNull WhoMine plugin,
            final @NotNull PacketType first,
            final PacketType @NotNull ... rest
    ) {
        super(plugin);

        this.sendWhiteList = new ObjectOpenHashSet<>();
        this.receiveWhiteList = new ObjectOpenHashSet<>();

        this.addPacketType(first);

        for (final var packetType : rest) {
            this.addPacketType(packetType);
        }
    }

    /**
     * @return Types of received packets listened to by this listener
     * @see PacketType
     */
    public final @NotNull @UnmodifiableView Set<PacketType> getReceiveWhiteList() {
        return Collections.unmodifiableSet(this.receiveWhiteList);
    }

    /**
     * @return Types of sent packets listened to by this listener
     * @see PacketType
     */
    public final @NotNull @UnmodifiableView Set<PacketType> getSendWhiteList() {
        return Collections.unmodifiableSet(this.sendWhiteList);
    }

    @Override
    public @NotNull String toString() {
        return this.getClass().getSimpleName() +
                "plugin=" + this.getPlugin() +
                ", sendWhiteList=[" + Joiner.on(", ").join(this.sendWhiteList) + ']' +
                ", receiveWhiteList=[" + Joiner.on(", ").join(this.receiveWhiteList) + ']' +
                '}';
    }

    @Override
    public void register() throws IllegalStateException {
        this.getPlugin().getListenerManager().registerPacket(this);
        this.onRegister();
    }

    /**
     * Packet receive event handler
     *
     * @param event The packet event
     */
    public void onPacketReceive(final @NotNull PaperPacketEvent event) {
        throw new UnsupportedOperationException("Packet receive not implemented for " + event.getPacketContainer().getType().getResourcedPath());
    }

    /**
     * Packet send event handler
     *
     * @param event The packet event
     */
    public void onPacketSend(final @NotNull PaperPacketEvent event) {
        throw new UnsupportedOperationException("Packet send not implemented for " + event.getPacketContainer().getType().getResourcedPath());
    }

    private void addPacketType(final @NotNull PacketType packetType) {
        if (packetType.isServerbound()) {
            this.receiveWhiteList.add(packetType);
        } else {
            this.sendWhiteList.add(packetType);
        }
    }
}
