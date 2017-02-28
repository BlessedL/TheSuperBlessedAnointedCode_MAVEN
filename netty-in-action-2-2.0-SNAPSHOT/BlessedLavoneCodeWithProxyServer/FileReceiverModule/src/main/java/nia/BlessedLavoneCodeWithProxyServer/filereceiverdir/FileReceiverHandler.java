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
package nia.BlessedLavoneCodeWithProxyServer.filereceiverdir;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
//import io.netty.util.ReferenceCountUtil;
import java.nio.charset.Charset;

public class FileReceiverHandler extends SimpleChannelInboundHandler<ByteBuf> {
  public int testAnonymousFunctionVar;
  private String remoteHost;
  private int remotePort;
  private Logger logger;
  private volatile Channel outboundChannel;
  private enum ProxyServerState {
    INITIALIZE, CONNECT, WAITING_TO_SEND_CONNECT_ACK, CONNECT_ERROR, TRANSFER
  }
  private ProxyServerState proxyServerState;

  public final int CONTROL_CHANNEL_TYPE = 0;
  public final int DATA_CHANNEL_TYPE = 1;
  public int INT_SIZE = 4;
  public int LONG_SIZE = 8;

  //private enum State myState;
  private int channelReadCounter;
  private int pathLength,aliasPathLength;
  private int myConnectionType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum;
  private int fileNameStringSize, fileId;

  private ByteBuf aliasPathSizeBuf;
  private ByteBuf aliasPathBuf,connectionTypeBuf,controlChannelIdBuf,dataChannelIdBuf;
  private ByteBuf parallelNumBuf,concurrencyNumBuf;
  private ByteBuf fileNameStringSizeBuf, fileNameStringBuf, offSetBuf, fragmentLengthBuf, fileIdBuf;
  private boolean aliasPathLengthSet,pathStringSet;
  private boolean readInAliasPath,readInConnectionType;
  private boolean readInDataChannelId, readInControlChannelId,readInParallelNum,readInConcurrencyNum;
  private boolean fileNameStringSizeSet, readInFileNameString, readInOffset, readInFragmentLength, readInFileId, readInFileFragment;
  private boolean timeStartedSet, timeEndedSet;
  private String pathString, theAliasPath;
  private byte[] pathBytes;
  private boolean sentConnectionMsg, canIconnectToRemoteNode, amIconnectedToRemoteHost;
  private boolean canIconnectToRemoteHost;
  private String  theConnectionTypeString;
  private String thefileName;
  private File emptyFile;
  private RandomAccessFile f;
  private FileChannel fc;
  private long bytesRead, currentOffset, fragmentLength, remainingFragmentLength, timeStarted,timeEnded;
  private int currentTotalFileBytesWrote;

  //Boolean variables for msg
  private boolean msgTypeSet,connectionMsgReceived,pathLengthSet,readInPath, connected;

  //ByteBuf variables for msg's
  private ByteBuf msgTypeBuf,pathSizeBuf,pathBuf;

  //Actual Variables holding the values of the msg
  private int msgType;
  private String thePath,theNodeToForwardTo;
  //private Logger logger;
  private long threadId;

  public final int CONNECTION_MSG_TYPE = 1;
  public final int CONNECTION_MSG_ACK_TYPE = 1;


    public FileReceiverHandler() throws Exception {
      testAnonymousFunctionVar = 1;
      this.remoteHost = null;
      this.remotePort = -1;
      proxyServerState = ProxyServerState.INITIALIZE;
      logger = Logger.getLogger(FileReceiverHandler.class.getName());
      channelReadCounter = 0;
      pathLength = -1; aliasPathLength = -1;
      pathLengthSet = false; aliasPathLengthSet = false; pathStringSet = false;
      readInPath = false; readInAliasPath = false; readInConnectionType = false;
      readInControlChannelId = false; readInDataChannelId = false; readInParallelNum = false;
      readInConcurrencyNum = false;
      fileNameStringSizeSet = false; readInFileNameString = false; readInOffset = false; readInFragmentLength = false;
      readInFileId = false; readInFileFragment = false;
      theAliasPath = null;
      pathBytes = null;
      connectionMsgReceived = false; sentConnectionMsg = false;canIconnectToRemoteNode = false;
      canIconnectToRemoteHost = false;
      amIconnectedToRemoteHost = false;
      theNodeToForwardTo = null;thePath = null;
      myConnectionType = -1; myControlChannelId = -1; myDataChannelId = -1;
      myParallelNum = -1; myConcurrencyNum = -1;
      fileNameStringSize = -1;
      connectionTypeBuf = Unpooled.buffer(INT_SIZE);
      controlChannelIdBuf = Unpooled.buffer(INT_SIZE);
      dataChannelIdBuf = Unpooled.buffer(INT_SIZE);
      parallelNumBuf = Unpooled.buffer(INT_SIZE);
      concurrencyNumBuf = Unpooled.buffer(INT_SIZE);
      pathSizeBuf = Unpooled.buffer(INT_SIZE);
      aliasPathSizeBuf = Unpooled.buffer(INT_SIZE);
      fileNameStringSizeBuf = Unpooled.buffer(INT_SIZE);
      fragmentLengthBuf = Unpooled.buffer(LONG_SIZE);
      fileIdBuf = Unpooled.buffer(INT_SIZE);
      fileNameStringBuf = null;
      offSetBuf = Unpooled.buffer(LONG_SIZE);
      msgTypeBuf = Unpooled.buffer(INT_SIZE);
      thefileName = null;
      theConnectionTypeString = null;
      emptyFile = null;
      f = null;
      fc = null;
      fileId = -1;
      bytesRead = 0; fragmentLength = -1; currentOffset = -1; remainingFragmentLength = -1;
      timeStarted = -1; timeEnded = -1;
      timeStartedSet = false; timeEndedSet = false;
      currentTotalFileBytesWrote = 0;
      msgTypeSet = false;
      msgType = -1;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

      proxyServerState = ProxyServerState.CONNECT;
      logger.info("FileReceiverServer is active and is now in the CONNECT state");
    }

    @Override
    //public void channelRead0(final ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    /*
       Make sure after I read the connection ack msg, I set the channel pipeline to use the frame decoder
       also based on the frame decoder bytes read: Get the File Header, then get the file (File Fragment) it self
     */
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
      try {
        while (msg.readableBytes() >= 1){
          //Read in Msg Type
          if (!msgTypeSet) {
            msgTypeBuf.writeBytes(msg, ((msgTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : msgTypeBuf.writableBytes()));
            //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
            if (msgTypeBuf.readableBytes() >= 4) {
              msgType = msgTypeBuf.getInt(msgTypeBuf.readerIndex());//Get Size at index = 0;
              msgTypeSet = true;
              String msgTypeString = ((msgType == CONNECTION_MSG_TYPE) ? "CONNECTION MSG TYPE" : " FILE MSG TYPE ");
              logger.info("ProxyServerFrontendHandler(" + threadId + "): channelRead: READ IN THE MSG Type, Msg Type = " + msgTypeString);
            }
          } else {
            if (msgType == CONNECTION_MSG_TYPE) {
              System.err.printf("\n **********FileReceiverHandler(%d): Connection MSG Type Received **********\n\n", threadId);
              if (!connectionMsgReceived) {
                //Process Msg Type
                logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: msg.readableBytes(" + msg.readableBytes() + ") >= 1");
                //Read in Path Size
                if (!pathLengthSet) {
                  //if pathSizeBuf's writable bytes (number of bytes that can be written to - (Capacity - Writer index) is greater than or equal to in's readable bytes then set the length to in's readable bytes
                  //else set the length to the pathSizeBuf writable bytes
                  pathSizeBuf.writeBytes(msg, ((pathSizeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : pathSizeBuf.writableBytes()));
                  logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg : pathSizeBuf.readableBytes() =  " + pathSizeBuf.readableBytes());
                  if (pathSizeBuf.readableBytes() >= 4) {
                    pathLength = pathSizeBuf.getInt(pathSizeBuf.readerIndex());//Get Size at index = 0;
                    pathLengthSet = true;
                    pathBuf = ctx.alloc().buffer(pathLength);
                    logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE PATH LENGTH: " + pathLength);
                  }
                  //Read in Path
                } else if (!readInPath) {
                  pathBuf.writeBytes(msg, ((pathBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : pathBuf.writableBytes()));
                  if (pathBuf.readableBytes() >= pathLength) {
                    //Read in path in ipFormat
                    readInPath = true;
                    connectionMsgReceived = true;
                    //convert the data in pathBuf to an ascii string
                    thePath = pathBuf.toString(Charset.forName("US-ASCII"));
                    logger.info("*************** FileReceiverHandler: RECEIVED THE PATH AND IT EQUALS = " + thePath + "***********************");

                    /////////////////////////////////////////////////////////
                    // This is a receiver we don't need to parse the path //
                    // because it just consists of this node's IP Address //
                    ////////////////////////////////////////////////////////

                    //SEND REPLY BACK
                    int msgReplyType =  CONNECTION_MSG_ACK_TYPE;
                    ByteBuf replyMsgTypeBuf = Unpooled.copyInt(msgReplyType);
                    ctx.write(replyMsgTypeBuf);
                    ctx.flush();

                    //the path is a string of ip addresses and ports separated by a comma, it doesn't include the source node (the first node in the path)
                    //if path is WS5,WS7,WS12 with the below ip address and port
                    //192.168.0.2:4959.192.168.0.1:4959,192.168.1.2:4959
                    //then only the ip addresses & ports of WS7, WS12 is sent, note this proxy server is WS7
                    //So the path = 192.168.0.1:4959,192.168.1.2:4959
                    //parse out the first ip address



                    /*
                    String[] tokens = thePath.split("[,]+");
                    logger.info("FileRecieverHandler: tokens[0] = " + tokens[0] + ", tokens[1] = " + tokens[1] );
                    theNodeToForwardTo = tokens[1]; // = 192.168.0.1:4959
                    logger.info("FileReceiverHandler: theNodeToForwardTo = " + theNodeToForwardTo);
                    //Separate the ip address from the port
                    String[] ip_and_port = theNodeToForwardTo.split("[:]+");
                    remoteHost = ip_and_port[0]; //"192.168.0.1"
                    logger.info("FileReceiverHandler:ChannelRead: Remote Host = " + remoteHost);
                    remotePort = new Integer(ip_and_port[1]).intValue(); //=4959
                    //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE PATH: " + thePath);
                    logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE PATH " + thePath);
                    */

                  }
                }//readPath
              }//If Not Connection Msg Received
            }//CONNECTION_MSG_TYPE
          }
        }//While loop
      }catch(Exception e){
        System.err.printf("ChannelRead Error Msg: " + e.getMessage());
        e.printStackTrace();

      }
    }//End Read Method


    /*
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }
    */

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        //closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    /*
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
  */
}
