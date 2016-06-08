package nzikic.pp1;

import java_cup.runtime.Symbol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import nzikic.pp1.util.Log4JUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LexerTest {
	
	static 
	{
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main (String args[]) throws IOException
	{
		Logger log = Logger.getLogger(LexerTest.class);
		Reader br = null;
		
		try 
		{
			File sourceCode = new File("tests/mjtest.mj");
			log.info("\nCompiling source file: " + sourceCode.getAbsolutePath());
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			Symbol currToken = null;
			while ( (currToken = lexer.next_token()).sym != sym.EOF ) 
			{
				if (currToken != null) 
				{
					log.info(currToken.toString() + " " + currToken.value.toString());
				}
			}
		} 
		finally 
		{
			if (br != null)
			{
				try 
				{
					br.close();
				} 
				catch (IOException e1) 
				{
					log.error(e1.getMessage(), e1);
				}
			}
		}	
	}	
}
