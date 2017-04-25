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
//package nia.BlessedLavoneCodeWithEverything.filereceiverdir;
package nia.BlessedLavoneCodeWithEverything_withChunkWriteHandler.filereceiverdir;


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
  public final int CONNECTION_MSG_TYPE = 1;
  public final int FILE_MSG_TYPE = 2;
  public final int FILE_ACK_MSG_TYPE = 2;
  public final int DONE_MSG_TYPE = 3;
  public final int CONTROL_CHANNEL_TYPE = 0;
  public final int DATA_CHANNEL_TYPE = 1;

  public int INT_SIZE = 4;
  public int LONG_SIZE = 8;

  //private enum State myState;
  private int msgType;
  private int channelReadCounter;
  private int pathLength,aliasPathLength;
  private int myConnectionType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum;
  private int fileNameStringSize, fileId, channelId, connectionCloseMsgType;
  private ByteBuf msgTypeBuf;
  private ByteBuf pathSizeBuf, aliasPathSizeBuf;
  private ByteBuf pathBuf,aliasPathBuf,connectionTypeBuf,controlChannelIdBuf,dataChannelIdBuf,channelIdBuf;
  private ByteBuf parallelNumBuf,concurrencyNumBuf;
  private ByteBuf fileNameStringSizeBuf, fileNameStringBuf, offSetBuf, fragmentLengthBuf, fileIdBuf;
  private ByteBuf startTimeByteBuf, endTimeByteBuf, bytesReadByteBuf;
  private ByteBuf connectionCloseByteBuf, printThroughputMsgTypeBuf;
  private boolean pathLengthSet,aliasPathLengthSet,pathStringSet,msgTypeSet;
  private boolean readInPath,readInAliasPath,readInConnectionType;
  private boolean readInChannelId, connectionCloseMsgReceived;
  private boolean readInDataChannelId, readInControlChannelId,readInParallelNum,readInConcurrencyNum;
  private boolean fileNameStringSizeSet, readInFileNameString, readInOffset, readInFragmentLength, readInFileId, readInFileFragment,readInPrintThroughputMsg;
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
  private int currentTotalFileBytesWrote;
  private int connectionType;
  private int myPrintThroughputMsgVal;
  private ChannelHandlerContext controlChannelCtx;
  private ChannelHandlerContext myChannelCtx;
  private long threadId;
  private String channelTypeString;
  private FileReceiverHandler myControlChannelHandler;
  private FileReceiverHandler myDataChannelHandler;
  //               File ID, FileAckObject
  private HashMap<String, ArrayList<FileAckObject>> myFileAckHashMap; //= new HashMap<String,FileAckObject>();
  // public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannels = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
  private boolean myMinStartTimeSet,myMaxEndTimeSet;
  private long myMinStartTime, myMaxEndTime;
  private long myTotalBytesRead;
  private ArrayList<FileReceiverHandler> myDataChannelHandlerList;

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
      connectionCloseMsgReceived = false; msgTypeSet = false;
      fileNameStringSizeSet = false; readInFileNameString = false; readInOffset = false; readInFragmentLength = false;
      readInFileId = false; readInChannelId = false; readInFileFragment = false; readInPrintThroughputMsg = false;
      theAliasPath = null;
      pathBytes = null;
      connectionMsgReceived = false; sentConnectionMsg = false;canIconnectToRemoteNode = false;
      canIconnectToRemoteHost = false;
      amIconnectedToRemoteHost = false;
      theNodeToForwardTo = null;thePath = null;
      msgType = -1; myConnectionType = -1; myControlChannelId = -1; myDataChannelId = -1; channelId = -1;
      myParallelNum = -1; myConcurrencyNum = -1;
      fileNameStringSize = -1;
      msgTypeBuf = Unpooled.buffer(INT_SIZE);
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
      printThroughputMsgTypeBuf = Unpooled.buffer(INT_SIZE);
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
      connectionType = -1;
      controlChannelCtx = null;
      threadId = -1;
      channelTypeString = "";
      myPrintThroughputMsgVal = -1;
      myControlChannelHandler = null;
      myDataChannelHandler = null;
      myFileAckHashMap = null;
      myMinStartTimeSet = false; myMaxEndTimeSet = false;
      myMinStartTime = -1; myMaxEndTime = -1;
      myTotalBytesRead = 0;
      myDataChannelHandlerList = null;

    }

    public void setControlChannelHandler(FileReceiverHandler aControlChannelHandler){
      myControlChannelHandler = aControlChannelHandler;
    }

    public FileReceiverHandler getControlChannelHandler(){
      return myControlChannelHandler;
    }

    public void setDataChannelHandler(FileReceiverHandler aDataChannelHandler){
      myDataChannelHandler = aDataChannelHandler;
    }

    public FileReceiverHandler getDataChannelHandler(){
       return myDataChannelHandler;
    }

    public boolean addDataChannelHandler(FileReceiverHandler aDataChannelHandler){
      boolean addedDataChannelHandler = false;
      if (myDataChannelHandlerList != null){
        addedDataChannelHandler = myDataChannelHandlerList.add(aDataChannelHandler);
      }
      return addedDataChannelHandler;
    }

    public int getConnectionType(){
      return connectionType;
    }

    public void setConnectionType(int aConnectionType){
      connectionType = aConnectionType;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        threadId = Thread.currentThread().getId();
        //logger.info("********************************************************");
        //logger.info("FileReceiverHandler:ChannelActive ThreadId = " + threadId );
        //logger.info("*********************************************************");

        proxyServerState = ProxyServerState.CONNECT;
        //logger.info("Thread ID: " + threadId + " FileReceiverHandler is active and is now in the CONNECT state");
        this.myChannelCtx = ctx;
        channelTypeString = "";

    }

    //sendFileAck(myControlChannelHandlerAndFileAckObject.getFileId(),myControlChannelHandlerAndFileAckObject.getBytesRead(),myControlChannelHandlerAndFileAckObject.getStartTime(),myControlChannelHandlerAndFileAckObject.getEndTime());
    //myControlChannelHandlerAndFileAckObject.getFileId(),myControlChannelHandlerAndFileAckObject.getBytesRead(),myControlChannelHandlerAndFileAckObject.getStartTime(),myControlChannelHandlerAndFileAckObject.getEndTime());
    public synchronized void sendFileAck(int aFileId, long theBytesRead, long theStartTime, long theEndTime) throws Exception {


    //Msg Type: FILE_ACK_MSG_TYPE = 2
    ByteBuf theMsgTypeBuf = Unpooled.copyInt(FILE_ACK_MSG_TYPE);
    //Send FileId
    ByteBuf theFileIdBuf = Unpooled.copyInt(aFileId);
    //Bytes Read
    ByteBuf theBytesReadBuf = Unpooled.copyLong(theBytesRead);
    //Start Time
    ByteBuf theStartTimeBuf = Unpooled.copyLong(theStartTime);
    //End Time
    ByteBuf theEndTimeBuf = Unpooled.copyLong(theEndTime);

    myChannelCtx.write(theMsgTypeBuf);
    myChannelCtx.write(theFileIdBuf);
    myChannelCtx.write(theBytesReadBuf);
    myChannelCtx.write(theStartTimeBuf);
    myChannelCtx.write(theEndTimeBuf);
    myChannelCtx.flush();


    /*
    int tempFileId = 77;
    long tempBytesRead = 777777;
    long tempStartTime = 8888888;
    long tempEndTime = 9999999;

    ByteBuf theMsgTypeBuf = Unpooled.copyInt(FILE_ACK_MSG_TYPE);
    //Send FileId
      ByteBuf theFileIdBuf = Unpooled.copyLong(tempFileId);
      //Bytes Read
      ByteBuf theBytesReadBuf = Unpooled.copyLong(tempBytesRead);
      //Start Time
      ByteBuf theStartTimeBuf = Unpooled.copyLong(tempStartTime);
      //End Time
      ByteBuf theEndTimeBuf = Unpooled.copyLong(tempEndTime);

    myChannelCtx.write(theMsgTypeBuf);
    myChannelCtx.write(theFileIdBuf);
    myChannelCtx.write(theBytesReadBuf);
    myChannelCtx.write(theStartTimeBuf);
    myChannelCtx.write(theEndTimeBuf);
    myChannelCtx.flush();
    */

      //logger.info("\n****FileReceiverHandler: sendFileAck: CONTROL CHANNEL " + myControlChannelId + "SENT MIN START TIME = " + theStartTime + ", MAX END TIME = " + theEndTime + ", BYTES READ =  " + theBytesRead + "******\n" );
    }

     /*
        In the previous code, I saw that the msg passed in is alway 1024 bytes, so I am not sure if
        the first part of the 1024 bytes is the connection msg and the remaining bytes are garbage bytes
        or if the first part of the 1024 bytes is the connection msg and the remaining byres are the file header and file bytes, I will have to see

        I'm assuming the remaining bytes are the file header, but I want to wait until all data channels receive the connection msg
        When processing the connection message, I'm not sure if the msg passed in to this channel read method
        contains the connection message and either remaining garbage bytes or the fileName String
       */
    @Override
    //public void channelRead0(final ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    /*
       Make sure after I read the connection ack msg, I set the channel pipeline to use the frame decoder
       also based on the frame decoder bytes read: Get the File Header, then get the file (File Fragment) it self
     */
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
      try {
        while (msg.readableBytes() >= 1 ){
          //Read in Msg Type
          if (!msgTypeSet) {
            msgTypeBuf.writeBytes(msg, ((msgTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : msgTypeBuf.writableBytes()));
            //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
            if (msgTypeBuf.readableBytes() >= 4) {
              msgType = msgTypeBuf.getInt(msgTypeBuf.readerIndex());//Get Size at index = 0;
              msgTypeSet = true;
              String msgTypeString = ((msgType == CONNECTION_MSG_TYPE) ? "CONNECTION MSG TYPE" : " FILE MSG TYPE ");
              //logger.info("FileReceiverHandler(" + threadId + "): channelRead: READ IN THE MSG Type, Msg Type = " + msgTypeString);
            }else {
              //logger.info("FileReceiverHandler(" + threadId + "): channelRead: DID NOT READ IN THE MSG TYPE, msgTypeBuf.readableBytes() >= 4" );
            }

          } else if (msgType == CONNECTION_MSG_TYPE) {
            System.err.printf("\n **********FileReceiverHandler(%d): Connection MSG Type Received **********\n\n",threadId);
            if (!connectionMsgReceived) {
              //Process Msg Type
              //logger.info("FileReceiverHandler(" + threadId +") ProcessConnectionMsg: msg.readableBytes(" + msg.readableBytes() + ") >= 1");
              //Read in Path Size
              if (!pathLengthSet) {
                //if pathSizeBuf's writable bytes (number of bytes that can be written to - (Capacity - Writer index) is greater than or equal to in's readable bytes then set the length to in's readable bytes
                //else set the length to the pathSizeBuf writable bytes
                pathSizeBuf.writeBytes(msg, ((pathSizeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : pathSizeBuf.writableBytes()));
                //logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg : pathSizeBuf.readableBytes() =  " + pathSizeBuf.readableBytes());
                if (pathSizeBuf.readableBytes() >= 4) {
                  pathLength = pathSizeBuf.getInt(pathSizeBuf.readerIndex());//Get Size at index = 0;
                  pathLengthSet = true;
                  pathBuf = ctx.alloc().buffer(pathLength);
                  //logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE PATH LENGTH: " + pathLength);
                }
                //Read in Path
              } else if (!readInPath) {
                pathBuf.writeBytes(msg, ((pathBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : pathBuf.writableBytes()));
                if (pathBuf.readableBytes() >= pathLength) {
                  //Read in path in ipFormat
                  readInPath = true;
                  //convert the data in pathBuf to an ascii string
                  thePath = pathBuf.toString(Charset.forName("US-ASCII"));
                  //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE PATH: " + thePath);
                  //logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE PATH " + thePath);

                }
              } else if (!aliasPathLengthSet) {
                //logger.info("FileReceiverServer: ProcessConnectionMsg: MSG.readableBytes = " + msg.readableBytes() + " aliasPathSizeBuf.writable Bytes =  " + aliasPathSizeBuf.writableBytes() + " aliasPathSizeBuf.readableBytes = " + aliasPathSizeBuf.readableBytes());
                if (aliasPathSizeBuf.writableBytes() >= msg.readableBytes()) {
                  //logger.info("FileReceiverServer: ProcessConnectionMsg: aliasPathSizeBuf.writableBytes(" + aliasPathSizeBuf.writableBytes() +") >=  msg.readableBytes(" + msg.readableBytes() + "), THE LENGTH OF BYTES TO WRITE TO THE ALIAS_PATH_SIZE_BUF = msg.readableBytes = "+msg.readableBytes() );
                  aliasPathSizeBuf.writeBytes(msg, msg.readableBytes());

                } else {
                  //logger.info("FileReceiverServer: ProcessConnectionMsg: msg.readableBytes(" + msg.readableBytes() + ") >= aliasPathSizeBuf.writableBytes(" + aliasPathSizeBuf.writableBytes() + ") THE LENGTH OF BYTES TO WRITE TO THE ALIAS_PATH_SIZE_BUF  = aliasPathSizeBuf.writableBytes = " + aliasPathSizeBuf.writableBytes());
                  aliasPathSizeBuf.writeBytes(msg, aliasPathSizeBuf.writableBytes());
                }
                //aliasPathSizeBuf.writeBytes(msg,( (aliasPathSizeBuf.writableBytes()>=msg.readableBytes())?msg.readableBytes():aliasPathSizeBuf.writableBytes()));
                if (aliasPathSizeBuf.readableBytes() >= 4) {
                  aliasPathLength = aliasPathSizeBuf.getInt(aliasPathSizeBuf.readerIndex());//Get Size at index = 0;
                  aliasPathLengthSet = true;
                  aliasPathBuf = ctx.alloc().buffer(aliasPathLength);
                  //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE ALIAS PATH LENGTH = " + aliasPathLength);
                  //logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE ALIAS PATH LENGTH " + aliasPathLength);
                }
              } else if (!readInAliasPath) {
                aliasPathBuf.writeBytes(msg, ((aliasPathBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : aliasPathBuf.writableBytes()));
                if (aliasPathBuf.readableBytes() >= aliasPathLength) {
                  //Read in alias Path
                  readInAliasPath = true;
                  //convert the data in aliasPathBuf to an ascii string
                  theAliasPath = aliasPathBuf.toString(Charset.forName("US-ASCII"));
                  //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE ALIAS PATH = " + theAliasPath);
                  //logger.info("FileReceiverHandler(" + threadId + ") ProcessConnectionMsg: READ IN THE ALIAS PATH LENGTH " + aliasPathLength);
                }

              } else if (!readInConnectionType) {
                connectionTypeBuf.writeBytes(msg, ((connectionTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : connectionTypeBuf.writableBytes()));
                //logger.info("FileReceiverServer: ProcessConnectionMsg: connectionTypeBuf.writableBytes() = " + connectionTypeBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                if (connectionTypeBuf.readableBytes() >= 4) {
                  myConnectionType = connectionTypeBuf.getInt(connectionTypeBuf.readerIndex());//Get Size at index = 0;
                  readInConnectionType = true;
                  if (myConnectionType == CONTROL_CHANNEL_TYPE) {
                    //logger.info("**************** FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN THE CONNECTION TYPE, THE CONNECTION TYPE = CONTROL CHANNEL ****************");
                    this.controlChannelCtx = ctx;
                    this.myControlChannelHandler = this;
                    channelTypeString = "CONTROL_CHANNEL";
                    //Create the Data Channel Handler List
                    myDataChannelHandlerList = new ArrayList<FileReceiverHandler>();
                  } else {
                    channelTypeString = "DATA_CHANNEL";
                    this.myDataChannelHandler = this;
                    //logger.info("**************** FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN THE CONNECTION TYPE, THE CONNECTION TYPE = DATA CHANNEL *******************");
                  }

                  //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE CONNECTION TYPE, THE CONNECTION TYPE = " + myConnectionType);
                }
              } else if (!readInControlChannelId) {
                controlChannelIdBuf.writeBytes(msg, ((controlChannelIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : controlChannelIdBuf.writableBytes()));
                //logger.info("FileReceiverServer: ProcessConnectionMsg: controlChannelIdBuf.writableBytes() = " + controlChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                if (controlChannelIdBuf.readableBytes() >= 4) {
                  myControlChannelId = controlChannelIdBuf.getInt(controlChannelIdBuf.readerIndex());//Get Size at index = 0;
                  readInControlChannelId = true;
                  //logger.info("FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN CONTROL CHANNEL: " + myControlChannelId);
                }

              } else if (!readInDataChannelId) {
                dataChannelIdBuf.writeBytes(msg, ((dataChannelIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : dataChannelIdBuf.writableBytes()));
                //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                if (dataChannelIdBuf.readableBytes() >= 4) {
                  myDataChannelId = dataChannelIdBuf.getInt(dataChannelIdBuf.readerIndex());//Get Size at index = 0;
                  readInDataChannelId = true;
                  //logger.info("FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN THE DATA CHANNEL ID, THE DATA CHANNEL ID = " + myDataChannelId);
                }
              } else if (!readInParallelNum) {
                parallelNumBuf.writeBytes(msg, ((parallelNumBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : parallelNumBuf.writableBytes()));
                //logger.info("FileReceiverServer: ProcessConnectionMsg: parallelNumBuf.writableBytes() = " + parallelNumBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                if (parallelNumBuf.readableBytes() >= 4) {
                  myParallelNum = parallelNumBuf.getInt(parallelNumBuf.readerIndex());//Get Size at index = 0;
                  readInParallelNum = true;
                  //logger.info("FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN THE PARALLEL NUM, THE PARALLEL NUM = " + myParallelNum);
                }
              } else if (!readInConcurrencyNum) {
                concurrencyNumBuf.writeBytes(msg, ((concurrencyNumBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : concurrencyNumBuf.writableBytes()));
                //logger.info("FileReceiverServer: ProcessConnectionMsg: concurrencyNumBuf.writableBytes() = " + concurrencyNumBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                if (concurrencyNumBuf.readableBytes() >= 4) {
                  myConcurrencyNum = concurrencyNumBuf.getInt(concurrencyNumBuf.readerIndex());//Get Size at index = 0;
                  readInConcurrencyNum = true;
                  //CONNECTION MSG RECEIVED
                  connectionMsgReceived = true;
                  //logger.info("FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN THE CONCURRENCY NUM, THE CONCURRENCY NUM = " + myConcurrencyNum);
                  //logger.info("FileReceiverHandler(" + threadId + ": ProcessConnectionMsg: CONNECTION MSG HAS BEEN ******COMPLETELY READ IN*********");
                  //logger.info("FileReceiverHandler(" + threadId + ": ProcessConnectionMsg: ABOUT TO REGISTER CHANNEL *********");


                  //Register the Control Channel or Data Channel
                  //Note the registerChannelCtx method will return NULL if all data channels have not registered
                  ChannelHandlerContext aCtx = null;
                  if (myConnectionType == CONTROL_CHANNEL_TYPE){
                    //REGISTER THIS CONTROL CHANNEL
                    //Pass in this FileReceiverHandler, this is the Control Channel Handler
                    aCtx = FileReceiver.registerChannelCtx(theAliasPath, myChannelCtx, myConnectionType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum, this, null, threadId);
                    //Create the FileAckObject Hash Map for this ControlChannelHandler
                    myFileAckHashMap = new HashMap<String,ArrayList<FileAckObject>>();
                  }
                  else {
                    //REGISTER THIS DATA CHANNEL
                    aCtx = FileReceiver.registerChannelCtx(theAliasPath, myChannelCtx, myConnectionType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum, null, this,threadId);
                  }
                  //Check to see if all data channels have registered:  If so, send the Connection Ack through the control channel
                  //aCtx is the Control Channel Context Handler that is returned when all data channels have connected
                  //Whether this is a Control Channel registering or a Data Channel Registering, if aCtx returns Null
                  //this either means all data channels have not connected yet or the control channel have not connected yet
                  if (aCtx != null) {
                    //Create the Connection Ak Byte Buf
                    ByteBuf aConnectAckBuf = Unpooled.copyInt(CONNECTION_MSG_TYPE);
                    //aCtx.write(connectionAckMsg);
                    if (myConnectionType == DATA_CHANNEL_TYPE) {
                      controlChannelCtx = aCtx;
                      //logger.info("FileReceiverHandler: ChannelRead: This is Data Channel #" + myDataChannelId + " belonging to Control Channel " + myControlChannelId + " ALL CHANNELS ARE REGISTERED ");
                    }
                    //Send the Connection Ack MsG through the Control Channel for this Data Channel or Control Channel
                    //Note if this is a Control Channel the ControlChannelCtx is already set
                    //Send the Connection Ack Msg
                    controlChannelCtx.writeAndFlush(aConnectAckBuf);
                  }

                  //logger.info("\n ******************FileReceiverHandler: " + channelTypeString + " (" + channelId +"), Thread ID: " + threadId + ": ProcessConnectionMsg: HERE ARE THE CHANNELS REGISTERED "+ FileReceiver.registeredChannelsToString() + " *********************** \n");
                  //logger.info("\n ******************FileReceiverHandler: " + channelTypeString + " (" + channelId +"), Thread ID: " + threadId + ": ProcessConnectionMsg: AFTER PROCESSING CONNECTION MSG, msg.readableBytes() = " + msg.readableBytes() + " *********************** \n");

                  //Reset Msg Type & CONNECTION MSG BUFF
                  msgTypeSet = false;
                  msgTypeBuf.clear();
                  msgType = -1;
                  //Clear the Connection ByteBuf's that we will reuse
                  pathSizeBuf.clear();
                  pathBuf.clear();
                  aliasPathSizeBuf.clear();
                  aliasPathBuf.clear();
                  //Null out - We will not use thes ByteBufs any more
                  connectionTypeBuf = null;
                  controlChannelIdBuf = null;
                  dataChannelIdBuf = null;
                  parallelNumBuf = null;
                  concurrencyNumBuf = null;

                  //proxyServerState = ProxyServerState.TRANSFER;

                  //myServerHandlerHelper.registerChannelCtx(theAliasPath, ctx, myConnectionType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum );

                  //IF THIS IS A CONTROL CHANNEL CHECK TO SEE IF ALL PARALLEL CHANNELS HAVE RECEIVED THE ACKNOWLEDGEMENT
                }
              }
            }
          } else if (msgType == FILE_MSG_TYPE) {
            //NOTE ONLY DATA CHANNELS RECEIVE FILE FRAGMENTS
              if (!timeStartedSet) {
                timeStarted = System.currentTimeMillis();
                timeStartedSet = true;
              }
              //logger.info("FileReceiverServer: ChannelRead: msg.readableBytes(" + msg.readableBytes() + ") >= 1");
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
                  //logger.info("FileReceiverHandler: ChannelRead: READ IN THE FILE DIRECTORY PATH LENGTH: " + fileNameStringSize);
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
                //logger.info("offSetBuf.writeBytes(msg, ((offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ")))");
                offSetBuf.writeBytes(msg, ((offSetBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : offSetBuf.writableBytes()));
                if (offSetBuf.readableBytes() >= LONG_SIZE) {
                  //logger.info("offSetBuf.getLong(offSetBuf.readerIndex(" + offSetBuf.readerIndex() + "))");
                  currentOffset = offSetBuf.getLong(offSetBuf.readerIndex());//Get Size at index = 0;
                  readInOffset = true;
                  bytesRead += offSetBuf.readableBytes();
                  //logger.info("FileReceiverHandler: Current Offset = " + currentOffset);
                }

              } else if (!readInFragmentLength) {
                //logger.info("fragmentBuf.writeBytes(msg, ((offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : offSetBuf.writableBytes(" + offSetBuf.writableBytes() + ")))");
                fragmentLengthBuf.writeBytes(msg, ((fragmentLengthBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fragmentLengthBuf.writableBytes()));
                if (fragmentLengthBuf.readableBytes() >= LONG_SIZE) {
                  //logger.info("fragmentLengthBuf.getLong(offSetBuf.readerIndex(" + offSetBuf.readerIndex() + "))");
                  fragmentLength = fragmentLengthBuf.getLong(fragmentLengthBuf.readerIndex());//Get Size at index = 0;
                  remainingFragmentLength = fragmentLength;
                  bytesRead += fragmentLengthBuf.readableBytes();
                  readInFragmentLength = true;
                  //logger.info("FileReceiverHandler: fragment length = : " + fragmentLength);
                }
              } else if (!readInFileId) {
                fileIdBuf.writeBytes(msg, ((fileIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fileIdBuf.writableBytes()));
                if (fileIdBuf.readableBytes() >= INT_SIZE) {
                  //logger.info("fileIdBuf.getInt(fileIdBuf.readerIndex(" + fileIdBuf.readerIndex() + "))");
                  fileId = fileIdBuf.getInt(fileIdBuf.readerIndex());//Get Size at index = 0;
                  readInFileId = true;
                  bytesRead += fileIdBuf.readableBytes();
                  //logger.info("FileReceiverServer: The File ID = : " + fileId);
                }
              } else {
                if (!readInFileFragment) {
                  //logger.info("FileReceiverHandler: DID NOT FINISH READING IN THE FILE, READING IN THE FILE, CHUNK BY CHUNK");
                  //Store NETTY'S MSG READABLE BYTES IN A REGULAR JAVA BYTEBUFFER
                  //ByteBuffer theByteBuffer = msg.nioBuffer();
                  ByteBuffer theByteBuffer = null;
                  //if (msg.readableBytes() > 0) {
                  //if the remainingFragmentLength is greater than the msg readable bytes then copy
                  //all the readable bytes to
                  if (msg.readableBytes() <= remainingFragmentLength) {
                    //logger.info("msg.readableBytes(" + msg.readableBytes() + ") <= remainingFragmentLength(" + remainingFragmentLength +")");
                    //logger.info("msg.capacity =  " + msg.capacity() + " msg.readableBytes = " + msg.readableBytes());
                    theByteBuffer = msg.nioBuffer();
                    //logger.info("theByteBuffer = msg.nioBuffer() and the java ByteBuffer capacity = " + theByteBuffer.capacity() + " Which should be equal to Netty's msg ByteBuf readable bytes which = " + msg.readableBytes() );
                    //logger.info("Java's Byte Buffer Position = " + theByteBuffer.position() + " Java's theByteBuffer Limit = " + theByteBuffer.limit() + " Java's theByteBuf remaining bytes to read = " + theByteBuffer.remaining() );
                    //logger.info("Netty's Byte Buff Capacity = " + msg.capacity() + " Netty's Byte Buff Reader Index = " + msg.readerIndex() + " Netty's Byte Buff Writer Index = " + msg.writerIndex());

                  } else {
                    //msg.readableBytes()> remainingFragmentLength
                    //logger.info("msg.readableBytes(" + msg.readableBytes() + ") > remainingFragmentLength(" + remainingFragmentLength +")");
                    //logger.info("msg.capacity =  " + msg.capacity() + " msg.readableBytes = " + msg.readableBytes());
                    //logger.info("Netty's Byte Buff Capacity = " + msg.capacity() + " Netty's Byte Buff Reader Index = " + msg.readerIndex() + " Netty's Byte Buff Writer Index = " + msg.writerIndex());

                    //msg.readableBytes() >remainingFragmentLength
                    //so just copy the necessary bytes
                    //Since msg.readableBytes returns an int, this means that the remainingFragmentLength is small enough to be an int
                    int theRemainingFragmentLengthInt = (int) remainingFragmentLength;
                    theByteBuffer = msg.nioBuffer(msg.readerIndex(), theRemainingFragmentLengthInt);
                    //logger.info("msg.nioBuffer(msg.readerIndex(" + msg.readerIndex() +"), theRemainingFragmentLengthInt(" + theRemainingFragmentLengthInt + ")");
                    //logger.info("Java's ByteBuffer capacity = " + theByteBuffer.capacity() + " Java's ByteBuffer position = " + theByteBuffer.position() + " Java's ByteBuffer remaining bytes to read = " +  theByteBuffer.remaining() + " Netty's ByteBuf Readable Bytes = " + msg.readableBytes() + " Netty's ByteBuf Capacity = " + msg.capacity());

                  }

                  while(theByteBuffer.hasRemaining()) {
                    int fileBytesWritten = fc.write(theByteBuffer, currentOffset);
                    //logger.info(" int fileBytesWritten(" + fileBytesWritten + " = fc.write(theByteBuffer, currentOffset(" + currentOffset + ")");
                    //logger.info("BEFORE UPDATING VALUES: file Bytes written = " + fileBytesWritten + " currentTotalFileBytesWrote = " + currentTotalFileBytesWrote + ", remainingFragmentLength = " + remainingFragmentLength);
                    if (fileBytesWritten > -1) {
                      //logger.info(" fileBytesWritten(" + fileBytesWritten + ") > -1 ");
                      currentTotalFileBytesWrote += fileBytesWritten;
                      //currentOffset += msg.readableBytes();
                      currentOffset += fileBytesWritten;
                      //remainingFragmentLength -= msg.readableBytes();
                      remainingFragmentLength -= fileBytesWritten;
                      //bytesRead += msg.readableBytes();
                      bytesRead += fileBytesWritten;
                      //logger.info("AFTER UPDATING VALUES: file Bytes written = " + fileBytesWritten + " currentTotalFileBytesWrote = " + currentTotalFileBytesWrote + ", remainingFragmentLength = " + remainingFragmentLength);
                      int updatedMsgReaderIndex = msg.readerIndex() + fileBytesWritten;
                      //logger.info("Current Msg.readerIndex BEFORE UPDATE = " + msg.readerIndex() + " And msg.writerIndex = " + msg.writerIndex());
                      //msg.readerIndex(msg.readerIndex() + msg.readableBytes());
                      //since the ByteBuf.nioBuffer method does not increase the reader index, I must increase it manually
                      // Increase the reader index of (ByteBuf) msg by the readableBytes
                      //msg.readerIndex(msg.readableBytes());
                      msg.readerIndex(msg.readerIndex() + fileBytesWritten);
                      //logger.info("UPDATED MSG READER INDEX = Current Msg.readerIndex: " + msg.readerIndex() + " + File Bytes Writen: " + fileBytesWritten + " = updatedMsgReaderIndex: " + updatedMsgReaderIndex);

                    } else {
                      //logger.info("DID NOT UPDATE ANY VALUES BECAUSE: (fileBytesWritten(" + fileBytesWritten + " > -1)");
                    }


                    //if (!theByteBuffer.hasRemaining()) {
                    //If done reading the file fragment then register the file ack
                    if (remainingFragmentLength <= 0){
                      timeEnded = System.currentTimeMillis();
                      //logger.info("\n***************************--------------******************\n DATA CHANNEL: " + myDataChannelId + ", THREAD ID:" + threadId+ ", TIME STARTED: " + timeStarted + ", TIME ENDED: " + timeEnded + ", BYTES READ: " + bytesRead +" \n************************------------------------------*********************************\n");
                      //Tell Conrol Channel via the FileReceiver that we are done reading the file fragment  //String aPathAliasName, int aControlChannelId, int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime )
                      //registerFileAck(int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime ){
                      if (myControlChannelHandler != null) {
                        //This Data Channel registers the file ack with the Control Channel, if the Control Channel received all file fragments for this FileID it sends an Acknowledgement back
                        myControlChannelHandler.registerAndSendFileAck(myDataChannelId, fileId, bytesRead, timeStarted, timeEnded);
                        logger.info("FileDataChannelHandler: " + myDataChannelId + "for Control Channel: " + myControlChannelId + " for Path: " + theAliasPath + " REGISTERED FILE ACK for FileId: " + fileId);
                      } else {
                        logger.info("FileDataChannelHandler: " + myDataChannelId + "for Control Channel: " + myControlChannelId + " for Path: " + theAliasPath + " DID NOT REGISTER File Ack for FileId: " + fileId + " CONTROL HANDLER IS NULL");
                      }

                      //FileReceiver.ControlChannelHandlerAndFileAckObject myControlChannelHandlerAndFileAckObject = FileReceiver.registerFileAck(theAliasPath,myControlChannelId,myDataChannelId,fileId,bytesRead,timeStarted,timeEnded);

                      /*
                      if (myControlChannelHandlerAndFileAckObject != null) {
                        //This was the last data channel to report the file fragment for the fileID, so get the control handler and send the file ack
                        FileReceiverHandler theControlChannelHandler = myControlChannelHandlerAndFileAckObject.getControlChannelHandler();
                        if (theControlChannelHandler != null){
                          //SENDING FILE ACK THROUGH THE CONTROL CHANNEL HANDLER
                          theControlChannelHandler.sendFileAck(myControlChannelHandlerAndFileAckObject.getFileId(),myControlChannelHandlerAndFileAckObject.getBytesRead(),myControlChannelHandlerAndFileAckObject.getStartTime(),myControlChannelHandlerAndFileAckObject.getEndTime());
                          //logger.info("\n***************************--------------******************\n CONTROL CHANNEL SENT FILE ACK VIA DATA CHANNEL: " + myDataChannelId + ", THREAD ID:" + threadId+ ", TIME STARTED: " + timeStarted + ", TIME ENDED: " + timeEnded + ", BYTES READ: " + bytesRead +" \n************************------------------------------*********************************\n");
                        }
                        else{
                          //logger.info("\n***************************--------------******************\n (CAN NOT SEND FILE ACK THROUGH CONTROL CHANNEL, THE CONTROL HANDLER IS NULL) DATA CHANNEL: " + myDataChannelId + ", THREAD ID:" + threadId+ ", TIME STARTED: " + timeStarted + ", TIME ENDED: " + timeEnded + ", BYTES READ: " + bytesRead +" \n************************------------------------------*********************************\n");
                        }


                        //myControlChannelHandlerAndFileAckObject.getControlChannelHandler().sendFileAck(myControlChannelHandlerAndFileAckObject.getFileId(),myControlChannelHandlerAndFileAckObject.getBytesRead(),myControlChannelHandlerAndFileAckObject.getStartTime(),myControlChannelHandlerAndFileAckObject.getEndTime());
                      }
                      */

                      //READ IN FILE FRAGMENT but don't really need to set this I can just reset the readIn variables before this
                      //readInFileFragment = true;

                      //NEED TO RESET ALL OF THE BOOLEAN

                      //Reset Msg Type & MsgTypeBuffer
                      msgTypeSet = false;
                      msgTypeBuf.clear();
                      msgType = -1;

                      fileNameStringSizeSet = false;
                      readInFileNameString = false;
                      readInOffset = false;
                      readInFragmentLength = false;
                      readInFileId = false;
                      readInFileFragment = false;

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
                      //timeStartedSet = false;

                    }
                  }
                }//End else if file fragment
                else {
                  //logger.info("FileReceiverHandler: " + channelTypeString +", Thread ID: " + threadId + "ChannelRead: msg.readableBytes(" + msg.readableBytes() + ") >= 1");
                  ByteBuf tempBuf = Unpooled.buffer(INT_SIZE);
                  tempBuf.writeBytes(msg, ((tempBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : tempBuf.writableBytes()));
                  if (tempBuf.readableBytes() >= INT_SIZE) {
                    //logger.info("tempBuf.getInt(tempBuf.readerIndex(" + tempBuf.readerIndex() + "))");
                    int tempNum = tempBuf.getInt(tempBuf.readerIndex());//Get Size at index = 0;
                    //logger.info("FileReceiverHandler: " + channelTypeString +", Thread ID: " + threadId + ", ChannelRead: after reading 4 bytes, msg.readableBytes() = " + msg.readableBytes() + ", and the value of the readable bytes =  " +  tempNum);
                  }

                }

              }//End Else
          }else if (msgType == DONE_MSG_TYPE) {
            logger.info("\n********** RECEIVED THE DONE MSG TYPE *****************\n");
            if (!readInPrintThroughputMsg) {
              printThroughputMsgTypeBuf.writeBytes(msg, ((printThroughputMsgTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : printThroughputMsgTypeBuf.writableBytes()));
              //logger.info("FileReceiverServer: ProcessConnectionMsg: controlChannelIdBuf.writableBytes() = " + controlChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
              if (printThroughputMsgTypeBuf.readableBytes() >= 4) {
                myPrintThroughputMsgVal = printThroughputMsgTypeBuf.getInt(printThroughputMsgTypeBuf.readerIndex());//Get Size at index = 0;
                readInPrintThroughputMsg = true;
                FileReceiver.printAllThreadIds();
                logger.info("\n********** RECEIVED THE PRINT THROUGHPUT MSG TYPE *****************\n");
                FileReceiver.printAllThroughputToScreen();
                logger.info("\n********** PRINTED THE OVERALL THROUGHPUT *********************\n");
                msgTypeSet = false;
                msgTypeBuf.clear();
                msgType = -1;
                //logger.info("FileReceiverHandler(" + threadId + "): ProcessConnectionMsg: READ IN CONTROL CHANNEL: " + myControlChannelId);
              }

            }
            //FileReceiver.printAllThroughputToScreen();
            //Reset Msg Type & CONNECTION MSG BUFF
            /*
            msgTypeSet = false;
            msgTypeBuf.clear();
            msgType = -1;
            */
          }else{
            logger.info("FileReceiverHandler: Error Attempting to process an invalid Msg: ***************BREAK************************");
            break;
          }

        }//End While


      }catch(Exception e){
        System.err.printf("ChannelRead Error Msg: " + e.getMessage() + "\n");
        e.printStackTrace();

      }
    }//End Read Method


  // If this method is called, then this class is a Control Channel, only data channels
  //calls this control channel's registerFileAck method
  // Only Data Channels should call this method, as a data channel will be the only one registering File Acks
  //This method assumes a control object exist since all channels already registered. This method also assumes each
  // Control Object has a ControlChannelHandler
  //Input
  //Output: returns the ControlChannelHandlerAndFileAckObject - if the data channel calling this method is the last one registering the fileACK
  //        else NULL is returned - indicating this is not the last data channel that is registering the file ack
  //----public static synchronized FileReceiver.ControlChannelHandlerAndFileAckObject registerFileAck(String aPathAliasName, int aControlChannelId, int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime ){
  public void registerAndSendFileAck(int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime ){
    try {

      long minStartTime = 0;
      long maxEndTime = 0;
      long totalBytesRead = 0;
      boolean minStartTimeSet = false;
      boolean maxEndTimeSet = false;

      if (myFileAckHashMap != null ) {
        //See if the FileId exist for the FileAckMap
        if (!myFileAckHashMap.containsKey(String.valueOf(aFileId))) {
          //If Not: Create the File Ack List for this File Id
          myFileAckHashMap.put(String.valueOf(aFileId), new ArrayList<FileAckObject>());
        }

        //Get the List of File Acks for this FileID which exists now, if it didn't before
        ArrayList<FileAckObject> myFileAckList = myFileAckHashMap.get(String.valueOf(aFileId));
        // Add the file ack to the existing file ack list for this File Id
        myFileAckList.add(new FileAckObject(aDataChannelId, theBytesRead, theStartTime, theEndTime));
        //logger.info("FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", REGISTERED FILE ACK FOR FILE " + aFileId +", ALSO SIZE OF FILE ACK LIST = " + myFileAckList.size() + ", NUMBER OF PARALLEL DATA CHANNELS = " + myControlChannelObject.getParallelDataChannelNum());
        //See if all data channels for this Control Channel reported
        // they received the file fragment for this FileId
        if (myFileAckList.size() >= myParallelNum) {
          //ALL DATA CHANNELS REPORTED RECEIVING THE FILE FRAGMENT FOR THE FILE ID
          //Iterate through the FileAckObject List & Get the Min Start Time, Max End Time and the Total Bytes Read
          for (FileAckObject aFileAckObject : myFileAckList) {
            //Get StartTime
            if (!minStartTimeSet) {
              if (aFileAckObject.getStartTime() > 0) {
                minStartTime = aFileAckObject.getStartTime();
                minStartTimeSet = true;
              }
            } else {
              //Get the min time
              minStartTime = ((aFileAckObject.getStartTime() < minStartTime) ? aFileAckObject.getStartTime() : minStartTime);
            }

            if (!maxEndTimeSet) {
              if (aFileAckObject.getEndTime() > 0) {
                maxEndTime = aFileAckObject.getEndTime();
                maxEndTimeSet = true;
              }
            } else {
              //Get the max time
              maxEndTime = ((aFileAckObject.getEndTime() > maxEndTime) ? aFileAckObject.getEndTime() : maxEndTime);
            }
            totalBytesRead += aFileAckObject.getBytesRead();
          }
          //logger.info("\n****FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", MIN START TIME = " + minStartTime + ", MAX END TIME = " + maxEndTime + ", BYTES READ =  " + totalBytesRead + "******\n" );
          //SEND THE FILE ACK TO THE SENDER
          //Msg Type: FILE_ACK_MSG_TYPE = 2
          ByteBuf theMsgTypeBuf = Unpooled.copyInt(FILE_ACK_MSG_TYPE);
          //Send FileId
          ByteBuf theFileIdBuf = Unpooled.copyInt(aFileId);
          //Bytes Read
          ByteBuf theBytesReadBuf = Unpooled.copyLong(totalBytesRead);
          //Start Time
          ByteBuf theStartTimeBuf = Unpooled.copyLong(minStartTime);
          //End Time
          ByteBuf theEndTimeBuf = Unpooled.copyLong(maxEndTime);

          myChannelCtx.write(theMsgTypeBuf);
          myChannelCtx.write(theFileIdBuf);
          myChannelCtx.write(theBytesReadBuf);
          myChannelCtx.write(theStartTimeBuf);
          myChannelCtx.write(theEndTimeBuf);
          myChannelCtx.flush();

          //Add new ControlChannelHandlerAndFileAckObject - contains total bytes read, start time and end time
          //myControlChannelHandlerAndFileAckObject = new FileReceiver.ControlChannelHandlerAndFileAckObject(myControlChannelHandler, aFileId, totalBytesRead, minStartTime, maxEndTime);

          //Add total bytes read, start time and end time to the control channel object
          //Note the Control Channel Object it self will determine the min start time and Max End Time when passing in these values
          this.setMinStartTime(minStartTime);
          this.setMaxEndTime(maxEndTime);
          this.addToTotalBytes(totalBytesRead);

          //remove FileId Entry from the FileAckMap
          myFileAckHashMap.remove(String.valueOf(aFileId));

        }
      }


      //return myControlChannelHandler;
      //return myControlChannelHandlerAndFileAckObject;

    }catch(Exception e){
      System.err.printf("RegisterChannel Error: " + e.getMessage());
      e.printStackTrace();
      //return null;
    }
  }

  public void setMinStartTime(long aVal){
    try {
      if (!myMinStartTimeSet) {
        if (aVal > 0) {
          myMinStartTime = aVal;
          myMinStartTimeSet = true;
        }
      } else {
        if (aVal > 0) {
          myMinStartTime = ((aVal < myMinStartTime) ? aVal : myMinStartTime);
        }
      }
    }catch(Exception e){
      System.err.printf("FileReceiver: ControlChannelObject: setMinStartTime error msg: " + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }//End SetMinStartTime

  public long getMinStartTime(){
    return myMinStartTime;
  }

  public void setMaxEndTime(long aVal){
    try {
      if (!myMaxEndTimeSet) {
        if (aVal > 0) {
          myMaxEndTime = aVal;
          myMaxEndTimeSet = true;
        }
      } else {
        if (aVal > 0) {
          myMaxEndTime = ((aVal > myMaxEndTime) ? aVal : myMaxEndTime);
        }
      }
    }catch(Exception e){
      System.err.printf("FileReceiver: ControlChannelObject: setMaxEndTime: error msg: " + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }//End SetMinStartTime

  public long getMaxEndTime(){
    return myMaxEndTime;
  }

  public void addToTotalBytes(long aVal){
    try {
      if (aVal > 0) {
        myTotalBytesRead += aVal;
      }
    }catch(Exception e){
      System.err.printf("FileReceiver: ControlChannelObject: addToTotalBytes: error msg: " + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }

  public long getTotalBytesRead(){
    return myTotalBytesRead;
  }

  public boolean getMinStartTimeSet(){
    return myMinStartTimeSet;
  }

  public boolean getMaxEndTimeSet(){
    return myMaxEndTimeSet;
  }

  public long getThreadId() {
    return threadId;
  }

  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }

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

/*
        while (msg.readableBytes() >= 1) {
          if (!timeStartedSet) {
            timeStarted = System.currentTimeMillis();
          }
          logger.info("******  FileReceiverServer: ChannelRead: msg.readableBytes(" + msg.readableBytes() + ") >= 1  and msg.capacity = " + msg.capacity() +"  *********");


          //Close this client channel
          //ctx.channel().close();
          // Then close the parent channel (the one attached to the bind)
          //ctx.channel().parent().close();



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
        */