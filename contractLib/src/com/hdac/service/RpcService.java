package com.hdac.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.ECKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hdac.comm.HdacUtil;
import com.hdac.comm.JsonUtil;
import com.hdac.comm.StringUtil;
import com.hdacSdk.hdacWallet.HdacTransaction;
import com.hdacSdk.hdacWallet.HdacWallet;

/**
 * RPC service
 * 
 * 
 * @see     java.util.List
 * @see     java.math.BigDecimal
 * @see     java.math.BigInteger
 * @see     java.util.ArrayList
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.bitcoinj.core.ECKey
 * @see     org.json.JSONArray
 * @see     org.json.JSONException
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class RpcService
{
	private RpcService()
	{
	}
	public static RpcService getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final RpcService INSTANCE = new RpcService();  
	}

	/**
	 * get the information of all transaction informations of selected block.
	 * 
	 * @param blockHeight (long) block index
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (JSONObject) return block informations
	 */	
	public JSONObject getblock(long blockHeight, Map<String, Object> config)
	{
		Object[] params = new Object[2];
		params[0] = String.valueOf(blockHeight);
		params[1] = 4;

		return HdacUtil.getDataJSONObject("getblock", params, config);
	}
	
	/**
	 * get the block count
	 * 
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (JSONObject) return block informations
	 */	
	public long getBlockCount(Map<String, Object> config) throws JSONException
	{
		String strBlockCount = HdacUtil.getDataFromRPC("getblockcount", new String[0], config);
		JSONObject objBlockCount = new JSONObject(strBlockCount);
		return objBlockCount.getLong("result");
	}

	/**
	 * get the information of all transaction informations of selected block.
	 * 
	 * @param txid (String) transaction hash value to get raw data
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (JSONObject) return transaction informations
	 */	
	public JSONObject getRawTransaction(String txid, Map<String, Object> config)
	{
		Object[] params = new Object[2];
		params[0] = txid;
		params[1] = 1;

		return HdacUtil.getDataJSONObject("getrawtransaction", params, config);
	}


	/**
	 * get the information of all transaction informations of selected block.
	 * 
	 * @param paramMap (Map(String, Object)) address,token data to get the list of utxos
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (List(JSONObject)) return utxo list
	 */	
	public List<JSONObject> getUtxos(Map<String, Object> paramMap, Map<String, Object> config) throws JSONException
	{
		List<JSONObject> list = new ArrayList<JSONObject>();

		long blockCount = getBlockCount(config);

		String[] addresses = StringUtil.nvl(paramMap.get("addresses")).split(",");

		Object[] params = new Object[1];
		params[0] = addresses;

		JSONArray objMempoolArray = HdacUtil.getDataJSONArray("getaddressmempool", addresses, config);

		List<JSONObject> vinList = new ArrayList<JSONObject>();
		separateMempoolList(vinList, list, objMempoolArray);

		JSONArray objUtxoBlockArray = HdacUtil.getDataJSONArray("getaddressutxos", addresses, config);
		int blockLength = objUtxoBlockArray.length();
		for (int i = 0; i < blockLength; i++)
		{
			JSONObject obj = objUtxoBlockArray.getJSONObject(i);

			if (txContains(list, obj) == false)
				list.add(obj);
		}

		for (int i = list.size() - 1; i >= 0; i--)
		{
			JSONObject obj = list.get(i);

			if (txContainsPrev(vinList, obj))
			{
				list.remove(i);
				continue;
			}

			if (obj.has("script"))	// rpc
			{
				String txid = obj.getString("txid");
				BigDecimal satoshis = obj.getBigDecimal("satoshis");

				JSONObject newObj = new JSONObject();
				newObj.put("unspent_hash",		txid);
				newObj.put("address",			obj.get("address"));
				newObj.put("scriptPubKey",		obj.get("script"));
				newObj.put("amount",			satoshis.divide(BigDecimal.TEN.pow(8)));
				newObj.put("vout",				obj.get("outputIndex"));
				newObj.put("confirmations",		blockCount - obj.getLong("height") + 1);
				newObj.put("satoshis",			satoshis.toBigInteger());
				newObj.put("txid",				txid);

				list.set(i, newObj);
			}
			else	// mempool
			{
				String txid = obj.getString("txid");
				int index = obj.getInt("index");
				BigDecimal satoshis = obj.getBigDecimal("satoshis");
				String scriptPubKey = getScriptPubKey(txid, index, config);

				JSONObject newObj = new JSONObject();
				newObj.put("unspent_hash",		txid);
				newObj.put("address",			obj.get("address"));
				newObj.put("scriptPubKey",		scriptPubKey);
				newObj.put("amount",			satoshis.divide(BigDecimal.TEN.pow(8)));
				newObj.put("vout",				index);
				newObj.put("confirmations",		0);
				newObj.put("satoshis",			satoshis.toBigInteger());
				newObj.put("txid",				txid);

				list.set(i, newObj);
			}
		}
		return list;
	}

	private void separateMempoolList(List<JSONObject> vinList, List<JSONObject> voutList, JSONArray objMempoolArray) throws JSONException
	{
		int length = objMempoolArray.length();
		for (int i = 0; i < length; i++)
		{
			JSONObject obj = objMempoolArray.getJSONObject(i);
			long satoshis = obj.getLong("satoshis");
			if (satoshis > 0)
			{
				voutList.add(obj);
			}
			else if (satoshis < 0)
			{
				vinList.add(obj);
			}
		}
	}

	private boolean txContains(List<JSONObject> list, JSONObject obj) throws JSONException
	{
		String txid = obj.getString("txid");
		for (JSONObject source : list)
		{
			if (txid.equals(source.getString("txid")))
			{
				int sIndex = getIndex(source);
				int tIndex = getIndex(obj);

				if (sIndex == tIndex)
					return true;
			}
		}
		return false;
	}

	private int getIndex(JSONObject obj) throws JSONException
	{
		int index = -1;
		
		if (obj.has("vout"))
		{
			index = obj.getInt("vout");
		}
		else if (obj.has("index"))
		{
			index = obj.getInt("index");
		}
		else if (obj.has("outputIndex"))
		{
			index = obj.getInt("outputIndex");
		}
		return index;
	}

	private boolean txContainsPrev(List<JSONObject> list, JSONObject obj) throws JSONException
	{
		String txid = obj.getString("txid");
		for (JSONObject source : list)
		{
			if (txid.equals(source.getString("prevtxid")))
			{
				int sIndex = getIndexPrev(source);
				int tIndex = getIndex(obj);

				if (sIndex == tIndex)
					return true;
			}
		}
		return false;
	}

	private int getIndexPrev(JSONObject obj) throws JSONException
	{
		int index = -1;
		
		if (obj.has("prevout"))
		{
			index = obj.getInt("prevout");
		}
		return index;
	}

	private String getScriptPubKey(String txid, int index, Map<String, Object> config) throws JSONException
	{
		Object[] param = new Object[2];
		param[0] = txid;
		param[1] = index;

		JSONObject options = new JSONObject();
		options.put("unconfirmed", true);

		JSONObject voutObj = HdacUtil.getDataJSONObject("gettxout", param, options, config);
		if (voutObj.has("scriptPubKey"))
		{
			JSONObject scriptObj = voutObj.getJSONObject("scriptPubKey");
			if (scriptObj.has("hex"))
				return scriptObj.getString("hex");
		}
		return "";
	}


	/**
	 * get asset utxos of addresses,token
	 * 
	 * @param paramMap (Map(String, Object)) address,token data to get the list of utxos
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (List(JSONObject)) return utxo list
	 */	
	public List<JSONObject> getAssetUtxos(Map<String, Object> paramMap, Map<String, Object> config)
	{
		List<JSONObject> listMap = new ArrayList<JSONObject>();

		try
		{
			Object[] params = new Object[3];
			params[0] = 0;
			params[1] = 999999;
			params[2] = StringUtil.nvl(paramMap.get("addresses")).split(",");

			JSONArray objUtxoBlockArray = HdacUtil.getDataJSONArray("listunspent", params, config);

			String asset = StringUtil.nvl(paramMap.get("asset"));
			int length = objUtxoBlockArray.length();
			for (int i = 0; i < length; i++)
			{
				JSONArray assetArray = objUtxoBlockArray.getJSONObject(i).getJSONArray("assets");

				if (assetArray.length() <= 0)
					continue;

				if (asset.equals(assetArray.getJSONObject(0).get("name")))
				{
					JSONObject obj = objUtxoBlockArray.getJSONObject(i);
					BigDecimal amount = obj.getBigDecimal("amount");

					JSONObject map = new JSONObject();
					map.put("unspent_hash",		obj.get("txid"));
					map.put("address",			obj.get("address"));
					map.put("scriptPubKey",		obj.get("scriptPubKey"));
					map.put("amount",			amount);
					map.put("vout",				obj.get("vout"));
					map.put("confirmations",	obj.get("confirmations"));
					map.put("satoshis",			amount.multiply(BigDecimal.TEN.pow(8)).toBigInteger());
					map.put("txid",				obj.get("txid"));
					map.put("assets",			obj.getJSONArray("assets"));

					listMap.add(map);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return listMap;
	}


	/**
	 * get asset utxos of addresses,token
	 * 
	 * @param transaction (HdacTransaction) make transaction object to hex string 
	 * @return    (String) return hex string
	 */
	public String makeRawTransaction(HdacTransaction transaction)
	{
		return StringUtil.toSmallLetter(transaction.getTxBuilder().build().toHex(), 0);
	}

	/**
	 * broadcast transaction to config blockchain
	 * 
	 * @param rawTx (String) raw transaction hex-data 
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (String) return txid
	 */	
	public String sendRawTransaction(String rawTx, Map<String, Object> config) throws JSONException
	{
		Object[] params = new Object[1];
		params[0] = rawTx;

		String strSendResult = HdacUtil.getDataFromRPC("sendrawtransaction", params, config);
		JSONObject objSendResult = new JSONObject(strSendResult);

		if (objSendResult.get("error").equals(null))
			return objSendResult.getString("result");

		return null;
	}


	/**
	 * issue token to config blockchain
	 * 
	 * @param paramMap (Map(String, Object)) informations required to issue the token
	 * @param config (Map(String, Object)) the chain information to connect
	 * @return    (String) return issued txid
	 */	
	public String issueToken(Map<String, Object> paramMap, Map<String, Object> config) throws JSONException
	{
//		JSONObject tokenParams = new JSONObject();
//		tokenParams.put("name", StringUtil.nvl(paramMap.get("tokenName")));
//		tokenParams.put("open", "true".equals(paramMap.get("open")));

		Object[] params = new Object[4];
		params[0] = StringUtil.nvl(paramMap.get("address"));
//		params[1] = tokenParams;
		params[1] = StringUtil.nvl(paramMap.get("tokenName"));
		params[2] = new BigDecimal(StringUtil.nvl(paramMap.get("amount"), "0"));
		params[3] = new BigDecimal(StringUtil.nvl(paramMap.get("unit"), "0"));

		if(params[1].toString().indexOf('{') != -1) {
			JSONObject typeName = new JSONObject(StringUtil.nvl(paramMap.get("tokenName")));
			if(typeName.has("name"))  params[1] = typeName;
		}
		
		System.out.println("params : " + params[0]);
		System.out.println("params : " + params[1]);
		System.out.println("params : " + params[2]);
		System.out.println("params : " + params[3]);
		
		JSONObject result = new JSONObject(HdacUtil.getDataFromRPC("issue", params, config));

		if (result.get("error").equals(null))
			return result.getString("result");

		return null;
	}

	/**
	 * register contract informations to main blockchain
	 * 
	 * @param wallet (HdacWallet) hdac wallet for sign
	 * @param data (JSONArray) utxo json array 
	 * @param paramMap (Map(String, Object)) Information required to register a contract, including token information
	 * @return    (String) return raw transaction hex-string
	 */		
	public String makeRawTransaction(HdacWallet wallet, JSONArray data, Map<String, Object> paramMap)
	{
		HdacTransaction transaction = new HdacTransaction(wallet.getNetworkParams());
		String contractString = "";
		
		if (paramMap.size() > 0)
			contractString = JsonUtil.toJsonString(paramMap).toString();

		BigDecimal balance = BigDecimal.ZERO;
		try
		{
			int len = data.length();
	    	for (int i = 0; i < len; i++)
	    	{
				JSONObject utxo = data.getJSONObject(i);
				balance = balance.add(utxo.getBigDecimal("amount"));

				transaction.addInput(utxo);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}

		//for checking balance 
		BigInteger lBalance	= balance.multiply(BigDecimal.TEN.pow(8)).toBigInteger();
		BigInteger fee		= BigInteger.valueOf(2).multiply(BigInteger.TEN.pow(8 - 2)).add(BigInteger.valueOf(contractString.length()).multiply(BigInteger.TEN.pow(3)));
		BigInteger remain	= lBalance.subtract(fee);
		
		if (remain.compareTo(BigInteger.ZERO) < 0)
			return null;

		transaction.addOutput(wallet.getHdacAddress(), remain.longValue());
		transaction.addOpReturnOutput(JsonUtil.toJsonString(paramMap).toString(), "UTF-8");

		try
		{
			int len = data.length();
	    	for (int i = 0; i < len; i++)
			{
				JSONObject utxo = data.getJSONObject(i);
				ECKey sign = wallet.getHdacSigKey(utxo.getString("address"));
				if (sign != null)
					transaction.setSignedInput(i, utxo, sign);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return transaction.getTxBuilder().build().toHex();
	}
}