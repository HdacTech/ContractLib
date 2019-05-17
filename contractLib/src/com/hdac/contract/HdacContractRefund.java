package com.hdac.contract;

import java.util.Iterator;
import java.util.LinkedHashMap;
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

abstract class HdacContractRefund extends HdacContract
{
	protected abstract void init();
	protected abstract void refund(Map<String, Object> map, JSONObject resultObj, String txid);
	protected abstract List<JSONObject> addUtxos(Map<String, Object> tokenInfo, Map<String, Object> config);
	protected abstract void addSenderInfo(JSONObject obj, String tokenName, Map<String, Map<String, Object>> recvMap);
	protected abstract boolean addUnsignedData(HdacTransaction transaction, Map<String, Object> txMap
		, Map<String, Map<String, Object>> recvList, JSONObject resultObj, Map<String, Object> tokenInfo, List<JSONObject> utxos);

	protected void refund(List<Map<String, Object>> list, Map<String, Object> config)
	{
		if ((list == null) || (list.size() <= 0))
			return;

		RpcService rService = RpcService.getInstance();

		for (Map<String, Object> map : list)
		{
			String txid = StringUtil.nvl(map.get("txid"));
			JSONObject resultObj = rService.getRawTransaction(txid, config);
			JSONArray dataArr = resultObj.getJSONArray("data");
			if (dataArr.length() > 0)
				continue;

			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println(resultObj);

			refund(map, resultObj, txid);
//			break;
		}
	}

	protected void refund(HdacWallet wallet, Map<String, Object> txMap, JSONObject resultObj
		, Map<String, Object> tokenInfo, Map<String, Object> config, String txid)
	{
		// step 0. gather utxos
		List<JSONObject> utxos = addUtxos(tokenInfo, config);

		String tokenName = StringUtil.nvl(tokenInfo.get("tokenName"));
		// step 1. split transaction by vin addresses
		Map<String, Map<String, Object>> recvList = splitTransaction(resultObj, txid, tokenName, config);
		if (recvList.size() <= 0)
			return;
		System.out.println(recvList);

		// step 2. create new transaction
		HdacTransaction transaction = new HdacTransaction(wallet.getNetworkParams());

		// step 3. add unsigned data to transaction
		boolean success = addUnsignedData(transaction, txMap, recvList, resultObj, tokenInfo, utxos);
		if (success == false)
			return;

		// step 4. add signed data to transaction
		putSignListToJson(transaction, wallet, utxos);

		RpcService service = RpcService.getInstance();
		// step 5. make raw transaction
		String rawTx = service.makeRawTransaction(transaction);
		System.out.println("rawTx\n" + rawTx);

		// step 6. send transaction
		String sendTxid = service.sendRawTransaction(rawTx, config);
		System.out.println("txid : " + sendTxid);

		// step 7. insert DB
		if (sendTxid != null)
		{
			SqlSession sqlSession = getSqlSession(false);

			try
			{
				ContractService cService = ContractService.getInstance();
				int ret = cService.insertRefund(sendTxid, txMap, config, sqlSession);
				if (ret > 0)
					sqlSession.commit();
			}
			finally
			{
				sqlSession.close();
			}
		}
	}

	private Map<String, Map<String, Object>> splitTransaction(JSONObject resultObj, String txid, String tokenName, Map<String, Object> config)
	{
		Map<String, Map<String, Object>> recvMap = new LinkedHashMap<String, Map<String, Object>>();

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
					addSenderInfo(inObj, tokenName, recvMap);
				}
			}
		}
		return recvMap;
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