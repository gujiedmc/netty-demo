package com.gujiedmc.study.netty.ws.client;

import com.gujiedmc.study.netty.ws.server.StringMsgHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * websocket客户端
 *
 * @author gujiedmc
 * @date 2021-07-23
 */
@Slf4j
@Setter
@Getter
public class WsClient {

    /**
     * 是否启用ssl
     */
    private boolean ssl = false;
    /**
     * 主机
     */
    private String host = "127.0.0.1";
    /**
     * 端口
     */
    private int port = 9000;
    /**
     * 路径
     */
    private String path = "/";
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
     * 连接
     */
    private Channel channel;
    /**
     * 是否已经连接
     */
    private boolean connect;
    /**
     * string消息处理器
     */
    private SimpleChannelInboundHandler<?> wsMsgHandler = new StringMsgHandler();

    /**
     * 创建连接
     *
     * @return 连接
     */
    public Channel connect() {
        synchronized (this) {
            if (!connect) {
                doConnect();
            }
        }
        return null;
    }

    private void doConnect() {
        String url = getUrl();

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .group(group)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (ssl) {
                            SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                            pipeline.addLast("ssl", sslCtx.newHandler(socketChannel.alloc(), host, port));
                        }
                        pipeline.addLast(
                                new HttpClientCodec(),
                                new HttpObjectAggregator(1024 * 1024 * 10),
                                new WebSocketClientProtocolHandler(new URI(url), WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 60 * 1024),
                                new IdleStateHandler(readIdle, writeIdle, allIdle),
                                wsMsgHandler
                        );
                    }
                });
        try {
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            // 添加启动异常监听
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    log.info("Websocket client connected. url:{}, readIdle:{}, writeIdle:{}, allIdle:{}",
                            url, readIdle, writeIdle, allIdle);
                    this.channel = channelFuture.channel();
                } else {
                    log.error("Websocket Server start failure. Error cause: ", future.cause());
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("Websocket client connect interrupted", e);
        }
    }

    private String getUrl() {
        StringBuilder sb = new StringBuilder();
        if (ssl) {
            sb.append("wss://");
        } else {
            sb.append("ws://");
        }
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(path);
        return sb.toString();
    }

    public void sendMessage(String message) {
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    public boolean isConnect(){
        return channel != null;
    }
}
