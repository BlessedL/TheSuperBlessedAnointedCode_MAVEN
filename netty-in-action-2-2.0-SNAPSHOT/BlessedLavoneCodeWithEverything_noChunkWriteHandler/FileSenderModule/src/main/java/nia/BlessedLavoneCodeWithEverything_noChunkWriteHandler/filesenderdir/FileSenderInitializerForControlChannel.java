//package io.netty.example.oneHopSingleFileTransfer;
//package nia.BlessedLavoneCodeWithEverything.filesenderdir;
package nia.BlessedLavoneCodeWithEverything_noChunkWriteHandler.filesenderdir;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.codec.*;
import io.netty.handler.stream.*;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.channel.ChannelPipeline;


/**
 * Created by lrodolph on 9/7/16.
 */

public class FileSenderInitializerForControlChannel extends ChannelInitializer<SocketChannel> {

    /*
      new FileSenderInitializer(fileRequest, offset, currentFragmentSize, dataChannelId  ))
     */

    private String aliasPathString, pathInIpAddressFormatWithoutSrc;
    private int channelType;
    private int controlChannelId;
    private int dataChannelId;
    private FileSender fileSender;
    private int parallelNum, concurrencyNum, pipelineNum;
    //private EventExecutorGroup aNonIoGroup;

    //public FileSenderInitializerForControlChannel(EventExecutorGroup aNonIoGroup, String aPathInIpAddressFormatWithoutSrc, String anAliasPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, int aPipelineNum) {
    public FileSenderInitializerForControlChannel(String aPathInIpAddressFormatWithoutSrc, String anAliasPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum, int aPipelineNum) {
        this.pathInIpAddressFormatWithoutSrc = aPathInIpAddressFormatWithoutSrc;
        this.aliasPathString = anAliasPathString ;
        this.channelType = aChannelType;
        this.controlChannelId = aControlChannelId;
        this.dataChannelId = aDataChannelId;
        this.fileSender = aFileSender;
        this.concurrencyNum = aConcurrencyNum;
        this.parallelNum = aParallelNum;
        this.pipelineNum = aPipelineNum;
        //this.aNonIoGroup = aNonIoGroup;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new FileSenderControlChannelHandler(pathInIpAddressFormatWithoutSrc,aliasPathString, channelType ,controlChannelId, dataChannelId, fileSender,concurrencyNum, parallelNum, pipelineNum));
        //pipeline.addLast(aNonIoGroup, new ControlChannelShadowHandler(pathInIpAddressFormatWithoutSrc,aliasPathString, channelType ,controlChannelId, dataChannelId, fileSender,concurrencyNum, parallelNum, pipelineNum);
/*
                ch.pipeline().addLast(
                //new LengthFieldPrepender(8),
                //new ChunkedWriteHandler(),
                new FileSenderControlChannelHandler(pathInIpAddressFormatWithoutSrc,aliasPathString, channelType ,controlChannelId, dataChannelId, fileSender,concurrencyNum, parallelNum, pipelineNum));
                new ControlChannelHandlerShadow
*/
                //pipeline.addLast(group,"serverHandler",new ServerHandler());
    //}

    }
}
