package com.hdac.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hdac.service.CommonService;

/**
 * abstract HdacSideGrab class extends HdacContractGrab
 * 
 * 
 * @see     java.math.BigDecimal
 * @see     java.math.BigInteger
 * @see     java.util.ArrayList
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.json.JSONArray
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class HdacSideGrab extends HdacContractGrab
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> sideChain = null;
	private long loopCount = 1;

	@Override
	public void run()
	{
		init();

		List<Map<String, Object>> txList = new ArrayList<Map<String, Object>>();

		long blockHeight = getTxList(txList, this.tokenInfo, this.sideChain, this.loopCount);
		insertTxList(txList, blockHeight, this.sideChain);
	}

	@Override
	protected void init()
	{
		SqlSession sqlSession = getSqlSession();

		try
		{
			CommonService service = CommonService.getInstance();
			this.tokenInfo = service.getTokenInfo(sqlSession);
			this.loopCount = service.getSideLoopCount(sqlSession);
			this.sideChain = service.getSideChainInfo(sqlSession);
		}
		finally
		{
			sqlSession.close();
		}
	}

	protected BigInteger getValueSum(JSONObject voutObj, String tokenName)
	{
		BigInteger valueSum = BigInteger.ZERO;
		JSONArray assetArr = voutObj.getJSONArray("assets");
		int assetArrLength = assetArr.length();
		for (int i = 0; i < assetArrLength; i++)
		{
			JSONObject assetObj = assetArr.getJSONObject(i);

			if (tokenName.equals(assetObj.get("name")) == false)
				continue;

			BigInteger value = assetObj.getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger();
			valueSum = valueSum.add(value);
		}
		return valueSum;
	}
}