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
    private ByteBuf msgTypeBuf,startTimeBuf, endTimeBuf, bytesReadBuf;
    private boolean msgTypeReceived, finishedProcessingConnectionAckMsgType, allControlChannelsReceivedConnectAckMsg;
    private boolean doneReadingFileRequests;
    private boolean readInStartTime, readInEndTime, readInBytesRead;
    private int msgType;

    //myFileRequestList is volatile so it can read right from memory instead
    //of the local cache, which means multiple threads can read from myFileRequestList
    //without race conditions and get the updated value, however writes are a different story
    private volatile ArrayList<String> myFileRequestList;

    public final int CONNECTION_MSG_TYPE = 1;
    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    public int INT_SIZE = 4;
    public int LONG_SIZE = 8;
    public final int CONNECTION_ACK_MSG_TYPE = 1;
    public int myFileId;
    public String myChannelTypeString;
    private String fileRequest;
    private long offSet;
    private long currentFragmentSize;
    private int channelId;

    private long startTimeRead, endTimeRead, bytesRead;
    private FileSender theFileSender;

    //FileSenderHandler(theFileRequest,theOffset,theCurrentFragmentSize,theDataChannelId));
    public FileSenderHandler(FileSender aFileSender, String theFileRequest, long theOffset, long theCurrentFragmentSize, int theDataChannelId) throws Exception {

        this.fileRequest = theFileRequest;
        this.offSet = theOffset;
        this.currentFragmentSize = theCurrentFragmentSize;
        this.channelId = theDataChannelId;
        this.theFileSender = aFileSender;



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

        //Setting initial values for the throughput info being sent back from the receiver
        readInStartTime = false; readInEndTime = false; readInBytesRead = false;
        startTimeBuf =  Unpooled.buffer(LONG_SIZE);
        endTimeBuf = Unpooled.buffer(LONG_SIZE);
        bytesReadBuf =  Unpooled.buffer(LONG_SIZE);
        startTimeRead = -1; endTimeRead = -1; bytesRead = -1;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      try {

        //String fileRequest = "transfer N0/root/1GB_File.dat N1/tmp/1GB_File_Copy.dat";
        //String fileRequest = "transfer N0/home/lrodolph/10MB_File.dat N1/home/lrodolph/10MB_File_Copy.dat";

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
          ByteBuf theDataChannelId = Unpooled.copyInt( this.channelId);

          //Get the file
          File theFile = new File(theSrcFilePath);
          FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
          //raf = new RandomAccessFile(theSrcFilePath,"r");
          //long length = theFileChannel.size();
          //long offSet = 0;
          //long remainingFragmentSize = length;
          long remainingFragmentSize = currentFragmentSize;
          //long currentFragmentSize = 0;
          //int parallel_counter = 0;
          //long leftOverBytes = 0;

          //get the file fragment size that will be sent through each parallel TCP Stream
         // currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));

          //leftOverBytes = length - (currentFragmentSize * myParallelNum);
          //logger.info("FileSenderHandler:Active: File Length = " + length + ", Current Fragment Size = " + currentFragmentSize + " leftOverBytes = " + leftOverBytes);

          //////////////////////////////////////////////
          //Iterate through the DataChannelObject List
          //Start Sending the files
          //////////////////////////////////////////

          //Send file name length, the filename,  file fragment offset, file fragment length, file fragment ID

          //if fragment size did not divide evenly meaning there were a few left
          //over bytes, add it to the last task object
          //if ((parallel_counter + 1) >= myParallelNum) {
           // currentFragmentSize += leftOverBytes;
          //}                                                                          //1MB
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
          ctx.write(theDataChannelId);
          ctx.flush();

          //Send the File Fragment for this data channel
          ctx.write(new ChunkedNioFile(theFileChannel, offSet, currentFragmentSize, 1024 * 1024 * 1));
          ctx.flush();
          //Update offset
          //offSet += currentFragmentSize;
          //Update remaining size
          //remainingFragmentSize -= currentFragmentSize;
          //Update the parallel counter
          //parallel_counter++;

          //get file request
          //on the receiving end add the frame decoder               //Get the number of data channels and divide file among data channels
          //send the file                                            //Have all concurrent channels share the path queue, pass it in as a parameter
        //End if File Request list = null

      }catch(Exception e){
         System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
         e.printStackTrace();
      }
      }  //End channelActive


    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        try {
            while (msg.readableBytes() >= 1) {
                //Read in start time
                if (!readInStartTime) {
                    logger.info("startTimeBuf.writeBytes(msg, ((offSetBuf.writableBytes(" + startTimeBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : startTimeBuf.writableBytes(" + startTimeBuf.writableBytes() + ")))");
                    startTimeBuf.writeBytes(msg, ((startTimeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : startTimeBuf.writableBytes()));
                    if (startTimeBuf.readableBytes() >= LONG_SIZE) {
                        logger.info("startTimeBuf.getLong(startTimeBuf.readerIndex(" + startTimeBuf.readerIndex() + "))");
                        startTimeRead = startTimeBuf.getLong(startTimeBuf.readerIndex());//Get Size at index = 0;
                        readInStartTime = true;
                        logger.info("FileSenderHandler: StartTime = " + startTimeRead);
                    }


                } else if (!readInEndTime) { ////Read in end time

                    logger.info("endTimeBuf.writeBytes(msg, ((endTimeBuf.writableBytes(" + endTimeBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : endTimeBuf.writableBytes(" + endTimeBuf.writableBytes() + ")))");
                    endTimeBuf.writeBytes(msg, ((endTimeBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : endTimeBuf.writableBytes()));
                    if (endTimeBuf.readableBytes() >= LONG_SIZE) {
                        logger.info("endTimeBuf.getLong(endTimeBuf.readerIndex(" + endTimeBuf.readerIndex() + "))");
                        endTimeRead = endTimeBuf.getLong(endTimeBuf.readerIndex());//Get Size at index = 0;
                        readInEndTime = true;
                        logger.info("FileSenderHandler: StartTime = " + startTimeRead);
                    }

                } else {//Read in the bytes transferred through this channel
                    if (!readInBytesRead) { ////Read in end time

                        logger.info("bytesReadBuf.writeBytes(msg, ((bytesReadBuf.writableBytes(" + bytesReadBuf.writableBytes() + ") >= msg.readableBytes(" + msg.readableBytes() + ")) ? msg.readableBytes(" + msg.readableBytes() + ") : bytesReadBuf.writableBytes(" + bytesReadBuf.writableBytes() + ")))");
                        bytesReadBuf.writeBytes(msg, ((bytesReadBuf.writableBytes() >= msg.readableBytes()) ? msg.readableBytes() : bytesReadBuf.writableBytes()));
                        if (bytesReadBuf.readableBytes() >= LONG_SIZE) {
                            logger.info("bytesReadBuf.getLong(bytesReadBuf.readerIndex(" + bytesReadBuf.readerIndex() + "))");
                            bytesRead = bytesReadBuf.getLong(endTimeBuf.readerIndex());//Get Size at index = 0;
                            readInEndTime = true;
                            bytesRead += endTimeBuf.readableBytes();
                            logger.info("FileSenderHandler: StartTime = " + startTimeRead);
                        }

                    }
                    if (readInBytesRead) {
                        //reportThroughputInfo is a static method
                        theFileSender.reportThroughput(channelId, startTimeRead, endTimeRead, bytesRead);
                        //ctx.channel().close();
                        break;
                    }

                }
            }
        }catch(Exception e){
            System.err.printf("ChannelRead Error Msg: " + e.getMessage());
            e.printStackTrace();

        }
    }//End Read Method

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
