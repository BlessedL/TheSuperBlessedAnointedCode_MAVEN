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
public class FileSenderControlChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Logger logger;
    //private  Path myPath;
    private String pathInIpAddressFormatWithoutSrc,myPathString;
    private int myChannelType, myControlChannelId, myDataChannelId;
    private int myParallelNum, myConcurrencyNum, myPipeLineNum;
    private Channel myChannel; //I also can try using the ChannelContextHandler ctx
    private ChannelHandlerContext ctx;
    private FileSender myFileSender;
    private ByteBuf msgTypeBuf, msgAckTypeBuf;
    private boolean msgTypeReceived, msgAckTypeSet, finishedProcessingConnectionAckMsgType, allControlChannelsReceivedConnectAckMsg;
    private boolean doneReadingFileRequests;
    private int msgType, msgAckType;
    private  List<FileSender.DataChannelObject> myDataChannelObjectList;
    private FileSender.ControlChannelObject myControlChannelObject;
    //              FileId, Expected File Ack corresponding to the File Id
    //private HashMap<String,ArrayList<ExpectedFileFragmentAck>> myFileAckList;

    //private volatile ArrayList<String> myFileRequestList;
    private ArrayList<String> myFileRequestList;

    //MSG Types
    public final int CONNECTION_MSG_TYPE = 1;
    public final int CONNECTION_ACK_MSG_TYPE = 1;

    public final int FILE_MSG_TYPE = 2;
    public final int FILE_ACK_MSG_TYPE = 2;
    public final int DONE_MSG_TYPE = 3;
    public final int PRINT_THROUGHPUT_MSG_TYPE = 4;

    //CHANNEL Types
    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    //ByteBuf Buffer Sizes
    public int INT_SIZE = 4;
    public int LONG_SIZE = 8;

    public int myFileId;
    public String myChannelTypeString;
    public boolean msgAckTypeReceived;
    public boolean myFileRequestListSet;
    public long threadId;

    //File Ack Variables
    private int fileId;
    private long bytesRead, startTime, endTime;
    //Flags to read in File Ack
    public boolean readInFileId;
    public boolean readInBytesRead;
    public boolean readInStartTime;
    public boolean readInEndTime;
    //Byte Bufs for File Ack
    ByteBuf fileIdBuf;
    ByteBuf bytesReadBuf;
    ByteBuf startTimeBuf;
    ByteBuf endTimeBuf;

    //FileSenderHandler(theFileRequest,theOffset,theCurrentFragmentSize,theDataChannelId));
    public FileSenderControlChannelHandler(String aPathInIpAddressFormatWithoutSrc, String aPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, int aPipelineNum) throws Exception {
        this.pathInIpAddressFormatWithoutSrc = aPathInIpAddressFormatWithoutSrc;
        //this.myPath = thePath;
        this.myPathString = aPathString; //Is this in IP Format or is this an Alias Path String
        this.myChannelType = aChannelType;
        //this.myChannelTypeString = "NOT_SET";
        //this.myChannelTypeString = ((this.myChannelType == CONTROL_CHANNEL_TYPE)?"CONTROL_CHANNEL":"DATA_CHANNEL");
        myChannelTypeString = "CONTROL_CHANNEL";
        this.myControlChannelId = aControlChannelId;
        this.myDataChannelId = aDataChannelId;
        this.myConcurrencyNum = aConcurrencyNum;
        this.myParallelNum = aParallelNum;
        this.myPipeLineNum = aPipelineNum;
        this.msgAckTypeReceived = false;
        this.msgAckTypeSet = false;
        myChannel = null;
        ctx = null;
        myControlChannelObject = null;
        myDataChannelObjectList = null;
        //myFileAckList = new HashMap<Integer,ArrayList<FileSender.ExpectedFileFragmentAck>>();
        //myFileAckList = null;
        logger = Logger.getLogger(FileSenderControlChannelHandler.class.getName());
        this.myFileSender = aFileSender;

        msgTypeBuf = Unpooled.buffer(INT_SIZE);
        msgAckTypeBuf = Unpooled.buffer(INT_SIZE);
        msgTypeReceived = false; finishedProcessingConnectionAckMsgType = false;
        allControlChannelsReceivedConnectAckMsg = false;
        msgType = -1;
        msgAckType =-1;

        if (myChannelType == CONTROL_CHANNEL_TYPE){
            //myFileRequestList = myFileSender.getFileRequestList(myPath.toStringAliasNames());
            //Or I can do the following which is synchronized which means the thread will block:
            //String fileRequest = myFileSender.getNextFileRequestFromList(myPath.toStringAliasNames());
        }
        doneReadingFileRequests = false;
        myFileId = 0;
        threadId = -1;

        //File Ack variables
        fileId = -1;
        bytesRead = -1;
        startTime = -1;
        endTime = -1;
        //File Ack Flags
        readInFileId = false;
        readInBytesRead = false;
        readInStartTime = false;
        readInEndTime = false;
        //File Ack Byte Bufs
        fileIdBuf = Unpooled.buffer(INT_SIZE);
        bytesReadBuf = Unpooled.buffer(LONG_SIZE);
        startTimeBuf =  Unpooled.buffer(LONG_SIZE);
        endTimeBuf = Unpooled.buffer(LONG_SIZE);
        myFileRequestListSet = false;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      try {
          this.ctx = ctx;
          myChannel = ctx.channel();
          //FileSender.this.registerChannelCtx(this.ctx, myPath.toStringAliasNames(),myChannelType, myControlChannelId, myDataChannelId);
          FileSender.registerChannelCtx(myPathString, this, this.ctx, myChannelType, myControlChannelId, myDataChannelId, null);
          String theRegisteredChannels = FileSender.registeredChannelsToString();
          threadId = Thread.currentThread().getId();

          //logger.info("******************************************************");
          //logger.info("FileSender:startFileSender ThreadId = " + threadId);
          //logger.info("******************************************************");

          //logger.info("FileSenderControlHandler: ChannelActive: for Path: " + myPathString + " The channels who were registered were: " + theRegisteredChannels);
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

            //String myAliasPathString = myPath.toStringAliasNames();
            //String myAliasPathString = "WS5,WS7,WS12";
            String myAliasPathString = this.myPathString;
            //Get length of Alias path and then get the Alias Path
            byte[] myAliasPathInBytes = myAliasPathString.getBytes();
            int myAliasPathSize = myAliasPathInBytes.length;
            ByteBuf myAliasPathSizeBuf = Unpooled.copyInt(myAliasPathSize);
            //I can also use copiedBuffer(CharSequence string, Charset charset)
            ByteBuf myAliasPathBuf = Unpooled.copiedBuffer(myAliasPathInBytes);

            //Get msg type: Connection Msg Type
            ByteBuf myMsgTypeBuf = Unpooled.copyInt(msgType);

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
            //logger.info("SendConnectionMsg: Wrote the Connection Msg Type for CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myPathSizeBuf);
            //logger.info("SendConnectionMsg: Wrote the Size of the IP Path For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myPathBuf);
            //logger.info("SendConnectionMsg: Wrote the IP Path For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myAliasPathSizeBuf);
            //logger.info("SendConnectionMsg: Wrote the Size of the Alias Path For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myAliasPathBuf);
            //logger.info("SendConnectionMsg: Wrote the ALIAS Path For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            //logger.info("SendConnectionMsg: Wrote the Alias Path For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);

            this.ctx.write(myConnectionTypeBuf);
            //logger.info("SendConnectionMsg: Wrote the CONNECTION TYPE For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);

            this.ctx.write(myControlChannelIdBuf);
            //logger.info("SendConnectionMsg: Wrote the CONTROL CHANNEL ID  For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myDataChannelIdBuf);
           // logger.info("SendConnectionMsg: Wrote the DATA CHANNEL ID  For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myParallelNumBuf);
            //logger.info("SendConnectionMsg: Wrote the PARALLEL NUM  For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myConcurrencyNumBuf);
            //logger.info("SendConnectionMsg: Wrote the CONCURRENCY NUM For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);


            //Flush out the connection msg to the wire
            this.ctx.flush();
            //logger.info("SendConnectionMsg: FLUSHED THE CONNECTION MSG For CONTROL CHANNEL("+myControlChannelId+") for Path: "+myAliasPathString);
        }catch(Exception e){
            System.err.printf("FileSenderHandler:SendConnectionMsg: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//

    /*
      if (!msgTypeSet) {
            msgTypeBuf.writeBytes(msg, ((msgTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : msgTypeBuf.writableBytes()));
            //logger.info("FileReceiverServer: ProcessConnectionMsg: DataChannelIdBuf.writableBytes() = " + dataChannelIdBuf.writableBytes() + " msg.readableBytes() = " + msg.readableBytes());
            if (msgTypeBuf.readableBytes() >= 4) {
              msgType = msgTypeBuf.getInt(msgTypeBuf.readerIndex());//Get Size at index = 0;
              msgTypeSet = true;
              String msgTypeString = ((msgType == CONNECTION_MSG_TYPE) ? "CONNECTION MSG TYPE" : " FILE MSG TYPE ");
              logger.info("FileReceiverHandler(" + threadId + "): channelRead: READ IN THE MSG Type, Msg Type = " + msgTypeString);
            }
          }
     */

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        try {
            while (msg.readableBytes() >= 1) {
                if (!msgAckTypeSet) {
                    msgAckTypeBuf.writeBytes(msg, ((msgAckTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : msgAckTypeBuf.writableBytes()));
                    //logger.info("FileSenderControlChannelHandler: Channel Read (" + this.myChannelTypeString + "): msgAckTypeBuf.readableBytes() =  " + msgAckTypeBuf.readableBytes());
                    if (msgAckTypeBuf.readableBytes() >= INT_SIZE) {
                        msgAckType = msgAckTypeBuf.getInt(msgAckTypeBuf.readerIndex());//Get Msg Ack Type at index = 0;
                        //logger.info("FileSenderControlChannelHandler: Channel Read: (" + this.myChannelTypeString + "): Processing MSG ACK TYPE (" + msgAckType + ")");
                        msgAckTypeSet = true;
                        //Check if the msgAckType = CONNECTION_ACK_MSG_TYPE
                        if (msgAckType == CONNECTION_ACK_MSG_TYPE) {
                           // logger.info("FileSenderControlChannelHandler: RECEIVED CONNECTION ACK MSG TYPE  and msg.readableBytes() = " + msg.readableBytes());
                            FileSender.registerConnectionAck(myPathString, myControlChannelId);
                            //Finished processing the ConnectionAckMsgType
                            finishedProcessingConnectionAckMsgType = true;
                            //Reset the variable indicating that we receiced the msg type, since we are now waiting on the next msg type
                            msgAckTypeSet = false;
                            msgAckTypeBuf.clear();
                           // logger.info("FileSenderControlChannelHandler: Channel Read already processed Connection msg Ack AND reset msg ack type to false, msg.readableBytes =  " + msg.readableBytes());
                            this.processConnectionAckMsgType(ctx);
                        }
                    }
                }else if (msgAckType == FILE_ACK_MSG_TYPE) {
                    //Read in the FileID
                    if (!readInFileId) {
                        //logger.info("FileSenderControlChannel: ChannelRead: fileIdBuf.writeBytes(msg, ((fileIdBuf.writableBytes(" + fileIdBuf.writableBytes()+") >= msg.readableBytes(" +  msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() +" ) : fileIdBuf.writableBytes(" +fileIdBuf.writableBytes() +" )))");
                        fileIdBuf.writeBytes(msg, ((fileIdBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : fileIdBuf.writableBytes()));
                        if (fileIdBuf.readableBytes() >= INT_SIZE) {
                            //logger.info("fileIdBuf.getInt(fileIdBuf.readerIndex(" + fileIdBuf.readerIndex() + "))");
                            fileId = fileIdBuf.getInt(fileIdBuf.readerIndex());//Get Size at index = 0;
                            readInFileId = true;
                            //Either remove FileId from the list of ExpectedFileAcks here or after I receive the whole msg
                            //logger.info("FileSenderControlHandler: The File ID = : " + fileId);
                        }
                    } else if (!readInBytesRead) {
                        //logger.info("FileSenderControlChannel: ChannelRead: bytesReadBuf.writeBytes(msg, ((bytesReadBuf.writableBytes(" + bytesReadBuf.writableBytes()+") >= msg.readableBytes(" +  msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() +" ) : bytesReadBuf.writableBytes(" +bytesReadBuf.writableBytes() +" )))");
                        bytesReadBuf.writeBytes(msg, ((bytesReadBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : bytesReadBuf.writableBytes()));
                        if (bytesReadBuf.readableBytes() >= LONG_SIZE) {
                            //logger.info("BytesReadBuf.getLong(bytesReadBuf.readerIndex(" + bytesReadBuf.readerIndex() + "))");
                            bytesRead = bytesReadBuf.getLong(bytesReadBuf.readerIndex());//Get Size at index = 0;
                            readInBytesRead = true;
                            //logger.info("FileSenderControlHandler: The Bytes Read = : " + bytesRead);
                        }
                    }else if (!readInStartTime) {
                        //logger.info("FileSenderControlChannel: ChannelRead: startTimeBuf.writeBytes(msg, ((startTimeBuf.writableBytes(" + startTimeBuf.writableBytes()+") >= msg.readableBytes(" +  msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() +" ) : startTimeBuf.writableBytes(" +startTimeBuf.writableBytes() +" )))");
                        startTimeBuf.writeBytes(msg, ((startTimeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : startTimeBuf.writableBytes()));
                        if (startTimeBuf.readableBytes() >= LONG_SIZE) {
                            //logger.info("StartTimeBuf.getLong(bytesReadBuf.readerIndex(" + startTimeBuf.readerIndex() + "))");
                            startTime = startTimeBuf.getLong(startTimeBuf.readerIndex());//Get Size at index = 0;
                            readInStartTime = true;
                            //logger.info("FileSenderControlHandler: The StartTime = : " + startTime);
                        }
                    }else if (!readInEndTime) {
                        //logger.info("FileSenderControlChannel: ChannelRead: endTimeBuf.writeBytes(msg, ((endTimeBuf.writableBytes(" + endTimeBuf.writableBytes()+") >= msg.readableBytes(" +  msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() +" ) : endTimeBuf.writableBytes(" + endTimeBuf.writableBytes() +" )))");
                        endTimeBuf.writeBytes(msg, ((endTimeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : endTimeBuf.writableBytes()));
                        if (endTimeBuf.readableBytes() >= LONG_SIZE) {
                            //logger.info("EndTimeBuf.getLong(bytesReadBuf.readerIndex(" + endTimeBuf.readerIndex() + "))");
                            endTime = endTimeBuf.getLong(endTimeBuf.readerIndex());//Get Size at index = 0;
                            readInEndTime = true;
                            //logger.info("FileSenderControlHandler: The EndTime = : " + endTime);

                            //REPORT THROUGHPUT & REMOVE FILEID AND CHECK THE PIPE LINE VALUE TO SEE IF
                            //MORE FILES CAN BE SENT
                            //Remove the fileId from the list of Expected File Acks
                            //REMOVE THE FILE ID
                            if (myControlChannelObject != null) {
                                //Report the throughput
                                myControlChannelObject.reportThroughput(startTime, endTime, bytesRead);
                                //Remove the File Id
                                myControlChannelObject.removeFileId(fileId);
                                logger.info("FileSenderControlChannelHandler: Received the File Ack AND Removed the Ack/ID from the ID List, FILE ID REMOVED WAS: " + fileId );

                            }

                            //Send new file once received Ack

                            ///////////////////////////////////////////////
                            // THE FILE ACK HAS BEEN READ IN             //
                            // Reset all variables & Clear all ByteBufs  //
                            //////////////////////////////////////////////
                            //Reset MsgAck Variables
                            msgAckTypeSet = false;
                            msgAckTypeBuf.clear();
                            //Reset boolean/flag variables
                            readInFileId = false;
                            readInBytesRead = false;
                            readInStartTime = false;
                            readInEndTime = false;
                            //Reset & clear the ByteBuf
                            fileIdBuf.clear();
                            bytesReadBuf.clear();
                            startTimeBuf.clear();
                            endTimeBuf.clear();
                            //Reset values

                            //Keep sending more files either until files can not be sent due to the
                            //the pipeling values or there are no more files in the file list
                            //Keep sending files until there are no more files in the file request list OR until the pipeline limit has been reached
                            doneReadingFileRequests = false;
                            while ((!doneReadingFileRequests) && (myControlChannelObject.getFileIdListSize() < myPipeLineNum)) { //And while pipeline limit not reached
                                logger.info("FileSenderControlChannel: Channel Read: Received File Ack and (doneReadingFileRequest = false) && (Size of Control Channel Expected File Ack List = " + myControlChannelObject.getFileIdListSize() + "<= Pipeline Num: " + myPipeLineNum);
                                String fileRequest = FileSender.getNextFileRequestFromList(myPathString);
                                logger.info("FileSenderControlChannel: Channel Read: Received File Ack AND READING NEW FILE REQUEST FROM THE FILE REQUEST LIST, READ FILE REQUEST: " + fileRequest);

                                //start reading file request from the queue / File Request List associated with the path
                                //String fileRequest = FileSender.getNextFileRequestFromList(this.myChannelTypeString,myPathString);
                                //String fileRequest = "transfer WS5/home/lrodolph/100MB_File.dat WS7/home/lrodolph/100MB_File_Copy.dat";
                                //String fileRequest = FileSender.getNextFileRequestFromList("");
                                //logger.info("Process Connection Ack. File Request = " + fileRequest);
                                if (fileRequest != null) {
                                    //Increment File ID
                                    myFileId++;

                                    //REGISTER THE FILE ID
                                    if (myControlChannelObject != null) {
                                        //Register the File Id
                                        myControlChannelObject.registerFileId(myFileId);
                                        logger.info("FileSenderControlChannel: Channel Read: Received File Ack AND now REGISTERING NEW FILE REQUEST ID");
                                    }

                                    ///////////////////////////////
                                    //Parse current File Request
                                    //////////////////////////////

                                    // transfer W7/home/lrodolph/1GB_File.dat W11/home/lrodolph/1GB_File_Copy.dat
                                    // TOKENS[0] = transfer
                                    //TOKENS[1] = W7/home/lrodolph/1GB_File.dat
                                    //TOKENS[2] = W11/home/lrodolph/1GB_File_Copy.dat


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

                                    //Have the Control Channel send the FileID
                                    //this.ctx.write(theFileIdBuf);

                                    //////////////////////////////////////////////
                                    //Iterate through the DataChannelObject List
                                    //Start Sending the files
                                    //////////////////////////////////////////

                                    for (FileSender.DataChannelObject aDataChannelObject : myDataChannelObjectList) {
                                        logger.info("Data Channel Object ID: " + aDataChannelObject.getDataChannelId() );
                                        //ChannelHandlerContext theCtx = aDataChannelObject.getDataChannel();
                                        //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

                                        //if fragment size did not divide evenly meaning there were a few left
                                        //over bytes, add it to the last fragment
                                        if ((parallel_counter + 1) >= myParallelNum) {
                                            currentFragmentSize += leftOverBytes;
                                        } //1MB

                                        //START SENDING THE FILE
                                        //FileSenderDataChannelHandler aFileSenderDataChannelHandler = aDataChannelObject.getFileSenderDataChannelHandler();
                                        //aFileSenderDataChannelHandler.startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);
                                        aDataChannelObject.getFileSenderDataChannelHandler().startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);

                                        //Update offset
                                        offSet += currentFragmentSize;
                                        //Update remaining size
                                        remainingFragmentSize -= currentFragmentSize;
                                        //Update the parallel counter
                                        parallel_counter++;

                                    }
                                    fileRequest = null;
                                    //doneReadingFileRequests = true;
                                    //get file request
                                    //on the receiving end add the frame decoder               //Get the number of data channels and divide file among data channels
                                    //send the file                                            //Have all concurrent channels share the path queue, pass it in as a parameter
                                } //End if File Request list = null
                                else {
                                    logger.info("FileSenderControlChannelHandler: Channel Read: READ IN FileRequest = NULL ");
                                    doneReadingFileRequests = true;
                                    break;
                                }
                            } //End While !doneReadingFileRequests
                            //Reset doneReadingFileRequests to false once outside of the while loop
                            doneReadingFileRequests = false;
                            //Check to see if the file Request list size for the path is empty AND
                            //If the expected file id list is empty if so report to the PathDone Structure that it is done the throughput for all the paths and control channel
                            if (!myFileRequestListSet) {
                                myFileRequestList = FileSender.getFileRequestList(myPathString);
                                if (myFileRequestList != null){
                                    myFileRequestListSet = true;
                                }
                            }
                                //myFileReuestList is Set and is NOT NULL
                                if (myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()) {
                                    logger.info("FileSenderControlChannelHandler: ChannelRead: myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()");
                                    //Report to the FileSender
                                    boolean shouldIprintThroughput = FileSender.reportControlChannelDone(myPathString);
                                    if (shouldIprintThroughput) {

                                        //Send the Done Msg Type
                                        int theDoneMsgType = DONE_MSG_TYPE;
                                        int thePrintThroughputMsg = PRINT_THROUGHPUT_MSG_TYPE;
                                        ByteBuf myDoneMsgBuf = Unpooled.copyInt(theDoneMsgType);
                                        ByteBuf myPrintThroughputMsgBuf = Unpooled.copyInt(PRINT_THROUGHPUT_MSG_TYPE);
                                        ctx.write(myDoneMsgBuf);
                                        ctx.write(myPrintThroughputMsgBuf);
                                        ctx.flush();

                                        //Iterate through the myRegisteredCTXHashMap (Control Channel HashMap) printing the throughput for each Path and Control Channel
                                        //It will be printed out as a continous String or one path at a time, how long can a string be
                                        FileSender.printAllThroughputToScreen();

                                    }
                                    //logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: File Request = " + fileRequest);
                                }



                            /*
                            if (myFileRequestList != null){
                                synchronized(myFileRequestList) {
                                    if (myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()) {
                                        //Report to the FileSender
                                        boolean shouldIprintThroughput = FileSender.reportControlChannelDone(myPathString);
                                        if (shouldIprintThroughput) {
                                            //Iterate through the myRegisteredCTXHashMap (Control Channel HashMap) printing the throughput for each Path and Control Channel
                                            //It will be printed out as a continous String or one path at a time, how long can a string be
                                            FileSender.printAllThroughputToScreen();
                                        }
                                        //logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: File Request = " + fileRequest);
                                    }
                                }
                            }
                            */

                        }
                    }else {
                        logger.info("FileSenderControlHandler(Channel ID: " + myControlChannelId + ", thread ID: " + this.threadId + " FILE ACK MSG ERROR: GOT TO THIS STATEMENT UNEXPECTEDLY WITH MORE BYTES TO READ ");
                    }
                }//End if FILE_ACK_MSG_TYPE
            }//End While
        }catch(Exception e){
            System.err.printf("ChannelRead Error Msg: " + e.getMessage());
            e.printStackTrace();

        }
    }//End Read Method

    public void getAndProcessMsgType(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //Read in Msg Type
        //If 1 connection Msg Type
        //tell the main file sender the connection ack for this channel (Data Channel or Control Channel) has been received (shared object)
        msgTypeBuf.writeBytes(msg, ((msgTypeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : msgTypeBuf.writableBytes()));
        logger.info("FileSenderHandler: ProcessMsgType ("+ this.myChannelTypeString +"): msgTypeBuf.readableBytes() =  " + msgTypeBuf.readableBytes());
        if (msgTypeBuf.readableBytes() >= 4) {
            msgType = msgTypeBuf.getInt(msgTypeBuf.readerIndex());//Get Msg Type at index = 0;
            logger.info("FileSenderHandler: ProcessMsgType: ("+this.myChannelTypeString +"): Processing the CONNECTION MSG TYPE (" + msgType +")");
            msgTypeReceived = true;
            processMsgType(msgType, ctx);
            logger.info("FileSenderHandler: (" + this.myChannelTypeString + ") MsgType = " + msgType);
        }
    }//End ProcessMsgType

    public void processMsgType(int aMsgType,ChannelHandlerContext ctx) throws Exception {

        if (myPathString != null) {
            //Mark that this Control Channel and it's associated data channels received the connection Ack Msg
            processConnectionAckMsgType(ctx);
            //Wait until all control channels have received the Control Ack Msg
            //Start Sending Files through the data channel Object list
        } else {
            logger.info("FileSenderHandler: processMsgType: (" + this.myChannelTypeString + ") MY PATH IS NULL");
        }


    }//End ProcessMsgType

    //Process Connection Ack Msg Type for this Control Channel
    //Mark that this Control Channel and it's associated data channels received the connection Ack Msg
    //Start reading files from the list and sending them

    public void processConnectionAckMsgType(ChannelHandlerContext ctx) throws Exception {
        try{
            String theRegisteredChannels = FileSender.registeredChannelsToString();
            logger.info("("+this.myChannelTypeString + ") FileSenderHandler: processConnectionAckMsgType: for Path: "+ myPathString + " The channels who were registered were: " + theRegisteredChannels);
            String myChannelConnectAckString = FileSender.getConnectedChannelAckIDsInStringFormat(myPathString, myControlChannelId);
            //logger.info("("+ this.myChannelTypeString+") FileSenderHandler: processConnectionAckMsgType: This Control Channel (" + myControlChannelId +") Received the Connection Ack for Path: " + myChannelConnectAckString);

            //Get the Control Channel Object for this Control Channel
            myControlChannelObject = FileSender.getControlChannelObject(myPathString, myControlChannelId);

            //Get the list of Data Channels (Channel Handler Contexts - CTX) Associated with this control channel
            myDataChannelObjectList = FileSender.getDataChannelObjectList(myPathString, myControlChannelId);

            //Get the FileRequestList associated with this path
            myFileRequestList = FileSender.getFileRequestList(myPathString);
            if (myFileRequestList != null ) {
                logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: myFileRequestList IS NOT NULL ");
            }else {
                logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: myFileRequestList IS NULL ");
            }


            /*
            START READING FILE REQUESTS AND SENDING THE FILES
            NOTE: IF WE ARE PROCESSING THE CONNECTION ACK MSG THEN
            THIS IS A CONTROL CHANNEL, ONLY CONTROL CHANNELS RECEIVE
            CONNECTION ACK MSG

           */

            //Set up the pipelining data structure
            //Create the Expected File Fragment Ack HashMap to keep track of pipelining

            //myFileAckList = new HashMap<String, ArrayList<ExpectedFileFragmentAck>>();

            //Wait until this control channel receives the connection ack msg
            //This means that this Control Channel and it's associated data channels are all connected
            //For the while loop to work the read method must be called

            //logger.info("FileSenderHandler: Path: " + myPathString + " Control Channel(" + myControlChannelId + ") acknowledged that ALL CONTROL CHANNELS HAVE RECEIVED THE CONNECT ACK MSG");

            doneReadingFileRequests = false;

            //Keep sending files until there are no more files in the file request list OR until the pipeline limit has been reached
            while ((!doneReadingFileRequests) && (myControlChannelObject.getFileIdListSize() < myPipeLineNum)) { //And while pipeline limit not reached
                logger.info("FileSenderControlChannel: ProcessConnectionAck: (doneReadingFileRequest = false) && (Size of Control Channel Expected File Ack List = " + myControlChannelObject.getFileIdListSize() + "<= Pipeline Num: " + myPipeLineNum);
                //start reading file request from the queue / File Request List associated with the path
                String fileRequest = FileSender.getNextFileRequestFromList(myPathString);
                logger.info("FileSenderControlChannel: ProcessConnectionAck: FileRequest =  " + fileRequest);
                /*
                if (myFileRequestList != null){
                    synchronized(myFileRequestList) {
                        if (!myFileRequestList.isEmpty()) {
                            fileRequest = myFileRequestList.remove(0);
                            logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: File Request = " + fileRequest);
                        }
                        else {
                            //File Request List is Empty
                            doneReadingFileRequests = true;
                            break;
                        }
                    }
                }
                */
                //String fileRequest = FileSender.getNextFileRequestFromList(this.myChannelTypeString,myPathString);
                //String fileRequest = "transfer WS5/home/lrodolph/100MB_File.dat WS7/home/lrodolph/100MB_File_Copy.dat";
                //String fileRequest = FileSender.getNextFileRequestFromList("");
                //logger.info("Process Connection Ack. File Request = " + fileRequest);
                if (fileRequest != null) {
                    //Increment File ID
                    myFileId++;

                    //REGISTER THE FILE ID
                    if (myControlChannelObject != null) {
                        //Register the File Id
                        myControlChannelObject.registerFileId(myFileId);
                        logger.info("FileSenderControlChannel: ProcessConnectionAck: registerFileId: " + myFileId );
                    }

                    ///////////////////////////////
                    //Parse current File Request
                    //////////////////////////////

              // transfer W7/home/lrodolph/1GB_File.dat W11/home/lrodolph/1GB_File_Copy.dat
              // TOKENS[0] = transfer
               //TOKENS[1] = W7/home/lrodolph/1GB_File.dat
               //TOKENS[2] = W11/home/lrodolph/1GB_File_Copy.dat


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

                    //Have the Control Channel send the FileID
                    //this.ctx.write(theFileIdBuf);

                    //////////////////////////////////////////////
                    //Iterate through the DataChannelObject List
                    //Start Sending the files
                    //////////////////////////////////////////

                    for (FileSender.DataChannelObject aDataChannelObject : myDataChannelObjectList) {
                        logger.info("Data Channel Object ID: " + aDataChannelObject.getDataChannelId() );
                        //ChannelHandlerContext theCtx = aDataChannelObject.getDataChannel();
                        //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

                        //if fragment size did not divide evenly meaning there were a few left
                        //over bytes, add it to the last fragment
                        if ((parallel_counter + 1) >= myParallelNum) {
                            currentFragmentSize += leftOverBytes;
                        } //1MB

                        //START SENDING THE FILE
                        //FileSenderDataChannelHandler aFileSenderDataChannelHandler = aDataChannelObject.getFileSenderDataChannelHandler();
                        //aFileSenderDataChannelHandler.startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);
                        aDataChannelObject.getFileSenderDataChannelHandler().startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);

                        //Update offset
                        offSet += currentFragmentSize;
                        //Update remaining size
                        remainingFragmentSize -= currentFragmentSize;
                        //Update the parallel counter
                        parallel_counter++;

                    }
                    fileRequest = null;
                    //doneReadingFileRequests = true;
                    //get file request
                    //on the receiving end add the frame decoder               //Get the number of data channels and divide file among data channels
                    //send the file                                            //Have all concurrent channels share the path queue, pass it in as a parameter
                } //End if File Request list = null
                else {
                    logger.info("FileSenderControlChannel: ProcessConnectionAck: FileRequest = Null, DONE READING FILE REQUESTS");
                    doneReadingFileRequests = true;
                    break;
                }
            } //End While !doneReadingFileRequests
            //Reset doneReadingFileRequests to false since outside of the while loop
            doneReadingFileRequests = false;

        }catch(Exception e){
            System.err.println("FileSenderControlChannel: processConnectionAckMsgType Error: " + e.getMessage());
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
