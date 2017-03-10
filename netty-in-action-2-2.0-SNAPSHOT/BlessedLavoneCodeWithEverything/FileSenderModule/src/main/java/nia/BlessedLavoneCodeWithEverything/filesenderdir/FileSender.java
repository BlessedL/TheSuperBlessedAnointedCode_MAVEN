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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import io.netty.buffer.*;
import io.netty.handler.stream.ChunkedFile;
import java.io.RandomAccessFile;
import java.util.logging.*;
import java.io.File;
import java.nio.channels.FileChannel;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;


/*
  Write contents to a file when reading, after reading length amount of bytes
 */
public final class FileSender {

    //Note the String in the 2nd HashMap of myRegisteredChannels is really the ControlChannelObject ID converted to the String
    //  myRegisteredChannels = HashMap<Path, <Control Channel ID, Control Channel Object>>
    public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannels = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
    //  myRegisteredChannelsCtx = HashMap<Path, <Control Channel ID, Control Channel Object>>
    public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannelsCtx = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
    static HashMap<String, ArrayList<String>> myPathAndFileRequestList = new HashMap<String, ArrayList<String>>();

    //static final String HOST = System.getProperty("host", "127.0.0.1");
    //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

  static final String HOST = System.getProperty("host", "192.168.0.1");
    //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
  static final int PORT = Integer.parseInt(System.getProperty("port","4959"));
    static final Logger logger = Logger.getLogger(FileSender.class.getName());
    //Logger logger = Logger.getLogger(this.getClass().getName());
    //public List<ThroughputObject> throughputObjectList = new ArrayList<ThroughputObject>();
    public List<ThroughputObject> throughputObjectList;

    static List<StaticThroughputObject> staticThroughputObjectList = new ArrayList<StaticThroughputObject>();
    static List<ChannelFuture> staticChannelFutureList = new ArrayList<ChannelFuture>();
    //main FileRequest List
    static ArrayList<String> mainFileRequestList = new ArrayList<String>();
    static ArrayList<FileSender.TempPathObject> tempPathList = new ArrayList<TempPathObject>();





    static long dataChannelCounter = 0;
    static boolean ALL_CHANNELS_CLOSED = false;

    public static int CONTROL_CHANNEL_TYPE = 0;
    public static int DATA_CHANNEL_TYPE = 1;

    EventLoopGroup group;

  /*
    public static void addToStaticThroughputObjectList(ThroughputObject  o)
    {
        staticThroughputObjectList.add(o);
    }
*/
    int myParallelNum;
    int myParallelNum2;

    int myConcurrencyNum;
    int myPipelineNum;

    //File Name
    String fileName;
    String fileRequest;

    String fileName2;
    String fileRequest2;
    ArrayList<ChannelFuture> channelFutureList;

    public FileSender() {
        channelFutureList = new ArrayList<ChannelFuture>();
        myParallelNum = 1;
        myConcurrencyNum = 1;

        //File Name
        fileName = null;
        fileRequest = null;

        fileName2 = null;
        fileRequest2 = null;

        throughputObjectList = new ArrayList<ThroughputObject>();
        group = null;


    }

    public static void addFileRequestToList()
    {
        //String a fileRequest = "transfer WS5/home/lrodolph/100MB_File.dat WS7/home/lrodolph/100MB_File_Copy.dat";
        FileSender.mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_File.dat WS7/home/lrodolph/100MB_File_Copy.dat");
    }
    public static void addFileRequestToList(String aFileRequest)
    {
        //String a fileRequest = "transfer WS5/home/lrodolph/100MB_File.dat WS7/home/lrodolph/100MB_File_Copy.dat";
        FileSender.mainFileRequestList.add(aFileRequest);
    }


    public static void addTempObjectToTempPathList(FileSender.TempPathObject aTempPathObject){
        tempPathList.add(aTempPathObject);
    }

    public static void addTempObjectToTempPathList(String anIpAddressWithoutSrc, String anAliasPathName, int aConcurrencyNum, int aParallelNum, int aPiplelineNum){
        tempPathList.add(new FileSender.TempPathObject(anIpAddressWithoutSrc, anAliasPathName, aConcurrencyNum, aParallelNum, aPiplelineNum));
    }


    public static void addStaticThroughputObject( FileSender.StaticThroughputObject aThroughputObject)
    {
        staticThroughputObjectList.add(aThroughputObject);
        if (staticThroughputObjectList.size() >= dataChannelCounter){
            logger.info("staticThroughputObjectList.size(" + staticThroughputObjectList.size() + ") >= dataChannelCounter("+dataChannelCounter+")");
            if (!ALL_CHANNELS_CLOSED){
                //Close all channels
                Iterator<ChannelFuture>   staticChannelFutureListIterator = staticChannelFutureList.iterator();
                while (staticChannelFutureListIterator.hasNext()) {
                    ChannelFuture aStaticChannelFuture = staticChannelFutureListIterator.next();
                    aStaticChannelFuture.channel().close();
                }
                ALL_CHANNELS_CLOSED = true;
            }
        }
    }


    public class ThroughputObject{
        int myChannelId;
        long myStartTime;
        long myEndTime;
        long myBytesRead;
        double throughput;

        public ThroughputObject(int channelId, long startTimeRead, long endTimeRead, long bytesRead){
            myChannelId = channelId;
            myStartTime = startTimeRead;
            myEndTime = endTimeRead;
            myBytesRead = bytesRead;
            //double throughput = (myEndTime-myStartTime)/myBytesRead;
            double throughput = -1;
        }

        public int getMyChannelId(){
            return myChannelId;
        }

        public void setChannelId(int aChannelId){
            myChannelId = aChannelId;
        }

        public long getMyStartTime(){
            return myStartTime;
        }

        public void setStartTime(long aStartTime){
            myStartTime = aStartTime;
        }

        public long getEndTime(){
            return myEndTime;
        }

        public void setEndTime(long anEndTime){
            myEndTime = anEndTime;
        }

        public long getBytesRead(){
            return myBytesRead;
        }

        public void setBytesRead(long theBytesRead){
            myBytesRead = theBytesRead;
        }



        /*
        Input:
            theBytes - Amount of Bytes transferred
            theStartTime - The time the transfer  started in milliseconds
            theEndTime - The time the transfer ended in milliseconds
            Output/return value = bits/second (b/s)
        */
        public double getThroughput() throws Exception{
            try {
                if (((myStartTime > 0) && (myEndTime > 0)) && (myBytesRead > 0)) {
                    throughput = (myEndTime - myStartTime) / myBytesRead;
                    //Convert Bytes per millisecond to Bytes per second
                    throughput = throughput * 1000; //Converted throughput from Bytes per millisecond to Bytes per second
                    //Convert Bytes per second (B/s) to bits per second (b/s)
                    throughput = throughput * 8;
                }

                return throughput;
            }catch(Exception e){
                System.err.printf("FileSender: getThroughput: Error Msg: " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }

        public void setThroughput(long theThroughput){
            throughput = theThroughput;
        }


    }


    public static class StaticThroughputObject{
        int myChannelId;
        long myStartTime;
        long myEndTime;
        long myBytesRead;
        double throughput;

        public StaticThroughputObject(int channelId, long startTimeRead, long endTimeRead, long bytesRead){
            myChannelId = channelId;
            myStartTime = startTimeRead;
            myEndTime = endTimeRead;
            myBytesRead = bytesRead;
            //double throughput = (myEndTime-myStartTime)/myBytesRead;
            double throughput = -1;
        }

        public int getMyChannelId(){
            return myChannelId;
        }

        public void setChannelId(int aChannelId){
            myChannelId = aChannelId;
        }

        public long getMyStartTime(){
            return myStartTime;
        }

        public void setStartTime(long aStartTime){
            myStartTime = aStartTime;
        }

        public long getEndTime(){
            return myEndTime;
        }

        public void setEndTime(long anEndTime){
            myEndTime = anEndTime;
        }

        public long getBytesRead(){
            return myBytesRead;
        }

        public void setBytesRead(long theBytesRead){
            myBytesRead = theBytesRead;
        }



        /*
        Input:
            theBytes - Amount of Bytes transferred
            theStartTime - The time the transfer  started in milliseconds
            theEndTime - The time the transfer ended in milliseconds
            Output/return value = bits/second (b/s)
        */
        public double getThroughput() throws Exception{
            try {
                if (((myStartTime > 0) && (myEndTime > 0)) && (myBytesRead > 0)) {
                    throughput = (myEndTime - myStartTime) / myBytesRead;
                    //Convert Bytes per millisecond to Bytes per second
                    throughput = throughput * 1000; //Converted throughput from Bytes per millisecond to Bytes per second
                    //Convert Bytes per second (B/s) to bits per second (b/s)
                    throughput = throughput * 8;
                }

                return throughput;
            }catch(Exception e){
                System.err.printf("FileSender: getThroughput: Error Msg: " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }

        public void setThroughput(long theThroughput){
            throughput = theThroughput;
        }


    }

    public static class DataChannelObject{
        int myDataChannelId;
        ChannelHandlerContext myDataChannelCtx;
        boolean connectMsgReceived;
        FileSenderDataChannelHandler myFileSenderDataChannelHandler;

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = null;
        }

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx, FileSenderDataChannelHandler aFileSenderDataChannelHandler){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = aFileSenderDataChannelHandler;
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

        public  FileSenderDataChannelHandler getFileSenderDataChannelHandler(){
            return myFileSenderDataChannelHandler;
        }

        public void setFileSenderDataChannelHandler(FileSenderDataChannelHandler aFileSenderDataChannelHandler){
            myFileSenderDataChannelHandler  = aFileSenderDataChannelHandler;
        }


    }

    public static class ControlChannelObject{
        int myControlChannelId;
        ChannelHandlerContext myControlChannelCtx;
        FileSenderControlChannelHandler myFileSenderControlHandler;
        List<FileSender.DataChannelObject> myDataChannelList;
        List<String> myFileIdList; //Note the size of this list is controlled by the pipeline value
        int parallelDataChannelNum;
        boolean connectAckMsgReceived; //For the File Sender
        boolean connectMsgReceived; //For the File Receiver
        //Throughput related variables
        long myPreviousStartTime;
        long myPreviousEndTime;
        long myPreviousTotalBytesRead;
        long myStartTime;
        long myEndTime;
        long myTotalBytesRead;
        boolean myStartTimeSet;
        boolean myEndTimeSet;
        boolean myTotalBytesReadSet;

        public final int CONTROL_CHANNEL_TYPE = 0;
        public final int DATA_CHANNEL_TYPE = 1;

        public ControlChannelObject(int aControlChannelId, FileSenderControlChannelHandler aFileSenderControlChannelHandler, ChannelHandlerContext aControlChannelCtx, List<DataChannelObject> aDataChannelList ){
            myControlChannelId = aControlChannelId;
            myFileSenderControlHandler = aFileSenderControlChannelHandler;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = aDataChannelList;
            connectAckMsgReceived = false;
            myDataChannelList = new ArrayList<FileSender.DataChannelObject>();
            myFileIdList = new ArrayList<String>();
            myStartTimeSet = false;
            myEndTimeSet = false;
            myTotalBytesReadSet = false;
            myPreviousStartTime = -1;
            myPreviousEndTime = -1;
            myPreviousTotalBytesRead = -1;
            myStartTime = -1;
            myEndTime = -1;
            myTotalBytesRead =-1;
        }

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, List<DataChannelObject> aDataChannelList ){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = aDataChannelList;
            connectAckMsgReceived = false;
            myDataChannelList = new ArrayList<FileSender.DataChannelObject>();
            myFileSenderControlHandler = null;
            myFileIdList = new ArrayList<String>();
            myStartTimeSet = false;
            myEndTimeSet = false;
            myTotalBytesReadSet = false;
            myPreviousStartTime = -1;
            myPreviousEndTime = -1;
            myPreviousTotalBytesRead = -1;
            myStartTime = -1;
            myEndTime = -1;
            myTotalBytesRead =-1;
        }

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
            connectAckMsgReceived = false;
            myDataChannelList = new ArrayList<FileSender.DataChannelObject>();
            myFileSenderControlHandler = null;
            myFileIdList = new ArrayList<String>();
            myStartTimeSet = false;
            myEndTimeSet = false;
            myTotalBytesReadSet = false;
            myPreviousStartTime = -1;
            myPreviousEndTime = -1;
            myPreviousTotalBytesRead = -1;
            myStartTime = -1;
            myEndTime = -1;
            myTotalBytesRead =-1;

            //myDataChannelList = new LinkedList<DataChannelObject>();
        }

        public ControlChannelObject(int aControlChannelId, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aDataChannelId, int aChannelType, ChannelHandlerContext aChannelCtx){
            myControlChannelId = aControlChannelId;
            connectAckMsgReceived = false;
            myStartTimeSet = false;
            myEndTimeSet = false;
            myTotalBytesReadSet = false;
            myPreviousStartTime = -1;
            myPreviousEndTime = -1;
            myPreviousTotalBytesRead = -1;
            myStartTime = -1;
            myEndTime = -1;
            myTotalBytesRead =-1;
            //Create File ID List
            myFileIdList = new ArrayList<String>();
            if (aChannelType == CONTROL_CHANNEL_TYPE) {
                myControlChannelCtx = aChannelCtx;
                myFileSenderControlHandler = aFileSenderControlChannelHandler;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
                }
            }
            else { //This is a data Channel
                myFileSenderControlHandler = null;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
                    myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx));
                }
                else {
                    //Add the Data Channel to the List
                    myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx));
                }
                myControlChannelCtx = null;
            }
            //myDataChannelList = new LinkedList<DataChannelObject>();

            myFileSenderControlHandler = null;
        }

        //Register the FileId - this is necessary to keep track of pipelining
        public void registerFileId(int aFileId){
            //Add the file ID to the File Id list
            myFileIdList.add(String.valueOf(aFileId));
        }

        public int getFileIdListSize(){
            return myFileIdList.size();
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

        //De-register / remove the FileId - necessary
        public boolean removeFileId(int aFileId){
            boolean removedSuccessfully = myFileIdList.remove(String.valueOf(aFileId));
            return removedSuccessfully;
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

        public void setFileSenderControlHandler(FileSenderControlChannelHandler aFileSenderControlHandler) {
            myFileSenderControlHandler = aFileSenderControlHandler;
        }

        public FileSenderControlChannelHandler getFileSenderControlChannelHandler(){
            return myFileSenderControlHandler;
        }

        //setConnectMsgAck
        public void setConnectAckMsgReceivedFlag(boolean aVal){
            connectAckMsgReceived = aVal;
        }

        public void addDataChannelObject(FileSender.DataChannelObject aDataChannelObject){
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

        public List<FileSender.DataChannelObject> getDataChannelObjectList(){
            return myDataChannelList;
        }

        public void setDataChannelObjectList(List<FileSender.DataChannelObject> aDataChannelList){

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
                List<FileSender.DataChannelObject> theDataChannelObjectList = this.getDataChannelObjectList();
                if (theDataChannelObjectList != null) {
                    if (theDataChannelObjectList.size() > 0) {
                        //Iterate through the Data Channel Object
                        Iterator<FileSender.DataChannelObject> dataChannelObjectIterator = theDataChannelObjectList.iterator();
                        while (dataChannelObjectIterator.hasNext()) {
                            FileSender.DataChannelObject theDataChannelObject = dataChannelObjectIterator.next();
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

    public static class TempPathObject{
        String ipAddressWithoutSrc;
        String aliasPathName;
        int concurrencyNum;
        int parallelNum;
        int pipelineNum;

        public TempPathObject(String anIpAddressWithoutSrc, String anAliasPathName, int aConcurrencyNum, int aParallelNum, int aPiplelineNum){
            ipAddressWithoutSrc = anIpAddressWithoutSrc;
            aliasPathName = anAliasPathName;
            concurrencyNum = aConcurrencyNum;
            parallelNum = aParallelNum;
            pipelineNum = aPiplelineNum;
        }


        public String getAliasPathName() {
            return aliasPathName;
        }

        public void setAliasPathName(String aliasPathName) {
            this.aliasPathName = aliasPathName;
        }


        public int getConcurrencyNum() {
            return concurrencyNum;
        }

        public void setConcurrencyNum(int concurrencyNum) {
            this.concurrencyNum = concurrencyNum;
        }

        public int getParallelNum() {
            return parallelNum;
        }

        public void setParallelNum(int parallelNum) {
            this.parallelNum = parallelNum;
        }


        public int getPipelineNum() {
            return pipelineNum;
        }

        public void setPipelineNum(int pipelineNum) {
            this.pipelineNum = pipelineNum;
        }


        public String getIpAddressWithoutSrc() {
            return ipAddressWithoutSrc;
        }

        public void setIpAddressWithoutSrc(String ipAddressWithoutSrc) {
            this.ipAddressWithoutSrc = ipAddressWithoutSrc;
        }

    } //End TempPathObject


    public synchronized static void registerChannelCtx(String aPathAliasName, FileSenderControlChannelHandler aFileSenderControlChannelHandler, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, FileSenderDataChannelHandler aFileSenderDataChannelHandler){
        try {
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                if (!myRegisteredChannelsCtx.containsKey(aPathAliasName)) {
                    myRegisteredChannelsCtx.put(aPathAliasName, new HashMap<String, FileSender.ControlChannelObject>());
                }
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                //return myHashMap2;

                if (!myControlChannelObjectMap.containsKey( String.valueOf(aControlChannelId) ) ) {

                    //If the ControlObject Doesn't exist - Add the ControlChannelCTX or the DataChannel CTX
                    myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileSender.ControlChannelObject(aControlChannelId, aFileSenderControlChannelHandler, aDataChannelId, aChannelType, aChannelCtx));

                }
                else {
                    //a Control Channel Object exist with this Control Channel ID already
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        //Add the Control ChannelCTX to the Control Object
                        myControlChannelObject.setControlChannel(aChannelCtx);
                    }
                    else{
                        //Add the Data ChannelCTX
                        myControlChannelObject.addDataChannelObject(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx, aFileSenderDataChannelHandler));
                    }
                }
            }

        }catch(Exception e){
            System.err.printf("RegisterChannel Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized static String registeredChannelsToString(){
        try {
            //public HashMap<String,HashMap<String,ControlChannelObject>> myRegisteredChannels;
            String StringToPrint = "";
            Iterator<Map.Entry<String,HashMap<String,FileSender.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                Map.Entry<String, HashMap<String, FileSender.ControlChannelObject>> aPathEntry = pathIterator.next();
                //Get the Control Channel HashMap belonging to this path
                HashMap<String, FileSender.ControlChannelObject> myControlChannelHashMap = aPathEntry.getValue();
                String theAliasPath = aPathEntry.getKey();
                StringToPrint=StringToPrint+"["+theAliasPath+"]: ";
                //Iterate through the control channel hashMap associated with the above path
                Iterator<Map.Entry<String, FileSender.ControlChannelObject>> controlChannelIterator = myControlChannelHashMap.entrySet().iterator();
                while (controlChannelIterator.hasNext()) {
                    Map.Entry<String, FileSender.ControlChannelObject> aControlChannelEntry = controlChannelIterator.next();
                    String theControlChannelIdString = aControlChannelEntry.getKey();
                    StringToPrint=StringToPrint+"\n"+"  Control Channel Id: "+ theControlChannelIdString;

                    FileSender.ControlChannelObject theControlChannelObject = aControlChannelEntry.getValue();
                    //Get the data object list and iterate through it
                    List<FileSender.DataChannelObject> theDataChannelObjectList = theControlChannelObject.getDataChannelObjectList();

                    Iterator<FileSender.DataChannelObject> theDataChannelListIterator = theDataChannelObjectList.iterator();
                    FileSender.DataChannelObject theDataChannelObject = null;
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

    /*
      I can do either two things: return the Control Channels for this specific path if all control channels are connected
      Or I can return the control channels for ALL the paths if all the control channels for every single path is connected
       Right now I am returning for every single path, but each path has it's own event loop so  I have to test to see if getting references to
       the FileSenderControlHandlers and starting the FileSender method will work. The whole point of doing this is so I can start sending files at
       the same time.
       I also have to test to see if

       Right Now I am returning ALL Control Channels, if ALL control Channels are connected
       If ALL Control Channels are not connected, I return NULL
     */

    /*
    public synchronized static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> registerConnectionAckAndReturnChannels(String aPathAliasName, int aControlChannelId){
        try {
            String StringToPrint = "";
            boolean allControlChannelsConnected = false;
            HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myControlChannelsInAllPaths = null;
            String myCurrentRegisteredChannels = FileSender.registeredChannelsToString();
            logger.info("FileSender: registerConnectionAck: registerConnectionAck METHOD ENTERED FOR PATH: " + aPathAliasName + "AND CONTROL CHANNEL: " + aControlChannelId + " CURRENT REGISTERED CHANNELS ARE: " + myCurrentRegisteredChannels );
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                logger.info("FileSender: registerConnectionAck Before calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                logger.info("FileSender: registerConnectionAck after calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                //All Control Channels have been registered already
                if (myControlChannelObjectMap != null ) {
                    logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                    //a Control Channel Object exist with this Control Channel ID already
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));

                    if (myControlChannelObject != null ){
                        logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                        myControlChannelObject.setConnectAckMsgReceivedFlag(true);
                    }
                }
                allControlChannelsConnected = true;
                //Check to see if ALL CONTROL CHANNELS are Connected
                Iterator<Map.Entry<String,HashMap<String,FileSender.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
                //Iterate through each path in the HashMap
                while (pathIterator.hasNext()) {
                    Map.Entry<String, HashMap<String, FileSender.ControlChannelObject>> aPathEntry = pathIterator.next();
                    //Get the Control Channel HashMap belonging to this path
                    HashMap<String, FileSender.ControlChannelObject> myControlChannelHashMap = aPathEntry.getValue();
                    String theAliasPath = aPathEntry.getKey();
                    StringToPrint=StringToPrint+"["+theAliasPath+"]: ";
                    //Iterate through the control channel hashMap associated with the above path
                    Iterator<Map.Entry<String, FileSender.ControlChannelObject>> controlChannelIterator = myControlChannelHashMap.entrySet().iterator();
                    while ( (controlChannelIterator.hasNext()) && (allControlChannelsConnected == true)) {
                        Map.Entry<String, FileSender.ControlChannelObject> aControlChannelEntry = controlChannelIterator.next();
                        String theControlChannelIdString = aControlChannelEntry.getKey();
                        StringToPrint=StringToPrint+"\n"+"  Control Channel Id: "+ theControlChannelIdString;

                        FileSender.ControlChannelObject theControlChannelObject = aControlChannelEntry.getValue();
                        if (theControlChannelObject.getConnectMsgReceivedFlag() == false ){
                            allControlChannelsConnected = false;
                            break;
                        }
                    }//End iterating over the control channels per path
                    if (allControlChannelsConnected == false)
                        break;
                }//End iterating over the paths
                if (allControlChannelsConnected == true) {
                    myControlChannelsInAllPaths = FileSender.myRegisteredChannelsCtx;
                }
                return myControlChannelsInAllPaths;
            }//End if aPathAliasName == null
            //Check to see if all Channels are connected



        }catch(Exception e){
            System.err.printf("RegisterConnectionAck Error: " + e.getMessage() + " FOR PATH: " + aPathAliasName + "AND CONTROL CHANNEL: " + aControlChannelId );
            e.printStackTrace();
        }
    }

    */

    public synchronized static void registerConnectionAck(String aPathAliasName, int aControlChannelId){
        try {
            String myCurrentRegisteredChannels = FileSender.registeredChannelsToString();
            //logger.info("FileSender: registerConnectionAck: registerConnectionAck METHOD ENTERED FOR PATH: " + aPathAliasName + "AND CONTROL CHANNEL: " + aControlChannelId + " CURRENT REGISTERED CHANNELS ARE: " + myCurrentRegisteredChannels );
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                //logger.info("FileSender: registerConnectionAck Before calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                //logger.info("FileSender: registerConnectionAck after calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                //All Control Channels have been registered already
                if (myControlChannelObjectMap != null ) {
                    //logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                    //a Control Channel Object exist with this Control Channel ID already
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));

                    if (myControlChannelObject != null ){
                        //logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                        myControlChannelObject.setConnectAckMsgReceivedFlag(true);
                    }
                }
            }//End if aPathAliasName == null
            //logger.info("******************* Registered Channels in string format *************** = " + registeredChannelsToString());
            //System.err.printf("******************* Registered Channels in string format *************** = %s",registeredChannelsToString());
        }catch(Exception e){
            System.err.printf("RegisterConnectionAck Error: " + e.getMessage() + " FOR PATH: " + aPathAliasName + "AND CONTROL CHANNEL: " + aControlChannelId );
            e.printStackTrace();
        }
    }


    public synchronized static String getConnectedChannelAckIDsInStringFormat(String aPathAliasName, int aControlChannelId){
        try {
            //public HashMap<String,HashMap<String,ControlChannelObject>> myRegisteredChannels;
            String StringToPrint = "";

            if ( aPathAliasName != null) {
                //Add the Alias Path to the String
                StringToPrint = StringToPrint + aPathAliasName + ": ";
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                if (myControlChannelObjectMap != null) {
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    if (myControlChannelObject != null) {
                        //Add the Control Channel Id to the String
                        StringToPrint = StringToPrint + "\n" + "  Control Channel Id: " + myControlChannelObject.getControlChannelId() + " Connected";
                        List<FileSender.DataChannelObject> theDataChannelObjectList = myControlChannelObject.getDataChannelObjectList();
                        if (theDataChannelObjectList != null) {
                            if (theDataChannelObjectList.size() > 0) {
                                //Iterate through the Data Channel Object
                                Iterator<FileSender.DataChannelObject> dataChannelObjectIterator = theDataChannelObjectList.iterator();
                                while (dataChannelObjectIterator.hasNext()) {
                                    FileSender.DataChannelObject theDataChannelObject = dataChannelObjectIterator.next();
                                    //Add the Data Channel to the string
                                    StringToPrint = StringToPrint + "\n" + "    Data Channel Id: " + theDataChannelObject.getDataChannelId() + " Connected";
                                }//end while
                            }//End (theDataChannelObjectList.size() > 0)
                        }//End (theDataChannelObjectList != null)
                    }//End myControlChannelObject != null
                }//myControlChannelObjectMap != null
            }//End aPathAliasName != null

            return StringToPrint;

        }catch(Exception e){
            System.err.printf(" ServerHandlerHelper:getConnectedChannelIdsInStringFormat Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }// End getConnectedChannelIDsInStringFormat

    public synchronized static FileSender.ControlChannelObject getControlChannelObject(String anAliasPathString, int aControlChannelId){
        try {

            FileSender.ControlChannelObject myControlChannelObject = null;

            //Check to see if the alias path exist, if not add path to the HashMap
            if ( anAliasPathString  != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(anAliasPathString);
                if (myControlChannelObjectMap != null) {
                    myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                }
            }//End if aPathAliasName == null
            return myControlChannelObject;
        }catch(Exception e){
            System.err.printf("getControlChannelObject Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public synchronized static List<FileSender.DataChannelObject> getDataChannelObjectList(String aPathAliasName, int aControlChannelId){
        try {

            List<FileSender.DataChannelObject> aDataChannelObjectList = null;

            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);

                if (myControlChannelObjectMap != null) {
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    //get the Data Channel List
                    aDataChannelObjectList = myControlChannelObject.getDataChannelObjectList();
                }

            }//End if aPathAliasName == null
            return aDataChannelObjectList;
        }catch(Exception e){
            System.err.printf("getDataChannelObjectList Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public synchronized static void addFileRequestToPathList(String anAliasPath, String aFileRequest){
        try{

            ArrayList<String> aFileRequestList = null;

            if (anAliasPath !=null) {
                aFileRequestList = FileSender.myPathAndFileRequestList.get(anAliasPath);
                if (aFileRequestList != null) {
                    aFileRequestList.add(aFileRequest);
                    //logger.info("FileSender: addFileRequestToPathList: added file request: " + aFileRequest + " to the path: " + anAliasPath);

                }
                /*
                else {
                    logger.info("FileSender: addFileRequestToPathList: aFileRequest = NULL");
                }
                */
            }

        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();

        }
    }

    public synchronized static String getNextFileRequestFromList(String anAliasPath){
        try {
            logger.info("FileSender: getNextFileRequestFromList Method Entered");
            String aFileRequest = null;
            ArrayList<String> aFileRequestList = null;

            if (anAliasPath !=null) {
                if (myPathAndFileRequestList == null) {
                    logger.info("FileSender: getNextFileRequestFromList: myPathAndFileRequestList == null");
                } else {
                    aFileRequestList = myPathAndFileRequestList.get(anAliasPath);
                    if (aFileRequestList != null) {
                        if (!aFileRequestList.isEmpty()) {
                            aFileRequest = aFileRequestList.remove(0);
                        }
                        /*
                        else {
                            logger.info("FileSender: getNextFileRequestFromList: aFileRequestList is Empty");
                        }
                        */
                    }
                    /*
                    else {
                        logger.info("FileSender: getNextFileRequestFromList: aFileRequest = NULL");
                    }
                    */
                } //End else
            } //End if (anAliasPath !=null)
            /*
            else {
                logger.info("FileSender: getNextFileRequestFromList: anAliasPath = NULL");
            }
            */
            //logger.info("getNextFileRequest: FileRequest = " + aFileRequest);
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //public synchronized static void createFileRequestListPerPath(PathList aThreadPathList, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
    public synchronized static void createFileRequestListPerPath(String aPathName){
        try{
            //Iterate through the path List
            //Path theHead2 = aThreadPathList.getHead();
            //while ( theHead2 != null){
                //Create the Path and it's File Request List
                //aPathAndFileRequestList.put(theHead2.toStringAliasNames(),new ArrayList<String>());
                //aPathAndFileRequestList.put(aPathName, new ArrayList<String>());
                myPathAndFileRequestList.put(aPathName, new ArrayList<String>());
                //logger.info("createFileRequestListPerPath: Created new File Request List for path: "+theHead2.toStringAliasNames());
                //logger.info("createFileRequestListPerPath: Created new File Request List for path: "+ aPathName);
                //theHead2 = theHead2.next;
            //}
            //For each path, add the file transfer parameters
        }catch(Exception e){
            System.err.printf("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public synchronized static String getNextFileRequestFromList(String theChannelType, String anAliasPath){
        try {
            logger.info("("+ theChannelType +") FileSender: getNextFileRequestFromList Method Entered");
            String aFileRequest = null;
            ArrayList<String> aFileRequestList = null;

            if (anAliasPath !=null) {
                if (myPathAndFileRequestList == null) {
                    logger.info("("+ theChannelType +") FileSender: getNextFileRequestFromList: myPathAndFileRequestList == null");
                } else {
                    //if (myPathAndFileRequestList != null){
                    aFileRequestList = myPathAndFileRequestList.get(anAliasPath);
                    if (aFileRequestList != null) {
                        if (!aFileRequestList.isEmpty()) {
                            aFileRequest = aFileRequestList.remove(0);
                        }
                        /*
                        else {
                            logger.info("("+ theChannelType +") FileSender: getNextFileRequestFromList: aFileRequestList is Empty");
                        }
                        */
                    }
                    /*
                    else {
                        logger.info("("+ theChannelType + ") FileSender: getNextFileRequestFromList: aFileRequest = NULL");
                    }
                    */
                } //End else
            } //End if (anAliasPath !=null)
            /*
            else {
                logger.info("("+ theChannelType + ") FileSender: getNextFileRequestFromList: anAliasPath = NULL");
            }
            */
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //public synchronized static void createFileRequestListPerPath(PathList aThreadPathList, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
    public synchronized static void createFileRequestListPerPath(){
        try{
            //Iterate through the path List
            for (FileSender.TempPathObject aTempPathObject : tempPathList){
                //Create a FileRequest List for this path
                myPathAndFileRequestList.put(aTempPathObject.getAliasPathName(), new ArrayList<String>());
            }
        }catch(Exception e){
            System.err.printf("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //aFileRequestList - List of file requests
    //public synchronized static void runFileDistributionAlg(String aFileDistributionAlgString,HashMap<String, ArrayList<String>> aPathAndFileRequestList,ArrayList<String> aFileRequestList ){
    public synchronized static void runFileDistributionAlg(String aFileDistributionAlgString){
        logger.info("runFileDistributionAlg: Inside the runFileDistributionAlg  ");
        //Get Algorithm
        boolean done = false;
        //If File Distribution Algorithm is round robin
        if (aFileDistributionAlgString.equalsIgnoreCase("rr")) {
            logger.info("runFileDistributionAlg: The File Distribution Algorithm = Round Robin");
            //Iterate through the Hash Map Entry (Specifically each path's File Request List), retrieve the file request from the main file request list and
            //put it in the Path's File Request list
            if (!myPathAndFileRequestList.isEmpty()){
                //logger.info("runFileDistributionAlg: The Path and File Request List is NOT EMPTY");
                //Iterator<String> aFileRequestIterator = aFileRequestList.iterator();
                Iterator<String> aFileRequestIterator = mainFileRequestList.iterator();
                //Iterate through the list of paths,
                while (!done) {
                    Iterator<Map.Entry<String, ArrayList<String>>> iterator = myPathAndFileRequestList.entrySet().iterator();
                    //Iterate through each path in the path List
                    while (iterator.hasNext()) {
                        Map.Entry<String, ArrayList<String>> aPathAndFileRequestListEntry = iterator.next();
                        ArrayList<String> thePathFileRequestList = aPathAndFileRequestListEntry.getValue();
                        //Check to see if the main file request list has another file request
                        if (aFileRequestIterator.hasNext()) {
                            String theFileRequest = aFileRequestIterator.next();
                            //Add the File Reuest List to the Path's File Request List
                            thePathFileRequestList.add(theFileRequest);
                            logger.info("runFileDistributionAlg: Path: "+ aPathAndFileRequestListEntry.getKey() + " Added the FileRquest: " + theFileRequest);
                        } else {
                            done = true;
                        }
                    }
                }

            }//End aPathAndFileRequestList.isEmpty()
            else {
                logger.info("runFileDistributionAlg: the Path And File Request List is Empty ");
            }
        }//aFileDistributionAlgString.equalsIgnoreCase("rr")
    } //End Method

    public void startFileSender() throws InvocationTargetException, Exception{
        try {//////////////////////////////
            long threadId = Thread.currentThread().getId();
            //logger.info("******************************************************");
            //logger.info("FileSender:startFileSender ThreadId = " + threadId);
            //logger.info("******************************************************");

            /*
              Should each path have it's own Event Loop Group ?

             */
            ///////////////////////////////////////////////////
            //                                               //
            //  ITERATE THROUGH THE TEMP PATH LIST            //
            //  EACH PATH WILL HAVE IT'S OWN EVENT LOOP GROUP //
            //                                               //
            //////////////////////////////////////////////////

            for (FileSender.TempPathObject aTempPathObject: FileSender.tempPathList) {

                // Configure the File Sender
                EventLoopGroup group = new NioEventLoopGroup();

                //Create the parallel data Channels
                int controlChannelId = 0;
                int dataChannelId = -1;

                String remoteHost = null;
                int remotePort = -1;

                //See if I am Connecting to a Proxy Server or a Receiver, based on the ipAddress string path: note the ip address string path doesn't contain the source node
                //if the Path consist of more than one node, it will separate the nodes by a comma: 192.168.0.1:4959, 192.168.0.2:4959, this means we are connecting to a proxy server
                //if the path just consists of one node we are connecting to a receiver, since there will be no commas separating the ip addresses: example: 192.168.0.2:4959
                //
                if (aTempPathObject.getIpAddressWithoutSrc().indexOf(",") > 0) {
                    //There is more than one node, we are connecting to a proxy server
                    //192.168.0.1:4959,192.168.0.2:4959
                    String[] tokens = aTempPathObject.getIpAddressWithoutSrc().split("[,]+");
                    //logger.info("FileSender: StartFileSender: tokens[0] = " + tokens[0]);
                    //logger.info("FileSender: StartFileSender: tokens[1] = " + tokens[1]);
                    String theNodeToConnectTo = tokens[0]; // = 192.168.0.1:4959
                    //Separate the ip address from the port
                    String[] ip_and_port = theNodeToConnectTo.split("[:]+");
                    //logger.info("FileSender:StartFileSender: ip_and_port[0] = " + ip_and_port[0]);
                    //logger.info("FileSender:StartFileSender: ip_and_port[1]= " + ip_and_port[1]);
                    remoteHost = ip_and_port[0]; //"192.168.0.1"
                    //logger.info("FileSender:StartFileSender: Remote Host = " + remoteHost);
                    remotePort = new Integer(ip_and_port[1]).intValue(); //=4959
                    //logger.info("FileSender:StartFileSender: Remote Port = " + remotePort);

                }
                else {
                    //There is only one node, we are connecting directly to the receiver
                    //192.168.0.1:4959
                    String theNodeToConnectTo = aTempPathObject.getIpAddressWithoutSrc();
                    //Separate the ip address from the port
                    String[] ip_and_port = theNodeToConnectTo.split("[:]+");
                    //logger.info("FileSender:StartFileSender: ip_and_port[0] = " + ip_and_port[0]);
                    //logger.info("FileSender:StartFileSender: ip_and_port[1]= " + ip_and_port[1]);
                    remoteHost = ip_and_port[0]; //"192.168.0.1"
                    //logger.info("FileSender:StartFileSender: Remote Host = " + remoteHost);
                    remotePort = new Integer(ip_and_port[1]).intValue(); //=4959
                    //logger.info("FileSender:StartFileSender: Remote Port = " + remotePort);
                }



                /*
                myConcurrencyNum = 1;
                myParallelNum = 2;
                myPipelineNum = 1;
                String pathString = "WS5,WS7,WS12";
                */

                ///////////////////////////////
                //Connect the Control Channel
                //////////////////////////////
                for (int i = 0; i < aTempPathObject.getConcurrencyNum(); i++) {
                    controlChannelId = i + 1;
                    dataChannelId = -1;
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .handler(new FileSenderInitializerForControlChannel(aTempPathObject.getIpAddressWithoutSrc(),aTempPathObject.getAliasPathName(), CONTROL_CHANNEL_TYPE, controlChannelId, dataChannelId, this, aTempPathObject.getConcurrencyNum(), aTempPathObject.getParallelNum(),aTempPathObject.getPipelineNum() ));

                    //Actually connect to the proxyServer and use sync to wait until the connection is completed, but this isn't really asynchronous until we use listeners to wait, since it is blocking
                    //Using "sync" blocks until the channel is connected.
                    //ChannelFuture controlChannelFuture = b.connect(HOST, PORT).sync();
                    ChannelFuture controlChannelFuture = b.connect(remoteHost,remotePort ).sync();
                    channelFutureList.add(controlChannelFuture);
                    staticChannelFutureList.add(controlChannelFuture);
                    //logger.info("*******FileSender: StartFileSender: Connected Control Channel id: " + controlChannelId);
                    //System.err.printf("\n******FileSender: StartFileSender: Connected Control Channel id: %s \n\n", controlChannelId);

                    ///////////////////////////////
                    //Connect the Data Channel
                    //////////////////////////////


                    for (int j = 0; j < aTempPathObject.getParallelNum(); j++) {
                        dataChannelId = j + 1;
                        //For all channels maybe I can use just one BootStrap or copy one
                        Bootstrap b1 = new Bootstrap();
                        b1.group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.TCP_NODELAY, true)
                                .handler(new FileSenderInitializerForDataChannel(aTempPathObject.getIpAddressWithoutSrc(),aTempPathObject.getAliasPathName(), DATA_CHANNEL_TYPE, controlChannelId, dataChannelId, this, aTempPathObject.getConcurrencyNum(), aTempPathObject.getParallelNum()));

                        //Actually connect to the proxyServer and use sync to wait until the connection is completed
                        //ChannelFuture dataChannelFuture = b1.connect(HOST, PORT).sync();
                        ChannelFuture dataChannelFuture = b1.connect(remoteHost,remotePort).sync();
                        channelFutureList.add(dataChannelFuture);
                        staticChannelFutureList.add(dataChannelFuture);
                        //logger.info("*********FileSender: StartFileSender: Connected Data Channel id: " + dataChannelId + " For Control Channel" + controlChannelId);
                        //System.err.printf("\n****** FileSender: StartFileSender: Connected Data Channel id: %d for Control Channel ID: %d \n\n", dataChannelId, controlChannelId);
                    }

                }
            }//End Iterating through the path list


            //Iterator<ChannelFuture> channelFutureListIterator = channelFutureList.iterator();

            //Iterate through File Request List
            int counter = 1;

            //Iterate through the channel list: Make all channels stay up and block until they are closed
            //or do: For each ChannelFuture in channelFutureList
            // for (ChannelFuture aChannelFuture: channelFutureList) {
            //    aChannelFuture.channel().closeFuture().sync();
            //}
            for (ChannelFuture aChannelFuture : channelFutureList) {
                aChannelFuture.channel().closeFuture().sync();
                logger.info("Waiting for channel " + counter + " to close");
                counter++;
            }
            /*
            while (channelFutureListIterator.hasNext()) {
               ChannelFuture aChannelFuture = channelFutureListIterator.next();

                //Keep the connection up, until I press control c to close it or unitl I actually write channel().close() in the handler Wait until the connection is closed
                aChannelFuture.channel().closeFuture().sync();
                //aChannelFuture.channel().closeFuture();
                //Add the File Reuest List to the Path's File Request List
                logger.info("Waiting for data channel " + counter + " to close");
                counter++;
            }
            */
            logger.info("FileSender: CLOSED CONNECTION TO WS7");

        }finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
            logger.info("Channels were closed");
        }


    }

    //This method is static so other classes can call this method without having an instance of the FileSender Class
    //Static methods belong to the class and not to objects (instances) of the class, so static methods can be called using the class name like
    //FileSender.reportThroughput()
    public synchronized void reportThroughput(int channelId, long startTimeRead, long endTimeRead, long bytesRead) throws Exception {
        try {
            ThroughputObject aThroughputObject = this.new ThroughputObject(channelId, startTimeRead, endTimeRead, bytesRead);
            throughputObjectList.add(aThroughputObject);
        }catch(Exception e) {
            System.err.printf("FileSender: Report Throughput Error: %s \n ", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }//End Catch
    }

    public synchronized String throughputInfoToString() {
        try {
            String StringToPrint = "";
            long startTime = -1;
            long endTime = -1;
            long totalBytesRead = 0;
            double throughput = 0;
            if (!throughputObjectList.isEmpty()) {
                Iterator<ThroughputObject> throughputObjectListIterator = throughputObjectList.iterator();
                //Iterate through the throughput Object List
                while (throughputObjectListIterator.hasNext()) {
                    ThroughputObject theThroughputObject = throughputObjectListIterator.next();
                    //Get the lowest Start Time
                     if (startTime < 0) {
                         startTime = theThroughputObject.getMyStartTime();
                     }else {
                         if (startTime > theThroughputObject.getMyStartTime() ){
                             startTime = theThroughputObject.getMyStartTime();
                         }
                     }
                     //Get the highest end time
                     if (endTime < 0){
                         endTime = theThroughputObject.getEndTime();
                     }
                     else {
                         if (endTime < theThroughputObject.getEndTime()){
                             endTime = theThroughputObject.getEndTime();
                         }
                     }
                     //Add the bytes Read
                    totalBytesRead += theThroughputObject.getBytesRead();
                    //Add the File Reuest List to the Path's File Request List
                    //logger.info("FileSender: Print Throughput = " + theThroughputObject);
                    StringToPrint = StringToPrint + "Channel ID: " + theThroughputObject.getMyChannelId() + ", Start Time: " + theThroughputObject.getMyStartTime() + ", End Time: " + theThroughputObject.getEndTime() + " Throughput: " + convertThroughputToString(theThroughputObject.getThroughput()) + "\n";
                }//End while loop
                //Calculate Throughput
                throughput = (endTime - startTime) / totalBytesRead;
                //Convert Bytes per millisecond to Bytes per second
                throughput = throughput * 1000; //Converted throughput from Bytes per millisecond to Bytes per second
                //Convert Bytes per second (B/s) to bits per second (b/s)
                throughput = throughput * 8;
                StringToPrint = StringToPrint + convertThroughputToString(throughput);
            }//End aPathAndFileRequestList.isEmpty()
            return StringToPrint;
        }catch(Exception e) {
            System.err.printf("FileSender: ThroughputToString Error: %s \n ", e.getMessage());
            e.printStackTrace();
            return null;
            //System.exit(1);
        }//End Catch

    }

    /*
Assumes input is in the bit/sec format, (b/s) the number of bits transferred in a sec
Example input: 5995.00 b/s
returns the throughput as a string with the closest unit, for example:
59992.00 b/s is converted to 59.992 Kb/s
*/
    public synchronized String convertThroughputToString(double bitsPerSecond){
        try {
            //keep dividing by 1000 until we can't divide no more
            double currentBitsPerSecond = bitsPerSecond;
            String theUnit = "b/s";
            String throughputString = null;
            if (currentBitsPerSecond > 0) {
                while (currentBitsPerSecond >= 1000) {
                    switch (theUnit.charAt(0)) {
                        case 'b':
                            currentBitsPerSecond = currentBitsPerSecond / 1000;
                            theUnit = "Kb/s";
                            break;
                        case 'K':
                            currentBitsPerSecond = currentBitsPerSecond / 1000;
                            theUnit = "Mb/s";
                            break;
                        case 'M':
                            currentBitsPerSecond = currentBitsPerSecond / 1000;
                            theUnit = "Gb/s";
                            break;
                        case 'G':
                            currentBitsPerSecond = currentBitsPerSecond / 1000;
                            theUnit = "Tb/s";
                            break;
                        case 'T':
                            currentBitsPerSecond = currentBitsPerSecond / 1000;
                            theUnit = "Pb/s";
                            break;
                        default:
                            break;
                    }//End Switch Statement
                }//End While

            }//End if
            throughputString = "" + currentBitsPerSecond + " " + theUnit;
            return throughputString;
        } catch (Exception e){
            System.err.printf("%s %n",e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        FileSender myFileSender = new FileSender();

        //Add File Request to the main list
        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/5MB_File.dat WS12/home/lrodolph/5MB_File_Copy.dat");
        //FileSender.addFileRequestToList("transfer WS5/home/lrodolph/10MB_File1.dat WS12/home/lrodolph/10MB_File_Copy1.dat");
        //FileSender.addFileRequestToList("transfer WS5/home/lrodolph/10MB_File2.dat WS12/home/lrodolph/10MB_File_Copy2.dat");


        //Add Paths to the Path List: IPAddress:Port,IPAddress:Port without src, Alias Name, Concurrency, Parallel Num, Pipeline Num
        //FileSender.addTempObjectToTempPathList("192.168.0.1:4959,192.168.1.2:4959", "WS5,WS7,WS12", 1, 1, 1);
        FileSender.addTempObjectToTempPathList("192.168.0.1:4959", "WS5,WS7", 1, 1, 1);

        //FileSender.addTempObjectToTempPathList("192.168.2.2:4959,192.168.3.2:4959", "WS5,WS11,WS12", 1, 2, 1);


        //FileSender.createFileRequestListPerPath("WS5,WS7");
        //Create a FileRequest List for each TempPathObject in the Temp Path List
        FileSender.createFileRequestListPerPath();

        FileSender.addFileRequestToPathList("WS5,WS7,WS12","transfer WS5/home/lrodolph/5MB_File.dat WS12/home/lrodolph/5MB_File_Copy.dat");

        //Run File Distribution Algorithm
        FileSender.runFileDistributionAlg("rr");

        myFileSender.startFileSender();

        /*
        logger.info(myFileSender.throughputInfoToString());
        if (myFileSender.throughputObjectList.isEmpty()){
            logger.info("Throughput Object List IS EMPTY");

        }
        else {
            logger.info("Throughput Object List IS NOT EMPTY");
        }
        if (staticThroughputObjectList.isEmpty()){
            logger.info("Static Throughput Object List IS EMPTY");
        }
        else {
            logger.info("Static Throughput Object List IS NOT EMPTY");
        }
        */


        //Do Below:
        //FileSender.ThroughputObject aThroughputObject = myFileSender.new ThroughputObject(11, 4, 7, 1024);
        //Add ThroughputObject is a Static Method and Class ThroughputObject is a non-static class

        //FileSender.addThroughputObject(myFileSender.new ThroughputObject(13,7,17,1024));
        //FileSender.addStaticThroughputObject(new FileSender.StaticThroughputObject(13,7,17,1024));

    }
}
