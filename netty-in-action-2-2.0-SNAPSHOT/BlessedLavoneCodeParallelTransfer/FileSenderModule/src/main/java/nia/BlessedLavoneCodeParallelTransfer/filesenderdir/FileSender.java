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
package nia.BlessedLavoneCodeParallelTransfer.filesenderdir;

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
import java.io.File;
import java.nio.channels.FileChannel;
import java.lang.*;
import java.util.*;
import java.io.*;

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

        //Parallel Num
        int myParallelNum = 1;

        //File Name
        String fileName = "/home/lrodolph/100MB_File.dat";
        String fileRequest = "transfer N0/home/lrodolph/100MB_File.dat N1/home/lrodolph/100MB_File_Copy.dat";

        //Get the file & File Length
        File theFile = new File(fileName);
        //Get the File Length
        FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();

        long length = theFileChannel.size();
        long offSet = 0;
        long remainingFragmentSize = length;
        long currentFragmentSize = 0;
        int parallel_counter = 0;
        long leftOverBytes = 0;

        //Create parallel connection
        //Pass in the fragment Size,

        //get the file fragment size that will be sent through each parallel TCP Stream
        currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));
        leftOverBytes = length - (currentFragmentSize * myParallelNum);
        logger.info("FileSenderHandler:Active: File Length = " + length + ", Current Fragment Size = " + currentFragmentSize + " leftOverBytes = " + leftOverBytes);
        ////////////////////////////////////
        //Connect the parallel data channels
        ////////////////////////////////////
        int dataChannelId = 0;
        // Configure the File Sender
        EventLoopGroup group = new NioEventLoopGroup();
        ArrayList<ChannelFuture> channelFutureList = new ArrayList<ChannelFuture>();
        try {
        for (int i =0; i<myParallelNum; i++) {
            if ((parallel_counter + 1) >= myParallelNum) {
                currentFragmentSize += leftOverBytes;
            }
            dataChannelId++;
            logger.info("FileSender: DataChannel: " + dataChannelId + " ,offset = " + offSet + " Current Fragment Size " + currentFragmentSize);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new FileSenderInitializer(fileRequest, offSet, currentFragmentSize, dataChannelId  ));

            //Actually connect to the proxyServer and use sync to wait until the connection is completed, but this isn't really asynchronous until we use listeners to wait, since it is blocking
            //Using "sync" blocks until the channel is connected.
            //What does it block? the current thread of execution? meaning it doesn't move to the next statement until
            //the channel is connected. Which means until the (the channelFuture has returned that it is connected)
            ChannelFuture f = b.connect(HOST, PORT).sync();
            channelFutureList.add(f);

            //I have to test to see if the channels are sending data one at a time and one after the other
            //or actuall in parallel. I can step through the debuger and see. Also put print statements in the receiver handler
            //and have the file sender send the channel id

            //Channel theDataChannel = f.channel();

            //Wait until the connection is closed
            //f.channel().closeFuture().sync();
            //logger.info("FileSender: CLOSED CONNECTION TO WS7");
            //logger.info("FileSender: dataChannel " + dataChannelId + " Reached the statement that will updatethe offset, the new offset = offset(" + offSet + ") + currentFragmentSize(" + currentFragmentSize + ")");
            offSet+=currentFragmentSize;
            //parallel_counter++;

        }


            Iterator<ChannelFuture>   channelFutureListIterator = channelFutureList.iterator();
            //Iterate through File Request List
            int counter = 1;
            while (channelFutureListIterator.hasNext()) {
                ChannelFuture aChannelFuture = channelFutureListIterator.next();
                //Wait until the connection is closed
                /*
                 http://stackoverflow.com/questions/24237142/netty-app-hangs-when-i-try-to-close-a-io-netty-channel-channel-using-closefuture
                 closeFuture.sync() is not for closing the Channel but rather allows you to block until the Channel is closed. For closing the Channel you would call Channel.close().

                 http://stackoverflow.com/questions/41505852/netty-closefuture-sync-channel-blocks-rest-api
                 So you might find this code in various example because generally you start the server (bind) in the main thread or so, and then if you don't wait for something,
                 the main thread will end up, giving your JVM finishing, and therefore your server to stop immediately after starting.
                 So basically I need to use  aChannelFuture.channel().closeFuture().sync() to keep this main program (thread) up, or it will finish and
                 shut down the handlers too, since the handler is also running from the main thread

                  I have to testt
                 */
                aChannelFuture.channel().closeFuture().sync();
                //aChannelFuture.channel().closeFuture();
                //Add the File Reuest List to the Path's File Request List
                logger.info("Waiting for data channel "+ counter + " to close");
                counter++;
            }




            logger.info("File Sender created the connections and is now outside of the for loop");
            // Wait until the connection is closed.
            //f.channel().closeFuture().sync();
            logger.info("FileSender: CLOSED CONNECTION TO WS7");
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
