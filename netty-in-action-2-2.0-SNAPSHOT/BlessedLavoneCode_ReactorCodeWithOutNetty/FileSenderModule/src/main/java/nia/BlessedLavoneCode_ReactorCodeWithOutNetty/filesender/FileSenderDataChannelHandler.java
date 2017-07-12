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


import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.nio.*;
//import java.nio.channels.*;
import java.nio.channels.FileChannel;


/**
 * Control Channel Handler implementation
 */
public class FileSenderDataChannelHandler implements Runnable {

    private Logger logger;
    //private  Path myPath;
    private String myPathString, pathInIpAddressFormatWithoutSrc;
    private int myChannelType, myControlChannelId, myDataChannelId;
    private int myParallelNum, myConcurrencyNum, myPipelineNum;
    private FileSender myFileSender;
    private List<FileSender.DataChannelObject> myDataChannelObjectList;
    private boolean transferInProgress;
    private File theFile;
    private FileChannel theFileChannel;
    private long remainingFragmentSize, currentFragmentSize, length, currentOffset;
    private long amountToTransfer;
    private long theBytesTransferredToSocket;

    public final int CONNECTION_MSG_TYPE = 1;
    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    public final long DATA_BUFFER_SIZE = 100*1024*1024; //100MB = 100*1024*1024

    public final int INT_SIZE = 4;
    public final int LONG_SIZE = 8;

    public final int CONNECTION_ACK_MSG_TYPE = 1;
    public final int FILE_MSG_TYPE = 2;


    public int myFileId;
    public String myChannelTypeString;
    public long threadId;
    private ByteBuffer myConnectionMsg;
    private ByteBuffer fileMsgHeader_part1;
    private ByteBuffer fileMsgHeader_part2;
    private byte[] theDestFilePathInBytes;


    private boolean registeredChannel;
    public Selector mySelector;
    public SocketChannel mySocketChannel;
    public SelectionKey mySelectionKey;
    private ArrayList<FileSenderDataChannelHandler.FileFragmentObject> fileQueue;// = new ArrayList<String>();
    private int writeCallNumber;

    //FileSenderHandler(theFileRequest,theOffset,theCurrentFragmentSize,theDataChannelId));
    public FileSenderDataChannelHandler(Selector aSelector, SocketChannel aSocketChannel, String aPathInIpAddressFormatWithoutSrc, String aPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, int aPipelineNum,SelectionKey aSelectionKey) throws Exception {
        this.mySelectionKey = aSelectionKey;
        this.mySelector = aSelector;
        this.mySocketChannel = aSocketChannel;
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
        this.myPipelineNum = aPipelineNum;
        this.theDestFilePathInBytes = null;

        this.registeredChannel = false;
        this.amountToTransfer = -1;
        this.theBytesTransferredToSocket = -1;

        myDataChannelObjectList = null;
        transferInProgress = false;
        //myFileAckList = new HashMap<Integer,ArrayList<FileSender.ExpectedFileFragmentAck>>();
        //myFileAckList = null;
        logger = Logger.getLogger(FileSenderDataChannelHandler.class.getName());
        //myParallelNum = -1;
        //myConcurrencyNum = -1;
        this.myFileSender = aFileSender;

        myFileId = 0;
        this.threadId = -1;

        theFile = null;
        theFileChannel = null;
        remainingFragmentSize = -1;
        currentFragmentSize = -1;
        length = -1;
        currentOffset = -1;

        myConnectionMsg = null;
        fileMsgHeader_part1 = ByteBuffer.allocateDirect(INT_SIZE + INT_SIZE);
        fileMsgHeader_part2 = ByteBuffer.allocateDirect(LONG_SIZE + LONG_SIZE + INT_SIZE);

        fileQueue = new ArrayList<FileSenderDataChannelHandler.FileFragmentObject>();
        this.writeCallNumber = 0;

        // Register _socketChannel with _selector and let the _socketChannel tell the _selector it wants to write a message
        // Callback: Handler, selected when the connection is established and ready for READ
        //mySelectionKey = mySocketChannel.register(mySelector, SelectionKey.OP_WRITE);
        //mySelectionKey.interestOps(SelectionKey.OP_WRITE);
        //mySelectionKey.attach(this); //When attaching this Handler, are the states updated or the same
        //LAR: Why are we waking up Select again?
        //LAR: What thread is this waking up the selector?
        //LAR: How do we know the Selector is sleep?
        mySelector.wakeup(); // let blocking select() return
    }

    public class FileFragmentObject{

        String theSrcFilePath, theDestFilePath;
        long theOffset, theCurrentFragmentSize;
        int theFileId;

        public FileFragmentObject(String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId){
            theSrcFilePath =  aSrcFilePath;
            theDestFilePath = aDestFilePath;
            theOffset = offSet;
            theCurrentFragmentSize = currentFragmentSize;
            theFileId = aFileId;
        }


        public String getTheSrcFilePath() {
            return theSrcFilePath;
        }

        public void setTheSrcFilePath(String aSrcFilePath){
            theSrcFilePath = aSrcFilePath;
        }

        public String getTheDestFilePath() {
            return theDestFilePath;
        }

        public void setTheDestFilePath(String aDestFilePath){
            theDestFilePath = aDestFilePath;
        }

        public long getTheOffSet() {
            return theOffset;
        }

        public void setTheOffSet(long anOffset){
            theOffset = anOffset;
        }

        public long getTheCurrentFragmentSize() {
            return theCurrentFragmentSize;
        }

        public void setTheCurrentFragmentSize(long aCurrentFragmentSize){
            theCurrentFragmentSize = aCurrentFragmentSize;
        }

        public int getTheFileId(){
            return theFileId;
        }

        public void setTheFileId(int aFileId){
            theFileId = aFileId;
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

    public synchronized boolean isChannelRegistered() {
        try {
            return this.registeredChannel;
        } catch (Exception e) {
            System.err.printf("FileSender:isFileTransferQueueEmpty: Error: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void setIsChannelRegistered(boolean aVal) {
        try {
            this.registeredChannel = aVal;
        } catch (Exception e) {
            System.err.printf("FileSender:isFileTransferQueueEmpty: Error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }


    public synchronized boolean isTransferInProgress() {
        try {
            return transferInProgress;
        } catch (Exception e) {
            System.err.printf("%n FileSenderDataChannelHandler:getTransferInProgress: Error: %s %n",e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void setTransferInProgress(boolean aVal) {
        try {
            transferInProgress = aVal;
        } catch (Exception e) {
            System.err.printf("%n FileSenderDataChannelHandler:setTransferInProgress: Error: %s %n",e.getMessage());
            e.printStackTrace();
        }
    }

    /*
      This is the method that the control channel calls when it initially wants to send a file.
      If a file transfer is already in progress or if there are files in the file queue
      then this method will add a fileFragmentObject to the FileFragmentObjectQueue to be processed
      later.  All file fragments are processed in the order they arrived.

     */

    public synchronized boolean isFileQueueEmpty() {
        try {
            return fileQueue.isEmpty();
        } catch (Exception e) {
            System.err.printf("%n FileSenderDataChannelHandler:setTransferInProgress: Error: %s %n",e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean addFileFragmentToFileQueue(String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId) {
        try {
           return this.fileQueue.add(this.new FileFragmentObject(aSrcFilePath, aDestFilePath, offSet, currentFragmentSize, aFileId));
        } catch (Exception e) {
            System.err.printf("%n FileSenderDataChannelHandler:setTransferInProgress: Error: %s %n",e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized FileSenderDataChannelHandler.FileFragmentObject removeFileFragmentFromFileQueue() {
        try {
            FileSenderDataChannelHandler.FileFragmentObject aFileFragmentObject = null;
            if (!this.isFileQueueEmpty()) {
                aFileFragmentObject = this.fileQueue.remove(0);
            }
            return aFileFragmentObject;
        } catch (Exception e) {
            System.err.printf("%n FileSenderDataChannelHandler:setTransferInProgress: Error: %s %n",e.getMessage());
            e.printStackTrace();
            return null;
        }
    }







    public synchronized void startSendingFile(String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId){
        try {
              logger.info("START SENDING FILE PARAMETERS: Source File Path = " + aSrcFilePath + ", Destination File Path: " + aDestFilePath + ", Offset = " + offSet + " Fragment Size: " + currentFragmentSize + " FileId: " + myFileId);
              if ((!this.isTransferInProgress()) && (this.isFileQueueEmpty())) {

                  //Set tranfer in progress to true
                  this.setTransferInProgress(true);

                  //Get the file
                  theFile = new File(aSrcFilePath);
                  theFileChannel = new RandomAccessFile(theFile, "r").getChannel();

                  this.currentOffset = offSet;
                  this.currentFragmentSize = currentFragmentSize;
                  this.remainingFragmentSize = currentFragmentSize;
                  this.myFileId = aFileId;
                  this.amountToTransfer = DATA_BUFFER_SIZE; //100MB = 100 * 1024 * 1024

                  fileMsgHeader_part1.clear();
                  fileMsgHeader_part2.clear();

                  //Get the Dest File Path In Bytes
                  //byte[] theDestFilePathInBytes = aDestFilePath.getBytes();
                  theDestFilePathInBytes = aDestFilePath.getBytes();
                  //Get the length of theFilePath
                  int theDestSize = theDestFilePathInBytes.length;
                  logger.info("Data Channel: THE DEST SIZE = " + theDestSize);

                  int theMsgType = FILE_MSG_TYPE;
                  //Insert the File Msg Type
                  fileMsgHeader_part1.putInt(theMsgType);
                  //Insert the Destination FileName Length
                  fileMsgHeader_part1.putInt(theDestSize);
                  //Flipe the fileMsgHeader
                  fileMsgHeader_part1.flip();

                  //Copy the theDestFilePathInBytes to the Byte Buf
                  ByteBuffer theDestFileBuf = ByteBuffer.allocate(theDestSize);
                  theDestFileBuf.put(theDestFilePathInBytes);
                  //Flip theDestFileBuf
                  theDestFileBuf.flip();



                  fileMsgHeader_part2.putLong(this.currentOffset);
                  fileMsgHeader_part2.putLong(this.currentFragmentSize);
                  fileMsgHeader_part2.putInt(this.myFileId);
                  //Flip
                  fileMsgHeader_part2.flip();

                  int bytesSent = -1;

                  //Send fileMsgHeader_part1 (FILE_MSG_TYPE & the Destination FileName Length)
                  while (fileMsgHeader_part1.hasRemaining()) {
                      bytesSent = mySocketChannel.write(fileMsgHeader_part1);
                  }
                  logger.info("Sent the File Msg Type and the number of characters in the file name that includes the file path");

                  //Send the destination file name
                  while (theDestFileBuf.hasRemaining()) {
                      bytesSent = mySocketChannel.write(theDestFileBuf);
                  }
                  logger.info("Sent the File Name of the Destination File Name");

                  //Send fileMsgHeader_part2 (the current file offset, file fragment length and the file id)
                  while (fileMsgHeader_part2.hasRemaining()) {
                      bytesSent = mySocketChannel.write(fileMsgHeader_part2);
                  }
                  logger.info("Sent the Current Offset, the Current Fragment Size and the File Id");

                  remainingFragmentSize = theFileChannel.size();
                  System.err.printf("%n FILE SIZE = %d %n", remainingFragmentSize);

                  amountToTransfer = DATA_BUFFER_SIZE; //100MB

                  if (remainingFragmentSize > 0) {
                      if (remainingFragmentSize < amountToTransfer) {
                          amountToTransfer = remainingFragmentSize;
                      }
                      System.err.printf("AMOUNT OF BYTES WE WANT TO TRANSFER FROM THE FILE TO THE SOCKET = %d %n", amountToTransfer);
                      //USE Zero Copy - to read data directly from file to the socket, reading 100MB at a time
                      theBytesTransferredToSocket = theFileChannel.transferTo(currentOffset, amountToTransfer, mySocketChannel);
                      System.err.printf("THE BYTES TRANSFERRED FROM THE FILE TO THE SOCKET = %d %n", theBytesTransferredToSocket);

                      if (theBytesTransferredToSocket > 0) {
                          //decrement the remainingFileFragmentLength
                          remainingFragmentSize -= theBytesTransferredToSocket;
                          //Increment current File Position
                          currentOffset += theBytesTransferredToSocket;
                          if (remainingFragmentSize < amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                              amountToTransfer = remainingFragmentSize;
                          }
                      }
                  } else {
                      //transferred the file fragment - either close the _socketChannel & deregister
                      // or change the SelectionKey interested operation to read
                      //How would I handle pipelining- since the documents says it's not good to have a selector
                      //look out for read events and write events on the same channel at the same time
                      //
                      //_doneSendingFileFragment = true;
                      this.setTransferInProgress(false);
                  }
                  //Set this data channel's selection key interest op to write
                  //To call the handleSendingFiles Method
                  mySelectionKey.interestOps(SelectionKey.OP_WRITE);
                  mySelectionKey.selector().wakeup();
              } else {
                  //Add current File Fragment to the file queue
                  //OuterClass.InnerClass innerObject = outerObject.new InnerClass();
                  this.addFileFragmentToFileQueue(aSrcFilePath, aDestFilePath, offSet, currentFragmentSize, aFileId);
              }



        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//End Start

    /*
       This method is called by this data channel in the run loop when this
       channels Selection Key Interest op is set to write, if the start sending method
       didn't finish sending a file fragment, this method is continously called
       by the selection key until the file fragment is totally sent along with the
       other files in the queue
     */
    public synchronized void handleSendingFiles(){
        try {
            //Finish sending the current File Fragment
            if (isTransferInProgress()) {
                //Ensure there is still data to transfer for the current file fragment
                if (remainingFragmentSize > 0) {
                    //Check to make sure the amount to transfer is set correctly
                    if (remainingFragmentSize < amountToTransfer) {
                        amountToTransfer = remainingFragmentSize;
                    }
                    System.err.printf("AMOUNT OF BYTES WE WANT TO TRANSFER FROM THE FILE TO THE SOCKET = %d %n", amountToTransfer);
                    //USE Zero Copy - to read data directly from file to the socket, reading 100MB at a time
                    theBytesTransferredToSocket = theFileChannel.transferTo(currentOffset, amountToTransfer, mySocketChannel);
                    System.err.printf("THE BYTES TRANSFERRED FROM THE FILE TO THE SOCKET = %d %n", theBytesTransferredToSocket);

                    if (theBytesTransferredToSocket > 0) {
                        //decrement the remainingFileFragmentLength
                        remainingFragmentSize -= theBytesTransferredToSocket;
                        //Increment current File Position
                        currentOffset += theBytesTransferredToSocket;
                        if (remainingFragmentSize < amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                            amountToTransfer = remainingFragmentSize;
                        }
                    }
                } else {
                    //Current File Fragment Transfer is complete
                    this.setTransferInProgress(false);
                    //Check to see if there are more file fragments to send
                    if (!fileQueue.isEmpty()) {
                        //There are more file fragments to send, get and process
                        //them the next time this method is called
                        mySelectionKey.interestOps(SelectionKey.OP_WRITE);
                        mySelectionKey.selector().wakeup();
                    } else {
                        //The file queue is empty there are no more file fragments
                        //to send. set the Selection Key Interest Op to Read
                        //This will stop the selector from selecting on write
                        //Since a Data Channel will never do a read, only
                        //Control Channels do reads
                        mySelectionKey.interestOps(SelectionKey.OP_READ);
                        mySelectionKey.selector().wakeup();
                    }
                }
            } else {
                //Check to see if there are file fragments to send in the queue
                //If there are get the first file fragment and start sending it
                //100MB at a time
                sendFiles();

            }
        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//End Start


    public synchronized void sendFiles(){
        try {

            //Check to see if there are file fragments to send in the queue
            //If there are get the first file fragment and start sending it
            //100MB at a time

            if (!fileQueue.isEmpty()) {

                //remove the file fragment object from the queue
                FileSenderDataChannelHandler.FileFragmentObject theFileFragmentObject = this.removeFileFragmentFromFileQueue();
                //Get the following data from the fragment object:
                //String aSrcFilePath, String aDestFilePath, long offSet, long currentFragmentSize, int aFileId

                //Set tranfer in progress to true
                this.setTransferInProgress(true);

                //Get the file
                theFile = new File(theFileFragmentObject.getTheSrcFilePath());
                theFileChannel = new RandomAccessFile(theFile, "r").getChannel();

                this.currentOffset = theFileFragmentObject.getTheOffSet();
                this.currentFragmentSize = theFileFragmentObject.getTheCurrentFragmentSize();
                this.remainingFragmentSize = currentFragmentSize;
                this.myFileId = theFileFragmentObject.getTheFileId();
                this.amountToTransfer = DATA_BUFFER_SIZE; //100MB

                fileMsgHeader_part1.clear();
                fileMsgHeader_part2.clear();

                //Get the Dest File Path In Bytes
                //byte[] theDestFilePathInBytes = aDestFilePath.getBytes();
                theDestFilePathInBytes = theFileFragmentObject.getTheDestFilePath().getBytes();
                //Get the length of theFilePath
                int theDestSize = theDestFilePathInBytes.length;

                //Insert the File Msg Type
                fileMsgHeader_part1.putInt(FILE_MSG_TYPE);
                //Insert the Destination FileName Length
                fileMsgHeader_part1.putInt(theDestSize);
                //Copy the theDestFilePathInBytes to the Byte Buf
                ByteBuffer theDestFileBuf = ByteBuffer.wrap(theDestFilePathInBytes);

                fileMsgHeader_part2.putLong(currentOffset);
                fileMsgHeader_part2.putLong(currentFragmentSize);
                fileMsgHeader_part2.putInt(myFileId);

                int bytesSent = -1;

                //Send fileMsgHeader_part1 (FILE_MSG_TYPE & the Destination FileName Length)
                while (fileMsgHeader_part1.hasRemaining()) {
                    bytesSent = mySocketChannel.write(fileMsgHeader_part1);
                }

                //Send the destination file name
                while (theDestFileBuf.hasRemaining()) {
                    bytesSent = mySocketChannel.write(theDestFileBuf);
                }

                //Send fileMsgHeader_part2 (the current file offset, file fragment length and the file id)
                while (fileMsgHeader_part2.hasRemaining()) {
                    bytesSent = mySocketChannel.write(fileMsgHeader_part2);
                }

                remainingFragmentSize = theFileChannel.size();
                System.err.printf("%n FILE SIZE = %d %n", remainingFragmentSize);

                if (remainingFragmentSize > 0) {
                    if (remainingFragmentSize < amountToTransfer) {
                        amountToTransfer = remainingFragmentSize;
                    }
                    System.err.printf("AMOUNT OF BYTES WE WANT TO TRANSFER FROM THE FILE TO THE SOCKET = %d %n", amountToTransfer);
                    //USE Zero Copy - to read data directly from file to the socket, reading 100MB at a time
                    theBytesTransferredToSocket = theFileChannel.transferTo(currentOffset, amountToTransfer, mySocketChannel);
                    System.err.printf("THE BYTES TRANSFERRED FROM THE FILE TO THE SOCKET = %d %n", theBytesTransferredToSocket);

                    if (theBytesTransferredToSocket > 0) {
                        //decrement the remainingFileFragmentLength
                        remainingFragmentSize -= theBytesTransferredToSocket;
                        //Increment current File Position
                        currentOffset += theBytesTransferredToSocket;
                        if (remainingFragmentSize < amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                            amountToTransfer = remainingFragmentSize;
                        }
                    }
                } else {
                    //transferred the file fragment - either close the _socketChannel & deregister
                    // or change the SelectionKey interested operation to read
                    //How would I handle pipelining- since the documents says it's not good to have a selector
                    //look out for read events and write events on the same channel at the same time

                    //Set this data channel's selection key interest op to write
                    //To call the handleSendingFiles Method
                    mySelectionKey.interestOps(SelectionKey.OP_WRITE);
                    this.setTransferInProgress(false);
                    mySelectionKey.selector().wakeup();
                }
            } else {
                //File Queue is Empty and we are not: Set the Selection Key's Interest Op to read
                //So the handleSendingFiles Method will not be called
                this.setTransferInProgress(false);
                mySelectionKey.interestOps(SelectionKey.OP_READ);
                mySelectionKey.selector().wakeup();
            }
        }catch(Exception e){
            System.err.printf("FileSenderHandler: Channel Active: Error: "+e.getMessage());
            e.printStackTrace();
        }
    }//End Start

    synchronized void handleRead() throws IOException {
        try {
            //Read Acknowledgement of the file
            System.out.println("Entered the read Method");


        }
        catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }


    public void registerTheChannel( )  {
        try {
            this.threadId = Thread.currentThread().getId();

            //registerChannelCtx          (String , FileSenderControlChannelHandler ,  int ,      int ,                    int ,     FileSenderDataChannelHandler , long ,      int ){
            FileSender.registerChannelCtx(myPathString,      null,              myChannelType,  myControlChannelId,  myDataChannelId,        this,                 threadId, myParallelNum );

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
                writeCallNumber++;
                logger.info("FileSenderDataChannelHandler: Write Call Number = " + writeCallNumber);
                //Check to see if the channel is registered with the Control Channel & FileSender
                if (!isChannelRegistered()){
                    logger.info("FileSenderDataChannelHandler: Channel is not registered");
                    //Register the Channel with the Control Channel & FileSender
                    this.registerTheChannel();
                    //Set Channel Registered to True
                    this.setIsChannelRegistered(true);
                    if (isChannelRegistered()){
                        logger.info("FileSenderDataChannelHandler: Channel is not registered");
                    }
                    this.sendConnectionMsg();
                }
                //handleSendingFiles();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
