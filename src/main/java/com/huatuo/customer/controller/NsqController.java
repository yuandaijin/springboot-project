package com.huatuo.customer.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.huatuo.customer.base.enums.MessageCenterState;
import com.huatuo.customer.base.enums.MessageCenterType;
import com.huatuo.customer.base.response.BaseResponse;
import com.huatuo.customer.base.util.Utils;
import com.huatuo.customer.domain.XtMessageCenter;
import com.huatuo.customer.service.LoginService;
import com.huatuo.customer.service.MessageCenterService;
import com.huatuo.register.domain.response.LoginDataResponse;

@RequestMapping(value = "nsq")
@RestController
public class NsqController {
	@Autowired
	private MessageCenterService messageCenterService;
	@Autowired
	private LoginService loginService;

	/**
	 * 返回未读消息列表,多个类型 最后一条在第一个
	 * 
	 * @param mzVisit
	 * @return
	 */
	@GetMapping(value = "selectLastMsg")
	public Object slectLast(HttpServletRequest request) {
		XtMessageCenter xtMessageCenter1 = new XtMessageCenter();
		String resultString = JSON.toJSONString(xtMessageCenter1, SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteNullStringAsEmpty);
		System.out.println(resultString);
		Map<String, Object> result = new HashMap<String, Object>();
		XtMessageCenter xtMessageCenter = new XtMessageCenter();
		LoginDataResponse loginDataResponse = loginService.getUserInfoByLogin(Utils.getToken(request));
		// 获取userId
		Long userId = Long.parseLong(loginDataResponse.getXtUser().getUserId());
		xtMessageCenter.setReceiveUserId(userId + "");
		xtMessageCenter.setState(MessageCenterState.C.getIndex());
		xtMessageCenter.setType(MessageCenterType.FREE.getIndex());
		result.put("FREE", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.FAMILY.getIndex());
		result.put("FAMILY", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.VIDEO.getIndex());
		result.put("VIDEO", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.BESPEAK.getIndex());
		result.put("BESPEAK", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.SYSTEM.getIndex());
		result.put("SYSTEM", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.ORDER.getIndex());
		result.put("ORDER", messageCenterService.select(xtMessageCenter));
		xtMessageCenter.setType(MessageCenterType.SERVICEORDER.getIndex());
		result.put("SERVICEORDER", messageCenterService.select(xtMessageCenter));
		result.put("code", BaseResponse.SUCCESS_CODE + "");
		return result;
	}

	/**
	 * 回调消息未读
	 * 
	 * @param mzVisit
	 * @return
	 */
	@GetMapping(value = "callBackUnread")
	public BaseResponse callBackUnread(String messageCenterId) {
		BaseResponse baseResponse = new BaseResponse();
		System.out.println(messageCenterId);
		String[] s = messageCenterId.split("\\,");
		for (String a : s) {
			XtMessageCenter xtMessageCenter = new XtMessageCenter();
			xtMessageCenter.setMessageCenterId(a);
			JSON.toJSONString(xtMessageCenter, SerializerFeature.WriteMapNullValue,
					SerializerFeature.WriteNullStringAsEmpty);
			List<XtMessageCenter> xtMessageCenters = messageCenterService.select(xtMessageCenter);
			if (xtMessageCenters.size() > 0) {
				xtMessageCenter = xtMessageCenters.get(0);
				xtMessageCenter.setState(MessageCenterState.A.getIndex());
				messageCenterService.updateByPrimaryKey(xtMessageCenter);
				baseResponse.setCode(BaseResponse.SUCCESS_CODE);
				baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
			} else {
				baseResponse.setMessage("未找到该消息");
				baseResponse.setCode(BaseResponse.ERROR_CODE);
			}
		}
		return baseResponse;
	}

	/**
	 * 回调消息已读
	 * 
	 * @param mzVisit
	 * @return
	 */
	@GetMapping(value = "callBackSuccess")
	public BaseResponse updateMsg(String messageCenterId) {
		BaseResponse baseResponse = new BaseResponse();
		System.out.println(messageCenterId);
		String[] s = messageCenterId.split("\\,");
		for (String a : s) {
			XtMessageCenter xtMessageCenter = new XtMessageCenter();
			xtMessageCenter.setMessageCenterId(a);
			JSON.toJSONString(xtMessageCenter, SerializerFeature.WriteMapNullValue,
					SerializerFeature.WriteNullStringAsEmpty);
			List<XtMessageCenter> xtMessageCenters = messageCenterService.select(xtMessageCenter);
			if (xtMessageCenters.size() > 0) {
				xtMessageCenter = xtMessageCenters.get(0);
				xtMessageCenter.setState(MessageCenterState.B.getIndex());
				messageCenterService.updateByPrimaryKey(xtMessageCenter);
				baseResponse.setCode(BaseResponse.SUCCESS_CODE);
				baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
			} else {
				baseResponse.setMessage("未找到该消息");
				baseResponse.setCode(BaseResponse.ERROR_CODE);
			}
		}
		return baseResponse;
	}

	@GetMapping(value = "delMessage")
	public BaseResponse delMessage(String messageCenterId) {
		System.out.println(messageCenterId);
		BaseResponse baseResponse = new BaseResponse();
		String[] s = messageCenterId.split("\\,");
		for (String a : s) {
			XtMessageCenter xtMessageCenter = new XtMessageCenter();
			xtMessageCenter.setMessageCenterId(a);
			List<XtMessageCenter> xtMessageCenters = messageCenterService.select(xtMessageCenter);
			if (xtMessageCenters.size() > 0) {
				xtMessageCenter = xtMessageCenters.get(0);
				xtMessageCenter.setState(MessageCenterState.D.getIndex());
				messageCenterService.updateByPrimaryKey(xtMessageCenter);
				baseResponse.setCode(BaseResponse.SUCCESS_CODE);
				baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
			} else {
				baseResponse.setMessage("未找到该消息");
				baseResponse.setCode(BaseResponse.ERROR_CODE);
			}
		}
		return baseResponse;
	}

	/**
	 * 回调消息已读
	 * 
	 * @param mzVisit
	 * @return
	 */
	@GetMapping(value = "callBackSuccessByDoctorId")
	public BaseResponse callBackSuccessByDoctorId(String doctorId, String patientId, Integer type) {
		BaseResponse baseResponse = new BaseResponse();
		XtMessageCenter xtMessageCenter = new XtMessageCenter();
		if (doctorId != null && !doctorId.equals("")) {
			xtMessageCenter.setSponsorUserId(doctorId);
		}
		xtMessageCenter.setReceiveUserId(patientId);
		xtMessageCenter.setType(type);
		xtMessageCenter.setState(MessageCenterState.C.getIndex());
		List<XtMessageCenter> xtMessageCenters = messageCenterService.select(xtMessageCenter);
		for (XtMessageCenter xtMessageCenter1 : xtMessageCenters) {
			xtMessageCenter1.setState(MessageCenterState.B.getIndex());
			messageCenterService.updateByPrimaryKey(xtMessageCenter1);
		}
		baseResponse.setCode(BaseResponse.SUCCESS_CODE);
		baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
		return baseResponse;
	}

	/**
	 * @param mzVisit
	 * @return
	 */
	@GetMapping(value = "delMessageByDoctorId")
	public BaseResponse delMessageByDoctorId(String patientId, String doctorId, Integer type) {
		BaseResponse baseResponse = new BaseResponse();
		XtMessageCenter xtMessageCenter = new XtMessageCenter();
		if (doctorId != null && !doctorId.equals("")) {
			xtMessageCenter.setSponsorUserId(doctorId);
		}
		xtMessageCenter.setReceiveUserId(patientId);
		xtMessageCenter.setType(type);
		List<XtMessageCenter> xtMessageCenters = messageCenterService.select(xtMessageCenter);
		for (XtMessageCenter xtMessageCenter1 : xtMessageCenters) {
			xtMessageCenter1.setState(MessageCenterState.D.getIndex());
			messageCenterService.updateByPrimaryKey(xtMessageCenter1);
		}
		baseResponse.setCode(BaseResponse.SUCCESS_CODE);
		baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
		return baseResponse;
	}

	@GetMapping(value = "add")
	public BaseResponse add(String xtMessageCenter) throws Exception {
		BaseResponse baseResponse = new BaseResponse();
		NsqWebSocket.push(xtMessageCenter);
		baseResponse.setCode(BaseResponse.SUCCESS_CODE);
		baseResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
		return baseResponse;
	}
}