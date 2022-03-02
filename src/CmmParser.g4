parser grammar CmmParser;

options {
    tokenVocab=CmmLexer;
}

program: extDef* EOF;
extDef: specifier extDecList? SEMI #extDefVar
    | specifier funDec compSt #extDefFun
    ;
extDecList: varDec (COMMA varDec)*;

specifier: TYPE
    | structSpecifier;
structSpecifier: STRUCT optTag LC defList RC
    | STRUCT tag;
optTag: ID?;
tag: ID;

varDec: ID (LB
    (
    INT
    | {notifyErrorListeners("array size must be an integer constant, not "+this.getCurrentToken().getText());} FLOAT
    | {notifyErrorListeners("array size must be an integer constant, not "+this.getCurrentToken().getText());} ID
    )
    RB)*;
funDec: ID LP varList? RP;
varList: paramDec (COMMA paramDec)*;
paramDec: specifier varDec;

compSt: LC defList stmtList RC;
stmtList: stmt*;
stmt: exp SEMI #stmt_
    | compSt #stmt_
    | RETURN exp SEMI #stmtRet
    | IF LP exp RP stmt #stmtLog
    | IF LP exp RP stmt ELSE stmt #stmtLog
    | WHILE LP exp RP stmt #stmtLog
    ;

defList: def*;
def: specifier decList SEMI;
decList: dec (COMMA dec)*;
dec: varDec (ASSIGNOP exp)?;

exp:  LP exp RP #expP
    | ID LP args RP #expFun
    | ID LP RP #expFun
    | exp LB exp RB #expArr
    | exp DOT ID #expStt
    | <assoc=right> (MINUS|NOT) exp #expCalLog
    | exp (STAR|DIV) exp #expCal
    | exp (PLUS|MINUS) exp #expCal
    | exp RELOP exp #expCal
    | exp AND exp #expLog
    | exp OR exp #expLog
    | <assoc=right> exp ASSIGNOP exp #expAsn
    | ID #expID
    | INT #expTmp
    | FLOAT #expTmp
    ;
args: exp (COMMA exp)*;