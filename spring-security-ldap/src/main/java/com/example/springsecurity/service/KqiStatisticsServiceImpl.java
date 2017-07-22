package com.ericsson.volteapp.service.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.ericsson.volteapp.core.util.ConsistentVariable;
import com.ericsson.volteapp.core.util.ExcelConfiguration;
import com.ericsson.volteapp.core.util.ExporterUtil;
import com.ericsson.volteapp.core.util.KQIStatisticsSQL;
import com.ericsson.volteapp.core.util.Response;
import com.ericsson.volteapp.entity.KqiStatisticsQuery;
import com.ericsson.volteapp.entity.KqiStatisticsVo;
import com.ericsson.volteapp.service.ImpalaQueryService;
import com.ericsson.volteapp.service.KqiStatisticsService;
/**
 * kqi指标统计接口的实现类
 * @author Dali
 *
 */
@Service
public class KqiStatisticsServiceImpl implements KqiStatisticsService {
	private Logger logger = Logger.getLogger(KqiStatisticsServiceImpl.class);
	@Resource
	private ImpalaQueryService impalaQueryService;
	@Resource
	private KQIStatisticsSQL kqiStatisticsSql;
	@Resource
	private ExcelConfiguration excelConfiguration;
	/**
	 * 查询kqi指标统计
	 * 查询条件(参数)的非空检验均在controller中完成
	 */
	@Override
	public List<Map> getKqiStatisticsResults(KqiStatisticsQuery kqiStatisticsQuery) {
		if(kqiStatisticsQuery == null){
			return null;
		}
		String sql = getSql(kqiStatisticsQuery);

		sql = addQueryFields(sql,kqiStatisticsQuery);

		sql = addWhereClause(sql,kqiStatisticsQuery);

		Response impalaResult = impalaQueryService.ImpalaQuery(sql);

		List<Map> result = dealWithImpalaResult(impalaResult,kqiStatisticsQuery.getKqiName(),sql);

		return result;
	}
	/**
	 * 根据kqiName获取对应的sql语句
	 * @param kqiStatisticsQuery
	 * @return sql
	 */
	private String getSql(KqiStatisticsQuery kqiStatisticsQuery){
		String kqiName = kqiStatisticsQuery.getKqiName();
		String sql = null;
		if (kqiName != null && !"".equals(kqiName)) {
			if("call-1".equals(kqiName)){
				sql = kqiStatisticsSql.getVOLTE_VIDEO_CONNECT_RATE_SQL();
			}
			if ("call-2".equals(kqiName)) {
				sql = kqiStatisticsSql.getVOLTE_VIDEO_CONNECT_RATE_SQL();
			}
			if ("call-3".equals(kqiName)) {
				sql = kqiStatisticsSql.getVOLTE_MO_LATENCY_AVERAGE_SQL();
			}
			if ("call-4".equals(kqiName)) {
				sql = kqiStatisticsSql.getVOLTE_ANSWER_RATE_SQL();
			}
			if ("call-5".equals(kqiName)) {
				sql = kqiStatisticsSql.getVOLTE_VOICE_DROP_RATE_SQL();
			}
			if ("call-6".equals(kqiName)) {
				sql = kqiStatisticsSql.getVOLTE_VIDEO_DROP_RATE_SQL();
			}
			if ("register-7".equals(kqiName)) {
				sql = kqiStatisticsSql.getIMS_REGISTER_SUCCESS_RATE_SQL();
			}
			if ("attach-8".equals(kqiName)) {
				sql = kqiStatisticsSql.getATTACH_SUCCESS_RATE_SQL();
			}
			if ("srvcc-9".equals(kqiName)) {
				sql = kqiStatisticsSql.getSRVCC_SUCCESS_RATE_SQL();
			}
			if ("srvcc-10".equals(kqiName)) {
				sql = kqiStatisticsSql.getSRVCC_DELAY_AVERAGE_SQL();
			}
			if (sql == null) {
				logger.info("something is wrong with kqiName: " + kqiName);
			}
		}
		logger.info("getSql(): sql=" + sql);
		return sql;
	}
	/**
	 * 根据页面传递的参数category,判断所选分类 (cell,terminal,mme)三个字段中需要查询的字段,并替换sql语句中的CATEGORY_CLAUSE
	 * @param sql
	 * @param kqiStatisticsQuery
	 * @return
	 */
	private String addQueryFields(String sql,KqiStatisticsQuery kqiStatisticsQuery){
		String[] scenes = kqiStatisticsQuery.getScenes();
		String[] category = kqiStatisticsQuery.getCategory();
		StringBuffer CATEGORY_CLAUSE = new StringBuffer();
		if (scenes != null && scenes.length > 0) {
			CATEGORY_CLAUSE.append(" t2.scene, ");
		}
		if(category != null && category.length > 0){
			for (String c : category) {
				if ("cell".equals(c)) {			//查询小区字段
					CATEGORY_CLAUSE.append(" t2.cellname, ");
				}
				if ("terminal".equals(c)) {		//查询终端字段
					CATEGORY_CLAUSE.append(" t1.terminal_model, ");
				}
				if(!kqiStatisticsQuery.getKqiName().startsWith("register")){	//注册表没有mme_ip,不查询此字段
					if("mme".equals(c)){			//查询网元字段
						CATEGORY_CLAUSE.append(" t1.mme, ");
					}
				}
			}
		}
		logger.info("CATEGORY_CLAUSE: " + CATEGORY_CLAUSE);
		sql = sql.replaceAll("CATEGORY_CLAUSE",CATEGORY_CLAUSE.toString());
		logger.info("dealWithCategory() : sql=" + sql);
		return sql;
	}
	/**
	 * 添加where语句到sql语句中
	 * 1.date
	 * 2.city
	 * 3.district
	 * 4.scene
	 * 5.order by :根据当前查询的指标名称来决定排序的字段
	 * 6.limit
	 */
	private String addWhereClause(String sql,KqiStatisticsQuery kqiStatisticsQuery){
		StringBuffer WHERE_CLAUSE = new StringBuffer(" where 1=1 ");
		if(kqiStatisticsQuery.getDates() != null && kqiStatisticsQuery.getDates().length == 2){
			String[] dates = kqiStatisticsQuery.getDates();			//t1==e2e_call_kqi
			WHERE_CLAUSE.append(" and t1.hour_id >= '"+dates[0]+"' ").append(" and t1.hour_id <= '"+dates[1]+"' ");
		}
		if(kqiStatisticsQuery.getCity() != null && !"".equals(kqiStatisticsQuery.getCity())){	//t2==networkparam
			WHERE_CLAUSE.append(" and t2.city='" + kqiStatisticsQuery.getCity() + "'");
		}
		if(kqiStatisticsQuery.getDistrict() != null && !"".equals(kqiStatisticsQuery.getDistrict())){
			WHERE_CLAUSE.append(" and t2.district='" + kqiStatisticsQuery.getDistrict() + "'");
		}
		if(kqiStatisticsQuery.getScenes() != null && kqiStatisticsQuery.getScenes().length > 0){
			String[] array = kqiStatisticsQuery.getScenes();
			String SCENES = "";									//	String SCENES = " '乡村','城区','高铁' "
			if("all".equals(array[0])){
				for (int i = 1; i < array.length; i++) {
					if (i == array.length - 1) {
						SCENES += "'"+array[i]+"'" ;
					}else{
						SCENES += "'"+array[i]+"'" + "," ;
					}
				}
			}else{
				for (int i = 0; i < array.length; i++) {
					if (i == array.length - 1) {
						SCENES += "'"+array[i]+"'" ;
					}else{
						SCENES += "'"+array[i]+"'" + "," ;
					}
				}
			}
			WHERE_CLAUSE.append(" and t2.scene in ("+SCENES+") ");
		}
		//设置排序字段
		//1: VoLTE语音接通率 2:VoLTE视频接通率 3:VoLTE始呼接入时长 4:VoLTE应答率 5:VoLTE语音掉话率
		//6: VoLTE视频掉话率 7:IMS注册成功率  8:Attach成功率 9:SRVCC切换成功率  10:SRVCC切换时长
		if (kqiStatisticsQuery.getKqiName() != null && !"".equals(kqiStatisticsQuery.getKqiName())) {
			if ("call-1".equals(kqiStatisticsQuery.getKqiName())) {
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("call-2".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("call-3".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("call-4".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("call-5".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("call-6".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("register-7".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("attach-8".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("srvcc-9".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}else if("srvcc-10".equals(kqiStatisticsQuery.getKqiName())){
				WHERE_CLAUSE.append(" order by " + "' kqi '" + " desc ");
			}
		}
		if(kqiStatisticsQuery.getLimit() != null){
			WHERE_CLAUSE.append(" limit "+kqiStatisticsQuery.getStartIndex()+" offset "+kqiStatisticsQuery.getLimit());
		}
		sql = sql + WHERE_CLAUSE.toString();
		logger.info("WHERE_CLAUSE:" + WHERE_CLAUSE.toString());
		logger.info("addWhereClause(): sql=" + sql);
		return sql;
	}
	/**
	 * 处理kqiName
	 */
	private String MappingKqiName(String kqiName){
		if ("call-1".equals(kqiName)) {
			kqiName = "VoLTE语音接通率";
		}
		if ("call-2".equals(kqiName)) {
			kqiName = "VoLTE视频接通率";
		}
		if ("call-3".equals(kqiName)) {
			kqiName = "VoLTE始呼接入时长";
		}
		if ("call-4".equals(kqiName)) {
			kqiName = "VoLTE应答率";
		}
		if ("call-5".equals(kqiName)) {
			kqiName = "VoLTE语音掉话率";
		}
		if ("call-6".equals(kqiName)) {
			kqiName = "VoLTE视频掉话率";
		}
		if ("register-7".equals(kqiName)) {
			kqiName = "IMS注册成功率";
		}
		if ("attach-8".equals(kqiName)) {
			kqiName = "Attach成功率";
		}
		if ("srvcc-9".equals(kqiName)) {
			kqiName = "SRVCC切换成功率";
		}
		if ("srvcc-10".equals(kqiName)) {
			kqiName = "SRVCC切换时长";
		}
		return kqiName;
	}
	/**
	 * 处理impalaResult结果集
	 * @param response
	 * @return list<Map>
	 */
	private List<Map> dealWithImpalaResult(Response impalaResult,String kqiName,String sql){
		if (ConsistentVariable.FAILED_STATUS.equals(impalaResult.getStatus())) {
			logger.error("failed to get datas from impala when query for :" + kqiName + "by execute sql: " + sql);
			logger.error("due to :" + impalaResult.getMessage());
			return null;
		}
		String responseString = impalaResult.getResponseString();

		if(responseString == null || "".equals(responseString) || "[]".equals(responseString)){
			logger.info("no data's found from impala for " + kqiName);
			return null;
		}

		responseString = responseString.replace("NA", "");
		responseString = responseString.replace("NULL", "");
		responseString = responseString.replace("null", "");

		try {
			List<Map> list= JSON.parseArray(responseString,Map.class);
//			List<KqiStatisticsVo> list= JSON.parseArray(responseString,KqiStatisticsVo.class);

			if (list == null || list.size() == 0) {
				logger.error("No " + kqiName + " data from impala query after json parsing.");
				return null;
			}
			return list;
		} catch (Exception e) {
			logger.error("parse jstring come from response to list<Map> failed ; jstring" + responseString);
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 将kqi指标统计结果集导出到excel
	 * @param list
	 * @param filePath
	 * @return
	 */
	@Override
	public boolean exportKqiStatisticsResults(List<KqiStatisticsVo> list,String filePath,KqiStatisticsQuery kqiStatisticsQuery) {
		boolean ifSuccess = true;
		SXSSFWorkbook workBook = new SXSSFWorkbook(); // 2010增强版
		long total = list.size();		// 查询结果集记录数
		long avg = 1048575; 			// Office2010的EXCEL一个工作表最多有1048575条记录
		double forInt = (float) total / avg;
		int sheetNum = (int) Math.ceil(forInt);
		int p = 0;
		if (sheetNum == 0) {
			Sheet sheet = workBook.createSheet("kqi_statistics_1");
			ExporterUtil exportUtil = new ExporterUtil(workBook, sheet);
			CellStyle headStyle = exportUtil.getHeadStyle();
		} else {
			for (int num = 1; num <= sheetNum; num++) {
				Sheet sheet = workBook.createSheet("kqi_statistics_" + num);
				ExporterUtil exportUtil = new ExporterUtil(workBook, sheet);
				CellStyle headStyle = exportUtil.getHeadStyle();
				CellStyle bodyStyle = exportUtil.getBodyStyle();
				SXSSFRow headRow = (SXSSFRow) sheet.createRow(0);
				SXSSFCell cell = null;
				List<String> titles = new ArrayList<String>(0);		//table title
				String kqiName = kqiStatisticsQuery.getKqiName();
				kqiName = MappingKqiName(kqiName);
				String[] scenes = kqiStatisticsQuery.getScenes();
				String[] categorys = kqiStatisticsQuery.getCategory();
				if (scenes != null || scenes.length > 0) {
					titles.add("场景");
				}
				for (int i = 0; i < categorys.length; i++) {
					if ("cell".equals(categorys[i])) {
						titles.add("小区");
					}
					if ("terminal".equals(categorys[i])) {
						titles.add("终端");
					}
					if (!"IMS注册成功率".equals(kqiName)) {	//IMS注册成功率没有mme字段
						if ("mme".equals(categorys[i])) {
							titles.add("网元");
						}
					}
				}
				titles.add(kqiName);
				for (int i = 0; i < titles.size(); i++) {
					cell = (SXSSFCell) headRow.createCell(i);
					cell.setCellStyle(headStyle);
					cell.setCellValue(titles.get(i));
				}
				int j = 0;
				if (j >= avg) {
					continue;
				} else {
					for (long po = p; po < num * avg; po++) {
						if (po < total) {
							SXSSFRow bodyRow = (SXSSFRow) sheet.createRow(j + 1);

							cell = (SXSSFCell) bodyRow.createCell(0);
							cell.setCellStyle(bodyStyle);
							cell.setCellValue(list.get((int) po).getScene());

							cell = (SXSSFCell) bodyRow.createCell(1);
							cell.setCellStyle(bodyStyle);
							cell.setCellValue(list.get((int) po).getCell());

							cell = (SXSSFCell) bodyRow.createCell(2);
							cell.setCellStyle(bodyStyle);
							cell.setCellValue(list.get((int) po).getTerminal());

							if("IMS注册成功率".equals(kqiName)){
								cell = (SXSSFCell) bodyRow.createCell(3);
								cell.setCellStyle(bodyStyle);
								cell.setCellValue(list.get((int) po).getKqi());
							}else{
								cell = (SXSSFCell) bodyRow.createCell(3);
								cell.setCellStyle(bodyStyle);
								cell.setCellValue(list.get((int) po).getMme());

								cell = (SXSSFCell) bodyRow.createCell(4);
								cell.setCellStyle(bodyStyle);
								cell.setCellValue(list.get((int) po).getKqi());
							}
							j++;
							p++;
						}

					}
				}
				if (num % 100 == 0) {
					try {
						((SXSSFSheet) sheet).flushRows();
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			}
		}
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(filePath);
			workBook.write(fileOut);
			fileOut.flush();
			fileOut.close();
		} catch (Exception e) {
			ifSuccess = false;
			logger.error(e.getMessage());
		} finally {
			try {
				if (fileOut != null)
					fileOut.close();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return ifSuccess;
	}

}
