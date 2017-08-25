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
package nia.BlessedLavoneCode_ReactorCodeWithOutNetty.filesender;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.nio.*;
//import java.nio.channels.*;
import java.nio.channels.FileChannel;
//NIO Reactor
import java.lang.Exception;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel; // listening for incoming TCP connections
import java.nio.channels.SocketChannel;    // TCP connection
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Control Channel Handler implementation
 */
public class FileSenderControlChannelHandler implements Runnable {

    private Logger logger;
    //private  Path myPath;
    private String pathInIpAddressFormatWithoutSrc,myPathString;
    private int myChannelType, myControlChannelId, myDataChannelId;
    private int myParallelNum, myConcurrencyNum, myPipeLineNum;
    private FileSender myFileSender;
    private ByteBuffer msgTypeBuf, msgAckTypeBuf;
    private ByteBuffer myConnectionMsg;
    private boolean msgTypeReceived, msgAckTypeSet, finishedProcessingConnectionAckMsgType, allControlChannelsReceivedConnectAckMsg;
    private boolean doneReadingFileRequests, processedMsgType;
    private int msgType, msgAckType;
    private  List<FileSender.DataChannelObject> myDataChannelObjectList;
    private FileSender.ControlChannelObject myControlChannelObject;
    //              FileId, Expected File Ack corresponding to the File Id
    //private HashMap<String,ArrayList<ExpectedFileFragmentAck>> myFileAckList;

    //private volatile ArrayList<String> myFileRequestList;
    private ArrayList<String> myFileRequestList;

    //EXPECTED FILE ACK LIST
    //FileId are unique numbers converted to a string
    List<String> myFileIdList; //Note the size of this list is controlled by the pipeline value

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
    private long myBytesRead, myStartTime, myEndTime, myPreviousStartTime, myPreviousEndTime, myTotalBytesRead, myPreviousTotalBytesRead;
    //Flags to read in File Ack
    public boolean readInFileId;
    public boolean readInBytesRead;
    public boolean readInStartTime;
    public boolean readInEndTime;
    public boolean myStartTimeSet, myEndTimeSet, myTotalBytesReadSet;
    //Byte Bufs for File Ack
    ByteBuffer fileIdBuf;
    ByteBuffer bytesReadBuf;
    ByteBuffer startTimeBuf;
    ByteBuffer endTimeBuf;

    ArrayList<String> mainFileRequestList;

    public Selector mySelector;
    public SocketChannel mySocketChannel;

    public boolean registeredChannel;
    public SelectionKey mySelectionKey;


    //FileSenderHandler(theFileRequest,theOffset,theCurrentFragmentSize,theDataChannelId));
    //                                     String aPathInIpAddressFormatWithoutSrc, String anAliasPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, _selector, theSocketChannel,thePathInIpAddressFormatWithoutSrc,theAliasPathString,theChannelType,theControlChannelId,theDataChannelId,theFileSender,theConcurrencyNum,theParallelNum,thePipelineNum,theSocketChannelSocketChannel aSocketChannel
    public FileSenderControlChannelHandler(Selector aSelector, SocketChannel aSocketChannel, String aPathInIpAddressFormatWithoutSrc, String aPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, int aPipelineNum, SelectionKey aSelectionKey) throws Exception {
        this.mySelectionKey = aSelectionKey;
        this.mySelector = aSelector;
        this.mySocketChannel = aSocketChannel;
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

        this.myConnectionMsg = null;

        this.msgAckTypeReceived = false;
        this.msgAckTypeSet = false;

        this.registeredChannel = false;
        myControlChannelObject = null;
        myDataChannelObjectList = null;
        //myFileAckList = new HashMap<Integer,ArrayList<FileSender.ExpectedFileFragmentAck>>();
        //myFileAckList = null;
        logger = Logger.getLogger(FileSenderControlChannelHandler.class.getName());
        this.myFileSender = aFileSender;

        msgTypeBuf = ByteBuffer.allocateDirect(INT_SIZE);
        msgAckTypeBuf = ByteBuffer.allocateDirect(INT_SIZE);
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
        this.threadId = -1;

        //File Ack variables
        fileId = -1;
        bytesRead = -1;
        startTime = -1;
        endTime = -1;
        myBytesRead = -1;
        myStartTime = -1;
        myEndTime = -1;
        myPreviousStartTime = -1;
        myPreviousEndTime = -1;
        myTotalBytesRead = -1;
        myPreviousTotalBytesRead = -1;
        //File Ack Flags
        readInFileId = false;
        readInBytesRead = false;
        readInStartTime = false;
        readInEndTime = false;
        myStartTimeSet = false;
        myEndTimeSet = false;
        myTotalBytesReadSet = false;
        //File Ack Byte Bufs
        fileIdBuf = ByteBuffer.allocateDirect(INT_SIZE);
        bytesReadBuf = ByteBuffer.allocateDirect(LONG_SIZE);
        startTimeBuf = ByteBuffer.allocateDirect(LONG_SIZE);
        endTimeBuf = ByteBuffer.allocateDirect(LONG_SIZE);
        myFileRequestListSet = false;

        //Expected File ID List
        this.myFileIdList = new ArrayList<String>();
        //this.mainFileRequestList = new ArrayList<String>();
        //this.mainFileRequestList = aMainFileRequestList;
        this.processedMsgType = false;

        // Register _socketChannel with _selector and let the _socketChannel tell the _selector it wants to write a message
        // Callback: Handler, selected when the connection is established and ready for READ
        //mySelectionKey = mySocketChannel.register(mySelector, SelectionKey.OP_WRITE);
        //mySelectionKey
        //mySelectionKey.attach(this); //When attaching this Handler, are the states updated or the same
        //LAR: Why are we waking up Select again?
        //LAR: What thread is this waking up the selector?
        //LAR: How do we know the Selector is sleep?
        //mySelector.wakeup(); // let blocking select() return

    }


      public String getNextFileRequest(){
        try{
            String aFileRequest = null;
            if (myFileRequestList != null ){
                if (!myFileRequestList.isEmpty()){
                    aFileRequest = myFileRequestList.remove(0);
                }
            }
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
      }


    public void sendConnectionMsg(){
        try {

            this.myConnectionMsg = ByteBuffer.allocateDirect(28);

            int msgType = CONNECTION_MSG_TYPE;
            //Get the Path in IP Address Format without the source node
            //String myIpAddStringWithOutSrcNode = myPath.toStringWithoutSourceNodeAndDestFileName();
            //String myIpAddStringWithOutSrcNode = "192.168.0.1:4959";
            //String myIpAddStringWithOutSrcNode = "192.168.0.1:4959,192.168.1.2:4959";



            //I can also use copiedBuffer(CharSequence string, Charset charset)

            String myIpAddStringWithOutSrcNode = this.pathInIpAddressFormatWithoutSrc;
            //Get length of path (without source node - the ip Address version) and the actual path without the source node
            byte[] myPathInBytes = myIpAddStringWithOutSrcNode.getBytes();
            int myPathSize = myPathInBytes.length;

            String myAliasPathString = this.myPathString;
            //Get length of Alias path and then get the Alias Path
            byte[] myAliasPathInBytes = myAliasPathString.getBytes();
            int myAliasPathSize = myAliasPathInBytes.length;

            myConnectionMsg.putInt(msgType);
            myConnectionMsg.putInt(myChannelType);
            myConnectionMsg.putInt(myControlChannelId);
            myConnectionMsg.putInt(myDataChannelId);
            myConnectionMsg.putInt(myParallelNum);
            myConnectionMsg.putInt(myConcurrencyNum);
            myConnectionMsg.putInt(myPathSize);
            //ByteBuffer myPathBuffer = ByteBuffer.wrap(myPathInBytes);
            ByteBuffer myPathBuffer = ByteBuffer.allocate(myPathSize);
            myPathBuffer.put(myPathInBytes);
            ByteBuffer myAliasPathSizeBuffer = ByteBuffer.allocateDirect(4).putInt(myAliasPathSize);
            //ByteBuffer myAliasPathBuffer = ByteBuffer.wrap(myAliasPathInBytes);
            ByteBuffer myAliasPathBuffer = ByteBuffer.allocate(myAliasPathSize);
            myAliasPathBuffer.put(myAliasPathInBytes);
            //Flip the Connection Msg and ByteBuffers
            myConnectionMsg.flip();
            myPathBuffer.flip();
            myAliasPathSizeBuffer.flip();
            myAliasPathBuffer.flip();

            //Send the Connection Msg
            while (myConnectionMsg.hasRemaining()){
                mySocketChannel.write(myConnectionMsg);
            }

            //Send the Path
            while (myPathBuffer.hasRemaining()){
                mySocketChannel.write(myPathBuffer);
            }

            //Send the AliasPathSize
            while (myAliasPathSizeBuffer.hasRemaining()){
                mySocketChannel.write(myAliasPathSizeBuffer);
            }

            //Send the AliasPath
            while (myAliasPathBuffer.hasRemaining()){
                mySocketChannel.write(myAliasPathBuffer);
            }
            
            mySelectionKey.interestOps(SelectionKey.OP_READ);
            mySelectionKey.selector().wakeup();

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




    //Process Connection Ack Msg Type for this Control Channel
    //Mark that this Control Channel and it's associated data channels received the connection Ack Msg
    //Start reading files from the list and sending them

    public void processConnectionAckMsgType(ChannelHandlerContext ctx) throws Exception {
        try{
            String theRegisteredChannels = FileSender.registeredChannelsToString();
            //logger.info("("+this.myChannelTypeString + ") FileSenderHandler: processConnectionAckMsgType: for Path: "+ myPathString + " The channels who were registered were: " + theRegisteredChannels);
            String myChannelConnectAckString = FileSender.getConnectedChannelAckIDsInStringFormat(myPathString, myControlChannelId);
            //logger.info("("+ this.myChannelTypeString+") FileSenderHandler: processConnectionAckMsgType: This Control Channel (" + myControlChannelId +") Received the Connection Ack for Path: " + myChannelConnectAckString);

            //Get the Control Channel Object for this Control Channel
            myControlChannelObject = FileSender.getControlChannelObject(myPathString, myControlChannelId);

            //Get the list of Data Channels (Channel Handler Contexts - CTX) Associated with this control channel
            myDataChannelObjectList = FileSender.getDataChannelObjectList(myPathString, myControlChannelId);

            //Get the FileRequestList associated with this path
            //myFileRequestList = FileSender.getFileRequestList(myPathString);
            /*
            if (myFileRequestList != null ) {
                logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: myFileRequestList IS NOT NULL ");
            }else {
                logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: myFileRequestList IS NULL ");
            }
            */


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
                //logger.info("FileSenderControlChannel: ProcessConnectionAck: (doneReadingFileRequest = false) && (Size of Control Channel Expected File Ack List = " + myControlChannelObject.getFileIdListSize() + "<= Pipeline Num: " + myPipeLineNum);
                //start reading file request from the queue / File Request List associated with the path
                //String fileRequest = FileSender.getNextFileRequestFromList(myPathString);

                String fileRequest = FileSender.getNextFileRequestFromList();
                //logger.info("FileSenderControlChannel: ProcessConnectionAck: FileRequest =  " + fileRequest);
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
                        //logger.info("FileSenderControlChannel: ProcessConnectionAck: registerFileId: " + myFileId );
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
                        //logger.info("Data Channel Object ID: " + aDataChannelObject.getDataChannelId() );
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


            //See if I need to report the throughput if this control channel is not reading in a file request
            if (!myFileRequestListSet) {
                myFileRequestList = FileSender.getFileRequestList(myPathString);
                if (myFileRequestList != null){
                    myFileRequestListSet = true;
                }
            }


            //myFileReuestList is Set and is NOT NULL
            if (myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()) {
            //if (FileSender.isMainFileRequestListEmpty() && myControlChannelObject.isFileIdListEmpty()) {
                //logger.info("FileSenderControlChannelHandler: ChannelRead: myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()");
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
                    FileSender.printAllThreadIds();
                    FileSender.printAllThroughputToScreen();

                }
                //logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: File Request = " + fileRequest);
            }


        }catch(Exception e){
            System.err.println("FileSenderControlChannel: processConnectionAckMsgType Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //startSendingFiles(aConcurrencyControlChannelAndFileRequest.getMySrcFilePath(), aConcurrencyControlChannelAndFileRequest.getMyDestFilePath(), aConcurrencyControlChannelAndFileRequest.getMyFileLength(), aConcurrencyControlChannelAndFileRequest.getCurrentFragmentSize(),aConcurrencyControlChannelAndFileRequest.getLeftOverBytes(), aConcurrencyControlChannelObject.getConcurrencyControlChannelId()  );
    /*
       Pipeling occurs after the fisrt acknowledgement - This method sends a single file to the receiver through it's
       data channels.  After this control channel receives an ack then it sends more files
     */
    public void startSendingFiles(String aSrcFilePath, String aDestFilePath, long aFileLength, long theCurrentFragmentSize, long theLeftOverBytes, int theFileId) throws Exception {
        try{
            logger.info("ENTERED THE startSendingFiles Method");

            //Src File Path = /home/lrodolph/1GB_File.dat W11/home/lrodolph/1GB_File_Copy.dat
            String theSrcFilePath = aSrcFilePath;

            //Dest File Path = /home/lrodolph/1GB_File_Copy.dat
            String theDestFilePath = aDestFilePath;
            logger.info("Destination File Path: " + theDestFilePath);

            //File Length
            long length = aFileLength;

            //Set Remaining Size to the File Length
            long remainingFragmentSize = length;

            //Current Fragment Size
            long currentFragmentSize = theCurrentFragmentSize;

            //Left OverBytes
            long leftOverBytes = theLeftOverBytes;

            //FileId
            myFileId = theFileId;

            //Get the Control Channel Object for this Control Channel
            myControlChannelObject = FileSender.getControlChannelObject(myPathString, myControlChannelId);

            //
            if (myControlChannelObject == null){
                logger.info("Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " == NULL");
            } else{
                logger.info("Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS NOT NULL");
            }

            //Register the file ID
            myControlChannelObject.registerFileId(myFileId);

            //Get the list of Data Channels (Channel Handler Contexts - CTX) Associated with this control channel
            //myDataChannelObjectList = FileSender.getDataChannelObjectList(myPathString, myControlChannelId);
            myDataChannelObjectList = myControlChannelObject.getDataChannelObjectList();

            if (myDataChannelObjectList == null){
                logger.info("Data Channel Object List for Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " == NULL");
            } else{
                logger.info("Data Channel Object List Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS NOT NULL");
            }

            if (myDataChannelObjectList.isEmpty()){
                logger.info("Data Channel Object List for Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS EMPTY");
            } else {
                logger.info("Data Channel Object List for Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS NOT EMPTY");
            }


            int parallel_counter = 0;
            long offSet = 0;

            //////////////////////////////////////////////
            //Iterate through the DataChannelObject List
            //Start Sending the files
            //////////////////////////////////////////
            for (FileSender.DataChannelObject aDataChannelObject : myDataChannelObjectList) {
                //logger.info("Data Channel Object ID: " + aDataChannelObject.getDataChannelId() );
                //ChannelHandlerContext theCtx = aDataChannelObject.getDataChannel();
                //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

                //if fragment size did not divide evenly meaning there were a few left
                //over bytes, add it to the last fragment
                if ((parallel_counter + 1) >= myParallelNum) {
                    currentFragmentSize += leftOverBytes;
                }

                if (aDataChannelObject == null){
                    logger.info("Data Channel Object in Data Channel Object List for Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS NULL");
                }else{
                    logger.info("Data Channel Object in Data Channel Object List for Control Channel Object ID: " + myControlChannelId + " for Path: " + myPathString + " IS NOT NULL");
                }

                //START SENDING THE FILE                                                                    theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId
                FileSenderDataChannelHandler aDataChannelHandler = aDataChannelObject.getFileSenderDataChannelHandler();
                if (aDataChannelHandler == null ){
                    logger.info("Data Channel Handler in Data Channel Object for Control Channel ID: " + myControlChannelId + " for Path: " + myPathString + " IS NULL");
                }else {
                    logger.info("Data Channel Handler in Data Channel Object for Control Channel ID: " + myControlChannelId + " for Path: " + myPathString + " IS NOT NULL");
                }
                aDataChannelObject.getFileSenderDataChannelHandler().startSendingFile(theSrcFilePath, theDestFilePath, offSet, currentFragmentSize, myFileId);
                //aDataChannelObject.getFileSenderDataChannelHandler().handleSendingFiles();

                //LAR Aug. 20

                //Update offset
                offSet += currentFragmentSize;
                //Update remaining size
                remainingFragmentSize -= currentFragmentSize;
                //Update the parallel counter
                parallel_counter++;


            }

            boolean doneReadingFileRequests = false;
            while ((!doneReadingFileRequests) && (myControlChannelObject.getFileIdListSize() < myPipeLineNum)) { //And while pipeline limit not reached
                //logger.info("FileSenderControlChannel: Channel Read: Received File Ack and (doneReadingFileRequest = false) && (Size of Control Channel Expected File Ack List = " + myControlChannelObject.getFileIdListSize() + "<= Pipeline Num: " + myPipeLineNum);
                String fileRequest = FileSender.getNextFileRequestFromList(myPathString);
                //String fileRequest = FileSender.getNextFileRequestFromList();
                //logger.info("FileSenderControlChannel: Channel Read: Received File Ack AND READING NEW FILE REQUEST FROM THE FILE REQUEST LIST, READ FILE REQUEST: " + fileRequest);

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
                        //logger.info("FileSenderControlChannel: Channel Read: Received File Ack AND now REGISTERING NEW FILE REQUEST ID");
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
                    theSrcFilePath = tokens[1].substring(begginingOfFilePath, tokens[1].length());
                    //theSrcFilePath
                    //////////////////////////////////////////////////////////////////////
                    //Destination File length, File Name with direcotry path, and FileId
                    // is the same for all Data Channels
                    /////////////////////////////////////////////////////////////////////
                    //Parse out Destination File Path
                    int begginingOfFilePathForDest = tokens[2].indexOf('/');
                    String aliasDest = tokens[2].substring(0, begginingOfFilePathForDest); //-->C
                    theDestFilePath = tokens[2].substring(begginingOfFilePathForDest, tokens[2].length());

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
                    length = theFileChannel.size();
                    offSet = 0;
                    remainingFragmentSize = length;
                    currentFragmentSize = 0;
                    parallel_counter = 0;
                    leftOverBytes = 0;

                    //get the file fragment size that will be sent through each parallel TCP Stream
                    currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));

                    leftOverBytes = length - (currentFragmentSize * myParallelNum);


                    //////////////////////////////////////////////
                    //Iterate through the DataChannelObject List
                    //Start Sending the files
                    //////////////////////////////////////////
                    logger.info("Control Channel ID: " + myControlChannelId + " HAS " + myDataChannelObjectList.size() + " DATA CHANNEL OBJECTS in it's LIST");
                    for (FileSender.DataChannelObject aDataChannelObject : myDataChannelObjectList) {
                        //logger.info("Data Channel Object ID: " + aDataChannelObject.getDataChannelId() );
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
            }//End While

            if (!myFileRequestListSet) {
                myFileRequestList = FileSender.getFileRequestList(myPathString);
                if (myFileRequestList != null){
                    myFileRequestListSet = true;
                }
            }

            //if (FileSender.isMainFileRequestListEmpty() && myControlChannelObject.isFileIdListEmpty()) {
            if (myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()) {
                //logger.info("FileSenderControlChannelHandler: ChannelRead: myFileRequestList.isEmpty() && myControlChannelObject.isFileIdListEmpty()");
                //Report to the FileSender
                boolean shouldIprintThroughput = FileSender.reportControlChannelDone(myPathString);
                if (shouldIprintThroughput) {
                    logger.info("FileSenderControlChannelHandler(" + threadId +"): StartSendingFiles: FileAck: Path: " + myPathString + ", Control Channel ID: " + myControlChannelId + "SHOULD I PRINT THROUGHPUT = TRUE,  FILE REQUEST LIST IS EMPTY AND FILE ID LIST IS EMPTY, ALL FILES WERE ACKNOWLEDGED");
                    //Send the Done Msg Type
                    int theDoneMsgType = DONE_MSG_TYPE;
                    int thePrintThroughputMsg = PRINT_THROUGHPUT_MSG_TYPE;
                    ByteBuffer myDoneMsgBuf = ByteBuffer.allocateDirect(4).putInt(theDoneMsgType);
                    myDoneMsgBuf.flip();
                    ByteBuffer myPrintThroughputMsgBuf = ByteBuffer.allocateDirect(4).putInt(PRINT_THROUGHPUT_MSG_TYPE);
                    myPrintThroughputMsgBuf.flip();
                    int numBytesWrote = 0;
                    while (myDoneMsgBuf.hasRemaining()){
                        numBytesWrote = mySocketChannel.write(myDoneMsgBuf);
                    }
                    while (myPrintThroughputMsgBuf.hasRemaining()){
                        numBytesWrote = mySocketChannel.write(myPrintThroughputMsgBuf);
                    }

                    //Iterate through the myRegisteredCTXHashMap (Control Channel HashMap) printing the throughput for each Path and Control Channel
                    //It will be printed out as a continous String or one path at a time, how long can a string be
                    //FileSender.printAllThreadIds();
                    FileSender.printAllThroughputToScreen();

                } else {
                    logger.info("FileSenderControlChannelHandler(" + threadId + "): StartSendingFiles: FileAck: Path: " + myPathString + ", Control Channel ID: " + myControlChannelId + "SHOULD I PRINT THROUGHPUT = FALSE,  FILE REQUEST LIST IS EMPTY AND FILE ID LIST IS EMPTY, ALL FILES WERE ACKNOWLEDGED");
                    FileSender.printPathDoneObject(myPathString,myControlChannelId,threadId);
                }
                //logger.info("FileSenderControlChannelHandler(" + threadId + "): processConnectionAckMsgType: File Request = " + fileRequest);
            }


        }catch(Exception e){
            logger.info("startSendingFiles Error: " + e.getMessage() + " %n");
            //System.err.println("FileSenderControlChannel: startSendingFiles Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reportThroughput(long aStartTime, long anEndTime, long theBytesRead){
        //Set Start Time
        if (!myStartTimeSet){
            myStartTime = aStartTime;
            myPreviousStartTime = myStartTime;
            myStartTimeSet = true;
        }else {
            myPreviousStartTime = myStartTime;
            //Get the Min Start Time, don't really need to do this
            //since all files are sent sequentially and acks are sent sequentially
            //through the control channel
            myStartTime = (aStartTime < myStartTime) ? aStartTime : myStartTime;
        }
        //Set End Time
        if(!myEndTimeSet) {
            myEndTime = anEndTime;
            myPreviousEndTime = myEndTime;
            myEndTimeSet = true;
        }else {
            myPreviousEndTime = myEndTime;
            myEndTime = (anEndTime > myEndTime)? anEndTime : myEndTime;
        }
        //Set Total Bytes Read
        if (!myTotalBytesReadSet){
            myTotalBytesRead = theBytesRead;
            myPreviousTotalBytesRead = myTotalBytesRead;
            myTotalBytesReadSet = true;
        }else {
            myPreviousTotalBytesRead = myTotalBytesRead;
            myTotalBytesRead = ( theBytesRead > 0) ? myTotalBytesRead+theBytesRead : myTotalBytesRead;
        }

    }

    //Register the FileId - this is necessary to keep track of pipelining
    public void registerFileId(int aFileId){
        //Add the file ID to the File Id list
        myFileIdList.add(String.valueOf(aFileId));
    }

    //De-register / remove the FileId - necessary
    public boolean removeFileId(int aFileId){
        //Note each FileId is a unique integer
        boolean removedSuccessfully = myFileIdList.remove(String.valueOf(aFileId));
        return removedSuccessfully;
    }

    public boolean isFileIdListEmpty(){
        boolean isFileIdEmpty = myFileIdList.isEmpty();
        return isFileIdEmpty;
    }

    public int getFileIdListSize(){
        return myFileIdList.size();
    }

    synchronized void handleRead() throws IOException {
        try {
            if (!msgAckTypeSet) {
                while (msgAckTypeBuf.hasRemaining()) {
                    mySocketChannel.read(msgAckTypeBuf);
                }
                if (msgAckTypeBuf.remaining() == 0) {
                    msgAckTypeBuf.flip();
                    msgAckType = msgAckTypeBuf.getInt();
                    msgAckTypeSet = true;
                    msgAckTypeBuf.clear();
                }
            }//End msgAckTypeSet
            if (msgAckTypeSet && (!processedMsgType)) {
                //Check if the msgAckType = CONNECTION_ACK_MSG_TYPE
                if (msgAckType == CONNECTION_ACK_MSG_TYPE) {
                    FileSender.registerConnectionAck(myPathString, myControlChannelId);
                    //Register Connection Ack with the PathDone Object
                    FileSender.registerConnectionAckWithPathDoneObject(myPathString);
                    //Finished processing the ConnectionAckMsgType
                    finishedProcessingConnectionAckMsgType = true;
                    //Reset the variable indicating that we receiced the msg type, since we are now waiting on the next msg type
                    msgAckTypeSet = false;
                    msgAckTypeBuf.clear();
                    //will start the sending process (sending files through all of the control channels)
                    logger.info("FileSenderControlHandler: ControlChannel ID: " + myControlChannelId + " RECEIVED CONNECTION MSG ACK ");
                    if (FileSender.didAllControlChannelsReceiveConnectionAck()) {
                        logger.info("FileSenderControlHandler: CONTROL CHANNEL: RECEIVED CONNECTION MSG ");
                        //Start Sending Files through the Control Channels

                        //////////////////////////////////////////////////////////
                        //----COMMENTED OUT JUNE 29, 2017
                        //--Create the ConcurrencyChannel Object List
                        /*
                        FileSender.createTheConcurrencyControlObjectList();
                        //--Start Sending Files through the ConcurrencyChannels
                        FileSender.startSendingFilesThroughTheConcurrentChannels();
                        */

                        //Start Sending hardcoded file through this control channel handler
                        //FileSender.sendHardCodedFile(this control channel id);
                        String theTempSrcFilePath = "/home/lrodolph/5GB_DIR/5GB_File1.dat";
                        String theTempDestFilePath = "/home/lrodolph/5GB_DIR/5GB_File1_Copy.dat";
                        //Get the File
                        File theTempFile = new File(theTempSrcFilePath);
                        FileChannel theTempFileChannel = new RandomAccessFile(theTempFile, "r").getChannel();
                        //Get the File Length
                        long myTempFileLength = theTempFileChannel.size();
                        long theTempFragmentSize = myTempFileLength;
                        long theTempLeftOverBytes = 0;
                        int theTempFileId = 8;

                        //this.startSendingFiles(String aSrcFilePath, String aDestFilePath, long aFileLength, long theCurrentFragmentSize, long theLeftOverBytes, int theFileId);
                        this.startSendingFiles(theTempSrcFilePath, theTempDestFilePath , myTempFileLength, theTempFragmentSize, theTempLeftOverBytes, theTempFileId);

                        ///////////////////////////////////////////////////////////////
                    }
                    processedMsgType = true;
                    //Check to see if ALL Paths's Control Channels received the Connection Ack Msg
                } else if (msgAckType == FILE_ACK_MSG_TYPE) {
                    //Read in the FileID
                    if (!readInFileId) {
                        while (fileIdBuf.hasRemaining()) {
                            mySocketChannel.read(fileIdBuf);
                        }
                        if (fileIdBuf.remaining() == 0) {
                            fileIdBuf.flip();
                            fileId = fileIdBuf.getInt();
                            readInFileId = true;
                        }
                    }
                    if (readInFileId && (!readInBytesRead)) {
                        while (bytesReadBuf.hasRemaining()) {
                            mySocketChannel.read(bytesReadBuf);
                        }
                        if (bytesReadBuf.remaining() == 0) {
                            bytesReadBuf.flip();
                            bytesRead = bytesReadBuf.getLong();
                            readInBytesRead = true;
                        }
                    }
                    if (readInFileId && (!readInStartTime)) {
                        while (startTimeBuf.hasRemaining()) {
                            mySocketChannel.read(startTimeBuf);
                        }
                        if (startTimeBuf.remaining() == 0) {
                            startTimeBuf.flip();
                            startTime = startTimeBuf.getLong();
                            readInStartTime = true;
                            myStartTimeSet = true;
                        }
                    }
                    if (readInFileId && (!readInEndTime)) {
                        while (endTimeBuf.hasRemaining()) {
                            mySocketChannel.read(endTimeBuf);
                        }
                        if (endTimeBuf.remaining() == 0) {
                            endTimeBuf.flip();
                            endTime = endTimeBuf.getLong();
                            readInEndTime = true;
                            //REPORT THROUGHPUT & REMOVE FILEID AND CHECK THE PIPE LINE VALUE TO SEE IF
                            this.reportThroughput(startTime, endTime, bytesRead);
                            //Remove the fileId from the list of Expected File Acks
                            //REMOVE THE FILE ID
                            this.removeFileId(fileId);
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
                        }
                    }//End if read in End Time
                }//End File_Ack
            }//End (msgAckTypeSet && (!processedMsgType)){
        }catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
    void handleWrite() throws IOException {
        try {

        }catch(Exception e){
            System.err.printf("FileSenderHandler:Write Method Error: %s %n", e.getMessage());
            e.printStackTrace();
        }

    }

    public void registerTheChannel( )  {
        try {
            this.threadId = Thread.currentThread().getId();

            //registerChannelCtx          (String , FileSenderControlChannelHandler ,  int ,      int ,                    int ,     FileSenderDataChannelHandler , long ,      int ){
            FileSender.registerChannelCtx(myPathString,      this,              myChannelType,  myControlChannelId, myDataChannelId,        null,                 threadId, myParallelNum );

        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }  //End channelActive

    public void run() {
        try {

            if (mySelectionKey.isReadable()) {
                handleRead();
            }
            else if (mySelectionKey.isWritable()) {
                if (!registeredChannel){
                    this.registerTheChannel();
                    this.registeredChannel = true;
                    this.sendConnectionMsg();

                }
                //handleWrite();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


}
