package sepses.jsontordf;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextSplitter {
		

	
	public String[] splitTextByLine(String text) {
		
		String[] splitText = text.split("\r\n");
		return splitText;
	}
	
    public String findTextByRegex(String string, String reg) {
    	String ResultString = null;
    	try {
    	    Pattern regex = Pattern.compile(reg, Pattern.DOTALL);
    	    Matcher regexMatcher = regex.matcher(string);
    	    if (regexMatcher.find()) {
    	        ResultString = regexMatcher.group(1);
    	    } 
    	} catch (PatternSyntaxException ex) {
    	    // Syntax error in the regular expression
    	}
		
    	return ResultString;
    }
	
	
	public String[] splitTextByColon(String text) {
		String[] splitText = text.split("\\w+:\\w+");
		return splitText;
	}
	
	public String generatePrefix(String prefix) {
		return "prefix : "+prefix+"\r\n";
	}
	
	public String generateGlobalSelectQuery() {
		return "SELECT * WHERE {\r\n";
	}
	public String generateSelectQuery() {
		return "SELECT * WHERE {\r\n";
	}
	
	public String generateSPOTriple() {
		return("     ?s ?p ?o");
		
	}
	
	public String getObjectType(String string) {
		String[] st = string.split(":");
		return st[0];			
	}
	public String getActionType(String string) {
		String[] st = string.split(":");
		return st[1];			
	}
	
	public String getObjectName(String string) {
		String[] st = string.split(" = ");
		return st[0];
				
	}
	
	public String parseFilterTripleByObjectName(String objectName, String string){
		//System.out.println(objectName);
		//System.exit(0);
		String triples="";
		if(string.contains("==")) {
			String[] st = string.split("==");
			//System.out.println(st[0]);
//			System.exit(0);
			if(st[0].contains(".")) {

				String[] st2 = st[0].split("\\.");
				
				//System.exit(0);
				if(st2[0].trim().equals(objectName)) {
					//System.exit(0);
					triples = "     "+triples+"?"+objectName+" :"+st2[1]+" ?"+objectName+"_"+st2[1]+".\r\n";
					//System.out.println(triples);
//					System.exit(0);
				}
			}
			
			if(st[1].contains(".")) {
				//System.out.println(st[1]);
				//System.exit(0);
				String[] st2 = st[1].split("\\.");
				if(st2[0].trim().equals(objectName)) {
					//System.out.println(objectName);
					//System.exit(0);
					triples = "     "+triples+"?"+objectName+" :"+st2[1]+" ?"+objectName+"_"+st2[1]+".\r\n";
				}
			}
		}
			
		return  triples;
		
		
	}
	
	public String generateObjectNameTriple(String objectName) {
		String objectTriple = "     ?"+objectName+" :hasObjectName :"+objectName+".\r\n";
		return objectTriple;
		
		
	}
	
	public String generateObjectActionTriple(String object, String action, String objectName) {
		String objectTriple = "     ?"+objectName+" a :"+object+".\r\n";
		String actionType = "     ?"+objectName+" :hasAction :"+action+".\r\n";
		
		return objectTriple+actionType;
	}
	public String generateFieldTriple(String string, String objectName) {
		String triple = "";
		if(string.contains("==")) {
			String[] st = string.split("==");
			triple = "     ?"+objectName+" :"+st[0].trim()+" ?"+objectName+"_"+st[0].trim()+".\r\n";
			 
			
		}else if(string.contains("match")) {
			String[] st = string.split("match");
			triple = "     ?"+objectName+" :"+st[0].trim()+" ?"+objectName+"_"+st[0].trim()+".\r\n";
			 
			
		} else if (string.contains("!=")) {
			String[] st = string.split("!=");
			triple = "     ?"+objectName+" :"+st[0].trim()+" ?"+objectName+"_"+st[0].trim()+".\r\n";
			
		}
		return  triple;
		
		
	}
	
	public String generateFilter(String string, String objectName) {
		String filter = "";
		if(string.contains("==")) {
			String[] st = string.split("==");
			filter="     FILTER regex(str(?"+objectName+"_"+st[0].trim()+"), "+st[1].trim()+").\r\n";
			
		} else if(string.contains("match")) {
			String[] st = string.split("match");
			
			filter="     FILTER regex(str(?"+objectName+"_"+st[0].trim()+"), "+st[1]+").\r\n";
			 
			
		}else if (string.contains("!=")) {
			String[] st = string.split("!=");
			filter="     FILTER NOT EXISTS { FILTER regex(str(?"+objectName+"_"+st[0].trim()+"), "+st[1].trim()+")}.\r\n";
			
		}
		return  filter;
		
		
	}
	
	public String generateFilterJoin(String string) {
		String filter = "";
		if(string.contains("==")) {
			String[] st = string.split("==");
			
			String termFilter0 = st[0].trim().replaceAll("\\.", "_");
			String termFilter1 = st[1].trim().replaceAll("\\.", "_");
			//System.out.print(termFilter0);
			//System.exit(0);
			
			filter="     FILTER (?"+termFilter0+" = ?"+termFilter1+").\r\n";
			
		} else if (string.contains("!=")) {
			String[] st = string.split("!=");
			String termFilter0 = st[0].trim().replaceAll(".", "_");
			String termFilter1 = st[1].trim().replaceAll(".", "_");
			
			filter="     FILTER (?"+termFilter0+" != "+termFilter1+").\r\n";			
		}
		return  filter;
		
		
	}

}
