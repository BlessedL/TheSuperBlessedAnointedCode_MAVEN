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
    private String myPathString;
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
    public FileSenderDataChannelHandler(String aPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum) throws Exception {
        //this.myPath = thePath;
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
        myParallelNum = -1;
        myConcurrencyNum = -1;
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
          logger.info("FileSenderDataChannelHandler: ChannelActive: for Path: " + myPathString + " The channels who were registered were: " + theRegisteredChannels);
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
            String myIpAddStringWithOutSrcNode = "192.168.0.1:4959";
            //Get length of path (without source node - the ip Address version) and the actual path without the source node
            byte[] myPathInBytes = myIpAddStringWithOutSrcNode.getBytes();
            int myPathSize = myPathInBytes.length;
            ByteBuf myPathSizeBuf = Unpooled.copyInt(myPathSize);
            //I can also use copiedBuffer(CharSequence string, Charset charset)
            ByteBuf myPathBuf = Unpooled.copiedBuffer(myPathInBytes);

            //Get msg type: Connection Msg Type
            ByteBuf myMsgTypeBuf = Unpooled.copyInt(msgType);

            //String myAliasPathString = myPath.toStringAliasNames();
            String myAliasPathString = "WS5,WS7";
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
            logger.info("SendConnectionMsg: Wrote the Size of the IP Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Sent SIZE OF FILE PATH \n\n");
            this.ctx.write(myPathBuf);
            logger.info("SendConnectionMsg: Wrote the IP Path For Data CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: File & File path \n\n");
            this.ctx.write(myAliasPathSizeBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Alias Path Size \n\n");
            logger.info("SendConnectionMsg: Wrote the Size of the Alias Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myAliasPathBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Alias Path \n\n");
            logger.info("SendConnectionMsg: Wrote the ALIAS Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            logger.info("SendConnectionMsg: Wrote the Alias Path For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myConnectionTypeBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Connection Type \n\n");
            logger.info("SendConnectionMsg: Wrote the CONNECTION TYPE For DATA CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myControlChannelIdBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: CONTROL CHANNEL ID \n\n");
            logger.info("SendConnectionMsg: Wrote the DATA CHANNEL ID  For CONTROL CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myDataChannelIdBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: DATA CHANNEL ID \n\n");

            logger.info("SendConnectionMsg: Wrote the DATA CHANNEL ID  For CONTROL CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myParallelNumBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: PARALLEL NUMBER \n\n");
            logger.info("SendConnectionMsg: Wrote the PARALLEL NUM  For CONTROL CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            this.ctx.write(myConcurrencyNumBuf);
            System.err.printf("\n****** FileSenderDataChannelHandler: sendConnectionMsg: Concurrency Number \n\n");
            logger.info("SendConnectionMsg: Wrote the CONCURRENCY NUM For CONTROL CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
            //Flush out the connection msg to the wire
            this.ctx.flush();
            logger.info("SendConnectionMsg: FLUSHED THE CONNECTION MSG For CONTROL CHANNEL("+myDataChannelId+") for Path: "+myAliasPathString);
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
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE SIZE (# OF CHARACTERS IN THE FILE NAME & PATH) ");
            //does theCtx.write(theDestSizeBuf); increase the writer and reader index of theDestSizeBuf
            this.ctx.write(theDestFileBuf);
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE ACTUAL FILE NAME & PATH");
            this.ctx.write(offSetBuf);
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE OFFSET: " + offSet);
            this.ctx.write(currentFragmentSizeBuf);
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE FRAGMENT SIZE:  " + currentFragmentSize);
            this.ctx.write(theFileIdBuf);
            logger.info("***FileSenderDataChannelHandler: DATA CHANNEL " + myDataChannelId + " SENT THE FILE ID:  " + aFileId );
            this.ctx.flush();
            //aDataChannelObject.getDataChannel().flush();

            //Send the File Fragment for this data channel
            this.ctx.write(new ChunkedNioFile(aFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 1));
            this.ctx.flush();
            logger.info("***FileSenderControlChannelHandler: DATA CHANNEL " + myDataChannelId + " WROTE AND FLUSH THE ACTUAL FRAGMENT ");
            //aDataChannelObject.getDataChannel().flush();
        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//End Start

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

    public void processConnectionAckMsgType(ChannelHandlerContext ctx) throws Exception {
        try{
            String theRegisteredChannels = FileSender.registeredChannelsToString();
            logger.info("("+this.myChannelTypeString + ") FileSenderHandler: processConnectionAckMsgType: for Path: "+ myPathString + " The channels who were registered were: " + theRegisteredChannels);
            FileSender.registerConnectionAck(myPathString, myControlChannelId);
            //Print the data channel ID's associated with this Control Channel that have received the connection Ack Msg
            String myChannelConnectAckString = FileSender.getConnectedChannelAckIDsInStringFormat(myPathString, myControlChannelId);
            logger.info("("+ this.myChannelTypeString+") FileSenderHandler: processConnectionAckMsgType: This Control Channel (" + myControlChannelId +") Received the Connection Ack for Path: " + myChannelConnectAckString);

            //Get the list of Data Channels (Channel Handler Contexts - CTX) Associated with this control channel
            //myDataChannelObjectList = myFileSender.getDataChannelObjectList(myPath.toStringAliasNames(), myControlChannelId);

            //Finished processing the ConnectionAckMsgType
            finishedProcessingConnectionAckMsgType = true;
            //Reset the variable indicating that we receiced the msg type, since we are now waiting on the next msg type
            msgTypeReceived = false;


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

            //Get the list of Data Channels (Channel Handler Contexts - CTX) Associated with this control channel
            myDataChannelObjectList = FileSender.getDataChannelObjectList(myPathString, myControlChannelId);


            /*
            //Wait until ALL Control Channels received the Connection Ack Msg
            while (!allControlChannelsReceivedConnectAckMsg) {
              logger.info("FileSenderHandler: Path: " + myPath.toStringAliasNames() + " Control Channel(" + myControlChannelId + ") is WAITING FOR ALL OTHER CONTROL CHANNELS TO RECEIVE THE CONNECT ACK MSG");
              allControlChannelsReceivedConnectAckMsg = myFileSender.didAllControlChannelsReceiveConnectAckMsg();
            }
            */
            logger.info("FileSenderHandler: Path: " + myPathString + " Control Channel(" + myControlChannelId + ") acknowledged that ALL CONTROL CHANNELS HAVE RECEIVED THE CONNECT ACK MSG");


            while (!doneReadingFileRequests) { //And while pipeline limit not reached
                //start reading file request from the queue / File Request List associated with the path
                String fileRequest = FileSender.getNextFileRequestFromList(this.myChannelTypeString,myPathString);
                if (fileRequest != null) {
                    //Increment File ID
                    myFileId++;

                    ///////////////////////////////
                    //Parse current File Request
                    //////////////////////////////
              /*
               transfer W7/home/lrodolph/1GB_File.dat W11/home/lrodolph/1GB_File_Copy.dat
               TOKENS[0] = transfer
               TOKENS[1] = W7/home/lrodolph/1GB_File.dat
               TOKENS[2] = W11/home/lrodolph/1GB_File_Copy.dat

               */
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
                    RandomAccessFile raf = null;
                    File theFile = new File(theSrcFilePath);
                    FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
                    //raf = new RandomAccessFile(theSrcFilePath,"r");
                    long length = raf.length();
                    long offSet = 0;
                    long remainingFragmentSize = length;
                    long currentFragmentSize = 0;
                    int parallel_counter = 0;
                    long leftOverBytes = 0;

                    //get the file fragment size that will be sent through each parallel TCP Stream
                    currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));

                    leftOverBytes = length - (currentFragmentSize * myParallelNum);

                    //Have the Control Channel send the FileID
                    this.ctx.write(theFileIdBuf);

                    //////////////////////////////////////////////
                    //Iterate through the DataChannelObject List
                    //Start Sending the files
                    //////////////////////////////////////////

                    for (FileSender.DataChannelObject aDataChannelObject : myDataChannelObjectList) {
                        ChannelHandlerContext theCtx = aDataChannelObject.getDataChannel();
                        //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

                        //if fragment size did not divide evenly meaning there were a few left
                        //over bytes, add it to the last task object
                        if ((parallel_counter + 1) >= myParallelNum) {
                            currentFragmentSize += leftOverBytes;
                        }                                                                          //1MB
                        //First send out the File Headers and Flush out the data
                        //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID
                        ByteBuf offSetBuf = Unpooled.copyLong(offSet);
                        ByteBuf currentFragmentSizeBuf = Unpooled.copyLong(currentFragmentSize);
                        //Send the file Headers: FileName Length, the FileName, the Offset, the file fragment length, the file Id
                        theCtx.write(theDestSizeBuf);
                        //does theCtx.write(theDestSizeBuf); increase the writer and reader index of theDestSizeBuf
                        theCtx.write(theDestFileBuf);
                        theCtx.write(offSetBuf);
                        theCtx.write(currentFragmentSizeBuf);
                        theCtx.write(theFileIdBuf);
                        theCtx.flush();

                        //Send the File Fragment for this data channel
                        theCtx.write(new ChunkedNioFile(theFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 1));
                        theCtx.flush();
                        //Update offset
                        offSet += currentFragmentSize;
                        //Update remaining size
                        remainingFragmentSize -= currentFragmentSize;
                        //Update the parallel counter
                        parallel_counter++;

                    }
                    //get file request
                    //on the receiving end add the frame decoder               //Get the number of data channels and divide file among data channels
                    //send the file                                            //Have all concurrent channels share the path queue, pass it in as a parameter
                } //End if File Request list = nule
                else {
                    doneReadingFileRequests = true;
                    break;
                }
            } //End While !doneReadingFileRequests

        }catch(Exception e){
            System.err.println("ServerHandler: processConnectionAckMsgType Error: " + e.getMessage());
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
