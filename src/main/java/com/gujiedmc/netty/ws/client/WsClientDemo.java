package com.gujiedmc.netty.ws.client;

import lombok.extern.slf4j.Slf4j;

/**
 * websocket client demo
 *
 * @author gujiedmc
 * @date 2021-07-24
 */
@Slf4j
public class WsClientDemo {

    public static void main(String[] args) {
        WsClient wsClient = new WsClient();
        wsClient.connect();

        boolean flag = true;
        while (flag) {
            if (wsClient.isConnect()) {
                wsClient.sendMessage("PING");
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                log.debug("ws client send message sleep error", e);
            }
        }
    }
}
