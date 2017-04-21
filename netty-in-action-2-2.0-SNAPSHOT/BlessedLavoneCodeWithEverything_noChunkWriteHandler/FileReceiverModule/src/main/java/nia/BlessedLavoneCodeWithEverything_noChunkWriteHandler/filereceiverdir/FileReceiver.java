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
package nia.BlessedLavoneCodeWithEverything_noChunkWriteHandler.filereceiverdir;

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
        //       File ID, File Ack Object List (Data Channels who reported File Ack for the given file id)
        HashMap<String, ArrayList<FileReceiver.FileAckObject>> myFileAckHashMap;
        int parallelDataChannelNum;
        boolean connectAckMsgReceived; //For the File Sender
        boolean connectMsgReceived; //For the File Receiver
        FileReceiverHandler myControlChannelHandler;
        long threadId;

        //Throughput Statistics
        long totalBytesRead;
        long minStartTime;
        long maxEndTime;
        boolean minStartTimeSet;
        boolean maxEndTimeSet;

        public final int CONTROL_CHANNEL_TYPE = 0;
        public final int DATA_CHANNEL_TYPE = 1;

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, List<FileReceiver.DataChannelObject> aDataChannelList, int aParallelNum ){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = aDataChannelList;
            connectAckMsgReceived = false;
            parallelDataChannelNum = aParallelNum;
            myFileAckHashMap = new HashMap<String,ArrayList<FileReceiver.FileAckObject>>();
            myControlChannelHandler = null;
            //Throughput Statistics
            minStartTime = 0;
            maxEndTime = 0;
            totalBytesRead = 0;
            minStartTimeSet = false;
            maxEndTimeSet = false;

        }

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, int aParallelNum){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
            connectAckMsgReceived = false;
            //myDataChannelList = new LinkedList<DataChannelObject>();
            parallelDataChannelNum = aParallelNum;
            myFileAckHashMap = new HashMap<String,ArrayList<FileReceiver.FileAckObject>>();
            myControlChannelHandler = null;
            //Throughput Statistics
            minStartTime = 0;
            maxEndTime = 0;
            totalBytesRead = 0;
            minStartTimeSet = false;
            maxEndTimeSet = false;
        }

        public ControlChannelObject(int aControlChannelId, int aDataChannelId, int aChannelType, ChannelHandlerContext aChannelCtx, int aParallelNum, FileReceiverHandler aControlChannelHandler, FileReceiverHandler aDataChannelHandler, long aThreadId){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = null;
            myControlChannelHandler = null;
            parallelDataChannelNum = aParallelNum;
            //Throughput Statistics
            minStartTime = 0;
            maxEndTime = 0;
            totalBytesRead = 0;
            minStartTimeSet = false;
            maxEndTimeSet = false;


            myFileAckHashMap = new HashMap<String,ArrayList<FileReceiver.FileAckObject>>();
            //What does the connect Ack Msg Received indicate, does it indicate the control channel successfully connected or does it indicate that both the control channel and its associated parallel data channels successfully connected
            connectAckMsgReceived = false;
            if (aChannelType == CONTROL_CHANNEL_TYPE) {
                myControlChannelCtx = aChannelCtx;
                myControlChannelHandler = aControlChannelHandler;
                threadId = aThreadId;
                if (myControlChannelHandler!=null) {
                    //logger.info("***FILE RECEIVER: INSIDE CONTROL CHANNEL OBJECT: CONTROL CHANNEL " + aControlChannelId + " ADDED CONTROL CHANNEL HANDLER TO THE CONTROL CHANNEL OBJECT *****");
                    connectAckMsgReceived = true;
                }
                else{
                    //logger.info("***FILE RECEIVER: INSIDE CONTROL CHANNEL OBJECT: CONTROL CHANNEL " + aControlChannelId + " DID NOT ADD CONTROL CHANNEL HANDLER TO THE CONTROL CHANNEL OBJECT, THE CONTROL HANDLER IS NULL *** ");
                }

                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
                }
            }
            else { //This is a data Channel
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileReceiver.DataChannelObject>();
                    myDataChannelList.add(new FileReceiver.DataChannelObject(aDataChannelId, aChannelCtx,aThreadId, aDataChannelHandler));
                }
                else {
                    //Add the Data Channel to the List
                    myDataChannelList.add(new FileReceiver.DataChannelObject(aDataChannelId, aChannelCtx, aThreadId, aDataChannelHandler));
                }
            }
            //myDataChannelList = new LinkedList<DataChannelObject>();

        }

        public void setMinStartTime_ForControlObject(long aVal){
            try {
                if (!minStartTimeSet) {
                    if (aVal > 0) {
                        minStartTime = aVal;
                        minStartTimeSet = true;
                    }
                } else {
                    if (aVal > 0) {
                        minStartTime = ((aVal < minStartTime) ? aVal : minStartTime);
                    }
                }
            }catch(Exception e){
                System.err.printf("FileReceiver: ControlChannelObject: setMinStartTime error msg: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }//End SetMinStartTime

        public long getMinStartTime_ForControlObject(){
            return minStartTime;
        }

        public void setMaxEndTime_ForControlObject(long aVal){
            try {
                if (!maxEndTimeSet) {
                    if (aVal > 0) {
                        maxEndTime = aVal;
                        maxEndTimeSet = true;
                    }
                } else {
                    if (aVal > 0) {
                        maxEndTime = ((aVal > maxEndTime) ? aVal : maxEndTime);
                    }
                }
            }catch(Exception e){
                System.err.printf("FileReceiver: ControlChannelObject: setMaxEndTime: error msg: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }//End SetMinStartTime

        public long getMaxEndTime_ForControlObject(){
            return maxEndTime;
        }

        public void addToTotalBytes_ForControlObject(long aVal){
            try {
                if (aVal > 0) {
                    totalBytesRead += aVal;
                }
            }catch(Exception e){
                System.err.printf("FileReceiver: ControlChannelObject: addToTotalBytes: error msg: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }

        public long getTotalBytesRead_ForControlObject(){
            return totalBytesRead;
        }

        public boolean getMinStartTimeSet_ForControlObject(){
            return minStartTimeSet;
        }

        public boolean getMaxEndTimeSet_ForControlObject(){
            return maxEndTimeSet;
        }

        public long getThreadId_ForControlObject() {
            return threadId;
        }

        public void setThreadId_ForControlObject(long threadId) {
            this.threadId = threadId;
        }

        /*
          Note some Control Objects may not process files so they will not have any throughput values
          So I need to check to see if the minStartTimeSet boolean flag is set and the maxEndTimeSet boolean flag
          if so then this Control Channel Object has Throughput Information
         */

        public HashMap<String,ArrayList<FileReceiver.FileAckObject>> getFileAckHashMap_ForControlObject(){
          return myFileAckHashMap;
        }

        public void setFileAckHashMap_ForControlObject(HashMap<String,ArrayList<FileReceiver.FileAckObject>> aFileAckHashMap){
            myFileAckHashMap = aFileAckHashMap;
        }

        public FileReceiverHandler getControlChannelHandler_ForControlObject(){
            return myControlChannelHandler;
        }

        public void setControlChannelHandler_ForControlObject(FileReceiverHandler aControlChannelHandler){
            myControlChannelHandler = aControlChannelHandler;
        }

        public boolean getConnectMsgReceivedFlag_ForControlObject(){
            return connectMsgReceived;
        }

        //setConnectMsgAck
        public void setConnectMsgReceivedFlag_ForControlObject(boolean aVal){
            connectMsgReceived = aVal;
        }

        public boolean getConnectAckMsgReceivedFlag_ForControlObject(){
            return connectAckMsgReceived;
        }

        //setConnectMsgAck
        public void setConnectAckMsgReceivedFlag_ForControlObject(boolean aVal){
            connectAckMsgReceived = aVal;
        }

        public void addDataChannelObject_ToControlObject(FileReceiver.DataChannelObject aDataChannelObject){
        /*
        if (myDataChannelList == null){
            myDataChannelList = new LinkedList<DataChannelObject>();
        }
        */
            myDataChannelList.add(aDataChannelObject);
        }

        public int getControlChannelId_ForControlObject(){
            return myControlChannelId;
        }

        public void setControlChannelId_ForControlObject(int aControlChannelId){
            myControlChannelId = aControlChannelId;
        }

        public void setControlChannel_ForControlObject(ChannelHandlerContext aControlChannelCtx){
            myControlChannelCtx = aControlChannelCtx;
        }

        public ChannelHandlerContext getControlChannel_ForControlObject(){
            return myControlChannelCtx;
        }

        /*
        public void setControlChannelHandler_ForControlObject(FileReceiverHandler aControlChannelHandler){
            myControlChannelHandler = aControlChannelHandler;
        }
        */

        /*
        public FileReceiverHandler getControlChannelHandler_ForControlObject(){
            return myControlChannelHandler;
        }
        */

        public List<FileReceiver.DataChannelObject> getDataChannelObjectList_ForControlObject(){
            return myDataChannelList;
        }

        public void setDataChannelObjectList_ForControlObject(List<FileReceiver.DataChannelObject> aDataChannelList){

            myDataChannelList = aDataChannelList;
        }

        public int getParallelDataChannelNum_ForControlObject(){
            return parallelDataChannelNum;
        }

        public void setParallelDataChannelNum_ForControlObject(int aVal){
            parallelDataChannelNum = aVal;
        }

        public String controlChannelObjectToString_ForControlObject(){
            try {
                //Add the Control Channel Id to the String
                String StringToPrint = "--Control Channel Id: " + this.getControlChannelId_ForControlObject();
                List<FileReceiver.DataChannelObject> theDataChannelObjectList = this.getDataChannelObjectList_ForControlObject();
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
        long threadId;
        FileReceiverHandler myDataChannelHandler;
        FileReceiverHandler myControlChannelHandler;

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx, long aThreadId, FileReceiverHandler aDataChannelHandler){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = true;
            threadId = aThreadId;
            myDataChannelHandler = aDataChannelHandler;
            myControlChannelHandler = null;
        }

        public void setControlChannelHandler(FileReceiverHandler aControlChannelHandler){
            myControlChannelHandler = aControlChannelHandler;
        }

        public FileReceiverHandler getControlChannelHandler(){
            return myControlChannelHandler;
        }

        public FileReceiverHandler getDataChannelHandler(){
            return myDataChannelHandler;
        }

        public void setDataChannelHandler(FileReceiverHandler aDataChannelHandler){
            myDataChannelHandler = aDataChannelHandler;
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

        public long getThreadId() {
            return threadId;
        }

        public void setThreadId(long threadId) {
            this.threadId = threadId;
        }

    }


    public static class FileAckObject{
        int myDataChannelId;
        long bytesRead;
        long startTime;
        long endTime;

        public FileAckObject(int aDataChannelId, long theBytesRead, long aStartTime, long anEndTime){
            myDataChannelId = aDataChannelId;
            bytesRead = theBytesRead;
            startTime = aStartTime;
            endTime = anEndTime;
        }

        public int getDataChannelId(){
            return myDataChannelId;
        }

        public void setDataChannelId(int aDataChannelId){
            myDataChannelId = aDataChannelId;
        }

        public long getBytesRead(){
            return bytesRead;
        }

        public void setBytesRead(long theBytesRead){
            bytesRead = theBytesRead;
        }

        public long getStartTime(){
            return startTime;
        }

        public void setStartTime(long aStartTime){
            startTime = aStartTime;
        }

        public long getEndTime(){
            return endTime;
        }

        public void setEndTime(long anEndTime){
            endTime = anEndTime;
        }
    }

    //Add new ControlChannelHandlerAndFileAckObject - contains total bytes read, start time and end time
    //ControlChannelHandlerAndFileAckObject aControlChannelHandlerAndFileAckObject = new ControlChannelHandlerAndFileAckObject(myControlChannelObject.getControlChannelHandler(),totalBytesRead, startTime, endTime);
    //remove FileId from the FileAckMap
    public static class ControlChannelHandlerAndFileAckObject{
        FileReceiverHandler myControlChannelHandler;
        int myFileId;
        long bytesRead;
        long startTime;
        long endTime;

        public ControlChannelHandlerAndFileAckObject(FileReceiverHandler aControlChannelHandler, int aFileId, long theBytesRead, long aStartTime, long anEndTime){
            myControlChannelHandler = aControlChannelHandler;
            myFileId = aFileId;
            bytesRead = theBytesRead;
            startTime = aStartTime;
            endTime = anEndTime;
        }

        public FileReceiverHandler getControlChannelHandler(){
            return myControlChannelHandler;
        }

        public void setControlChannelHandler(FileReceiverHandler aControlChannelReceiver){
            myControlChannelHandler  = aControlChannelReceiver;
        }

        public int getFileId(){
            return myFileId;
        }

        public void setFileId(int aFileId){
            myFileId = aFileId;
        }


        public long getBytesRead(){
            return bytesRead;
        }

        public void setBytesRead(long theBytesRead){
            bytesRead = theBytesRead;
        }

        public long getStartTime(){
            return startTime;
        }

        public void setStartTime(long aStartTime){
            startTime = aStartTime;
        }

        public long getEndTime(){
            return endTime;
        }

        public void setEndTime(long anEndTime){
            endTime = anEndTime;
        }
    }

    /*
       Method Description: Registers the Channel Handler Context of either a data channel or a control channel and also checks to
                           see if all data channels are registered
       Returns: Returns the Control Channel Handler Context if all data channels are connected, else it returns Null;
     */
    //----public static synchronized ChannelHandlerContext registerChannelCtx(String aPathAliasName, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, int aParallelNum, int aConcurrencyNum, FileReceiverHandler aControlChannelHandler ){
    public static synchronized ChannelHandlerContext registerChannelCtx(String aPathAliasName, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, int aParallelNum, int aConcurrencyNum, FileReceiverHandler aControlChannelHandler, FileReceiverHandler aDataChannelHandler, long aThreadId  ){
        try {
            registerChannelCtxCounter++;
            /*
            if (aChannelType == CONTROL_CHANNEL_TYPE){
                logger.info("**************--------- REGISTERING CONTROL CHANNEL " + aControlChannelId + " ******************************----------------" );
            }else {
                logger.info("*******************--------- REGISTERING DATA CHANNEL " +  aDataChannelId + " BELONGING TO CONTROL CHANNEL " + aControlChannelId + " ***********************-----------------");
            }
            */
            ChannelHandlerContext returnCtx = null;
            FileReceiverHandler myControlChannelHandler = null;
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                if (!myRegisteredChannelsCtx.containsKey(aPathAliasName)) {
                    //ControlChannelId ControlChannelObject
                    myRegisteredChannelsCtx.put(aPathAliasName, new HashMap<String, FileReceiver.ControlChannelObject>());
                }
                //the  Hashmap now contains the path if it didn't before, or if it did now just use the path to get the ControlChannel Map (Control Channel ID, Control Channel Object)
                HashMap<String, FileReceiver.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                //return myHashMap2;

                //If the Control Channel Object Map doesn't contain the specified Channel ID
                if (!myControlChannelObjectMap.containsKey( String.valueOf(aControlChannelId) ) ) {

                    //If the ControlObject Doesn't exist - Create the ChannelControlObject either with a ControlChannelCTX if a control channel is registering or with a DataChannelCTX if a DataChannel is registering
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileReceiver.ControlChannelObject(aControlChannelId, aDataChannelId, aChannelType, aChannelCtx, aParallelNum, aControlChannelHandler, null, aThreadId));
                    }
                    else {
                        //THIS IS A DATA CHANNEL REGISTERING SET THE CONTROL CHANNEL HANDLER TO NULL
                        myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileReceiver.ControlChannelObject(aControlChannelId, aDataChannelId, aChannelType, aChannelCtx, aParallelNum, null, aDataChannelHandler, aThreadId));
                    }
                    ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    //Print the Control Object String
                    //String aControlObjectString = myControlChannelObject.controlChannelObjectToString();
                    //System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                    //logger.info("FileReceiver:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        if (myControlChannelObject.getControlChannelHandler_ForControlObject() != null) {
                            //logger.info("FileReceiver:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " FOR THIS CONTROL CHANNEL I SET THE CONNECTION MSG AS RECEIVED (MEANING THE CONTROL CHANNEL REGISTERED IT SELF AS RECEIVING THE CONNECTION MSG");
                            myControlChannelObject.setConnectMsgReceivedFlag_ForControlObject(true);
                        }
                        //myControlChannelObject.setControlChannelHandler(aControlChannelHandler);
                        //System.err.printf("\n*************FILE RECEIVER: REGISTERING CONTROL CHANNEL ID: %d ***********************\n\n",aControlChannelId  );
                    }
                    /*
                    else {
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING DATA CHANNEL ID: %d FOR CONTROL CHANNEL ID: %d ***********************\n\n",aDataChannelId, aControlChannelId );
                    }
                    */
                    //logger.info("FileReceiver:registerChannelCtx: Size of Data Channel Object List = " + myControlChannelObject.getDataChannelObjectList().size() + " Number of expected parallel Data channels for this control channel object = " + myControlChannelObject.getParallelDataChannelNum() );
                    //check to see if all data channels are registered
                    System.err.printf("\n************* myControlChannelObject.getDataChannelObjectList().size( %d ) >= myControlChannelObject.getParallelDataChannelNum( %d ) ***********************\n\n",myControlChannelObject.getDataChannelObjectList_ForControlObject().size(),myControlChannelObject.getParallelDataChannelNum_ForControlObject() );
                    if (myControlChannelObject.getDataChannelObjectList_ForControlObject().size() >= myControlChannelObject.getParallelDataChannelNum_ForControlObject()){
                        //Check to make sure the Control Channel is registered, if it is return the ContextChannelHandler
                        if (myControlChannelObject.getControlChannel_ForControlObject() != null) {
                            returnCtx = myControlChannelObject.getControlChannel_ForControlObject();
                        }
                    }
                }//End ControlObject Doesn't exist -
                else {
                    //a Control Channel Object exist with this Control Channel ID already
                    FileReceiver.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        //Add the Control ChannelCTX and the Control Channel Handler to the Control Object
                        myControlChannelObject.setControlChannel_ForControlObject(aChannelCtx);
                        myControlChannelObject.setControlChannelHandler_ForControlObject(aControlChannelHandler);
                        myControlChannelObject.setConnectMsgReceivedFlag_ForControlObject(true);
                        myControlChannelObject.setThreadId_ForControlObject(aThreadId);

                        //check to see if all data channels are registered
                        System.err.printf("\n*************FILE RECEIVER: REGISTERING CONTROL CHANNEL ID: %d WITH THE EXISTING CONTROL CHANNEL OBJECT ***********************\n\n",aControlChannelId  );
                        //Print the control channel
                        String aControlObjectString = myControlChannelObject.controlChannelObjectToString_ForControlObject();
                        System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                        //logger.info("ServerHandlerHelper:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    }
                    else{
                        //Add the Data ChannelCTX
                        myControlChannelObject.addDataChannelObject_ToControlObject(new FileReceiver.DataChannelObject(aDataChannelId,aChannelCtx,aThreadId,aDataChannelHandler));
                        //FileReceiver.DataChannelObject aDataChannelObject = new FileReceiver.DataChannelObject(aDataChannelId,aChannelCtx);
                        //Set Connection Msg Received
                        //aDataChannelObject.setConnectMsgReceivedFlag(true);
                        //myControlChannelObject.addDataChannelObject(aDataChannelObject);

                        //System.err.printf("\n*************FILE RECEIVER: REGISTERING DATA CHANNEL ID: %d FOR CONTROL CHANNEL ID: %d WITH THE EXISTING CONTROL CHANNEL OBJECT ***********************\n\n",aDataChannelId, aControlChannelId );
                        String aControlObjectString = myControlChannelObject.controlChannelObjectToString_ForControlObject();
                        //System.err.printf("\n*************FILE RECEIVER: CONTROL CHANNEL OBJECT IN STRING FORMAT = %s ***********************\n\n",aControlObjectString );
                        //logger.info("ServerHandlerHelper:registerChannelCtx: The Control Channel ID("+ aControlChannelId + ") ADDED TO THE CONTROL CHANNEL OBJECT MAP FOR PATH: " + aPathAliasName + " THE CONTROL OBJECT TO STRING IS: " + aControlObjectString);
                    }
                    //check to see if all data channels are registered
                    //logger.info("FileReceiver:registerChannelCtx: Size of Data Channel Object List = " + myControlChannelObject.getDataChannelObjectList().size() + " Number of expected parallel Data channels for this control channel object = " + myControlChannelObject.getParallelDataChannelNum() );
                    //System.err.printf("\n************* myControlChannelObject.getDataChannelObjectList().size( %d ) >= myControlChannelObject.getParallelDataChannelNum( %d ) ***********************\n\n",myControlChannelObject.getDataChannelObjectList().size(),myControlChannelObject.getParallelDataChannelNum() );
                    if (myControlChannelObject.getDataChannelObjectList_ForControlObject().size() >= myControlChannelObject.getParallelDataChannelNum_ForControlObject()){
                        //Check to make sure the Control Channel is registered, if it is return the ContextChannelHandler
                        if (myControlChannelObject.getControlChannel_ForControlObject() != null) {
                            returnCtx = myControlChannelObject.getControlChannel_ForControlObject();
                            myControlChannelHandler = myControlChannelObject.getControlChannelHandler_ForControlObject();
                            List<FileReceiver.DataChannelObject> theDataChannelObjectList = myControlChannelObject.getDataChannelObjectList_ForControlObject();
                            for (FileReceiver.DataChannelObject aDataChannelObject : theDataChannelObjectList ){
                                //Add the ControlHandler to Each Data Channel Object to report the throughput
                                aDataChannelObject.setControlChannelHandler(myControlChannelHandler);
                                FileReceiverHandler theDataChannelHandler = aDataChannelObject.getDataChannelHandler();
                                //Add the ControlHandler to Each Data Channel Handler to report the throughput
                                if (theDataChannelHandler != null){
                                    //First add the dataChannelHandler to ControlChannelHandler's DataChannelHandler List
                                    myControlChannelHandler.addDataChannelHandler(theDataChannelHandler);
                                    //Add the ControlHandler to Each Data Channel Handler to report the throughput
                                    theDataChannelHandler.setControlChannelHandler(myControlChannelHandler);
                                }
                            }

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

    //----public synchronized static String registeredChannelsToString(){
    public static synchronized String registeredChannelsToString(){
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
                    List<FileReceiver.DataChannelObject> theDataChannelObjectList = theControlChannelObject.getDataChannelObjectList_ForControlObject();

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

    //Only Data Channels should call this method, as a data channel will be the only one registering File Acks
    //This method assumes a control object exist since all channels already registered. This method also assumes each
    // Control Object has a ControlChannelHandler
    //Input
    //Output: returns the ControlChannelHandlerAndFileAckObject - if the data channel calling this method is the last one registering the fileACK
    //        else NULL is returned - indicating this is not the last data channel that is registering the file ack
    //----public static synchronized FileReceiver.ControlChannelHandlerAndFileAckObject registerFileAck(String aPathAliasName, int aControlChannelId, int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime ){
    public static FileReceiver.ControlChannelHandlerAndFileAckObject registerFileAck(String aPathAliasName, int aControlChannelId, int aDataChannelId, int aFileId, long theBytesRead, long theStartTime, long theEndTime ){
        try {
            FileReceiverHandler myControlChannelHandler = null;
            ControlChannelHandlerAndFileAckObject myControlChannelHandlerAndFileAckObject = null;
            long minStartTime = 0;
            long maxEndTime = 0;
            long totalBytesRead = 0;
            boolean minStartTimeSet = false;
            boolean maxEndTimeSet = false;

            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //Get the Control Channel HashMap for the given Path
                HashMap<String, FileReceiver.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);

                //a Control Channel Object exist with this Control Channel ID already (Since ALL CHANNELS Registered)
                FileReceiver.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));

                //Get the File Ack HashMap
                HashMap<String, ArrayList<FileReceiver.FileAckObject>> myFileAckMap = myControlChannelObject.getFileAckHashMap_ForControlObject();

                //See if the FileId exist for the FileAckMap
                if (!myFileAckMap.containsKey(String.valueOf(aFileId))) {
                    //If Not: Create the File Ack List for this File Id
                    myFileAckMap.put(String.valueOf(aFileId), new ArrayList<FileReceiver.FileAckObject>());
                }
                //Get the List of File Acks for this FileID which exists now, if it didn't before
                ArrayList<FileReceiver.FileAckObject> myFileAckList = myFileAckMap.get(String.valueOf(aFileId));
                // Add the file ack to the existing file ack list for this File Id
                myFileAckList.add(new FileReceiver.FileAckObject(aDataChannelId, theBytesRead, theStartTime, theEndTime));
                //logger.info("FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", REGISTERED FILE ACK FOR FILE " + aFileId +", ALSO SIZE OF FILE ACK LIST = " + myFileAckList.size() + ", NUMBER OF PARALLEL DATA CHANNELS = " + myControlChannelObject.getParallelDataChannelNum());
                //See if all data channels for this Control Channel reported
                // they received the file fragment for this FileId
                if (myFileAckList.size() >= myControlChannelObject.getParallelDataChannelNum_ForControlObject()) {
                    //ALL DATA CHANNELS REPORTED RECEIVING THE FILE FRAGMENT FOR THE FILE ID
                    myControlChannelHandler = myControlChannelObject.getControlChannelHandler_ForControlObject();
                    /*
                    if (myControlChannelHandler != null) {
                        logger.info("FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", GOT CONTROL CHANNEL HANDLER AND IT IS NOT NULL. ALSO SIZE OF FILE ACK LIST = " + myFileAckList.size() + ", NUMBER OF PARALLEL DATA CHANNELS = " + myControlChannelObject.getParallelDataChannelNum() );
                    }else {
                        logger.info("FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", DID NOT GET THE EXPECTED CONTROL CHANNEL HANDLER BECAUSE IT IS NULL");
                    }
                    */
                    //Iterate through the FileAckObject List & Get the Min Start Time, Max End Time and the Total Bytes Read
                    for (FileReceiver.FileAckObject aFileAckObject: myFileAckList){
                        //Get StartTime
                        if (!minStartTimeSet){
                            minStartTime = aFileAckObject.getStartTime();
                            minStartTimeSet = true;
                        } else {
                            //Get the min time
                            minStartTime = ((aFileAckObject.getStartTime() < minStartTime) ? aFileAckObject.getStartTime() : minStartTime);
                        }

                        if (!maxEndTimeSet){
                            maxEndTime = aFileAckObject.getEndTime();
                            maxEndTimeSet = true;
                        } else {
                            //Get the max time
                            maxEndTime = ((aFileAckObject.getEndTime() > maxEndTime) ? aFileAckObject.getEndTime() : maxEndTime);
                        }
                        totalBytesRead+=aFileAckObject.getBytesRead();
                    }
                    //logger.info("\n****FileReceiver: RegisterFileAck: DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId + ", MIN START TIME = " + minStartTime + ", MAX END TIME = " + maxEndTime + ", BYTES READ =  " + totalBytesRead + "******\n" );

                    //Add new ControlChannelHandlerAndFileAckObject - contains total bytes read, start time and end time
                    myControlChannelHandlerAndFileAckObject = new FileReceiver.ControlChannelHandlerAndFileAckObject(myControlChannelHandler,aFileId,totalBytesRead,minStartTime,maxEndTime);

                    //Add total bytes read, start time and end time to the control channel object
                    //Note the Control Channel Object it self will determine the min start time and Max End Time when passing in these values
                    myControlChannelObject.setMinStartTime_ForControlObject(minStartTime);
                    myControlChannelObject.setMaxEndTime_ForControlObject(maxEndTime);
                    myControlChannelObject.addToTotalBytes_ForControlObject(totalBytesRead);

                    //remove FileId Entry from the FileAckMap
                    myFileAckMap.remove(String.valueOf(aFileId));

                }
            }
            else {
               // logger.info("FileReceiver: RegisterFileAck: THE PASSED IN ALIAS PATH IS NULL FOR DATA CHANNEL " + aDataChannelId + ", BELONGING TO CONTROL CHANNEL " + aControlChannelId);
            }

            //return myControlChannelHandler;
            return myControlChannelHandlerAndFileAckObject;

        }catch(Exception e){
            System.err.printf("RegisterChannel Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //----public synchronized static void printAllThreadIds(){
    public static void printAllThreadIds(){
        try {
            String StringToPrint = "";
            StringToPrint = StringToPrint + "\n FileReceiver: THREAD ID: " + Thread.currentThread().getId();

            //Iterate through the Path and Control Channel HashMap
            Iterator<Map.Entry<String, HashMap<String,FileReceiver.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                //        Path             Control Channel ID, Control Channel Object
                Map.Entry<String, HashMap<String, FileReceiver.ControlChannelObject>> myRegisteredChannelsCtxEntry = pathIterator.next();
                //Get the Path Name
                StringToPrint = StringToPrint + "\nPath: " + myRegisteredChannelsCtxEntry.getKey();
                //Get the Control Channel Id Hash Map and iterate through it
                HashMap<String, FileReceiver.ControlChannelObject> theControlChannelIdHashMap = myRegisteredChannelsCtxEntry.getValue();
                //Iterate through the control channel Id HashMap for the given path
                Iterator<Map.Entry<String, FileReceiver.ControlChannelObject>> controlChannelIdIterator = theControlChannelIdHashMap.entrySet().iterator();
                while (controlChannelIdIterator.hasNext()) {
                    //Get the Control Channel Id - Hash Map Entry
                    Map.Entry<String, FileReceiver.ControlChannelObject> controlChannelIdEntry = controlChannelIdIterator.next();
                    //Get the Control Channel Id
                    StringToPrint = StringToPrint + "\n--Control Channel " + controlChannelIdEntry.getKey() + ": THREAD ID: ";
                    //Get the control channel Object
                    FileReceiver.ControlChannelObject aControlChannelObject = controlChannelIdEntry.getValue();
                    StringToPrint = StringToPrint + aControlChannelObject.getThreadId_ForControlObject();
                    //Get Data Channel List
                    List<FileReceiver.DataChannelObject> myDataChannelObjectList = aControlChannelObject.getDataChannelObjectList_ForControlObject();
                    //Iterate through the Data Channel Object List
                    for (FileReceiver.DataChannelObject aDataChannelObject: myDataChannelObjectList){
                        StringToPrint = StringToPrint + "\n ----Data Channel: " + aDataChannelObject.getThreadId();
                    }
                } //End Iterating through the Control Channel for this given path

            }//End iterating through paths in the HashMap

            logger.info(StringToPrint);

        }catch(Exception e) {
            System.err.printf("FileReceiver: ThroughputToString Error: %s \n ", e.getMessage());
            e.printStackTrace();
            //System.exit(1);
        }//End Catch
    }//End printAllThreadIds

    //---public static synchronized void printAllThroughputToScreen(){
    public static void printAllThroughputToScreen(){
        try {
            logger.info("FileReceivre: printAllThroughputToScreen Method Entered");
            String StringToPrint = "";
            StringToPrint = StringToPrint + "\n";
            //Individual Path start time, end time and total Bytes
            long pathMinStartTime = -1;
            long pathMaxEndTime = -1;
            long pathTotalBytes = 0;
            //Overall (Total) min start time, end time and total Bytes from all paths
            long overallMinStartTime = -1;
            long overallMaxEndTime = -1;
            long overallTotalBytes = 0;
            //boolean Individual Path flags,
            boolean pathMinStartTimeSet = false;
            boolean pathMaxEndTimeSet = false;
            boolean pathTotalBytesSet = false;
            //boolean Overall (Total) Path
            boolean overallMinStartTimeSet = false;
            boolean overallMaxEndTimeSet = false;
            boolean overallTotalBytesSet = false;
            //Throughput in Mb/s
            double controlChannelThroughput = 0;
            double pathThroughput = 0;
            double overAllThroughput = 0;


            //Iterate through the Path and Control Channel HashMap
            //                <Path,   <Control Channel Id, Control Channel Object>
            Iterator<Map.Entry<String, HashMap<String,FileReceiver.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                //        Path             Control Channel ID, Control Channel Object
                Map.Entry<String, HashMap<String, FileReceiver.ControlChannelObject>> myRegisteredChannelsCtxEntry = pathIterator.next();
                //Get the Path Name
                StringToPrint = StringToPrint + "Path: " + myRegisteredChannelsCtxEntry.getKey();
                //Get the Control Channel Id Hash Map and iterate through it
                HashMap<String, FileReceiver.ControlChannelObject> theControlChannelIdHashMap = myRegisteredChannelsCtxEntry.getValue();
                //Iterate through the control channel Id HashMap for the given path
                Iterator<Map.Entry<String, FileReceiver.ControlChannelObject>> controlChannelIdIterator = theControlChannelIdHashMap.entrySet().iterator();
                while (controlChannelIdIterator.hasNext()) {
                    //Get the Control Channel Id - Hash Map Entry
                    Map.Entry<String, FileReceiver.ControlChannelObject> controlChannelIdEntry = controlChannelIdIterator.next();
                    //Get the Control Channel Id
                    StringToPrint = StringToPrint + "\n ----Control Channel " + controlChannelIdEntry.getKey() + ": ";
                    //Get the control channel Object
                    FileReceiver.ControlChannelObject aControlChannelObject = controlChannelIdEntry.getValue();

                    //Check to make sure this control channel received and processed a file transfer
                    //if (aControlChannelObject.getMinStartTimeSet() && aControlChannelObject.getMaxEndTimeSet()) {
                        //Calculate Throughput in Mb/s AND make sure the endTime > startTime, this will avoid getting an arithmetic error
                        //if (aControlChannelObject.getMaxEndTime() > aControlChannelObject.getMinStartTime()) {
                        if ((aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime() > 0) && (aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime() > 0)) {
                            controlChannelThroughput = (((aControlChannelObject.getControlChannelHandler_ForControlObject().getTotalBytesRead() * 8) / (aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime() - aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime())) * 1000) / 1000000;

                            //Get start Time, End Time and BytesRead from this Control Channel Object
                            //StringToPrint = StringToPrint + " Start Time: " + aControlChannelObject.getMinStartTime_ForControlObject() + ", End Time: " + aControlChannelObject.getMaxEndTime_ForControlObject() + ", Bytes Read: " + aControlChannelObject.getTotalBytesRead_ForControlObject() + ", Throguhput: " + controlChannelThroughput + "Mb/s \n";
                            StringToPrint = StringToPrint + " Start Time: " + aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime() + ", End Time: " + aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime() + ", Bytes Read: " + aControlChannelObject.getControlChannelHandler_ForControlObject().getTotalBytesRead() + ", Throguhput: " + controlChannelThroughput + "Mb/s \n";
                            //Path Total Bytes Read
                            pathTotalBytes+=aControlChannelObject.getControlChannelHandler_ForControlObject().getTotalBytesRead();

                            //Set Path Min Start Time
                            if (!pathMinStartTimeSet){
                                pathMinStartTimeSet = true;
                                //pathMinStartTime = aControlChannelObject.getMinStartTime_ForControlObject();
                                pathMinStartTime = aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime();
                            } else {
                                pathMinStartTime = (aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime() < pathMinStartTime) ? aControlChannelObject.getControlChannelHandler_ForControlObject().getMinStartTime() : pathMinStartTime;
                            }

                            //Set Path Max End Time
                            if (!pathMaxEndTimeSet) {
                                pathMaxEndTimeSet = true;
                                pathMaxEndTime = aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime();
                            } else {
                                pathMaxEndTime = (aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime() > pathMaxEndTime) ? aControlChannelObject.getControlChannelHandler_ForControlObject().getMaxEndTime() : pathMaxEndTime;
                            }

                        }else {
                            StringToPrint = StringToPrint + " Throguhput: 0.00 Mb/s";
                        }

                    //}
                } //End Iterating through the Control Channel for this given path

                //Check to ensure that the path's Start Time and End Time are greater than 0
                if ((pathMaxEndTime > 0) && (pathMinStartTime > 0)) {
                    //Calculate Throughput in Mb/s
                    pathThroughput = (((pathTotalBytes * 8) / (pathMaxEndTime - pathMinStartTime)) * 1000) / 1000000;
                    StringToPrint = StringToPrint + "\n --Path " + myRegisteredChannelsCtxEntry.getKey() + " Metrics: Min Start Time: " + pathMinStartTime + " Max End Time: " + pathMaxEndTime + " Total Bytes Read: " + pathTotalBytes + ", Throughput:  " + pathThroughput + "Mb/s" + "\n";
                    //Set OverAll (Total of All Path's Bytes Read
                    overallTotalBytes += pathTotalBytes;

                    //Set Overall Min Start Time
                    if (!overallMinStartTimeSet) {
                        overallMinStartTimeSet = true;
                        overallMinStartTime = pathMinStartTime;
                    } else {
                        overallMinStartTime = (pathMinStartTime < overallMinStartTime) ? pathMinStartTime : overallMinStartTime;
                    }

                    //Set Overall Max End Time
                    if (!overallMaxEndTimeSet) {
                        overallMaxEndTimeSet = true;
                        overallMaxEndTime = pathMaxEndTime;
                    } else {
                        overallMaxEndTime = (pathMaxEndTime > overallMaxEndTime) ? pathMaxEndTime : overallMaxEndTime;
                    }
                }
                else {
                    StringToPrint = StringToPrint + "\n --Path " + myRegisteredChannelsCtxEntry.getKey() + " 0.00 Mb/s";
                }

                //Reset Path Start Time and Path End Time Values;
                pathMinStartTime = -1;
                pathMaxEndTime = -1;
                pathTotalBytes = 0;
                pathMinStartTimeSet = false;
                pathMaxEndTimeSet = false;

            }//End iterating through paths in the HashMap

            //Calculate Throughput in Mb/s
            if ((overallMinStartTime > 0) && (overallMaxEndTime > 0)) {
                overAllThroughput = (((overallTotalBytes * 8) / (overallMaxEndTime - overallMinStartTime)) * 1000) / 1000000;
            }
            else {
                overAllThroughput = 0;
            }
            StringToPrint = StringToPrint + "\n****\n" + "OVERALL END-TO-END THROUGHPUT METRICS: Min Start Time: " + overallMinStartTime + ", Max End Time: " + overallMaxEndTime + ", Total Bytes Read: " + overallTotalBytes + ", Throughput: " + overAllThroughput + " Mb/s" + "\n *****\n";
            logger.info(StringToPrint);

        }catch(Exception e) {
            System.err.printf("FileSender: ThroughputToString Error: %s \n ", e.getMessage());
            e.printStackTrace();
            //System.exit(1);
        }//End Catch
    }//End printAllThroughputToScreen



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
