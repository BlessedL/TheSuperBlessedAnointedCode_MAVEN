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
//package nia.BlessedLavoneCodeWithEverything.proxyserverdir;
//package nia.BlessedLavoneCodeWithEverything_noChunkWriteHandler.filereceiverdir;
package nia.BlessedLavoneCodeWithEpoll_andChunkWriteHandler.proxyserverdir;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.EpollEventLoopGroup;
//import io.netty.channel.socket.nio.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
//import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.channel.FixedRecvByteBufAllocator;

public final class ProxyServer {

    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "4959"));
    static final String REMOTE_HOST = System.getProperty("remoteHost", "192.168.1.2"); //Connect to WS12
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "4959"));

    public static void main(String[] args) throws Exception {
        System.err.println("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST + ':' + REMOTE_PORT + " ...");

        // Configure the bootstrap.
        //EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EpollEventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        //EventLoopGroup workerGroup = new EpollEventLoopGroup();
        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup();
        workerGroup.setIoRatio(100);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(EpollServerSocketChannel.class)
           //  .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ProxyServerInitializer(REMOTE_HOST, REMOTE_PORT))
             .childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED)
             .childOption(EpollChannelOption.AUTO_READ, false)
             .childOption(EpollChannelOption.SO_RCVBUF, 100 * 1024 * 1024)
             .childOption(EpollChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(100*1024*1024))
             .childOption(EpollChannelOption.TCP_NODELAY, true)
             .bind(LOCAL_PORT).sync().channel().closeFuture().sync(); //closeFuture().synch() keeps the channel up, until it is closed
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
