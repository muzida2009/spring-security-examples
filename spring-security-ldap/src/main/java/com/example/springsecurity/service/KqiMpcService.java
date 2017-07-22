package com.ericsson.volteapp.service;

import java.util.List;
import java.util.Map;

import com.ericsson.volteapp.entity.KqiMpcQuery;

public interface KqiMpcService {

	List<Map> getFaultDomain(KqiMpcQuery kqiMpcQuery);

}
