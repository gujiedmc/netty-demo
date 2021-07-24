package com.gujiedmc.study.netty.ws.server;

import lombok.extern.slf4j.Slf4j;

/**
 * websocket server demo
 *
 * @author gujiedmc
 * @date 2021-07-24
 */
@Slf4j
public class WsServerDemo {

    public static void main(String[] args) {
        WsServer wsServer = new WsServer();
        wsServer.setReadIdle(1);
        wsServer.setAllIdle(10);
        wsServer.start();
    }
}
