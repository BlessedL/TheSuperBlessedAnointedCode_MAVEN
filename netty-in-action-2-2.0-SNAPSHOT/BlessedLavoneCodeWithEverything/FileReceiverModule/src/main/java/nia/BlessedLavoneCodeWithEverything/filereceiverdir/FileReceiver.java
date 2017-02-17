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
package nia.BlessedLavoneCodeWithEverything.filereceiverdir;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.*;
import java.util.logging.Logger;

public final class FileReceiver {

    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "4959"));
    private final static Logger logger = Logger.getLogger(FileReceiver.class.getName());
    static final int CONTROL_CHANNEL_TYPE = 0;
    static final int DATA_CHANNEL_TYPE = 1;
    static int registerChannelCtxCounter = 0;
    //myRegisteredChannelsCtx = a HashMaps of NetworkPaths where each Network Path Entry has a Hashmap of Control Channels associated with it
    //myRegisteredChannelsCtx = HashMap<PathName, HashMap<ControlChannel ID, Control Channel Object>>
    public static HashMap<String,HashMap<String,FileReceiver.ControlChannelObject>> myRegisteredChannelsCtx = new HashMap<String,HashMap<String,FileReceiver.ControlChannelObject>>();
    public static HashMap<String,String> myAliasPathAndConcurrencyNum = new HashMap<String, String>();

    public static class ControlChannelObject{
        int myControlChannelId;
        ChannelHandlerContext myControlChannelCtx;
        List<FileReceiver.DataChannelObject> myDataChannelList;
        int parallelDataChannelNum;
        boolean connectAckMsgReceived; //For the File Sender
        boolean connectMsgReceived; //For the File Receiver

        public final int CONTROL_CHANNEL_TYPE = 0;
        public final int DATA_CHANNEL_TYPE = 1;

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, List<FileReceiver.DataChannelObject> aDataChannelList, int aParallelNum ){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = aDataChannelList;
            connectAckMsgReceived = false;
            parallelDataChannelNum = aParallelNum;
        }

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, int aParallelNum){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
            connectAckMsgReceived = false;
            //myDataChannelList = new LinkedList<DataChannelObject>();
            parallelDataChannelNum = aParallelNum;
        }

        public ControlChannelObject(int aControlChannelId, int aDataChannelId, int aChannelType, ChannelHandlerContext aChannelCtx, int aParallelNum){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = null;
            parallelDataChannelNum = aParallelNum;
            //What does the connect Ack Msg Received indicate, does it indicate the control channel successfully connected or does it indicate that both the control channel and its associated parallel data channels successfully connected
            connectAckMsgReceived = false;
            if (aChannelType == CONTROL_CHANNEL_TYPE) {
                myControlChannelCtx = aChannelCtx;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
                }
            }
            else { //This is a data Channel
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
                    myDataChannelList.add(new FileReceiver.DataChannelObject(aDataChannelId, aChannelCtx));
                }
                else {
                    //Add the Data Channel to the List
                    myDataChannelList.add(new FileReceiver.DataChannelObject(aDataChannelId, aChannelCtx));
                }
            }
            //myDataChannelList = new LinkedList<DataChannelObject>();

        }

        public boolean getConnectMsgReceivedFlag(){
            return connectMsgReceived;
        }

        //setConnectMsgAck
        public void setConnectMsgReceivedFlag(boolean aVal){
            connectMsgReceived = aVal;
        }

        public boolean getConnectAckMsgReceivedFlag(){
            return connectAckMsgReceived;
        }

        //setConnectMsgAck
        public void setConnectAckMsgReceivedFlag(boolean aVal){
            connectAckMsgReceived = aVal;
        }

        public void addDataChannelObject(FileReceiver.DataChannelObject aDataChannelObject){
        /*
        if (myDataChannelList == null){
            myDataChannelList = new LinkedList<DataChannelObject>();
        }
        */
            myDataChannelList.add(aDataChannelObject);
        }

        public int getControlChannelId(){
            return myControlChannelId;
        }

        public void setControlChannelId(int aControlChannelId){
            myControlChannelId = aControlChannelId;
        }

        public void setControlChannel(ChannelHandlerContext aControlChannelCtx){
            myControlChannelCtx = aControlChannelCtx;
        }

        public ChannelHandlerContext getControlChannel(){
            return myControlChannelCtx;
        }

        public List<FileReceiver.DataChannelObject> getDataChannelObjectList(){
            return myDataChannelList;
        }

        public void setDataChannelObjectList(List<FileReceiver.DataChannelObject> aDataChannelList){

            myDataChannelList = aDataChannelList;
        }

        public int getParallelDataChannelNum(){
            return parallelDataChannelNum;
        }

        public void setParallelDataChannelNum(int aVal){
            parallelDataChannelNum = aVal;
        }

        public String controlChannelObjectToString(){
            try {
                //Add the Control Channel Id to the String
                String StringToPrint = "--Control Channel Id: " + this.getControlChannelId();
                List<FileReceiver.DataChannelObject> theDataChannelObjectList = this.getDataChannelObjectList();
                if (theDataChannelObjectList != null) {
                    if (theDataChannelObjectList.size() > 0) {
                        //Iterate through the Data Channel Object
                        Iterator<FileReceiver.DataChannelObject> dataChannelObjectIterator = theDataChannelObjectList.iterator();
                        while (dataChannelObjectIterator.hasNext()) {
                            FileReceiver.DataChannelObject theDataChannelObject = dataChannelObjectIterator.next();
                            //Add the Data Channel to the string
                            StringToPrint = StringToPrint + "\n" + "----Data Channel Id: " + theDataChannelObject.getDataChannelId();
                        }//end while
                    }//End (theDataChannelObjectList.size() > 0)
                }//End (theDataChannelObjectList != null)
                return StringToPrint;
            }catch(Exception e){
                System.err.printf("ControlChannelObject: controlChannelObjectToString  Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }// End controlChannelObjectToString

    }

    public static class DataChannelObject{
        int myDataChannelId;
        ChannelHandlerContext myDataChannelCtx;
        boolean connectMsgReceived;

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = true;
        }

        public int getDataChannelId(){
            return myDataChannelId;
        }

        public void setDataChannelId(int aDataChannelId){
            myDataChannelId = aDataChannelId;
        }

        public ChannelHandlerContext getDataChannel(){
            return myDataChannelCtx;
        }

        public void setDataChannel(ChannelHandlerContext aDataChannelCtx){
            myDataChannelCtx = aDataChannelCtx;
        }

        public boolean getConnectMsgReceivedFlag(){
            return connectMsgReceived;
        }

        public void setConnectMsgReceivedFlag(boolean aVal){
            connectMsgReceived = aVal;
        }
    }


    /*
       Method Description: Registers the Channel Handler Context of either a data channel or a control channel and also checks to
                           see if all data channels are registered
       Returns: Returns the Control Channel Handler Context if all data channels are connected, else it returns Null;
     */
    public static synchronized ChannelHandlerContext registerChannelCtx(String aPathAliasName, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, int aParallelNum, int aConcurrencyNum){
        try {
            registerChannelCtxCounter++;
            if (aChannelType == CONTROL_CHANNEL_TYPE){
                logger.info("**************--------- REGISTERING CONTROL CHANNEL " + aControlChannelId + " ******************************----------------" );
            }else {
                logger.info("*******************--------- REGISTERING DATA CHANNEL " +  aDataChannelId + " BELONGING TO CONTROL CHANNEL " + aControlChannelId + " ***********************-----------------");
            }

            ChannelHandlerContext returnCtx = null;
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                if (!myRegisteredChannelsCtx.containsKey(aPathAliasName)) {
                    //ControlChannelId ControlChannelObject
                    myRegisteredChannelsCtx.put(aPathAliasName, new HashMap<String, FileReceiver.ControlChannelObject>());
                }
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                HashMap<String, FileReceiver.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                //return myHashMap2;

                //If the Control Channel Object Map doesn't contain the specified Channel ID
                if (!myControlChannelObjectMap.containsKey( String.valueOf(aControlChannelId) ) ) {

                    //If the ControlObject Doesn't exist - Create the ChannelControlObject either with a ControlChannelCTX if a control channel is registering or with a DataChannelCTX if a DataChannel is registering
                    myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileReceiver.ControlChannelObject(aControlChannelId, aDataChannelId, aChannelType, aChannelCtx, aParallelNum));
                    ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    //Print the Control Object String
                    String aControlObjectString = myControlChannelObject.controlChannelObjectToString();
                    System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                    logger.info("FileReceiver:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        myControlChannelObject.setConnectMsgReceivedFlag(true);
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING CONTROL CHANNEL ID: %d ***********************\n\n",aControlChannelId  );
                    }
                    else {
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING DATA CHANNEL ID: %d FOR CONTROL CHANNEL ID: %d ***********************\n\n",aDataChannelId, aControlChannelId );
                    }
                    logger.info("FileReceiver:registerChannelCtx: Size of Data Channel Object List = " + myControlChannelObject.getDataChannelObjectList().size() + " Number of expected parallel Data channels for this control channel object = " + myControlChannelObject.getParallelDataChannelNum() );
                    //check to see if all data channels are registered
                    System.err.printf("\n************* myControlChannelObject.getDataChannelObjectList().size( %d ) >= myControlChannelObject.getParallelDataChannelNum( %d ) ***********************\n\n",myControlChannelObject.getDataChannelObjectList().size(),myControlChannelObject.getParallelDataChannelNum() );
                    if (myControlChannelObject.getDataChannelObjectList().size() >= myControlChannelObject.getParallelDataChannelNum()){
                        //Check to make sure the Control Channel is registered, if it is return the ContextChannelHandler
                        if (myControlChannelObject.getControlChannel() != null) {
                            returnCtx = myControlChannelObject.getControlChannel();
                        }
                    }
                }//End ControlObject Doesn't exist -
                else {
                    //a Control Channel Object exist with this Control Channel ID already
                    FileReceiver.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        //Add the Control ChannelCTX to the Control Object
                        myControlChannelObject.setControlChannel(aChannelCtx);
                        myControlChannelObject.setConnectMsgReceivedFlag(true);
                        //check to see if all data channels are registered
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING CONTROL CHANNEL ID: %d WITH THE EXISTING CONTROL CHANNEL OBJECT ***********************\n\n",aControlChannelId  );
                        //Print the control channel
                        String aControlObjectString = myControlChannelObject.controlChannelObjectToString();
                        System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                        logger.info("ServerHandlerHelper:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    }
                    else{
                        //Add the Data ChannelCTX
                        FileReceiver.DataChannelObject aDataChannelObject = new FileReceiver.DataChannelObject(aDataChannelId,aChannelCtx);
                        //Set Connection Msg Received
                        aDataChannelObject.setConnectMsgReceivedFlag(true);
                        //myControlChannelObject.addDataChannelObject(aDataChannelObject);
                        myControlChannelObject.addDataChannelObject(new FileReceiver.DataChannelObject(aDataChannelId,aChannelCtx));
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING DATA CHANNEL ID: %d FOR CONTROL CHANNEL ID: %d WITH THE EXISTING CONTROL CHANNEL OBJECT ***********************\n\n",aDataChannelId, aControlChannelId );
                        String aControlObjectString = myControlChannelObject.controlChannelObjectToString();
                        System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                        logger.info("ServerHandlerHelper:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    }
                    //check to see if all data channels are registered
                    logger.info("FileReceiver:registerChannelCtx: Size of Data Channel Object List = " + myControlChannelObject.getDataChannelObjectList().size() + " Number of expected parallel Data channels for this control channel object = " + myControlChannelObject.getParallelDataChannelNum() );
                    System.err.printf("\n************* myControlChannelObject.getDataChannelObjectList().size( %d ) >= myControlChannelObject.getParallelDataChannelNum( %d ) ***********************\n\n",myControlChannelObject.getDataChannelObjectList().size(),myControlChannelObject.getParallelDataChannelNum() );
                    if (myControlChannelObject.getDataChannelObjectList().size() >= myControlChannelObject.getParallelDataChannelNum()){
                        //Check to make sure the Control Channel is registered, if it is return the ContextChannelHandler
                        if (myControlChannelObject.getControlChannel() != null) {
                            returnCtx = myControlChannelObject.getControlChannel();
                        }
                    }
                }

                if (!myAliasPathAndConcurrencyNum.containsKey(aPathAliasName)){
                    myAliasPathAndConcurrencyNum.put(aPathAliasName,String.valueOf(aConcurrencyNum));
                }
            }//End aPathAliasName != Null
            //logger.info("FileReceiver: Registered Channels: " + registeredChannelsToString());
            //System.err.printf("FileReceiver: Registered Channels: %s",registeredChannelsToString());
            return returnCtx;
        }catch(Exception e){
            System.err.printf("RegisterChannel Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public synchronized static String registeredChannelsToString(){
        try {
            //public HashMap<String,HashMap<String,ControlChannelObject>> myRegisteredChannels;
            String StringToPrint = "";
            Iterator<Map.Entry<String,HashMap<String,FileReceiver.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();

            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                Map.Entry<String, HashMap<String, FileReceiver.ControlChannelObject>> aPathEntry = pathIterator.next();
                //Get the Control Channel HashMap belonging to this path
                HashMap<String, FileReceiver.ControlChannelObject> myControlChannelHashMap = aPathEntry.getValue();
                String theAliasPath = aPathEntry.getKey();
                StringToPrint=StringToPrint+"["+theAliasPath+"]: ";
                //Iterate through the control channel hashMap associated with the above path
                Iterator<Map.Entry<String, FileReceiver.ControlChannelObject>> controlChannelIterator = myControlChannelHashMap.entrySet().iterator();
                while (controlChannelIterator.hasNext()) {
                    Map.Entry<String, FileReceiver.ControlChannelObject> aControlChannelEntry = controlChannelIterator.next();
                    String theControlChannelIdString = aControlChannelEntry.getKey();
                    StringToPrint=StringToPrint+"\n"+"  Control Channel Id: "+ theControlChannelIdString;

                    FileReceiver.ControlChannelObject theControlChannelObject = aControlChannelEntry.getValue();
                    //Get the data object list and iterate through it
                    List<FileReceiver.DataChannelObject> theDataChannelObjectList = theControlChannelObject.getDataChannelObjectList();

                    Iterator<FileReceiver.DataChannelObject> theDataChannelListIterator = theDataChannelObjectList.iterator();
                    FileReceiver.DataChannelObject theDataChannelObject = null;
                    while (theDataChannelListIterator.hasNext()) {
                        theDataChannelObject = theDataChannelListIterator.next();
                        int theDataChannelId = theDataChannelObject.getDataChannelId();
                        //String theDataChannelIdString = theDataChannelId.toString();
                        String theDataChannelIdString = String.valueOf(theDataChannelId);
                        StringToPrint=StringToPrint+"\n"+"    Data Channel Id: "+ theDataChannelIdString;
                    }//End iterating over the data channels
                }//End iterating over the control channels per path
            }//End iterating over the paths
            return StringToPrint;

        }catch(Exception e){
            System.err.printf("RegisterChannelTo String Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    public static void main(String[] args) throws Exception {

      //ServerHandlerHelper myServerHandlerHelper = new ServerHandlerHelper();

        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new FileReceiverInitializer())
             .childOption(ChannelOption.AUTO_READ, true); // when false have to manually call channel read, need to set to true to automatically have server read
             //.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
            // Start the server.
            ChannelFuture f = b.bind(LOCAL_PORT).sync();


            // Wait/block until this server socket is closed by some event. Note this is the Parent socket
            //Still must
            f.channel().closeFuture().sync();
             logger.info("Started the Server on port " + LOCAL_PORT);

            long threadId = Thread.currentThread().getId();
            logger.info("******************************************************");
            logger.info("FileReceiver:  ThreadId = " + threadId );
            logger.info("******************************************************");

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
