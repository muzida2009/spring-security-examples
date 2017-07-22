package com.ericsson.volteapp.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ericsson.volteapp.core.util.ConsistentVariable;
import com.ericsson.volteapp.core.util.Response;
import com.ericsson.volteapp.entity.KqiMpcQuery;
import com.ericsson.volteapp.service.KqiMpcService;

@Controller
@RequestMapping("/mpc")
public class VolteKqiMpcController {
	@Resource
	private KqiMpcService kqiMpcService;

	@RequestMapping("/getFaultDomain.action")
	@ResponseBody
	public Response getFaultDomain(KqiMpcQuery kqiMpcQuery){
		Response response = new Response();
		response.setStatus(ConsistentVariable.FAILED_STATUS);
		response.setResponseObject(ConsistentVariable.LIST_RESULT);

		//1.校验参数
		//2.调用service查询结果集
		List<Map> result = kqiMpcService.getFaultDomain(kqiMpcQuery);

		if(result==null||result.isEmpty()){
			response.setMessage("no result.");
			return response;
		}
		response.setStatus(ConsistentVariable.SUCCESS_STATUS);
		response.setResponseObject(result);
		return response;
	}

}
