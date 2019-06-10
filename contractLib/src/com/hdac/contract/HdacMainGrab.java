package com.hdac.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONObject;

import com.hdac.service.CommonService;

/**
 * abstract HdacMainGrab class extends HdacContractGrab
 * 
 * @see     java.math.BigDecimal
 * @see     java.math.BigInteger
 * @see     java.util.ArrayList
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class HdacMainGrab extends HdacContractGrab
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> mainChain = null;
	private long loopCount = 1;

	/**
	 * run init method and insert contract txs into database
	 * 
	 */		
	@Override
	public void run()
	{
		init();

		List<Map<String, Object>> txList = new ArrayList<Map<String, Object>>();

		long blockHeight = getTxList(txList, this.tokenInfo, this.mainChain, this.loopCount);
		insertTxList(txList, blockHeight, this.mainChain);
	}

	/**
	 * connect database and get chain,token informations from database
	 * 
	 */		
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