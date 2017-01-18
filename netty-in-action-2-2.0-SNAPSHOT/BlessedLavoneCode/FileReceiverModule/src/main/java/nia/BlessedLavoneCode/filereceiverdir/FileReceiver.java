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
//package io.netty.example.proxy;
//package io.netty.example.oneHopSingleFileTransfer;
package nia.BlessedLavoneCode.filereceiverdir;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Scanner;
import java.util.logging.Logger;

public final class FileReceiver {

    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "4959"));
    private final static Logger logger = Logger.getLogger(FileReceiver.class.getName());

    public static void main(String[] args) throws Exception {

      //ServerHandlerHelper myServerHandlerHelper = new ServerHandlerHelper();

        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new FileReceiverInitializer())
             .childOption(ChannelOption.AUTO_READ, true); // when false have to manually call channel read, need to set to true to automatically have server read
             //.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
            // Start the server.
            ChannelFuture f = b.bind(LOCAL_PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
             logger.info("Started the Server on port " + LOCAL_PORT);


        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
