/*
	https://github.com/BlackOverlord666/mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package mslinks.extra;

import io.ByteReader;
import io.ByteWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import mslinks.Serializable;
import mslinks.ShellLinkException;

public class ConsoleFEData implements Serializable {
	public static final int signature = 0xA0000004;
	public static final int size = 0xc;
	
	private String lang;
	
	public ConsoleFEData() {
		Locale l = Locale.getDefault();
		lang = l.getLanguage() + "-" + l.getCountry();
	}
	
	public ConsoleFEData(ByteReader br, int sz) throws ShellLinkException, IOException {
		if (sz != size) throw new ShellLinkException();
		int t = (int)br.read4bytes();
		lang = ids.get(t >>> 16);
	}

	@Override
	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(size);
		bw.write4bytes(signature);
		bw.write4bytes(langs.get(lang) << 16);
	}
	
	public String getLanguage() { return lang; }
	public ConsoleFEData setLanguage(String s) { lang = s; return this;}
	
	private static Object[] langData = new Object[] {
		"ar", 0x0001,
		"bg", 0x0002,
		"ca", 0x0003,
		"zh-Hans", 0x0004,
		"cs", 0x0005,
		"da", 0x0006,
		"de", 0x0007,
		"el", 0x0008,
		"en", 0x0009,
		"es", 0x000a,
		"fi", 0x000b,
		"fr", 0x000c,
		"he", 0x000d,
		"hu", 0x000e,
		"is", 0x000f,
		"it", 0x0010,
		"ja", 0x0011,
		"ko", 0x0012,
		"nl", 0x0013,
		"no", 0x0014,
		"pl", 0x0015,
		"pt", 0x0016,
		"rm", 0x0017,
		"ro", 0x0018,
		"ru", 0x0019,
		"bs", 0x001a,
		"hr", 0x001a,
		"sr", 0x001a,
		"sk", 0x001b,
		"sq", 0x001c,
		"sv", 0x001d,
		"th", 0x001e,
		"tr", 0x001f,
		"ur", 0x0020,
		"id", 0x0021,
		"uk", 0x0022,
		"be", 0x0023,
		"sl", 0x0024,
		"et", 0x0025,
		"lv", 0x0026,
		"lt", 0x0027,
		"tg", 0x0028,
		"fa", 0x0029,
		"vi", 0x002a,
		"hy", 0x002b,
		"az", 0x002c,
		"eu", 0x002d,
		"dsb", 0x002e,
		"hsb", 0x002e,
		"mk", 0x002f,
		"st", 0x0030,
		"ts", 0x0031,
		"tn", 0x0032,
		"ve", 0x0033,
		"xh", 0x0034,
		"zu", 0x0035,
		"af", 0x0036,
		"ka", 0x0037,
		"fo", 0x0038,
		"hi", 0x0039,
		"mt", 0x003a,
		"se", 0x003b,
		"ga", 0x003c,
		"yi", 0x003d,
		"ms", 0x003e,
		"kk", 0x003f,
		"ky", 0x0040,
		"sw", 0x0041,
		"tk", 0x0042,
		"uz", 0x0043,
		"tt", 0x0044,
		"bn", 0x0045,
		"pa", 0x0046,
		"gu", 0x0047,
		"or", 0x0048,
		"ta", 0x0049,
		"te", 0x004a,
		"kn", 0x004b,
		"ml", 0x004c,
		"as", 0x004d,
		"mr", 0x004e,
		"sa", 0x004f,
		"mn", 0x0050,
		"bo", 0x0051,
		"cy", 0x0052,
		"km", 0x0053,
		"lo", 0x0054,
		"my", 0x0055,
		"gl", 0x0056,
		"kok", 0x0057,
		"mni", 0x0058,
		"sd", 0x0059,
		"syr", 0x005a,
		"si", 0x005b,
		"chr", 0x005c,
		"iu", 0x005d,
		"am", 0x005e,
		"tzm", 0x005f,
		"ks", 0x0060,
		"ne", 0x0061,
		"fy", 0x0062,
		"ps", 0x0063,
		"fil", 0x0064,
		"dv", 0x0065,
		"bin", 0x0066,
		"ff", 0x0067,
		"ha", 0x0068,
		"ibb", 0x0069,
		"yo", 0x006a,
		"quz", 0x006b,
		"nso", 0x006c,
		"ba", 0x006d,
		"lb", 0x006e,
		"kl", 0x006f,
		"ig", 0x0070,
		"kr", 0x0071,
		"om", 0x0072,
		"ti", 0x0073,
		"gn", 0x0074,
		"haw", 0x0075,
		"la", 0x0076,
		"so", 0x0077,
		"ii", 0x0078,
		"pap", 0x0079,
		"arn", 0x007a,
		"moh", 0x007c,
		"br", 0x007e,
		"ug", 0x0080,
		"mi", 0x0081,
		"oc", 0x0082,
		"co", 0x0083,
		"gsw", 0x0084,
		"sah", 0x0085,
		"qut", 0x0086,
		"rw", 0x0087,
		"wo", 0x0088,
		"prs", 0x008c,
		"gd", 0x0091,
		"ku", 0x0092,
		"quc", 0x0093,
		"ar-SA", 0x0401,
		"bg-BG", 0x0402,
		"ca-ES", 0x0403,
		"zh-TW", 0x0404,
		"cs-CZ", 0x0405,
		"da-DK", 0x0406,
		"de-DE", 0x0407,
		"el-GR", 0x0408,
		"en-US", 0x0409,
		"es-ES_tradnl", 0x040a,
		"fi-FI", 0x040b,
		"fr-FR", 0x040c,
		"he-IL", 0x040d,
		"hu-HU", 0x040e,
		"is-IS", 0x040f,
		"it-IT", 0x0410,
		"ja-JP", 0x0411,
		"ko-KR", 0x0412,
		"nl-NL", 0x0413,
		"nb-NO", 0x0414,
		"pl-PL", 0x0415,
		"pt-BR", 0x0416,
		"rm-CH", 0x0417,
		"ro-RO", 0x0418,
		"ru-RU", 0x0419,
		"hr-HR", 0x041a,
		"sk-SK", 0x041b,
		"sq-AL", 0x041c,
		"sv-SE", 0x041d,
		"th-TH", 0x041e,
		"tr-TR", 0x041f,
		"ur-PK", 0x0420,
		"id-ID", 0x0421,
		"uk-UA", 0x0422,
		"be-BY", 0x0423,
		"sl-SI", 0x0424,
		"et-EE", 0x0425,
		"lv-LV", 0x0426,
		"lt-LT", 0x0427,
		"tg-Cyrl-TJ", 0x0428,
		"fa-IR", 0x0429,
		"vi-VN", 0x042a,
		"hy-AM", 0x042b,
		"az-Latn-AZ", 0x042c,
		"eu-ES", 0x042d,
		"hsb-DE", 0x042e,
		"mk-MK", 0x042f,
		"st-ZA", 0x0430,
		"ts-ZA", 0x0431,
		"tn-ZA", 0x0432,
		"ve-ZA", 0x0433,
		"xh-ZA", 0x0434,
		"zu-ZA", 0x0435,
		"af-ZA", 0x0436,
		"ka-GE", 0x0437,
		"fo-FO", 0x0438,
		"hi-IN", 0x0439,
		"mt-MT", 0x043a,
		"se-NO", 0x043b,
		"yi-Hebr", 0x043d,
		"ms-MY", 0x043e,
		"kk-KZ", 0x043f,
		"ky-KG", 0x0440,
		"sw-KE", 0x0441,
		"tk-TM", 0x0442,
		"uz-Latn-UZ", 0x0443,
		"tt-RU", 0x0444,
		"bn-IN", 0x0445,
		"pa-IN", 0x0446,
		"gu-IN", 0x0447,
		"or-IN", 0x0448,
		"ta-IN", 0x0449,
		"te-IN", 0x044a,
		"kn-IN", 0x044b,
		"ml-IN", 0x044c,
		"as-IN", 0x044d,
		"mr-IN", 0x044e,
		"sa-IN", 0x044f,
		"mn-MN", 0x0450,
		"bo-CN", 0x0451,
		"cy-GB", 0x0452,
		"km-KH", 0x0453,
		"lo-LA", 0x0454,
		"my-MM", 0x0455,
		"gl-ES", 0x0456,
		"kok-IN", 0x0457,
		"mni-IN", 0x0458,
		"sd-Deva-IN", 0x0459,
		"syr-SY", 0x045a,
		"si-LK", 0x045b,
		"chr-Cher-US", 0x045c,
		"iu-Cans-CA", 0x045d,
		"am-ET", 0x045e,
		"tzm-Arab-MA", 0x045f,
		"ks-Arab", 0x0460,
		"ne-NP", 0x0461,
		"fy-NL", 0x0462,
		"ps-AF", 0x0463,
		"fil-PH", 0x0464,
		"dv-MV", 0x0465,
		"bin-NG", 0x0466,
		"fuv-NG", 0x0467,
		"ha-Latn-NG", 0x0468,
		"ibb-NG", 0x0469,
		"yo-NG", 0x046a,
		"quz-BO", 0x046b,
		"nso-ZA", 0x046c,
		"ba-RU", 0x046d,
		"lb-LU", 0x046e,
		"kl-GL", 0x046f,
		"ig-NG", 0x0470,
		"kr-NG", 0x0471,
		"om-ET", 0x0472,
		"ti-ET", 0x0473,
		"gn-PY", 0x0474,
		"haw-US", 0x0475,
		"la-Latn", 0x0476,
		"so-SO", 0x0477,
		"ii-CN", 0x0478,
		"pap-029", 0x0479,
		"arn-CL", 0x047a,
		"moh-CA", 0x047c,
		"br-FR", 0x047e,
		"ug-CN", 0x0480,
		"mi-NZ", 0x0481,
		"oc-FR", 0x0482,
		"co-FR", 0x0483,
		"gsw-FR", 0x0484,
		"sah-RU", 0x0485,
		"qut-GT", 0x0486,
		"rw-RW", 0x0487,
		"wo-SN", 0x0488,
		"prs-AF", 0x048c,
		"plt-MG", 0x048d,
		"zh-yue-HK", 0x048e,
		"tdd-Tale-CN", 0x048f,
		"khb-Talu-CN", 0x0490,
		"gd-GB", 0x0491,
		"ku-Arab-IQ", 0x0492,
		"quc-CO", 0x0493,
		"qps-ploc", 0x0501,
		"qps-ploca", 0x05fe,
		"ar-IQ", 0x0801,
		"ca-ES-valencia", 0x0803,
		"zh-CN", 0x0804,
		"de-CH", 0x0807,
		"en-GB", 0x0809,
		"es-MX", 0x080a,
		"fr-BE", 0x080c,
		"it-CH", 0x0810,
		"ja-Ploc-JP", 0x0811,
		"nl-BE", 0x0813,
		"nn-NO", 0x0814,
		"pt-PT", 0x0816,
		"ro-MD", 0x0818,
		"ru-MD", 0x0819,
		"sr-Latn-CS", 0x081a,
		"sv-FI", 0x081d,
		"ur-IN", 0x0820,
		"az-Cyrl-AZ", 0x082c,
		"dsb-DE", 0x082e,
		"tn-BW", 0x0832,
		"se-SE", 0x083b,
		"ga-IE", 0x083c,
		"ms-BN", 0x083e,
		"uz-Cyrl-UZ", 0x0843,
		"bn-BD", 0x0845,
		"pa-Arab-PK", 0x0846,
		"ta-LK", 0x0849,
		"mn-Mong-CN", 0x0850,
		"bo-BT", 0x0851,
		"sd-Arab-PK", 0x0859,
		"iu-Latn-CA", 0x085d,
		"tzm-Latn-DZ", 0x085f,
		"ks-Deva", 0x0860,
		"ne-IN", 0x0861,
		"ff-Latn-SN", 0x0867,
		"quz-EC", 0x086b,
		"ti-ER", 0x0873,
		"qps-plocm", 0x09ff,
		"ar-EG", 0x0c01,
		"zh-HK", 0x0c04,
		"de-AT", 0x0c07,
		"en-AU", 0x0c09,
		"es-ES", 0x0c0a,
		"fr-CA", 0x0c0c,
		"sr-Cyrl-CS", 0x0c1a,
		"se-FI", 0x0c3b,
		"mn-Mong-MN", 0x0c50,
		"tmz-MA", 0x0c5f,
		"quz-PE", 0x0c6b,
		"ar-LY", 0x1001,
		"zh-SG", 0x1004,
		"de-LU", 0x1007,
		"en-CA", 0x1009,
		"es-GT", 0x100a,
		"fr-CH", 0x100c,
		"hr-BA", 0x101a,
		"smj-NO", 0x103b,
		"tzm-Tfng-MA", 0x105f,
		"ar-DZ", 0x1401,
		"zh-MO", 0x1404,
		"de-LI", 0x1407,
		"en-NZ", 0x1409,
		"es-CR", 0x140a,
		"fr-LU", 0x140c,
		"bs-Latn-BA", 0x141a,
		"smj-SE", 0x143b,
		"ar-MA", 0x1801,
		"en-IE", 0x1809,
		"es-PA", 0x180a,
		"fr-MC", 0x180c,
		"sr-Latn-BA", 0x181a,
		"sma-NO", 0x183b,
		"ar-TN", 0x1c01,
		"en-ZA", 0x1c09,
		"es-DO", 0x1c0a,
		"sr-Cyrl-BA", 0x1c1a,
		"sma-SE", 0x1c3b,
		"ar-OM", 0x2001,
		"en-JM", 0x2009,
		"es-VE", 0x200a,
		"fr-RE", 0x200c,
		"bs-Cyrl-BA", 0x201a,
		"sms-FI", 0x203b,
		"ar-YE", 0x2401,
		"en-029", 0x2409,
		"es-CO", 0x240a,
		"fr-CD", 0x240c,
		"sr-Latn-RS", 0x241a,
		"smn-FI", 0x243b,
		"ar-SY", 0x2801,
		"en-BZ", 0x2809,
		"es-PE", 0x280a,
		"fr-SN", 0x280c,
		"sr-Cyrl-RS", 0x281a,
		"ar-JO", 0x2c01,
		"en-TT", 0x2c09,
		"es-AR", 0x2c0a,
		"fr-CM", 0x2c0c,
		"sr-Latn-ME", 0x2c1a,
		"ar-LB", 0x3001,
		"en-ZW", 0x3009,
		"es-EC", 0x300a,
		"fr-CI", 0x300c,
		"sr-Cyrl-ME", 0x301a,
		"ar-KW", 0x3401,
		"en-PH", 0x3409,
		"es-CL", 0x340a,
		"fr-ML", 0x340c,
		"ar-AE", 0x3801,
		"en-ID", 0x3809,
		"es-UY", 0x380a,
		"fr-MA", 0x380c,
		"ar-BH", 0x3c01,
		"en-HK", 0x3c09,
		"es-PY", 0x3c0a,
		"fr-HT", 0x3c0c,
		"ar-QA", 0x4001,
		"en-IN", 0x4009,
		"es-BO", 0x400a,
		"ar-Ploc-SA", 0x4401,
		"en-MY", 0x4409,
		"es-SV", 0x440a,
		"ar-145", 0x4801,
		"en-SG", 0x4809,
		"es-HN", 0x480a,
		"en-AE", 0x4c09,
		"es-NI", 0x4c0a,
		"en-BH", 0x5009,
		"es-PR", 0x500a,
		"en-EG", 0x5409,
		"es-US", 0x540a,
		"en-JO", 0x5809,
		"es-419", 0x580a,
		"en-KW", 0x5c09,
		"en-TR", 0x6009,
		"en-YE", 0x6409,
		"bs-Cyrl", 0x641a,
		"bs-Latn", 0x681a,
		"sr-Cyrl", 0x6c1a,
		"sr-Latn", 0x701a,
		"smn", 0x703b,
		"az-Cyrl", 0x742c,
		"sms", 0x743b,
		"zh", 0x7804,
		"nn", 0x7814,
		"bs", 0x781a,
		"az-Latn", 0x782c,
		"sma", 0x783b,
		"uz-Cyrl", 0x7843,
		"mn-Cyrl", 0x7850,
		"iu-Cans", 0x785d,
		"tzm-Tfng", 0x785f,
		"zh-Hant", 0x7c04,
		"nb", 0x7c14,
		"sr", 0x7c1a,
		"tg-Cyrl", 0x7c28,
		"dsb", 0x7c2e,
		"smj", 0x7c3b,
		"uz-Latn", 0x7c43,
		"pa-Arab", 0x7c46,
		"mn-Mong", 0x7c50,
		"sd-Arab", 0x7c59,
		"chr-Cher", 0x7c5c,
		"iu-Latn", 0x7c5d,
		"tzm-Latn", 0x7c5f,
		"ff-Latn", 0x7c67,
		"ha-Latn", 0x7c68,
		"ku-Arab", 0x7c92,
	};
	
	private static HashMap<String, Integer> langs = new HashMap<>();
	private static HashMap<Integer, String> ids = new HashMap<>();
	
	static {
		for (int i = 0; i < langData.length; i +=2) {
			langs.put((String)langData[i], (Integer)langData[i+1]);
			ids.put((Integer)langData[i+1], (String)langData[i]);
		}
	}
}
