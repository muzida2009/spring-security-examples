package com.ericsson.volteapp.controller;

import java.util.HashMap;
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
	/**
	 * 获取kqi的统计结果集 --- add 20170222 lida
	 * @param: kqiStatisticsQuery
	 * @return: response
	 */
	@ResponseBody
	@RequestMapping("/getKqiStatistics.action")
	public String getKqiStatistics(KqiStatisticsQuery kqiStatisticsQuery){

		validateParams(kqiStatisticsQuery);

		HashMap<String,Object> resultMap = new HashMap<String,Object>(0);

		List<Map> rows = kqiStatisticsService.getKqiStatisticsResults(kqiStatisticsQuery);

		if (rows == null || rows.size() == 0) {
			logger.info("no data from impala  ");
			resultMap.put("success",false);
			resultMap.put("rows",null);
			resultMap.put("total", 0);
		}else{
			resultMap.put("success",true);
			resultMap.put("rows",rows);
			resultMap.put("total", 15);
		}

		String resultJson = JsonUtil.convertToJson(resultMap);
		return resultJson;
	}
	/**
	 * 获取用户异常汇总（注册会话数、呼叫会话数、SRVCC切换会话）
	 * @param input
	 * @param choice
	 * @param date
	 * @param limit
	 * @param page
	 * @param start
	 * @return
	 */
	@RequestMapping(value = "/getRegAndCallCount", produces = "application/json;charset=utf-8", method = RequestMethod.GET)
	@ResponseBody
	public Response getRegAndCallCount(@RequestParam("input") String input, @RequestParam("choice") String choice,
			@RequestParam("date") String date, @RequestParam("sessionType") String sessionType) {
		Response response = new Response();
		input = input.trim();
		Map<String, String> dateMap = getFormatStartEndtime(date);
		if (dateMap == null) {
			response.setStatus(ConsistentVariable.FAILED_STATUS);
			response.setMessage(ErrorMsg.CH_ERR_MSG_WRONG_TIME_FORMAT);
			response.setResponseString("");
			return response;
		}
		Map<String, Object> result = voLTEService.getRegAndCallCount(input, choice, dateMap.get("from"),
				dateMap.get("to"), sessionType);
		if (result == null) {
			response.setStatus(ConsistentVariable.FAILED_STATUS);
			response.setMessage(ErrorMsg.CH_ERR_MSG_EXCEPTION_SUMMARY_NOT_FOUND);
			response.setResponseString("");
			return response;
		}
		response.setStatus(ConsistentVariable.SUCCESS_STATUS);
		response.setMessage("New data");
		response.setResponseObject(result);
		return response;
	}

}
