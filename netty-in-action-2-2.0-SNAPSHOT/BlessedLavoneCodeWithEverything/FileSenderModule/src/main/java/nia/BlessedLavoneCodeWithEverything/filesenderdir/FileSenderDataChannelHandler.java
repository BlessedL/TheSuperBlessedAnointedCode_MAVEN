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
package nia.BlessedLavoneCodeWithEverything.filesenderdir;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.*;
import io.netty.handler.stream.ChunkedNioFile;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.nio.*;
//import java.nio.channels.*;
import io.netty.handler.codec.LengthFieldPrepender;
import  io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import java.nio.channels.FileChannel;


/**
 * Control Channel Handler implementation
 */
public class FileSenderDataChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Logger logger;
    //private  Path myPath;
    private String myPathString, pathInIpAddressFormatWithoutSrc;
    private int myChannelType, myControlChannelId, myDataChannelId;
    private int myParallelNum, myConcurrencyNum;
    private Channel myChannel; //I also can try using the ChannelContextHandler ctx
    private ChannelHandlerContext ctx;
    private FileSender myFileSender;
    private ByteBuf msgTypeBuf;
    private boolean msgTypeReceived, finishedProcessingConnectionAckMsgType, allControlChannelsReceivedConnectAckMsg;
    private boolean doneReadingFileRequests;
    private int msgType;
    private  List<FileSender.DataChannelObject> myDataChannelObjectList;
    //              FileId, Expected File Ack corresponding to the File Id
    //private HashMap<String,ArrayList<ExpectedFileFragmentAck>> myFileAckList;

    private volatile ArrayList<String> myFileRequestList;

    public final int CONNECTION_MSG_TYPE = 1;
    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    public int INT_SIZE = 4;
    public final int CONNECTION_ACK_MSG_TYPE = 1;
    public final int FILE_MSG_TYPE = 2;
    public int myFileId;
    public String myChannelTypeString;

    //FileSenderHandler(theFileRequest,theOffset,theCurrentFragmentSize,theDataChannelId));
    public FileSenderDataChannelHandler(String aPathInIpAddressFormatWithoutSrc, String aPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum) throws Exception {
        //this.myPath = thePath;
        this.pathInIpAddressFormatWithoutSrc = aPathInIpAddressFormatWithoutSrc;
        this.myPathString = aPathString;
        this.myChannelType = aChannelType;
        //this.myChannelTypeString = "NOT_SET";
        //this.myChannelTypeString = ((this.myChannelType == CONTROL_CHANNEL_TYPE)?"CONTROL_CHANNEL":"DATA_CHANNEL");
        myChannelTypeString = "DATA_CHANNEL";
        this.myControlChannelId = aControlChannelId;
        this.myDataChannelId = aDataChannelId;
        this.myConcurrencyNum = aConcurrencyNum;
        this.myParallelNum = aParallelNum;
        myChannel = null;
        ctx = null;
        myDataChannelObjectList = null;
        //myFileAckList = new HashMap<Integer,ArrayList<FileSender.ExpectedFileFragmentAck>>();
        //myFileAckList = null;
        logger = Logger.getLogger(FileSenderDataChannelHandler.class.getName());
        //myParallelNum = -1;
        //myConcurrencyNum = -1;
        this.myFileSender = aFileSender;

        msgTypeBuf = Unpooled.buffer(INT_SIZE);
        msgTypeReceived = false; finishedProcessingConnectionAckMsgType = false;
        allControlChannelsReceivedConnectAckMsg = false;
        msgType = -1;
        doneReadingFileRequests = false;
        myFileId = 0;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      try {
          System.err.printf("\n****** FileSenderDataChannelHandler: INSIDE CHANNEL ACTIVE METHOD \n\n");
          logger.info("****** FileSenderDataChannelHandler: INSIDE CHANNEL ACTIVE METHOD");
          if (myChannelType == DATA_CHANNEL_TYPE){
              logger.info("THIS IS A DATA CHANNEL");
          }
          this.ctx = ctx;
          myChannel = ctx.channel();
          //FileSender.this.registerChannelCtx(this.ctx, myPath.toStringAliasNames(),myChannelType, myControlChannelId, myDataChannelId);
          FileSender.registerChannelCtx(myPathString, null, this.ctx, myChannelType, myControlChannelId, myDataChannelId, this);
          String theRegisteredChannels = FileSender.registeredChannelsToString();
          //logger.info("FileSenderDataChannelHandler: ChannelActive: for Path: " + myPathString + " The channels who were registered were: " + theRegisteredChannels);
          ///////////////////////////////////
          // Send Connection Msg
          //////////////////////////////////
          this.sendConnectionMsg();



      }catch(Exception e){
         System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
         e.printStackTrace();
      }
      }  //End channelActive

    public void sendConnectionMsg(){
        try {

            int msgType = CONNECTION_MSG_TYPE;

            //Get the Path in IP Address Format without the source node
            //String myIpAddStringWithOutSrcNode = myPath.toStringWithoutSourceNodeAndDestFileName();
            //String myIpAddStringWithOutSrcNode = "192.168.0.1:4959";
            //String myIpAddStringWithOutSrcNode = "192.168.0.1:4959,192.168.1.2:4959";
            String myIpAddStringWithOutSrcNode = this.pathInIpAddressFormatWithoutSrc;
            //Get length of path (without source node - the ip Address version) and the actual path without the source node
            byte[] myPathInBytes = myIpAddStringWithOutSrcNode.getBytes();
            int myPathSize = myPathInBytes.length;
            ByteBuf myPathSizeBuf = Unpooled.copyInt(myPathSize);
            //I can also use copiedBuffer(CharSequence string, Charset charset)
            ByteBuf myPathBuf = Unpooled.copiedBuffer(myPathInBytes);

            //Get msg type: Connection Msg Type
            ByteBuf myMsgTypeBuf = Unpooled.copyInt(msgType);

            //String myAliasPathString = myPath.toStringAliasNames();
            //String myAliasPathString = "WS5,WS7,WS12";
            String myAliasPathString = this.myPathString;
            //Get length of Alias path and then get the Alias Path
            byte[] myAliasPathInBytes = myAliasPathString.getBytes();
            int myAliasPathSize = myAliasPathInBytes.length;
            ByteBuf myAliasPathSizeBuf = Unpooled.copyInt(myAliasPathSize);
            //I can also use copiedBuffer(CharSequence string, Charset charset)
            ByteBuf myAliasPathBuf = Unpooled.copiedBuffer(myAliasPathInBytes);

            //Get connection msg type: Control Channel or Data Channel
            ByteBuf myConnectionTypeBuf = Unpooled.copyInt(myChannelType);
            //Get Control Channel Id
            ByteBuf myControlChannelIdBuf = Unpooled.copyInt(myControlChannelId);
            //Get Data Channel Id
            ByteBuf myDataChannelIdBuf = Unpooled.copyInt(myDataChannelId);

            //Get the parallel num
            ByteBuf myParallelNumBuf = Unpooled.copyInt(myParallelNum);
            //Get the Concurrency num
            ByteBuf myConcurrencyNumBuf = Unpooled.copyInt(myConcurrencyNum);



            //Write/Send out the Connection Msg ByteBuf's
            this.ctx.write(myMsgTypeBuf);
            this.ctx.write(myPathSizeBuf);
            //logger.info("SendConnectionMsg: Wrote the Size of the IP Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Sent SIZE OF FILE PATH \n\n");
            this.ctx.write(myPathBuf);
            //logger.info("SendConnectionMsg: Wrote the IP Path For Data CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: File & File path \n\n");
            this.ctx.write(myAliasPathSizeBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Alias Path Size \n\n");
            //logger.info("SendConnectionMsg: Wrote the Size of the Alias Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myAliasPathBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Alias Path \n\n");
            //logger.info("SendConnectionMsg: Wrote the ALIAS Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            //logger.info("SendConnectionMsg: Wrote the Alias Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myConnectionTypeBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Connection Type \n\n");
            //logger.info("SendConnectionMsg: Wrote the CONNECTION TYPE For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myControlChannelIdBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: DATA CHANNEL SENDING IT'S CONTROL CHANNNEL \n\n");
            //logger.info("SendConnectionMsg: Wrote the DATA CHANNEL ID  For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myDataChannelIdBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: DATA CHANNEL ID \n\n");

            //logger.info("SendConnectionMsg: Wrote the DATA CHANNEL ID  For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myParallelNumBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: PARALLEL NUMBER \n\n");
            //logger.info("SendConnectionMsg: Wrote the PARALLEL NUM  For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myConcurrencyNumBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Concurrency Number \n\n");
            //logger.info("SendConnectionMsg: Wrote the CONCURRENCY NUM For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            //Flush out the connection msg to the wire
            this.ctx.flush();
            //logger.info("SendConnectionMsg: FLUSHED THE CONNECTION MSG For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
        }catch(Exception e){
            System.err.printf("FileSenderHandler:SendConnectionMsg: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        try {
           logger.info("Inside Data Channel Handler");
        }catch(Exception e){
            System.err.printf("ChannelRead Error Msg: " + e.getMessage());
            e.printStackTrace();

        }
    }//End Read Method

    //aFileSenderDataChannelHandler.startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);
    public void startSendingFile(String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId){
        try {
            File aFile = new File(aSrcFilePath);
            FileChannel aFileChannel = new RandomAccessFile(aFile, "r").getChannel();

            //Get the Dest File Path In Bytes
            byte[] theDestFilePathInBytes = aDestFilePath.getBytes();
            //Get the length of theFilePath
            int theDestSize = theDestFilePathInBytes.length;
            //Copy the Dest File Path length to the ByteBuf
            ByteBuf theDestSizeBuf = Unpooled.copyInt(theDestSize);
            //Copy the theDestFilePathInBytes to the Byte Buf
            ByteBuf theDestFileBuf = Unpooled.copiedBuffer(theDestFilePathInBytes);
            //Copy the FileId to a byteBuf
            ByteBuf theFileIdBuf = Unpooled.copyInt(aFileId);

            ByteBuf fileMsgTypeBuf = Unpooled.copyInt(FILE_MSG_TYPE);
            ByteBuf offSetBuf = Unpooled.copyLong(offSet);
            ByteBuf currentFragmentSizeBuf = Unpooled.copyLong(currentFragmentSize);
            //Send the File Msg Type
            this.ctx.write(fileMsgTypeBuf);
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE MSG TYPE: FILE_MSG_TYPE ");
            //Send the file Headers: FileName Length, the FileName, the Offset, the file fragment length, the file Id
            this.ctx.write(theDestSizeBuf);
            //logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE SIZE (# OF CHARACTERS IN THE FILE NAME & PATH) ");
            //does theCtx.write(theDestSizeBuf); increase the writer and reader index of theDestSizeBuf
            this.ctx.write(theDestFileBuf);
            //logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE ACTUAL FILE NAME & PATH");
            this.ctx.write(offSetBuf);
            //logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE OFFSET: " + offSet);
            this.ctx.write(currentFragmentSizeBuf);
            //logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE FRAGMENT SIZE:  " + currentFragmentSize);
            this.ctx.write(theFileIdBuf);
            //logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE FILE ID:  " + aFileId );
            this.ctx.flush();
            //aDataChannelObject.getDataChannel().flush();

            //Send the File Fragment for this data channel
            this.ctx.write(new ChunkedNioFile(aFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 100));
            this.ctx.flush();
            //logger.info("***FileSenderControlChannelHandler: DATA CHANNEL " + myDataChannelId + " WROTE AND FLUSH THE ACTUAL FRAGMENT ");
            //aDataChannelObject.getDataChannel().flush();
        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//End Start


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
