/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package nia.BlessedLavoneCodeWithProxyServer.proxyserverdir;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;

public class ProxyServerFrontendHandler extends ChannelInboundHandlerAdapter {
    //Note for either ChannelInboundHadlerAdapter or SimpleChannelAdapter I must release the byteBuf

    private final String remoteHost;
    private final int remotePort;

    private volatile Channel outboundChannel;

    public ProxyServerFrontendHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
         .channel(ctx.channel().getClass())
         .handler(new ProxyServerBackendHandler(inboundChannel))
         .option(ChannelOption.AUTO_READ, false); //Autoread need to be false, so I can read only when data is available
        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();
        //Note what keeps the outbound channel up, there is no f.channel().closeFuture().sync() after the connection is made
        // to keep the channel up,  so what keeps the channel up. Awe, in the below channel read method, the read method calls it self recursively
        //thus keeping the channel up. When does the channel close, is it until it can't read any more or when there is a problem reading.
        // I think when there is a problem reading the channel closes, but even when there is nothing to read, I think the proxy server just waits until there is something to read.
        // I am not sure if there is a time out value specifying the maximum time to wait to read data.
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                    // inboundChannel.read() calls the below channelRead(final ChannelHandlerContext ctx, Object msg) method
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        //Note: I need to make the parameter ChannelHandlerContext final, so it can be passed to the below anonymous function.
        //Anonymous functions can only recognize final variables from the parent class
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read(); //does this initiate this channelRead or another one which eventually calls this one?
                        //The above ctx.channel().read() call this read function
                    } else {
                        future.channel().close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
