package com.huatuo.customer.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huatuo.customer.base.page.Page;
import com.huatuo.customer.base.response.BaseResponse;
import com.huatuo.customer.domain.XtZdDistrict;
import com.huatuo.customer.domain.ZdOrg;
import com.huatuo.customer.query.ZdOrgQuery;
import com.huatuo.customer.response.ZdOrgResponse;
import com.huatuo.customer.service.XtZdDistrictService;
import com.huatuo.customer.service.ZdOrgService;

@RequestMapping(value = "org")
@RestController
/**
 * 医院管理
 * @author master
 *
 */
public class ZdOrgController {
	
	@Autowired
	private ZdOrgService zdOrgService;
	
	@Autowired
	private XtZdDistrictService xtZdDistrictService;
	
	/**
	 * 分页查找
	 * @param zdOrgQuery
	 * @return
	 */
	@PostMapping(value = "selectOrgsByPage")
	public Object getOrgsByPage(
			@RequestBody ZdOrgQuery zdOrgQuery){
		ZdOrgResponse zdOrgResponse = new ZdOrgResponse();
		Page<ZdOrg> page = zdOrgService.selectOrgsByPage(zdOrgQuery);
		zdOrgResponse.setPage(page);
		zdOrgResponse.setCode(BaseResponse.SUCCESS_CODE);
		zdOrgResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
		return zdOrgResponse;
	}
	
	/**
	 * 医院详情获取
	 * @return
	 */
	@PostMapping(value = "getOrgDetails")
	public Object getOrgDetials(
			@RequestBody ZdOrgQuery zdOrgQuery){
		ZdOrgResponse zdOrgResponse = new ZdOrgResponse();
		ZdOrg zdOrg = zdOrgService.selectOrgDetailsById(zdOrgQuery);
		zdOrgResponse.setZdOrg(zdOrg);
		zdOrgResponse.setCode(BaseResponse.SUCCESS_CODE);
		zdOrgResponse.setMessage(BaseResponse.SUCCESS_MESSAGE);
		return zdOrgResponse;
	}
	
	/**
	 * 根据定位查询周围的5个医院
	 * @return
	 */
	@PostMapping(value = "selectZdOrgByLocationData")
	public Object selectZdOrgByLocationData(@RequestBody ZdOrgQuery zdOrgQuery){
		ZdOrgResponse zdOrgResponse = new ZdOrgResponse();
		XtZdDistrict xtZdDistrict = xtZdDistrictService.selectDistrictByLocationData(
				zdOrgQuery.getProvinceName());
		if(xtZdDistrict != null){
			zdOrgResponse.setList(zdOrgService.selectOrgListByLocationData(xtZdDistrict.getId()));
		}
		return zdOrgResponse;
	}
}
