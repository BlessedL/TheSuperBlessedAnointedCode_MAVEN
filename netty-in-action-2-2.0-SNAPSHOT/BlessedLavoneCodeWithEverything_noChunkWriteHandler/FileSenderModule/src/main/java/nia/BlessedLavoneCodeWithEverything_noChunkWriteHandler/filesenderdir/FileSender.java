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
package nia.BlessedLavoneCodeWithEverything_noChunkWriteHandler.filesenderdir;

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
import java.util.Collections;


/*
  Write contents to a file when reading, after reading length amount of bytes
 */
public final class FileSender {

    //Note the String in the 2nd HashMap of myRegisteredChannels is really the ControlChannelObject ID converted to the String
    //  myRegisteredChannels = HashMap<Path, <Control Channel ID, Control Channel Object>>
    public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannels = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
    //  myRegisteredChannelsCtx = HashMap<Path, <Control Channel ID, Control Channel Object>>
    public static HashMap<String,HashMap<String,FileSender.ControlChannelObject>> myRegisteredChannelsCtx = new HashMap<String,HashMap<String,FileSender.ControlChannelObject>>();
    //            PathName, FileRequestList
    static HashMap<String, ArrayList<String>> myPathAndFileRequestList = new HashMap<String, ArrayList<String>>();
    //                    Path,     Control Channel Id, FileRequestList
    //public static HashMap<String,HashMap<String,ArrayList<String>>> myRegisteredChannelsCtx = new HashMap<String,HashMap<String,ArrayList<String>>>();
    //static HashMap<String, List<String>> myPathAndFileRequestList = new HashMap<String, List<String>>();

    public List<ThroughputObject> throughputObjectList;

    static List<StaticThroughputObject> staticThroughputObjectList = new ArrayList<StaticThroughputObject>();
    static List<ChannelFuture> staticChannelFutureList = new ArrayList<ChannelFuture>();
    //main FileRequest List
    static ArrayList<String> mainFileRequestList = new ArrayList<String>();
    static ArrayList<FileSender.TempPathObject> tempPathList = new ArrayList<FileSender.TempPathObject>();
    static ArrayList<FileSender.PathDoneObject> pathDoneList = new ArrayList<FileSender.PathDoneObject>();
    static List<FileSender.ConcurrencyControlChannelObject> concurrencyControlChannelObjectList = new ArrayList<FileSender.ConcurrencyControlChannelObject>();



    //static final String HOST = System.getProperty("host", "127.0.0.1");
    //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

  static final String HOST = System.getProperty("host", "192.168.0.1");
    //static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));
    static final int PORT = Integer.parseInt(System.getProperty("port","4959"));
    static final Logger logger = Logger.getLogger(FileSender.class.getName());
    //Logger logger = Logger.getLogger(this.getClass().getName());
    //public List<ThroughputObject> throughputObjectList = new ArrayList<ThroughputObject>();




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

    public static void addPathDoneObjectToPathDoneList(String anAliasPathName, int aConcurrencyNum){
        FileSender.pathDoneList.add(new FileSender.PathDoneObject(anAliasPathName,aConcurrencyNum));
    }

    //Find the PathDoneObject with this path Alias String
    //decrement/decrease the concurrency count by 1
    //if the new concurrency count = 0 for this path object
    //remove the path object
    //After removing the path object, if the PathObjectList is now empty, size = 0
    //return true, meaning print the throughput for all paths and control channels

    //----public synchronized static boolean reportControlChannelDone(String anAliasPathName) {
    public static boolean reportControlChannelDone(String anAliasPathName) {
        try {
            //Find the PathDoneObject with this path Alias String
            int counter = 0;
            boolean found = false;
            boolean printThroughputForAllPaths = false;
            if (anAliasPathName != null) {
                Iterator<FileSender.PathDoneObject> myIterator = FileSender.pathDoneList.iterator();
                while (!found && myIterator.hasNext()) {
                    FileSender.PathDoneObject aPathDoneObject = myIterator.next();
                    //logger.info("FileSender: ReportControlChannelDone Method: Alias Path Name Parameter = " + anAliasPathName + ", aPathDoneObject.getAliasPathName =  " + aPathDoneObject.getAliasPathName() + ", Size of Path Done List = " + FileSender.pathDoneList.size());
                    if (anAliasPathName.equalsIgnoreCase(aPathDoneObject.getAliasPathName())) {
                        found = true;
                        //logger.info("FileSender: ReportControlChannelDone Method: FOUND PATH, Alias Path Name Parameter = " + anAliasPathName + ", aPathDoneObject.getAliasPathName =  " + aPathDoneObject.getAliasPathName() + " AND CONCURRENCY NUMBER = " + aPathDoneObject.getConcurrencyNum());
                        //decrement/decrease the concurrency count by 1
                        aPathDoneObject.setConcurrencyNum(aPathDoneObject.getConcurrencyNum() - 1);
                        //logger.info("FileSender: ReportControlChannelDone Method: AFTER FINDING AND DECREMENTING CONCCURENCY NUM FOR Alias Path Name Parameter = " + anAliasPathName + ", aPathDoneObject.getAliasPathName =  " + aPathDoneObject.getAliasPathName() + " NEW CONCURRENCY NUMBER = " + aPathDoneObject.getConcurrencyNum());
                        //if the new concurrency count = 0 for this path object
                        if (aPathDoneObject.getConcurrencyNum() == 0) {
                            //logger.info("FileSender: ReportControlChannelDone Method: AFTER FINDING AND DECREMENTING CONCCURENCY NUM FOR Alias Path Name Parameter = " + anAliasPathName + ", aPathDoneObject.getAliasPathName =  " + aPathDoneObject.getAliasPathName() + " NEW CONCURRENCY NUMBER = " + aPathDoneObject.getConcurrencyNum() + " WHICH EQUALS ZERO AND SIZE OF PATH DONE LIST BEFORE REMOVING THE PATH OBJECT = " + FileSender.pathDoneList.size() + ", at position: " + counter );
                            //remove the Path Done object at Counter
                            FileSender.pathDoneList.remove(counter);
                            //logger.info("FileSender: ReportControlChannelDone Method: AFTER FINDING AND DECREMENTING CONCCURENCY NUM FOR Alias Path Name Parameter = " + anAliasPathName + ", aPathDoneObject.getAliasPathName =  " + aPathDoneObject.getAliasPathName() + " NEW CONCURRENCY NUMBER = " + aPathDoneObject.getConcurrencyNum() + " WHICH EQUALS ZERO AND SIZE OF PATH DONE LIST BEFORE REMOVING THE PATH OBJECT = " + FileSender.pathDoneList.size() + ", at position: " + counter );
                        }
                        /*
                        else {
                            logger.info("FileSender: ReportControlChannelDone Method: Path: " + anAliasPathName + " A PATH DONE OBJECT CONCURRENCY NUMBER NOT EQUAL ZERO, BUT EQUALS " +  aPathDoneObject.getConcurrencyNum() );
                        }
                        */
                        //After removing the path object, if the PathObjectList is now empty, size = 0
                        //This means all control channesl have received all file acks and have no more
                        //files to send
                        if (FileSender.pathDoneList.isEmpty()) {
                            //logger.info("FileSender: ReportControlChannelDone: Path: " + anAliasPathName + " Removed last Path Done Object, Path Done Object List is Empty, PRINT THROUGHPUT OBJECT = TRUE");
                            printThroughputForAllPaths = true;
                            break;
                        }
                        /*
                        else {
                            logger.info("FileSender: ReportControlChannelDone: Path: " + anAliasPathName + " Removed last Path Done Object, Path Done Object List is NOT EMPTY, PRINT THROUGHPUT OBJECT = FALSE");
                        }
                        */
                    }
                    //Increment the counter
                    counter++;
                }
            }
            return printThroughputForAllPaths;
        }catch(Exception e){
            System.err.printf("FileSender: reportControlChannelDone: Error: "+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    //printPathDoneObject(myPathString,myControlChannelId,threadId);
    //----public synchronized static void printPathDoneObject(String aPathString, int aControlChannelId, long threadId) {
    public static void printPathDoneObject(String aPathString, int aControlChannelId, long threadId) {
        try {
            String stringToPrint = "";
            stringToPrint = stringToPrint + "\nPRINTING DONE OBJECT LIST FOR: Path: " + aPathString + ", Control Channel Id: " + aControlChannelId + ", Thread ID: " + threadId + "\n";

            for (FileSender.PathDoneObject aPathDoneObject : FileSender.pathDoneList){
                stringToPrint = stringToPrint + "\n PathDoneObject: Path: " + aPathDoneObject.getAliasPathName() + ", Concurrency Num = " + aPathDoneObject.getConcurrencyNum();
            }
            logger.info(stringToPrint);
        }catch(Exception e){
            System.err.printf("FileSender: reportControlChannelDone: Error: "+e.getMessage());
            e.printStackTrace();

        }
    }


   //registerConnectionAckWithPathDoneObject(myPathString);
   //----public synchronized static void registerConnectionAckWithPathDoneObject(String anAliasPathName) {
   public synchronized static void registerConnectionAckWithPathDoneObject(String anAliasPathName) {
       try {
           //Find the PathDoneObject with this path Alias String
           boolean found = false;
           if (anAliasPathName != null) {
               Iterator<FileSender.PathDoneObject> myIterator = FileSender.pathDoneList.iterator();
               while (!found && myIterator.hasNext()) {
                   FileSender.PathDoneObject aPathDoneObject = myIterator.next();
                   if (anAliasPathName.equalsIgnoreCase(aPathDoneObject.getAliasPathName())) {
                       found = true;
                       //increment the number of registered control channels by one
                       aPathDoneObject.setRegisteredControlChannelsNum(aPathDoneObject.getRegisteredControlChannelsNum() + 1);
                       break;
                   }
               }
           }
       }catch(Exception e){
           System.err.printf("FileSender: reportControlChannelDone: Error: "+e.getMessage());
           e.printStackTrace();
       }
   }

   //didAllControlChannelsReceiveConnectionAck
   //---public synchronized static boolean didAllControlChannelsReceiveConnectionAck() {
   public static boolean didAllControlChannelsReceiveConnectionAck() {
       try {
           //Find the PathDoneObject with this path Alias String
           boolean allPathsReceivedConnectionAck = true;
           Iterator<FileSender.PathDoneObject> myIterator = FileSender.pathDoneList.iterator();
           while (allPathsReceivedConnectionAck && myIterator.hasNext()) {
               FileSender.PathDoneObject aPathDoneObject = myIterator.next();
               //Check to see if all the control channels associated with this path received the connection Ack
               if (aPathDoneObject.getRegisteredControlChannelsNum() != aPathDoneObject.getConcurrencyNum()){
                   allPathsReceivedConnectionAck = false;
                   break;
               }
           }
           return allPathsReceivedConnectionAck;
       }catch(Exception e){
           System.err.printf("FileSender: didAllControlChannelsReceiveConnectionAck: Error: "+e.getMessage());
           e.printStackTrace();
           return false;
       }
   }

    public static void addStaticThroughputObject( FileSender.StaticThroughputObject aThroughputObject)
    {
        staticThroughputObjectList.add(aThroughputObject);
        if (staticThroughputObjectList.size() >= dataChannelCounter){
            //logger.info("staticThroughputObjectList.size(" + staticThroughputObjectList.size() + ") >= dataChannelCounter("+dataChannelCounter+")");
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
        long myThreadId;

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = null;
            myThreadId = -1;
        }

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx, long aThreadId){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = null;
            myThreadId = aThreadId;
        }

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx, FileSenderDataChannelHandler aFileSenderDataChannelHandler){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = aFileSenderDataChannelHandler;
            myThreadId = -1;
        }

        public DataChannelObject(int aDataChannelId, ChannelHandlerContext aDataChannelCtx, FileSenderDataChannelHandler aFileSenderDataChannelHandler, long aThreadId){
            myDataChannelId = aDataChannelId;
            myDataChannelCtx = aDataChannelCtx;
            connectMsgReceived = false;
            myFileSenderDataChannelHandler = aFileSenderDataChannelHandler;
            myThreadId = aThreadId;
        }

        public void setThreadId(long aVal){
            myThreadId = aVal;
        }

        public long getThreadId(){
           return myThreadId;
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
        //FileId are unique numbers converted to a string
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
        long myThreadId;
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

        public ControlChannelObject(int aControlChannelId, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aDataChannelId, int aChannelType, ChannelHandlerContext aChannelCtx, long aThreadId, int aParallelNum){
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
            myFileSenderControlHandler = null;
            myControlChannelCtx = null;
            parallelDataChannelNum = aParallelNum;

            //Create File ID List
            myFileIdList = new ArrayList<String>();
            if (aChannelType == CONTROL_CHANNEL_TYPE) {
                myControlChannelCtx = aChannelCtx;
                myFileSenderControlHandler = aFileSenderControlChannelHandler;
                myThreadId = aThreadId;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
                }
            }
            else { //This is a data Channel
                //myFileSenderControlHandler = null;
                if (myDataChannelList == null) {
                    //Data Channel List for this Control Channel is EMPTY
                    myDataChannelList = new LinkedList<FileSender.DataChannelObject>();
                    //myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx));
                    myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx, aThreadId));

                }
                else {
                    //Add the Data Channel to the List
                    //myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx));
                    myDataChannelList.add(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx, aThreadId));
                }

            }

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

        public long getStartTime(){
            return myStartTime;
        }

        public long getEndTime(){
            return myEndTime;
        }

        public long getTotalBytesRead(){
            return myTotalBytesRead;
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

        public void setThreadId(long aVal){
            myThreadId = aVal;
        }

        public long getThreadId(){
            return myThreadId;
        }

    }

    /*
       This Concurrency Control Channel Object contains a list of
       FileSenderControlChannelHandlers from different paths
       Example: Path 1: cc =2, Path 2: cc = 3, Path 3: cc = 1
       Concurrency Control Channel Object 1:
       ---Path 1: ControlChannelHandler 1,
       ---Path 2: ControlChannelHandler 1,
       ---Path 3: ControlChannelHandler 1
       Concurrency Control Channel Object 2:
       ---Path 1: ControlChannelHandler 2,
       ---Path 2: ControlChannelHandler 2,
       Concurrency Control Channel Object 3:
       ---Path 2: ControlChannelHandler 3,

     */
    public static class ConcurrencyControlChannelObject{

        final int myConcurrencyControlChannelId;
        //List<FileSenderControlChannelHandler> myFileSenderControlChannelHandlerList;
        List<FileSender.ConcurrencyControlChannelAndFileRequest> myControlChannelHandlerAndFileRequestList;
        //Need Path to get File Request
        //List<FileSenderControlChannelHandler>


        public ConcurrencyControlChannelObject(int aConcurrencyControlChannelId ){
            //myFileSenderControlChannelHandlerList = new ArrayList<FileSenderControlChannelHandler>();
            myControlChannelHandlerAndFileRequestList = new ArrayList<FileSender.ConcurrencyControlChannelAndFileRequest>();
            myConcurrencyControlChannelId = aConcurrencyControlChannelId;

        }

        public int getConcurrencyControlChannelId(){
            return myConcurrencyControlChannelId;
        }

        /*
          Add a FileSenderControlChannelHandler to the List
         */
        /*
        public void addFileSenderControlHandlerToList(FileSenderControlChannelHandler aFileSenderControlChannelHandler){
            try{
                myFileSenderControlChannelHandlerList.add(aFileSenderControlChannelHandler);
            }catch(Exception e){
                System.err.printf("addFileSenderControlHandlerToList Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        */

        public void addControlChannelHandlerAndFileRequestToList(ConcurrencyControlChannelAndFileRequest aControlChannelHandlerAndFileRequest){
            try{
                myControlChannelHandlerAndFileRequestList.add(aControlChannelHandlerAndFileRequest);
            }catch(Exception e){
                System.err.printf(" Error: addControlHandlerAndFileRequestToList" + e.getMessage());
                e.printStackTrace();
            }
        }
        /*
          Return FileSenderControlChannelHandler List
         */
        public  List<FileSender.ConcurrencyControlChannelAndFileRequest> getControlChannelHandlerAndFileRequestList(){
            try {
                return myControlChannelHandlerAndFileRequestList;
            }catch(Exception e){
                System.err.printf("getFileSenderControlHandlerList Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }


    }


    /*
      This class holds a FileSenderControlChannelHandler and A fileRequest
      but the fileRequest is already parsed in the form of Src file path, dest filePath
      Also holds the Path String
      Variables:
        FileSenderControlHandler -
        Alias Path String -
        File Request (String) -

     */
    public static class ConcurrencyControlChannelAndFileRequest{

        FileSenderControlChannelHandler myFileSenderControlHandler;
        String myAliasPathString;
        String myFileRequest;
        String mySrcFilePath, myDestFilePath;
        FileChannel theFileChannel;
        long myFileLength;
        long theCurrentFragmentSize;
        long leftOverBytes;
        int myParallelNum;
        File theFile;

        public ConcurrencyControlChannelAndFileRequest(String anAliasPathString, String aFileRequest, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aParallelNum ){
            try {
                this.myAliasPathString = anAliasPathString;
                this.myFileRequest = aFileRequest;
                this.myFileSenderControlHandler = aFileSenderControlChannelHandler;
                this.mySrcFilePath = null;
                this.myDestFilePath = null;
                this.theFile = null;
                this.theFileChannel = null;
                this.myParallelNum = aParallelNum;
                this.theCurrentFragmentSize = -1;
                this.myFileLength = -1;
                this.leftOverBytes = -1;
                this.theCurrentFragmentSize = -1;

                if (myFileRequest != null) {
                    //Parse out the Src File Path and the Dest File Path
                    String[] tokens = myFileRequest.split("[ ]+");

                    //Parse out Source File Path
                    int begginingOfFilePath = tokens[1].indexOf('/');
                    String aliasSource = tokens[1].substring(0, begginingOfFilePath); //-->A
                    mySrcFilePath = tokens[1].substring(begginingOfFilePath, tokens[1].length());

                    //Parse out Destination File Path
                    int begginingOfFilePathForDest = tokens[2].indexOf('/');
                    String aliasDest = tokens[2].substring(0, begginingOfFilePathForDest); //-->C
                    myDestFilePath = tokens[2].substring(begginingOfFilePathForDest, tokens[2].length());

                    //Get the File
                    theFile = new File(mySrcFilePath);
                    FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
                    //Get the File Length
                    myFileLength = theFileChannel.size();

                    //get the file fragment size that will be sent through each parallel TCP Stream
                    theCurrentFragmentSize = Math.round(Math.floor((double) (myFileLength / myParallelNum)));
                    //Get the LeftOverBytes
                    leftOverBytes = myFileLength - (theCurrentFragmentSize * myParallelNum);
                }
            }catch(Exception e){
                System.err.printf("FileSender: ConcurrencyControlChannelAndFileRequest Error: " + e.getMessage());
                e.printStackTrace();
            }

        }


        public String getMyAliasPathString() {
            return myAliasPathString;
        }

        public void setMyAliasPathString(String myAliasPathString) {
            this.myAliasPathString = myAliasPathString;
        }

        public String getMyFileRequest() {
            return myFileRequest;
        }

        public void setMyFileRequest(String myFileRequest) {
            this.myFileRequest = myFileRequest;
        }

        public String getMySrcFilePath() {
            return mySrcFilePath;
        }

        public void setMySrcFilePath(String mySrcFilePath) {
            this.mySrcFilePath = mySrcFilePath;
        }

        public String getMyDestFilePath() {
            return myDestFilePath;
        }

        public void setMyDestFilePath(String myDestFilePath) {
            this.myDestFilePath = myDestFilePath;
        }

        public long getMyFileLength() {
            return myFileLength;
        }

        public void setMyFileLength(long myFileLength) {
            this.myFileLength = myFileLength;
        }

        public File getTheFile() {
            return theFile;
        }

        public void setTheFile(File theFile) {
            this.theFile = theFile;
        }

        public FileChannel getTheFileChannel() {
            return theFileChannel;
        }

        public void setTheFileChannel(FileChannel theFileChannel) {
            this.theFileChannel = theFileChannel;
        }

        public long getLeftOverBytes() {
            return leftOverBytes;
        }

        public long getTheCurrentFragmentSize() {
            return theCurrentFragmentSize;
        }

        public void setTheCurrentFragmentSize(long theCurrentFragmentSize) {
            this.theCurrentFragmentSize = theCurrentFragmentSize;
        }

        public void setLeftOverBytes(long leftOverBytes) {
            this.leftOverBytes = leftOverBytes;
        }

        public int getMyParallelNum() {
            return myParallelNum;
        }

        public void setMyParallelNum(int myParallelNum) {
            this.myParallelNum = myParallelNum;
        }

        public FileSenderControlChannelHandler getMyFileSenderControlHandler() {
            return myFileSenderControlHandler;
        }

        public void setMyFileSenderControlHandler(FileSenderControlChannelHandler myFileSenderControlHandler) {
            this.myFileSenderControlHandler = myFileSenderControlHandler;
        }


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

    /*
      Path Done Object - Each Control Channel reports to the PathDoneObjectList that it is done
       reading files from the fileRequest List and that it's expected FileId List is empty - meaning
       All the files it sent has been acknowledged by the receiver.
       I implemented this so I can print the throughput of all paths and it's concurrent channel
       any time a control channel from  a specific path access it's path object, it decreases the concurrency count by 1
        when the concurrency count = 0 for the pathDoneObject it is removed from the list
        When the list doesn't contain any more objects the child thread and main thread are all shut down
        this is, if the paths use the same event loop, if they use different event loops then I may have to
        shut down each path

        Variables: String -  Alias Name of Path, example: WS5,WS7,WS12
                   Concurrency Num - Number of concurrent channels associated with this path
                                     any time a concurrent channel / control channel reports that it is done
                                     sending files and all files have been acknowledged it decrements the concurrent count
     */
    public static class PathDoneObject{
        String aliasPathName;
        int concurrencyNum;
        int registeredControlChannelsNum;

        public PathDoneObject(String anAliasPathName, int aConcurrencyNum){
            aliasPathName = anAliasPathName;
            concurrencyNum = aConcurrencyNum;
            registeredControlChannelsNum = 0;
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

        public int getRegisteredControlChannelsNum() {
            return registeredControlChannelsNum;
        }

        public void setRegisteredControlChannelsNum(int registeredControlChannelsNum) {
            this.registeredControlChannelsNum = registeredControlChannelsNum;
        }




    } //End PathDoneObject

    public static class FileRequestObject{

        String fileRequest;
        boolean rangeOn; //range = transfer a range from a given offset with the length, all = transfer whole file
        long offset;
        long length;

        public FileRequestObject(String aFileRequest, boolean turnRangeOn, long anOffset, long aLength){
            fileRequest = aFileRequest;
            turnRangeOn = rangeOn;
            offset = anOffset;
            length = aLength;
        }


        public String getFileRequest() {
            return fileRequest;
        }

        public void setFileRequest(String fileRequest) {
            this.fileRequest = fileRequest;
        }

        public boolean isRangeOn() {
            return rangeOn;
        }

        public void setRangeOn(boolean rangeOn) {
            this.rangeOn = rangeOn;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

    }

//////////////////////////////////////////
    /*

      Registers both DataChannels & Control Channels
      If a Data Channel Registers before a Control Channel
         A Control Channel is created, but the data channel is added to the data channel list

      Active -
      Path 1: Control Channel 1 Registers
      Path 1: Control Channel 2 Registers
      Path 2: Control Channel 1 Registers
      Path 2: Control Channel 2 Registers

     */

    ///////////
      ////////////
    //----public synchronized static void registerChannelCtx(String aPathAliasName, FileSenderControlChannelHandler aFileSenderControlChannelHandler, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, FileSenderDataChannelHandler aFileSenderDataChannelHandler, long aThreadId, int aParallelNum){
    public synchronized static void registerChannelCtx(String aPathAliasName, FileSenderControlChannelHandler aFileSenderControlChannelHandler, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, FileSenderDataChannelHandler aFileSenderDataChannelHandler, long aThreadId, int aParallelNum){
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
                    //myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileSender.ControlChannelObject(aControlChannelId, aFileSenderControlChannelHandler, aDataChannelId, aChannelType, aChannelCtx));
                    myControlChannelObjectMap.put(String.valueOf(aControlChannelId), new FileSender.ControlChannelObject(aControlChannelId, aFileSenderControlChannelHandler, aDataChannelId, aChannelType, aChannelCtx, aThreadId, aParallelNum));
                }
                else {
                    //a Control Channel Object exist with this Control Channel ID already
                    FileSender.ControlChannelObject myControlChannelObject = myControlChannelObjectMap.get(String.valueOf(aControlChannelId));
                    if (aChannelType == CONTROL_CHANNEL_TYPE) {
                        //Add the Control ChannelCTX to the Control Object
                        //String aPathAliasName, FileSenderControlChannelHandler aFileSenderControlChannelHandler, ChannelHandlerContext aChannelCtx, int aChannelType, int aControlChannelId, int aDataChannelId, FileSenderDataChannelHandler aFileSenderDataChannelHandler, long aThreadId, int aParallelNum
                        myControlChannelObject.setFileSenderControlHandler(aFileSenderControlChannelHandler);
                        myControlChannelObject.setControlChannel(aChannelCtx);
                        myControlChannelObject.setControlChannelId(aControlChannelId);
                        myControlChannelObject.setThreadId(aThreadId);
                        myControlChannelObject.setParallelDataChannelNum(aParallelNum);
                    }
                    else{
                        //Add the Data ChannelCTX
                        //myControlChannelObject.addDataChannelObject(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx, aFileSenderDataChannelHandler));
                        myControlChannelObject.addDataChannelObject(new FileSender.DataChannelObject(aDataChannelId, aChannelCtx, aFileSenderDataChannelHandler, aThreadId));
                    }
                }
            }

        }catch(Exception e){
            System.err.printf("RegisterChannel Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //----public synchronized static String registeredChannelsToString(){
    public static synchronized String registeredChannelsToString(){
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
    Creates a list of Concurrency Control Channel Objects
    A concurrency Control Object contains FileSenderControlHandlers from all paths with a specific control channel ID
    For Example Given 3 Paths with the specified Concurrency Values:
    Path 1: cc =2, Path 2: cc = 3, Path 3: cc = 1 then the Concurrency Control
    And assuming Path 3 is the slowest path (lowest bandwidth), Path 2 is faster than path (higher bandwidth) and
    Path 3 has the highest bandwidth
    Channel Objects in the list will look as follows:
    Concurrency Control Channel Object 1:
       ---Path 1: ControlChannelHandler 1,
       ---Path 2: ControlChannelHandler 1,
       ---Path 3: ControlChannelHandler 1
    Concurrency Control Channel Object 2:
       ---Path 1: ControlChannelHandler 2,
       ---Path 2: ControlChannelHandler 2,
    Concurrency Control Channel Object 3:
       ---Path 2: ControlChannelHandler 3,

    Input - A TempPathObjectList - This list orders paths from slowest to fastest
            registeredChannelsCTX HashMap - HashMap<Path, HashMap<Control Channel ID, ControlChannelObject>>
            Note the Path and TempPathObjectList is Associated with the Path in the HashMap
    Output - A Concurrency Control Channel Object List like above

    //This assumes that when the FileSenderControlChannel registers it's self in it's Active Method that the Path Order
            //Will be the same as the PathOrder in the TempPathList Object
            //So I can Either Iterate through the Temp PathList Object and get the Path or Assume
            //The paths registered in order.  I don't want to tie in the path List with this, but if I have to, I will

            //Iterate through the Temp Path List - This Path List orders paths from slowest to fastest based on Bandwidth Delay Product
            //This is done
            /*

               PathList
               --------
               Path 1: WS5,WS11,WS12,WS7, cc = 3
               Path 2: WS5,WS7, cc = 4

               Iterate through the Path List
               -----------------------------
               Path 1: WS5, WS11, WS12,WS7
                  ConcurrencyObject 1: Path 1: ControlChannel 1 & File Request
                  ConcurrencyObject 2: Path 1: ControlChannel 2 & File Request (is it possible controlChannel can recieve a FileAck before cc = 2 grabs a file, but it doesn't matter since the file will be removed
                  ConcurrencyObject 3: Path 1: ControlChannel 3 & File Request

                Path 2: WS5 ,WS7
                  ConcurrencyObject 1: Path 2: ControlChannel 1 & File Request
                  ConcurrencyObject 2: Path 2: ControlChannel 2 & File Request (is it possible controlChannel can recieve a FileAck before cc = 2 grabs a file, but it doesn't matter since the file will be removed
                  ConcurrencyObject 3: Path 2: ControlChannel 3 & File Request
                  ConcurrencyObject 4: Path 2: ControlChannel 4 & File Request

                So The Concurrency Objects will look as follows:
                -------------------------------------------------
                ConcurrencyObject 1:
                    Path 1: ControlChannel 1 & File Request
                    Path 2: ControlChannel 1 & File Request
                ConcurrencyObject 2:
                    Path 1: ControlChannel 2 & File Request
                    Path 2: ControlChannel 2 & File Request
                ConcurrencyObject 3:
                    Path 1: ControlChannel 3 & File Request
                    Path 2: ControlChannel 3 & File Request
                ConcurrencyObject 4:
                    Path 2: ControlChannel 4 & File Request


                List(ConcurrencyObject 1, ConcurrencyObject 2, ConcurrencyObject 3)
                ---ConcurrencyObject 1 (Contains a list of the first ControlChannels from each path
                   ---List(Path 1: ConcurrencyControlChannelAndFileRequest 1
                           Path 2: ConcurrencyControlChannelAndFileRequest 1,)
                ---ConcurrencyObject 2 (Contains a list of the first ControlChannels from each path
                  ---List(Path 1: ConcurrencyControlChannelAndFileRequest 2
                          Path 2: ConcurrencyControlChannelAndFileRequest 2)
                ---ConcurrencyObject 3 (Contains a list of the first ControlChannels from each path
                  ---List(Path 1: ConcurrencyControlChannelAndFileRequest 3
                          Path 2: ConcurrencyControlChannelAndFileRequest 3)
                ---ConcurrencyObject 4 (Contains a list of the first ControlChannels from each path
                  ---List(Path 2: ConcurrencyControlChannelAndFileRequest 4

                Iterate through the Path List
                --For each Path
                ----Iterate through the controlChannelHashMap
                ------For Each Control Channel
                ---------Check to see if the ConcurrencyObject with ConcurrencyObject ID exist for this controlChannelId
                   ---------If it doesn't exist create it
                   ---------If it does exist get it
                --------------Get the FileSenderControlHandler from the Control Channel Object
                --------------Get the pathFileRequest List (FileSender.getNextFileRequestFromList(myPathString);)
                -----------------Add the aliasPath, FileSenderControlHandler, pathFileRequest and ParallelNum to the <ConcurrencyControlChannelAndFileRequest>

             */
    //----public synchronized static void createTheConcurrencyControlObjectList(){
    public static void createTheConcurrencyControlObjectList(){
        try {
            //Iterate through the Temp Path List - This Path List orders paths from slowest to fastest based on Bandwidth Delay Product
            for (FileSender.TempPathObject aTempPathObject : tempPathList){

                //int theControlChannelId = 1; //Initial Control Channel id
                //Get the ControlChannelObjectHashMap from the registered Channels HashMap Data Structure
                HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aTempPathObject.getAliasPathName());
                //Iterate through this ControlChannelObjectHashMap for the given path
                // Basically getting each <Control ID, Control Channel Object> map entry
                //--Add the associated FileSenderControlChannelHandler to the ConcurrencyControlObject List for the given Control Channel ID
                Iterator<Map.Entry<String, FileSender.ControlChannelObject>> controlChannelEntryIterator = myControlChannelObjectMap.entrySet().iterator();
                while (controlChannelEntryIterator.hasNext()){
                    //Get the Control Channel Object Hash Map Entry
                    Map.Entry<String, FileSender.ControlChannelObject> controlChannelEntry = controlChannelEntryIterator.next();
                    //Get the control channel Object
                    FileSender.ControlChannelObject aControlChannelObject = controlChannelEntry.getValue();
                    //Get the control channel ID
                    if (aControlChannelObject != null) {
                        int theControlChannelId = aControlChannelObject.getControlChannelId();
                        logger.info("FileSender: createTheConcurrencyControlObjectList: Path: " + aTempPathObject.getAliasPathName() + " CONTROL CHANNEL OBJECT ID =  " + theControlChannelId);
                        //Check to see if the ConcurrencyControlChannelObject List already contains the ConcurrencyControlChannelObject with specified Control Channel ID
                        if (!FileSender.doesConcurrencyControlChannelListContainId(theControlChannelId)) {
                            logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList DOES NOT CONTAIN a CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId );
                            //Add the ConcurrencyControlChannelObject
                            //ConcurrencyControlChannelAndFileRequest(String anAliasPathString, String aFileRequest, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aParallelNum )
                            FileSender.concurrencyControlChannelObjectList.add(new FileSender.ConcurrencyControlChannelObject(theControlChannelId));
                            logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList ADDED A CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId + " For PATH: " + aTempPathObject.getAliasPathName() +" Let's see if we can find it" );
                            if (FileSender.doesConcurrencyControlChannelListContainId(theControlChannelId)) {
                                logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList FOUND CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId + " FOR PATH: "+ aTempPathObject.getAliasPathName());
                            }else{
                                logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList DID NOT FIND CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId + " For PATH: " + aTempPathObject.getAliasPathName());
                            }

                        }
                        else {
                            logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList ALREADY contains a CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId + " For PATH: " + aTempPathObject.getAliasPathName() );
                        }
                        //Get the ConcurrencyControlChannelObject from the List
                        FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject = FileSender.getConcurrencyControlChannelObject(theControlChannelId);
                        if (aConcurrencyControlChannelObject != null) {
                            logger.info("FileSender:createTheConcurrencyControlObjectList:  ConcurrencyControlChannelList ALREADY contains a CONCURRENCY CONTROL CHANNEL OBJECT with CONTROL CHANNEL ID: " + theControlChannelId + " For PATH: " + aTempPathObject.getAliasPathName() );

                            if (aControlChannelObject.getFileSenderControlChannelHandler() != null ){
                                logger.info("FileSender:createTheConcurrencyControlObjectList:   ControlChannelObject WITH id: " + theControlChannelId + " and PATH: " + aTempPathObject.getAliasPathName() +" RETURNED THE CONTROL CHANNEL HANDLER AND IT IS NOT NULL, HALLELUJAH!!");
                            }else {
                                logger.info("FileSender:createTheConcurrencyControlObjectList:   ControlChannelObject WITH id: " + theControlChannelId + " and PATH: " + aTempPathObject.getAliasPathName() +" RETURNED THE CONTROL CHANNEL HANDLER AND IT IS NULL, BUT I PRAISE GOD FOR THE VICTORY, HALLELUJAH!!");
                            }
                            //ConcurrencyControlChannelAndFileRequest(String anAliasPathString,    String aFileRequest, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aParallelNum ){
                            aConcurrencyControlChannelObject.addControlChannelHandlerAndFileRequestToList(new FileSender.ConcurrencyControlChannelAndFileRequest(aTempPathObject.getAliasPathName(), FileSender.getNextFileRequestFromList(aTempPathObject.getAliasPathName()), aControlChannelObject.getFileSenderControlChannelHandler(), aControlChannelObject.getParallelDataChannelNum()));
                            logger.info("FileSender:createTheConcurrencyControlObjectList - ADDED ConcurrencyControlChannelAndFileRequest to A ConcurrencyControlChannelAndFileRequest LIST with id " + theControlChannelId + " FOR PATH: " + aTempPathObject.getAliasPathName() + " AND THE NEW ConcurrencyControlChannelAndFileRequest List Size = " + aConcurrencyControlChannelObject.getControlChannelHandlerAndFileRequestList().size());
                            //String anAliasPathString, String aFileRequest, FileSenderControlChannelHandler aFileSenderControlChannelHandler, int aParallelNum
                            //Increment theControlChannelId
                            //theControlChannelId++;
                        } else {
                            logger.info("FileSender:createTheConcurrencyControlObjectList: ConcurrencyControlChannelObject  == NULL " + " For PATH: " + aTempPathObject.getAliasPathName() + "And Control Channel ID: " + theControlChannelId);
                        }
                    }


                }//End While

            }//End for
            //Iterate through the RegisteredChannelsCtx HashMap <Path, HashMap<Control Channel Id String, ControlChannelObject>>
            // HashMap<String, FileSender.ControlChannelObject> myControlChannelObjectMap = myRegisteredChannelsCtx.get(aPathAliasName);

            //Iterate through ConcurrencyControlObjectList and start each FileSenderControlChannel

        }catch(Exception e){
            System.err.printf("createTheConcurrencyControlObjectList: Error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    /*
     /////////////////////////////////////////////////////
     // startSendingFilesThroughTheConcurrentChannels() //
     ////////////////////////////////////////////////////

    //Iterate through ConcurrencyControlObjectList and start each FileSenderControlChannel
    This Concurrency Control Channel Object contains a list of
       FileSenderControlChannelHandlers from different paths
       Example: Path 1: cc =2, Path 2: cc = 3, Path 3: cc = 1
       Concurrency Control Channel Object 1:
       ---Path 1: ControlChannelHandler 1,
       ---Path 2: ControlChannelHandler 1,
       ---Path 3: ControlChannelHandler 1
       Concurrency Control Channel Object 2:
       ---Path 1: ControlChannelHandler 2,
       ---Path 2: ControlChannelHandler 2,
       Concurrency Control Channel Object 3:
       ---Path 2: ControlChannelHandler 3,

       //Iterate through the Concurrency Control Object List
            //For each Concurrency Control Object
              //-----Get the ConcurrencyControlChannelAndFileRequestList
              //----------Iterate through the ConcurrencyControlChannelAndFileRequestList
              //-------------Get the ControlChannelHandler and start the sending file method sending: the srcFilePath, DestFilePath, CurrentFragmentSize, leftover bytes, file length, file id (which equals the control channel id)

    */
    //---public synchronized static void startSendingFilesThroughTheConcurrentChannels(){
    public static void startSendingFilesThroughTheConcurrentChannels(){
        try {
            //Iterate through the Concurrency Control Object List
            //For each Concurrency Control Object
              //-----Get the ConcurrencyControlChannelAndFileRequestList
              //----------Iterate through the ConcurrencyControlChannelAndFileRequestList
              //-------------Get the ControlChannelHandler and start the sending file method sending: the srcFilePath, DestFilePath, CurrentFragmentSize, leftover bytes, file length, file id (which equals the control channel id)
            int myFileId = 1;

            for (FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject : FileSender.concurrencyControlChannelObjectList){
                //-----Get the ConcurrencyControlChannelAndFileRequestList
                List<FileSender.ConcurrencyControlChannelAndFileRequest> aConcurrencyControlChannelAndFileRequestList = aConcurrencyControlChannelObject.getControlChannelHandlerAndFileRequestList();
                if (aConcurrencyControlChannelAndFileRequestList != null){
                    if (!aConcurrencyControlChannelAndFileRequestList.isEmpty()){
                        //Iterate through the ConcurrencyControlChannelAndFileRequestList
                        for (FileSender.ConcurrencyControlChannelAndFileRequest aConcurrencyControlChannelAndFileRequest : aConcurrencyControlChannelAndFileRequestList){
                            //Get the ControlChannelHandler and start the sending file method sending: the srcFilePath, DestFilePath, CurrentFragmentSize, leftover bytes, file length, file id (which equals the control channel id)
                            //Note: the fileId = aConcurrencyControlChannelObject ID                 startSendingFiles(                                 String aSrcFilePath,                                       String aDestFilePath,                                   long aFileLength,                                        long theCurrentFragmentSize,                                long theLeftOverBytes,          int theFileId) throws Exception {
                            //aConcurrencyControlChannelAndFileRequest.getMyFileSenderControlHandler().startSendingFiles(aConcurrencyControlChannelAndFileRequest.getMySrcFilePath(), aConcurrencyControlChannelAndFileRequest.getMyDestFilePath(), aConcurrencyControlChannelAndFileRequest.getMyFileLength(), aConcurrencyControlChannelAndFileRequest.getTheCurrentFragmentSize(),aConcurrencyControlChannelAndFileRequest.getLeftOverBytes(), myFileId  ); //Note each control channel object has it's own file id list
                            FileSenderControlChannelHandler aControlChannelHandler = aConcurrencyControlChannelAndFileRequest.getMyFileSenderControlHandler();
                            if (aControlChannelHandler != null){
                                aControlChannelHandler.startSendingFiles(aConcurrencyControlChannelAndFileRequest.getMySrcFilePath(), aConcurrencyControlChannelAndFileRequest.getMyDestFilePath(), aConcurrencyControlChannelAndFileRequest.getMyFileLength(), aConcurrencyControlChannelAndFileRequest.getTheCurrentFragmentSize(),aConcurrencyControlChannelAndFileRequest.getLeftOverBytes(), myFileId  ); //Note each control channel object has it's own file id list
                              logger.info("FileSender:startSendingFilesThroughTheConcurrentChannels - FileSenderControlChannelHandler NOT EQUAL NULL");
                            }else {
                                logger.info("FileSender:startSendingFilesThroughTheConcurrentChannels - FileSenderControlChannelHandler EQUAL NULL");
                            }

                            //aConcurrencyControlChannelAndFileRequest.getMyFileSenderControlHandler().startSendingFiles(aConcurrencyControlChannelAndFileRequest.getMySrcFilePath(), aConcurrencyControlChannelAndFileRequest.getMyDestFilePath(), aConcurrencyControlChannelAndFileRequest.getMyFileLength(), aConcurrencyControlChannelAndFileRequest.getTheCurrentFragmentSize(),aConcurrencyControlChannelAndFileRequest.getLeftOverBytes(), aConcurrencyControlChannelObject.getConcurrencyControlChannelId()  );
                        }
                    }else {
                        logger.info("FileSender.startSendingFilesThroughTheConcurrentChannels: aConcurrencyControlChannelAndFileRequestList is EMPTY for ControlChannel " + aConcurrencyControlChannelObject.getConcurrencyControlChannelId());
                    }

                }
                else {
                    logger.info("FileSender.startSendingFilesThroughTheConcurrentChannels: aConcurrencyControlChannelAndFileRequestList is NULL for ControlChannel " + aConcurrencyControlChannelObject.getConcurrencyControlChannelId());
                }
            }
        }catch(Exception e){
            System.err.printf("FileSender: startSendingFilesThroughTheConcurrentChannels: Error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    /*
       Iterate through the ConcurrencyControlChannelList and check to see if any of it's
       ConcurrencyControlObjects contains the specified ControlChannel ID.
       Output - Returns true if the ConcurrencyControlObjects contains the specified controlChannelId
                Returns false if the ConcurrencyControlObject does not contain the specified controlChannelId
     */

    //----public synchronized static boolean doesConcurrencyControlChannelListContainId(int aControlChannelId){
    public static boolean doesConcurrencyControlChannelListContainId(int aControlChannelId){
        try{
            boolean found = false;
            Iterator<FileSender.ConcurrencyControlChannelObject> myIterator = FileSender.concurrencyControlChannelObjectList.iterator();
            while (!found && myIterator.hasNext()) {
                FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject = myIterator.next();
                if (aConcurrencyControlChannelObject.getConcurrencyControlChannelId() == aControlChannelId ){
                    found = true;
                    break;
                }
            }
            return found;

        }catch(Exception e){
            System.err.printf("FileSender.doesConcurrencyControlChannelListContainId Error Msg: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    //FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject = FileSender.getConcurrencyControlChannelObject(theControlChannelId);
    //----public synchronized static FileSender.ConcurrencyControlChannelObject getConcurrencyControlChannelObject(int aConcurrencyControlChannelObjectId) {
    public static FileSender.ConcurrencyControlChannelObject getConcurrencyControlChannelObject(int aConcurrencyControlChannelObjectId) {
        try{
            boolean found = false;
            FileSender.ConcurrencyControlChannelObject myConcurrencyControlChannelObject = null;

            Iterator<FileSender.ConcurrencyControlChannelObject> myIterator = FileSender.concurrencyControlChannelObjectList.iterator();
            while (!found && myIterator.hasNext()) {
                FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject = myIterator.next();
                if (aConcurrencyControlChannelObject.getConcurrencyControlChannelId() ==  aConcurrencyControlChannelObjectId){
                    found = true;
                    myConcurrencyControlChannelObject = aConcurrencyControlChannelObject;
                    break;
                }
            }
            return myConcurrencyControlChannelObject;

        }catch(Exception e){
            System.err.printf("FileSender.getConcurrencyControlChannelObject Error Msg: " + e.getMessage() + "\n");
            e.printStackTrace();
            return null;
        }

    }

    /*
    //Iterate through ConcurrencyControlObjectList and start each FileSenderControlChannel
    This Concurrency Control Channel Object contains a list of
       FileSenderControlChannelHandlers from different paths
       Example: Path 1: cc =2, Path 2: cc = 3, Path 3: cc = 1
       Concurrency Control Channel Object 1:
       ---Path 1: ControlChannelHandler 1,
       ---Path 2: ControlChannelHandler 1,
       ---Path 3: ControlChannelHandler 1
       Concurrency Control Channel Object 2:
       ---Path 1: ControlChannelHandler 2,
       ---Path 2: ControlChannelHandler 2,
       Concurrency Control Channel Object 3:
       ---Path 2: ControlChannelHandler 3,
    */
    /*
    public synchronized static void startSendingFilesThroughControlChannelHandlers(){
        try{
            //Iterate through the Concurrency Control Channel Object List
            for (FileSender.ConcurrencyControlChannelObject aConcurrencyControlChannelObject : FileSender.concurrencyControlChannelObjectList) {
                //Iterate through the FileSenderControlChannelHandler list
                List<FileSenderControlChannelHandler> aFileSenderControlChannelHandlerList = aConcurrencyControlChannelObject.getFileSenderControlHandlerList();
                for ( FileSenderControlChannelHandler aFileSenderControlChannelHandler : aFileSenderControlChannelHandlerList) {
                    aFileSenderControlChannelHandlerList.StartSendingFiles();
                }

            }

        }catch(Exception e){
            System.err.printf("startSendingFilesThroughControlChannelHandlers Error Msg: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

    }
    */



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

    //----public synchronized static void registerConnectionAck(String aPathAliasName, int aControlChannelId){
    public synchronized static void registerConnectionAck(String aPathAliasName, int aControlChannelId){
        try {
            //String myCurrentRegisteredChannels = FileSender.registeredChannelsToString();

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


    //----public synchronized static String getConnectedChannelAckIDsInStringFormat(String aPathAliasName, int aControlChannelId){
    public static String getConnectedChannelAckIDsInStringFormat(String aPathAliasName, int aControlChannelId){
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

    //----public synchronized static FileSender.ControlChannelObject getControlChannelObject(String anAliasPathString, int aControlChannelId){
    public static FileSender.ControlChannelObject getControlChannelObject(String anAliasPathString, int aControlChannelId){
        try {

            FileSender.ControlChannelObject myControlChannelObject = null;

            //Check to see if the alias path exist, if not add path to the HashMap
            if ( anAliasPathString  != null) {
                //If myRegisteredChannels doesn't contain the path, place the path in the hashMap
                //the  Hashmap now contains the path if it didn't before, or if it did now just get it from the hash map
                //      ControlChannel Id, Control Channel Object
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

    //----public synchronized static List<FileSender.DataChannelObject> getDataChannelObjectList(String aPathAliasName, int aControlChannelId){
    public static List<FileSender.DataChannelObject> getDataChannelObjectList(String aPathAliasName, int aControlChannelId){
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

    //----public synchronized static void addFileRequestToPathList(String anAliasPath, String aFileRequest){
    public static void addFileRequestToPathList(String anAliasPath, String aFileRequest){
        try{

            ArrayList<String> aFileRequestList = null;

            if (anAliasPath !=null) {
                if (FileSender.myPathAndFileRequestList.containsKey(anAliasPath)){
                    FileSender.myPathAndFileRequestList.get(anAliasPath).add(aFileRequest);
                    logger.info("FileSender: addFileRequestToPathList: added file request: " + aFileRequest + " to the path: " + anAliasPath);
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


    //----public synchronized static String getNextFileRequestFromList(String anAliasPath){
    public static String getNextFileRequestFromList(String anAliasPath){
        try {
            //logger.info("FileSender: getNextFileRequestFromList Method Entered");
            String aFileRequest = null;
            ArrayList<String> aFileRequestList = null;

            if (anAliasPath !=null) {
                if (myPathAndFileRequestList == null) {
                    //logger.info("FileSender: getNextFileRequestFromList: myPathAndFileRequestList == null");
                } else {
                    aFileRequestList = myPathAndFileRequestList.get(anAliasPath);
                    if (aFileRequestList != null) {
                        if (!aFileRequestList.isEmpty()) {
                            aFileRequest = aFileRequestList.remove(0);
                        }

                    }

                } //End else
            } //End if (anAliasPath !=null)
            //logger.info("getNextFileRequest: FileRequest = " + aFileRequest);
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //----public synchronized static String getNextFileRequestFromList(){
    public static String getNextFileRequestFromList(){
        try {
            //logger.info("FileSender: getNextFileRequestFromList Method Entered");
            String aFileRequest = null;
            if (!FileSender.mainFileRequestList.isEmpty()) {
                aFileRequest = mainFileRequestList.remove(0);
            }
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getNextFileRequestFromList: Error: "+e.getMessage() + "\n");
            e.printStackTrace();
            return null;
        }

    }


    //----public synchronized static boolean isMainFileRequestListEmpty(){
    public static boolean isMainFileRequestListEmpty(){
        try {
            return FileSender.mainFileRequestList.isEmpty();
        }catch(Exception e){
            System.err.printf("FileSender: Error: isMainFileRequestListEmpty "+e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }

    }

    //public synchronized static void createFileRequestListPerPath(PathList aThreadPathList, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
    //----public synchronized static void createFileRequestListPerPath(String aPathName){
    public static void createFileRequestListPerPath(String aPathName){
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

    /*
    public synchronized static List<String> getFileRequestList(String anAliasPath){
        try {
            List<String> aFileRequestList = null;
            if (anAliasPath != null) {
                aFileRequestList = myPathAndFileRequestList.get(anAliasPath);
            }
            return aFileRequestList;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    */

    //----public synchronized static ArrayList<String> getFileRequestList(String anAliasPath){
    public static ArrayList<String> getFileRequestList(String anAliasPath){
        try {
            ArrayList<String> aFileRequestList = null;
            if (anAliasPath != null) {
                aFileRequestList = myPathAndFileRequestList.get(anAliasPath);
            }
            return aFileRequestList;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /*
    public synchronized static String getNextFileRequestFromList(String anAliasPath){
        try {

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

                    }


                } //End else
            } //End if (anAliasPath !=null)
            return aFileRequest;
        }catch(Exception e){
            System.err.printf("FileSender:getFileRequestList: Error: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    */


    //public synchronized static void createFileRequestListPerPath(PathList aThreadPathList, HashMap<String, ArrayList<String>> aPathAndFileRequestList ){
    //----public synchronized static void createFileRequestListPerPath(){
    public static void createFileRequestListPerPath(){
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

    /*

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
*/

    public void startFileSender() throws InvocationTargetException, Exception{
        try {//////////////////////////////
            long threadId = Thread.currentThread().getId();
            //logger.info("******************************************************");
            //logger.info("FileSender:startFileSender ThreadId = " + threadId);
            //logger.info("******************************************************");

            /*
              Should each path have it's own Event Loop Group ?

             */

            //Each Path will share the event loop
            EventLoopGroup group = new NioEventLoopGroup();

            ///////////////////////////////////////////////////
            //                                               //
            //  ITERATE THROUGH THE TEMP PATH LIST            //
            //  EACH PATH WILL HAVE IT'S OWN EVENT LOOP GROUP //
            //                                               //
            //////////////////////////////////////////////////
            for (FileSender.TempPathObject aTempPathObject: FileSender.tempPathList) {

                //Configure the File Sender
                //EventLoopGroup group = new NioEventLoopGroup();

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
    //----public synchronized void reportThroughput(int channelId, long startTimeRead, long endTimeRead, long bytesRead) throws Exception {
    public void reportThroughput(int channelId, long startTimeRead, long endTimeRead, long bytesRead) throws Exception {
        try {
            ThroughputObject aThroughputObject = this.new ThroughputObject(channelId, startTimeRead, endTimeRead, bytesRead);
            throughputObjectList.add(aThroughputObject);
        }catch(Exception e) {
            System.err.printf("FileSender: Report Throughput Error: %s \n ", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }//End Catch
    }

    //----public synchronized static void printAllThroughputToScreen(){
    public static void printAllThroughputToScreen(){
        try {
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
            //boolean Individual Control Channel flags,
            boolean controlChannelMinStartTimeSet = false;
            boolean controlChannelMaxEndTimeSet = false;
            boolean controlChannelTotalBytesSet = false;

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
            Iterator<Map.Entry<String, HashMap<String,FileSender.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                //        Path             Control Channel ID, Control Channel Object
                Map.Entry<String, HashMap<String, FileSender.ControlChannelObject>> myRegisteredChannelsCtxEntry = pathIterator.next();
                //Get the Path Name
                StringToPrint = StringToPrint + "Path: " + myRegisteredChannelsCtxEntry.getKey();
                //Get the Control Channel Id Hash Map and iterate through it
                HashMap<String, FileSender.ControlChannelObject> theControlChannelIdHashMap = myRegisteredChannelsCtxEntry.getValue();
                //Iterate through the control channel Id HashMap for the given path
                Iterator<Map.Entry<String, FileSender.ControlChannelObject>> controlChannelIdIterator = theControlChannelIdHashMap.entrySet().iterator();
                while (controlChannelIdIterator.hasNext()) {
                    //Get the Control Channel Id - Hash Map Entry
                    Map.Entry<String, FileSender.ControlChannelObject> controlChannelIdEntry = controlChannelIdIterator.next();
                    //Get the Control Channel Id
                    StringToPrint = StringToPrint + "\n----Control Channel " + controlChannelIdEntry.getKey() + ": ";
                    //Get the control channel Object
                    FileSender.ControlChannelObject aControlChannelObject = controlChannelIdEntry.getValue();
                    //Calculate Throughput in Mb/s
                    //Ensure the start time and end time are greater than "zero"
                    if ( (aControlChannelObject.getStartTime() > 0) && (aControlChannelObject.getEndTime() > 0) ) {
                        controlChannelThroughput = (((aControlChannelObject.getTotalBytesRead() * 8) / (aControlChannelObject.getEndTime() - aControlChannelObject.getStartTime())) * 1000) / 1000000;

                        //Get start Time, End Time and BytesRead from this Control Channel Object
                        StringToPrint = StringToPrint + " Start Time: " + aControlChannelObject.getStartTime() + ", End Time: " + aControlChannelObject.getEndTime() + ", Bytes Read: " + aControlChannelObject.getTotalBytesRead() + ", Throguhput: " + controlChannelThroughput + "Mb/s \n";

                        //Path Total Bytes Read
                        pathTotalBytes += aControlChannelObject.getTotalBytesRead();

                        //Set Path Min Start Time
                        if (!pathMinStartTimeSet) {
                            pathMinStartTimeSet = true;
                            pathMinStartTime = aControlChannelObject.getStartTime();
                        } else {
                            pathMinStartTime = (aControlChannelObject.getStartTime() < pathMinStartTime) ? aControlChannelObject.getStartTime() : pathMinStartTime;
                        }

                        //Set Path Max End Time
                        if (!pathMaxEndTimeSet) {
                            pathMaxEndTimeSet = true;
                            pathMaxEndTime = aControlChannelObject.getEndTime();
                        } else {
                            pathMaxEndTime = (aControlChannelObject.getEndTime() > pathMaxEndTime) ? aControlChannelObject.getEndTime() : pathMaxEndTime;
                        }
                    }
                    else {
                        StringToPrint = StringToPrint + " Throguhput: 0.00 Mb/s \n";
                    }

                } //End Iterating through the Control Channel for this given path

                if ((pathMinStartTime > 0) && (pathMaxEndTime > 0)) {
                    //Calculate Throughput in Mb/s
                    pathThroughput = (((pathTotalBytes * 8) / (pathMaxEndTime - pathMinStartTime)) * 1000) / 1000000;
                    StringToPrint = StringToPrint + "\n--Path:" + myRegisteredChannelsCtxEntry.getKey() + " Metrics:  Min Start Time: " + pathMinStartTime + " Max End Time: " + pathMaxEndTime + " Total Bytes Read: " + pathTotalBytes + ", Throughput:  " + pathThroughput + "Mb/s" + "\n";
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
                }//End if path start time and path end time > 0

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
            } else {
                overAllThroughput = 0;
            }

            StringToPrint = StringToPrint + "****\n" + "OVERALL END-TO-END THROUGHPUT METRICS: Min Start Time: " + overallMinStartTime + ", Max End Time: " + overallMaxEndTime + ", Total Bytes Read: " + overallTotalBytes + ", Throughput: " + overAllThroughput + "Mb/s" + "\n *****\n";
            logger.info(StringToPrint);

        }catch(Exception e) {
            System.err.printf("FileSender: ThroughputToString Error: %s \n ", e.getMessage());
            e.printStackTrace();
            //System.exit(1);
        }//End Catch
    }//End printAllThroughputToScreen

    //----public synchronized static void printAllThreadIds(){
    public static void printAllThreadIds(){
        try {
            String StringToPrint = "";
            StringToPrint = StringToPrint + "\n FileSender: THREAD ID: " + Thread.currentThread().getId() + " Thread Name: " + Thread.currentThread().getName();

            //Iterate through the Path and Control Channel HashMap
            Iterator<Map.Entry<String, HashMap<String,FileSender.ControlChannelObject>>> pathIterator = myRegisteredChannelsCtx.entrySet().iterator();
            //Iterate through each path in the HashMap
            while (pathIterator.hasNext()) {
                //        Path             Control Channel ID, Control Channel Object
                Map.Entry<String, HashMap<String, FileSender.ControlChannelObject>> myRegisteredChannelsCtxEntry = pathIterator.next();
                //Get the Path Name
                StringToPrint = StringToPrint + "\nPath: " + myRegisteredChannelsCtxEntry.getKey();
                //Get the Control Channel Id Hash Map and iterate through it
                HashMap<String, FileSender.ControlChannelObject> theControlChannelIdHashMap = myRegisteredChannelsCtxEntry.getValue();
                //Iterate through the control channel Id HashMap for the given path
                Iterator<Map.Entry<String, FileSender.ControlChannelObject>> controlChannelIdIterator = theControlChannelIdHashMap.entrySet().iterator();
                while (controlChannelIdIterator.hasNext()) {
                    //Get the Control Channel Id - Hash Map Entry
                    Map.Entry<String, FileSender.ControlChannelObject> controlChannelIdEntry = controlChannelIdIterator.next();
                    //Get the Control Channel Id
                    StringToPrint = StringToPrint + "\n--Control Channel " + controlChannelIdEntry.getKey() + ": THREAD ID: ";
                    //Get the control channel Object
                    FileSender.ControlChannelObject aControlChannelObject = controlChannelIdEntry.getValue();
                    StringToPrint = StringToPrint + aControlChannelObject.getThreadId();
                    //Get Data Channel List
                    List<FileSender.DataChannelObject> myDataChannelObjectList = aControlChannelObject.getDataChannelObjectList();
                    //Iterate through the Data Channel Object List
                    for (FileSender.DataChannelObject aDataChannelObject: myDataChannelObjectList){
                        StringToPrint = StringToPrint + "\n ----Data Channel: " + aDataChannelObject.getThreadId();
                    }
                } //End Iterating through the Control Channel for this given path

            }//End iterating through paths in the HashMap

            logger.info(StringToPrint);

        }catch(Exception e) {
            System.err.printf("FileSender: ThroughputToString Error: %s \n ", e.getMessage());
            e.printStackTrace();
            //System.exit(1);
        }//End Catch
    }//End printAllThreadIds

    //----public synchronized String throughputInfoToString() {
    public String throughputInfoToString() {
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
    //----public synchronized String convertThroughputToString(double bitsPerSecond){
    public String convertThroughputToString(double bitsPerSecond){
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

    /*
    public static ArrayList<String> getFileRequestList(String aPath){
        try {
            ArrayList<String> aFileRequestList = null;
            if (aPath != null ) {
                aFileRequestList = FileSender.myPathAndFileRequestList.get(aPath);
            }
            return aFileRequestList;

        }catch(Exception e){
            System.err.printf("FileSender: getFileRequestList Error " + e.getMessage() + "\n");
            e.printStackTrace();
            return null;
        }

    }
*/
    public static void main(String[] args) throws Exception {
        FileSender myFileSender = new FileSender();

        //Add Path WS5,WS11,WS12,WS7
        FileSender.addTempObjectToTempPathList("192.168.2.2:4959,192.168.3.3:4959,192.168.1.1:4959", "WS5,WS11,WS12,WS7", 1, 1, 1);
        FileSender.addPathDoneObjectToPathDoneList("WS5,WS11,WS12,WS7",1);


        //Add Path WS5,WS7 to Temp Object List and PathDone List
        FileSender.addTempObjectToTempPathList("192.168.0.1:4959", "WS5,WS7", 1, 1, 1);
        FileSender.addPathDoneObjectToPathDoneList("WS5,WS7",1);

        //Create File Request List for the Path: WS5,WS11, WS12,WS7
        //myPathAndFileRequestList.put("WS5,WS11,WS12,WS7", Collections.synchronizedList(new ArrayList<String>()));
        myPathAndFileRequestList.put("WS5,WS11,WS12,WS7", new ArrayList<String>());



        //Create File Request List for the Path: WS5,WS7
        //myPathAndFileRequestList.put("WS5,WS7",Collections.synchronizedList(new ArrayList<String>()));
        myPathAndFileRequestList.put("WS5,WS7", new ArrayList<String>());


        //Get the Array List associated with the Path: WS5, WS7
        ArrayList<String> myFileRequestList_WS5_WS7 = FileSender.myPathAndFileRequestList.get("WS5,WS7");
        //List<String> myFileRequestList_WS5_WS7 = FileSender.myPathAndFileRequestList.get("WS5,WS7");
        //Add file Requests to WS5,WS7 Array List




        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File1.dat WS7/home/lrodolph/100MB_DIR/100MB_File1_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File2.dat WS7/home/lrodolph/100MB_DIR/100MB_File2_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File3.dat WS7/home/lrodolph/100MB_DIR/100MB_File3_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File4.dat WS7/home/lrodolph/100MB_DIR/100MB_File4_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File5.dat WS7/home/lrodolph/100MB_DIR/100MB_File5_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File6.dat WS7/home/lrodolph/100MB_DIR/100MB_File6_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File7.dat WS7/home/lrodolph/100MB_DIR/100MB_File7_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File8.dat WS7/home/lrodolph/100MB_DIR/100MB_File8_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File9.dat WS7/home/lrodolph/100MB_DIR/100MB_File9_Copy.dat");
        myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File10.dat WS7/home/lrodolph/100MB_DIR/100MB_File10_Copy.dat");



        //myFileRequestList_WS5_WS7.add("transfer WS5/home/lrodolph/1000MB_DIR/1000MB_File1.dat WS7/home/lrodolph/1000MB_DIR/1000MB_File1_Copy.dat");

        //Get the Array List associated with the Path: WS5,WS11,WS12,WS7
        ArrayList<String> myFileRequestList_WS5_WS11_WS12_WS7 = FileSender.myPathAndFileRequestList.get("WS5,WS11,WS12,WS7");



        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File11.dat WS7/home/lrodolph/100MB_DIR/100MB_File11_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File12.dat WS7/home/lrodolph/100MB_DIR/100MB_File12_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File13.dat WS7/home/lrodolph/100MB_DIR/100MB_File13_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File14.dat WS7/home/lrodolph/100MB_DIR/100MB_File14_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File15.dat WS7/home/lrodolph/100MB_DIR/100MB_File15_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File16.dat WS7/home/lrodolph/100MB_DIR/100MB_File16_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File17.dat WS7/home/lrodolph/100MB_DIR/100MB_File17_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File18.dat WS7/home/lrodolph/100MB_DIR/100MB_File18_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File19.dat WS7/home/lrodolph/100MB_DIR/100MB_File19_Copy.dat");
        myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File20.dat WS7/home/lrodolph/100MB_DIR/100MB_File20_Copy.dat");



        /*
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File1.dat WS7/home/lrodolph/100MB_DIR/100MB_File1_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File2.dat WS7/home/lrodolph/100MB_DIR/100MB_File2_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File3.dat WS7/home/lrodolph/100MB_DIR/100MB_File3_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File4.dat WS7/home/lrodolph/100MB_DIR/100MB_File4_Copy.dat");
        */
/*
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File5.dat WS7/home/lrodolph/100MB_DIR/100MB_File5_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File6.dat WS7/home/lrodolph/100MB_DIR/100MB_File6_Copy.dat");

        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File7.dat WS7/home/lrodolph/100MB_DIR/100MB_File7_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File8.dat WS7/home/lrodolph/100MB_DIR/100MB_File8_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File9.dat WS7/home/lrodolph/100MB_DIR/100MB_File9_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File10.dat WS7/home/lrodolph/100MB_DIR/100MB_File10_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File11.dat WS7/home/lrodolph/100MB_DIR/100MB_File11_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File12.dat WS7/home/lrodolph/100MB_DIR/100MB_File12_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File13.dat WS7/home/lrodolph/100MB_DIR/100MB_File13_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File14.dat WS7/home/lrodolph/100MB_DIR/100MB_File14_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File15.dat WS7/home/lrodolph/100MB_DIR/100MB_File15_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File16.dat WS7/home/lrodolph/100MB_DIR/100MB_File16_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File17.dat WS7/home/lrodolph/100MB_DIR/100MB_File17_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File18.dat WS7/home/lrodolph/100MB_DIR/100MB_File18_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File19.dat WS7/home/lrodolph/100MB_DIR/100MB_File19_Copy.dat");
        mainFileRequestList.add("transfer WS5/home/lrodolph/100MB_DIR/100MB_File20.dat WS7/home/lrodolph/100MB_DIR/100MB_File20_Copy.dat");
*/



        //myFileRequestList_WS5_WS11_WS12_WS7.add("transfer WS5/home/lrodolph/1000MB_DIR/1000MB_File2.dat WS7/home/lrodolph/1000MB_DIR/1000MB_File2_Copy.dat");

        //List<String> myFileRequestList_WS5_WS11_WS12_WS7 = FileSender.myPathAndFileRequestList.get("WS5,WS11,WS12,WS7");
        //myFileRequestList_WS5_WS11_WS12_WS7.add("WS5/home/lrodolph/1GB_DIR/1GB_File10.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File10_Copy.dat");

        //Run File Distribution Algorithm
        //FileSender.runFileDistributionAlg("rr");

        /////////////////////////////
        myFileSender.startFileSender();
        ///////////////////////////////


        //Do Below:
        //FileSender.ThroughputObject aThroughputObject = myFileSender.new ThroughputObject(11, 4, 7, 1024);
        //Add ThroughputObject is a Static Method and Class ThroughputObject is a non-static class

        //FileSender.addThroughputObject(myFileSender.new ThroughputObject(13,7,17,1024));
        //FileSender.addStaticThroughputObject(new FileSender.StaticThroughputObject(13,7,17,1024));

    }

    /*
    public static void main(String[] args) throws Exception {
        FileSender myFileSender = new FileSender();

        //Add File Request to the main list

        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/5MB_File.dat WS12/home/lrodolph/5MB_File_Copy.dat");
        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/10MB_File.dat WS12/home/lrodolph/10MB_File_Copy.dat");
        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/15MB_File.dat WS12/home/lrodolph/15MB_File_Copy.dat");
        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/20MB_File.dat WS12/home/lrodolph/20MB_File_Copy.dat");
        FileSender.addFileRequestToList("transfer WS5/home/lrodolph/25MB_File.dat WS12/home/lrodolph/25MB_File_Copy.dat");



        for (String aFileRequest : FileSender.mainFileRequestList ){
            logger.info("File Request Added to the Main File Request List = " + aFileRequest);
        }


    //FileSender.addFileRequestToList("transfer WS5/home/lrodolph/10MB_File1.dat WS12/home/lrodolph/10MB_File_Copy1.dat");
    //FileSender.addFileRequestToList("transfer WS5/home/lrodolph/10MB_File2.dat WS12/home/lrodolph/10MB_File_Copy2.dat");
        logger.info("Size of mainFileRequestList =  " + FileSender.mainFileRequestList.size());


    //Add Paths to the Path List: IPAddress:Port,IPAddress:Port without src, Alias Name, Concurrency, Parallel Num, Pipeline Num
    //FileSender.addTempObjectToTempPathList("192.168.0.1:4959,192.168.1.2:4959", "WS5,WS7,WS12", 1, 1, 1);
        FileSender.addTempObjectToTempPathList("192.168.0.1:4959", "WS5,WS7", 1, 1, 1);
        FileSender.addPathDoneObjectToPathDoneList("WS5,WS7",1);


    //FileSender.addTempObjectToTempPathList("192.168.2.2:4959,192.168.3.2:4959", "WS5,WS11,WS12", 1, 2, 1);


    //FileSender.createFileRequestListPerPath("WS5,WS7");
    //Create a FileRequest List for each TempPathObject in the Temp Path List
    ////////////////////////////////////////////
    //FileSender.createFileRequestListPerPath();
    //////////////////////////////////////////////


    //FileSender.addFileRequestToPathList("WS5,WS7,WS12","transfer WS5/home/lrodolph/5MB_File.dat WS12/home/lrodolph/5MB_File_Copy.dat");

    //Create File Request List for the Path: WS5,WS7
        myPathAndFileRequestList.put("WS5,WS7", new ArrayList<String>());
        myPathAndFileRequestList.put("WS5,WS11,WS12,WS7", new ArrayList<String>());
    //Get the Array List associated with the Path: WS5, WS7
    ArrayList<String> myArrayList1 = FileSender.myPathAndFileRequestList.get("WS5,WS7");
    //Add file Requests to WS5,WS7 Array List
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File1.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File1_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File2.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File2_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File3.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File3_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File4.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File4_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File5.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File5_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File6.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File6_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File7.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File7_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File8.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File8_Copy.dat");
        myArrayList1.add("WS5/home/lrodolph/1GB_DIR/1GB_File9.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File9_Copy.dat");

    //Get the Array List associated with the Path: WS5,WS12,WS11,WS7
    ArrayList<String> myArrayList2 = FileSender.myPathAndFileRequestList.get("WS5,WS11,WS12,WS7");
        myArrayList2.add("WS5/home/lrodolph/1GB_DIR/1GB_File10.dat WS7/home/lrodolph/home/lrodolph/1GB_DIR/1GB_File10_Copy.dat");

    //Practicing removing file request from the Array List
    //1
    String aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //2
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //3
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //4
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //5
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //6
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //7
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //8
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //9
    aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    //10
        if (!myArrayList1.isEmpty()) {
        aFileRequest = myArrayList1.remove(0);
        logger.info("Removed File Request: " + aFileRequest + " from [WS5,WS] File Request List");
        logger.info("Size of Array List after Removal = " + myArrayList1.size());
    }



    Iterator<String> aFileRequestIterator = myArrayList1.iterator();


        for (String aFileRequest : FileSender.mainFileRequestList ) {

            myArrayList.add(aFileRequest);
        }



        ArrayList<String> myArrayList2 = FileSender.myPathAndFileRequestList.get("WS5,WS7");
        if (myArrayList2 != null){
            logger.info("MyArrayList2 IS NOT NULL");
            if (!myArrayList2.isEmpty()){
                logger.info("MyArrayList2 IS NOT EMPTY");
                for (String aFileRequest : myArrayList2)
                    logger.info("File Request for Path WS5,WS7 = " + aFileRequest);
            }
            else {
                logger.info("MyArrayList2 IS EMPTY");
            }
        }
        else {
            logger.info("MyArrayList2 IS NULL");
        }


    //Run File Distribution Algorithm
    //FileSender.runFileDistributionAlg("rr");

    /////////////////////////////
    //myFileSender.startFileSender();
    ///////////////////////////////


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



    //Do Below:
    //FileSender.ThroughputObject aThroughputObject = myFileSender.new ThroughputObject(11, 4, 7, 1024);
    //Add ThroughputObject is a Static Method and Class ThroughputObject is a non-static class

    //FileSender.addThroughputObject(myFileSender.new ThroughputObject(13,7,17,1024));
    //FileSender.addStaticThroughputObject(new FileSender.StaticThroughputObject(13,7,17,1024));

}
     */
}
