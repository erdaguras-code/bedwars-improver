package com.bedwarstrainer.pvp.fake;

import com.bedwarstrainer.mixin.ClientConnectionAccessor;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;

/**
 * Sahte oyuncu icin, giden paketleri hicbir yere yollamayan sahte baglanti.
 * Carpet'in FakeClientConnection'inin Yarn 1.21.1 karsiligi.
 *
 * ⚠️ CI-DOGRULA: Yarn 1.21.1'de override edilecek metot imzalari degisebilir.
 *   'send' overload'lari ve varsa setupInboundProtocol / disconnect burada
 *   no-op olmali. Derleme hatasi cikarsa Carpet'in ilgili surum dosyasina bak.
 */
public class FakeClientConnection extends ClientConnection {
    public FakeClientConnection(NetworkSide side) {
        super(side);
        // isOpen() true donsun diye sahte bir kanal ata
        ((ClientConnectionAccessor) this).bwt$setChannel(new EmbeddedChannel());
    }

    @Override
    public void send(Packet<?> packet) {
        // no-op: sahte oyuncunun istemcisi yok
    }
}
