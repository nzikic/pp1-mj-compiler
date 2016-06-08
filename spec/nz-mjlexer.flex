package nzikic.pp1;

import java_cup.runtime.Symbol;


%%

%{
    // ukljucivanje informacije o poziciji tokena
    private Symbol new_symbol(int type) 
    {
        return new Symbol (type, yyline+1, yycolumn);
    }
    
    private Symbol new_symbol(int type, Object value) 
    {
        return new Symbol (type, yyline+1, yycolumn, value);
    }
    
    public StringBuffer g_stringbuffer = new StringBuffer();    // StringBuffer za punjenje u stanju STRING
%}


LineTerminator = \r|\n|\r\n

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]
// InputCharacter = [^\r\n]

PrintableCharacter = [\x20-\x21|\x23-\x26|\0x28-\x5B|\x5D-\x7E]        /* svi printable chars sem ' " /    */

%cup
%line
%column

%xstate COMMENT
%xstate CHARLITERAL
%xstate STRING


%eofval{
    return new_symbol(sym.EOF);
%eofval}


%%

" "            {}
"\b"        {}
"\t"        {}
"\r\n"        {}
"\r"        {}
"\n"        {}
"\f"        {}

"program"                        { return new_symbol( sym.PROGRAM, yytext() ); }
"print"                            { return new_symbol( sym.PRINT, yytext() ); }
"return"                        { return new_symbol( sym.RETURN, yytext() ); }
"void"                            { return new_symbol( sym.VOID, yytext() ); }
"break"                            { return new_symbol( sym.BREAK, yytext() ); }
"class"                            { return new_symbol( sym.CLASS, yytext() ); }
"if"                            { return new_symbol( sym.IF, yytext() ); }
"else"                            { return new_symbol( sym.ELSE, yytext() ); }
"new"                            { return new_symbol( sym.NEW, yytext() ); }
"read"                            { return new_symbol( sym.READ, yytext() ); }
"while"                            { return new_symbol( sym.WHILE, yytext() ); }
"extends"                        { return new_symbol( sym.EXTENDS, yytext() ); }
"const"                            { return new_symbol( sym.CONST, yytext() ); }
"true"                            { return new_symbol( sym.BOOLCONST, Boolean.valueOf(true) ); }
"false"                            { return new_symbol( sym.BOOLCONST, Boolean.valueOf(false) ); }
"++"                            { return new_symbol( sym.INC, yytext() ); }
"--"                            { return new_symbol( sym.DEC, yytext() ); }
"+"                                { return new_symbol( sym.PLUS, yytext() ); }
"-"                                { return new_symbol( sym.MINUS, yytext() ); }
"="                                { return new_symbol( sym.ASSIGN, yytext() ); }
";"                                { return new_symbol( sym.SEMI, yytext() ); }
","                                { return new_symbol( sym.COMMA, yytext() ); }
"*"                                { return new_symbol( sym.MUL, yytext() ); }
"/"                                { return new_symbol( sym.DIV, yytext() ); }
"%"                                { return new_symbol( sym.MOD, yytext() ); }
"=="                            { return new_symbol( sym.EQ, yytext() ); }
"!="                            { return new_symbol( sym.NEQ, yytext() ); }
">"                                { return new_symbol( sym.GT, yytext() ); }
">="                            { return new_symbol( sym.GE, yytext() ); }
"<"                                { return new_symbol( sym.LT, yytext() ); }
"<="                            { return new_symbol( sym.LE, yytext() ); }
"&&"                            { return new_symbol( sym.AND, yytext() ); }
"||"                            { return new_symbol( sym.OR, yytext() ); }
"."                                { return new_symbol( sym.DOT, yytext() ); }
"("                                { return new_symbol( sym.LPAREN, yytext() ); }
")"                                { return new_symbol( sym.RPAREN, yytext() ); }
"{"                                { return new_symbol( sym.LBRACE, yytext() ); }
"}"                                { return new_symbol( sym.RBRACE, yytext() ); }
"["                                { return new_symbol( sym.LBRACKET, yytext() ); }
"]"                                { return new_symbol( sym.RBRACKET, yytext() ); }


"//"                          { yybegin( COMMENT ); }
<COMMENT> .                   { yybegin( COMMENT ); }
<COMMENT> {LineTerminator}    { yybegin( YYINITIAL );    } 

[0-9]+                            { return new_symbol( sym.NUMBER, new Integer(yytext()) ); }
[0-9]+(\'|PrintableCharacter)+    { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Nedozvoljeni znakovi u broju, samo cifre su dozvoljene!"); }
([a-z]|[A-Z])[a-z|A-Z|_|0-9]*    { return new_symbol( sym.IDENT, yytext() ); }



/* string literal */
\"                        { yybegin(STRING); g_stringbuffer.setLength(0); }


/* character literal */
''                        { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char literal mora da sadrzi samo jedan printable character izmedju jednostrukih navoda"); }
\'                        { yybegin(CHARLITERAL); }

<STRING> {
    \"                        { yybegin(YYINITIAL); return new_symbol( sym.STRCONST, g_stringbuffer.toString() ); }
    
    {StringCharacter}+        { g_stringbuffer.append( yytext() ); }
    
    /* escape sequences */
    "\\b"                            { g_stringbuffer.append( '\b' ); }
    "\\t"                            { g_stringbuffer.append( '\t' ); }
    "\\n"                            { g_stringbuffer.append( '\n' ); }
    "\\f"                            { g_stringbuffer.append( '\f' ); }
    "\\r"                            { g_stringbuffer.append( '\r' ); }
     "\\\""                            { g_stringbuffer.append( '\"' ); }
    "\\'"                            { g_stringbuffer.append( '\'' ); }
    "\\\\"                            { g_stringbuffer.append( '\\' ); }
//    \\[0-3]?{OctDigit}?{OctDigit}    { char val = (char) Integer.parseInt(yytext().substring(1),8); g_stringbuffer.append( val ); }
    
    /* error cases */
    \\.                                { System.err.println("Leksicka greska u stringu na liniji " + (yyline+1) + ", koloni " + yycolumn + " : Nelegalna escape sekvenca \"" +yytext()+"\"" ); }
    {LineTerminator}                { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : String literal mora biti u jednom redu!"); }
}    


<CHARLITERAL> {

    {SingleCharacter}\'                { yybegin(YYINITIAL); return new_symbol( sym.CHARCONST, new Character( yytext().charAt(0) ) ); }
    
    /* escape sequences */
    "\\b"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\b')); }
    "\\t"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\t')); }
    "\\n"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\n')); }
    "\\f"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\f')); }
    "\\r"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\r')); }
    "\\\""\'                       { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\"')); }
    "\\'"\'                        { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\'')); }
    "\\\\"\'                       { yybegin(YYINITIAL); return new_symbol(sym.CHARCONST, new Character('\\')); }
    
    /* error cases */
    
    {SingleCharacter}{SingleCharacter}+\'     { yybegin(YYINITIAL); System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char literal mora da sadrzi samo jedan printable character izmedju jednostrukih navoda"); }
    \\.                                { yybegin(YYINITIAL); System.err.println("Leksicka greska na liniji " + (yyline+1) + ", koloni " + yycolumn + " : Nelegalna escape sekvenca \"" +yytext()+"\"" ); }
    {LineTerminator}                { yybegin(YYINITIAL); System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char literal mora biti u jednom redu!"); }
//    .                                { yybegin(YYINITIAL); System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char mora biti jedan printable char u jednom redu izmedju jednostrukih navoda!!! ' '"); }
}





.     { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn); }












/******  Proba, nadjeno na stackoverflow!  ******/
/************************************************/

/*    STRING 
    String se sastoji od navodnika                     "
    pracen sa nula ili vise eskejpovanog bilocega     \\.
    ili non-quote karaktera                         [^"]
    i zavrsava se sa navodikom                         "
*/
//    \"(\\.|[^"])*\"        { return new_symbol( sym.STRCONST, new String(yytext()) ); }        // Leksika jezika C - http://www.lysator.liu.se/c/ANSI-C-grammar-l.html

/*    CHARCONST
    Character literal pocinje apostrofom            '
    pracen sa jednim eskejpovanim karakterom        \\.
    ili bilo cim sem ' \ ili novog reda                [^'\\\n]
    i zavrsava se apostrofom                        '
    http://stackoverflow.com/questions/15550553/flex-regular-expression-literal-char
*/
//    '([^'\\\n]|\\.)'    { return new_symbol( sym.CHARCONST, new Character(yytext().charAt(1)) ); }


// edge i error cases
//    ''                    { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char literal mora imati jedan character u sebi!"); }
//    '                    { System.err.println("Leksicka greska (" + yytext() + ") u liniji " + (yyline+1) + ", koloni " + yycolumn + " : Char literal nije zatvoren!"); }





/* string literal */
















