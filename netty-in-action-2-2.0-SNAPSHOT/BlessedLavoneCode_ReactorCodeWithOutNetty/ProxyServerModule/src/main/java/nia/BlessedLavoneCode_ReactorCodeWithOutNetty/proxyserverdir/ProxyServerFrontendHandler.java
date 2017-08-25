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
//package nia.BlessedLavoneCodeWithEverything.proxyserverdir;
package nia.BlessedLavoneCode_ReactorCodeWithOutNetty.proxyserverdir;

import java.util.logging.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.File;
import java.net.StandardSocketOptions;
import java.net.InetSocketAddress;

public class ProxyServerFrontendHandler implements Runnable {
    ///////////////////////
    // REACTOR VARIABLES //
    ///////////////////////
    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;

    private boolean backEndSocketChannelWriteLock;

    private Selector _selector;
    private final SocketChannel _socketChannel;
    private SocketChannel backendSocketChannel;
    private final SelectionKey _selectionKey;
    private final int DATA_BUFFER_SIZE = 100 * 1024 * 1024; //100MB
    private final int ACK_BUFFER_SIZE = 8 * 1024;//8KB

    private int numBytesRead, numBytesWrote, myDataChannelId, myParallelNum, myConcurrencyNum, myPathSize;
    private ProxyServerBackendHandler myProxyServerBackendHandler;
    private boolean backendSocketChannelConnected;

    private ByteBuffer dataBuffer, myConnectionMsg, myPathBuffer, myAliasPathSizeBuffer, myAliasPathBuffer, ackBuffer;

    private int _writeCallNumber, _readCallNumber;
    private ByteBuffer _fileHeaderBuffer, _myFilePathBuffer;
    private boolean _fileHeaderReadIn, _destFilePathReadIn, _fileFragmentTransferComplete;

    private long _bytesRead, _currentOffset, _fragmentLength, _remainingFileFragmentLength, _amountToTransfer;
    private long _theBytesTransferredFromSocket;
    private long _timeStartedTransfer, _timeEndedTransfer, _totalBytesTransferred;
    private double _throughput;

    private int _destFilePathSize, myAliasPathSize;
    private String channelTypeString, _theDestFilePath, myFilePath, myAliasFilePath;
    private File _myFile;
    private FileChannel _fc;
    private int myChannelType, myControlChannelId, numBytesSent;
    private ProxyServerFrontendHandler myProxyServerFrontendHandler;

    ///////////////////////
    //  NETTY VARIABLES  //
    ///////////////////////
    private boolean msgTypeSet,connectionMsgReceived,pathLengthSet,readInPath, connected;
    private boolean readInRestOfMsg;
    //Actual Variables holding the values of the msg
    private int msgType, remotePort;
    private String theNodeToForwardTo, remoteHost;
    private Logger logger;
    private long threadId;
    public final int CONNECTION_MSG_TYPE = 1;
    public final int INT_SIZE = 4;
    public int remainingBytes;
    //////////////////////////////////////////////////////////

    public ProxyServerFrontendHandler(Selector aSelector, SocketChannel aSocketChannel) throws IOException{
        //////////////////////////////////
        // MY REACOTR PATTERN VARIABLES //
        /////////////////////////////////

        backEndSocketChannelWriteLock = false;
        channelTypeString = "";
        //ALLOCATE BYTEBUFFERS
        dataBuffer = ByteBuffer.allocateDirect(DATA_BUFFER_SIZE);
        ackBuffer = ByteBuffer.allocateDirect(ACK_BUFFER_SIZE);

        myConnectionMsg = ByteBuffer.allocateDirect(28);
        numBytesRead = -1;

        _selector = aSelector;
        _socketChannel = aSocketChannel;
        _socketChannel.configureBlocking(false);
        _bytesRead = 0; _currentOffset = 0; _fragmentLength = -1; _remainingFileFragmentLength =-1;
        _amountToTransfer = 100*1024*1024;
        _theBytesTransferredFromSocket = -1;
        _destFilePathSize = -1;
        _theDestFilePath = null;

        _writeCallNumber = 0; _readCallNumber = 0;
        _fileHeaderBuffer = ByteBuffer.allocateDirect(20);
        _myFilePathBuffer = null;
        _fileHeaderReadIn = false; _destFilePathReadIn = false; _fileFragmentTransferComplete = false;
        _myFile = null;
        _fc = null;
        backendSocketChannel = null;
        backendSocketChannelConnected = false;
        myProxyServerFrontendHandler = this;

        // Register _socketChannel with _selector listening on OP_READ events.
        // Callback: FileReceiverHandler, selected when the connection is established and ready for READ
        _selectionKey = _socketChannel.register(_selector, SelectionKey.OP_READ);
        _selectionKey.attach(this); //When attaching this FileReceiverHandler, are the states updated or the same
        //LAR: Why are we waking up Select again?
        //LAR: What thread is this waking up the selector?
        //LAR: How do we know the Selector is sleep?
        _selector.wakeup(); // let blocking select() return

        ////////////////////////
        //NETTY VARIABLES///////
        ////////////////////////
        msgTypeSet = false;
        connectionMsgReceived = false;
        pathLengthSet = false;
        readInPath = false;
        connected = false;
        numBytesSent = -1;


        myAliasPathSizeBuffer = ByteBuffer.allocateDirect(INT_SIZE);

        logger = Logger.getLogger(ProxyServerFrontendHandler.class.getName());
        readInRestOfMsg = false;
        ////////////////////////////////////////
    }

    // Connector: Finish the connection process and assign a FileSenderHandler to the SocketChannel/SelectionKey.
    private class Connector implements Runnable {
        SelectionKey theSelectionKey;

        Connector(SelectionKey aKey) throws IOException {
            theSelectionKey = aKey;
        }

        public void run() {
            try {
                backendSocketChannelConnected = backendSocketChannel.finishConnect();
                if (backendSocketChannelConnected) {
                    //I want this to refer to the ProxyServerFrontEndHandler
                    //is this referring to the Connector Class or the ProxyServerFrontEndHandler
                    //new ProxyServerBackendHandler(_selector, backendSocketChannel, this.getProxyServerFrontendHandler(), myChannelType );
                    myProxyServerBackendHandler = new ProxyServerBackendHandler(_selector, backendSocketChannel, myProxyServerFrontendHandler, myChannelType );
                }
                else {
                    System.err.printf("SocketChannel did not connect to remote peer, deregistering the SelectionKey associated with this socketChannel from the selector");
                    theSelectionKey.cancel();
                }
            }
            catch (Exception e) {
                System.err.printf("Connector: Error Connecting to remote peer: %s %n", e.getMessage());
                e.printStackTrace();
                theSelectionKey.cancel();
            }
        }
    }


    public void setConnection(boolean aVal){
        logger.info("Set Connection to " + aVal);
        connected = aVal;
    }

    public boolean getConnected(){
        return connected;
    }

    //LAR: why is this method synchronized?
    //LAR: Isn't only one channel/Selection Key associated with this method?
    //LAR: but do all FileReceiverHandlers share the same single thread
    //LAR: What happens if this method is not synchronized?
    synchronized void read() throws IOException {
        try {
            //////////////////////////////////////
            // READ IN THE CONNECTION MSG FIRST //
            //////////////////////////////////////
            /*
            myConnectionMsg.putInt(msgType);
            myConnectionMsg.putInt(myChannelType);
            myConnectionMsg.putInt(myControlChannelId);
            myConnectionMsg.putInt(myDataChannelId);
            myConnectionMsg.putInt(myParallelNum);
            myConnectionMsg.putInt(myConcurrencyNum);
            myConnectionMsg.putInt(myPathSize);
            ByteBuffer myPathBuffer = ByteBuffer.wrap(myPathInBytes);
            ByteBuffer myAliasPathSizeBuffer = ByteBuffer.allocateDirect(4).putInt(myAliasPathSize);
            ByteBuffer myAliasPathBuffer = ByteBuffer.wrap(myAliasPathInBytes);

             */
            if (!connectionMsgReceived) {
                while (myConnectionMsg.hasRemaining()) {
                    numBytesRead = _socketChannel.read(myConnectionMsg);
                }

                //Flip the myConnectionMsg
                myConnectionMsg.flip();

                //Get the Msg Type
                msgType = myConnectionMsg.getInt();

                //Get the Channel Type
                myChannelType = myConnectionMsg.getInt();
                channelTypeString = ((myChannelType == CONTROL_CHANNEL_TYPE) ? "CONTROL CHANNEL" : " DATA CHANNEL ");

                //Get the Control Channel ID
                myControlChannelId = myConnectionMsg.getInt();
                logger.info(channelTypeString + " Control Channel ID = " + myControlChannelId);

                //Get the Data Channel ID
                myDataChannelId = myConnectionMsg.getInt();
                logger.info(channelTypeString + " Data Channel ID = " + myDataChannelId);

                //Get the Parallel Num
                myParallelNum = myConnectionMsg.getInt();
                logger.info(channelTypeString + " Parallel Number = " + myParallelNum);

                //Get the Concurrency Num
                myConcurrencyNum = myConnectionMsg.getInt();
                logger.info(channelTypeString + " Concurrency Number = " + myConcurrencyNum);
                logger.info(channelTypeString + " ************** THE POSITION OF THE PATH SIZE = " + myConnectionMsg.position());

                //Get the Path Size
                myPathSize = myConnectionMsg.getInt();
                logger.info(channelTypeString + " Path Size = " + myPathSize);

                //Flip the myConnectionMsg
                myConnectionMsg.flip();

                //Get the Path Buffer
                myPathBuffer = ByteBuffer.allocateDirect(myPathSize);

                //Read in the Network Path in IP Address Format
                //If the network path is: WS5,WS11,WS12,WS7 then ONLY THE
                //IP ADDRESSES OF WS11,WS12,WS7 are sent
                while (myPathBuffer.hasRemaining()) {
                    numBytesRead = _socketChannel.read(myPathBuffer);
                }
                //Flip the Buffer
                myPathBuffer.flip();
                //Note the StandardCharsets.US_ASCII.decode method increases myPathBuffer's position
                myFilePath = StandardCharsets.US_ASCII.decode(myPathBuffer).toString();
                logger.info(channelTypeString + " Network Path in IP Address Format = " + myFilePath + " AND the position of myPathBuffer = " + myPathBuffer.position());
                logger.info(channelTypeString + ", ********************FILE PATH BUFFER POSITION AFTER GETTING IP ADDRESS = " + myPathBuffer.position() );
                //Need to flip myPathBuffer
                myPathBuffer.flip();

                //System.err.printf("ProxyServerFrontEndServer: Path in IP Address Format = %s %n", myFilePath);

                //Read in the size of the Alias Path: WS5,WS11,WS12,WS7
                while (myAliasPathSizeBuffer.hasRemaining()) {
                    numBytesRead = _socketChannel.read(myAliasPathSizeBuffer);
                }
                //Flip the Buffer
                myAliasPathSizeBuffer.flip();
                myAliasPathSize = myAliasPathSizeBuffer.getInt();
                logger.info(channelTypeString + " Size and the Number of Characters in the Alias Path = " + myAliasPathSize);
                //Flip the Buffer
                myAliasPathSizeBuffer.flip();

                myAliasPathBuffer = ByteBuffer.allocateDirect(myAliasPathSize);

                //Read in the Alias Path: WS5,WS11,WS12,WS7
                while (myAliasPathBuffer.hasRemaining()) {
                    numBytesRead = _socketChannel.read(myAliasPathBuffer);
                }

                //Flip the Buffer
                myAliasPathBuffer.flip();
                myAliasFilePath = StandardCharsets.US_ASCII.decode(myAliasPathBuffer).toString();
                //System.err.printf("ProxyServerFrontEndServer: Alias Path = %s %n", myAliasFilePath);
                logger.info(channelTypeString + " ALIAS PATH = " + myAliasFilePath);
                //Flip the Buffer
                myAliasPathBuffer.flip();

                //Set Connection Msg Received to TRUE
                connectionMsgReceived = true;


                /////////////////////////////////
                //the path is a string of ip addresses and ports separated by a comma, it doesn't include the source node (the first node in the path)
                //if path is WS5,WS7,WS12 with the below ip address and port
                //192.168.0.2:4959.192.168.0.1:4959,192.168.1.2:4959
                //then only the ip addresses & ports of WS7, WS12 is sent, note this proxy server is WS7
                //So the path = 192.168.0.1:4959,192.168.1.2:4959
                //parse out the first ip address
                String[] tokens = myFilePath.split("[,]+");
                //System.err.printf("ProxyServer:ChannelRead: tokens[0] = %s %n", tokens[0]);
                //System.err.printf("ProxyServer:ChannelRead: tokens[1] = %s %n", tokens[1]);
                logger.info(channelTypeString + " tokens[0] = " + tokens[0]);
                logger.info(channelTypeString + " tokens[1] = " + tokens[1]);

                theNodeToForwardTo = tokens[1]; // = 192.168.1.2:4959
                //Separate the ip address from the port
                String[] ip_and_port = theNodeToForwardTo.split("[:]+");
                //System.err.printf("ProxyServer:ChannelRead: ip_and_port[0] = %s %n", ip_and_port[0]);
                //System.err.printf("ProxyServer:ChannelRead: ip_and_port[1] = %s %n", ip_and_port[1]);
                logger.info(channelTypeString + " ip_and_port[0] = " + ip_and_port[0]);
                logger.info(channelTypeString + " ip_and_port[1] = " + ip_and_port[1]);

                remoteHost = ip_and_port[0]; //"192.168.0.1"
                remotePort = new Integer(ip_and_port[1]).intValue(); //=4959
                //System.err.printf("ProxyServerFrontEndHandler:ChannelRead: Remote Host = %s and Remote Port = %d %n", remoteHost, remotePort);
                logger.info(channelTypeString + "Remote Host = " + remoteHost + ", Remote Port = " + remotePort);

                //logger.info("FileReceiverServer: ProcessConnectionMsg: READ IN THE PATH: " + thePath);
                //logger.info(" **************ProxyServer:ChannelRead:( ThreadId:" + threadId + ") FINISHED READING IN THE PATH " + thePath + " in.readableBytes = " + in.readableBytes() + "********************");
                /////////////////////////////////
                //Create Selector
                //this._selector = SelectorProvider.provider().openSelector();
                //Create Socket Channel and configure it
                this.backendSocketChannel = SocketChannel.open();
                this.backendSocketChannel.configureBlocking(false);
                this.backendSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 1024 * 1024 * 100); //Set Send Buffer Socket Option to 100MB
                this.backendSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 1024 * 100); //Set Send Buffer Socket Option to 100MB
                //this._socketChannel.setOption(SocketOption.TCP_NODELAY, true);

                // Connect the SocketChannel to the remote peer
                this.backendSocketChannel.connect(new InetSocketAddress(remoteHost, remotePort));
                while (!this.backendSocketChannel.finishConnect()){
                    //DO NOTHING
                }
                logger.info("THE PROXY SERVER FINISHED CONNECTING TO: " + remoteHost + ":" + remotePort);
                //myProxyServerBackendHandler = new ProxyServerBackendHandler(this._selector, backendSocketChannel, myProxyServerFrontendHandler, myChannelType );

                /*
                aDataChannel.connect(new InetSocketAddress(remoteHost,remotePort));
                while (!aDataChannel.finishConnect()){
                    //DO NOTHING
                }
                */

                // Register _socketChannel with _selector listening on OP_CONNECT events.
                // Callback: FileSenderHandler, selected when the SocketChannel tells the selector it is ready to finish connecting
                //SelectionKey selectionKey = this._socketChannel.register(this._selector, SelectionKey.OP_CONNECT);
                myProxyServerBackendHandler = new ProxyServerBackendHandler(this._selector, this.backendSocketChannel, this, myChannelType );
                SelectionKey selectionKey = this._socketChannel.register(this._selector, SelectionKey.OP_READ);
                selectionKey.attach(myProxyServerBackendHandler);
                //selectionKey.attach(new ProxyServerBackendHandler(this._selector, this.backendSocketChannel, this, myChannelType ));
                selectionKey.selector().wakeup();




                //the path is a string of ip addresses and ports separated by a comma, it doesn't include the source node (the first node in the path)
                //if path is WS5,WS7,WS12 with the below ip address and port
                //192.168.0.2:4959.192.168.0.1:4959,192.168.1.2:4959
                //then only the ip addresses & ports of WS7, WS12 is sent, note this proxy server is WS7
                //So the path = 192.168.0.1:4959,192.168.1.2:4959
                //Now when WS7 send the IP Path Addresses to WS12, it doesn't send it's own IP Address just
                //that of WS12. So WS12 will receive it's own IP Address.
                int anIndex = myFilePath.indexOf(',');

                String newPathToSend = null; //since this is a proxy server, the path will always consists of at least one path - the receiver path
                if (anIndex > 0) { //If this is the dest node, then the file path is attached, remove the file path from node
                    newPathToSend = myFilePath.substring(anIndex + 1, myFilePath.length());
                    logger.info(channelTypeString + " SendConnectionMsg: The old Path = " + myFilePath + ", the New Path to Send = " + newPathToSend);
                    //System.out.println("TransferContext: getNodeToForwardTo: node to forward to had a file path, it is now removed and the node is: " + theNodeToForwardTo);
                }

                if (newPathToSend != null) {
                    //Get the New File Path In Bytes
                    byte[] newPathToSendInBytes = newPathToSend.getBytes();
                    //Get the length of theNewFilePath
                    int newPathToSendInBytesSize = newPathToSendInBytes.length;
                    logger.info(channelTypeString + " Size/length (Number of characters in the New Path to Send = " + newPathToSendInBytesSize +") " );
                    //PLACE THE NEW PATH SIZE IN THE CONNECTION MSG AT POSTION 24 OUT OF 28
                    myConnectionMsg.putInt(24,newPathToSendInBytesSize);

                    //Copy the New File Path length to the ByteBuffer
                    //ByteBuffer newPathToSendInBytesSizeBuf = ByteBuffer.allocateDirect(INT_SIZE);
                    //newPathToSendInBytesSizeBuf.putInt(newPathToSendInBytesSize);
                    //Flip the NewPathSizeBuffer
                    //newPathToSendInBytesSizeBuf.flip();

                    ByteBuffer newPathToSendInBytesBuf = ByteBuffer.allocate(newPathToSendInBytesSize);
                    newPathToSendInBytesBuf.put(newPathToSendInBytes);
                    //Flip the newPathToSendInBytesBuf
                    newPathToSendInBytesBuf.flip();

                    //Send Connection Msg
                    //Send the Connection Msg
                    int numBytesWrote = 0;
                    int totalBytesWrote = 0;
                    logger.info(channelTypeString + " ABOUT TO SEND CONNECTION MSG, POSITION = " + myConnectionMsg.position());
                    while (myConnectionMsg.hasRemaining()){
                        numBytesWrote = backendSocketChannel.write(myConnectionMsg);
                        if (numBytesWrote > 0) {
                            totalBytesWrote += numBytesWrote;
                        }
                    }
                    logger.info(channelTypeString + " SENT CONNECTION MSG, TOTAL BYTES SENT = " + totalBytesWrote);

                    //THANK YOU GOD FOR SHOWING ME THIS! THE SIZE OF THE PATH IS IN THE CONNECTION MSG


                    /*

                    numBytesWrote = 0;
                    totalBytesWrote = 0;
                    logger.info(channelTypeString + " ABOUT TO SEND THE SIZE OF THE NEW PATH, POSITION = " + newPathToSendInBytesSizeBuf.position());
                    //Send the Size of the New Network Path
                    while (newPathToSendInBytesSizeBuf.hasRemaining()){
                        numBytesWrote = backendSocketChannel.write(newPathToSendInBytesSizeBuf);
                        if (numBytesWrote > 0) {
                            totalBytesWrote += numBytesWrote;
                        }
                    }
                    logger.info(channelTypeString + " SENT THE SIZE OF THE NEW PATH, TOTAL BYTES SENT = " + totalBytesWrote);
                    */

                    numBytesWrote = 0;
                    totalBytesWrote = 0;
                    logger.info(channelTypeString + " ABOUT TO SEND THE NEW PATH, POSITION = " + newPathToSendInBytesBuf.position());
                    //Send the New Network Path
                    while (newPathToSendInBytesBuf.hasRemaining()){
                        numBytesWrote = backendSocketChannel.write(newPathToSendInBytesBuf);
                        if (numBytesWrote > 0) {
                            totalBytesWrote += numBytesWrote;
                        }
                    }
                    logger.info(channelTypeString + " SENT THE NEW PATH, TOTAL BYTES SENT = " + totalBytesWrote);

                    numBytesWrote = 0;
                    totalBytesWrote = 0;
                    logger.info(channelTypeString + " ABOUT TO SEND THE SIZE OF THE ALIAS PATH, POSITION BEFORE FLIP = " + myAliasPathSizeBuffer.position());
                    //Flip the Alias Path Size Buffer
                    //myAliasPathSizeBuffer.flip();
                    logger.info(channelTypeString + " ABOUT TO SEND THE SIZE OF THE ALIAS PATH, POSITION AFTER FLIP = " + myAliasPathSizeBuffer.position());
                    //Send the AliasPathSize
                    while (myAliasPathSizeBuffer.hasRemaining()){
                        numBytesWrote = backendSocketChannel.write(myAliasPathSizeBuffer);
                        if (numBytesWrote > 0) {
                            totalBytesWrote += numBytesWrote;
                        }
                    }
                    logger.info(channelTypeString + " SENT THE SIZE OF THE ALIAS PATH, TOTAL BYTES SENT = " + totalBytesWrote);


                    numBytesWrote = 0;
                    totalBytesWrote = 0;
                    logger.info(channelTypeString + " ABOUT TO SEND THE ALIAS PATH, POSITION BEFORE FLIP = " + myAliasPathBuffer.position());
                    //Flip the Alias Path Buffer
                    //myAliasPathBuffer.flip();
                    logger.info(channelTypeString + " ABOUT TO SEND THE ALIAS PATH, POSITION AFTER FLIP = " + myAliasPathBuffer.position());
                    //Send the AliasPath
                    while (myAliasPathBuffer.hasRemaining()){
                        numBytesWrote = backendSocketChannel.write(myAliasPathBuffer);
                    }
                    logger.info(channelTypeString + " SENT THE ALIAS PATH, TOTAL BYTES SENT = " + totalBytesWrote);
                }




               //ProxyServerBackendHandler(Selector aSelector, SocketChannel aBackendSocketChannel, ProxyServerFrontendHandler aProxyServerFrontEndHandler, int aChannelType)
                //selectionKey.attach(new Connector(selectionKey));
            } else {
                //START FORWARDING THE DATA ONLY IF IT'S CONNECTED
                if (backendSocketChannelConnected) {
                    logger.info("*************FRONT END PRXY SERVER: BACKEND CHANNEL IS CONNECTED");
                    //Read data into ByteBuffer and Forward to the next server
                    //If BackEndSocketChannel does not have write lock, read data into buffer
                    if (!getBackEndSocketChannelWriteLock()) {
                        //Clear dataBuffer before I Read into it: Position set to 0 and limit set to capacity
                        dataBuffer.clear();
                        numBytesRead = _socketChannel.read(dataBuffer);
                        logger.info(" **************NUM FILE BYTES READ = " + numBytesRead);
                        if (numBytesRead > 0) {
                            //Flip the DataBuffer, set limit = position, set position to 0
                            dataBuffer.flip();
                            //Set the Write lock to true
                            this.setBackEndSocketChannelWriteLock(true);
                            logger.info("************FRONT END PROXY SERVER SET SOCKET CHANNEL WRITE LOCK TO TRUE");
                            //To avoid delay in writing
                            //if possible send whatever data I can from the backEndChannel here
                            numBytesWrote = backendSocketChannel.write(dataBuffer);
                            logger.info("***********************FRONT END PROXY SERVER: USING BACKEND SOCKET CHANNEL, FORWARDED " + numBytesWrote + " Bytes to the RECEIVER ");

                            if (dataBuffer.hasRemaining()) {
                                //Set the BackEndHandler Selection Key's Interest Ops to write, so the BackEndHandler can send remaining data (if any) to the next server or proxy server
                                myProxyServerBackendHandler.setSelectionKeyInterestOps(SelectionKey.OP_WRITE);
                                //Wake up the Selector
                                _selector.wakeup();
                                logger.info("*************FRONT END PROXY SERVER SET THE BACKEND SERVER'S SELECTION KEY TO OP_WRITE, BECAUSE THERE IS MORE DATA TO SEND WITHIN THIS FRAGMENT ");
                            } else {
                                //All data in the dataBuffer was sent so I can release the lock
                                //Set the Write lock to FALSE
                                this.setBackEndSocketChannelWriteLock(false);
                            }
                        }
                    }
                }//end backendSocketChannelConnected
            }//end else

            //READ IN MSG TYPE
            //READ IN CONNECTION MSG TYPE ELSE ERROR
            //READ IN NETWORK PATH LENGTH - THE LENGTH OF THE PATH IN IP ADDRESS FORMAT
            //READ IN NETWORK PATH - THE PATH IN IP FORMAT
            //---//if path is WS5,WS7,WS12 with the below ip address and port
            //---192.168.0.2:4959.192.168.0.1:4959,192.168.1.2:4959
            //---WHEN I SEND THE NETWORK PATH IN IP FORMAT, DO I SEND THE SOURCE NODE IP ADDRESS AS WELL?
            //---I SEND IT WITHOUT THE SRC NODE, BECAUSE I SEND THE ALIAS PATH

            /*
            System.err.printf("FileReceiverHandler: Read Call Number = %d %n", _readCallNumber);
            if (!_fileHeaderReadIn) {
                //Read in the file header
                while (_fileHeaderBuffer.remaining() > 0) {
                    /////////////////////////////////////////////////
                    _bytesRead = _socketChannel.read(_fileHeaderBuffer);
                    //////////////////////////////////////////////////
                    System.err.printf("FileReceiverHandler: Reading File Header bytes, read % d bytes %n", _bytesRead);
                }

                _timeStartedTransfer = System.currentTimeMillis();


                if (_fileHeaderBuffer.remaining() == 0) {
                    _currentOffset = _fileHeaderBuffer.getLong(0);
                    System.err.printf("FileReceiverHandler: offset = %d %n", _currentOffset);
                    _fragmentLength = _fileHeaderBuffer.getLong(8);
                    System.err.printf("FileReceiverHandler: Fragment Length = %d %n", _fragmentLength);
                    _remainingFileFragmentLength = _fragmentLength;
                    _destFilePathSize = _fileHeaderBuffer.getInt(16);
                    System.err.printf("FileReceiverHandler: Dest File Path Size  = %d %n", _destFilePathSize);
                    //SET THE DESTINATION FILE PATH BUFFER
                    _myFilePathBuffer = ByteBuffer.allocate(_destFilePathSize);

                    //Flip Buffer
                    _fileHeaderBuffer.flip();
                    //Set Flag indicating read in File Header
                    _fileHeaderReadIn = true;

                    //Increment _totalBytesTransferred
                    _totalBytesTransferred +=_fileHeaderBuffer.capacity();

                }
            }
            if (!_destFilePathReadIn) {

                //Read in the destFileNamePath
                while (_myFilePathBuffer.remaining() > 0) {
                    /////////////////////////////////////////////////
                    _bytesRead = _socketChannel.read(_myFilePathBuffer);
                    //////////////////////////////////////////////////
                    System.err.printf("FileReceiverHandler: Reading Destination File Path, read %d bytes %n", _bytesRead);
                }
                if (_myFilePathBuffer.remaining() == 0) {

                    _myFilePathBuffer.flip();
                    _totalBytesTransferred += _myFilePathBuffer.capacity();

                    //_thePath = _myFilePathBuffer.toString(Charset.forName("US-ASCII"));
                    //https://stackoverflow.com/questions/17354891/java-bytebuffer-to-string
                    //_thePath = StandardCharsets.UTF_8.decode(_myFilePathBuffer).toString();
                    _theDestFilePath = StandardCharsets.US_ASCII.decode(_myFilePathBuffer).toString();
                    System.err.printf("FileReceiverHandler: Read in Destination File Path, destination file path =  %s bytes %n", _theDestFilePath);
                }
                //set file channel
                _myFile = new File(_theDestFilePath);
                //_myFile.createNewFile();
                _fc = new RandomAccessFile(_myFile, "rw").getChannel();
                _myFile.createNewFile();
                _remainingFileFragmentLength = _fragmentLength;

                if (_remainingFileFragmentLength < _amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                    _amountToTransfer = _remainingFileFragmentLength;
                }

                _destFilePathReadIn = true;
            }if (!_fileFragmentTransferComplete) {
                //USE Zero Copy - to read data directly from the socket to the file, reading 100MB at a time if permitted
                _theBytesTransferredFromSocket = _fc.transferFrom(_socketChannel, _currentOffset, _amountToTransfer);
                //Increment Total Bytes Transferred
                if (_theBytesTransferredFromSocket > 0) {
                    _totalBytesTransferred += _theBytesTransferredFromSocket;
                }
                System.err.printf("FileReceiverHandler: Number of bytes transferred from socket to file = %d, starting at current offset %d, amount wanted to be transferred %d %n", _theBytesTransferredFromSocket, _currentOffset, _amountToTransfer);
                if (_theBytesTransferredFromSocket > 0) {
                    //decrement the remainingFileFragmentLength
                    _remainingFileFragmentLength -= _theBytesTransferredFromSocket;
                    //Increment current File Position
                    _currentOffset += _theBytesTransferredFromSocket;
                    if (_remainingFileFragmentLength < _amountToTransfer) { //Assuming amount to transfer = 100MB  = 100 * 1024 * 1024
                        _amountToTransfer = _remainingFileFragmentLength;
                    }
                }
                if (_remainingFileFragmentLength <= 0){
                    _timeEndedTransfer = System.currentTimeMillis();
                    _fileFragmentTransferComplete = true;
                    _throughput = (((_totalBytesTransferred * 8) / (_timeEndedTransfer - _timeStartedTransfer)) * 1000) / 1000000;
                    String _units = "Mb/s";
                    System.err.printf("%n FileReceiverHandler: Throughput = %f, %s %n", _throughput, _units );
                    //Done reading File
                    System.err.printf(" %n FileReceiverHandler: Done Reading in FileFragment");
                }

            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
    /*
    Writes back to the sender or the previous proxy server
     */

    public void handleWrite() {
        try {
            //What happens if frontend read agains before the write lock is set
            //It will just read into the DataBuffer, but then when the write lock is set
            //I can have the FrontEnd set the BackEnd write Lock and then this method will unlock it
            //Once the data is sent.

            //Tell the ProxyServerBackEnd Handler that I am about to write out data
            //Check to see if the BackEndHandler set the write lock
            if (myProxyServerBackendHandler.getFrontEndSocketChannelWriteLock()){
                //First Check to see if this is a new write or a continuation of a previous write
                if (ackBuffer == null) {
                    //This is a new write since myDataBuffer is null, so get the ackBuffer
                    ackBuffer = myProxyServerBackendHandler.getAckBuffer();
                }
                //If I did get a new copy of the ByteBuf, make sure it's not null
                if (ackBuffer != null) {
                    //Ensure there is data to Send
                    if (ackBuffer.hasRemaining()) {
                        numBytesSent = _socketChannel.write(ackBuffer);
                        //If there is no more data to write, release the write lock
                        if (!ackBuffer.hasRemaining()) {
                            //Release the WriteLock, Set the BackEnd Handler writeLock to FALSE
                            myProxyServerBackendHandler.setFrontEndSocketChannelWriteLock(false);
                            ackBuffer = null;
                            //Set the Selection Key interest op for this socketChannel to Read to prevent the run loop
                            //from selecting the SelectionKey to Write
                            this.setSelectionKeyInterestOps(SelectionKey.OP_READ);
                            this._selector.wakeup();
                        }
                    }
                }
            }
        }catch(Exception e){
            System.err.printf("ProxyServerBackendHandler: handleWrite Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }

    synchronized void write() throws IOException {
        //Write buffer to socket
        //Get byteBuffer from ProxyBackEndBuffer to Write
        //ProxyServerBackendHandler myProxyServerBackEndHandler = this.getProxyServerBackEndHandler();
        ackBuffer = myProxyServerBackendHandler.getAckBuffer();
        //Assuming the AckBuff is already flipped, get write lock on the ack Buff, same as I did in the front end, just repeat the same process
        if (ackBuffer.hasRemaining()) {
            numBytesWrote = _socketChannel.write(ackBuffer);
        }


    }
    /*
   Sets this Selection key's interest set to the given value.
 */
    public synchronized void setSelectionKeyInterestOps(int ops) {
        try {
            _selectionKey.interestOps(ops);
        }catch(Exception e){
            System.err.printf("ProxyServerBackendHandler: setSelectionKeyInterestOps Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized SocketChannel getFrontEndSocketChannel(){
        try {
              return _socketChannel;
        }catch(Exception e){
            System.err.printf("ProxyServerFrontendHandler: getFrontEndSocketChannel Error: %s %n", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /*
       Sets the backEndSocketChannelWriteLock
     */
    public synchronized void setBackEndSocketChannelWriteLock(boolean aVal) {
        try {
            backEndSocketChannelWriteLock = aVal;
        }catch(Exception e){
            System.err.printf("ProxyServerFrontEndHandler: setBackEndSocketChannelWriteLock Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }

    /*
       Sets the backEndSocketChannelWriteLock
     */
    public synchronized boolean getBackEndSocketChannelWriteLock() {
        try {
            return backEndSocketChannelWriteLock;
        }catch(Exception e){
            System.err.printf("ProxyServerFrontEndHandler: getBackEndSocketChannelWriteLock Error: %s %n", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /*
      Sets the backEndSocketChannelWriteLock
    */
    public synchronized ProxyServerFrontendHandler getProxyServerFrontendHandler() {
            return myProxyServerFrontendHandler;
            //return this;
    }

    //setProxyServerBackendHandler
    public synchronized void setProxyServerBackendHandler(ProxyServerBackendHandler aProxyServerBackendHandler) {
        try {
            myProxyServerBackendHandler = aProxyServerBackendHandler;
        }catch(Exception e){
            System.err.printf("ProxyServerFrontEndHandler: setProxyServerBackEndHandler Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized ByteBuffer getDataBuffer() {
        try {
            return dataBuffer;
        }catch(Exception e){
            System.err.printf("ProxyServerFrontEndHandler: getProxyServerBackEndHandler Error: %s %n", e.getMessage());
            e.printStackTrace();
            return null;
        }
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
                read();
            }
            else if (_selectionKey.isWritable()) {
                handleWrite();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    } //End Run



}
