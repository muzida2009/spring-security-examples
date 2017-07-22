package com.ericsson.volteapp.service;

import java.util.List;
import java.util.Map;

import com.ericsson.volteapp.entity.KqiMpcQuery;

public interface KqiMpcService {

	List<Map> getFaultDomain(KqiMpcQuery kqiMpcQuery);
	/**
	 * 查询第一失败接口
	 * @param kqiMPcQuery
	 * @return
	 */
	List<KqiQueryVo> getFailedInterface(KqiMpcQuery kqiMPcQuery);

	public Map<String,Object> getSessionList(Map<String,String> paramMap) ;
}
