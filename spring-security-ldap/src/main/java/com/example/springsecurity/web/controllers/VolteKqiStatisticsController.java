package com.ericsson.volteapp.controller;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ericsson.volteapp.core.util.ConsistentVariable;
import com.ericsson.volteapp.core.util.ExcelConfiguration;
import com.ericsson.volteapp.core.util.ExporterUtil;
import com.ericsson.volteapp.core.util.JsonUtil;
import com.ericsson.volteapp.core.util.Response;
import com.ericsson.volteapp.entity.KqiStatisticsQuery;
import com.ericsson.volteapp.entity.KqiStatisticsVo;
import com.ericsson.volteapp.service.KqiStatisticsService;

/**
 * kqi指标统计
 * @author Dali
 *
 */
@Controller
@RequestMapping("/statistics")
public class VolteKqiStatisticsController {
	private Logger logger = Logger.getLogger(VolteKqiStatisticsController.class.getName());
	@Resource
	private KqiStatisticsService kqiStatisticsService;
	@Resource
	private ExcelConfiguration excelConfiguration;
	/**
	 * 获取kqi的统计结果集
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
	 * 查询条件非空校验
	 * @param response
	 * @param datas
	 * @param city
	 * @param district
	 * @param category
	 * @param scenes
	 * @param kqiNamem
	 * @param page
	 * @param limit
	 * @return
	 */
	private void validateParams(KqiStatisticsQuery kqiStatisticsQuery){

		if(kqiStatisticsQuery.getDates() == null || kqiStatisticsQuery.getDates().length != 2){
			logger.error("date's length is not 2 ");
		}
		if (kqiStatisticsQuery.getCity() == null || "".equals(kqiStatisticsQuery.getCity())) {
			logger.error("error: city is null or '' ");
		}
		if (kqiStatisticsQuery.getDistrict() == null || "".equals(kqiStatisticsQuery.getDistrict())) {
			logger.error("error: district is null or '' ");
		}
		if (kqiStatisticsQuery.getCategory() == null || kqiStatisticsQuery.getCategory().length <= 0) {
			logger.error("error: category is null or category'length = 0 ");
		}
		if (kqiStatisticsQuery.getScenes() == null || kqiStatisticsQuery.getScenes().length <= 0) {
			logger.error("error: scenes is null or scenes'length = 0 ");
		}
		if (kqiStatisticsQuery.getKqiName() == null || "".equals(kqiStatisticsQuery.getKqiName())) {
			logger.error("error: kqiName is null or '' ");
		}
		if (kqiStatisticsQuery.getPage() == null) {
			logger.error("error: page is null");
		}
		if (kqiStatisticsQuery.getLimit() == null) {
			logger.error("error: limit is null");
		}
	}
	/**
	 *	导出kqi指标统计结果集到excel表中
	 * @param request
	 * @param response
	 * @param kqiStatisticsQuery
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/exportToExcel.action")
	@ResponseBody
	public void exportToExcel(HttpServletRequest request,
			HttpServletResponse response, KqiStatisticsQuery kqiStatisticsQuery)
			throws IOException {

		List<KqiStatisticsVo> resultsList = null;

		Boolean isExported = false;
		String filePath = "";
		String date = "";
		if (resultsList != null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			date = format.format(new Date());
			File dirname = new File(excelConfiguration.getExcel_path());
			if (!dirname.isDirectory()) {
				dirname.mkdir();
			}
			filePath = excelConfiguration.getExcel_path()
					+ excelConfiguration.getExcel_name() + "_" + date + ".xlsx";
			isExported = kqiStatisticsService.exportKqiStatisticsResults(resultsList, filePath, kqiStatisticsQuery);
		}
		if (isExported) {
			Cookie cookie_2 = new Cookie("isExported", "true");
			response.addCookie(cookie_2);
			boolean isDownload = ExporterUtil.re_download(request, response,filePath, "multipart/form-data",
														excelConfiguration.getExcel_name() + "_" + date + ".xlsx");
//			 if(!isDownload){ request.setAttribute("msg", "网络异常，请稍后重试");
//			 request.getRequestDispatcher("views/error.jsp"); }
			try {
				ExporterUtil.download(request, response, filePath,
						"multipart/form-data",
						excelConfiguration.getExcel_name() + "_" + date
								+ ".xlsx");
				// ExporterUtil.deleteFile(filePath);
			} catch (Exception e) {
				logger.info(e.getMessage());
				// request.setAttribute("msg", "网络异常，请稍后重试");
				// request.getRequestDispatcher("views/error.jsp");
			}
		} else {
			logger.info("export failed!");
		}
	}
}
