lexer grammar CmmLexer;
//注释
LCOMMENT : '//'.*?'\n' -> skip;
MLCOMMENT : '/*'.*?'*/' -> skip;
//关键字
TYPE : 'int' | 'float';
STRUCT : 'struct';
RETURN : 'return';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
//符号
SEMI : ';';
COMMA : ',';
ASSIGNOP : '=';
RELOP : '>'|'<'|'>='|'<='|'=='|'!=';
PLUS : '+';
MINUS : '-';
STAR : '*';
DIV : '/';
AND : '&&';
OR : '||';
DOT : '.';
NOT : '!';
LP : '(';
RP : ')';
LB : '[';
RB : ']';
LC : '{';
RC : '}';
//数值
FLOAT: ([0-9]+'.'[0-9]+EXP?|'.'[0-9]+EXP|[0-9]+'.'EXP);
INT : INT_DEC | INT_OCT | INT_HEX;
//id
ID : [_a-zA-Z][_a-zA-Z0-9]*;
//空白符
WS : [ \t\r\n]+ -> skip;

fragment
EXP : [eE][+-]?[0-9]+;
fragment
INT_DEC : '0'|([1-9][0-9]*);
fragment
INT_OCT : '0'[0-7]+;
fragment
INT_HEX : '0'[xX][0-9a-fA-F]+;