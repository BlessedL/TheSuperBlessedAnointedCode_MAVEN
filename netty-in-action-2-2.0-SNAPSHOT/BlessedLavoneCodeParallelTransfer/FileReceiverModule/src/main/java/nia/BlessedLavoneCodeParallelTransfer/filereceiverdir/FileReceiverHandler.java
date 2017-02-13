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
package nia.BlessedLavoneCodeParallelTransfer.filereceiverdir;


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
  private int fileNameStringSize, fileId, channelId, connectionCloseMsgType;

  private ByteBuf pathSizeBuf, aliasPathSizeBuf;
  private ByteBuf pathBuf,aliasPathBuf,connectionTypeBuf,controlChannelIdBuf,dataChannelIdBuf,channelIdBuf;
  private ByteBuf parallelNumBuf,concurrencyNumBuf;
  private ByteBuf fileNameStringSizeBuf, fileNameStringBuf, offSetBuf, fragmentLengthBuf, fileIdBuf;
  private ByteBuf startTimeByteBuf, endTimeByteBuf, bytesReadByteBuf;
  private ByteBuf connectionCloseByteBuf;
  private boolean pathLengthSet,aliasPathLengthSet,pathStringSet;
  private boolean readInPath,readInAliasPath,readInConnectionType;
  private boolean readInChannelId, connectionCloseMsgReceived;
  private boolean readInDataChannelId, readInControlChannelId,readInParallelNum,readInConcurrencyNum;
  private boolean fileNameStringSizeSet, readInFileNameString, readInOffset, readInFragmentLength, readInFileId, readInFileFragment;
  private boolean timeStartedSet, timeEndedSet;
  private String pathString, theAliasPath;
  private byte[] pathBytes;
  private boolean connectionMsgReceived, sentConnectionMsg, canIconnectToRemoteNode, amIconnectedToRemoteHost;
  private boolean canIconnectToRemoteHost;
  private String theNodeToForwardTo, thePath, theConnectionTypeString;
  private String thefileName;
  private File emptyFile;
  private RandomAccessFile f;
  private FileChannel fc;
  private long bytesRead, currentOffset, fragmentLength, remainingFragmentLength, timeStarted,timeEnded;
  private int currentTotalFileBytesWrote ;


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
      connectionCloseMsgReceived = false;
      fileNameStringSizeSet = false; readInFileNameString = false; readInOffset = false; readInFragmentLength = false;
      readInFileId = false; readInChannelId = false; readInFileFragment = false;
      theAliasPath = null;
      pathBytes = null;
      connectionMsgReceived = false; sentConnectionMsg = false;canIconnectToRemoteNode = false;
      canIconnectToRemoteHost = false;
      amIconnectedToRemoteHost = false;
      theNodeToForwardTo = null;thePath = null;
      myConnectionType = -1; myControlChannelId = -1; myDataChannelId = -1; channelId = -1;
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
      channelIdBuf = Unpooled.buffer(INT_SIZE);
      startTimeByteBuf = Unpooled.buffer(LONG_SIZE); endTimeByteBuf = Unpooled.buffer(LONG_SIZE); bytesReadByteBuf = Unpooled.buffer(LONG_SIZE);
      connectionCloseByteBuf = Unpooled.buffer(INT_SIZE);
      fileNameStringBuf = null;
      offSetBuf = Unpooled.buffer(LONG_SIZE);
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
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        long threadId = Thread.currentThread().getId();
        logger.info("******************************************************");
        logger.info("FileSender:startFileSender ThreadId = " + threadId );
        logger.info("******************************************************");

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
        while (msg.readableBytes() >= 1) {
          if (!timeStartedSet) {
            timeStarted = System.currentTimeMillis();
          }
          logger.info("FileReceiverServer: ChannelRead: msg.readableBytes(" + msg.readableBytes() + ") >= 1");
          //Read in Path Size
          if (!fileNameStringSizeSet) {
            //if pathSizeBuf's writable bytes (number of bytes that can be written to - (Capacity - Writer index) is greater than or equal to in's readable bytes then set the length to in's readable bytes
            //else set the length to the pathSizeBuf writable bytes
            fileNameStringSizeBuf.writeBytes(msg, ((fileNameStringSizeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fileNameStringSizeBuf.writableBytes()));
            if (fileNameStringSizeBuf.readableBytes() >= 4) {
              fileNameStringSize = fileNameStringSizeBuf.getInt(fileNameStringSizeBuf.readerIndex());//Get Size at index = 0;
              fileNameStringSizeSet = true;
              fileNameStringBuf = ctx.alloc().buffer(fileNameStringSize);
              bytesRead += fileNameStringSizeBuf.readableBytes();
              logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE FILE & IT'S DIRECTORY PATH LENGTH: " + fileNameStringSize);
            }
          } else if (!readInFileNameString) {
            //Read in the file
            fileNameStringBuf.writeBytes(msg, ((fileNameStringBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fileNameStringBuf.writableBytes()));
            if (fileNameStringBuf.readableBytes() >= fileNameStringSize) {
              //Read in path in ipFormat
              readInFileNameString = true;
              //convert the data in pathBuf to an ascii string
              thefileName = fileNameStringBuf.toString(Charset.forName("US-ASCII"));
              bytesRead += fileNameStringBuf.readableBytes();

              //Create file
              emptyFile = new File(thefileName); //file Name includes the directory path
              f = new RandomAccessFile(emptyFile, "rw");
              fc = f.getChannel();
              logger.info("FileReceiverHandler: READ IN THE FILE NAME & ITS DIRECTORY PATH " + thefileName);
            }
          } else if (!readInOffset) {
            logger.info("offSetBuf.writeBytes(msg, ((offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ")))");
            offSetBuf.writeBytes(msg, ((offSetBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : offSetBuf.writableBytes()));
            if (offSetBuf.readableBytes() >= LONG_SIZE) {
              logger.info("offSetBuf.getLong(offSetBuf.readerIndex(" + offSetBuf.readerIndex() + "))");
              currentOffset = offSetBuf.getLong(offSetBuf.readerIndex());//Get Size at index = 0;
              readInOffset = true;
              bytesRead += offSetBuf.readableBytes();
              logger.info("FileReceiverHandler: Current Offset = " + currentOffset);
            }

          } else if (!readInFragmentLength) {
            logger.info("fragmentBuf.writeBytes(msg, ((offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ")))");
            fragmentLengthBuf.writeBytes(msg, ((fragmentLengthBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fragmentLengthBuf.writableBytes()));
            if (fragmentLengthBuf.readableBytes() >= LONG_SIZE) {
              logger.info("fragmentLengthBuf.getLong(offSetBuf.readerIndex(" + offSetBuf.readerIndex() + "))");
              fragmentLength = fragmentLengthBuf.getLong(fragmentLengthBuf.readerIndex());//Get Size at index = 0;
              remainingFragmentLength = fragmentLength;
              bytesRead += fragmentLengthBuf.readableBytes();
              readInFragmentLength = true;
              logger.info("FileReceiverHandler: fragment length = : " + fragmentLength);
            }
          } else if (!readInFileId) {
            fileIdBuf.writeBytes(msg, ((fileIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fileIdBuf.writableBytes()));
            if (fileIdBuf.readableBytes() >= INT_SIZE) {
              logger.info("fileIdBuf.getInt(fileIdBuf.readerIndex(" + fileIdBuf.readerIndex() + "))");
              fileId = fileIdBuf.getInt(fileIdBuf.readerIndex());//Get Size at index = 0;
              readInFileId = true;
              bytesRead += fileIdBuf.readableBytes();
              logger.info("FileReceiverServer: The File ID = : " + fileId);
            }
          } else if (!readInChannelId) {
            channelIdBuf.writeBytes(msg, ((channelIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : channelIdBuf.writableBytes()));
            if (channelIdBuf.readableBytes() >= INT_SIZE) {
              logger.info("channelIdBuf.getInt(channelIdBuf.readerIndex(" + channelIdBuf.readerIndex() + "))");
              channelId = channelIdBuf.getInt(channelIdBuf.readerIndex());//Get Size at index = 0;
              readInChannelId = true;
              bytesRead += channelIdBuf.readableBytes();
              logger.info("FileReceiverServer: The Channel ID = : " + channelId);
            }
          } else {
            if (!readInFileFragment) {
              logger.info("FileReceiverHandler: DID NOT FINISH READING IN THE FILE, READING IN THE FILE, CHUNK BY CHUNK");
              //Store NETTY'S MSG READABLE BYTES IN A REGULAR JAVA BYTEBUFFER
              //ByteBuffer theByteBuffer = msg.nioBuffer();
              ByteBuffer theByteBuffer = null;
              //if (msg.readableBytes() > 0) {
              //if the remainingFragmentLength is greater than the msg readable bytes then copy
              //all the readable bytes to
              if (msg.readableBytes() <= remainingFragmentLength) {
                  logger.info("channelId: " + channelId + " ,msg.readableBytes(" + msg.readableBytes() + ") <= remainingFragmentLength(" + remainingFragmentLength +")");
                  logger.info("channelId: " + channelId + " ,msg.capacity =  " + msg.capacity() + " msg.readableBytes = " + msg.readableBytes());
                  theByteBuffer = msg.nioBuffer();
                  logger.info("channelId: " + channelId + " ,theByteBuffer = msg.nioBuffer() and the java ByteBuffer capacity = " + theByteBuffer.capacity() + " Which should be equal to Netty's msg ByteBuf readable bytes which = " + msg.readableBytes() );
                  logger.info("channelId: " + channelId + " ,Java's Byte Buffer Position = " + theByteBuffer.position() + " Java's theByteBuffer Limit = " + theByteBuffer.limit() + " Java's theByteBuf remaining bytes to read = " + theByteBuffer.remaining() );
                  logger.info("channelId: " + channelId + " ,Netty's Byte Buff Capacity = " + msg.capacity() + " Netty's Byte Buff Reader Index = " + msg.readerIndex() + " Netty's Byte Buff Writer Index = " + msg.writerIndex());

              } else {
                //msg.readableBytes()> remainingFragmentLength
                logger.info("channelId: " + channelId + " ,msg.readableBytes(" + msg.readableBytes() + ") > remainingFragmentLength(" + remainingFragmentLength +")");
                logger.info("channelId: " + channelId + " ,msg.capacity =  " + msg.capacity() + " msg.readableBytes = " + msg.readableBytes());
                logger.info("channelId: " + channelId + " ,Netty's Byte Buff Capacity = " + msg.capacity() + " Netty's Byte Buff Reader Index = " + msg.readerIndex() + " Netty's Byte Buff Writer Index = " + msg.writerIndex());

                //msg.readableBytes() >= remainingFragmentLength
                //so just copy the necessary bytes
                //Since msg.readableBytes returns an int, this means that the remainingFragmentLength is small enough to be an int
                int theRemainingFragmentLengthInt = (int) remainingFragmentLength;
                theByteBuffer = msg.nioBuffer(msg.readerIndex(), theRemainingFragmentLengthInt);
                logger.info("channelId: " + channelId + " ,msg.nioBuffer(msg.readerIndex(" + msg.readerIndex() +"), theRemainingFragmentLengthInt(" + theRemainingFragmentLengthInt + ")");
                logger.info("channelId: " + channelId + " ,Java's ByteBuffer capacity = " + theByteBuffer.capacity() + " Java's ByteBuffer position = " + theByteBuffer.position() + " Java's ByteBuffer remaining bytes to read = " +  theByteBuffer.remaining() + " Netty's ByteBuf Readable Bytes = " + msg.readableBytes() + " Netty's ByteBuf Capacity = " + msg.capacity());

              }

              while(theByteBuffer.hasRemaining()) {
                int fileBytesWritten = fc.write(theByteBuffer, currentOffset);
                logger.info("channelId: " + channelId + " ,int fileBytesWritten(" + fileBytesWritten + " = fc.write(theByteBuffer, currentOffset(" + currentOffset + ")");
                logger.info("channelId: " + channelId + " ,BEFORE UPDATING VALUES: file Bytes written = " + fileBytesWritten + " currentTotalFileBytesWrote = " + currentTotalFileBytesWrote + ", remainingFragmentLength = " + remainingFragmentLength);
                if (fileBytesWritten > -1) {
                  logger.info("channelId: " + channelId + " ,fileBytesWritten(" + fileBytesWritten + ") > -1 ");
                  currentTotalFileBytesWrote += fileBytesWritten;
                  //currentOffset += msg.readableBytes();
                  currentOffset += fileBytesWritten;
                  //remainingFragmentLength -= msg.readableBytes();
                  remainingFragmentLength -= fileBytesWritten;
                  //bytesRead += msg.readableBytes();
                  bytesRead += fileBytesWritten;
                  logger.info("channelId: " + channelId + " ,AFTER UPDATING VALUES: file Bytes written = " + fileBytesWritten + " currentTotalFileBytesWrote = " + currentTotalFileBytesWrote + ", remainingFragmentLength = " + remainingFragmentLength);
                  int updatedMsgReaderIndex = msg.readerIndex() + fileBytesWritten;
                  logger.info("channelId: " + channelId + " ,Current Msg.readerIndex BEFORE UPDATE = " + msg.readerIndex() + " And msg.writerIndex = " + msg.writerIndex());
                  //msg.readerIndex(msg.readerIndex() + msg.readableBytes());
                  //since the ByteBuf.nioBuffer method does not increase the reader index, I must increase it manually
                  // Increase the reader index of (ByteBuf) msg by the readableBytes
                  //msg.readerIndex(msg.readableBytes());
                  msg.readerIndex(msg.readerIndex() + fileBytesWritten);
                  logger.info("channelId: " + channelId + " ,UPDATED MSG READER INDEX = Current Msg.readerIndex: " + msg.readerIndex() + " + File Bytes Writen: " + fileBytesWritten + " = updatedMsgReaderIndex: " + updatedMsgReaderIndex);

                } else {
                  logger.info("channelId: " + channelId + " ,DID NOT UPDATE ANY VALUES BECAUSE: (fileBytesWritten(" + fileBytesWritten + " > -1)");
                }


                //if (!theByteBuffer.hasRemaining()) {
                if (remainingFragmentLength <= 0){
                  startTimeByteBuf.writeLong(timeStarted);
                  timeEnded = System.currentTimeMillis();
                  endTimeByteBuf.writeLong(timeEnded);
                  bytesReadByteBuf.writeLong(bytesRead);

                  //Send Start time (long)
                  ctx.write(startTimeByteBuf);
                  //Send end time (long)
                  ctx.write(endTimeByteBuf);
                  //Send bytes Read (long)
                  ctx.write(bytesReadByteBuf);
                  ctx.flush();

                  //Reset Byte Buffer
                  fileNameStringSizeBuf.clear();
                  fileNameStringBuf = null;
                  offSetBuf.clear();
                  fragmentLengthBuf.clear();
                  fileIdBuf.clear();
                  //reset current offset
                  currentOffset = 0;
                  //remaining Fragment Length
                  remainingFragmentLength = 0;
                  //Reset File and FileChannel
                  emptyFile = null;
                  f = null;
                  fc = null;
                  //Reset Bytes Read
                  bytesRead = 0;
                  //Reset timer flags
                  timeEndedSet = false;
                  timeStartedSet = false;

                  //Wait to receive close channel msg
                  //Closing the channel
                  //ctx.channel().close();
                }
              }


            } else {//End else if file fragment
              if (!connectionCloseMsgReceived){
                logger.info("connectionCloseByteBuf.writeBytes(msg, ((connectionCloseByteBuf.writableBytes(" + connectionCloseByteBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : connectionCloseByteBuf.writableBytes(" + connectionCloseByteBuf.writableBytes() + ")))");
                connectionCloseByteBuf.writeBytes(msg, ((connectionCloseByteBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : connectionCloseByteBuf.writableBytes()));
                if (connectionCloseByteBuf.readableBytes() >= INT_SIZE) {
                  logger.info("connectionCloseByteBuf.getLong(offSetBuf.readerIndex(" + offSetBuf.readerIndex() + "))");
                  connectionCloseMsgType = connectionCloseByteBuf.getInt(connectionCloseByteBuf.readerIndex());//Get Size at index = 0;
                  connectionCloseMsgReceived = true;
                  //bytesRead += connectionCloseByteBuf.readableBytes();
                  logger.info("FileReceiverHandler: Received the Connection Close Msg = " + connectionCloseMsgType);
                  ctx.channel().close();
                }

              }

            }
          }//End Else
        }//End While
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
