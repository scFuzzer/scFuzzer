package edu.cmu.sv.kelinci.instrumentorjava;

import java.util.ArrayList;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class InstrumentorJava  {

	// public static ClassLoader classloader;

    public static void main(String[] args) throws IOException {
		
		// get class loader
		// classloader = Thread.currentThread().getContextClassLoader();
		
		// parse command line arguments
		JavaOptions options = JavaOptions.v();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			parser.printUsage(System.err);
			return;
		}

		Set<String> inputJavaFiles = JavaOptions.v().getInput();
		String rawInput = JavaOptions.v().getRawInput();


		for(String jf : inputJavaFiles){
			System.out.println("Instrumenting JavaFile " + jf);
			String inputPath = rawInput + "/" + jf;

			int num = 0;
			int tempchar;
			FileReader fr = new FileReader(inputPath);
			String str = "";
			while((tempchar = fr.read()) != -1) {
				if (((char) tempchar) != '\r') {
					str += (char) tempchar;
				}
				// str = new String(buf,0, num);
			}
			fr.close();
			int varNum = 0;


			ArrayList<Integer> insertPosition = new ArrayList<Integer>();
			String matchStr = "Integer.parseInt";
			Matcher m = Pattern.compile(matchStr).matcher(str);
			while (m.find()) {
				int k = m.end();
				while(str.charAt(k) != ';'){
					k++;
				}
				insertPosition.add(k+1);
			}


			String matchStr0 = "int\\s[A-Za-z0-9]+\\s=\\sInteger";
			Matcher m0 = Pattern.compile(matchStr0).matcher(str);
			int count = 0;
			String newStr = str;
			while (m0.find()) {
				String allString = m0.group();
				String nameOfValue = "";
				int k = 4;
				while(allString.charAt(k) != ' ' && allString.charAt(k) != '='){
					nameOfValue += allString.charAt(k);
					k++;
				}
				String oldStr = newStr;
				newStr = newStr.substring(0, insertPosition.get(count)) + "\n\t\tif(" + nameOfValue +" < 0){\n" +"\t\t\t\tthrow new RuntimeException(\"Uncheck the incoming parameters\");\n" +
						"\t\t}" + newStr.substring(insertPosition.get(count));
				for(int i = count + 1; i < insertPosition.size(); i++){
					insertPosition.set(i, insertPosition.get(i) + (newStr.length() - oldStr.length())) ;
				}
				count++;
			}


			String matchStrOfOverflow = "-=|\\+=";
			String newStrOfOverflow = newStr;
			Matcher mOfOverflow = Pattern.compile(matchStrOfOverflow).matcher(newStrOfOverflow);
			count = 0;
			while (mOfOverflow.find()) {
				int k = mOfOverflow.start() - 1 + count;
				String nameOfResult = "";
				String nameOfAddOrMinus = "";
				while(newStrOfOverflow.charAt(k) != ';' && newStrOfOverflow.charAt(k) != '}'){
					k--;
				}
				int beginOfSubstring = k + 1;

				while(newStrOfOverflow.charAt(k) != '-' && newStrOfOverflow.charAt(k) != '+'){
					if (!Character.isLetterOrDigit(newStrOfOverflow.charAt(k))){
						k++;
						continue;
					}
					nameOfResult += newStrOfOverflow.charAt(k);
					k++;
				}
				k = mOfOverflow.end() + count;
				while(newStrOfOverflow.charAt(k) != ';'){
					if (newStrOfOverflow.charAt(k) == ' ') {
						k++;
						continue;
					}
					nameOfAddOrMinus += newStrOfOverflow.charAt(k);
					k++;
				}
				String oldStr = newStrOfOverflow;
				if(mOfOverflow.group().equals("+=")){
					newStrOfOverflow = newStrOfOverflow.substring(0, beginOfSubstring) +
							"\n\t\tlong var" + varNum + " = (long)" + nameOfResult + " + " + nameOfAddOrMinus +
							";\n\t\tif( var" + varNum + " > Integer.MAX_VALUE){" +
							"\n\t\t\tthrow new RuntimeException(\"Integer Overflow\");" +
							"\n\t\t}"
							+ newStrOfOverflow.substring(beginOfSubstring);
					varNum++;
				}
				else{
					newStrOfOverflow = newStrOfOverflow.substring(0, beginOfSubstring) +
							"\n\t\tlong var" + varNum + " = (long)" + nameOfResult + " - " + nameOfAddOrMinus +
							";\n\t\tif( var" + varNum + " < Integer.MIN_VALUE){" +
							"\n\t\t\tthrow new RuntimeException(\"Integer Underflow\");" +
							"\n\t\t}"
							+ newStrOfOverflow.substring(beginOfSubstring);
					varNum++;
				}
				count += newStrOfOverflow.length() - oldStr.length();
			}



			String matchStrOfAnotherOverflow = "[A-Za-z0-9]+\\s*=\\s*[A-Za-z0-9]+\\s*(-|\\+)\\s*[A-Za-z0-9]+";
			String newStrOfAnotherOverflow = newStrOfOverflow;
			Matcher mOfAnotherOverflow = Pattern.compile(matchStrOfAnotherOverflow).matcher(newStrOfAnotherOverflow);
			count = 0;
			while (mOfAnotherOverflow.find()) {
				boolean existInt = false;
				int k = mOfAnotherOverflow.start() - 1 + count;
				String nameOfResult = "";
				String nameOfAddOrMinus1 = "";
				String nameOfAddOrMinus2 = "";
				while(newStrOfAnotherOverflow.charAt(k) != ';' && newStrOfAnotherOverflow.charAt(k) != '}'){
					k--;
				}
				int beginOfSubstring = k + 1;

				String findInt = "";
				while(newStrOfAnotherOverflow.charAt(k) != 't'){
					if (!Character.isLetterOrDigit(newStrOfAnotherOverflow.charAt(k))){
						k++;
						continue;
					}
					findInt += newStrOfAnotherOverflow.charAt(k);
					k++;
				}
				findInt += newStrOfAnotherOverflow.charAt(k);
				if (findInt.equals("int")){
					k++;
					existInt = true;
				}
				else k = beginOfSubstring - 1;

				while(newStrOfAnotherOverflow.charAt(k) != '='){
					if (!Character.isLetterOrDigit(newStrOfAnotherOverflow.charAt(k))){
						k++;
						continue;
					}
					nameOfResult += newStrOfAnotherOverflow.charAt(k);
					k++;
				}

				while(newStrOfAnotherOverflow.charAt(k) != '-' && newStrOfAnotherOverflow.charAt(k) != '+'){
					if (!Character.isLetterOrDigit(newStrOfAnotherOverflow.charAt(k))){
						k++;
						continue;
					}
					nameOfAddOrMinus1 += newStrOfAnotherOverflow.charAt(k);
					k++;
				}

				while(newStrOfAnotherOverflow.charAt(k) != ';'){
					if (!Character.isLetterOrDigit(newStrOfAnotherOverflow.charAt(k))) {
						k++;
						continue;
					}
					nameOfAddOrMinus2 += newStrOfAnotherOverflow.charAt(k);
					k++;
				}
				String oldStr = newStrOfAnotherOverflow;
				if(Pattern.compile("[A-Za-z0-9]+\\s*=\\s*[A-Za-z0-9]+\\s*\\+\\s*[A-Za-z0-9]+").matcher(mOfAnotherOverflow.group()).find()){
					if(existInt){
						newStrOfAnotherOverflow = newStrOfAnotherOverflow.substring(0, beginOfSubstring) +
								"\n\t\tlong var" + varNum + " = (long)" + nameOfAddOrMinus1 + " + " + nameOfAddOrMinus2 +
								";\n\t\tif( var" + varNum + " > Integer.MAX_VALUE){" +
								"\n\t\t\tthrow new RuntimeException(\"Integer Overflow\");" +
								"\n\t\t}"
								+ newStrOfAnotherOverflow.substring(beginOfSubstring);
					}else{
						newStrOfAnotherOverflow = newStrOfAnotherOverflow.substring(0, beginOfSubstring) +
								"\n\t\tif(" + nameOfResult + " instanceof Integer){" +
								"\n\t\t\tlong var" + varNum + " = (long)" + nameOfAddOrMinus1 + " + " + nameOfAddOrMinus2 +
								";\n\t\t\tif( var" + varNum + " > Integer.MAX_VALUE){" +
								"\n\t\t\t\tthrow new RuntimeException(\"Integer Overflow\");" +
								"\n\t\t\t}" +
								"\n\t\t}"
								+ newStrOfAnotherOverflow.substring(beginOfSubstring);
					}
					varNum++;
				}
				else{
					if(existInt) {
						newStrOfAnotherOverflow = newStrOfAnotherOverflow.substring(0, beginOfSubstring) +
								"\n\t\tlong var" + varNum + " = (long)" + nameOfAddOrMinus1 + " - " + nameOfAddOrMinus2 +
								";\n\t\tif( var" + varNum + " < Integer.MIN_VALUE){" +
								"\n\t\t\tthrow new RuntimeException(\"Integer Underflow\");" +
								"\n\t\t}"
								+ newStrOfAnotherOverflow.substring(beginOfSubstring);
					}
					else{
						newStrOfAnotherOverflow = newStrOfAnotherOverflow.substring(0, beginOfSubstring) +
								"\n\t\tif(" + nameOfResult + " instanceof Integer){" +
								"\n\t\t\tlong var" + varNum + " = (long)" + nameOfAddOrMinus1 + " - " + nameOfAddOrMinus2 +
								";\n\t\t\tif( var" + varNum + " < Integer.MIN_VALUE){" +
								"\n\t\t\t\tthrow new RuntimeException(\"Integer Underflow\");" +
								"\n\t\t\t}" +
								"\n\t\t}"
								+ newStrOfAnotherOverflow.substring(beginOfSubstring);
					}
					varNum++;
				}
				count += newStrOfAnotherOverflow.length() - oldStr.length();
			}

			// File file = new File("out/out.java");
			// Writer out = new FileWriter(file);
			// out.write(newStrOfAnotherOverflow);
			// out.close();

			writeJavaFile(jf, newStrOfAnotherOverflow);
		}
    }

	private static void writeJavaFile(String jf, String outputFile) {
		String path = JavaOptions.v().getOutput().endsWith("/") ? 
				JavaOptions.v().getOutput() + jf : 
				JavaOptions.v().getOutput() + "/" + jf;
		Path out = Paths.get(path);
		try {
			File file = new File(path);

			File fileParent = file.getParentFile();

			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			Writer outWriter = new FileWriter(file);
			outWriter.write(outputFile);
			outWriter.close();
			
			// Files.createDirectories(out.getParent());
			// Files.write(out, outputFile);
			System.out.println("File written: " + path);
		} catch (IOException e) {
			System.err.println("Error writing to file: " + path);
			e.printStackTrace();
		}
	}
}
