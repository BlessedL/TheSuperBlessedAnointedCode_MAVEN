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
package nia.BlessedLavoneCodeWithProxyServerExample2.filesenderdir;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.*;
import io.netty.handler.stream.ChunkedNioFile;
import java.util.logging.*;
import java.util.*;
import java.io.*;
//import java.nio.channels.*;
import java.nio.channels.FileChannel;


/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class FileSenderHandler extends SimpleChannelInboundHandler<ByteBuf> {
//How do I Receive String and File Data

    //private final String theFilePath;
    /*
    private Logger logger;
    private long currentOffset;
    private File emptyFile;
    private RandomAccessFile f;
    private FileChannel fc;
    private boolean readLengthOfFile = false;
    */
    /**
     * Creates a client-side handler.
     */
    private Logger logger;
    private int myParallelNum, myConcurrencyNum;
    private Channel myChannel; //I also can try using the ChannelContextHandler ctx
    private ChannelHandlerContext ctx;
    private FileSender myFileSender;
    private ByteBuf msgTypeBuf, replyTypeBuf;
    private boolean msgTypeReceived, finishedProcessingConnectionAckMsgType, allControlChannelsReceivedConnectAckMsg;
    private boolean doneReadingFileRequests;
    private int msgType;
    private boolean replyTypeSet;

    //myFileRequestList is volatile so it can read right from memory instead
    //of the local cache, which means multiple threads can read from myFileRequestList
    //without race conditions and get the updated value, however writes are a different story
    private volatile ArrayList<String> myFileRequestList;

    public final int CONNECTION_MSG_TYPE = 1;
    public final int CONNECTION_ACK_MSG_TYPE = 1;
    public final int SET_UP_MSG_TYPE = 2;
    //public final int SET_UP_MSG_TYPE = 2;
    public final int FILE_MSG_TYPE = 2;
    public final int FILE_ACK_MSG_TYPE = 2;

    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    public int INT_SIZE = 4;

    public int myFileId;
    public String myChannelTypeString;
    public long threadId;
    public int replyType;
    public int myDataChannelId;



    public FileSenderHandler()throws Exception {

        ctx = null;

        logger = Logger.getLogger(FileSenderHandler.class.getName());
        myParallelNum = 1;
        myConcurrencyNum = 1;

        msgTypeBuf = Unpooled.buffer(INT_SIZE);
        msgTypeReceived = false; finishedProcessingConnectionAckMsgType = false;
        allControlChannelsReceivedConnectAckMsg = false;
        msgType = -1;
        doneReadingFileRequests = false;
        myFileId = 0;
        threadId = -1;
        replyTypeSet = false;
        replyTypeBuf = Unpooled.buffer(INT_SIZE);
        replyType = -1;
        myDataChannelId = -1;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      try {
          this.ctx =  ctx;

          threadId = Thread.currentThread().getId();

          logger.info("******************************************************");
          logger.info("FileSender:startFileSender ThreadId = " + threadId);
          logger.info("******************************************************");

          //logger.info("FileSenderControlHandler: ChannelActive: for Path: " + myPathString + " The channels who were registered were: " + theRegisteredChannels);
          ///////////////////////////////////
          // Send Connection Msg
          //////////////////////////////////
          this.sendConnectionMsg();



          //String fileRequest = "transfer N0/root/1GB_File.dat N1/tmp/1GB_File_Copy.dat";
          //String fileRequest = "transfer N0/home/lrodolph/100MB_File.dat N1/home/lrodolph/100MB_File_Copy.dat";

        ///////////////////////////////
        //Parse current File Request
        //////////////////////////////
        /*
          transfer W7/home/lrodolph/1GB_File.dat W11/home/lrodolph/1GB_File_Copy.dat
          TOKENS[0] = transfer
          TOKENS[1] = W7/home/lrodolph/1GB_File.dat
          TOKENS[2] = W11/home/lrodolph/1GB_File_Copy.dat
         */

          /*
          String[] tokens = fileRequest.split("[ ]+");
          //Parse out Source File Path
          int begginingOfFilePath = tokens[1].indexOf('/');
          String aliasSource = tokens[1].substring(0, begginingOfFilePath); //-->A
          String theSrcFilePath = tokens[1].substring(begginingOfFilePath, tokens[1].length());

          //////////////////////////////////////////////////////////////////////
          //Destination File length, File Name with direcotry path, and FileId
          // is the same for all Data Channels
          /////////////////////////////////////////////////////////////////////
          //Parse out Destination File Path
          int begginingOfFilePathForDest = tokens[2].indexOf('/');
          String aliasDest = tokens[2].substring(0, begginingOfFilePathForDest); //-->C
          String theDestFilePath = tokens[2].substring(begginingOfFilePathForDest, tokens[2].length());

          //Get the Dest File Path In Bytes
          byte[] theDestFilePathInBytes = theDestFilePath.getBytes();
          //Get the length of theFilePath
          int theDestSize = theDestFilePathInBytes.length;
          //Copy the Dest File Path length to the ByteBuf
          ByteBuf theDestSizeBuf = Unpooled.copyInt(theDestSize);
          //Copy the theDestFilePathInBytes to the Byte Buf
          ByteBuf theDestFileBuf = Unpooled.copiedBuffer(theDestFilePathInBytes);
          //Copy the FileId to a byteBuf
          ByteBuf theFileIdBuf = Unpooled.copyInt(myFileId);

          //Get the file
          File theFile = new File(theSrcFilePath);
          FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
          //raf = new RandomAccessFile(theSrcFilePath,"r");
          long length = theFileChannel.size();
          long offSet = 0;
          long remainingFragmentSize = length;
          long currentFragmentSize = 0;
          int parallel_counter = 0;
          long leftOverBytes = 0;

          //get the file fragment size that will be sent through each parallel TCP Stream
          currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));

          leftOverBytes = length - (currentFragmentSize * myParallelNum);
          logger.info("FileSenderHandler:Active: File Length = " + length + ", Current Fragment Size = " + currentFragmentSize + " leftOverBytes = " + leftOverBytes);

          //////////////////////////////////////////////
          //Iterate through the DataChannelObject List
          //Start Sending the files
          //////////////////////////////////////////

          //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

          //if fragment size did not divide evenly meaning there were a few left
          //over bytes, add it to the last task object
          if ((parallel_counter + 1) >= myParallelNum) {
            currentFragmentSize += leftOverBytes;
          }                                                                          //1MB
          //currentFragmentSize = 797;
          //offSet = 577;
          //First send out the File Headers and Flush out the data
          //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID
          ByteBuf offSetBuf = Unpooled.copyLong(offSet);
          ByteBuf currentFragmentSizeBuf = Unpooled.copyLong(currentFragmentSize);
          //Send the file Headers: FileName Length, the FileName, the Offset, the file fragment length, the file Id
          ctx.write(theDestSizeBuf);
          //does theCtx.write(theDestSizeBuf); increase the writer and reader index of theDestSizeBuf
          ctx.write(theDestFileBuf);
          ctx.write(offSetBuf);
          ctx.write(currentFragmentSizeBuf);
          ctx.write(theFileIdBuf);
          ctx.flush();

          //Send the File Fragment for this data channel
          ctx.write(new ChunkedNioFile(theFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 100));
          ctx.flush();
          //Update offset
          offSet += currentFragmentSize;
          //Update remaining size
          remainingFragmentSize -= currentFragmentSize;
          //Update the parallel counter
          parallel_counter++;

          //get file request
          //on the receiving end add the frame decoder               //Get the number of data channels and divide file among data channels
          //send the file                                            //Have all concurrent channels share the path queue, pass it in as a parameter
        //End if File Request list = null
       */
      }catch(Exception e){
         System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
         e.printStackTrace();
      }
      }  //End channelActive


    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        try {
          //Read in Reply
            while (msg.readableBytes() >= 1) {
                //Read in Msg Type
                if (!replyTypeSet) {
                    replyTypeBuf.writeBytes(msg, ((replyTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : replyTypeBuf.writableBytes()));
                    //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
                    if (replyTypeBuf.readableBytes() >= 4) {
                        replyType = replyTypeBuf.getInt(replyTypeBuf.readerIndex());//Get Size at index = 0;
                        if (replyType == CONNECTION_ACK_MSG_TYPE) {
                            replyTypeSet = true;
                            logger.info("****************** FileSenderHandler(" + threadId + "): channelRead: READ IN THE Reply Type, Reply Type = CONNECTION_ACK_MSG_TYPE: " + replyType + "*****************");
                            //SEND File
                            //sendFile();
                            //sendSetupMsg();
                            //reset reply type
                            replyTypeSet = false;
                            replyTypeBuf.clear();
                        }
                        else {
                            if (replyType == FILE_ACK_MSG_TYPE) {
                                replyTypeSet = true;
                                logger.info("FileSenderHandler(" + threadId + "): channelRead: READ IN THE Reply Type, Reply Type = FILE_ACK_MSG_TYPE: " + replyType);
                            }
                        }
                    }
                }
            }

            }catch(Exception e){
            System.err.printf("FileSenderHandler: ChannelRead0: Error: "+e.getMessage());
            e.printStackTrace();
        }

    }

    public void sendConnectionMsg(){
        try {
            //Get msg type: Connection Msg Type
            int msgType = CONNECTION_MSG_TYPE;

            //Get the Path in IP Address Format without the source node
            //String myIpAddStringWithOutSrcNode = myPath.toStringWithoutSourceNodeAndDestFileName();
            //                                          W7           W12
            String myIpAddStringWithOutSrcNode = "192.168.0.1:4959,192.168.1.2:4959"; //W7,W12
            //Get length of path (without source node - the ip Address version) and the actual path without the source node
            byte[] myPathInBytes = myIpAddStringWithOutSrcNode.getBytes();
            int myPathSize = myPathInBytes.length;

            //String myAliasPathString = myPath.toStringAliasNames();
            String myAliasPathString = "WS5,WS7,WS12";
            //Get length of Alias path and then get the Alias Path
            byte[] myAliasPathInBytes = myAliasPathString.getBytes();
            int myAliasPathSize = myAliasPathInBytes.length;

            int myChannelType = 0;
            int myControlChannelId = 1;
            int myDataChannelId = -1;
            int myParallelNum = 3;
            int myConcurrencyNum = 4;

            //BYTE BUFS BEING SENT
            ByteBuf myMsgTypeBuf = Unpooled.copyInt(msgType);
            ByteBuf myPathSizeBuf = Unpooled.copyInt(myPathSize);
            ByteBuf myPathBuf = Unpooled.copiedBuffer(myPathInBytes);
            ByteBuf myAliasPathSizeBuf = Unpooled.copyInt(myAliasPathSize);
            ByteBuf myAliasPathBuf = Unpooled.copiedBuffer(myAliasPathInBytes);
            ByteBuf myConnectionTypeBuf = Unpooled.copyInt(myChannelType);
            ByteBuf myControlChannelIdBuf = Unpooled.copyInt(myControlChannelId);
            ByteBuf myDataChannelIdBuf = Unpooled.copyInt(myDataChannelId);
            ByteBuf myParallelNumBuf = Unpooled.copyInt(myParallelNum);
            ByteBuf myConcurrencyNumBuf = Unpooled.copyInt(myConcurrencyNum);


            //Write/Send out the Connection Msg ByteBuf's
            this.ctx.write(myMsgTypeBuf);
            logger.info("FileSenderHandler:SendConnectionMsg: Wrote the CONNECTION_MSG_TYPE ");
            this.ctx.write(myPathSizeBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Size of the IP Path: " + myPathSize);
            this.ctx.write(myPathBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the IP Path: " + myIpAddStringWithOutSrcNode);
            this.ctx.write(myAliasPathSizeBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Size of the Alias Path: " + myAliasPathSize);
            this.ctx.write(myAliasPathBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Alias Path: " + myAliasPathString);
            this.ctx.write(myConnectionTypeBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Connection Type: " + myChannelType);
            this.ctx.write(myControlChannelIdBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Control Channel ID: " + myControlChannelId);
            this.ctx.write(myDataChannelIdBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Data Channel ID: " + myDataChannelId);
            this.ctx.write(myParallelNumBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Parallel Num: " + myParallelNum);
            this.ctx.write(myConcurrencyNumBuf);
            logger.info("FileSenderHandler: SendConnectionMsg: Wrote the Parallel Num: " + myConcurrencyNum);
            //Flush out the connection msg to the wire
            this.ctx.flush();
            logger.info("SendConnectionMsg: FLUSHED THE CONNECTION MSG For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);


        }catch(Exception e){
            System.err.printf("FileSenderHandler:SendConnectionMsg: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//

    public void sendFile(){
        try {

            String fileRequest = "transfer WS5/home/lrodolph/500MB_File.dat WS12/home/lrodolph/500MB_File_Copy.dat";
            logger.info("Process Connection Ack. File Request = " + fileRequest);
            if (fileRequest != null) {
                //Increment File ID
                myFileId++;
                    ///////////////////////////////
                    //Parse current File Request
                    //////////////////////////////

                    // transfer WS5/home/lrodolph/500MB_File.dat WS12/home/lrodolph/500MB_File_Copy.dat";
                    // TOKENS[0] = transfer
                    //TOKENS[1] = WS5/home/lrodolph/500MB_File.dat
                    //TOKENS[2] = WS12/home/lrodolph/500MB_File_Copy.dat
                    //Split the file request based on white space
                    String[] tokens = fileRequest.split("[ ]+");
                    //Parse out Source File Path
                    int begginingOfFilePath = tokens[1].indexOf('/');
                    String aliasSource = tokens[1].substring(0, begginingOfFilePath); //-->A
                    String theSrcFilePath = tokens[1].substring(begginingOfFilePath, tokens[1].length());

                    //////////////////////////////////////////////////////////////////////
                    //Destination File length, File Name with direcotry path, and FileId
                    // is the same for all Data Channels
                    /////////////////////////////////////////////////////////////////////
                    //Parse out Destination File Path
                    int begginingOfFilePathForDest = tokens[2].indexOf('/');
                    String aliasDest = tokens[2].substring(0, begginingOfFilePathForDest); //-->C
                    String theDestFilePath = tokens[2].substring(begginingOfFilePathForDest, tokens[2].length());

                    //Get the Dest File Path In Bytes
                    byte[] theDestFilePathInBytes = theDestFilePath.getBytes();
                    //Get the length of theFilePath
                    int theDestSize = theDestFilePathInBytes.length;
                    //Copy the Dest File Path length to the ByteBuf
                    ByteBuf theDestSizeBuf = Unpooled.copyInt(theDestSize);
                    //Copy the theDestFilePathInBytes to the Byte Buf
                    ByteBuf theDestFileBuf = Unpooled.copiedBuffer(theDestFilePathInBytes);
                    //Copy the FileId to a byteBuf
                    ByteBuf theFileIdBuf = Unpooled.copyInt(myFileId);

                    //Get the file
                    File theFile = new File(theSrcFilePath);
                    FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
                    //raf = new RandomAccessFile(theSrcFilePath,"r");
                    long length = theFileChannel.size();
                    long offSet = 0;
                    long remainingFragmentSize = length;
                    long currentFragmentSize = length;
                    int parallel_counter = 0;
                    long leftOverBytes = 0;

                    //START SENDING THE FILE
                    ////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////
                    ByteBuf fileMsgTypeBuf = Unpooled.copyInt(FILE_MSG_TYPE);
                    ByteBuf offSetBuf = Unpooled.copyLong(offSet);
                    ByteBuf currentFragmentSizeBuf = Unpooled.copyLong(currentFragmentSize);
                    //Send the File Msg Type
                    this.ctx.write(fileMsgTypeBuf);

                    //Send the file Headers: FileName Length, the FileName, the Offset, the file fragment length, the file Id
                    this.ctx.write(theDestSizeBuf);
                    logger.info("***FileSenderHandler: CHANNEL " + myDataChannelId + " SENT THE SIZE (# OF CHARACTERS IN THE FILE NAME & PATH) ");
                    this.ctx.write(theDestFileBuf);
                    logger.info("***FileSenderHandler: CHANNEL " + myDataChannelId + " SENT THE ACTUAL FILE NAME & PATH");
                    this.ctx.write(offSetBuf);
                    logger.info("***FileSenderHandler: CHANNEL " + myDataChannelId + " SENT THE OFFSET: " + offSet);
                    this.ctx.write(currentFragmentSizeBuf);
                    logger.info("***FileSenderHandler: CHANNEL " + myDataChannelId + " SENT THE FRAGMENT SIZE:  " + currentFragmentSize);
                    this.ctx.write(theFileIdBuf);
                    logger.info("***FileSenderHandler: CHANNEL " + myDataChannelId + " SENT THE FILE ID:  " + myFileId );
                    this.ctx.flush();

                    //Send the File Fragment for this data channel
                    this.ctx.write(new ChunkedNioFile(theFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 1));
                    this.ctx.flush();

                } //End if File Request list = null

        }catch(Exception e){
            System.err.printf("FileSenderHandler: sendFile Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendSetupMsg(){
        try {

            ////////////////////////////////////////////////////////////
            //String myAliasPathString = myPath.toStringAliasNames();
            String myAliasPathString = "WS5,WS7,WS12";
            //Get length of Alias path and then get the Alias Path
            byte[] myAliasPathInBytes = myAliasPathString.getBytes();
            int myAliasPathSize = myAliasPathInBytes.length;
            ByteBuf myAliasPathSizeBuf = Unpooled.copyInt(myAliasPathSize);
            //I can also use copiedBuffer(CharSequence string, Charset charset)
            ByteBuf myAliasPathBuf = Unpooled.copiedBuffer(myAliasPathInBytes);

            //Get msg type: Connection Msg Type
            msgType = SET_UP_MSG_TYPE;
            ByteBuf myMsgTypeBuf = Unpooled.copyInt(msgType);

            //Get the parallel num
            ByteBuf myParallelNumBuf = Unpooled.copyInt(myParallelNum);
            //Get the Concurrency num
            ByteBuf myConcurrencyNumBuf = Unpooled.copyInt(myConcurrencyNum);

            //Write/Send out the Connection Msg ByteBuf's
            this.ctx.write(myMsgTypeBuf);
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote the Set Up Msg Type: " + msgType);
            this.ctx.write(myAliasPathSizeBuf);
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote the alias path size: " + myAliasPathSize);
            this.ctx.write(myAliasPathBuf);
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote the alias path: " + myAliasPathString);
            this.ctx.write(myParallelNumBuf);
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote the parallelNum: " + myParallelNum);
            this.ctx.write(myConcurrencyNumBuf);
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote the concurrencyNum: " + myConcurrencyNum);
            //Flush out the connection msg to the wire
            this.ctx.flush();
            logger.info("FileSenderHandler: sendSetUpMsg: Wrote flushed the Msg");

            /////////////////////////////////////////////////////////////



        }catch(Exception e){
            System.err.printf("FileSenderHandler:SendConnectionMsg: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
