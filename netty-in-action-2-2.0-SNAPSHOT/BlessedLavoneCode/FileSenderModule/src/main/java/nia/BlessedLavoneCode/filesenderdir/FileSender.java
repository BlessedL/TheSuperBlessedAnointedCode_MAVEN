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
//package io.netty.example.oneHopSingleFileTransfer;
package nia.BlessedLavoneCode.filesenderdir;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import io.netty.buffer.*;
import io.netty.handler.stream.ChunkedFile;
import java.io.RandomAccessFile;
import java.util.logging.*;

/*
  Write contents to a file when reading, after reading length amount of bytes
 */
public final class FileSender {

    //static final String HOST = System.getProperty("host", "127.0.0.1");
    //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

  static final String HOST = System.getProperty("host", "192.168.0.1");
  //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
  static final int PORT = Integer.parseInt(System.getProperty("port","4959"));
    static final Logger logger = Logger.getLogger(FileSender.class.getName());
    //Logger logger = Logger.getLogger(this.getClass().getName());




    public static void main(String[] args) throws Exception {

        //I can also start the server that will accept commands from a client
        //For now I can read in parameters from the command line.
        //But I can

        // Configure the client/ File Sender
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new FileSenderInitializer());

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();



            // Wait until the connection is closed.
            //closeFuture().sync() keeps the connection up until (control c is typed at terminal to close the connection)
            f.channel().closeFuture().sync();
            logger.info("FileSender: CLOSED CONNECTION TO WS7");
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
