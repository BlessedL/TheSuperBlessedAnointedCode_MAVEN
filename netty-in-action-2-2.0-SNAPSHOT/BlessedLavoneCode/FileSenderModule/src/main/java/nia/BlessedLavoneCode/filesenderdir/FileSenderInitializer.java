//package io.netty.example.oneHopSingleFileTransfer;
package nia.BlessedLavoneCode.filesenderdir;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.codec.*;
import io.netty.handler.stream.*;
import io.netty.handler.codec.LengthFieldPrepender;
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
