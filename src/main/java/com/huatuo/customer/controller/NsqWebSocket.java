package com.huatuo.customer.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.huatuo.customer.base.enums.MessageCenterState;
import com.huatuo.customer.base.nsq.NSQConsumer;
import com.huatuo.customer.base.nsq.NSQMessage;
import com.huatuo.customer.base.nsq.NSQProducer;
import com.huatuo.customer.base.nsq.callbacks.NSQMessageCallback;
import com.huatuo.customer.base.nsq.exceptions.NSQException;
import com.huatuo.customer.base.nsq.lookup.DefaultNSQLookup;
import com.huatuo.customer.base.nsq.lookup.NSQLookup;
import com.huatuo.customer.base.util.ConfigProperites;
import com.huatuo.customer.config.SpringContextUtils;
import com.huatuo.customer.domain.XtMessageCenter;
import com.huatuo.customer.service.LoginService;
import com.huatuo.customer.service.MessageCenterService;
import com.huatuo.register.domain.response.LoginDataResponse;

public class NsqWebSocket extends TextWebSocketHandler {
	private static String nsqAdress = ConfigProperites.getNsqAdress();
	private static String topic = "msg_topic";
	private static NSQProducer producer = new NSQProducer();
	private static NSQConsumer consumer = null;
	private static Map<String, WebSocketSession> clients = new ConcurrentHashMap<>();
	@Autowired
	private static MessageCenterService messageCenterService = (MessageCenterService) SpringContextUtils
			.getBean("messageCenterService");
	@Autowired
	private static LoginService loginService = (LoginService) SpringContextUtils.getBean("loginServiceImpl");
	private static Thread ClearThread = null;
	private static final Logger logger = LoggerFactory.getLogger(NsqWebSocket.class);
	{
		producer.addAddress(nsqAdress, 4150);
		producer.start();
		NSQLookup lookup = new DefaultNSQLookup();
		lookup.addLookupAddress(nsqAdress, 4161);
		// String channel="";
		// String os = System.getProperty("os.name");
		// if(os.toLowerCase().startsWith("win")){
		// channel=getLocalMac();
		// } else {
		// channel=getLocalMacLinux();
		// }
		// logger.info(getLocalMacLinux() + "---------" + getLocalMac());
		consumer = new NSQConsumer(lookup, topic, UUID.randomUUID().toString(), new NSQMessageCallback() {
			@Override
			public void message(NSQMessage message) {
				String msg = new String(message.getMessage());
				try {
					logger.info(msg);
					sendToOne(msg);
				} catch (Exception e) {
					logger.info(e.getMessage(), e);
				}
				message.finished();
			}
		});
		consumer.start();
		ClearThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					for (WebSocketSession session : clients.values()) {
						if (!session.isOpen()) {
							clients.remove(session.getAttributes().get("id").toString());
						}
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		ClearThread.start();
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws NSQException, TimeoutException {
		try {
			String json = message.getPayload();
			Map<String, Object> attributes = session.getAttributes();
			if (toMap(json).get("token") != null) {
				if (toMap(json).get("token").toString().equals("")) {
					String uuid = UUID.randomUUID().toString();
					if (attributes.get("id") != null && !attributes.get("id").toString().equals("")) {
						clients.remove(attributes.get("id").toString());
					}
					attributes.put("id", uuid);
					clients.put(uuid, session);
				} else {
					LoginDataResponse loginDataResponse = loginService
							.getUserInfoByLogin(toMap(json).get("token").toString());
					clients.put(loginDataResponse.getXtUser().getUserId(), session);
					if (attributes.get("id") != null && !attributes.get("id").toString().equals("")) {
						clients.remove(attributes.get("id").toString());
					}
					attributes.put("id", loginDataResponse.getXtUser().getUserId());
				}
				return;
			}
			if (toMap(json).get("userId") != null && !toMap(json).get("userId").toString().equals("")) {
				clients.put(toMap(json).get("userId").toString(), session);
				attributes.put("id", toMap(json).get("userId").toString());
				return;
			}
			push(json);
		} catch (Exception e) {
			// e.printStackTrace();
		}

	}

	private static void sendToOne(String msg) {
		try {
			XtMessageCenter xtMessageCenter = JSON.parseObject(msg, XtMessageCenter.class);
			if (xtMessageCenter.getReceiveUserId() == null || xtMessageCenter.getReceiveUserId().equals("")) {
				return;
			}
			String[] ReceiveUserIds = xtMessageCenter.getReceiveUserId().split("\\,");
			String[] ReceiveUserNames = xtMessageCenter.getReceiveUserName().split("\\,");
			for (int i = 0; i < ReceiveUserIds.length; i++) {
				WebSocketSession webSocketSession = clients.get(xtMessageCenter.getReceiveUserId());
				XtMessageCenter xtMessageCenterTemp = xtMessageCenter.clone();
				xtMessageCenterTemp.setMessageCenterId(xtMessageCenter.getMessageCenterId() + String.valueOf(i));
				xtMessageCenterTemp.setReceiveUserId(ReceiveUserIds[i]);
				xtMessageCenterTemp.setReceiveUserName(ReceiveUserNames[i]);
				xtMessageCenterTemp.setState(MessageCenterState.C.getIndex());
				XtMessageCenter record = new XtMessageCenter();
				record.setMessageCenterId(xtMessageCenterTemp.getMessageCenterId());
				if (messageCenterService.select(record).size() == 0) {
					messageCenterService.insert(xtMessageCenterTemp);
				}
				if (webSocketSession == null || !webSocketSession.isOpen()) {
					logger.info("用户未上线" + xtMessageCenter.getReceiveUserId());
				} else {
					try {
						synchronized (webSocketSession) {
							xtMessageCenterTemp.setState(MessageCenterState.A.getIndex());
							webSocketSession.sendMessage(new TextMessage(xtMessageCenterTemp.toJson()));
							messageCenterService.updateByPrimaryKey(xtMessageCenterTemp);
						}
					} catch (IOException e) {
						//logger.info(e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			//logger.info(e.getMessage(), e);
		}
	}

	public static void push(String msg) throws TimeoutException, NSQException {
		logger.info("用户push" + msg);
		producer.produce(topic, msg.getBytes());
	}

	@SuppressWarnings("unused")
	private static String getLocalMac() {
		StringBuffer sb = new StringBuffer("");
		try {
			InetAddress ia = InetAddress.getLocalHost();
			byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
			for (int i = 0; i < mac.length; i++) {
				if (i != 0) {
					sb.append("-");
				}
				int temp = mac[i] & 0xff;
				String str = Integer.toHexString(temp);
				if (str.length() == 1) {
					sb.append("0" + str);
				} else {
					sb.append(str);
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return sb.toString().toUpperCase();
	}

	@SuppressWarnings("rawtypes")
	public static Map toMap(String jsonString) throws JSONException {
		Map tempMap = JSON.parseObject(jsonString, HashMap.class);
		return tempMap;
	}

	public static String getLocalMacLinux() {
		String mac = "";
		try {
			Process p = new ProcessBuilder("ifconfig").start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				Pattern pat = Pattern.compile("\\b\\w+:\\w+:\\w+:\\w+:\\w+:\\w+\\b");
				Matcher mat = pat.matcher(line);
				if (mat.find()) {
					mac = mat.group(0);
				}
			}
			br.close();
		} catch (IOException e) {
		}
		return mac;
	}
}
