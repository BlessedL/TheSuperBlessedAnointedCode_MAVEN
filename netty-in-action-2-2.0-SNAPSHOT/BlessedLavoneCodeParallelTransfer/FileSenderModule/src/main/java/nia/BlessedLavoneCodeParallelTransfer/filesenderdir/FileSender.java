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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
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

/*
  Write contents to a file when reading, after reading length amount of bytes
 */
public final class FileSender {

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

    public FileSender() {
        myParallelNum = 1;
        myParallelNum2 = 1;
        myConcurrencyNum = 1;

        //File Name
        fileName = null;
        fileRequest = null;

        fileName2 = null;
        fileRequest2 = null;

        throughputObjectList = new ArrayList<ThroughputObject>();


    }

    public static void addThroughputObject( FileSender.StaticThroughputObject aThroughputObject)
    {
        staticThroughputObjectList.add(aThroughputObject);
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



    public void startFileSender() throws Exception{
        //Parallel Num
        myParallelNum = 1;
        myParallelNum2 =1;

        //File Name
        fileName = "/home/lrodolph/100MB_File.dat";
        fileRequest = "transfer N0/home/lrodolph/100MB_File.dat N1/home/lrodolph/100MB_File_Copy.dat";

        fileName2 = "/home/lrodolph/100MB_File2.dat";
        fileRequest2 = "transfer N0/home/lrodolph/100MB_File.dat N1/home/lrodolph/100MB_File_Copy2.dat";


        //Get the file & File Length
        File theFile = new File(fileName);
        //Get the File Length
        FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();

        long length = theFileChannel.size();
        long offSet = 0;
        long remainingFragmentSize = length;
        long currentFragmentSize = 0;
        int parallel_counter = 0;
        long leftOverBytes = 0;

        //Create parallel connection
        //Pass in the fragment Size,

        //get the file fragment size that will be sent through each parallel TCP Stream
        currentFragmentSize = Math.round(Math.floor((double) (length / myParallelNum)));
        leftOverBytes = length - (currentFragmentSize * myParallelNum);
        logger.info("FileSenderHandler:Active: File Length = " + length + ", Current Fragment Size = " + currentFragmentSize + " leftOverBytes = " + leftOverBytes);
        ////////////////////////////////////
        //Connect the parallel data channels
        ////////////////////////////////////
        int dataChannelId = 0;

        /////////////////////////////////////////

        //CONCURRENCY = 2

        ///////////////////////////////////////////

        //Get the file & File Length
        File theFile2 = new File(fileName2);
        //Get the File Length
        FileChannel theFileChannel2 = new RandomAccessFile(theFile2, "r").getChannel();

        long length2 = theFileChannel2.size();
        long offSet2 = 0;
        long remainingFragmentSize2 = length2;
        long currentFragmentSize2 = 0;
        int parallel_counter2 = 0;
        long leftOverBytes2 = 0;

        //Create parallel connection
        //Pass in the fragment Size,

        //get the file fragment size that will be sent through each parallel TCP Stream
        currentFragmentSize2 = Math.round(Math.floor((double) (length2 / myParallelNum2)));
        leftOverBytes2 = length2 - (currentFragmentSize2 * myParallelNum2);
        logger.info("FileSenderHandler:Active: File Length = " + length2 + ", Current Fragment Size = " + currentFragmentSize2 + " leftOverBytes = " + leftOverBytes2);
        ////////////////////////////////////
        //Connect the parallel data channels
        ////////////////////////////////////
        int dataChannelId2 = 0;

        // Configure the File Sender
        EventLoopGroup group = new NioEventLoopGroup();
        ArrayList<ChannelFuture> channelFutureList = new ArrayList<ChannelFuture>();
        try {
            for (int i =0; i<myParallelNum; i++) {
                if ((parallel_counter + 1) >= myParallelNum) {
                    currentFragmentSize += leftOverBytes;
                }
                dataChannelId++;
                logger.info("FileSender: DataChannel: " + dataChannelId + " ,offset = " + offSet + " Current Fragment Size " + currentFragmentSize);

                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new FileSenderInitializer(this,fileRequest, offSet, currentFragmentSize, dataChannelId  ));

                //Actually connect to the proxyServer and use sync to wait until the connection is completed, but this isn't really asynchronous until we use listeners to wait, since it is blocking
                //Using "sync" blocks until the channel is connected.
                //What does it block? the current thread of execution? meaning it doesn't move to the next statement until
                //the channel is connected. Which means until the (the channelFuture has returned that it is connected)
                ChannelFuture f = b.connect(HOST, PORT).sync();
                channelFutureList.add(f);
                //I have to test to see if the channels are sending data one at a time and one after the other
                //or actuall in parallel. I can step through the debuger and see. Also put print statements in the receiver handler
                //and have the file sender send the channel id
                //Channel theDataChannel = f.channel();
                //Wait until the connection is closed
                //f.channel().closeFuture().sync(); --> If I do this here, the first channel will block all the operations from happening
                //logger.info("FileSender: CLOSED CONNECTION TO WS7");
                //logger.info("FileSender: dataChannel " + dataChannelId + " Reached the statement that will updatethe offset, the new offset = offset(" + offSet + ") + currentFragmentSize(" + currentFragmentSize + ")");
                offSet+=currentFragmentSize;
                //parallel_counter++;
            }

            for (int j =0; j<myParallelNum2; j++) {
                if ((parallel_counter2 + 1) >= myParallelNum2) {
                    currentFragmentSize2 += leftOverBytes2;
                }
                dataChannelId2++;
                logger.info("FileSender: DataChannel: " + dataChannelId2 + " ,offset = " + offSet2 + " Current Fragment Size " + currentFragmentSize2);

                Bootstrap b2 = new Bootstrap();
                b2.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new FileSenderInitializer(this,fileRequest2, offSet2, currentFragmentSize2, dataChannelId2  ));

                //Actually connect to the proxyServer and use sync to wait until the connection is completed, but this isn't really asynchronous until we use listeners to wait, since it is blocking
                //Using "sync" blocks until the channel is connected.
                //What does it block? the current thread of execution? meaning it doesn't move to the next statement until
                //the channel is connected. Which means until the (the channelFuture has returned that it is connected)
                ChannelFuture f2 = b2.connect(HOST, PORT).sync();
                channelFutureList.add(f2);
                //I have to test to see if the channels are sending data one at a time and one after the other
                //or actuall in parallel. I can step through the debuger and see. Also put print statements in the receiver handler
                //and have the file sender send the channel id
                //Channel theDataChannel = f.channel();
                //Wait until the connection is closed
                //f.channel().closeFuture().sync(); --> If I do this here, the first channel will block all the operations from happening
                //logger.info("FileSender: CLOSED CONNECTION TO WS7");
                //logger.info("FileSender: dataChannel " + dataChannelId + " Reached the statement that will updatethe offset, the new offset = offset(" + offSet + ") + currentFragmentSize(" + currentFragmentSize + ")");
                offSet2+=currentFragmentSize2;
                //parallel_counter++;
            }


            Iterator<ChannelFuture>   channelFutureListIterator = channelFutureList.iterator();
            //Iterate through File Request List
            int counter = 1;


            while (channelFutureListIterator.hasNext()) {
                ChannelFuture aChannelFuture = channelFutureListIterator.next();
                //Wait until the connection is closed
                /*
                 http://stackoverflow.com/questions/24237142/netty-app-hangs-when-i-try-to-close-a-io-netty-channel-channel-using-closefuture
                 closeFuture.sync() is not for closing the Channel but rather allows you to block until the Channel is closed. For closing the Channel you would call Channel.close().

                 http://stackoverflow.com/questions/41505852/netty-closefuture-sync-channel-blocks-rest-api
                 So you might find this code in various example because generally you start the server (bind) in the main thread or so, and then if you don't wait for something,
                 the main thread will end up, giving your JVM finishing, and therefore your server to stop immediately after starting.
                 So basically I need to use  aChannelFuture.channel().closeFuture().sync() to keep this main program (thread) up, or it will finish and
                 shut down the handlers too, since the handler is also running from the main thread

                  I have to test

                  Does not closing one of the connections causes the main program to end, terminate
                 */
                //Keep the connection up, until I press control c to close it or unitl I actually write channel().close() in the handler Wait until the connection is closed
                aChannelFuture.channel().closeFuture().sync();
                //aChannelFuture.channel().closeFuture();
                //Add the File Reuest List to the Path's File Request List
                logger.info("Waiting for data channel "+ counter + " to close");
                counter++;
            }

            logger.info("File Sender created the connections and is now outside of the for loop");
            // Wait until the connection is closed.
            //f.channel().closeFuture().sync();
            logger.info("FileSender: CLOSED CONNECTION TO WS7");
            logger.info("FileSender: Throughput Information: " + throughputInfoToString());
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
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
        //ThroughputObject aThroughputObject = new ThroughputObject(11, 4, 7, 1024);
        FileSender.ThroughputObject aThroughputObject = myFileSender.new ThroughputObject(11, 4, 7, 1024);
        //Add ThroughputObject is a Static Method and Class ThroughputObject is a non-static class

        //FileSender.addThroughputObject(myFileSender.new ThroughputObject(13,7,17,1024));
        FileSender.addThroughputObject(new FileSender.StaticThroughputObject(13,7,17,1024));

    }
}
