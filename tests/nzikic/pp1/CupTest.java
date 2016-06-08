package nzikic.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import rs.etf.pp1.mj.runtime.Code;

public class CupTest {

	public static void main(String[] args) throws Exception 
	{
		Logger log  = Logger.getLogger(CupTest.class);
		
		Reader br = null;
		try
		{
		    File sourceCode = new File(args[0]);
		    log.info("Compiling souce file : " + sourceCode.getAbsolutePath());
		    
		    br = new BufferedReader(new FileReader(sourceCode));
		    Yylex lexer = new Yylex(br);
		    MJParser parser = new MJParser(lexer);
		    parser.parse();   // pocetak parsiranja
		    
		    parser.dump();
		    
		    // Generisanje koda
		    parser.report_info("Code generation...", null);
		    if (parser.bErrorDetected)
		    {
		        parser.report_error("---- BUILD FAILED - neuspesno parsiranje", null);
		    }
		    else
		    {
		        File outputFile;		        
		        if (args.length == 1)
		        {
		            Path path = Paths.get(args[0]);
		            String  sFile= path.getFileName().toString();
		            int dotPos = sFile.lastIndexOf('.');
		            String sFileName = (dotPos != -1) ? sFile.substring(0, dotPos) : sFile; 
		            outputFile = new File("out/" + sFileName + ".obj");
		        }
		        else
		        {
		            outputFile = new File(args[1]);
		        }
		        
		        if (outputFile.exists())
		        {
		            outputFile.delete();
		        }

		        Code.write(new FileOutputStream(outputFile));
                parser.report_info("---- BUILD SUCCEEDED - uspesno parsiranje", null);		        
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
		            e1.printStackTrace();
		        }
		    }
		}
	}
	
}
