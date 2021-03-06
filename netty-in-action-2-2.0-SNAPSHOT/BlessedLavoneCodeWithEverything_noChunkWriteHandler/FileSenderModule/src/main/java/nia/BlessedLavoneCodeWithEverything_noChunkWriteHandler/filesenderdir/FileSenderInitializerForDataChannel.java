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


/**
 * Created by lrodolph on 9/7/16.
 */

public class FileSenderInitializerForDataChannel extends ChannelInitializer<SocketChannel> {

    /*
      new FileSenderInitializer(fileRequest, offset, currentFragmentSize, dataChannelId  ))
     */

    private String pathInIpAddressFormatWithoutSrc, aliasPathString;
    private int channelType;
    private int controlChannelId;
    private int dataChannelId;
    private FileSender fileSender;
    private int parallelNum, concurrencyNum;
                                               //pathString,      this.DATA_CHANNEL_TYPE, controlChannelId, dataChannelId,       this,                   myConcurrencyNum,     myParallelNum
    public FileSenderInitializerForDataChannel(String aPathInIpAddressFormatWithoutSrc, String anAliasPathString, int aChannelType, int aControlChannelId, int aDataChannelId, FileSender aFileSender, int aConcurrencyNum, int aParallelNum) {
        this.pathInIpAddressFormatWithoutSrc = aPathInIpAddressFormatWithoutSrc;
        this.aliasPathString = anAliasPathString;
        this.channelType = aChannelType;
        this.controlChannelId = aControlChannelId;
        this.dataChannelId = aDataChannelId;
        this.fileSender = aFileSender;
        this.concurrencyNum = aConcurrencyNum;
        this.parallelNum = aParallelNum;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                //new LengthFieldPrepender(8),
                new ChunkedWriteHandler(),
                new FileSenderDataChannelHandler(pathInIpAddressFormatWithoutSrc, aliasPathString, channelType ,controlChannelId, dataChannelId, fileSender,concurrencyNum, parallelNum));
    }
}
