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
		paramInputToSearchEngineInputMap.put("database_file", "fasta_file"); 			     // input from SE
		paramInputToSearchEngineInputMap.put("enzyme_name", "enzyme_name");				     // input from SE
		paramInputToSearchEngineInputMap.put("missed_cleavage_no","missed_cleavages");	     // input from SE	
		paramInputToSearchEngineInputMap.put("fragment_search_plus", "product_tolerance");   // input from SE
		paramInputToSearchEngineInputMap.put("fragment_search_minus", "product_tolerance");  // input from SE
		paramInputToSearchEngineInputMap.put("parent_search_plus", "precursor_tolerance");   // input from SE
		paramInputToSearchEngineInputMap.put("parent_search_minus", "precursor_tolerance");  // input from SE
		paramInputToSearchEngineInputMap.put("modifications", " ");							 // input from SE - Needs to be resolved 
		paramInputToSearchEngineInputMap.put("user_name", "user_name");								 // input from param/parser input		
		paramInputToSearchEngineInputMap.put("decoy_regex", "decoy_regex");							 // input from param/parser input	
		paramInputToSearchEngineInputMap.put("decoy_ratio", "decoy_ratio");							 // input from param/parser input	
		paramInputToSearchEngineInputMap.put("rank_threshold", "rank_threshold");						 // input from param/parser input	
	}
	
	public HashMap<String, String> getTheMappingForParamToSearchEngineFile(){
		return paramInputToSearchEngineInputMap;
	}
}
