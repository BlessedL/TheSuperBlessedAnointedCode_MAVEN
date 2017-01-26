//package io.netty.example.oneHopSingleFileTransfer;
package nia.BlessedLavoneCodeWithProxyServer.filesenderdir;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;


/**
 * Created by lrodolph on 9/7/16.
 */

public class FileSenderInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                //new LengthFieldPrepender(8),
                new ChunkedWriteHandler(),
                new FileSenderHandler());
    }
}
