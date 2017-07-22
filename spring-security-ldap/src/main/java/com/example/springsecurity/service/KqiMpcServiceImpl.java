package com.ericsson.volteapp.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ericsson.volteapp.core.util.KQIMpcSQL;
import com.ericsson.volteapp.entity.KqiMpcQuery;
import com.ericsson.volteapp.service.KqiMpcService;
@Service
public class KqiMpcServiceImpl implements KqiMpcService {
	private Logger logger = Logger.getLogger(KqiMpcServiceImpl.class);
	@Resource
	private KQIMpcSQL kqiMpcSql;
	/**
	 * 根据faultDomain统计countFailure
	 */
	@Override
	public List<Map> getFaultDomain(KqiMpcQuery kqiMpcQuery) {
		//1.获取sql,并替换TABLE_CLAUSE1
		String sql = getSql(kqiMpcQuery);
		//2.添加where条件
		//3.调用impalaService,执行sql语句
		//4.处理查询结果

		return null;
	}

	private String getSql(KqiMpcQuery kqiMpcQuery) {
		if (kqiMpcQuery == null) {
			logger.error("kqiMpcQuery is null at getSql()");
			return null;
		}
//		KQI_MPC_QUERY_TYPE=faultDomain-faultInterface-faultNode-faultNode-faultTrend-faultRaster-faultSessionList-faultSessionCount
		String[] queryTypes = kqiMpcSql.getKQI_MPC_QUERY_TYPE().split("-");
		if (queryTypes == null || queryTypes.length == 0) {
			logger.error("failed to get queryTypes at getSql()");
		}
		String queryType = kqiMpcQuery.getQueryType();
		if (queryType == null || "".equals(queryType)) {
			logger.error("queryType is '' or null");
			return null;
		}
//		if (queryType.equals(queryTypes[0])) {	//查询faultDomain
//
//		}else if(queryType.equals(queryTypes[1]) {	//
//
//		}


		return null;
	}
	/**
	 * 查询失败接口
	 * @param kqiMpcQuery
	 * @return
	 */
	private List<Map> getFailedInterface(KqiMpcQuery kqiMpcQuery ){
		String sql = getSql(kqiMpcQuery);
		sql = addQueryFields(kqiMpcQuery);
		sql = addWhereClause(kqiMPcQuery);
		Response response = impalaQueryService.query(sql);
		List<Map> results = dealWithImpalaResults(response);
		if (results == null || result.size() == 0) {
			loggre.info("no data's found from impala");
		}
	}
	@Override
	public Map<String, Object> getSignaling(Parameters parameters) {
		Map<String,Object> rsMap = new HashMap<>();
		//GD CMCC doesn't offer ne params, so we can't get ne info from mysql, we should get ne list from [ne type + xdr's sourc/target (ne ip)]
		//get ne info from mysql
		/*Map<String, Object> neInfoMap = getNeInfo();
		if(neInfoMap.isEmpty()){
			return rsMap;
		}
		rsMap.put(SignalingMapping.NELIST, neInfoMap.get(SignalingMapping.FOR_DISPLAY));*/
		//get ne type for interface/procedure from csv.
		Map<String, String> neTypeInfo = getNeTypeInfo();
		//get interface info from csv.
		Map<String, List<InterfaceEntity>> interfaceInfo = getInterfaceInfo();
		if(interfaceInfo.isEmpty()){
			return rsMap;
		}
		//get signaling info from impala.
		List<InterfaceEntity> itfList = interfaceInfo.get(SignalingMapping.sessionTypes[Integer.valueOf(parameters.getType())-1]);
		rsMap = getSignalingInfo(parameters,itfList,neTypeInfo);
		return rsMap;
	}

}
