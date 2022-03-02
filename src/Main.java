import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;
import java.util.*;

public class Main
{
    public static boolean err = false;
    public static Boolean[] errLine=new Boolean[1000];

    public static SymbolTable symbolTable = new SymbolTable();
    public static void main(String[] args){
        try{
            File file = new File(args[0]);
            FileOutputStream outFile = new FileOutputStream(args[1]);
            PrintStream out = new PrintStream(outFile);


            InputStream fin = new FileInputStream(file);
            CharStream s = CharStreams.fromStream(fin);
            CmmLexer cmmLexer = new CmmLexer(s){
                @Override
                public void notifyListeners(LexerNoViableAltException e) {
                }
            };
            fin.close();

            CommonTokenStream tokens = new CommonTokenStream(cmmLexer);
            CmmParser cmmParser = new CmmParser(tokens) {
                @Override
                public void notifyErrorListeners(Token offendingToken, String msg, RecognitionException e) {
                    if(Main.errLine[offendingToken.getLine()]!=null&&Main.errLine[offendingToken.getLine()]) return;
                    Main.errLine[offendingToken.getLine()] = true;
                    Main.err=true;
                    System.err.println("Error type B at Line "+offendingToken.getLine());
                }
            };
            ParseTree tree = cmmParser.program();
            if(err) return;
            Function readFun = new Function(new Int());
            symbolTable.fill("read",readFun);
            Function writeFun = new Function(new Int());
            writeFun.addParam("",new Int());
            symbolTable.fill("write",writeFun);
            ParseTreeWalker walker = new ParseTreeWalker();
            MyListener listener = new MyListener(cmmLexer,cmmParser);
            walker.walk(listener, tree);
            IRVisitor visitor = new IRVisitor();
            InterCodeList code = visitor.visit(tree);
            code.printAll();
            System.setOut(out);
            code.generateCode();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

class MyListener extends CmmParserBaseListener {
    ParseTreeProperty<Type> types = new ParseTreeProperty<>();
    ParseTreeProperty<String> names = new ParseTreeProperty<>();
    ParseTreeProperty<Boolean> isStructureField = new ParseTreeProperty<>();
    String[] lexerRuleNames;
    String[] parserRuleNames;
    boolean printTree = false;
    int depth=0;
    public MyListener(CmmLexer cmmLexer, CmmParser cmmParser) {
        lexerRuleNames = cmmLexer.getRuleNames();
        parserRuleNames = cmmParser.getRuleNames();
    }

    void align(int depth) {
        for(int i=0;i<depth-1;i++) {
            System.err.print("  ");
        }
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if(printTree) {
            this.depth++;
            if (ctx.getChildCount() == 0) return;
            align(this.depth);
            System.err.println(parserRuleNames[ctx.getRuleIndex()].substring(0, 1).toUpperCase()
                    + parserRuleNames[ctx.getRuleIndex()].substring(1)
                    + " (" + ctx.start.getLine() + ")");
        }
    }
    public void exitEveryRule(ParserRuleContext ctx) {
        if(printTree) {
            this.depth--;
        }
    }

    @Override public void exitSpecifier(CmmParser.SpecifierContext ctx) {
        if(ctx.TYPE()!=null) {
            if(ctx.TYPE().getText().equals("int")) {
                types.put(ctx,new Int());
            }
            else if(ctx.TYPE().getText().equals("float")) {
                types.put(ctx,new Float());
            }
        }
        types.put(ctx.parent,types.get(ctx));
    }
    @Override public void exitOptTag(CmmParser.OptTagContext ctx) {
        if(ctx.ID()==null) return;
        String name = ctx.ID().getSymbol().getText();
        Structure structure = new Structure(name);
        try{
            if(Main.symbolTable.search(name)!=null) {
                throw new MyException(16);
            }
            names.put(ctx.parent,name);
            types.put(ctx.parent,structure);
            Main.symbolTable.fill(name,structure);
        }catch (MyException e) {
            e.print(ctx.start.getLine());
            types.put(ctx.parent,null);
        }
    }
    @Override public void enterStructSpecifier(CmmParser.StructSpecifierContext ctx) {
        Structure structure = new Structure(null);
        types.put(ctx,structure);
        isStructureField.put(ctx,true);
    }
    @Override public void exitStructSpecifier(CmmParser.StructSpecifierContext ctx) {
        String name;
        Structure structure = (Structure) types.get(ctx);
        if(structure==null) return;
        if (ctx.tag() != null) {
            name = ctx.tag().ID().getSymbol().getText();
            Type type = Main.symbolTable.search(name);
            try{
                if(type==null || type.getKind()!=Kind.STRUCTURE) {
                    throw new MyException(17);
                }
                structure = (Structure) type;
                if(!structure.getName().equals(name)) {
                    throw new MyException(17);
                }
                types.put(ctx, structure);
                types.put(ctx.parent, structure);
            }catch (MyException e) {
                e.print(ctx.start.getLine());
            }
        } else {
            List<CmmParser.DefContext> defList = ctx.defList().def();
            for (CmmParser.DefContext def : defList) {
                List<CmmParser.DecContext> decList = def.decList().dec();
                for (CmmParser.DecContext dec : decList) {
                    Type dType = types.get(dec.varDec());
                    if(dType!=null) {
                        String dName = names.get(dec.varDec());
                        structure.addMember(dName, dType);
                    }
                }
            }
            types.put(ctx.parent, structure);
        }

    }
    @Override public void enterExtDecList(CmmParser.ExtDecListContext ctx) {
        types.put(ctx,types.get(ctx.parent));
    }

    @Override public void exitVarDec(CmmParser.VarDecContext ctx) {
        Type type = types.get(ctx.parent);
        if(type==null) return;
        String name = ctx.ID(0).getSymbol().getText();
        if(ctx.LB()!=null&&ctx.LB().size()!=0) {
            type = new Array(type,ctx.LB().size(),Integer.parseInt(ctx.INT(0).getText())); // only one dimension
        }
        boolean flag = false;
        if(isStructureField.get(ctx.parent)!=null) {
            flag = isStructureField.get(ctx.parent);
        }
        try {
            if (Main.symbolTable.search(name) != null) {
                if(flag)
                    throw new MyException(15);
                else
                    throw new MyException(3);
            }
            types.put(ctx,type);
            names.put(ctx,name);
            Main.symbolTable.fill(name,type.clone());
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void enterFunDec(CmmParser.FunDecContext ctx) {
        Type type = types.get(ctx.parent);
        if(type==null) return;
        String name = ctx.getText().split("\\(")[0];
        try{
            if(Main.symbolTable.search(name)!=null) {
                throw new MyException(4);
            }
            Type t = type.clone();
            t.setLeftVal(false);
            Function fun = new Function(t);
            Main.symbolTable.fill(name,fun);
            types.put(ctx,fun);
        }catch (MyException e) {
            e.print(ctx.start.getLine());
            types.put(ctx.parent,null);
        }
    }
    @Override public void enterVarList(CmmParser.VarListContext ctx) {
        if(types.get(ctx.parent)==null) {
            ctx.children = null;
        }
    }
    @Override public void exitFunDec(CmmParser.FunDecContext ctx) {
        Type type = types.get(ctx.parent);
        if(type==null) return;
        String name = ctx.ID().getText();
        Function function = (Function)Main.symbolTable.search(name);
        if(ctx.varList()!=null) {
            List<CmmParser.ParamDecContext> paramDecList = ctx.varList().paramDec();
            for(CmmParser.ParamDecContext paramDec: paramDecList) {
                Type pType = types.get(paramDec.varDec());
                if(pType!=null) {
                    function.addParam(names.get(paramDec.varDec()),pType);
                }
            }
        }
        //Main.symbolTable.fill(name,function);
        types.put(ctx,function);
        names.put(ctx,name);
    }

    @Override public void exitDec(CmmParser.DecContext ctx) {
        Type type = types.get(ctx.varDec());
        if(type==null) return;
        boolean flag = false;
        if(isStructureField.get(ctx.parent)!=null) {
            flag = isStructureField.get(ctx);
        }
        try {
            if(ctx.ASSIGNOP()!=null) {
                if(flag)
                    throw new MyException(15);
                else {
                    Type eType = types.get(ctx.exp());
                    if(eType!=null && !type.equals(eType)) {
                        throw new MyException(5);
                    }
                }
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }

    @Override public void enterCompSt(CmmParser.CompStContext ctx) {
        Type type = types.get(ctx.parent);
        if(type==null) {
            ctx.children=null;
            return;
        }
        types.put(ctx,type);
    }
    @Override public void enterDefList(CmmParser.DefListContext ctx) {
        Type type = types.get(ctx.parent);
        if(type==null) {
            ctx.children=null;
            return;
        }
        isStructureField.put(ctx,isStructureField.get(ctx.parent));
    }
    @Override public void enterDef(CmmParser.DefContext ctx) {
        isStructureField.put(ctx,isStructureField.get(ctx.parent));
    }
    @Override public void enterDecList(CmmParser.DecListContext ctx) {
        isStructureField.put(ctx,isStructureField.get(ctx.parent));
        types.put(ctx,types.get(ctx.parent));
    }
    @Override public void enterDec(CmmParser.DecContext ctx) {
        isStructureField.put(ctx,isStructureField.get(ctx.parent));
        types.put(ctx,types.get(ctx.parent));
    }
    @Override public void enterStmtList(CmmParser.StmtListContext ctx) {
        types.put(ctx,types.get(ctx.parent));
    }
    @Override public void enterStmt_(CmmParser.Stmt_Context ctx) {
        types.put(ctx,types.get(ctx.parent));
    }
    @Override public void enterStmtLog(CmmParser.StmtLogContext ctx) {
        types.put(ctx,types.get(ctx.parent));
    }

    @Override public void exitStmtRet(CmmParser.StmtRetContext ctx) {
        try{
            Type funcType = types.get(ctx.parent);
            Type type = types.get(ctx.exp());
            if(type==null) return;
            if(!funcType.equals(type)) {
                throw new MyException(8);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitStmtLog(CmmParser.StmtLogContext ctx) {
        Type type = types.get(ctx.exp());
        if(type==null) return;
        try{
            if(type.getKind()!=Kind.INT) {
                throw new MyException(7);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }

    @Override public void exitExpP(CmmParser.ExpPContext ctx) {
        types.put(ctx,types.get(ctx.exp()));
    }
    @Override public void exitExpTmp(CmmParser.ExpTmpContext ctx) {
        if(ctx.FLOAT()!=null) {
            Float type = new Float();
            type.setLeftVal(false);
            types.put(ctx, type);
        }
        else if(ctx.INT()!=null) {
            Int type = new Int();
            type.setLeftVal(false);
            types.put(ctx, type);
        }
    }
    @Override public void exitExpID(CmmParser.ExpIDContext ctx) {
        try {
            String name = ctx.ID().getSymbol().getText();
            Type type = Main.symbolTable.search(name);
            if(type==null) {
                throw new MyException(1);
            }
            else if(type.getKind()==Kind.STRUCTURE && name.equals(((Structure)type).getName())) {
                throw new MyException(1);
            }
            else {
                types.put(ctx,type);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpFun(CmmParser.ExpFunContext ctx) {
        try {
            String name = ctx.ID().getSymbol().getText();
            Type type = Main.symbolTable.search(name);
            if(type==null) {
                throw new MyException(2);
            }
            else {
                if(type.getKind()!=Kind.FUNCTION) {
                    throw new MyException(11);
                }
                FieldList paramList = ((Function)type).getParamList();
                FieldList eList = null;
                boolean flag = true;
                if(ctx.args()!=null) {
                    List<CmmParser.ExpContext> expList = ctx.args().exp();
                    for (CmmParser.ExpContext exp : expList) {
                        Type pType = types.get(exp);
                        if (pType != null) {
                            if(eList==null) {
                                eList = new FieldList(null,pType);
                            }
                            else {
                                eList.add(new FieldList(null,pType));
                            }
                        }
                        else {
                            flag = false;
                        }
                    }
                }
                if(flag) {
                    if(paramList==null && eList==null) {
                        //do nothing
                    }
                    else if(paramList!=null && eList!=null) {
                        if(!paramList.equals(eList)) {
                            throw new MyException(9);
                        }
                    }
                    else {
                        throw new MyException(9);
                    }
                    types.put(ctx,((Function) type).getReturnType());
                }
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpArr(CmmParser.ExpArrContext ctx) {
        try{
            Type type = types.get(ctx.exp(0));
            Type type1 = types.get(ctx.exp(1));
            if(type==null || type1==null) return;
            if(type.getKind()!=Kind.ARRAY) {
                throw new MyException(10);
            }
            else if(type1.getKind()!=Kind.INT) {
                throw new MyException(12);
            }
            else {
                Array array = (Array) type;
                Array thisType = new Array(array.getType(),array.getSize()-1,0); // it won't happen
                if(thisType.getSize()==0) {
                    types.put(ctx,thisType.getType());
                }
                else {
                    types.put(ctx,thisType);
                }
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpStt(CmmParser.ExpSttContext ctx) {
        try {
            String name = names.get(ctx.exp());
            String targetName = ctx.ID().getSymbol().getText();
            Type type = types.get(ctx.exp());
            if(type==null) {
                return;
            }
            else {
                if(type.getKind()!=Kind.STRUCTURE) {
                    throw new MyException(13);
                }
                Type target = ((Structure)type).getMember(targetName);
                if(target==null) {
                    throw new MyException(14);
                }
                types.put(ctx,target);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }

    @Override public void exitExpAsn(CmmParser.ExpAsnContext ctx) {
        try{
            CmmParser.ExpContext expLeft = ctx.exp(0), expRight = ctx.exp(1);
            Type typeLeft = types.get(expLeft), typeRight = types.get(expRight);
            if(typeLeft==null||typeRight==null) return;
            if(!typeLeft.isLeftVal()) {
                throw new MyException(6);
            }
            if(!typeLeft.equals(typeRight)) {
                throw new MyException(5);
            }
            types.put(ctx,typeLeft);
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpCal(CmmParser.ExpCalContext ctx) {
        try{
            CmmParser.ExpContext expLeft = ctx.exp(0), expRight = ctx.exp(1);
            Type typeLeft = types.get(expLeft), typeRight = types.get(expRight);
            if(typeLeft==null||typeRight==null) return;
            if(typeLeft.getKind()!=Kind.INT && typeLeft.getKind()!=Kind.FLOAT) {
                throw new MyException(7);
            }
            if(typeRight.getKind()!=Kind.INT && typeRight.getKind()!=Kind.FLOAT) {
                throw new MyException(7);
            }
            if(!typeLeft.equals(typeRight)) {
                throw new MyException(7);
            }
            if(ctx.RELOP()!=null)  {
                Type type = new Int();
                type.setLeftVal(false);
                types.put(ctx,type);
            }
            else {
                Type type = null;
                if(typeLeft.getKind()==Kind.INT) {
                    type = new Int();
                    type.setLeftVal(false);
                }
                else if(typeLeft.getKind()==Kind.FLOAT) {
                    type = new Float();
                    type.setLeftVal(false);
                }
                types.put(ctx,type);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpLog(CmmParser.ExpLogContext ctx) {
        try{
            CmmParser.ExpContext expLeft = ctx.exp(0), expRight = ctx.exp(1);
            Type typeLeft = types.get(expLeft), typeRight = types.get(expRight);
            if(typeLeft==null||typeRight==null) return;
            if(typeLeft.getKind()!=Kind.INT) {
                throw new MyException(7);
            }
            if(typeRight.getKind()!=Kind.INT) {
                throw new MyException(7);
            }
            Type type = new Int();
            type.setLeftVal(false);
            types.put(ctx,type);
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }
    @Override public void exitExpCalLog(CmmParser.ExpCalLogContext ctx) {
        try{
            Type type = types.get(ctx.exp());
            if(type==null) return;
            if(ctx.MINUS()!=null) {
                if(type.getKind()!=Kind.INT && type.getKind()!=Kind.FLOAT) {
                    throw new MyException(7);
                }
                Type rtype = null;
                if(type.getKind()==Kind.INT) {
                    rtype = new Int();
                    rtype.setLeftVal(false);
                }
                else if(type.getKind()==Kind.FLOAT) {
                    rtype = new Float();
                    rtype.setLeftVal(false);
                }
                types.put(ctx,rtype);
            }
            else if(ctx.NOT()!=null) {
                if(type.getKind()!=Kind.INT) {
                    throw new MyException(7);
                }
                Type rType = new Int();
                rType.setLeftVal(false);
                types.put(ctx,rType);
            }
        }catch (MyException e) {
            e.print(ctx.start.getLine());
        }
    }

    @Override
    public void visitTerminal(TerminalNode node) { //只用于打印树
        if(node.getSymbol().getType()==-1) return;
        if(printTree) align(this.depth+1);
        int type = node.getSymbol().getType();
        String tokenText = node.getSymbol().getText();
        String tokenType = lexerRuleNames[type-1];
        switch (tokenType) {
            case "INT": {
                long num = 0;
                if (tokenText.charAt(0) == '0' && tokenText.length() > 1) {
                    if (tokenText.charAt(1) == 'x' || tokenText.charAt(1) == 'X') {
                        num = Long.parseUnsignedLong(tokenText.substring(2, tokenText.length()), 16);
                    } else {
                        num = Long.parseUnsignedLong(tokenText, 8);
                    }
                } else {
                    num = Long.parseUnsignedLong(tokenText);
                }
                if(printTree) System.err.println("INT: " + num);
                break;
            }
            case "FLOAT": {
                Double num = Double.parseDouble(tokenText);
                if(printTree) System.err.printf("FLOAT: %.6f\n", num);
                break;
            }
            case "ID":
            case "TYPE":
                if(printTree) System.err.println(tokenType + ": " + node.getSymbol().getText());
                break;
            default:
                if(printTree) System.err.println(tokenType);
                break;
        }
    }
}

/*****************************
 Lab3
 ****************************/

class MyException extends Exception{
    int type;
    public MyException(int type) {
        this.type = type;
    }
    public int getType() {
        return type;
    }
    public void print(int line) {
        System.err.printf("Error type %d at Line %d\n",type,line);
    }
}

enum Kind {
    INT,
    FLOAT,
    ARRAY,
    STRUCTURE,
    FUNCTION
}
class Type implements Cloneable{
    protected boolean isLeftVal=true;
    boolean isAddr = false;
    int memSize;
    protected Kind kind;
    public Kind getKind() {
        return kind;
    }
    public boolean equals(Type t) { //赋值时的类型匹配，false为错误5
        if(kind != t.kind) {
            return false;
        }
        if(kind==Kind.ARRAY) {
            Array thisArray = (Array) this, tArray=(Array) t;
            if(thisArray.getSize()!=tArray.getSize()) return false;
            return thisArray.getType().equals(tArray.getType());
        }
        if(kind==Kind.STRUCTURE) {
            Structure thisStructure = (Structure) this, tStructure = (Structure) t;
            if(thisStructure.getName()!=null&&tStructure.getName()!=null) {
                if(thisStructure.getName().equals(tStructure.getName())) {
                    return true;
                }
            }
            //比较fieldList
            if(thisStructure.getMemberList()==null&&tStructure.getMemberList()==null) {
                return true;
            }
            else if(thisStructure.getMemberList()!=null&&tStructure.getMemberList()!=null) {
                return thisStructure.getMemberList().equals(tStructure.getMemberList());
            }
            else {
                return false;
            }
        }
        return true;
    }

    public void setLeftVal(boolean leftVal) {
        isLeftVal = leftVal;
    }
    public boolean isLeftVal() {
        return isLeftVal;
    }
    public void setAddr(boolean addr) {
        isAddr = addr;
    }
    public boolean isAddr() {
        return isAddr;
    }

    public int getMemSize() {
        return memSize;
    }

    @Override
    public Type clone() {
        try {
            Type clone = (Type) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
class Int extends Type{
    public Int() {
        kind = Kind.INT;
        memSize = 4;
    }
}
class Float extends Type{
    public Float() {
        kind = Kind.FLOAT;
        memSize = 4;
    }
}
class Array extends Type{
    private final Type type;
    int size;
    public Array(Type type, int size, int length) {
        kind = Kind.ARRAY;
        this.type = type;
        this.size = size;
        memSize = type.getMemSize() * length;
    }
    public Type getType() {
        return type;
    }
    public int getSize() {
        return size;
    }
}
class Function extends Type {
    private final Type returnType;
    private FieldList paramList;
    public Function(Type returnType) {
        this.isLeftVal=false;
        kind = Kind.FUNCTION;
        this.returnType = returnType;
    }
    public void addParam(String name, Type type) {
        FieldList item = new FieldList(name,type);
        if(this.paramList==null) {
            this.paramList = item;
        }
        else {
            this.paramList.add(item);
        }
    }

    public Type getReturnType() {
        return returnType;
    }
    public FieldList getParamList() {
        return paramList;
    }
}
class Structure extends Type {
    private final String name;
    private FieldList memberList;
    public Structure(String name) {
        kind = Kind.STRUCTURE;
        this.name = name;
        memSize = 0;
    }
    public void addMember(String name, Type type) {
        memSize += type.getMemSize();
        FieldList item = new FieldList(name,type);
        if(this.memberList==null) {
            this.memberList = item;
        }
        else {
            this.memberList.add(item);
        }
    }
    public Type getMember(String name) {
        FieldList f = this.memberList;
        while(f!=null) {
            if(f.getName().equals(name)) {
                return f.getType();
            }
            f=f.getNext();
        }
        return null;
    }

    public String getName() {
        return name;
    }
    public FieldList getMemberList() {
        return memberList;
    }
}
// FieldList并不是⼀个Type，仅⽤来存储函数的参数以及结构体的成员
class FieldList {
    private final String name;
    private final Type type;
    private FieldList next;
    public FieldList(String name, Type type) {
        this.name = name;
        this.type = type;
    }
    public void add(FieldList item) {
        if(this.next==null) {
            this.next = item;
        }
        else {
            this.next.add(item);
        }
    }
    public boolean equals(FieldList f) {
        if(!this.type.equals(f.type)) {
            return false;
        }
        if(this.next!=null && f.next!=null) {
            return this.next.equals(f.next);
        }
        else if(this.next==null && f.next==null) {
            return true;
        }
        else {
            return false;
        }
    }

    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }
    public FieldList getNext() {
        return next;
    }
}

class SymbolTable {
    int HASH_TABLE_SIZE = 2^10 - 1;
    HashNode[] nodeList = new HashNode[HASH_TABLE_SIZE + 1];
    private int getHashIndex(String name) {
        int val = 0, i;
        for (char c : name.toCharArray()) {
            val = (val << 2) + (int) c;
            // HASH_TABLE_SIZE描述了符号表的⼤⼩
            if ((i = (val & ~HASH_TABLE_SIZE)) != 0) {
                val = (val ^ (i >> 12)) & HASH_TABLE_SIZE;
            }
        }
        return val;
    }
    public Type search(String name) { //返回null时为错误1或2
        int i = getHashIndex(name);
        if(nodeList[i]!=null) {
            return nodeList[i].getType(name);
        }
        return null;
    }
    public void fill(String name, Type type) {
        //System.out.printf("Fill: %s %s\n",name,type.getKind().name());
        HashNode node = new HashNode(name,type);
        int i = getHashIndex(name);
        if(nodeList[i]!=null) {
            nodeList[i].addNext(node);
        }
        else {
            nodeList[i]=node;
        }
    }
}
// 使⽤散列表实现的符号表，散列表节点内容如下
class HashNode {
    String name;
    Type type;
    // 链表解决hash冲突
    HashNode next;
    public HashNode(String name, Type type) {
        this.name = name;
        this.type = type;
    }
    public void addNext(HashNode node) {
        if(this.next==null) {
            this.next=node;
        }
        else {
            this.next.addNext(node);
        }
    }
    public Type getType(String name) {
        if(this.name.equals(name)) {
            return this.type;
        }
        if(this.next!=null) {
            return this.next.getType(name);
        }
        return null;
    }
}

/*****************************
 Lab4
 ****************************/

enum CodeKind {
    LABEL,
    FUNCTION,
    ASSIGN,
    ADD,
    SUB,
    MULTIPLY,
    DIVIDE,
    GETADDR,
    GETCONTENT,
    ASSIGNADDR,
    GOTO,
    IF,
    RETURN,
    DEC,
    ARG,
    CALL,
    PARAM,
    READ,
    WRITE
}
abstract class InterCode {
    // 枚举类，可以参考Project_3.pdf的表1
    CodeKind codeKind;
    // 指向下⼀条中间代码
    InterCode next;
    public InterCode(CodeKind codeKind) {
        this.codeKind = codeKind;
    }
    public void addInterCode(InterCode interCode) {
        if(interCode==null) return;
        if(this.next == null) {
            this.next = interCode;
        }
        else {
            this.next.addInterCode(interCode);
        }
    }
    public abstract void print();

    public static class MonoOpInterCode extends InterCode {
        Operand operand;

        public MonoOpInterCode(CodeKind codeKind, Operand operand) {
            super(codeKind);
            this.operand = operand;
        }
        @Override
        public void print() {
            if(codeKind==CodeKind.LABEL) {
                System.out.println("LABEL "+operand.value+" :");
            }
            else if(codeKind==CodeKind.FUNCTION) {
                System.out.println("FUNCTION "+operand.value+" :");
            }
            else if(codeKind==CodeKind.GOTO) {
                System.out.println("GOTO "+operand.value);
            }
            else if(codeKind==CodeKind.RETURN) {
                System.out.println("RETURN "+operand.value);
            }
            else if(codeKind==CodeKind.ARG) {
                System.out.println("ARG "+operand.value);
            }
            else if(codeKind==CodeKind.CALL) {
                System.out.println("CALL "+operand.value);
            }
            else if(codeKind==CodeKind.PARAM) {
                System.out.println("PARAM "+operand.value);
            }
            else if(codeKind==CodeKind.READ) {
                System.out.println("READ "+operand.value);
            }
            else if(codeKind==CodeKind.WRITE) {
                System.out.println("WRITE "+operand.value);
            }
        }
    }
    public static class BinOpInterCode extends InterCode {
        Operand operand1;
        Operand operand2;
        Operand result;

        public BinOpInterCode(CodeKind codeKind, Operand result, Operand operand1, Operand operand2) {
            super(codeKind);
            this.result = result;
            this.operand1 = operand1;
            this.operand2 = operand2;
        }

        @Override
        public void print() {
            if(codeKind==CodeKind.ADD) {
                System.out.println(result.value+" := "+operand1.value+" + "+operand2.value);
            }
            else if(codeKind==CodeKind.SUB) {
                System.out.println(result.value+" := "+operand1.value+" - "+operand2.value);
            }
            else if(codeKind==CodeKind.MULTIPLY) {
                System.out.println(result.value+" := "+operand1.value+" * "+operand2.value);
            }
            else if(codeKind==CodeKind.DIVIDE) {
                System.out.println(result.value+" := "+operand1.value+" / "+operand2.value);
            }
        }
    }
    public static class AssignInterCode extends InterCode {
        Operand leftOperand;
        Operand rightOperand;

        public AssignInterCode(CodeKind codeKind, Operand leftOperand, Operand rightOperand) {
            super(codeKind);
            this.leftOperand = leftOperand;
            this.rightOperand = rightOperand;
        }

        @Override
        public void print() {
            if(codeKind==CodeKind.ASSIGN) {
                System.out.println(leftOperand.value+" := "+rightOperand.value);
            }
            else if(codeKind==CodeKind.GETADDR) {
                System.out.println(leftOperand.value+" := &"+rightOperand.value);
            }
            else if(codeKind==CodeKind.GETCONTENT) {
                System.out.println(leftOperand.value+" := *"+rightOperand.value);
            }
            else if(codeKind==CodeKind.ASSIGNADDR) {
                System.out.println("*"+leftOperand.value+" := "+rightOperand.value);
            }
        }
    }
    public static class ConditionJumpInterCode extends InterCode {
        Operand operand1;
        String relop;
        Operand operand2;
        Operand label;

        public ConditionJumpInterCode(CodeKind codeKind, Operand operand1, String relop, Operand operand2, Operand label) {
            super(codeKind);
            this.operand1 = operand1;
            this.relop = relop;
            this.operand2 = operand2;
            this.label = label;
        }

        @Override
        public void print() {
            System.out.println("IF "+operand1.value+" "+relop+" "+operand2.value+" GOTO "+label.value);
        }
    }
    public static class MemDecInterCode extends InterCode {
        Operand operand;
        int size;

        public MemDecInterCode(CodeKind codeKind, Operand operand, int size) {
            super(codeKind);
            this.operand = operand;
            this.size = size;
        }

        @Override
        public void print() {
            System.out.println("DEC "+operand.value+" "+size);
        }
    }
}
class InterCodeList{
    InterCode head,tail;
    public InterCodeList() {
        head = null;
        tail = null;
    }
    public InterCodeList add(InterCodeList interCodeList) {
        if(interCodeList==null) return this;
        if (this.head==null) {
            this.head = interCodeList.head;
            this.tail = interCodeList.tail;
        }
        else if(interCodeList.head == null) {
            //do nothing
        }
        else {
            this.tail.addInterCode(interCodeList.head);
            this.tail = interCodeList.tail;
        }
        return this;
    }
    public InterCodeList add(InterCode interCode) {
        if(interCode==null) {
            // do nothing
        }
        else if(this.head==null) {
            this.head = interCode;
            this.tail = interCode;
        }
        else {
            this.tail.addInterCode(interCode);
            this.tail=interCode;
        }
        return this;
    }
    public void printAll() {
        InterCode interCode = head;
        while(interCode!=null) {
            interCode.print();
            interCode = interCode.next;
        }
    }
    public void generateCode() {
        System.out.println(
                ".data\n"+
                "_prompt: .asciiz \"Enter an integer:\"\n"+
                "_ret: .asciiz \"\\n\"\n"+
                "\n"+
                ".globl main\n"+
                ".text\n"+
                "\n"+
                "read:\n"+
                "    li $v0, 4\n"+
                "    la $a0, _prompt\n"+
                "    syscall\n"+
                "    li $v0, 5\n"+
                "    syscall\n"+
                "    jr $ra\n"+
                "\n"+
                "write:\n"+
                "    lw $a0, 8($sp)\n"+
                "    li $v0, 1\n"+
                "    syscall\n"+
                "    li $v0, 4\n"+
                "    la $a0, _ret\n"+
                "    syscall\n"+
                "    move $v0, $0\n"+
                "    jr $ra\n");
        InterCode interCode = head;
        Map<String,Integer> vars = new HashMap<>();
        int argSize = 0;
        int paramSize = 0;
        while(interCode!=null) {
            // label and jump
            if(interCode.codeKind==CodeKind.LABEL) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(op.sourceValue+" :");
            }
            else if(interCode.codeKind==CodeKind.GOTO) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println("    j "+op.sourceValue);
            }
            else if(interCode.codeKind==CodeKind.IF) {
                Operand operand1 = ((InterCode.ConditionJumpInterCode)interCode).operand1;
                Operand operand2 = ((InterCode.ConditionJumpInterCode)interCode).operand2;
                Operand label = ((InterCode.ConditionJumpInterCode)interCode).label;
                String relop = ((InterCode.ConditionJumpInterCode)interCode).relop;
                if(operand1.operandKind==OperandKind.CONSTANT) {
                    System.out.println("    li $t0, "+operand1.sourceValue);
                }
                else {
                    System.out.println("    lw $t0, -"+vars.get(operand1.sourceValue)+"($fp)");
                }
                if(operand2.operandKind==OperandKind.CONSTANT) {
                    System.out.println("    li $t1, "+operand2.sourceValue);
                }
                else {
                    System.out.println("    lw $t1, -"+vars.get(operand2.sourceValue)+"($fp)");
                }
                switch (relop) {
                    case "==" :
                        System.out.println("    beq $t0, $t1, " + label.sourceValue);
                        break;
                    case "!=" :
                        System.out.println("    bne $t0, $t1, " + label.sourceValue);
                        break;
                    case ">" :
                        System.out.println("    bgt $t0, $t1, " + label.sourceValue);
                        break;
                    case "<" :
                        System.out.println("    blt $t0, $t1, " + label.sourceValue);
                        break;
                    case ">=" :
                        System.out.println("    bge $t0, $t1, " + label.sourceValue);
                        break;
                    case "<=" :
                        System.out.println("    ble $t0, $t1, " + label.sourceValue);
                        break;
                }
            }
            // functions
            else if(interCode.codeKind==CodeKind.FUNCTION) {
                String funName = ((InterCode.MonoOpInterCode)interCode).operand.sourceValue;
                int totalSize = 0;
                vars.clear();
                paramSize=0;
                InterCode cur = interCode.next;
                System.out.println("\n    # START FUNCTION `"+funName+"`");
                while(cur!=null && cur.codeKind!=CodeKind.FUNCTION) {
                    String name = null;
                    Operand op = null;
                    int size = 4;
                    if(cur.codeKind==CodeKind.PARAM
                            || cur.codeKind==CodeKind.READ) {
                        op = ((InterCode.MonoOpInterCode)cur).operand;
                    }
                    else if(cur.codeKind==CodeKind.ADD
                            || cur.codeKind==CodeKind.SUB
                            || cur.codeKind==CodeKind.MULTIPLY
                            || cur.codeKind==CodeKind.DIVIDE) {
                        op = ((InterCode.BinOpInterCode)cur).result;
                    }
                    else if(cur.codeKind==CodeKind.ASSIGN
                            || cur.codeKind==CodeKind.ASSIGNADDR
                            || cur.codeKind==CodeKind.GETCONTENT
                            || cur.codeKind==CodeKind.GETADDR) {
                        op = ((InterCode.AssignInterCode)cur).leftOperand;
                    }
                    else if(cur.codeKind==CodeKind.DEC) {
                        op = ((InterCode.MemDecInterCode)cur).operand;
                        size = ((InterCode.MemDecInterCode)cur).size;
                    }
                    if(op!=null) {
                        name = op.sourceValue;
                        if(name!=null && vars.get(name)==null) {
                            totalSize += size;
                            vars.put(name, totalSize);
                            System.out.println("    # " + name + ", " + totalSize);
                        }
                    }
                    cur = cur.next;
                }
                System.out.println(funName+":");
                System.out.println("    move $fp, $sp");
                System.out.println("    addi $sp, $sp, -"+totalSize);
            }
            else if(interCode.codeKind==CodeKind.RETURN) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    lw $t0, -"+vars.get(op.sourceValue)+"($fp)\n" +
                        "    move $v0, $t0\n" +
                        "    move $sp, $fp\n" +
                        "    jr $ra");
            }
            else if(interCode.codeKind==CodeKind.READ) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    addi $sp, $sp, -8\n" +
                        "    sw $ra, 0($sp)\n" +
                        "    sw $fp, 4($sp)\n" +
                        "    jal read\n" +
                        "    lw $fp, 4($sp)\n" +
                        "    lw $ra, 0($sp)\n" +
                        "    addi $sp, $sp, 8\n" +
                        "    move $t0, $v0\n" +
                        "    sw $t0, -"+vars.get(op.sourceValue)+"($fp)");
            }
            else if(interCode.codeKind==CodeKind.WRITE) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    addi $sp, $sp, -4\n" +
                        "    lw $t0, -"+vars.get(op.sourceValue)+"($fp)\n" +
                        "    sw $t0, 0($sp)\n"+
                        "    addi $sp, $sp, -8\n" +
                        "    sw $ra, 0($sp)\n" +
                        "    sw $fp, 4($sp)\n" +
                        "    jal write\n" +
                        "    lw $fp, 4($sp)\n" +
                        "    lw $ra, 0($sp)\n" +
                        "    addi $sp, $sp, 12");
            }
            else if(interCode.codeKind==CodeKind.ARG) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    addi $sp, $sp, -4\n" +
                        "    lw $t0, -"+vars.get(op.sourceValue)+"($fp)\n" +
                        "    sw $t0, 0($sp)");
                argSize+=4;
            }
            else if(interCode.codeKind==CodeKind.PARAM) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    lw $t0, "+(8+paramSize)+"($fp)\n" +
                        "    sw $t0, -"+vars.get(op.sourceValue)+"($fp)");
                paramSize+=4;
            }
            else if(interCode.codeKind==CodeKind.CALL) {
                Operand op = ((InterCode.MonoOpInterCode)interCode).operand;
                System.out.println(
                        "    addi $sp, $sp, -8\n" +
                        "    sw $ra, 0($sp)\n" +
                        "    sw $fp, 4($sp)\n" +
                        "    jal "+op.sourceValue+"\n" +
                        "    lw $fp, 4($sp)\n" +
                        "    lw $ra, 0($sp)\n" +
                        "    addi $sp, $sp, "+(8+argSize));
                argSize=0;
            }
            // assigns
            else if(interCode.codeKind==CodeKind.ASSIGN) {
                Operand leftOperand = ((InterCode.AssignInterCode)interCode).leftOperand;
                Operand rightOperand = ((InterCode.AssignInterCode)interCode).rightOperand;
                if(rightOperand.operandKind==OperandKind.FUNCTION) {
                    System.out.println(
                            "    addi $sp, $sp, -8\n" +
                            "    sw $ra, 0($sp)\n" +
                            "    sw $fp, 4($sp)\n" +
                            "    jal "+rightOperand.sourceValue+"\n" +
                            "    lw $fp, 4($sp)\n" +
                            "    lw $ra, 0($sp)\n" +
                            "    addi $sp, $sp, "+(8+argSize)+"\n" +
                            "    move $t0, $v0\n" +
                            "    sw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)");
                    argSize=0;
                }
                else if(rightOperand.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t0, "+rightOperand.sourceValue+"\n"+
                            "    sw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)");
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(rightOperand.sourceValue)+"($fp)\n"+
                            "    sw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)");
                }
            }
            else if(interCode.codeKind==CodeKind.ASSIGNADDR) {
                Operand leftOperand = ((InterCode.AssignInterCode) interCode).leftOperand;
                Operand rightOperand = ((InterCode.AssignInterCode) interCode).rightOperand;
                if(rightOperand.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    lw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)\n" +
                            "    li $t1, "+rightOperand.sourceValue+"\n" +
                            "    sw $t1, 0($t0)");
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)\n" +
                            "    lw $t1, -"+vars.get(rightOperand.sourceValue)+"($fp)\n" +
                            "    sw $t1, 0($t0)");
                }
            }
            else if(interCode.codeKind==CodeKind.GETADDR) {
                Operand leftOperand = ((InterCode.AssignInterCode) interCode).leftOperand;
                Operand rightOperand = ((InterCode.AssignInterCode) interCode).rightOperand;
                System.out.println(
                        "    addi $t0, $fp, -"+vars.get(rightOperand.sourceValue)+"\n" +
                        "    sw $t0, -"+vars.get(leftOperand.sourceValue)+"($fp)");
            }
            else if(interCode.codeKind==CodeKind.GETCONTENT) {
                Operand leftOperand = ((InterCode.AssignInterCode) interCode).leftOperand;
                Operand rightOperand = ((InterCode.AssignInterCode) interCode).rightOperand;
                System.out.println(
                        "    lw $t0, -"+vars.get(rightOperand.sourceValue)+"($fp)\n" +
                        "    lw $t1, 0($t0)\n" +
                        "    sw $t1, -"+vars.get(leftOperand.sourceValue)+"($fp)");
            }
            // calculations
            else if(interCode.codeKind==CodeKind.ADD) {
                Operand operand1 = ((InterCode.BinOpInterCode) interCode).operand1;
                Operand operand2 = ((InterCode.BinOpInterCode) interCode).operand2;
                Operand result = ((InterCode.BinOpInterCode) interCode).result;
                if(operand1.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t0, "+operand1.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(operand1.sourceValue)+"($fp)");
                }
                if(operand2.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    addi $t2, $t0, "+operand2.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t1, -"+vars.get(operand2.sourceValue)+"($fp)\n" +
                            "    add $t2, $t0, $t1");
                }
                System.out.println("    sw $t2, -"+vars.get(result.sourceValue)+"($fp)");
            }
            else if(interCode.codeKind==CodeKind.SUB) {
                Operand operand1 = ((InterCode.BinOpInterCode) interCode).operand1;
                Operand operand2 = ((InterCode.BinOpInterCode) interCode).operand2;
                Operand result = ((InterCode.BinOpInterCode) interCode).result;

                if(operand1.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t0, "+operand1.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(operand1.sourceValue)+"($fp)");
                }
                if(operand2.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    addi $t2, $t0, -"+operand2.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t1, -"+vars.get(operand2.sourceValue)+"($fp)\n" +
                            "    sub $t2, $t0, $t1");
                }
                System.out.println("    sw $t2, -"+vars.get(result.sourceValue)+"($fp)");
            }
            else if(interCode.codeKind==CodeKind.MULTIPLY) {
                Operand operand1 = ((InterCode.BinOpInterCode) interCode).operand1;
                Operand operand2 = ((InterCode.BinOpInterCode) interCode).operand2;
                Operand result = ((InterCode.BinOpInterCode) interCode).result;
                if(operand1.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t0, "+operand1.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(operand1.sourceValue)+"($fp)");
                }
                if(operand2.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t1, "+operand2.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t1, -"+vars.get(operand2.sourceValue)+"($fp)");
                }
                System.out.println(
                        "    mul $t2, $t0, $t1\n" +
                        "    sw $t2, -"+vars.get(result.sourceValue)+"($fp)");
            }
            else if(interCode.codeKind==CodeKind.DIVIDE) {
                Operand operand1 = ((InterCode.BinOpInterCode) interCode).operand1;
                Operand operand2 = ((InterCode.BinOpInterCode) interCode).operand2;
                Operand result = ((InterCode.BinOpInterCode) interCode).result;
                if(operand1.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t0, "+operand1.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t0, -"+vars.get(operand1.sourceValue)+"($fp)");
                }
                if(operand2.operandKind==OperandKind.CONSTANT) {
                    System.out.println(
                            "    li $t1, "+operand2.sourceValue);
                }
                else {
                    System.out.println(
                            "    lw $t1, -"+vars.get(operand2.sourceValue)+"($fp)");
                }
                System.out.println(
                        "    div $t0, $t1\n" +
                        "    mflo $t2\n" +
                        "    sw $t2, -"+vars.get(result.sourceValue)+"($fp)");
            }

            interCode = interCode.next;
        }
    }
}
class Operand {
    OperandKind operandKind;
    String value;
    String sourceValue;
    public Operand(OperandKind operandKind, String value) {
        this.operandKind = operandKind;
        this.sourceValue = value;
        if(operandKind==OperandKind.CONSTANT) {
            this.value = "#" + value;
        }
        else if(operandKind==OperandKind.FUNCTION) {
            this.value = "CALL " + value;
        }
        else {
            this.value = value;
        }
    }

    public void setOperandKind(OperandKind operandKind) {
        this.operandKind = operandKind;
    }
}
enum OperandKind {
    // 变量
    VARIABLE,
    // 常量
    CONSTANT,
    // 地址
    ADDRESS,
    // 跳转标签
    LABEL,
    // 函数
    FUNCTION
}
class TranslateCondParam {
    public Operand labelTrue;
    public Operand labelFalse;
    public TranslateCondParam(Operand labelTrue, Operand labelFalse) {
        this.labelTrue = labelTrue;
        this.labelFalse = labelFalse;
    }
}
class IRVisitor extends CmmParserBaseVisitor<InterCodeList> {
    ParseTreeProperty<Operand> places = new ParseTreeProperty<>();
    ParseTreeProperty<TranslateCondParam> translateCondParams = new ParseTreeProperty<>();
    ParseTreeProperty<Type> types = new ParseTreeProperty<>();
    int tmpCount = 0;
    int labelCount = 0;
    Operand newTmp() {
        String tmp = "t"+tmpCount;
        Operand tmpOP = new Operand(OperandKind.VARIABLE,tmp);
        tmpCount++;
        return tmpOP;
    }
    Operand newLabel() {
        String label = "label"+labelCount;
        Operand labelOP = new Operand(OperandKind.LABEL,label);
        labelCount++;
        return labelOP;
    }

    Operand falseOP = new Operand(OperandKind.CONSTANT,"0");
    Operand trueOP = new Operand(OperandKind.CONSTANT,"1");

    @Override public InterCodeList visitProgram(CmmParser.ProgramContext ctx) {
        if(ctx.extDef().size()==0) return null;
        InterCodeList code = new InterCodeList();
        for (CmmParser.ExtDefContext ed : ctx.extDef()) {
            InterCodeList _code = visit(ed);
            code.add(_code);
        }
        return code;
    }
    @Override public InterCodeList visitExtDefVar(CmmParser.ExtDefVarContext ctx) {
        if(ctx.extDecList()==null) return null;
        return visit(ctx.extDecList());
    }
    @Override public InterCodeList visitExtDefFun(CmmParser.ExtDefFunContext ctx) {
        InterCodeList code = visit(ctx.funDec());
        code.add(visit(ctx.compSt()));
        return code;
    }
    @Override public InterCodeList visitExtDecList(CmmParser.ExtDecListContext ctx) {
        InterCodeList code = new InterCodeList();
        for(CmmParser.VarDecContext varDec : ctx.varDec()) {
            InterCodeList _code = visit(varDec);
            code.add(_code);
        }
        return code;
    }
    @Override public InterCodeList visitVarDec(CmmParser.VarDecContext ctx) {
        InterCodeList code = new InterCodeList();
        String name = ctx.ID(0).getText();
        Type type = Main.symbolTable.search(name);
        if(type.getKind()==Kind.ARRAY || type.getKind()==Kind.STRUCTURE) {
            Operand op = new Operand(OperandKind.VARIABLE,name);
            InterCode.MemDecInterCode code1 = new InterCode.MemDecInterCode(CodeKind.DEC,op,type.getMemSize());
            code.add(code1);
        }
        return code;
    }
    @Override public InterCodeList visitFunDec(CmmParser.FunDecContext ctx) {
        InterCodeList code = new InterCodeList();
        // 获取函数名
        String functionName = ctx.ID().getText();
        // 新建⼀个存储该函数名的Operand
        Operand functionOp = new Operand(OperandKind.VARIABLE, functionName);
        // funcDefineCode打印的中间代码为: FUNCTION XXX:
        InterCode.MonoOpInterCode funcDefineCode = new InterCode.MonoOpInterCode(CodeKind.FUNCTION, functionOp);
        code.add(funcDefineCode);
        // 从符号表中获得当前函数名对应的函数Type，并获得这个函数定义的形参
        FieldList curParam = ((Function) Main.symbolTable.search(functionName)).getParamList();
        // 遍历形参
        while (curParam != null) {
            // ⽣成函数参数声明的中间代码: PARAM xxx
            Operand paramOp = new Operand(OperandKind.VARIABLE, curParam.getName());
            InterCode paramCode = new InterCode.MonoOpInterCode(CodeKind.PARAM, paramOp);
            // 添加到以funcDefineCode为头节点的链中
            code.add(paramCode);
            Type type = curParam.getType();
            if(type.getKind()==Kind.ARRAY || type.getKind()==Kind.STRUCTURE) {
                Main.symbolTable.search(curParam.getName()).setAddr(true);
                type.setAddr(true);
            }
            // 获取下⼀个形参
            curParam = curParam.getNext();
        }
        return code;
    }
    @Override public InterCodeList visitCompSt(CmmParser.CompStContext ctx) {
        InterCodeList code = new InterCodeList();
        code.add(visit(ctx.defList()));
        code.add(visit(ctx.stmtList()));
        return code;
    }

    @Override public InterCodeList visitDefList(CmmParser.DefListContext ctx) {
        InterCodeList code = new InterCodeList();
        for(CmmParser.DefContext def : ctx.def()) {
            code.add(visit(def));
        }
        return code;
    }
    @Override public InterCodeList visitDef(CmmParser.DefContext ctx) {
        InterCodeList code = new InterCodeList();
        code.add(visit(ctx.decList()));
        return code;
    }
    @Override public InterCodeList visitDecList(CmmParser.DecListContext ctx) {
        InterCodeList code = new InterCodeList();
        for(CmmParser.DecContext dec : ctx.dec()) {
            code.add(visit(dec));
        }
        return code;
    }
    @Override public InterCodeList visitDec(CmmParser.DecContext ctx) {
        InterCodeList code = new InterCodeList();
        code.add(visit(ctx.varDec()));
        if(ctx.ASSIGNOP()!=null) {
            Operand t1 = newTmp();
            places.put(ctx,t1);
            code.add(visit(ctx.exp()));
            code.add(new InterCode.AssignInterCode(CodeKind.ASSIGN,new Operand(OperandKind.VARIABLE,ctx.varDec().ID(0).getText()),t1));
        }
        return code;
    }

    // Exp
    @Override public InterCodeList visitExpP(CmmParser.ExpPContext ctx) {
        places.put(ctx,places.get(ctx.parent));
        translateCondParams.put(ctx,translateCondParams.get(ctx.parent));
        InterCodeList code = visit(ctx.exp());
        types.put(ctx,types.get(ctx.exp()));
        return code;
    }
    @Override public InterCodeList visitExpTmp(CmmParser.ExpTmpContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);

        TranslateCondParam t = translateCondParams.get(ctx.parent);
        if(t!=null) {
            place = newTmp();
        }
        Type type = new Int();
        types.put(ctx,type);
        String num = ctx.getText();
        Operand intOP = new Operand(OperandKind.CONSTANT, num);
        if(place!=null) {
            InterCode.AssignInterCode code1 = new InterCode.AssignInterCode(CodeKind.ASSIGN,place,intOP);
            code.add(code1);
        }
        if(t!=null) {
            Operand label1 = t.labelTrue;
            Operand label2 = t.labelFalse;

            InterCode.ConditionJumpInterCode _code2 = new InterCode.ConditionJumpInterCode(CodeKind.IF,place,"!=",falseOP,label1);
            InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label2);
            code.add(_code2);
            code.add(_code3);
        }
        return code;
    }
    @Override public InterCodeList visitExpID(CmmParser.ExpIDContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);

        TranslateCondParam t = translateCondParams.get(ctx.parent);
        if(t!=null) {
            place = newTmp();
        }

        String var = ctx.getText();
        Type type = Main.symbolTable.search(var);
        types.put(ctx,type);
        Operand varOP = new Operand(OperandKind.VARIABLE, var);

        if(place!=null) {
            CodeKind codeKind;
            if(place.operandKind==OperandKind.ADDRESS && !type.isAddr()) {
                codeKind = CodeKind.GETADDR;
            }
            else {
                codeKind = CodeKind.ASSIGN;
            }
            InterCode.AssignInterCode code1 = new InterCode.AssignInterCode(codeKind,place,varOP);
            code.add(code1);
        }
        if(t!=null) {
            Operand label1 = t.labelTrue;
            Operand label2 = t.labelFalse;

            InterCode.ConditionJumpInterCode _code2 = new InterCode.ConditionJumpInterCode(CodeKind.IF,place,"!=",falseOP,label1);
            InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label2);
            code.add(_code2);
            code.add(_code3);
        }

        return code;
    }
    @Override public InterCodeList visitExpAsn(CmmParser.ExpAsnContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        TranslateCondParam t = translateCondParams.get(ctx.parent);
        if(t!=null) {
            place = newTmp();
        }

        CodeKind codeKind;
        Operand varOP = newTmp();
        varOP.setOperandKind(OperandKind.ADDRESS);
        places.put(ctx,varOP);
        InterCodeList code0 = visit(ctx.exp(0));
        Type type = types.get(ctx.exp(0));
        if(type==null || type.getKind()==Kind.STRUCTURE || type.getKind()==Kind.ARRAY) {
            codeKind = CodeKind.ASSIGNADDR;
            code.add(code0);
        }
        else {
            codeKind = CodeKind.ASSIGN;
            String name = ((CmmParser.ExpIDContext)ctx.exp(0)).ID().getText();
            varOP = new Operand(OperandKind.VARIABLE,name);
        }

        Operand t1 = newTmp();
        places.put(ctx,t1);
        InterCodeList code1 = visit(ctx.exp(1));

        InterCode.AssignInterCode code2 = new InterCode.AssignInterCode(codeKind,varOP,t1);
        code.add(code1);
        code.add(code2);
        if(place!=null) {
            if(varOP.operandKind==OperandKind.ADDRESS) {
                codeKind = CodeKind.GETCONTENT;
            }
            else {
                codeKind = CodeKind.ASSIGN;
            }
            InterCode.AssignInterCode code3 = new InterCode.AssignInterCode(codeKind,place,varOP);
            code.add(code3);
        }
        if(t!=null) {
            Operand label1 = t.labelTrue;
            Operand label2 = t.labelFalse;
            InterCode.ConditionJumpInterCode _code2 = new InterCode.ConditionJumpInterCode(CodeKind.IF,place,"!=",falseOP,label1);
            InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label2);
            code.add(_code2);
            code.add(_code3);
        }
        return code;
    }
    @Override public InterCodeList visitExpCal(CmmParser.ExpCalContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        if(ctx.RELOP()!=null) {
            TranslateCondParam t = translateCondParams.get(ctx.parent);
            Operand label1;
            Operand label2;
            if(t!=null) {
                label1 = t.labelTrue;
                label2 = t.labelFalse;
            }
            else {
                label1 = newLabel();
                label2 = newLabel();
            }
            if(place!=null) {
                InterCode.AssignInterCode code0 = new InterCode.AssignInterCode(CodeKind.ASSIGN,place,falseOP);
                code.add(code0);
            }
            Operand t1 = newTmp();
            Operand t2 = newTmp();
            places.put(ctx,t1);
            InterCodeList code1 = visit(ctx.exp(0));
            places.put(ctx,t2);
            InterCodeList _code2 = visit(ctx.exp(1));
            String op = ctx.RELOP().getText();
            InterCode.ConditionJumpInterCode _code3 = new InterCode.ConditionJumpInterCode(CodeKind.IF,t1,op,t2,label1);
            InterCode.MonoOpInterCode _code4 = new InterCode.MonoOpInterCode(CodeKind.GOTO, label2);
            code.add(code1);
            code.add(_code2);
            code.add(_code3);
            code.add(_code4);
            if(place!=null) {
                InterCode.MonoOpInterCode code2 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label1);
                code.add(code2);
                InterCode.AssignInterCode code2_1 = new InterCode.AssignInterCode(CodeKind.ASSIGN,place,trueOP);
                code.add(code2_1);
                InterCode.MonoOpInterCode code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label2);
                code.add(code3);
            }
            return code;
        }
        else {
            TranslateCondParam t = translateCondParams.get(ctx.parent);
            if(t!=null) {
                place = newTmp();
            }
            Operand t1 = newTmp();
            Operand t2 = newTmp();
            places.put(ctx,t1);
            InterCodeList code1 = visit(ctx.exp(0));
            places.put(ctx,t2);
            InterCodeList code2 = visit(ctx.exp(1));
            code.add(code1);
            code.add(code2);
            InterCode.BinOpInterCode code3 = null;
            if(place!=null) {
                if (ctx.PLUS() != null) {
                    code3 = new InterCode.BinOpInterCode(CodeKind.ADD, place, t1, t2);
                } else if (ctx.MINUS() != null) {
                    code3 = new InterCode.BinOpInterCode(CodeKind.SUB, place, t1, t2);
                } else if (ctx.STAR() != null) {
                    code3 = new InterCode.BinOpInterCode(CodeKind.MULTIPLY, place, t1, t2);
                } else if (ctx.DIV() != null) {
                    code3 = new InterCode.BinOpInterCode(CodeKind.DIVIDE, place, t1, t2);
                }
                code.add(code3);
            }

            if(t!=null) {
                Operand label1 = t.labelTrue;
                Operand label2 = t.labelFalse;

                InterCode.ConditionJumpInterCode _code2 = new InterCode.ConditionJumpInterCode(CodeKind.IF,place,"!=",falseOP,label1);
                InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label2);
                code.add(_code2);
                code.add(_code3);
            }
            return code;
        }
    }
    @Override public InterCodeList visitExpCalLog(CmmParser.ExpCalLogContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        if(ctx.MINUS()!=null) {
            TranslateCondParam t = translateCondParams.get(ctx.parent);
            if(t!=null) {
                place = newTmp();
            }

            Operand t1 = newTmp();
            places.put(ctx,t1);
            InterCodeList code1 = visit(ctx.exp());
            code.add(code1);

            if(place!=null) {
                InterCode.BinOpInterCode code2 = new InterCode.BinOpInterCode(CodeKind.SUB, place, falseOP, t1);
                code.add(code2);
            }

            if(t!=null) {
                Operand label1 = t.labelTrue;
                Operand label2 = t.labelFalse;

                InterCode.ConditionJumpInterCode _code2 = new InterCode.ConditionJumpInterCode(CodeKind.IF,place,"!=",falseOP,label1);
                InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label2);
                code.add(_code2);
                code.add(_code3);
            }
            return code;
        }
        else if(ctx.NOT()!=null) {
            TranslateCondParam t = translateCondParams.get(ctx.parent);
            Operand label1;
            Operand label2;
            if(t!=null) {
                label1 = t.labelTrue;
                label2 = t.labelFalse;
            }
            else {
                if(place==null) place = newTmp();
                label1 = newLabel();
                label2 = newLabel();
            }
            if(place!=null) {
                InterCode.AssignInterCode code0 = new InterCode.AssignInterCode(CodeKind.ASSIGN, place, falseOP);
                code.add(code0);
            }
            translateCondParams.put(ctx,new TranslateCondParam(label2,label1));
            InterCodeList code1 = visit(ctx.exp());
            code.add(code1);
            if(place!=null) {
                InterCode.MonoOpInterCode code2 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label1);
                code.add(code2);
                InterCode.AssignInterCode code2_1 = new InterCode.AssignInterCode(CodeKind.ASSIGN, place, trueOP);
                code.add(code2_1);
                InterCode.MonoOpInterCode code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label2);
                code.add(code3);
            }
            return code;
        }
        return code;
    }
    @Override public InterCodeList visitExpLog(CmmParser.ExpLogContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        TranslateCondParam t = translateCondParams.get(ctx.parent);
        Operand label1;
        Operand label2;
        if(t!=null) {
            label1 = t.labelTrue;
            label2 = t.labelFalse;
        }
        else {
            if(place==null) place = newTmp();
            label1 = newLabel();
            label2 = newLabel();
        }
        if(place!=null) {
            InterCode.AssignInterCode code0 = new InterCode.AssignInterCode(CodeKind.ASSIGN, place, falseOP);
            code.add(code0);
        }
        if(ctx.AND()!=null) {
            Operand label = newLabel();
            t = new TranslateCondParam(label,label2);
            translateCondParams.put(ctx,t);
            InterCodeList code1 = visit(ctx.exp(0));
            code.add(code1);
            t = new TranslateCondParam(label1,label2);
            translateCondParams.put(ctx,t);
            InterCodeList _code2 = visit(ctx.exp(1));
            InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label);
            code.add(_code3);
            code.add(_code2);
        }
        else if(ctx.OR()!=null) {
            Operand label = newLabel();
            t = new TranslateCondParam(label1,label);
            translateCondParams.put(ctx,t);
            InterCodeList code1 = visit(ctx.exp(0));
            code.add(code1);
            t = new TranslateCondParam(label1,label2);
            translateCondParams.put(ctx,t);
            InterCodeList _code2 = visit(ctx.exp(1));
            InterCode.MonoOpInterCode _code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label);
            code.add(_code3);
            code.add(_code2);
        }
        if(place!=null) {
            InterCode.MonoOpInterCode code2 = new InterCode.MonoOpInterCode(CodeKind.LABEL, label1);
            code.add(code2);
            InterCode.AssignInterCode code2_1 = new InterCode.AssignInterCode(CodeKind.ASSIGN, place, trueOP);
            code.add(code2_1);
            InterCode.MonoOpInterCode code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL, label2);
            code.add(code3);
        }
        return code;
    }
    @Override public InterCodeList visitExpFun(CmmParser.ExpFunContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        if(place==null) {
            place = newTmp();
        }
        String name = ctx.ID().getText();
        if(ctx.args()==null) {
            if(name.equals("read")) {
                code.add(new InterCode.MonoOpInterCode(CodeKind.READ, place));
                return code;
            }
            code.add(new InterCode.AssignInterCode(CodeKind.ASSIGN, place, new Operand(OperandKind.FUNCTION, name)));
            return code;
        }
        else {
            if(name.equals("write")) {
                Operand t1 = newTmp();
                places.put(ctx.args(),t1);
                InterCodeList code1 = visit(ctx.args().exp(0));
                code.add(code1);
                code.add(new InterCode.MonoOpInterCode(CodeKind.WRITE,t1));
                return code;
            }
            Function function = (Function) Main.symbolTable.search(name);
            FieldList fieldList = function.getParamList();
            InterCodeList code2 = new InterCodeList();
            for (CmmParser.ExpContext exp : ctx.args().exp()) {
                Operand t1 = newTmp();
                if(fieldList.getType().isAddr()) {
                    t1.setOperandKind(OperandKind.ADDRESS);
                }
                places.put(ctx.args(), t1);
                InterCodeList _code1 = visit(exp);
                code.add(_code1);
                InterCode.MonoOpInterCode _code2 = new InterCode.MonoOpInterCode(CodeKind.ARG, t1);
                InterCodeList tmp = new InterCodeList();
                tmp.add(_code2);
                tmp.add(code2);
                code2 = tmp;
                fieldList = fieldList.getNext();
            }
            code.add(code2);
            code.add(new InterCode.AssignInterCode(CodeKind.ASSIGN,place,new Operand(OperandKind.FUNCTION,name)));
            return code;
        }
    }
    @Override public InterCodeList visitExpArr(CmmParser.ExpArrContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        if(place==null) place=newTmp();
        Operand baseAddr = newTmp();
        baseAddr.setOperandKind(OperandKind.ADDRESS);
        places.put(ctx,baseAddr);
        InterCodeList code1 = visit(ctx.exp(0));
        code.add(code1);
        Operand index = newTmp();
        places.put(ctx,index);
        InterCodeList code2 = visit(ctx.exp(1));
        code.add(code2);
        Operand offset = newTmp();
        Type type = ((Array)types.get(ctx.exp(0))).getType();
        InterCode.BinOpInterCode code3 = new InterCode.BinOpInterCode(CodeKind.MULTIPLY,offset,index,
                new Operand(OperandKind.CONSTANT,String.valueOf(type.getMemSize())));
        if(type.getKind()==Kind.STRUCTURE || type.getKind()==Kind.ARRAY)
            types.put(ctx,type);
        code.add(code3);
        if(place.operandKind==OperandKind.ADDRESS) {
            InterCode.BinOpInterCode code4 = new InterCode.BinOpInterCode(CodeKind.ADD,place,baseAddr,offset);
            code.add(code4);
        }
        else {
            Operand realAddr = newTmp();
            InterCode.BinOpInterCode code4 = new InterCode.BinOpInterCode(CodeKind.ADD,realAddr,baseAddr,offset);
            code.add(code4);
            InterCode.AssignInterCode code5 = new InterCode.AssignInterCode(CodeKind.GETCONTENT,place,realAddr);
            code.add(code5);
        }
        return code;
    }
    @Override public InterCodeList visitExpStt(CmmParser.ExpSttContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand place = places.get(ctx.parent);
        if(place==null) place=newTmp();
        Operand baseAddr = newTmp();
        baseAddr.setOperandKind(OperandKind.ADDRESS);
        places.put(ctx,baseAddr);
        InterCodeList code1 = visit(ctx.exp());
        code.add(code1);
        Structure structure = (Structure)types.get(ctx.exp());
        FieldList fieldList = structure.getMemberList();
        String name = ctx.ID().getText();
        int memSize = 0;
        while(fieldList!=null) {
            if(fieldList.getName().equals(name)) {
                if(fieldList.getType().getKind()==Kind.STRUCTURE || fieldList.getType().getKind()==Kind.ARRAY)
                    types.put(ctx,fieldList.getType());
                break;
            }
            else {
                memSize += fieldList.getType().getMemSize();
            }
            fieldList=fieldList.getNext();
        }
        if(place.operandKind==OperandKind.ADDRESS) {
            InterCode.BinOpInterCode code2 = new InterCode.BinOpInterCode(CodeKind.ADD,place,baseAddr,
                    new Operand(OperandKind.CONSTANT,String.valueOf(memSize)));
            code.add(code2);
        }
        else {
            Operand realAddr = newTmp();
            InterCode.BinOpInterCode code2 = new InterCode.BinOpInterCode(CodeKind.ADD,realAddr,baseAddr,
                    new Operand(OperandKind.CONSTANT,String.valueOf(memSize)));
            code.add(code2);
            InterCode.AssignInterCode code3 = new InterCode.AssignInterCode(CodeKind.GETCONTENT,place,realAddr);
            code.add(code3);
        }
        return code;
    }

    // Stmt
    @Override public InterCodeList visitStmtList(CmmParser.StmtListContext ctx) {
        InterCodeList code = new InterCodeList();
        for(CmmParser.StmtContext stmt : ctx.stmt()) {
            code.add(visit(stmt));
        }
        return code;
    }
    @Override public InterCodeList visitStmt_(CmmParser.Stmt_Context ctx) {
        InterCodeList code = new InterCodeList();
        if(ctx.exp()!=null) {
            places.put(ctx,null);
            return visit(ctx.exp());
        }
        else if(ctx.compSt()!=null) {
            // translateCompSt
            return visit(ctx.compSt());
        }
        return code;
    }
    @Override public InterCodeList visitStmtRet(CmmParser.StmtRetContext ctx) {
        InterCodeList code = new InterCodeList();
        Operand t1 = newTmp();
        places.put(ctx,t1);
        InterCodeList code1 = visit(ctx.exp());
        code.add(code1);
        InterCode.MonoOpInterCode code2 = new InterCode.MonoOpInterCode(CodeKind.RETURN,t1);
        code.add(code2);
        return code;
    }
    @Override public InterCodeList visitStmtLog(CmmParser.StmtLogContext ctx) {
        InterCodeList code = new InterCodeList();
        if(ctx.IF()!=null) {
            if(ctx.ELSE()!=null) {
                Operand label1 = newLabel();
                Operand label2 = newLabel();
                Operand label3 = newLabel();
                TranslateCondParam t = new TranslateCondParam(label1,label2);
                translateCondParams.put(ctx,t);
                InterCodeList code1 = visit(ctx.exp());
                code.add(code1);
                translateCondParams.put(ctx,null);
                InterCodeList code2 = visit(ctx.stmt(0));
                InterCodeList code3 = visit(ctx.stmt(1));
                InterCode.MonoOpInterCode code4 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label1);
                code.add(code4);
                code.add(code2);
                InterCode.MonoOpInterCode code5 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label3);
                code.add(code5);
                InterCode.MonoOpInterCode code6 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label2);
                code.add(code6);
                code.add(code3);
                InterCode.MonoOpInterCode code7 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label3);
                code.add(code7);
                return code;
            }
            else {
                Operand label1 = newLabel();
                Operand label2 = newLabel();
                TranslateCondParam t = new TranslateCondParam(label1,label2);
                translateCondParams.put(ctx,t);
                InterCodeList code1 = visit(ctx.exp());
                code.add(code1);
                translateCondParams.put(ctx,null);
                InterCodeList code2 = visit(ctx.stmt(0));
                InterCode.MonoOpInterCode code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label1);
                code.add(code3);
                code.add(code2);
                InterCode.MonoOpInterCode code4 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label2);
                code.add(code4);
                return code;
            }
        }
        else if(ctx.WHILE()!=null) {
            Operand label1 = newLabel();
            Operand label2 = newLabel();
            Operand label3 = newLabel();
            TranslateCondParam t = new TranslateCondParam(label2,label3);
            translateCondParams.put(ctx,t);
            InterCodeList code1 = visit(ctx.exp());
            translateCondParams.put(ctx,null);
            InterCodeList code2 = visit(ctx.stmt(0));
            InterCode.MonoOpInterCode code3 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label1);
            code.add(code3);
            code.add(code1);
            InterCode.MonoOpInterCode code4 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label2);
            code.add(code4);
            code.add(code2);
            InterCode.MonoOpInterCode code5 = new InterCode.MonoOpInterCode(CodeKind.GOTO,label1);
            code.add(code5);
            InterCode.MonoOpInterCode code6 = new InterCode.MonoOpInterCode(CodeKind.LABEL,label3);
            code.add(code6);
            return code;
        }
        return code;
    }
}