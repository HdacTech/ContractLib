package com.hdac.contract;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hdac.comm.CipherUtil;
import com.hdac.comm.HdacUtil;
import com.hdac.comm.StringUtil;
import com.hdac.service.ContractService;
import com.hdac.service.RpcService;
import com.hdacSdk.hdacWallet.HdacWallet;

abstract class HdacContractGrab extends HdacContract
{
	protected abstract void init();
	protected abstract BigInteger getValueSum(JSONObject voutObj, String tokenName);

	private long getBlockHeight(Map<String, Object> chainInfo)
	{
		SqlSession sqlSession = getSqlSession();

		try
		{
			ContractService service = ContractService.getInstance();
			return service.getBlockHeight(chainInfo, sqlSession);
		}
		finally
		{
			sqlSession.close();
		}
	}

	protected long getTxList(List<Map<String, Object>> txList, Map<String, Object> tokenInfo, Map<String, Object> chainInfo, long loopCount)
	{
		long blockHeight = getBlockHeight(chainInfo);

		try
		{
			HdacWallet wallet = HdacUtil.generateNewWallet();

			String contractAddress	= StringUtil.nvl(tokenInfo.get("contractAddress"));
			String tokenName		= StringUtil.nvl(tokenInfo.get("tokenName"));

			RpcService service = RpcService.getInstance();
			for (long i = 0; i < loopCount; i++)
			{
				blockHeight++;

				JSONObject obj = service.getblock(blockHeight, chainInfo);
				filterTxList(txList, wallet, obj, contractAddress, tokenName);
			}
			System.out.println(txList.size());
		}
		catch (JSONException e)
		{
			blockHeight--;
			e.printStackTrace();
		}
		return blockHeight;
	}

	private void filterTxList(List<Map<String, Object>> txList, HdacWallet wallet, JSONObject obj, String contractAddress, String tokenName)
	{
		JSONArray txArr = obj.getJSONArray("tx");
		txArr.forEach(item ->
		{
			if (item instanceof JSONObject)
			{
				JSONObject txObj = (JSONObject)item;
				if (isExistAddress(wallet, txObj, contractAddress, tokenName))
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("txid",				txObj.get("txid"));
					map.put("block_height",		obj.get("height"));

					txList.add(map);
				}
			}
		});
	}

	private boolean isExistAddress(HdacWallet wallet, JSONObject txObj, String contractAddress, String tokenName)
	{
		JSONArray vinArr = txObj.getJSONArray("vin");
		int vinArrLength = vinArr.length();
		for (int i = 0; i < vinArrLength; i++)
		{
			JSONObject vinObj = vinArr.getJSONObject(i);
			if (vinObj.has("scriptSig") == false)
				return false;

			String[] asm = vinObj.getJSONObject("scriptSig").getString("asm").split(" ");

			String publickey = asm[asm.length -1];
			String vinaddress = wallet.convPubkeyToHdacAddress(CipherUtil.toByteArray(publickey));
			if (contractAddress.equals(vinaddress))
				return false;
		}

		BigInteger valueSum = BigInteger.ZERO;
		JSONArray voutArr = txObj.getJSONArray("vout");
		int voutArrLength = voutArr.length();
		for (int i = 0; i < voutArrLength; i++)
		{
			JSONObject voutObj = voutArr.getJSONObject(i);
			JSONObject scriptPubKeyObj = voutObj.getJSONObject("scriptPubKey");
			if (scriptPubKeyObj.has("addresses") == false)
				continue;

			Iterator<Object> it = scriptPubKeyObj.getJSONArray("addresses").iterator();
			while (it.hasNext())
			{
				if (contractAddress.equals(it.next()))
				{
					valueSum = valueSum.add(getValueSum(voutObj, tokenName));
				}
			}
		}

		if (valueSum.compareTo(BigInteger.ZERO) <= 0)
			return false;

		return true;
	}

	protected int insertTxList(List<Map<String, Object>> list, long blockHeight, Map<String, Object> chainInfo)
	{
		SqlSession sqlSession = null;

		try
		{
			ContractService service = ContractService.getInstance();

			if ((list != null) && (list.size() > 0))
			{
				sqlSession = getSqlSession(false);
				return service.insertTxList(list, blockHeight, chainInfo, sqlSession);
			}
			else
			{
				sqlSession = getSqlSession();
				return service.insertBlockHeight(blockHeight, chainInfo, sqlSession);
			}
		}
		finally
		{
			if (sqlSession != null)
				sqlSession.close();
		}
	}	
}