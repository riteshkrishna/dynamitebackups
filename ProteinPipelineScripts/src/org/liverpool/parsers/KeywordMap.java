package org.liverpool.parsers;

import java.util.HashMap;

/**
 * 
 * The map which defines association between the ParamKeywords and OmssaKeywords
 * defined in resources/paramKeywords.txt and omssaKeywords.txt. The unmapped keys
 * have empty value
 * 
 * @author riteshk
 *
 */

public class KeywordMap {

	public HashMap <String, String> paramInputToSearchEngineInputMap;
	
	public KeywordMap() {
		
		paramInputToSearchEngineInputMap = new HashMap<String, String>();
		paramInputToSearchEngineInputMap.put("database_file", "fasta_file");
		paramInputToSearchEngineInputMap.put("enzyme_name", "enzyme_name");
		paramInputToSearchEngineInputMap.put("missed_cleavage_no","missed_cleavages");
		paramInputToSearchEngineInputMap.put("fragment_search_plus", "product_tolerance");
		paramInputToSearchEngineInputMap.put("fragment_search_minus", "product_tolerance");
		paramInputToSearchEngineInputMap.put("parent_search_plus", "precursor_tolerance");
		paramInputToSearchEngineInputMap.put("parent_search_minus", "precursor_tolerance");
		paramInputToSearchEngineInputMap.put("modifications", " ");
		paramInputToSearchEngineInputMap.put("user_name", " ");
		paramInputToSearchEngineInputMap.put("decoy_regex", " ");
		paramInputToSearchEngineInputMap.put("decoy_ratio", " ");
		paramInputToSearchEngineInputMap.put("rank_threshold", " ");
	}
	
	public HashMap<String, String> getTheMappingForParamToSearchEngineFile(){
		return paramInputToSearchEngineInputMap;
	}
}
