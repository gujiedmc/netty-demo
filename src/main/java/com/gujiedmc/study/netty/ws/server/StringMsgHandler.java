package com.gujiedmc.study.netty.ws.server;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

/**
 * 简单websocket string消息处理器
 *
 * @author gujiedmc
 * @date 2021-07-23
 */
@Slf4j
@ChannelHandler.Sharable
public class StringMsgHandler extends SimpleChannelInboundHandler<Object> {

    private final BiConsumer<Channel, String> textMsgConsumer;

    public StringMsgHandler() {
        this.textMsgConsumer = (channel, receiveMsg) -> {
            log.info("receive msg.client:{}, msg:{}", channel.id(), receiveMsg);
        };
    }

    public StringMsgHandler(BiConsumer<Channel, String> textMsgConsumer) {
        this.textMsgConsumer = textMsgConsumer;
    }

    /**
     * 连接首次创建
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("client connect: {}", ctx.channel().id());
    }

    /**
     * 连接断开。包括客户端请求断开，服务端主动断开，服务端异常断开
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("client disconnect: {}", ctx.channel().id());
    }

    /**
     * 读取消息
     *
     * @param channelHandlerContext 上下文
     * @param requestSocketFrame    请求内容
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object requestSocketFrame) {
        if (requestSocketFrame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) requestSocketFrame;
            textMsgConsumer.accept(channelHandlerContext.channel(), textWebSocketFrame.text());
        } else {
            log.warn("unsupported msg type. {}", requestSocketFrame.getClass().getName());
        }
    }

    /**
     * 异常拦截器。
     *
     * @param ctx   上下文
     * @param cause 异常原因
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("client error: {}", ctx.channel().id(), cause);
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 事件触发
     *
     * @param ctx 上下文
     * @param evt 事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {

        Channel channel = ctx.channel();
        ChannelId channelId = channel.id();

        //判断evt是否是IdleStateEvent(用于触发用户事件，包含读空闲/写空闲/读写空闲)
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;

            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.debug("client read idle. client: {}", channelId);
            } else if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                log.debug("client write idle. client: {}", channelId);
            } else if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                log.debug("write read and idle, close client: {}", channelId);
                channel.close();
            }
        }
    }
}
