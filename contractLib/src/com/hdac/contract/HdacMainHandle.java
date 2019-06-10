package com.hdac.contract;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hdac.comm.StringUtil;
import com.hdac.service.CommonService;
import com.hdac.service.ContractService;
import com.hdac.service.RpcService;
import com.hdacSdk.hdacWallet.HdacTransaction;
import com.hdacSdk.hdacWallet.HdacWallet;

/**
 * abstract HdacMainHandle class extends HdacContractHandle
 * 
 * 
 * @see     java.lang.reflect.Method
 * @see     java.math.BigDecimal
 * @see     java.math.BigInteger
 * @see     java.util.HashMap
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.json.JSONArray
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class HdacMainHandle extends HdacContractHandle
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> mainChain = null;
	private Map<String, Object> sideChain = null;
	private HdacWallet wallet = null;
	private long checkBlockHeight = 6;

	@Override
	public void run()
	{
		init();

		SqlSession sqlSession = getSqlSession();

		try
		{
			ContractService service = ContractService.getInstance();
			List<Map<String, Object>> list = service.getTxList(this.mainChain, sqlSession);
			handle(list, this.mainChain);
		}
		finally
		{
			sqlSession.close();
		}
	}

	@Override
	protected void init()
	{
		SqlSession sqlSession = getSqlSession();

		try
		{
			CommonService service = CommonService.getInstance();
			this.tokenInfo = service.getTokenInfo(sqlSession);
			this.mainChain = service.getMainChainInfo(sqlSession);
			this.sideChain = service.getSideChainInfo(sqlSession);
			this.wallet = service.getHdacWallet(sqlSession);
			this.checkBlockHeight = service.getCheckMainBlockHeight(sqlSession);
		}
		finally
		{
			sqlSession.close();
		}
	}

	@Override
	protected void handle(String[] split, Map<String, Object> map, JSONObject resultObj, Map<String, Object> config)
	{
		try
		{
//			Method method = this.getClass().getMethod(split[0], HdacWallet.class, Map.class, JSONObject.class, Map.class, Map.class, Map.class, String.class, String[].class, long.class);
//			method.invoke(this, wallet, map, resultObj, this.tokenInfo, this.mainChain, this.sideChain, txid, split, this.checkBlockHeight);
			Method method = this.getClass().getMethod(split[0], HdacWallet.class, Map.class, JSONObject.class, Map.class, Map.class, Map.class, String[].class, long.class);
			method.invoke(this, wallet, map, resultObj, this.tokenInfo, this.mainChain, this.sideChain, split, this.checkBlockHeight);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected List<JSONObject> addUtxos(Map<String, Object> tokenInfo, Map<String, Object> config)
	{
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("addresses",	tokenInfo.get("contractAddress"));
		paramMap.put("asset",		tokenInfo.get("tokenName"));

		RpcService service = RpcService.getInstance();
		return service.getAssetUtxos(paramMap, config);
	}

	@Override
	protected Map<String, Object> getSenderInfo(JSONObject obj, String tokenName)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("addr",		obj.getJSONObject("scriptPubKey").getJSONArray("addresses").get(0));
		map.put("value",	obj.get("value"));
		return map;
	}

	@Override
	protected boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, List<Map<String, Object>> senderList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos, String dataValue)
	{
		String remainAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));
		String txid				= StringUtil.nvl(tokenInfo.get("tokenTxid"));

		BigInteger balance		= BigInteger.ZERO;
		BigInteger assetBalance	= BigInteger.ZERO;
		BigInteger sendAmount	= BigInteger.ZERO;

		for (JSONObject utxo : utxos)
    	{
			assetBalance = assetBalance.add(utxo.getJSONArray("assets").getJSONObject(0).getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
			balance = balance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());

			transaction.addInput(utxo);
		}

    	System.out.println("assetBalance : " + assetBalance);
    	System.out.println("balance : " + balance);
		//for checking balance
		BigInteger assetRemain = assetBalance;
		//BigInteger assetRemain = assetBalance.subtract(assetValue);

		JSONArray voutArr = resultObj.getJSONArray("vout");
		if (voutArr.length() <= 0)
			return false;

		sendAmount = voutArr.getJSONObject(0).getBigDecimal("value").multiply(BigDecimal.TEN.pow(8)).toBigInteger();
		System.out.println("sendAmount : " + sendAmount);

		if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
			return false;
		if (sendAmount.compareTo(new BigDecimal(dataValue).multiply(BigDecimal.TEN.pow(8)).toBigInteger()) != 0)
			return false;
		if (assetRemain.compareTo(BigInteger.ZERO) <= 0)
			return false;

		for (Map<String, Object> map : senderList)
		{
			System.out.println(map);
			String sendAddress		= StringUtil.nvl(map.get("addr"));
			BigInteger value		= new BigDecimal(StringUtil.nvl(map.get("value"), "0")).multiply(BigDecimal.TEN.pow(8)).toBigInteger();
			BigInteger assetValue	= BigInteger.ZERO;

			System.out.println("value : " + value);
			if (value.compareTo(sendAmount) > 0)
			{
				assetValue = getAssetValue(sendAmount, tokenInfo);
				sendAmount = BigInteger.ZERO;
			}
			else
			{
				assetValue = getAssetValue(value, tokenInfo);
				sendAmount = sendAmount.subtract(assetValue);
			}

			if (assetRemain.compareTo(assetValue) >= 0)
				assetRemain = assetRemain.subtract(assetValue);
			else
			{
				assetValue = assetRemain;
				assetRemain = BigInteger.ZERO;
			}

			transaction.addAssetOutput(sendAddress, txid, assetValue.longValue(), 0);

			if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
				break;
			if (assetRemain.compareTo(BigInteger.ZERO) <= 0)
				break;
		}

		if (assetRemain.compareTo(BigInteger.ZERO) > 0)
			transaction.addAssetOutput(remainAddress, txid, assetRemain.longValue(), balance.longValue());

		return true;
	}

	private BigInteger getAssetValue(BigInteger value, Map<String, Object> tokenInfo)
	{
		long swapRatio = Long.parseLong(StringUtil.nvl(tokenInfo.get("tokenSwapRatio"), "0"));
		return value.multiply(BigInteger.valueOf(swapRatio));
	}
}