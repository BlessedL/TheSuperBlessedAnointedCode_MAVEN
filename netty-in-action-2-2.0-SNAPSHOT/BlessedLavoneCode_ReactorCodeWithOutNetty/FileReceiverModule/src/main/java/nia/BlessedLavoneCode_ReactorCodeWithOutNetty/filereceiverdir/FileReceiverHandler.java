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
package nia.BlessedLavoneCode_ReactorCodeWithOutNetty.filereceiverdir;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.net.StandardSocketOptions;
import java.net.InetSocketAddress;


public class FileReceiverHandler implements Runnable {
  public final int CONNECTION_MSG_TYPE = 1;
  public final int FILE_MSG_TYPE = 2;
  public final int FILE_ACK_MSG_TYPE = 2;
  public final int DONE_MSG_TYPE = 3;
  public final int CONTROL_CHANNEL_TYPE = 0;
  public final int DATA_CHANNEL_TYPE = 1;

  public final int INT_SIZE = 4;
  public final int LONG_SIZE = 8;

  private final SocketChannel _socketChannel;
  private final SelectionKey _selectionKey;

  private ByteBuffer myConnectionMsg, myPathBuffer;
  private Logger logger;

  private HashMap<String, ArrayList<FileAckObject>> myFileAckHashMap;

  private int _writeCallNumber, _readCallNumber, numBytesRead;
  private int msgType, myChannelType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum;
  private int myPathSize, myAliasPathSize, myPrintThroughputMsgVal;
  private ByteBuffer _fileHeaderBuffer, _myFilePathBuffer, myAliasPathSizeBuffer, myAliasPathBuffer;
  private ByteBuffer msgTypeBuf, printThroughputMsgTypeBuf;
  private ByteBuffer destFilePathSizeBuf;
  private boolean _fileHeaderReadIn, _destFilePathReadIn, _fileFragmentTransferComplete;
  private boolean connectionMsgReceived, msgTypeSet, startTimeSet, readInPrintThroughputMsg;
  private boolean readInDestFileNameSize;
  private int connectionMsgReceivedNum;
  private String channelTypeString;

  private long _bytesRead, _currentOffset, _fragmentLength, _remainingFileFragmentLength, _amountToTransfer;
  private long _theBytesTransferredFromSocket, threadId;
  private long _timeStartedTransfer, _timeEndedTransfer, _totalBytesTransferred;
  private double _throughput;

  private int _destFilePathSize, _fileId;
  private String _theDestFilePath, myFilePath, myAliasFilePath, threadName;
  private File _myFile;
  private FileChannel _fc;
  private ArrayList<FileReceiverHandler> myDataChannelHandlerList;
  private FileReceiverHandler myControlChannelHandler;
  private boolean myMinStartTimeSet, myMaxEndTimeSet, destFilePathSizeReadIn;
  //private boolean readInDestFileNameSize;
  private long myMinStartTime, myMaxEndTime, myTotalBytesRead;
  private Selector mySelector;


  public  FileReceiverHandler(Selector selector, SocketChannel socketChannel) throws IOException {
    mySelector = selector;
    logger = Logger.getLogger(FileReceiverHandler.class.getName());
    _socketChannel = socketChannel;
    _socketChannel.configureBlocking(false);
    _fileId = -1;
    destFilePathSizeReadIn = false;
    readInDestFileNameSize = false;
    readInDestFileNameSize = false;

    myConnectionMsg = ByteBuffer.allocateDirect(28);
    _bytesRead = 0; _currentOffset = 0; _fragmentLength = -1; _remainingFileFragmentLength =-1;
    _amountToTransfer = 100*1024*1024;
    _theBytesTransferredFromSocket = -1;
    _destFilePathSize = -1;
    _theDestFilePath = null; myFilePath = null; myAliasFilePath = null;

    _writeCallNumber = 0; _readCallNumber = 0; numBytesRead = -1;
    msgType = -1; myChannelType = -1; myControlChannelId = -1; myDataChannelId = -1;
    myParallelNum = -1; myConcurrencyNum = -1; myPathSize = -1; myAliasPathSize = -1;
    destFilePathSizeBuf = ByteBuffer.allocate(4);
    _fileHeaderBuffer = ByteBuffer.allocateDirect(20);
    msgTypeBuf = ByteBuffer.allocateDirect(4); printThroughputMsgTypeBuf = ByteBuffer.allocateDirect(4);
    _myFilePathBuffer = null; myPathBuffer = null; myAliasPathSizeBuffer = ByteBuffer.allocateDirect(4); myAliasPathBuffer = null;
    _fileHeaderReadIn = false; _destFilePathReadIn = false; _fileFragmentTransferComplete = false;
    connectionMsgReceived = false; msgTypeSet = false; startTimeSet = false; readInPrintThroughputMsg = false;
    connectionMsgReceivedNum = 0;
    _myFile = null;
    _fc = null;
    _timeStartedTransfer = -1; _timeEndedTransfer = -1; _totalBytesTransferred = 0; _throughput = 0; myPrintThroughputMsgVal = -1;
    myDataChannelHandlerList = null;
    myControlChannelHandler = null;
    myMinStartTimeSet = false; myMaxEndTimeSet = false;
    myMinStartTime = 0; myMaxEndTime = 0; myTotalBytesRead = 0;
    channelTypeString = "";
    //_throughput = overAllThroughput = (((overallTotalBytes * 8) / (overallMaxEndTime - overallMinStartTime)) * 1000) / 1000000; Mb/s
    //_throughput = (((_totalBytesTransferred * 8) / (_timeEndedTransfer - _timeStartedTransfer)) * 1000) / 1000000;
    //timeStarted = System.currentTimeMillis();
    //_timeStartedTransfer = System.currentTimeMillis();
    //_timeEndedTransfer = System.currentTimeMillis();
     //Change this because the thread ID can change depending on who is calling the selector method
     threadId = Thread.currentThread().getId();
     threadName = Thread.currentThread().getName();
    // Register _socketChannel with _selector listening on OP_READ events.
    // Callback: FileReceiverHandler, selected when the connection is established and ready for READ
    _selectionKey = _socketChannel.register(mySelector, SelectionKey.OP_READ);
    _selectionKey.attach(this); //When attaching this FileReceiverHandler, are the states updated or the same
    //LAR: Why are we waking up Select again?
    //LAR: What thread is this waking up the selector?
    //LAR: How do we know the Selector is sleep?
    selector.wakeup(); // let blocking select() return
  }

  public void run() {
    try {
      //LAR: How do we know this _selectionKey is readable, the actual _selectionKey with the "ready to read event " as returned by the Selector wasn't passed
      //LAR: into the run method, this _selectionKey just registered for the read event.
      //LAR: but I guess, since this run method is being called back and we have one SelectionKey per channel, than
      //LAR: the same Selection key is used. I think this is because the Selector has 2 sets of the Selection Key. This Selection Key
      //LAR: is always in the Selector's "key set" this happens when the socketChannel was registered with the Selector
      //LAR: and this SelectionKey will be in the "Selected Key Set" if an interested I/O event occurs and then removed. But just because this SelectionKey
      //LAR: is removed from the "Selected Key Set" that doesn't mean it is removed from the "Key Set", so it will always be in the "Key Set" unless the
      //LAR "Selection Key" is cancelled due to a channel closing or deregistering from the Selector.
      //LAR: "But this is how I can have a FileReceiverHandler per channel, because a SelectionKey is associated with a channel, and this FileReceiverHandler creates and keeps track of the SelectionKey,
      //LAR: keeps track of state".
      //LAR: This FileReceiverHandler keeps track of state for this channel
      if (_selectionKey.isReadable()) {
        handleRead();
      }
      else if (_selectionKey.isWritable()) {
        write();
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // Process data by echoing input to output
  synchronized void process() {
    System.err.printf("Process Method Entered");
        /*
         //LAR: But can't _readBuf still be accessed by the I/O Selector thread
        _readBuf.flip();
        byte[] bytes = new byte[_readBuf.remaining()];
        _readBuf.get(bytes, 0, bytes.length);
        System.out.print("process(): " + new String(bytes, Charset.forName("ISO-8859-1")));

        _writeBuf = ByteBuffer.wrap(bytes);

        // Set the key's interest to WRITE operation
        //LAR: I only have to attach the FileReceiverHandler once
        _selectionKey.interestOps(SelectionKey.OP_WRITE);
        //LAR: when waking up the selector thread is it this thread safe
        _selectionKey.selector().wakeup();
        */
  }

  //LAR: why is this method synchronized?
  //LAR: Isn't only one channel/Selection Key associated with this method?
  //LAR: but do all FileReceiverHandlers share the same single thread
  //LAR: What happens if this method is not synchronized?
  synchronized void handleRead() throws IOException {

  //Assumes ByteBuffer has already been flipped{
    try {
    _readCallNumber++;
    System.err.printf("%s: FileReceiverHandler: Read Call Number = %d %n",channelTypeString, _readCallNumber);
    //Read in connection Msg
      if (!connectionMsgReceived){
        logger.info("FileReceiverHandler: handleRead: DID NOT RECEIVE CONNECITON MSG");
        logger.info("FileReceiverHandler: handleRead: Connection MSG NUM = " + connectionMsgReceivedNum + "\n");
        int totalBytesReadFromSocket = 0;
        while (myConnectionMsg.hasRemaining()) {
          numBytesRead = _socketChannel.read(myConnectionMsg);
          if (numBytesRead > 0)
            totalBytesReadFromSocket+=numBytesRead;
            logger.info("FileReceiverHandler: handleRead: Reading in CONNECTION MSG, READ IN " + numBytesRead + " Bytes, Total Bytes Read In = " + totalBytesReadFromSocket + "\n");
            System.err.printf("FileReceiverHandler: handleRead: Reading in CONNECTION MSG, READ IN %d Bytes, Total Bytes Read In %d Bytes %n", numBytesRead,totalBytesReadFromSocket);
          }
        //Flip the myConnectionMsg
        myConnectionMsg.flip();
        //Get the Msg Type
        msgType = myConnectionMsg.getInt();
        msgTypeSet = true;
        logger.info("FileReceiverHandler: handleRead: Read in Msg Type, Msg Type = " + msgType);
        System.err.printf("FileReceiverHandler: handler Read: Read in Msg Type, Msg Type = %d %n", msgType);
        //Get the Channel Type
        myChannelType = myConnectionMsg.getInt();
        channelTypeString = ((myChannelType == CONTROL_CHANNEL_TYPE) ? "CONTROL CHANNEL" : " DATA CHANNEL ");
        logger.info(channelTypeString +": FileReceiverHandler: handleRead: Read in Channel Type, Channel Type = " + myChannelType );
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Channel Type, Channel Type = %d %n", channelTypeString, myChannelType);
        //Get the Control Channel ID
        myControlChannelId = myConnectionMsg.getInt();
        logger.info(channelTypeString +": FileReceiverHandler: handleRead: Read in Control Channel ID, Channel ID = " + myControlChannelId );
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Control Channel ID, Channel ID = %d %n", channelTypeString, myControlChannelId );

        //Get the Data Channel ID
        myDataChannelId = myConnectionMsg.getInt();
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Data Channel ID, Data Channel ID = %d %n",channelTypeString, myDataChannelId);
        //Get the Parallel Num
        myParallelNum = myConnectionMsg.getInt();
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Parallel Num, Parallel Num = %d %n", channelTypeString, myParallelNum);
        //Get the Concurrency Num
        myConcurrencyNum = myConnectionMsg.getInt();
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Concurrency Num, Concurrency Num = %d %n", channelTypeString, myConcurrencyNum);
        //Get the Path Size
        myPathSize = myConnectionMsg.getInt();
        System.err.printf("FileReceiverHandler: %s handler Read: Read in Path Size, Path Size = %d %n", channelTypeString, myPathSize);
        //Get the Path Buffer
        myPathBuffer = ByteBuffer.allocate(myPathSize);
        //Read in the Network Path in IP Address Format
        //If the network path is: WS5,WS11,WS12,WS7 then ONLY THE
        //IP ADDRESSES OF WS11,WS12,WS7 are sent
        totalBytesReadFromSocket = 0;
        while (myPathBuffer.hasRemaining()) {
          numBytesRead = _socketChannel.read(myPathBuffer);
          if (numBytesRead > 0)
            totalBytesReadFromSocket+=numBytesRead;
            logger.info("FileReceiverHandler: " + channelTypeString + " handleRead: Reading in the FILE PATH, READ IN " + numBytesRead + " Bytes, Total Bytes Read In = " + totalBytesReadFromSocket + "\n");
            System.err.printf("FileReceiverHandler: %s handleRead: Reading in FILE PATH, READ IN %d Bytes, Total Bytes Read In %d Bytes %n", channelTypeString, numBytesRead,totalBytesReadFromSocket);
        }
        //Flip the Buffer
        myPathBuffer.flip();
        myFilePath = StandardCharsets.US_ASCII.decode(myPathBuffer).toString();
        System.err.printf("FileReceiverHandler: %s Path in IP Address Format = %s %n", channelTypeString, myFilePath);

        //Read in the size of the Alias Path: WS5,WS11,WS12,WS7
        while (myAliasPathSizeBuffer.hasRemaining()) {
          numBytesRead = _socketChannel.read(myAliasPathSizeBuffer);
        }
        //Flip the Buffer
        myAliasPathSizeBuffer.flip();
        myAliasPathSize = myAliasPathSizeBuffer.getInt();

        myAliasPathBuffer = ByteBuffer.allocate(myAliasPathSize);

        //Read in the Alias Path: WS5,WS11,WS12,WS7
        while (myAliasPathBuffer.hasRemaining()) {
          numBytesRead = _socketChannel.read(myAliasPathBuffer);
        }

        //Flip the Buffer
        myAliasPathBuffer.flip();
        myAliasFilePath = StandardCharsets.US_ASCII.decode(myAliasPathBuffer).toString();
        System.err.printf(channelTypeString +": FileReceiverHandler: %s Alias Path = %s %n",channelTypeString, myAliasFilePath);

      ////////////////////////////////////////
      // SET CONNECTION MSG RECEIVED TRUE  //
      ///////////////////////////////////////
      connectionMsgReceived = true;
      //Set msgType Set to false
      msgTypeSet = false;
      connectionMsgReceivedNum++;
      logger.info(channelTypeString +": FileReceiverHandler: handleRead: INCREASED Connection MSG NUM, IT NOW EQUALS = " + connectionMsgReceivedNum + "\n");

      //Register the Control Channel or Data Channel
      //Note the registerChannelCtx method will return NULL if all data channels have not registered
      FileReceiverHandler aChannelHandler = null;
      if (myChannelType == CONTROL_CHANNEL_TYPE) {
        myDataChannelHandlerList = new ArrayList<FileReceiverHandler>();
        this.myControlChannelHandler = this;
        //REGISTER THIS CONTROL CHANNEL
        //Pass in this FileReceiverHandler, this is the Control Channel Handler
        aChannelHandler = FileReceiver.registerChannel(myAliasFilePath, myChannelType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum, this, null, threadId);
        //Create the FileAckObject Hash Map for this ControlChannelHandler
        myFileAckHashMap = new HashMap<String, ArrayList<FileAckObject>>();
      } else {
        //REGISTER THIS DATA CHANNEL
        aChannelHandler = FileReceiver.registerChannel(myAliasFilePath, myChannelType, myControlChannelId, myDataChannelId, myParallelNum, myConcurrencyNum, null, this, threadId);
      }
      //Check to see if all data channels have registered:  If so, send the Connection Ack through the control channel
      //aCtx is the Control Channel Context Handler that is returned when all data channels have connected
      //Whether this is a Control Channel registering or a Data Channel Registering, if aCtx returns Null
      //this either means all data channels have not connected yet or the control channel have not connected yet
      if (aChannelHandler != null) {
        //Create the Connection Ak Byte Buf
        ByteBuffer aConnectAckBuf = ByteBuffer.allocateDirect(INT_SIZE).putInt(CONNECTION_MSG_TYPE);
        //Flip the Buffer, limit = position = 4, position reset to 0;
        aConnectAckBuf.flip();
        //aCtx.write(connectionAckMsg);
        if (myChannelType == DATA_CHANNEL_TYPE) {
          FileReceiverHandler controlChannelHandler = aChannelHandler;
          //Send the Connection Ack MsG through the Control Channel for this Data Channel
          controlChannelHandler.send(aConnectAckBuf);
          logger.info("FileReceiverHandler: ChannelRead: This is Data Channel #" + myDataChannelId + " belonging to Control Channel " + myControlChannelId + " ALL CHANNELS ARE REGISTERED ");
        } else {
          //This FileReceiverHandler is the Control Channel and all Data Channels have received the Connection Ack Message
          //Send the Connection Ack MsG through this Control Channel
          this.send(aConnectAckBuf);
        }
      }

    } else {
      //If the Msg Type is Not Set And already read in the connection msg, Read in the Msg Type
      if (!msgTypeSet) {
        logger.info(channelTypeString + ": FileReceiverHandler: handleRead: MSG TYPE IS NOT SET");
        if (msgTypeBuf.hasRemaining()) {
          numBytesRead = _socketChannel.read(msgTypeBuf);
          logger.info(channelTypeString + ": Number of Bytes Read = " + numBytesRead);
        }
        if (msgTypeBuf.remaining() == 0) {
          //Flip the msgTypeBuf - set limit = position = 4, and reset position back to 0
          msgTypeBuf.flip();
          msgType = msgTypeBuf.getInt();//Get Size at index = 0;
          msgTypeSet = true;
          //Clear msgTypeBuf - set position back to 0 and limit to capacity = 4
          msgTypeBuf.clear();
          _timeStartedTransfer = System.currentTimeMillis();
          startTimeSet = true;
          String msgTypeString = ((msgType == FILE_MSG_TYPE) ? "FILE MSG TYPE" : " MSG TYPE TO BE DETERMINED ");
          logger.info(channelTypeString + ": (msgTypeBuf.remaining() == 0) Read in Msg Type: Msg TyPe = " + msgType);
        }
      }
      if (msgTypeSet) {
        logger.info(channelTypeString + " MSG TYPE SET and MSG_TYPE = " + msgType);
        if (msgType == FILE_MSG_TYPE) {
          logger.info(channelTypeString + " MSG TYPE == FILE_MSG_TYPE");
            /*
            _fileHeaderBuffer.putLong(_currentOffset); //offset position (long - 8 bytes)
            _fileHeaderBuffer.putLong(_remainingFileFragmentLength); //length of fragment (long - 8 bytes)
            _fileHeaderBuffer.putInt(_myDestFilePathSize); //length of the fileName (number of chars in fileName) (int 4 bytes)
            _fileHeaderBuffer.flip();
            _myFilePathBuffer = ByteBuffer.allocateDirect(_myDestFilePathSize);
            _myFilePathBuffer.put(_myDestFilePathInBytes);
            _myFilePathBuffer.flip();
            */
          ///////////////////////////////////////
          if (!destFilePathSizeReadIn){
            //destFilePathSizeBuf
            logger.info(channelTypeString + ": About to read in the Size of the Destination File Name (The Number of Characters in the fileName which includes the File Path");
            //Read in the file header
            if (destFilePathSizeBuf.hasRemaining()) {
              /////////////////////////////////////////////////
              _bytesRead = _socketChannel.read(destFilePathSizeBuf);
              //////////////////////////////////////////////////
              //System.err.printf("FileReceiverHandler: Reading File Header bytes, read % d bytes %n", _bytesRead);
              logger.info(channelTypeString + " Read in " + _bytesRead + " Bytes out of 4 Bytes, this 4 bytes is the integer number of the number of characters in the Destination File Name including the path");
            }else {
              logger.info(channelTypeString + ": destFilePathSizeBuf.hasRemaining() == false");
            }

            if (destFilePathSizeBuf.remaining() == 0 ) {
               //note I have to specify tge index or do destFilePathSizeBuf. flip since the position of destFilePathSizeBuf is at the end
              _destFilePathSize = destFilePathSizeBuf.getInt(0);
              logger.info(channelTypeString + " read in the Size of the Destination File Name (The Number of Characters in the fileName) BEFORE FLIP EQUALS: " + _destFilePathSize);
              //Flip the File Header Buffer
              //destFilePathSizeBuf.flip();
              //_destFilePathSize = destFilePathSizeBuf.getInt();
              logger.info(channelTypeString + " read in the Size of the Destination File Name (The Number of Characters in the fileName) AFTER FLIP EQUALS: " + _destFilePathSize);

              //Allocate the File Path Buffer
              _myFilePathBuffer = ByteBuffer.allocate(_destFilePathSize);

              //Set Flag indicating read in File Header
              destFilePathSizeReadIn = true;

              //Increment _totalBytesTransferred
              _totalBytesTransferred += destFilePathSizeBuf.capacity();

              //Clear Dest File Path Size Buffer
              destFilePathSizeBuf.clear();
              //_timeStartedTransfer = System.currentTimeMillis();
            }

          } else {
            logger.info(channelTypeString + ": ALreadY READ IN THE NUMBER OF CHARACTERS IN THE DESTINATION FILE NAME");
          }
          if ((destFilePathSizeReadIn) && (!_destFilePathReadIn)) {
            //Read in the destFileNamePath
            if (_myFilePathBuffer.hasRemaining()) {
              /////////////////////////////////////////////////
              _bytesRead = _socketChannel.read(_myFilePathBuffer);
              //////////////////////////////////////////////////
              //System.err.printf("FileReceiverHandler: Reading Destination File Path, read %d bytes %n", _bytesRead);
              logger.info(channelTypeString + ": Reading Destination File Path, read " + _bytesRead + " bytes ");
            }
            if (_myFilePathBuffer.remaining() == 0) {

              _myFilePathBuffer.flip();
              _totalBytesTransferred += _myFilePathBuffer.capacity();

              //_thePath = _myFilePathBuffer.toString(Charset.forName("US-ASCII"));
              //https://stackoverflow.com/questions/17354891/java-bytebuffer-to-string
              //_thePath = StandardCharsets.UTF_8.decode(_myFilePathBuffer).toString();
              _theDestFilePath = StandardCharsets.US_ASCII.decode(_myFilePathBuffer).toString();
              //System.err.printf("FileReceiverHandler: Read in Destination File Path, destination file path =  %s bytes %n", _theDestFilePath);
              logger.info(channelTypeString + " Read in Destination File Path, destination file path = " + _theDestFilePath);

              //set file channel
              _myFile = new File(_theDestFilePath);
              //_myFile.createNewFile();
              _fc = new RandomAccessFile(_myFile, "rw").getChannel();
              _myFile.createNewFile();

              //Set _destFilePathReadIn  to true
              _destFilePathReadIn = true;

              //CLEAR File Path Buffer
              _myFilePathBuffer.clear();
            }//End if _myFilePathBuffer.remaining = 0
          }
          if ((_destFilePathReadIn) && ((!_fileHeaderReadIn))) {
            logger.info("Did not read in the remaining file header yet....");
            //Read in the file header
            if (_fileHeaderBuffer.hasRemaining()) {
              /////////////////////////////////////////////////
              _bytesRead = _socketChannel.read(_fileHeaderBuffer);
              //////////////////////////////////////////////////
              //System.err.printf("FileReceiverHandler: Reading File Header bytes, read %d bytes %n", _bytesRead);
              logger.info(channelTypeString + ": Reading File Header bytes, read " + _bytesRead +" bytes");
            }else {
              logger.info(channelTypeString + ": fileHeaderBuffer.hasRemaining() == false");
            }
            if (_fileHeaderBuffer.remaining() == 0) {
              //Flip the File Header Buffer
              _fileHeaderBuffer.flip();
              _currentOffset = _fileHeaderBuffer.getLong();
              //System.err.printf("FileReceiverHandler: offset = %d %n", _currentOffset);
              logger.info(channelTypeString + " Current Offset = " + _currentOffset);
              _fragmentLength = _fileHeaderBuffer.getLong();
              _remainingFileFragmentLength = _fragmentLength;
              //System.err.printf("FileReceiverHandler: Fragment Length = %d %n", _fragmentLength);
              logger.info(channelTypeString + " Fragment Length = " + _fragmentLength);
              //Read in the File ID
              _fileId = _fileHeaderBuffer.getInt();
              logger.info(channelTypeString + " File ID: = " + _fileId);

              //Set Flag indicating read in File Header
              _fileHeaderReadIn = true;

              //Increment _totalBytesTransferred
              _totalBytesTransferred += _fileHeaderBuffer.capacity();

              //Clear File Header Buffer
              _fileHeaderBuffer.clear();

              if (_remainingFileFragmentLength < _amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                _amountToTransfer = _remainingFileFragmentLength;
              }

              _timeStartedTransfer = System.currentTimeMillis();

            }
          }
          if ((_fileHeaderReadIn) && (!_fileFragmentTransferComplete)) {
            //USE Zero Copy - to read data directly from the socket to the file, reading 100MB at a time if permitted
            logger.info(channelTypeString + "BEFORE ZERO COPY TRANSFER, CURRENT OFFSET = " + _currentOffset + " AND AMOUNT TO TRANSFER = " + _amountToTransfer);
            _theBytesTransferredFromSocket = _fc.transferFrom(_socketChannel, _currentOffset, _amountToTransfer);
            if (_theBytesTransferredFromSocket > 0) {
              System.err.printf(channelTypeString + " Number of bytes transferred from socket to file = %d, starting at current offset %d, amount wanted to be transferred %d %n", _theBytesTransferredFromSocket, _currentOffset, _amountToTransfer);
              //Increment Total Bytes Transferred
              _totalBytesTransferred += _theBytesTransferredFromSocket;
              //decrement the remainingFileFragmentLength
              _remainingFileFragmentLength -= _theBytesTransferredFromSocket;
              //Increment current File Position
              _currentOffset += _theBytesTransferredFromSocket;
              //If the amount of bytes to transfer is greater than the remaining file length, then set the amout to transfer to the remaining length
              if (_remainingFileFragmentLength < _amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                _amountToTransfer = _remainingFileFragmentLength;
                 logger.info(channelTypeString + " amount to transfer = " + _amountToTransfer + " and the remaining fragment length = " + _remainingFileFragmentLength);
              }

            }
            if (_remainingFileFragmentLength <= 0) {
              logger.info(channelTypeString+ " Remaining Fragment length = " + _remainingFileFragmentLength);
              _timeEndedTransfer = System.currentTimeMillis();
              _fileFragmentTransferComplete = true;
              //RESET ALL VARIABLES
               msgTypeSet = false;
               destFilePathSizeReadIn = false;
              _destFilePathReadIn = false;
              _fileHeaderReadIn = false;
              _fileFragmentTransferComplete = false;

              //CLEAR ALL BUFFERS
              //ALL BUFFERS WERE CLEARED

              _throughput = (((_totalBytesTransferred * 8) / (_timeEndedTransfer - _timeStartedTransfer)) * 1000) / 1000000;
              String _units = "Mb/s";
              System.err.printf("%n FileReceiverHandler: Throughput = %f, %s %n", _throughput, _units);
              //Done reading File
              System.err.printf(" %n FileReceiverHandler: Done Reading in FileFragment");
            }//End if _remainingFileFragmentLength <= 0
          } //End (!_fileFragmentTransferComplete)

          ////////////////////////////////////////
        } else if (msgType == DONE_MSG_TYPE) {
          logger.info("\n********** RECEIVED THE DONE MSG TYPE *****************\n");
          if (!readInPrintThroughputMsg) {
            numBytesRead = _socketChannel.read(printThroughputMsgTypeBuf);
            if (printThroughputMsgTypeBuf.remaining() == 0) {
              printThroughputMsgTypeBuf.flip();
              myPrintThroughputMsgVal = printThroughputMsgTypeBuf.getInt();//Get Size at index = 0;
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
        }else {
          logger.info("MSG TyPe is NOT EQUAL TO THE FILE_MSG_TYPE oR the DONE_MSG_TYPE");
        }
      }//End msgTypeSet
      else {
        logger.info("MSG TyPe is NOT EQUAL TO THE FILE_MSG_TYPE oR the DONE_MSG_TYPE");
      }


    }//End Else
  }catch (Exception e) {
    logger.info("FileReceiverHandler: Error: " + e.getMessage() + "\n");
    e.printStackTrace();
    return;
  }
}
  //Control Channel uses this method to send data through it's channel
  public synchronized void send(ByteBuffer aBuffer) {
    try {
      //Set the Selection key to write - no need I will just do a write without it
      int numBytesWrote = -1;
      if (aBuffer != null) {
        if (aBuffer.hasRemaining()) {
          while (aBuffer.hasRemaining()) {
            numBytesWrote = _socketChannel.write(aBuffer);
          }
        }
      }
    }catch(Exception e){
      System.err.printf("FileReceiverHandler: send error: %s %n", e.getMessage());
      e.printStackTrace();
    }
  }

  public void setControlChannelHandler(FileReceiverHandler aControlChannelHandler){
    myControlChannelHandler = aControlChannelHandler;
  }

  public synchronized boolean addDataChannelHandler(FileReceiverHandler aDataChannelHandler){
    boolean addedDataChannelHandler = false;
    if (myDataChannelHandlerList != null){
      addedDataChannelHandler = myDataChannelHandlerList.add(aDataChannelHandler);
    }
    return addedDataChannelHandler;
  }

  void write() throws IOException {
    System.err.printf("FileSenderHandler: Write Method Entered");

  }

  public synchronized void setMinStartTime(long aVal){
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

  public synchronized long getMinStartTime(){
    return myMinStartTime;
  }

  public synchronized void setMaxEndTime(long aVal){
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

  public synchronized long getMaxEndTime(){
    return myMaxEndTime;
  }

  public synchronized void addToTotalBytes(long aVal){
    try {
      if (aVal > 0) {
        myTotalBytesRead += aVal;
      }
    }catch(Exception e){
      System.err.printf("FileReceiver: ControlChannelObject: addToTotalBytes: error msg: " + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }

  public synchronized long getTotalBytesRead(){
    return myTotalBytesRead;
  }

}//End of Class











