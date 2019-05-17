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

import com.hdac.comm.HdacUtil;
import com.hdac.comm.StringUtil;
import com.hdac.service.ContractService;
import com.hdac.service.RpcService;
import com.hdacSdk.hdacWallet.HdacWallet;

/**
 * abstract HdacContractGrab class extends HdacContract
 * (Database SqlSesseion control) 
 * 
 * @version 0.8
 * 
 * @see     import java.math.BigInteger
 * @see     import java.util.HashMap
 * @see     import java.util.Iterator
 * @see     import java.util.List
 * @see     import java.util.Map
 * @see     import org.apache.ibatis.session.SqlSession
 * @see     import org.json.JSONArray
 * @see     import org.json.JSONException
 * @see     import org.json.JSONObject
 */
abstract class HdacContractGrab extends HdacContract
{
	protected abstract void init();
	protected abstract BigInteger getValueSum(JSONObject voutObj, String tokenName);

	/**
	 * get current chain block height
	 * 
	 * @param chainInfo (Map<String, Object>) set chain info 
	 * @return    (long) latest block height selected chain 
	 */		
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

	/**
	 * get transaction list associated with contract address
	 * 
	 * @param txList (List<Map<String, Object>>) get the list transactions associated with contact address
	 * @param tokenInfo (Map<String, Object>) get the token information
	 * @param chainInfo (Map<String, Object>) set the chain information to use
	 * @param loopCount (long) Number of blocks to get details
	 * @return    (long) last block height to get details 
	 */
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

	/**
	 * get transaction list associated with contract address
	 * 
	 * @param txList (List<Map<String, Object>>) get the list transactions associated with contact address
	 * @param wallet (HdacWallet) get the wallet with address
	 * @param obj (JSONObject) set the chain information to use
	 * @param contractAddress (String) contract address to use 
	 * @param tokenName (String) token name to use
	 * @return    (void) 
	 */
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

	/**
	 * get transaction list associated with contractaddress
	 * 
	 * @param wallet (HdacWallet) get the wallet with address
	 * @param txObj (JSONObject) get the transaction information to compare with contract address
	 * @param contractAddress (String) contract address to use 
	 * @param tokenName (String) token name to use
	 * @return    (boolean)  true if the transaction is associated with a contract address
	 */
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
			String vinaddress = wallet.convPubkeyToHdacAddress(StringUtil.toByteArray(publickey));
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

	/**
	 * Insert a transaction in the maria db that is associated with contract address.
	 * 
	 * @param list (List<Map<String, Object>> list) the list of transactions associated with contract address
	 * @param blockHeight (long) get the last blockheight 
	 * @param chainInfo (Map<String, Object>) set the chain information to use
	 * @return    (int)  Results inserted into db
	 */	
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