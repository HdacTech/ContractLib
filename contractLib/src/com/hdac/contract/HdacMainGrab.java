package com.hdac.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONObject;

import com.hdac.service.CommonService;

public class HdacMainGrab extends HdacContractGrab
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> mainChain = null;
	private long loopCount = 1;

	@Override
	public void run()
	{
		init();

		List<Map<String, Object>> txList = new ArrayList<Map<String, Object>>();

		long blockHeight = getTxList(txList, this.tokenInfo, this.mainChain, this.loopCount);
		insertTxList(txList, blockHeight, this.mainChain);
	}

	@Override
	protected void init()
	{
		SqlSession sqlSession = getSqlSession();

		try
		{
			CommonService service = CommonService.getInstance();
			this.tokenInfo = service.getTokenInfo(sqlSession);
			this.loopCount = service.getMainLoopCount(sqlSession);
			this.mainChain = service.getMainChainInfo(sqlSession);
		}
		finally
		{
			sqlSession.close();
		}
	}

	protected BigInteger getValueSum(JSONObject voutObj, String tokenName)
	{
		return voutObj.getBigDecimal("value").multiply(BigDecimal.TEN.pow(8)).toBigInteger();
	}
}