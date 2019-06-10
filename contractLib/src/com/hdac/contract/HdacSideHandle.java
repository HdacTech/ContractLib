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
 * abstract HdacSideHandle class extends HdacContractHandle
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
public class HdacSideHandle extends HdacContractHandle
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> mainChain = null;
	private Map<String, Object> sideChain = null;
	private HdacWallet wallet = null;
	private long checkBlockHeight = 6;
	private BigInteger fee = null;

	@Override
	public void run()
	{
		init();

		SqlSession sqlSession = getSqlSession();

		try
		{
			ContractService service = ContractService.getInstance();
			List<Map<String, Object>> list = service.getTxList(this.sideChain, sqlSession);
			handle(list, this.sideChain);
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
			this.checkBlockHeight = service.getCheckSideBlockHeight(sqlSession);

			String strFee = service.getTransactionFee(sqlSession);
			this.fee = new BigDecimal(strFee).multiply(BigDecimal.TEN.pow(8)).toBigInteger();
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
			Method method = this.getClass().getMethod(split[0], HdacWallet.class, Map.class, JSONObject.class, Map.class, Map.class, Map.class, String.class, String[].class, long.class);
			method.invoke(this, wallet, map, resultObj, this.tokenInfo, this.sideChain, this.mainChain, split, this.checkBlockHeight);
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

		RpcService service = RpcService.getInstance();
		return service.getUtxos(paramMap, config);
	}

	@Override
	protected Map<String, Object> getSenderInfo(JSONObject obj, String tokenName)
	{
		JSONArray assetArr = obj.getJSONArray("assets");
		int assetArrLength = assetArr.length();
		for (int i = 0; i < assetArrLength; i++)
		{
			JSONObject assetObj = assetArr.getJSONObject(i);
			if (tokenName.equals(assetObj.get("name")))
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("addr",		obj.getJSONObject("scriptPubKey").getJSONArray("addresses").get(0));
				map.put("value",	assetObj.get("qty"));

				return map;
			}
		}
		return null;
	}

	@Override
	protected boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, List<Map<String, Object>> senderList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos, String dataValue)
	{
		String tokenName		= StringUtil.nvl(tokenInfo.get("tokenName"));
		long swapRatio			= Long.parseLong(StringUtil.nvl(tokenInfo.get("tokenSwapRatio"), "0"));
		String remainAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));

		BigInteger sendAmount	= BigInteger.ZERO;	// asset
		BigInteger totalBalance = BigInteger.ZERO;	// coin
		BigInteger sendCoin		= BigInteger.ZERO;	// coin

	   	for (JSONObject utxo : utxos)
	   	{
			totalBalance = totalBalance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
			transaction.addInput(utxo);
		}

	   	System.out.println("totalBalance : " + totalBalance);

		if (totalBalance.compareTo(BigInteger.ZERO) <= 0)
			return false;

	   	JSONArray voutArr = resultObj.getJSONArray("vout");
	   	int voutArrLength = voutArr.length();
		if (voutArrLength <= 0)
			return false;

		for (int i = 0; i < voutArrLength; i++)
		{
			JSONObject voutObj = voutArr.getJSONObject(i);
			JSONObject scriptPubKey = voutObj.getJSONObject("scriptPubKey");
			if (scriptPubKey.has("addresses") == false)
				continue;

			if (tokenInfo.get("contractAddress").equals(scriptPubKey.getJSONArray("addresses").get(0)))
			{
				JSONArray assetArr = voutObj.getJSONArray("assets");
				int assetArrLength = assetArr.length();
				for (int j = 0; j < assetArrLength; j++)
				{
					JSONObject assetObj = assetArr.getJSONObject(j);
					if (tokenName.equals(assetObj.get("name")))
						sendAmount = sendAmount.add(assetObj.getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
				}
			}
		}
		
		if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
			return false;

		if (sendAmount.compareTo(new BigDecimal(dataValue).multiply(BigDecimal.TEN.pow(8)).toBigInteger()) != 0)
			return false;

		sendCoin = getCoinValue(sendAmount, swapRatio);

		BigInteger remain = totalBalance.subtract(sendCoin);

		System.out.println("sendCoin : " + sendCoin);
		System.out.println("remain : " + remain);
			
		if (remain.compareTo(BigInteger.ZERO) < 0)
			return false;

		for (Map<String, Object> map : senderList)
		{
			String senderAddress	= StringUtil.nvl(map.get("addr"));
			BigInteger assetValue	= new BigDecimal(StringUtil.nvl(map.get("value"), "0")).multiply(BigDecimal.TEN.pow(8)).toBigInteger();
			BigInteger value		= BigInteger.ZERO;

			if (assetValue.compareTo(sendAmount) >= 0)
			{
				value = getCoinValue(sendAmount, swapRatio);
				sendAmount = BigInteger.ZERO;
			}
			else
			{
				value = getCoinValue(assetValue, swapRatio);
				sendAmount = sendAmount.subtract(assetValue);
			}

			if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
			{
				value = value.subtract(this.fee);

				transaction.addOutput(senderAddress, value.longValue());
				break;
			}
			else
			{
				transaction.addOutput(senderAddress, value.longValue());
			}
		}

		transaction.addOutput(remainAddress, remain.longValue());
		return true;
	}

	private BigInteger getCoinValue(BigInteger value, long swapRatio)
	{
		return value.divide(BigInteger.valueOf(swapRatio));
	}
}