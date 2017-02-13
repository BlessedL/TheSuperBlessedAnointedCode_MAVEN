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
    public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannels = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
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

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
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

    public static class ControlChannelObject{
        int myControlChannelId;
        ChannelHandlerContext myControlChannelCtx;
        List<FileSender.DataChannelObject> myDataChannelList;
        int parallelDataChannelNum;
        boolean connectAckMsgReceived; //For the File Sender
        boolean connectMsgReceived; //For the File Receiver

        public final int CONTROL_CHANNEL_TYPE = 0;
        public final int DATA_CHANNEL_TYPE = 1;

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx, List<DataChannelObject> aDataChannelList ){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = aDataChannelList;
            connectAckMsgReceived = false;
        }

        public ControlChannelObject(int aControlChannelId, ChannelHandlerContext aControlChannelCtx){
            myControlChannelId = aControlChannelId;
            myControlChannelCtx = aControlChannelCtx;
            myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
            connectAckMsgReceived = false;
            //myDataChannelList = new LinkedList<DataChannelObject>();
        }

        public ControlChannelObject(int aControlChannelId, int aDataChannelId, int aChannelType, ChannelHandlerContext aChannelCtx){
            myControlChannelId = aControlChannelId;
            connectAckMsgReceived = false;
            if (aChannelType == CONTROL_CHANNEL_TYPE) {
                myControlChannelCtx = aChannelCtx;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
                }
            }
            else { //This is a data Channel
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


    public synchronized static void registerChannelCtx(String aPathAliasName, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId){
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
                    myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileSender.ControlChannelObject(aControlChannelId, aDataChannelId, aChannelType, aChannelCtx));

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
                        myControlChannelObject.addDataChannelObject(new FileSender.DataChannelObject(aDataChannelId,aChannelCtx));
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

    public synchronized static void registerConnectionAck(String aPathAliasName, int aControlChannelId){
        try {
            String myCurrentRegisteredChannels = FileSender.registeredChannelsToString();
            logger.info("FileSender: registerConnectionAck: registerConnectionAck METHOD ENTERED FOR PATH: " + aPathAliasName + "AND CONTROL CHANNEL: " + aControlChannelId + " CURRENT REGISTERED CHANNELS ARE: " + myCurrentRegisteredChannels );
            //Check to see if the path exist, if not add path to the HashMap
            if ( aPathAliasName != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                logger.info("FileSender: registerConnectionAck Before calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);
                logger.info("FileSender: registerConnectionAck after calling HashMap<String, ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName)");
                if (myControlChannelObjectMap != null ) {
                    logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                    //a Control Channel Object exist with this Control Channel ID already
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));

                    if (myControlChannelObject != null ){
                        logger.info("FileSender: registerConnectionAck: myControlChannelObjectMap != null" );
                        myControlChannelObject.setConnectAckMsgReceivedFlag(true);
                    }
                }
            }//End if aPathAliasName == null
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
                        else {
                            logger.info("("+ theChannelType +") FileSender: getNextFileRequestFromList: aFileRequestList is Empty");
                        }
                    } else {
                        logger.info("("+ theChannelType + ") FileSender: getNextFileRequestFromList: aFileRequest = NULL");
                    }
                } //End else
            } //End if (anAliasPath !=null)
            else {
                logger.info("("+ theChannelType + ") FileSender: getNextFileRequestFromList: anAliasPath = NULL");
            }
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //public synchronized static void createFileRequestListPerPath(PathList aThreadPathList, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
    public synchronized static void createFileRequestListPerPath(String aPathName, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
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

    //aFileRequestList - List of file requests
    //public synchronized static void runFileDistributionAlg(String aFileDistributionAlgString,HashMap<String, ArrayList<String>> aPathAndFileRequestList,ArrayList<String> aFileRequestList ){
    public synchronized static void runFileDistributionAlg(String aFileDistributionAlgString, ArrayList<String> aFileRequestList){
        logger.info("runFileDistributionAlg: Inside the runFileDistributionAlg  ");
        //Get Algorithm
        boolean done = false;
        //If File Distribution Algorithm is round robin
        if (aFileDistributionAlgString.equalsIgnoreCase("rr")) {
            logger.info("runFileDistributionAlg: The File Distribution Algorithm = Round Robin");
            //Iterate through the Hash Map Entry (Specifically each path's File Request List), retrieve the file request from the main file request list and
            //put it in the Path's File Request list
            if (!myPathAndFileRequestList.isEmpty()){
                logger.info("runFileDistributionAlg: The Path and File Request List is NOT EMPTY");
                Iterator<String> aFileRequestIterator = aFileRequestList.iterator();
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
            logger.info("******************************************************");
            logger.info("FileSender:startFileSender ThreadId = " + threadId);
            logger.info("******************************************************");

            // Configure the File Sender
            EventLoopGroup group = new NioEventLoopGroup();

            //Create the parallel data Channels
            int controlChannelId = 0;
            int dataChannelId = -1;

            myConcurrencyNum = 1;
            myParallelNum = 1;
            String pathString = "WS5,WS7";
            ///////////////////////////////
            //Connect the Control Channel
            //////////////////////////////
            for (int i = 0; i < myConcurrencyNum; i++) {
                controlChannelId = i + 1;
                dataChannelId = -1;
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new FileSenderInitializerForControlChannel(pathString, this.CONTROL_CHANNEL_TYPE, controlChannelId, dataChannelId, this, myConcurrencyNum, myParallelNum));

                //Actually connect to the proxyServer and use sync to wait until the connection is completed, but this isn't really asynchronous until we use listeners to wait, since it is blocking
                //Using "sync" blocks until the channel is connected.
                ChannelFuture controlChannelFuture = b.connect(HOST, PORT).sync();
                channelFutureList.add(controlChannelFuture);
                staticChannelFutureList.add(controlChannelFuture);

                ///////////////////////////////
                //Connect the Data Channel
                //////////////////////////////


                for (int j = 0; j < myParallelNum; j++) {
                    dataChannelId = j + 1;
                    //For all channels maybe I can use just one BootStrap or copy one
                    Bootstrap b1 = new Bootstrap();
                    b1.group(group)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .handler(new FileSenderInitializerForDataChannel(pathString, this.DATA_CHANNEL_TYPE, controlChannelId, dataChannelId, this, myConcurrencyNum, myParallelNum));

                    //Actually connect to the proxyServer and use sync to wait until the connection is completed
                    ChannelFuture dataChannelFuture = b.connect(HOST, PORT).sync();
                    channelFutureList.add(dataChannelFuture);
                    staticChannelFutureList.add(dataChannelFuture);
                }

            }


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
        myFileSender.startFileSender();
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

        //Do Below:
        //FileSender.ThroughputObject aThroughputObject = myFileSender.new ThroughputObject(11, 4, 7, 1024);
        //Add ThroughputObject is a Static Method and Class ThroughputObject is a non-static class

        //FileSender.addThroughputObject(myFileSender.new ThroughputObject(13,7,17,1024));
        //FileSender.addStaticThroughputObject(new FileSender.StaticThroughputObject(13,7,17,1024));

    }
}
