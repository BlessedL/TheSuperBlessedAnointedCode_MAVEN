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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import java.util.logging.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ProxyServerFrontendHandler extends ChannelInboundHandlerAdapter {
    //Note for either ChannelInboundHadlerAdapter or SimpleChannelAdapter I must release the byteBuf

    //private final String remoteHost;
    //private final int remotePort;
    private volatile Channel outboundChannel;
    private volatile Channel inboundChannel;

    //Boolean variables for msg
    private boolean msgTypeSet,connectionMsgReceived,pathLengthSet,readInPath, connected;

    //ByteBuf variables for msg's
    private ByteBuf msgTypeBuf,pathSizeBuf,pathBuf;

    //Actual Variables holding the values of the msg
    private int msgType, pathLength, remotePort;
    private String thePath,theNodeToForwardTo, remoteHost;
    private Logger logger;
    private long threadId;

    public final int CONNECTION_MSG_TYPE = 1;
    public final int INT_SIZE = 4;


    //public ProxyServerFrontendHandler(String remoteHost, int remotePort) {
    public ProxyServerFrontendHandler() {
        msgTypeSet = false;
        connectionMsgReceived = false;
        pathLengthSet = false;
        readInPath = false;
        connected = false;
        //Set the variables
        inboundChannel = null;
        outboundChannel = null;
        msgTypeBuf = Unpooled.buffer(INT_SIZE);
        pathSizeBuf = Unpooled.buffer(INT_SIZE);
        pathBuf = null;
        logger = Logger.getLogger(ProxyServerFrontendHandler.class.getName());


    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        inboundChannel = ctx.channel();
        //final Channel inboundChannel = ctx.channel();
        threadId = Thread.currentThread().getId();

        //logger.info("******************************************************");
        //logger.info("ProxyServerFrontEndHandler: ThreadId = " + threadId);
        //logger.info("******************************************************");

        //Autoread is false so manually call channelRead
        //Call channelRead(final ChannelHandlerContext ctx, Object msg)
        inboundChannel.read();


    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg){
            try {
                //inbound channel is final so it can be passed to the anonymous function
                //final Channel inboundChannel = ctx.channel();
                //Cast the object as a ByteBuf
                ByteBuf in = (ByteBuf) msg;
                /////////////////////////////////////////////////////////////
                // The Outbound Channel is connected so just read from the //
                // Inbound Channel and forward to the Outbound Channel     //
                ////////////////////////////////////////////////////////////
                if (connected) {
                    if (outboundChannel != null) {
                        if (outboundChannel.isActive()) {
                            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) {
                                    if (future.isSuccess()) {
                                        // was able to flush out data, start to read the next chunk
                                        ctx.channel().read();
                                    } else {
                                        future.channel().close();
                                    }
                                }
                            });
                        }
                    }

                } else {
                    ///////////////////////////////////////////////////////////////////
                    //The Outbound Channel is not connected, read in the
                    //connection msg to get the IP Address of the node to forward to
                    while (in.readableBytes() >= 1) {
                        logger.info("in.readableBytes = " + in.readableBytes());
                        //Read in Msg Type
                        if (!msgTypeSet) {
                            msgTypeBuf.writeBytes(in, ((msgTypeBuf.writableBytes() >= in.readableBytes()) ? in.readableBytes() : msgTypeBuf.writableBytes()));
                            //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                            if (msgTypeBuf.readableBytes() >= 4) {
                                msgType = msgTypeBuf.getInt(msgTypeBuf.readerIndex());//Get Size at index = 0;
                                msgTypeSet = true;
                                String msgTypeString = ((msgType == CONNECTION_MSG_TYPE) ? "CONNECTION MSG TYPE" : " FILE MSG TYPE ");
                                logger.info("ProxyServerFrontendHandler(" + threadId + "): channelRead: READ IN THE MSG Type, Msg Type = " + msgTypeString);
                            }
                        } else if (msgType == CONNECTION_MSG_TYPE) {
                            System.err.printf("\n **********FileReceiverHandler(%d): Connection MSG Type Received **********\n\n", threadId);
                            if (!connectionMsgReceived) {
                                //Process Msg Type
                                logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: msg.readableBytes(" + in.readableBytes() + ") >= 1");
                                //Read in Path Size
                                if (!pathLengthSet) {
                                    //if pathSizeBuf's writable bytes (number of bytes that can be written to - (Capacity - Writer index) is greater than or equal to in's readable bytes then set the length to in's readable bytes
                                    //else set the length to the pathSizeBuf writable bytes
                                    pathSizeBuf.writeBytes(in, ((pathSizeBuf.writableBytes() >= in.readableBytes()) ? in.readableBytes() : pathSizeBuf.writableBytes()));
                                    logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg : pathSizeBuf.readableBytes() =  " + pathSizeBuf.readableBytes());
                                    if (pathSizeBuf.readableBytes() >= 4) {
                                        pathLength = pathSizeBuf.getInt(pathSizeBuf.readerIndex());//Get Size at index = 0;
                                        pathLengthSet = true;
                                        pathBuf = ctx.alloc().buffer(pathLength);
                                        logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE PATH LENGTH: " + pathLength);
                                    }
                                    //Read in Path
                                } else {
                                    if (!readInPath) {
                                        pathBuf.writeBytes(in, ((pathBuf.writableBytes() >= in.readableBytes()) ? in.readableBytes() : pathBuf.writableBytes()));
                                        if (pathBuf.readableBytes() >= pathLength) {
                                            //Read in path in ipFormat
                                            readInPath = true;
                                            //convert the data in pathBuf to an ascii string
                                            thePath = pathBuf.toString(Charset.forName("US-ASCII"));
                                            logger.info("ProxyServer:ChannelRead: The Path = " + thePath);
                                            //the path is a string of ip addresses and ports separated by a comma, it doesn't include the source node (the first node in the path)
                                            //if path is WS5,WS7,WS12 with the below ip address and port
                                            //192.168.0.2:4959.192.168.0.1:4959,192.168.1.2:4959
                                            //then only the ip addresses & ports of WS7, WS12 is sent, note this proxy server is WS7
                                            //So the path = 192.168.0.1:4959,192.168.1.2:4959
                                            //parse out the first ip address
                                            String[] tokens = thePath.split("[,]+");
                                            logger.info("ProxyServer:ChannelRead: tokens[0] = " + tokens[0]);
                                            logger.info("ProxyServer:ChannelRead: tokens[1] = " + tokens[1]);
                                            theNodeToForwardTo = tokens[1]; // = 192.168.0.1:4959
                                            //Separate the ip address from the port
                                            String[] ip_and_port = theNodeToForwardTo.split("[:]+");
                                            logger.info("ProxyServer:ChannelRead: ip_and_port[0] = " + ip_and_port[0]);
                                            logger.info("ProxyServer:ChannelRead:  ip_and_port[1]= " + ip_and_port[1]);
                                            remoteHost = ip_and_port[0]; //"192.168.0.1"
                                            logger.info("ProxyServer:ChannelRead: Remote Host = " + remoteHost);
                                            remotePort = new Integer(ip_and_port[1]).intValue(); //=4959
                                            //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE PATH: " + thePath);
                                            logger.info(" **************ProxyServer:ChannelRead:( ThreadId:" + threadId + ") FINISHED READING IN THE PATH " + thePath + " in.readableBytes = " + in.readableBytes() + "********************");
                                            //Completely read in the path, now connect to the Remote Host
                                            if (!connected) {
                                                logger.info("ProxyServer:ChannelRead: Not Connected to File Receiver yet, Starting the CONNECTION PROCESS....");
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
                                                    ProxyServerFrontendHandler myProxServerFrontendHandler;
                                                    Channel myInboundChannel;
                                                    Channel myOutboundChannel;

                                                    public ChannelFutureListener init(ProxyServerFrontendHandler aProxyServerFrontendHandler, Channel anInboundChannel, Channel anOutboundChannel) {
                                                        myProxServerFrontendHandler = aProxyServerFrontendHandler;
                                                        myInboundChannel = anInboundChannel;
                                                        myOutboundChannel = anOutboundChannel;
                                                        return this;
                                                    }

                                                    @Override
                                                    public void operationComplete(ChannelFuture future) {
                                                        if (future.isSuccess()) {
                                                            // connection complete start to read first data
                                                            //inboundChannel.read();
                                                            //Send data through outbound channel and then call read on the inbound channel
                                                            myProxServerFrontendHandler.setConnection(true);
                                                            myProxServerFrontendHandler.sendConnectionMsg(myInboundChannel, myOutboundChannel);
                                                            // inboundChannel.read() calls the below channelRead(final ChannelHandlerContext ctx, Object msg) method
                                                        } else {
                                                            // Close the connection if the connection attempt has failed.
                                                            inboundChannel.close();
                                                        }
                                                    }
                                                }.init(this, inboundChannel, outboundChannel));
                                            }//End (!connected)
                                        }//if read in all the path bytes
                                    } //end read in the path
                                }//End else
                            }//End connection msg received
                        }//End CONNECTION_TYPE = MSG_TYPE
                        else{
                            logger.info("ProxyServer Error: MSG_TYPE NOT EQUAL TO CONNECTION MSG TYPE");
                        }
                    }//End While
                }

                //Release the ByteBuf
                in.release();

                //Should I release the other ByteBufs
            }catch(Exception e){
                System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
                e.printStackTrace();
            }//End Catch


    }//End Channel Read


    /*
       After extracting
     */

    public void setConnection(boolean aVal){
        logger.info("Set Conntection to " + aVal);
        connected = aVal;
    }

    public boolean getConnected(){
        return connected;
    }

    public void sendConnectionMsg(Channel anInboundChannel, Channel anOutboundChannel){
        try {
            logger.info("ProxyServerFrontEndHandler: sendConnectionMsg: About to send new connection msg with the new path");
            //If we go here then set connected = true
            connected = true;
            if (anOutboundChannel != null) {
                //Send the msg type
                ByteBuf theMsgTypeBuf = Unpooled.copyInt(msgType);
                //send the new path String with out the new source node
                //Path Received - 192.168.0.1:4959,192.168.1.2:4959
                int anIndex = thePath.indexOf(',');
                String newPathToSend = null; //since this is a proxy server, the path will always consists of at least one path - the receiver path
                if (anIndex > 0) { //If this is the dest node, then the file path is attached, remove the file path from node
                    newPathToSend = thePath.substring(anIndex + 1, thePath.length());
                    logger.info("ProxyServerFrontEndHandler: sendConnectionMsg: the old path = " + thePath +", the New Path to Send = " + newPathToSend);
                    //System.out.println("TransferContext: getNodeToForwardTo: node to forward to had a file path, it is now removed and the node is: " + theNodeToForwardTo);
                }

                if (newPathToSend !=null){
                    //Get the New File Path In Bytes
                    byte[] newPathToSendInBytes = newPathToSend.getBytes();
                    //Get the length of theNewFilePath
                    int newPathToSendInBytesSize = newPathToSendInBytes.length;
                    //Copy the New File Path length to the ByteBuf
                    ByteBuf newPathToSendInBytesSizeBuf = Unpooled.copyInt(newPathToSendInBytesSize);
                    //Copy the theDestFilePathInBytes to the Byte Buf
                    ByteBuf newPathToSendInBytesBuf = Unpooled.copiedBuffer(newPathToSendInBytes);
                    //Msg Type
                    anOutboundChannel.write(theMsgTypeBuf); //OR anOutboundChannel.write((Object)theMsgTypeBuf);
                    logger.info("ProxyServerFrontEndHandler: SendConnectionMsg: Wrote the Msg Type: " + msgType);
                    //New Path Size
                    anOutboundChannel.write(newPathToSendInBytesSizeBuf); //OR anOutboundChannel.write((Object)newPathToSendInBytesSizeBuf);
                    logger.info("ProxyServerFrontEndHandler: SendConnectionMsg: Wrote the New Path: " + newPathToSend);
                    //New Path
                    anOutboundChannel.write(newPathToSendInBytesBuf); //OR anOutboundChannel.write((Objects)newPathToSendInBytesBuf);
                    anOutboundChannel.flush();

                }

                //Now Call the Channel Read Method which will call
                inboundChannel.read(); // this calls --> channelRead(final ChannelHandlerContext ctx, Object msg) {

            }
        }catch(Exception e){
            e.printStackTrace();
            System.err.printf(e.getMessage());

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
