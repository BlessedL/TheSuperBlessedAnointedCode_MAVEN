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

public class ProxyServerBackendHandler implements Runnable {

    public final int CONTROL_CHANNEL_TYPE = 0;
    public final int DATA_CHANNEL_TYPE = 1;
    public final int ACK_BUFFER_SIZE = 100*1024; //
    private Logger logger;

   private Selector myBackendSelector;
   private SocketChannel myBackendSocketChannel;
   private SocketChannel myFrontendSocketChannel;
   private boolean frontEndSocketChannelWriteLock;
   private ProxyServerFrontendHandler myProxyServerFrontEndHandler;
   private SelectionKey myBackendSelectionKey;
   private ByteBuffer myDataBuffer, readBuffer, ackBuffer;
   private int numBytesSent, numBytesRead, numBytesWrote;

   int myChannelType; //Channel Type - A DATA CHANNEL or A CONTROL CHANNEL

    //new ProxyServerBackendHandler(_selector, backendSocketChannel, this );
    public ProxyServerBackendHandler(Selector aSelector, SocketChannel aBackendSocketChannel, ProxyServerFrontendHandler aProxyServerFrontEndHandler, int aChannelType) throws IOException {
        myBackendSelector = aSelector;
        myBackendSocketChannel = aBackendSocketChannel;
        myProxyServerFrontEndHandler = aProxyServerFrontEndHandler;
        myChannelType = aChannelType;
        if (myChannelType == CONTROL_CHANNEL_TYPE){
            ackBuffer = ByteBuffer.allocateDirect(ACK_BUFFER_SIZE);
        } else{
            //This is a Data Channel, Data Channels will never receive acknowledgement
            ackBuffer = null;
        }
        frontEndSocketChannelWriteLock = false;
        logger = Logger.getLogger(ProxyServerBackendHandler.class.getName());

        // Register _socketChannel with _selector and let the _socketChannel tell the _selector it wants to write a message
        // Callback: Handler, selected when the connection is established, Writing to this backend socket channel will be handled by the
        // ProxyServerFrontEndHandler
        myBackendSelectionKey = myBackendSocketChannel.register(myBackendSelector, SelectionKey.OP_READ);
        myBackendSelectionKey.attach(this); //When attaching this Handler, are the states updated or the same
        myProxyServerFrontEndHandler.setProxyServerBackendHandler(this);
        myFrontendSocketChannel = myProxyServerFrontEndHandler.getFrontEndSocketChannel();
        //myProxyServerFrontEndHandler.setBackendSocketChannel(myBackendSocketChannel);
        //LAR: Why are we waking up Select again?
        //LAR: What thread is this waking up the selector?
        //LAR: How do we know the Selector is sleep?
        myBackendSelector.wakeup(); // let blocking select() return
    }

    public void handleRead() {
        try {
            if (myChannelType == CONTROL_CHANNEL_TYPE){
                logger.info("*****BACKEND PROXY SERVER, ChannelType == CONTROL_CHANNEL_TYPE");
                {
                    //START FORWARDING THE DATA THIS BACKEND CHANNEL IS CONNECTED THE FILE SENDER OR NEXT PROXY SERVER
                    //IF IT WASN'T WE WOULDN'T BE IN THIS METHOD
                        //Read data into ByteBuffer and Forward to the next server
                        //If BackEndSocketChannel does not have write lock, read data into buffer
                        if (!getFrontEndSocketChannelWriteLock()) {
                            logger.info("GET FRONT END SOCKET CHANNEL WRITE LOCK == FALSE");
                            //Clear ackBuffer before I Read into it: Position set to 0 and limit set to capacity
                            ackBuffer.clear();
                            numBytesRead = myBackendSocketChannel.read(ackBuffer);
                            logger.info("************READ ACK BYTES, NUM ACK BYTES READ = " + numBytesRead);
                            if (numBytesRead > 0) {
                                //Flip the AckBuffer, set limit = position, set position to 0
                                ackBuffer.flip();
                                //Set the Write lock to true
                                this.setFrontEndSocketChannelWriteLock(true);
                                //To avoid delay in writing
                                //if possible send whatever data I can from the backEndChannel here
                                numBytesWrote = myFrontendSocketChannel.write(ackBuffer);
                                logger.info("**************ProxyServerBackendHandler: Used the frontEndSocketChannel to write to the SENDER: Number of Bytes Wrote = " + numBytesWrote);

                                if (ackBuffer.hasRemaining()) {
                                    //Set the BackEndHandler Selection Key's Interest Ops to write, so the BackEndHandler can send remaining data (if any) to the next server or proxy server
                                    myProxyServerFrontEndHandler.setSelectionKeyInterestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                                    logger.info("******************BackendProxyServer is making the FrontEndProxyServer register for both READ AND WRITE EVENTS");
                                    //Wake up the Selector
                                    myBackendSelector.wakeup();
                                } else {
                                    //All data in the ackBuffer was sent so I can release the lock
                                    //Set the Write lock to FALSE
                                    this.setFrontEndSocketChannelWriteLock(false);
                                }

                            }
                        }
                }

            }else {
                logger.info("*****BACKEND PROXY SERVER, CHANNEL TYPE = DATA_CHANNEL_TYPE");
            }

            //If This is a Data Channel than this Method will never be called
            //If This is a Control Channel then acknowledgements will be sent through here
            //Read Data into Byte Buff

        }catch(Exception e){
           System.err.printf("ProxyServerBackendHandler: handleRead Error: %s %n", e.getMessage());
           e.printStackTrace();
        }
    }

    /*
     This method sends data to the next proxy server or File Receiver
     Assumptions: This method assumes the ProxyServerFrontEndHandler sets the ProxyServerBackEndHandler write lock
    */
    public void handleWrite() {
        try {
            //What happens if frontend read agains before the write lock is set
            //It will just read into the DataBuffer, but then when the write lock is set
            //I can have the FrontEnd set the BackEnd write Lock and then this method will unlock it
            //Once the data is sent.

            //Tell the ProxyServerFrontEnd Handler that I am about to write out data
            //Check to see if the FrontEndHandler set the write lock
            if (myProxyServerFrontEndHandler.getBackEndSocketChannelWriteLock()){
                //First Check to see if this is a new write or a continuation of a previous write
                if (myDataBuffer == null) {
                    //This is a new write since myDataBuffer is null, so get the dataBuffer
                    myDataBuffer = myProxyServerFrontEndHandler.getDataBuffer();
                }
                //If I did get a new copy of the ByteBuf, make sure it's not null
                if (myDataBuffer != null) {
                    //Ensure there is data to Send
                    if (myDataBuffer.hasRemaining()) {
                        numBytesSent = myBackendSocketChannel.write(myDataBuffer);
                        //If there is no more data to write, release the write lock
                        if (!myDataBuffer.hasRemaining()) {
                            //Release the WriteLock, Set the BackEnd Handler writeLock to FALSE
                            myProxyServerFrontEndHandler.setBackEndSocketChannelWriteLock(false);
                            myDataBuffer = null;
                            //Set the Selection Key interest op for this socketChannel to Read to prevent the run loop
                            //from selecting the SelectionKey to Write
                            this.setSelectionKeyInterestOps(SelectionKey.OP_READ);
                            this.myBackendSelector.wakeup();
                        }
                    }
                }
            }
        }catch(Exception e){
            System.err.printf("ProxyServerBackendHandler: handleWrite Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }


    /*
       Sets this Selection key's interest set to the given value.
     */
    public void setSelectionKeyInterestOps(int ops) {
        try {
            myBackendSelectionKey.interestOps(ops);
        }catch(Exception e){
            System.err.printf("ProxyServerBackendHandler: setSelectionKeyInterestOps Error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    }

    /*
    Retrieves this Selection key's interest set.
     */
    public int getSelectionKeyInterestOps() {
        try {
            return myBackendSelectionKey.interestOps();
        }catch(Exception e){
            System.err.printf("ProxyServerBackendHandler: getSelectionKeyInterestOps Error: %s %n", e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public synchronized boolean getFrontEndSocketChannelWriteLock() {
        try {
            return frontEndSocketChannelWriteLock;
        }catch(Exception e){
            System.err.printf("ProxyServerBackEndHandler: getFrontEndSocketChannelWriteLock Error: %s %n", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized ByteBuffer getAckBuffer() {
        try {
            return ackBuffer;
        }catch(Exception e){
            System.err.printf("ProxyServerBackEndHandler: getFrontEndSocketChannelWriteLock Error: %s %n", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public synchronized void setFrontEndSocketChannelWriteLock(boolean aVal) {
        try {
            frontEndSocketChannelWriteLock = aVal;
        }catch(Exception e){
            System.err.printf("ProxyServerBackEndHandler: setFrontEndSocketChannelWriteLock Error: %s %n", e.getMessage());
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
            if (myBackendSelectionKey.isReadable()) {
                handleRead();
            }
            else if (myBackendSelectionKey.isWritable()) {
                handleWrite();
            }
        }
        catch (Exception e) {
            System.err.printf("ProxyServerBackendHandler: run error: %s %n", e.getMessage());
            e.printStackTrace();
        }
    } //End Run
}
