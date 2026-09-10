package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
//两类的类,一个tomcat根据请求每次new一个实例对象进行连接操作,一个spring管理,用于业务代码调用,向前端发消息,公用session信息
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    //存放会话对象
    //每个key就是对话对象id,储存session信息
    //使用 ConcurrentHashMap：WebSocket 连接/断开与业务线程群发是并发的，HashMap 并发读写可能丢数据甚至死循环
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("客户端：{}建立连接", sid);
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端：{}的信息:{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("连接断开:{}", sid);
        sessionMap.remove(sid);
    }

    /**
     * 群发
     *
     * @param message
     */
    //不属于tomcat服务器调用,spring的注入类调用时候使用这个业务代码
    public void sendToAllClient(String message) {
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            Session session = entry.getValue();
            try {
                //连接已失效则清理，避免 sessionMap 无限增长、内存泄漏
                if (!session.isOpen()) {
                    sessionMap.remove(entry.getKey(), session);
                    continue;
                }
                //使用异步发送：getBasicRemote 是阻塞式的，一个慢客户端会拖住整个群发循环
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("WebSocket消息发送失败，移除失效会话：{}", entry.getKey(), e);
                sessionMap.remove(entry.getKey(), session);
            }
        }
    }

}
