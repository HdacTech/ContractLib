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

/**
 * abstract HdacMainRefund class extends HdacContractRefund
 * 
 * 
 * @see     java.math.BigDecimal
 * @see     java.math.BigInteger
 * @see     java.util.HashMap
 * @see     java.util.List
 * @see     java.util.Map
 * @see     java.util.Set
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.json.JSONArray
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class HdacMainRefund extends HdacContractRefund
{
	private Map<String, Object> tokenInfo = null;
	private Map<String, Object> mainChain = null;
	private HdacWallet wallet = null;
	private BigInteger fee = null;

	@Override
	public void run()
	{
		init();

		SqlSession sqlSession = getSqlSession();

		try
		{
			ContractService service = ContractService.getInstance();
			List<Map<String, Object>> list = service.getTxList(this.mainChain, sqlSession);
			refund(list, this.mainChain);
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
			this.wallet = service.getHdacWallet(sqlSession);

			String strFee = service.getTransactionFee(sqlSession);
			this.fee = new BigDecimal(strFee).multiply(BigDecimal.TEN.pow(8)).toBigInteger();
		}
		finally
		{
			sqlSession.close();
		}
	}

	@Override
	protected void refund(Map<String, Object> map, JSONObject resultObj, String txid)
	{
		refund(this.wallet, map, resultObj, this.tokenInfo, this.mainChain);
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
	protected void addSenderInfo(JSONObject obj, String tokenName, Map<String, Map<String, Object>> recvMap)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("addr",		obj.getJSONObject("scriptPubKey").getJSONArray("addresses").get(0));
		map.put("value",	obj.get("value"));

		String key = String.valueOf(recvMap.size());
		recvMap.put(key, map);
	}

	@Override
	protected boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, Map<String, Map<String, Object>> recvList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos)
	{
		String contractAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));
		
		BigInteger totalBalance = BigInteger.ZERO;		
		BigInteger sendAmount	= BigInteger.ZERO;
		
		for (JSONObject utxo : utxos)
    	{
			totalBalance = totalBalance.add(utxo.getBigDecimal("amount").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
			transaction.addInput(utxo);
		}

    	System.out.println(utxos);
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

			if (contractAddress.equals(scriptPubKey.getJSONArray("addresses").get(0)))
				sendAmount = sendAmount.add(voutObj.getBigDecimal("value").multiply(BigDecimal.TEN.pow(8)).toBigInteger());
		}
		
		if (sendAmount.compareTo(BigInteger.ZERO) <= 0)
		return false;

		BigInteger remain = totalBalance.subtract(sendAmount);
		
		sendAmount = sendAmount.subtract(this.fee);

		if (remain.compareTo(BigInteger.ZERO) < 0)
			return false;

		Set<String> keySet = recvList.keySet();
		for (String index : keySet)
		{
			Map<String, Object> recvMap = recvList.get(index);

			String recvAddr		= StringUtil.nvl(recvMap.get("addr"));
			BigInteger value	= new BigDecimal(StringUtil.nvl(recvMap.get("value"), "0")).multiply(BigDecimal.TEN.pow(8)).toBigInteger();

			if (sendAmount.compareTo(value) > 0)
			{
				transaction.addOutput(recvAddr, value.longValue());
				sendAmount = sendAmount.subtract(value);
			}
			else
			{
				transaction.addOutput(recvAddr, sendAmount.longValue());
				break;
			}
		}
		
		transaction.addOutput(contractAddress, remain.longValue());		
		return true;
	}
}