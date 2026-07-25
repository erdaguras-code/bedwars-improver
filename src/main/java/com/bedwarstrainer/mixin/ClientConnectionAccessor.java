package com.bedwarstrainer.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Sahte baglantinin (FakeClientConnection) kanalini EmbeddedChannel yapip
 * isOpen()'in true donmesini saglamak icin. Carpet ClientConnectionInterface
 * ile ayni isi yapar.
 *
 * ⚠️ CI-DOGRULA: ClientConnection'daki alan adi Yarn 1.21.1'de 'channel' olmali.
 */
@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor("channel")
    void bwt$setChannel(Channel channel);
}
