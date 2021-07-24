package com.gujiedmc.study.netty.ws.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * websocket服务器
 *
 * @author gujiedmc
 * @date 2021-07-23
 */
@Slf4j
@Setter
public class WsServer {

    /**
     * 用于处理nio的Accept的线程数量
     */
    private int bossThread = 1;
    /**
     * 用于处理nio的Read和Write事件的线程数量
     */
    private int workerThread = 1;
    /**
     * 绑定ip
     */
    private String host = "0.0.0.0";
    /**
     *
     */
    private String path = "/";
    /**
     * 绑定端口
     */
    private int port = 9000;
    /**
     * 消息长度
     */
    private int maxContentLength = 64 * 1024;
    /**
     * read idle
     */
    private int readIdle = Integer.MAX_VALUE;
    /**
     * write idle
     */
    private int writeIdle = Integer.MAX_VALUE;
    /**
     * all idle
     */
    private int allIdle = Integer.MAX_VALUE;
    /**
     * string消息处理器
     */
    private SimpleChannelInboundHandler<?> wsMsgHandler = new StringMsgHandler((channel, receiveMsg) -> {
        log.info("receive msg. client:{}, msg:{}", channel.id(), receiveMsg);
        channel.writeAndFlush(new TextWebSocketFrame("receive msg: " + receiveMsg));
    });

    /**
     * 启动一个websocket连接
     */
    public void start() {
        // 创建用于连接的线程组
        EventLoopGroup boss = new NioEventLoopGroup(bossThread);
        // 创建用于处理消息读写的线程组
        EventLoopGroup worker = new NioEventLoopGroup(workerThread);

        // 启动一个netty server
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                // nio模式
                .channel(NioServerSocketChannel.class)
                // log
                .handler(new LoggingHandler(LogLevel.DEBUG))
                // 为Accept的Channel初始化处理器链
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        channelPipeline.addLast(
                                // 编码或解码http消息，websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                                new HttpServerCodec(),
                                // 将HttpMessage和HttpContent聚合成FullHttpRequest或者FullHttpResponse
                                new HttpObjectAggregator(maxContentLength),
                                // 用于支持文件之类的大的数据流异步传输
                                new ChunkedWriteHandler(),
                                // websocket协议
                                new WebSocketServerProtocolHandler(path),
                                // 心跳检测
                                new IdleStateHandler(readIdle, writeIdle, allIdle),
                                // 自定义消息处理器
                                wsMsgHandler
                        );
                    }
                });

        // 启动netty服务
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.bind(host, port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("Websocket Server start interrupted", e);
        }

        // 添加启动异常监听
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("Websocket Server started. host:{}, path:{}, port:{}, bossThread:{}, workerThread:{}, " +
                                "readIdle:{}, writeIdle:{}, allIdle:{}",
                        host, path, port, bossThread, workerThread, readIdle, writeIdle, allIdle);
            } else {
                log.error("Websocket Server start failure. Error cause: ", future.cause());
            }
        });

        //挂掉的时候 关掉 线程
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }
}
