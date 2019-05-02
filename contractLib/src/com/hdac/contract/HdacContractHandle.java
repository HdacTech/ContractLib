package com.hdac.contract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.bitcoinj.core.ECKey;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hdac.comm.StringUtil;
import com.hdac.service.ContractService;
import com.hdac.service.RpcService;
import com.hdacSdk.hdacWallet.HdacTransaction;
import com.hdacSdk.hdacWallet.HdacWallet;

abstract class HdacContractHandle extends HdacContract
{
	protected abstract void init();
	protected abstract void handle(String[] split, Map<String, Object> map, JSONObject resultObj, String txid, Map<String, Object> config);
	protected abstract List<JSONObject> addUtxos(Map<String, Object> tokenInfo, Map<String, Object> config);
	protected abstract Map<String, Object> getSenderInfo(JSONObject obj, String tokenName);
	protected abstract boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, List<Map<String, Object>> senderList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos, String dataValue);

	protected void handle(List<Map<String, Object>> list, Map<String, Object> config)
	{
		if ((list == null) || (list.size() <= 0))
			return;

		RpcService rService = RpcService.getInstance();

		for (Map<String, Object> map : list)
		{
			String txid = StringUtil.nvl(map.get("txid"));
			JSONObject resultObj = rService.getRawTransaction(txid, config);
			JSONArray dataArr = resultObj.getJSONArray("data");
			if (dataArr.length() <= 0)
				continue;

			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println(resultObj);

			String data = StringUtil.hexToString(dataArr.getString(0));
			System.out.println(data);
			String[] split = data.split(",");

			handle(split, map, resultObj, txid, config);
//			break;
		}
	}

	public void swap(HdacWallet wallet, Map<String, Object> txMap, JSONObject resultObj
		, Map<String,Object> tokenInfo, Map<String,Object> fromChain, Map<String,Object> toChain, String txid, String[] split, long checkBlockHeight)
	{
		RpcService service = RpcService.getInstance();

		// prestep. check block height
		long blockCount = service.getBlockCount(fromChain);
		long blockHeight = Long.parseLong(StringUtil.nvl(txMap.get("block_height"), "-1"));
		if ((blockHeight <= -1) || (blockHeight + checkBlockHeight >= blockCount))
			return;

		// step 0. gather utxos
		List<JSONObject> utxos = addUtxos(tokenInfo, toChain);

		String tokenName = StringUtil.nvl(tokenInfo.get("tokenName"));
		// step 1. split transaction by vin addresses
		List<Map<String, Object>> senderList = splitTransaction(resultObj, txid, tokenName, fromChain);
		if (senderList.size() <= 0)
			return;
		System.out.println(senderList);

		// step 2. create new transaction
		HdacTransaction transaction = new HdacTransaction(wallet.getNetworkParams());

		// step 3. add unsigned data to transaction
		boolean success = addUnsignedData(transaction, txMap, senderList, resultObj, tokenInfo, utxos, split[1]);
		if (success == false)
			return;

		// step 4. add signed data to transaction
		putSignListToJson(transaction, wallet, utxos);

		// step 5. make raw transaction
		String rawTx = service.makeRawTransaction(transaction);
		System.out.println("rawTx\n" + rawTx);

		// step 6. send transaction
		String sendTxid = service.sendRawTransaction(rawTx, toChain);

		// step 7. insert DB
		if (sendTxid != null)
		{
			SqlSession sqlSession = getSqlSession(false);

			try
			{
				ContractService cService = ContractService.getInstance();
				int ret = cService.insertHandle(sendTxid, txMap, split[0], fromChain, sqlSession);
				if (ret > 0)
					sqlSession.commit();
			}
			finally
			{
				sqlSession.close();
			}
		}
	}

	private List<Map<String, Object>> splitTransaction(JSONObject resultObj, String txid, String tokenName, Map<String, Object> config)
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		RpcService service = RpcService.getInstance();

		JSONArray vinArr = resultObj.getJSONArray("vin");
		System.out.println("!" + resultObj);
		int vinArrLength = vinArr.length();
		for (int i = 0; i < vinArrLength; i++)
		{
			JSONObject vinObj = vinArr.getJSONObject(i);
			String vinTxid = vinObj.getString("txid");

			JSONObject obj = service.getRawTransaction(vinTxid, config);
			System.out.println(obj);

			int voutN = vinObj.getInt("vout");
			JSONArray voutArr = obj.getJSONArray("vout");
			Iterator<Object> it = voutArr.iterator();
			while (it.hasNext())
			{
				JSONObject inObj = (JSONObject)it.next();
				if (inObj.getInt("n") == voutN)
				{
					Map<String, Object> map = getSenderInfo(inObj, tokenName);
					if (map != null)
						list.add(map);
				}
			}
		}
		return list;
	}

	private void putSignListToJson(HdacTransaction transaction, HdacWallet wallet, List<JSONObject> utxos)
	{
		int len = utxos.size();
		for (int i = 0; i < len; i++)
		{
			JSONObject utxo = utxos.get(i);
			ECKey sign = wallet.getHdacSigKey(utxo.getString("address"));
			if (sign != null)
				transaction.setSignedInput(i, utxo, sign, true);
		}
	}
}