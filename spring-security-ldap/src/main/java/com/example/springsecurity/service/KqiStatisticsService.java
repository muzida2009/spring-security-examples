package com.ericsson.volteapp.service;

import java.util.List;
import java.util.Map;

import com.ericsson.volteapp.entity.KqiStatisticsQuery;
import com.ericsson.volteapp.entity.KqiStatisticsVo;

/**
 * kqi指标统计的接口
 * @author Dali
 *
 */
public interface KqiStatisticsService {
	/**
	 * 查询kqi指标统计结果集
	 * @return
	 */
	List<Map> getKqiStatisticsResults(KqiStatisticsQuery kqiStatisticsQuery);
	/**
	 *	将kqi指标统计结果集导出到excel
	 * @param list
	 * @param filePath
	 * @return
	 */
	boolean exportKqiStatisticsResults(List<KqiStatisticsVo> list,String filePath,KqiStatisticsQuery kqiStatisticsQuery);
}
