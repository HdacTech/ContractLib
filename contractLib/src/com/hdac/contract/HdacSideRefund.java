package com.hdac.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hdac.comm.StringUtil;
import com.hdac.service.CommonService;
import com.hdac.service.ContractService;
import com.hdac.service.RpcService;
import com.hdacSdk.hdacWallet.HdacTransaction;
import com.hdacSdk.hdacWallet.HdacWallet;

public class HdacSideRefund extends HdacContractRefund
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> sideChain = null;
	private HdacWallet wallet = null;

	@Override
	public void run()
	{
		init();

		SqlSession sqlSession = getSqlSession();

		try
		{
			ContractService service = ContractService.getInstance();
			List<Map<String, Object>> list = service.getTxList(this.sideChain, sqlSession);
			refund(list, this.sideChain);
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
			this.sideChain = service.getSideChainInfo(sqlSession);
			this.wallet = service.getHdacWallet(sqlSession);
		}
		finally
		{
			sqlSession.close();
		}
	}

	@Override
	protected void refund(Map<String, Object> map, JSONObject resultObj, String txid, Map<String, Object> config)
	{
		refund(this.wallet, map, resultObj, this.tokenInfo, this.sideChain, txid);
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
	protected void addSenderInfo(JSONObject obj, String tokenName, Map<String, Map<String, Object>> recvMap)
	{
		JSONArray assetArr = obj.getJSONArray("assets");
		int assetArrLength = assetArr.length();
		for (int i = 0; i < assetArrLength; i++)
		{
			JSONObject assetObj = assetArr.getJSONObject(i);
//			if (tokenName.equals(assetObj.get("name")))
			{
				String recvAddr = obj.getJSONObject("scriptPubKey").getJSONArray("addresses").getString(0);

				if (recvMap.containsKey(recvAddr))
				{
					Map<String, Object> map = recvMap.get(recvAddr);
					BigDecimal qty = new BigDecimal(StringUtil.nvl(map.get("value")));

					map.put("value", qty.add(assetObj.getBigDecimal("qty")));
				}
				else
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("addr",		recvAddr);
					map.put("value",	assetObj.get("qty"));
					map.put("name",		assetObj.get("name"));

					recvMap.put(recvAddr, map);
				}
			}
		}
	}

	@Override
	protected boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, Map<String, Map<String, Object>> recvList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos)
	{
		String contractAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));		
		String tokenName		= StringUtil.nvl(tokenInfo.get("tokenName"));
		String txid				= StringUtil.nvl(tokenInfo.get("tokenTxid"));

		BigInteger balance		= BigInteger.ZERO;
		BigInteger assetBalance	= BigInteger.ZERO;
		BigInteger sendAmount	= BigInteger.ZERO;

//		Map<String, BigInteger> sendAmount	= new HashMap<String, BigInteger>();	// asset

		for (JSONObject utxo : utxos)
    	{
			assetBalance = assetBalance.add(utxo.getJSONArray("assets").getJSONObject(0).getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
			balance = balance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());

			transaction.addInput(utxo);
		}
//		for (JSONObject utxo : utxos)
//    	{
//			balance = balance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
//			
//			JSONArray assetArr = utxo.getJSONArray("assets");
//			int assetArrLength = assetArr.length();
//			for (int i = 0; i < assetArrLength; i++)
//			{
//				JSONObject assetObj = assetArr.getJSONObject(i);
//				String name = assetObj.getString("name");
//				BigInteger qty = assetObj.getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger();
//
//				if (sendAmount.containsKey(name))
//					qty = qty.add(sendAmount.get(name));
//
//				sendAmount.put(name, qty);
//			}
//
//			transaction.addInput(utxo);
//		}

    	System.out.println(utxos);
    	System.out.println("assetBalance : " + assetBalance);
    	System.out.println("balance : " + balance);

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

			if (contractAddress.equals(scriptPubKey.getJSONArray("addresses").get(0)))
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
		
    	System.out.println("sendAmount : " + sendAmount);
		//for checking balance
		BigInteger assetRemain = assetBalance.subtract(sendAmount);
    	System.out.println("assetRemain : " + assetRemain);

		if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
			return false;
		
		if (assetRemain.compareTo(BigInteger.ZERO) < 0)
			return false;
		
		Set<String> keySet = recvList.keySet();
		for (String recvAddr : keySet)
		{
			Map<String, Object> recvMap = recvList.get(recvAddr);
			String name = StringUtil.nvl(recvMap.get("name"));
			BigInteger assetValue = new BigDecimal(StringUtil.nvl(recvMap.get("value"), "0")).multiply(BigDecimal.TEN.pow(8)).toBigInteger();

//			BigInteger sendSome = sendAmount.get(name);
//			System.out.println(sendSome + "!!!!" + assetValue);

			if (sendAmount.compareTo(assetValue) > 0)
			{
				transaction.addAssetOutput(recvAddr, txid, assetValue.longValue(), 0);
				sendAmount = sendAmount.subtract(assetValue);
			}
			else
			{
				transaction.addAssetOutput(recvAddr, txid, sendAmount.longValue(), 0);
				break;
			}
		}
		
		if (assetRemain.compareTo(BigInteger.ZERO) > 0)
			transaction.addAssetOutput(contractAddress, txid, assetRemain.longValue(), 0);
		
		return true;
	}
	
//	@Override
//	protected boolean addUnsignedData2(HdacTransaction transaction, Map<String, Object> txMap
//		, List<Map<String, Object>> senderList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos, String dataValue)
//	{
//		String remainAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));
//		String txid				= StringUtil.nvl(tokenInfo.get("tokenTxid"));
//
//		BigInteger balance		= BigInteger.ZERO;
//		BigInteger assetBalance	= BigInteger.ZERO;
//		BigInteger sendAmount	= BigInteger.ZERO;
//
//		for (JSONObject utxo : utxos)
//    	{
//			assetBalance = assetBalance.add(utxo.getJSONArray("assets").getJSONObject(0).getBigDecimal("qty").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
//			balance = balance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
//
//			transaction.addInput(utxo);
//		}
//
//    	System.out.println("assetBalance : " + assetBalance);
//    	System.out.println("balance : " + balance);
//		//for checking balance
//		BigInteger assetRemain = assetBalance;
//		//BigInteger assetRemain = assetBalance.subtract(assetValue);
//
//		JSONArray voutArr = resultObj.getJSONArray("vout");
//		if (voutArr.length() <= 0)
//			return false;
//
//		sendAmount = voutArr.getJSONObject(0).getBigDecimal("value").multiply(BigDecimal.TEN.pow(8)).toBigInteger();
//		System.out.println("sendAmount : " + sendAmount);
//
//		if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
//			return false;
//		if (sendAmount.compareTo(new BigDecimal(dataValue).multiply(BigDecimal.TEN.pow(8)).toBigInteger()) != 0)
//			return false;
//		if (assetRemain.compareTo(BigInteger.ZERO) <= 0)
//			return false;
//
//		for (Map<String, Object> map : senderList)
//		{
//			System.out.println(map);
//			String sendAddress		= StringUtil.nvl(map.get("addr"));
//			BigInteger value		= new BigDecimal(StringUtil.nvl(map.get("value"), "0")).multiply(BigDecimal.TEN.pow(8)).toBigInteger();
//			BigInteger assetValue	= BigInteger.ZERO;
//
//			System.out.println("value : " + value);
//			if (value.compareTo(sendAmount) > 0)
//			{
//				assetValue = getAssetValue(sendAmount, tokenInfo);
//				sendAmount = BigInteger.ZERO;
//			}
//			else
//			{
//				assetValue = getAssetValue(value, tokenInfo);
//				sendAmount = sendAmount.subtract(assetValue);
//			}
//
//			if (assetRemain.compareTo(assetValue) >= 0)
//				assetRemain = assetRemain.subtract(assetValue);
//			else
//			{
//				assetValue = assetRemain;
//				assetRemain = BigInteger.ZERO;
//			}
//
//			transaction.addAssetOutput(sendAddress, txid, assetValue.longValue(), 0);
//
//			if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
//				break;
//			if (assetRemain.compareTo(BigInteger.ZERO) <= 0)
//				break;
//		}
//
//		if (assetRemain.compareTo(BigInteger.ZERO) > 0)
//			transaction.addAssetOutput(remainAddress, txid, assetRemain.longValue(), balance.longValue());
//
//		return true;
//	}
//
//	private BigInteger getAssetValue(BigInteger value, Map<String, Object> tokenInfo)
//	{
//		long swapRatio = Long.parseLong(StringUtil.nvl(tokenInfo.get("tokenSwapRatio"), "0"));
//		return value.multiply(BigInteger.valueOf(swapRatio));
//	}
}