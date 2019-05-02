package com.hdac.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hdacSdk.hdacWallet.HdacCoreAddrParams;
import com.hdacSdk.hdacWallet.HdacWallet;
import com.hdacSdk.hdacWallet.HdacWalletManager;
import com.hdacSdk.hdacWallet.HdacWalletUtils;
import com.hdacSdk.hdacWallet.HdacWalletUtils.NnmberOfWords;

public class HdacUtil
{
	public static HdacWallet generateNewWallet()
	{
		String passPhrase = null;
		List<String> seedWords = HdacUtil.getSeedWord(passPhrase);

		HdacCoreAddrParams params = new HdacCoreAddrParams(true);
		return HdacWalletManager.generateNewWallet(seedWords, passPhrase, params);
	}

	public static HdacWallet getHdacWallet(List<String> seedWords, String passPhrase)
	{
		HdacCoreAddrParams params = new HdacCoreAddrParams(true);	// hdac network parameter (true : public network / false : private network)
		return HdacWalletManager.generateNewWallet(seedWords, passPhrase, params);
	}

	public static List<String> getSeedWord(String passPhrase)
	{
		HdacWalletUtils.NnmberOfWords[] num =
		{
			NnmberOfWords.MNEMONIC_12_WORDS,
			NnmberOfWords.MNEMONIC_15_WORDS,
			NnmberOfWords.MNEMONIC_18_WORDS,
			NnmberOfWords.MNEMONIC_21_WORDS,
			NnmberOfWords.MNEMONIC_24_WORDS,
		};
		int rand = (int)(Math.random() * 5);
		List<String> seedWords = HdacWalletUtils.getRandomSeedWords(num[rand]);

		HdacWallet hdacWallet = getHdacWallet(seedWords, passPhrase);

		if (hdacWallet.isValidWallet())
			return seedWords;

		return null;
	}

	public static JSONObject getDataJSONObject(String method, Object[] params, Map<String, Object> config)
	{
		return getDataJSONObject(method, params, null, config);
	}
	public static JSONObject getDataJSONObject(String method, Object[] params, JSONObject options, Map<String, Object> config)
	{
		String result = null;
		try
		{
			result = getDataFromRPC(method, params, options, config);
			JSONObject obj = new JSONObject(result);

			if (obj.has("result"))
				return obj.getJSONObject("result");
		}
		catch (Exception e)
		{
			System.out.println("result : " + result);
			e.printStackTrace();
		}
		return new JSONObject();
	}

	public static JSONArray getDataJSONArray(String method, Object[] params, Map<String, Object> config)
	{
		return getDataJSONArray(method, params, null, config);
	}
	public static JSONArray getDataJSONArray(String method, Object[] params, JSONObject options, Map<String, Object> config)
	{
		try
		{
			String result = getDataFromRPC(method, params, options, config);
			JSONObject obj = new JSONObject(result);

			if (obj.has("result"))
				return obj.getJSONArray("result");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new JSONArray();
	}

	public static String getDataFromRPC(String method, Object[] params, Map<String, Object> config)
	{
		return getDataFromRPC(method, params, null, config);
	}
	public static String getDataFromRPC(String method, Object[] params, JSONObject options, Map<String, Object> config)
	{
		System.out.println("getDataFromRPC method = " + method);
		String body = "";
		try
		{
			body = getBody(method, params, options);
			System.out.println("String body = " + body);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		return sendRPC(body, config);
	}

	private static String getBody(String method, Object[] params, JSONObject options) throws JSONException
	{
		long id = (long)(Math.random() * 10000);

		JSONObject obj = new JSONObject();
		obj.put("jsonrpc",	"2.0");
		obj.put("method",	method);
		obj.put("params",	params);
		obj.put("id",		id);

		if (options != null)
			obj.put("options",	options);

		return obj.toString();
	}

	private static String sendRPC(String body, Map<String, Object> config)
	{
		
		StringBuilder result = new StringBuilder();
		CloseableHttpResponse response1 = null;

		try
		{
			StringBuilder auth = new StringBuilder("Basic ");
			auth.append(Base64.getEncoder().encodeToString((config.get("rpc_user") + ":" + config.get("rpc_password")).getBytes("UTF-8")));

			String address = StringUtil.nvl(config.get("rpc_address"));
			String port = StringUtil.nvl(config.get("rpc_port"));

			if (address.startsWith("http") == false)
				address = "http://" + address;

			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost(address + ":" + port);

			httpPost.addHeader("content-type", "application/json");
			httpPost.addHeader("Authorization", auth.toString());

			HttpEntity entity = new StringEntity(body);
	        httpPost.setEntity(entity);

			response1 = httpclient.execute(httpPost);

		    HttpEntity entity1 = response1.getEntity();

		    BufferedReader rd = new BufferedReader(new InputStreamReader(entity1.getContent()));

		    String line = "";
		    while ((line = rd.readLine()) != null)
		    {
		    	result.append(line);
		    }

		    EntityUtils.consume(entity1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
		    try
		    {
		    	if (response1 != null)
		    		response1.close();
			}
		    catch (IOException e)
		    {
			}
		}

		return result.toString();
	}

	public static List<String> decodeSeed(List<String> seed, String key)
	{
		List<String> decSeed = new ArrayList<String>();
		for (String word : seed)
		{
			decSeed.add(CipherUtil.AesDecode(word, key));
		}
		return decSeed;
	}

	public static String getKey()
	{
		return "IOT";//HdacUtil.chainName;
	}
}