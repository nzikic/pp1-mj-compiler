package nzikic.pp1.util;

import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Codegen 
{ 
    private static boolean m_bError = false;    // error tokom semanticke analize
    private static boolean m_bMain = false;     // u main funkciji smo
    private static boolean m_bArrayDesignator = false;  // da li je designator tipa array
    
    /** Mapira String konstantu i sve const objekte koji koriste tau String konstantu */
    private static Map<String, LinkedList<Obj>> m_mapStringToObjectList = new HashMap<String, LinkedList<Obj>>();
    
    /** Lista objekata klasnih tipova */
    private static List<Obj> m_listClassTypeObjects = new LinkedList<Obj>();
    /** Mapira imena klasa na vftp */
    private static Map<String, Integer> m_mapClassNameToVFTP = new HashMap<String, Integer>();
    /** Mapira ime objekta na adresu koju treba fixovati vftp-om tog objekta kada vftp nije jos poznat */
    private static Map<String, LinkedList<Integer>> m_mapClassObjectToVFTPAddrListFixup = new HashMap<String, LinkedList<Integer>>();
    
    /** Stek adresa za popravku u if uslovu. Adrese treba da skoce napred na else granu ili na kraj naredbe za unmatched. */
    private static Stack<Integer> m_stackFwdJmpIfStatementFalse = new Stack<Integer>();
    /** Stek adresa za popravku u else grani. Adrese treba da skoce napred na kraj else grane */
    private static Stack<Integer> m_stackFwdJmpToEndOfElseBranch = new Stack<Integer>();
    /** Stek adresa pocetka evaluacije while uslova. Skace se na nju na kraju iteracije */
    private static Stack<Integer> m_stackWhileCondEvalAdr = new Stack<Integer>();
    /** Stek adresa za popravku za skok na kraj while petlje (ako je uslov false); fwd jump */
    private static Stack<Integer> m_stackWhileEndAdrFixup = new Stack<Integer>();
    /** Stek lista adresa za popravku za skok na kraj while petlje. Moze biti vise break naredbi u jednoj while petlji */
    private static Stack<LinkedList<Integer>> m_stackBreakAdrFixup = new Stack<LinkedList<Integer>>();
    
    
    public static void setError()
    {
        Codegen.m_bError = false;
    }
    
    public static boolean getError()
    {
        return Codegen.m_bError;
    }
    
    public static void setIsArrayDesignator(boolean isArray)
    {
        m_bArrayDesignator = isArray;
    }
    
    public static void addClassTypeObject(Obj cls)
    {
        m_listClassTypeObjects.add(cls);
    }
    
    /**
     * Dodeljuje vrednost izraza sa eStacka designatoru
     * @param designator - destenation
     */
    public static void designatorAssignExpr(Obj dst)
    {
        if (m_bError) return;
        if (dst == null || dst == Tab.noObj) return;
        // sa eStack u dst
        Code.store(dst);
    }
    
    /**
     * ucitavanje instance klase cijem polju treba pristupiti na eStack
     * @param designator - objekat klase
     */
    public static void designatorDotIdent(Obj designator)
    {
        //System.out.println("designatorDotIdent" + designator.getName());
        if (m_bError) return;
        Code.load(designator);  // ucitaj vrednost na eStack
    }
    
    /**
     * Poziva se u DesignatorTrunk ::= DesignatorArrayExpr:dae za nizove
     * @param stackDesignator - stackDesignator iz parser klase; [0] je var/const/fld, [1] je method/function
     * @throws Exception
     */
    public static void arrayDesignator(Stack<Obj[]> stackDesignator) throws Exception
    {
        if (m_bError) return;
        if (m_bArrayDesignator && stackDesignator.isEmpty())
        {
            m_bError = true;
            throw new Exception("Codegen::arrayDesignator - stackDesignator is empty!");
        }
        
        if (m_bArrayDesignator)
        {
            Code.load(stackDesignator.peek()[0]);   // ucitaj vrednost na eStack
            m_bArrayDesignator = false;
        }
    }
    
    /**
     * Generise kod koji ce da izgenerise string na heapu
     * @param string - sadrzaj stringa ide na heap
     */
    public static void factorStrconst(String string)
    {
        if (m_bError) return;
        
        Code.loadConst(string.length());    // na expr stack duzina niza
        Code.put(Code.newarray); // novi niz
        Code.put(0);    // tipa char
        // ostaje na eStack adresa na heapu
        
        // neimenovana funkcija za kopiranje niza
        Code.put(Code.enter);
        Code.put(0); Code.put(1); // 0 args, 1 local
        Obj stringAddress = new Obj(Obj.Var, "", Tab.intType, 0, 1); // objekat adrese stringa, level = 1 (local var)
        Code.store(stringAddress);  // smesti verdnost sa eStack u local var (ta vrednost je adresa niza na heap-u)
        for (int i = 0; i < string.length(); ++i)
        {
            // TODO optimize - smotati kod neimenovane funkcije u petlju, ovako je razmotana 
            Code.load(stringAddress);           // eStack heap offser (adr of array)
            Code.loadConst(i);                  // eStack array offset
            Code.loadConst(string.charAt(i));   // eStack value
            Code.put(Code.bastore);  // bastore  ..., adr, i, val
        }
        Code.load(stringAddress);
        Code.put(Code.exit);
    }
    
    /**
     * Generisanje koda u smeni Factor ::= Designator
     * @param designator - designator koji moze biti Var, Const, Fld
     */
    public static void factor__Designator(Obj designator)
    {
        if (m_bError) return;
        
        try 
        {
            if (designator == null)        throw new Exception("Designator je null!");
            if (designator == Tab.noObj)   throw new Exception("Designator je Tab.noObj");
            
            if (designator.getKind() == Obj.Con && designator.getType().getKind() == Struct.Array)  // ConstDecl STRCONST
            {
                Code.load(new Obj(Obj.Var, "", designator.getType(), designator.getAdr(), 0));  // adresa (u .rodata) na eStack
            }
            else    // sve drugo
            {
                Code.load(designator); // designator na eStack
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Poziva se u smeni Factor ::= Designator OptionalFunctionCall;
     * @param designator - objekat metode/funkcije
     * @param beginCodeAdr - adresa pocetka metode (za funkcije se ne koristi)
     * @param endCodeAdr - adresa kraja metode (za funkcije se ne koristi)
     */
    public static void factor__Designator_Function(Obj designator, int beginCodeAdr, int endCodeAdr)
    {
        if (m_bError) return;
        
        if (designator != null && designator != Tab.noObj)
        {
            // funkcija
            if (designator.getName().endsWith("@Func"))
            {
                int callAdr = designator.getAdr() - Code.pc;
                Code.put(Code.call);
                Code.put2(callAdr);
            }
            else if (designator.getName().endsWith("@Meth"))
            {
                Obj vftp = new Obj(Obj.Fld, "", Tab.intType, 0, 1);
                String methodName = designator.getName().split("@")[0];
                // pristup designatoru (ako je neki krupniji izraz) - ostaje adresa objekta na heapu
                for (int i = beginCodeAdr; i < endCodeAdr; ++i)
                {
                    Code.buf[Code.pc] = Code.buf[i];
                    Code.pc++;
                }
                Code.load(vftp); // getfield_0
                Code.put(Code.invokevirtual);
                for (int i = 0; i < methodName.length(); ++i)
                {   // put method name
                    Code.put4(methodName.charAt(i));
                }
                // method name terminator
                Code.put4(-1);
            }
            else if (designator.getName().equals("len"))
            {
                Code.put(Code.arraylength);
            }
            // else if (ord || chr) {}
            // treba da rade same po sebi, nemaju telo i nece nista da urade,
            // jer je char zapravo int, a povratna vrednost metoda ce zadovoljiti semantiku da se lepo pokupi
            //else error
        }
        // else error
    }
    
    /**
     * Poziva se na kraju smene StmtFuncCall ::= LPAREN:lp ActualParamsRPAREN:ap;
     * @param designator - objekat metode/funkcije
     * @param beginCodeAdr - adresa pocetka metode (za funkcije se ne koristi)
     * @param endCodeAdr - adresa kraja metode (za funkcije se ne koristi)
     */
    public static void callStamement(Obj designator, int beginCodeAdr, int endCodeAdr)
    {
        if (m_bError) return;
        Codegen.factor__Designator_Function(designator, beginCodeAdr, endCodeAdr);
        // ako nije void, skini sa steka izraz jer nema ko da ga uhvati
        if (designator.getType() != Tab.noType)
        {
            Code.put(Code.pop);
        }
    }
    
    public static void addopTerm__Addop_Term(int addop, Struct termType)
    {
        if (m_bError) return;
        
        if (termType.compatibleWith(Tab.intType))
        {
            // sabiranje brojeva
            Code.put(addop);
        }
        else
        {
            // sabiranje stringova (konkatenacija)
            // adrese stringova su na eStack (adresa drugog na vrhu, pa ispod njega adresa prvog
            Code.put(Code.enter);
            Code.put(0); Code.put(7);   // 0 params, 7 local vars
            Obj adrFirst    = new Obj(Obj.Var, "", Tab.intType, 0, 1);
            Obj adrSecond   = new Obj(Obj.Var, "", Tab.intType, 1, 1);
            Obj lenFirst    = new Obj(Obj.Var, "", Tab.intType, 2, 1);
            Obj lenSecond   = new Obj(Obj.Var, "", Tab.intType, 3, 1);
            Obj index       = new Obj(Obj.Var, "", Tab.intType, 4, 1);
            Obj adrTarget = new Obj(Obj.Var, "", Tab.intType, 5, 1);
            
            Code.store(adrSecond);  // adresa u 1. local
            Code.store(adrFirst);   // adresa u 0. local
            // duzina 1. stringa
            Code.load(adrFirst); // adr prvog nazad na eStack
            Code.put(Code.arraylength); // duzina niza na eStack
            Code.store(lenFirst); // sacuvaj u local
            // duzina 2. stringa
            Code.load(adrSecond);   // analogno prethodnom
            Code.put(Code.arraylength);
            Code.store(lenSecond);
            // indeks = 0
            Code.loadConst(0);
            Code.store(index);
            // len = lenFirst + lenSecond na eStack
            Code.load(lenFirst);  
            Code.load(lenSecond);
            Code.put(Code.add);
            // adrTarget = new byte[len]
            Code.put(Code.newarray);  
            Code.put(0);  
            Code.store(adrTarget);
            
            // while (index != lenFirst)
            int loop = Code.pc;     
            Code.load(lenFirst);
            Code.load(index);
            Code.putFalseJump(Code.inverse[Code.eq], 0);    // ako su jednaki skoci napred
            int adrToFix = Code.pc - 2;
            // {
                Code.load(adrTarget);   // ptr dst elem
                Code.load(index);
                Code.load(adrFirst);    // ptr src elem
                Code.load(index);
                Code.put(Code.baload);  // load src
                Code.put(Code.bastore); // store to dst
                // ++index
                Code.load(index);
                Code.loadConst(1);
                Code.put(Code.add);
                Code.store(index);
                // jump back to LOOP
                Code.putJump(loop);
                // }    //end of while
            Code.fixup(adrToFix); 
            
            // src index = 0
            Obj index2 = new Obj(Obj.Var, "", Tab.intType, 6, 1);
            Code.loadConst(0);
            Code.store(index2);
            
            // while (index2 != lenFirst)
            loop = Code.pc;
            Code.load(lenSecond);
            Code.load(index2);
            Code.putFalseJump(Code.inverse[Code.eq], 0);    // ako su jednaki skoci napred
            adrToFix = Code.pc - 2;
            // {
                Code.load(adrTarget);
                Code.load(index);
                Code.load(adrSecond);
                Code.load(index2);
                Code.put(Code.baload);
                Code.put(Code.bastore); // adrTarget[index] = adrSecond[index2];
                // ++ index
                Code.load(index);
                Code.loadConst(1);
                Code.put(Code.add);
                Code.store(index);
                // ++index2
                Code.load(index2);
                Code.loadConst(1);
                Code.put(Code.add);
                Code.store(index2);
                Code.putJump(loop);
            // }
            Code.fixup(adrToFix);
            
            Code.load(adrTarget);   // ostavljamo adresu target stringa na sStack
            Code.put(Code.exit);
        }
    }
    
    /**
     * Uzima zraz sa eStack-a, mnozi ga sa -1 i vraca na eStack.
     */
    public static void multiplyByMinusOne()
    {
        if (m_bError) return;
        
        Code.loadConst(-1); // Code.put(Code.const_m1);
        Code.put(Code.mul);
    }
    
    private static LinkedList<Obj> getClassMethods(Obj class_)
    {
        LinkedList<Obj> methods = new LinkedList<Obj>();
        Iterator<Obj> it = class_.getType().getMembers().iterator();
        while (it.hasNext())
        {
            Obj member = it.next();
            if (member.getKind() == Obj.Meth)
            {
                methods.add(member);
            }
        }
        
        return methods;
    }
    
    private static void fixupUnknownVFTPOccurences()
    {
        Iterator<String> it = m_mapClassObjectToVFTPAddrListFixup.keySet().iterator();
        while (it.hasNext())
        {
            String className = it.next();
            Iterator<Integer> itAddrToFix = m_mapClassObjectToVFTPAddrListFixup.get(className).iterator();
            while (itAddrToFix.hasNext())
            {
                int addrToFix = itAddrToFix.next();
                int pc = Code.pc;
                Code.put(m_mapClassNameToVFTP.get(className));
                Code.pc = pc;
            }
        }
    }
    
    private static void initVFTs()
    {
        // smestamo na kraj static data
        Obj vftAddress = new Obj(Obj.Var, "", Tab.intType, Code.dataSize, 0);
        
        LinkedList<Obj> methodNames;
        Obj classObject = Tab.noObj;
        // prolaz kroz listu klasnih tipiva
        Iterator<Obj> itClass = m_listClassTypeObjects.iterator();
        while (itClass.hasNext())
        {
            classObject = itClass.next();
            m_mapClassNameToVFTP.put(classObject.getName(), vftAddress.getAdr());
            methodNames = Codegen.getClassMethods(classObject);
            // prolaz kroz listu metoda klase
            Iterator<Obj> itMethod = methodNames.iterator();
            while(itMethod.hasNext())
            {
                Obj method = itMethod.next();
                // dodaj ime u tabelu
                String name = method.getName().split("@")[0];
                for (int i = 0; i < name.length(); ++i)
                {
                    Code.loadConst(name.charAt(i));
                    Code.store(vftAddress);
                    vftAddress.setAdr(++Code.dataSize);
                }
                // terminate name
                Code.loadConst(-1);
                Code.store(vftAddress);
                vftAddress.setAdr(++Code.dataSize);
                // method address
                Code.loadConst(method.getAdr());
                Code.store(vftAddress);
                vftAddress.setAdr(++Code.dataSize);
                // ako nema vise metoda, stavi kraj VFT
                if (itMethod.hasNext() == false)
                {
                    Code.loadConst(-2);
                    Code.store(vftAddress);
                    vftAddress.setAdr(++Code.dataSize);
                }
            }
        }
    }
    
    /**
     * poziva se u smeni MetRetType_ident ::= MetRetType:mrt IDENT:id
     * @param id - ime metode (bez ukrasa)
     * @param method - objektni cvor metode koji se stavlja u tabelu simbola
     */
    public static void setMethodCodeAdress(String id, Obj method)
    {
        if (m_bError) return;
        
        method.setAdr(Code.pc);
        if (id.equals("main"))
        {
            Code.mainPc = Code.pc;
            m_bMain = true;
        }
    }
    
    /** Setuje adresu metode u kodu i postavlja aktivacioni zapis */
    public static void methodEnter(Obj method)
    {
        if (m_bError) return;
        if (method == null || method == Tab.noObj) return;

        method.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(method.getLevel()); Code.put(Tab.currentScope().getnVars());
    }
    
    /**
     * Poziva se kada zapocne definicija tela funkcije (posle formalnih parametara i vardecllist)
     * Ako je ovo main funkcija, dodatno inicijalizuje virtuelne tabele i .rodata sekciju
     * @param currentMethodDeclarationObj
     * @param isMainFunction
     */
    public static void functionDeclBegin(Obj func, boolean isMain)
    {
        if (m_bError) return;
        
        if (isMain)
        {
            Codegen.initVFTs();
            Codegen.fixupUnknownVFTPOccurences();
        }
        Code.put(Code.enter);
        Code.put(func.getLevel()); Code.put(Tab.currentScope().getnVars());
        if (m_bMain)
        {
            m_bMain = false;
            _rodataInit();
        }
    }
    
    /**
     * Alocira .rodata sekciju - ona je zapravo u na kraju static sekcije i sadrzi pokazivace na stringove na heapu
     * Setuje objektne cvorove konstanti tipa string da pokazuju na 
     */
    private static void _rodataInit()
    {
        Set<String> strings = m_mapStringToObjectList.keySet();
        Iterator<String> itStr = strings.iterator();
        while (itStr.hasNext())
        {
            String strconst = itStr.next();
            // alokacija prostora za string na heap
            Code.loadConst(strconst.length());
            Code.put(Code.newarray);
            Code.put(0);
            // adresa alociranog niza u static data (.rodata)
            Obj helper = new Obj(Obj.Var, "", Tab.intType, 
                                 m_mapStringToObjectList.get(strconst).getFirst().getAdr(), // pozicija stringa u parser.stringConstants ali ovo se gazi tako moze bilo sta
                                 0);
            Code.store(helper);
            // kopiraj string
            for (int i = 0; i < strconst.length(); ++i)
            {
                Code.load(helper); // adresa
                Code.loadConst(i); // index
                Code.loadConst(strconst.charAt(i)); // vrednost
                Code.put(Code.bastore);
            }
            // Zapisi adresu niza u sve objektne cvorove koji gadjaju njega
            //setPtrToStringForConstants(strconst, helper.getAdr());
            
            Iterator<Obj> itobj = m_mapStringToObjectList.get(strconst).iterator();
            itobj.next();
            if (itobj.hasNext())
            {
                Code.load(helper);
            }
            while (itobj.hasNext())
            {
                Obj obj = itobj.next();
                Obj static_ = new Obj(Obj.Var, "", obj.getType(), obj.getAdr(), 0);
                Code.store(static_);
                if (itobj.hasNext())
                {
                    Code.load(static_);
                }
            }
        }
    }
    
    public static void setPtrToStringForConstants()
    {
        Set<String> strings = m_mapStringToObjectList.keySet();
        Iterator<String> it = strings.iterator();
        while (it.hasNext())
        {
            String string = it.next();
            Iterator<Obj> itobj = m_mapStringToObjectList.get(string).iterator();
            while (itobj.hasNext())
            {
                Obj obj = itobj.next();
                obj.setAdr(Code.dataSize++);
            }
        }
        /*Iterator<Obj> it = m_mapStringToObjectList.get(strconst).iterator();
        while (it.hasNext())
        {
            Obj constant = it.next();
            constant.setAdr(pointer);
        }*/
    }
    
    public static void functionDeclEnd(Obj func)
    {
        if (m_bError) return;
        if (func.getType() == Tab.noType)   // za void stavi return (jer ne mora eksplicitno da se navede
        {
            Code.put(Code.exit);
            Code.put(Code.return_);
        }
        else
        {
            Code.put(Code.trap);    // trap za svaki slucaj
            Code.put(1);
        }
    }
    
    /**
     * Poziva se prilikom deklarisanja nove string konstante.
     * Pamti string konstantu i listu objektnih cvorova koji zahtevaju konstantu.
     * String konstante se alociraju kao nizovi na heapu pre pocetka main funkcij, a pokazivaci idu na kraju static data.
     * Svi objektni cvorovi string konstanti koji imaju istu vrednost stringa pokazuju na tu adresu u memoriji
     * @param constId - ime objektnog cvora string konstante koja se definise
     * @param lstString - ulancana lista svih string konstanti
     */
    public static void constantStringDeclaration(String constId, LinkedList<String> stringConstantsList) throws Exception
    {
        if (m_bError) return;
        Obj obj = Tab.find(constId);
        if (Tab.noObj == obj)
        {
            throw new Exception("Codegen.constantStringDeclaration(String, LinkedList<String>) - id konstante ne moze biti Tab.noObj");
        }
        // Ako nemamo u mapi nas string (konstanta se deklarisala pre ovog poziva i inicijalizuje na nas string)
        if (!m_mapStringToObjectList.containsKey(stringConstantsList.getLast()))
        {
            m_mapStringToObjectList.put(stringConstantsList.getLast(), new LinkedList<Obj>()); // dodaj novu listu objekata za ovaj string
        }
        
        m_mapStringToObjectList.get(stringConstantsList.getLast()).addLast(obj);
    }
    
    /**
     * Poziva se iz MetRetType_ident ::= MetRetType:mrt IDENT:id 
     * prilikom pravljenja objektnog cvora metode
     * @param id
     * @param currentMethodDeclarationObj
     */
    public static void functionTypeAndId(String id, Obj currentMethodDeclarationObj)
    {
        if (m_bError) return;
        currentMethodDeclarationObj.setAdr(Code.pc);
        if (id.equals("main"))
        {
            Code.mainPc = currentMethodDeclarationObj.getAdr();
            m_bMain = true;
        }
    }

    public static void returnStatement()
    {
        if (m_bError) return;
        Code.put(Code.exit);
        Code.put(Code.return_);
    }
    
    /**
     * Generisanje koda za read naredbu
     * @param dst - designator za smestanje vrednosti
     * @param tBool - parser bool type
     * @param tString - parser string type
     */
    public static void readStatement(Obj dst, Struct tBool, Struct tString)
    {
        if (m_bError) return;
        if (dst == null || dst == Tab.noObj) return;
        
        if (dst.getType() == Tab.intType || dst.getType() == tBool)
        {
            Code.put(Code.read);
            Code.store(dst);
        }
        else if (dst.getType() == Tab.charType)
        {
            Codegen.readChar();
            Code.store(dst);
        }
        else if (dst.getType() == tString)
        {
            Code.put(Code.enter);
            Code.put(0); Code.put(3);
            Obj len = new Obj(Obj.Var, "", Tab.intType, 0, 1);
            Obj readCharacter = new Obj(Obj.Var, "", Tab.intType, 1, 1);
            Obj address = new Obj(Obj.Var, "", Tab.intType, 2, 1);
            Code.loadConst(0);
            Code.store(len);
            // prvi char na eStack
            readChar();
            // ++len
            Code.loadConst(1);
            Code.load(len);
            Code.put(Code.add);
            Code.store(len);
            // while (1)
            // {
            int LOOP = Code.pc;
                // read(readCharacter)
                Code.put(Code.bread);
                Code.store(readCharacter);
                // if (readCharacter == \n\r\b\f\t)
                //    break;
                Code.load(readCharacter);
                Code.loadConst('\n');
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpNewLine = Code.pc - 2;
                Code.load(readCharacter);
                Code.loadConst('\r');
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpCarriageReturn = Code.pc - 2;
                Code.load(readCharacter);
                Code.loadConst('\b');
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpBackspace = Code.pc - 2;
                Code.load(readCharacter);
                Code.loadConst('\f');
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpFormFeed = Code.pc - 2;
                Code.load(readCharacter);
                Code.loadConst('\t');
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpTab = Code.pc - 2;
                // procitani char na eStack
                Code.load(readCharacter);
                // ++len
                Code.load(len);
                Code.loadConst(1);
                Code.put(Code.add);
                Code.store(len);
            // }
                Code.putJump(LOOP);
            
            Code.fixup(jmpNewLine);
            Code.fixup(jmpCarriageReturn);
            Code.fixup(jmpBackspace);
            Code.fixup(jmpFormFeed);
            Code.fixup(jmpTab);
            // address = new char[len]
            Code.load(len);
            Code.put(Code.newarray);
            Code.put(0);
            Code.store(address);
            //LOOP2
            LOOP = Code.pc;
            // while (len != 0)
            // {
                Code.load(len);
                Code.loadConst(0);
                Code.putFalseJump(Code.inverse[Code.eq], 0);
                int jmpEndLoop2 = Code.pc - 2;
                // --len
                Code.load(len);
                Code.loadConst(-1);
                Code.put(Code.add);
                Code.store(len);
                
                Code.store(readCharacter);
                // adress[len] = readCharacter
                Code.load(address);
                Code.load(len);
                Code.load(readCharacter);
                Code.put(Code.bastore);
            // }
                Code.putJump(LOOP);
            Code.fixup(jmpEndLoop2);
            Code.load(address); // adresa na eStack
            Code.put(Code.exit);
            Code.store(dst);    // pa u dst
        }
    }
    
    /**
     * Cita char sa ulaza i stavlja na sStack. Preskacu se \n\r\b\f\t karakteri
     */
    private static void readChar()
    {
        if (m_bError) return;
        Code.put(Code.enter);
        Code.put(0); Code.put(1);
        Obj readCharacter = new Obj(Obj.Var, "", Tab.charType, 0, 1);
        int jmpAdr = Code.pc;
        Code.put(Code.bread);
        Code.store(readCharacter);
        Code.load(readCharacter);
        Code.loadConst('\n');
        Code.putFalseJump(Code.inverse[Code.eq], jmpAdr);
        Code.load(readCharacter);
        Code.loadConst('\r');
        Code.putFalseJump(Code.inverse[Code.eq], jmpAdr);
        Code.load(readCharacter);
        Code.loadConst('\b');
        Code.putFalseJump(Code.inverse[Code.eq], jmpAdr);
        Code.load(readCharacter);
        Code.loadConst('\f');
        Code.putFalseJump(Code.inverse[Code.eq], jmpAdr);
        Code.load(readCharacter);
        Code.loadConst('\t');
        Code.putFalseJump(Code.inverse[Code.eq], jmpAdr);
        Code.load(readCharacter);
        Code.put(Code.exit);
    }
    
    /**
     * Generisanje koda za print statement 
     * @param type - tip izraza (mora biti int, char, bool, string
     * @param number - sirina ispisa
     */
    public static void printStatement(Struct type, int number)
    {
        if (m_bError) return;
        switch (type.getKind())
        {
            case Struct.Int:
                Code.loadConst(number);
                Code.put(Code.print);
                break;
            case Struct.Char:
                Code.loadConst(number);
                Code.put(Code.bprint);
                break;
            case Struct.Bool:
                Code.loadConst(number);
                Code.put(Code.print);
                break;
            case Struct.Array:
                Code.put(Code.enter);
                Code.put(0); Code.put(3);
                Obj adrString   = new Obj(Obj.Var, "", Tab.intType, 0, 1);
                Obj index       = new Obj(Obj.Var, "", Tab.intType, 1, 1);
                Obj width       = new Obj(Obj.Var, "", Tab.intType, 2, 1);
                Code.store(adrString);  // ucitaj u adrString adresu stringa sa eStack-a
                Code.loadConst(number); // stavi broj karaktera za stampanje na eStack
                Code.store(width);      // i stavi ga u width promenljivu
                
                Code.load(adrString);   // adr na eStack
                Code.put(Code.arraylength); // len na eStack
                Code.loadConst(0);          // 0 na eStack
                Code.putFalseJump(Code.inverse[Code.eq], 0); // da li je duzina stringa 0?
                int adr = Code.pc - 2;
                Code.loadConst(0);
                Code.store(index);  // index = 0
                // LOOP:
                    int LOOP = Code.pc;
                    Code.load(adrString);   // adr na eStack
                    Code.load(index);       // index na eStack
                    Code.put(Code.baload);  // vrednost (char) na eStack
                    Code.load(width);       // sirina na eStack
                    Code.put(Code.bprint);  // print char
                    
                    Code.loadConst(0);
                    Code.store(width);       // width = 0;
                    
                    
                    Code.load(index);
                    Code.loadConst(1);
                    Code.put(Code.add); 
                    Code.store(index);  // ++index
                    
                    Code.load(index);
                    Code.load(adrString);
                    Code.put(Code.arraylength);
                    Code.putFalseJump(Code.eq, LOOP); // if index != len jump back to LOOP
                // end of LOOP
                Code.fixup(adr);
                Code.put(Code.exit);
                break;
            default:
                break;
        }
    }
    
    /**
     * Inkrementira ili dekrementira designator za increment vrednost (IncDecStatement)
     * @param des - designator
     * @param startAdrDesignator - startna adresa
     * @param increment - vrednost za koju se designator inkrementira
     */
    public static void designatorIncDec(Obj des, int startAdrDesignator, int increment)
    {
        if (m_bError) return;
        if (des != null && des != Tab.noObj && des.getKind() != Obj.Con)
        {
            int endAdrDesignator = Code.pc;
            while (startAdrDesignator < endAdrDesignator)
            {
                Code.put(Code.get(startAdrDesignator));
                ++startAdrDesignator;
            }
            Code.load(des);
            Code.loadConst(increment);
            Code.put(Code.add);
            Code.store(des);
        }
    }
    
    public static void factorNEWType(Obj currentType, boolean isArray)
    {
        if (m_bError) return;
        if (currentType == null || currentType.getType() == null) return;
        
        if (isArray)
        {
            Code.put(Code.newarray);
            Code.put(currentType.getType() == Tab.charType ? 0 : 1);
        }
        else
        {
            Code.put(Code.enter);
            Code.put(0); Code.put(1);
            Obj address = new Obj(Obj.Var, "", Tab.intType, 0, 1); // local
            
            // instanciranje objekta
            Code.put(Code.new_);
            Code.put2(currentType.getType().getNumberOfFields() * 4);
            Code.store(address);
            
            // setovanje VFTP za instancu objekta
            Obj vftp = new Obj(Obj.Fld, "", Tab.intType, 0, 1); // fld
            Code.load(address); // adresa na eStack za pokupiti
            if (m_mapClassNameToVFTP.containsKey(currentType.getName()))
            {
                // ako je poznat vft stavi na eStack
                Code.put(Code.const_);
                Code.put4(m_mapClassNameToVFTP.get(currentType.getName())); // vftp na eStack
            }
            else
            {
                // nije poznat VFTP, fix later (slucaj kada se new poziva u metodi neke klase)
                if (!m_mapClassObjectToVFTPAddrListFixup.containsKey(currentType.getName()))
                {
                    m_mapClassObjectToVFTPAddrListFixup.put(currentType.getName(), new LinkedList<Integer>());
                }
                Code.put(Code.const_);
                m_mapClassObjectToVFTPAddrListFixup.get(currentType.getName()).addLast(Code.pc);
                Code.put4(0); // bice fixovano pred pocetak main-a
            }
            // smesti u objekat
            Code.store(vftp); // putfield_0

            // adresa objekta na eStack
            Code.load(address);
            Code.put(Code.exit);
        }
    }
    
    public static void exprRelopExpr(String relop)
    {
        if (m_bError) return;
        
        int fwdJmpIfFalse;  // skok na false ako uslov nije ispunjen
        int fwdJmpEnd;      // skok na kraj ako je ispunjen i propali smo dalje
        switch (relop)
        {
            case "==":
                Code.putFalseJump(Code.eq /*Code.inverse[Code.ne]*/, 0); // ako nisu jednaki skoci na false
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse); 
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            case "!=":
                Code.putFalseJump(Code.ne, 0);
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse);
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            case ">":
                Code.putFalseJump(Code.gt, 0);
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse);
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            case ">=":
                Code.putFalseJump(Code.ge, 0);
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse);
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            case "<":
                Code.putFalseJump(Code.lt, 0);
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse);
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            case "<=":
                Code.putFalseJump(Code.le, 0);
                fwdJmpIfFalse = Code.pc - 2;
                Code.loadConst(1);  // true
                Code.putJump(0);
                fwdJmpEnd = Code.pc - 2;
                
                Code.fixup(fwdJmpIfFalse);
                Code.loadConst(0);  // false
                Code.fixup(fwdJmpEnd);
                break;
            default:
                System.err.println("Codegen#exprRelopExpr - nevalidna operacija: " + relop);
                m_bError = true;
                break;
        }
    }
    
    public static void exprRelopExprArrays(String relop, boolean isChar)
    {
        if (m_bError) return;
        
        Code.put(Code.enter);
        Code.put(0); Code.put(4);
        Obj firstArray  = new Obj(Obj.Var, "", Tab.intType, 0, 1);
        Obj sencondArray= new Obj(Obj.Var, "", Tab.intType, 1, 1);
        Obj sizeArray   = new Obj(Obj.Var, "", Tab.intType, 2, 1);
        Obj it          = new Obj(Obj.Var, "", Tab.intType, 3, 1);
        
        Code.store(sencondArray);
        Code.store(firstArray);
        // it = 0;
        Code.loadConst(0);
        Code.store(it);
        // size?
        Code.load(firstArray);      // estack push len(ar1)
        Code.put(Code.arraylength); 
        Code.load(sencondArray);    // estack push len(ar2)
        Code.put(Code.arraylength);
        Code.store(sizeArray);      // sizeArray = len(ar2)
        Code.load(sizeArray);       // estack = len(ar1), len(ar2)
        // ako su jednaki nastavi 
        Code.putFalseJump(Code.eq, 0);
        int lensNEQFixup = Code.pc - 2;
            int LOOP = Code.pc;
            // LOOP - while(it != size)
            // {
            Code.load(sizeArray);
            Code.load(it);
            Code.putFalseJump(Code.ne, 0); // (if it == len) break;
            int loopBreakFixup = Code.pc - 2;
            // load elem from 1st
            Code.load(firstArray);
            Code.load(it);
            Code.put(isChar ? Code.baload : Code.aload);
            // load elem from 2nd
            Code.load(sencondArray);
            Code.load(it);
            Code.put(isChar ? Code.baload : Code.aload);
            // if (ar1[it] != ar2[it]) break;
            Code.putFalseJump(Code.eq, 0);
            int elemsDifferFixup = Code.pc - 2;
            // ++it;
            Code.load(it);
            Code.loadConst(1);
            Code.put(Code.add);
            Code.store(it);
            Code.putJump(LOOP);
            // }
            
        // jednaki
        Code.fixup(loopBreakFixup);
        Code.loadConst( relop.equals("==") ? 1 : 0);
        Code.putJump(0);
        int finish = Code.pc - 2;
        
        // nisu jednaki
        Code.fixup(lensNEQFixup);
        Code.fixup(elemsDifferFixup);
        Code.loadConst(relop.equals("==") ? 0 : 1);
        Code.fixup(finish);
        Code.put(Code.exit);
    }
    
    public static void conditionAnd()
    {
        if (m_bError) return;
        Code.put(Code.mul); // false je 0 pa pomnozeno dace 0 ili nesto razlicito od 0
    }
    
    public static void conditionOr()
    {
        if (m_bError) return;
        Code.put(Code.add); // saberi operande
        
        // stavi jedinicu na eStack ako smo u sabiranju dobili nesto vece od 1
        Code.put(Code.enter);
        Code.put(0); Code.put(1);
        Obj result = new Obj(Obj.Var, "", Tab.intType, 0, 1);
            Code.store(result);
            
            Code.load(result);
            Code.loadConst(2);
            Code.putFalseJump(Code.ne, 0);  // if (result != 2) (znaci 1 je)
            int jmpFwdIfFalse = Code.pc - 2;
            Code.load(result);              // ostavi ga na eStack
            Code.putJump(0);
            int jmpEnd = Code.pc - 2;
            
            Code.fixup(jmpFwdIfFalse);      // else
            Code.loadConst(1);              // stavi 1
            Code.fixup(jmpEnd);
        Code.put(Code.exit);
    }
    
    public static void ifCondition()
    {
        if (m_bError) return;
        // ako nije ispunjen uslov skoci na else/kraj
        Code.loadConst(0);
        Code.putFalseJump(Code.ne, 0);
        m_stackFwdJmpIfStatementFalse.push(Code.pc - 2); // ne znamo gde skacemo, sacuvaj fixup adresu
    }
    
    /**
     * Koristi se na kraju IF grane. Posle if grane moze a i ne mora da sledi ELSE grana.
     * Ako ima else grane, treba da je preskocimo ako smo bili u IF grani. 
     * U svakom slucaju, fixujemo adresu na koju treba da skocimo ako uslov u IF bio false.
     * @param bElse - da li sledi ELSE grana ili ne.
     */
    public static void ELSEOrEndOfIFstmt(boolean bElse)
    {
        if (m_bError) return;
        
        if (bElse)  // ako je ovo pocetak else grane, treba nam skok na kraj else (za slucaj kada je if (true))
        {
            Code.putJump(0);
            m_stackFwdJmpToEndOfElseBranch.push(Code.pc - 2);
        }
        // ako je IF uslov bio false, skacemo ovde (nebitno da li je ovde else ili neka druga naredba
        if (!m_stackFwdJmpIfStatementFalse.isEmpty())
        {
            Code.fixup(m_stackFwdJmpIfStatementFalse.pop());
        }
    }
    
    /** Poziva se kada se zavrsava ELSE grana */
    public static void endOfELSEBranch()
    {
        if (m_bError) return;
        
        if (!m_stackFwdJmpToEndOfElseBranch.isEmpty())
        {
            Code.fixup(m_stackFwdJmpToEndOfElseBranch.pop());
        }
    }
    
    /** Cuvanje adrese evaluacije uslova za popravku */
    public static void whileSaveConditionEvaluationAddress()
    {
        if (m_bError) return;
        m_stackWhileCondEvalAdr.push(Code.pc);
    }
    
    /** Evaluacija while uslova */
    public static void whileConditionEvaluation()
    {
        if (m_bError) return;
        Code.loadConst(0);
        Code.putFalseJump(Code.ne, 0); // ako je uslov false, skaci na kraj
        // sacuvaj adrese skoka na kraj iteracije za popravku
        m_stackWhileEndAdrFixup.push(Code.pc - 2);
        m_stackBreakAdrFixup.push(new LinkedList<Integer>());
    }
    
    /** Popravlja skokove na kraj petlje ako uslov nije bio ispunjen
     *  Popravlja break skokove na kraj petlje
     */
    public static void whileIterationEnd()
    {
        if (m_bError) return;
        Code.putJump(m_stackWhileCondEvalAdr.pop());
        Code.fixup(m_stackWhileEndAdrFixup.pop());
        LinkedList<Integer> breaks = m_stackBreakAdrFixup.pop();
        for (int adr : breaks)
        {
            Code.fixup(adr);
        }
    }
    
    /** pamti adrese skokova na kraj petlje koje treba popraviti u tekucoj while petlji */
    public static void breakStatement()
    {
        if (m_bError) return;
        Code.putJump(0);
        m_stackBreakAdrFixup.peek().addLast(Code.pc - 2);
    }
}