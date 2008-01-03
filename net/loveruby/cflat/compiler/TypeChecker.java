package net.loveruby.cflat.compiler;
import net.loveruby.cflat.ast.*;
import net.loveruby.cflat.type.*;
import net.loveruby.cflat.exception.*;
import java.util.*;

class TypeChecker extends Visitor {
    static public void check(AST ast, TypeTable typeTable,
            ErrorHandler handler) throws SemanticException {
        new TypeChecker(typeTable, handler).visit(ast);
    }

    protected TypeTable typeTable;
    protected ErrorHandler errorHandler;

    public TypeChecker(TypeTable typeTable, ErrorHandler errorHandler) {
        this.typeTable = typeTable;
        this.errorHandler = errorHandler;
    }

    public void visit(AST ast) throws SemanticException {
        Iterator funcs = ast.functions();
        while (funcs.hasNext()) {
            DefinedFunction f = (DefinedFunction)funcs.next();
            checkReturnType(f);
            checkParamTypes(f);
            resolve(f.body());
        }
        if (errorHandler.errorOccured()) {
            throw new SemanticException("compile failed.");
        }
    }

    protected void checkReturnType(DefinedFunction f) {
        if (f.returnType().isArray()) {
            errorHandler.error("returns an array: " + f.name());
        }
        else if (f.returnType().isStruct()) {
            errorHandler.error("returns a struct: " + f.name());
        }
        else if (f.returnType().isUnion()) {
            errorHandler.error("returns a union: " + f.name());
        }
    }

    protected void checkParamTypes(DefinedFunction f) {
        Iterator params = f.parameters();
        while (params.hasNext()) {
            Parameter param = (Parameter)params.next();
            if (isInvalidArgType(param.type())) {
                errorHandler.error("invalid parameter type: "
                                   + param.type().textize());
            }
        }
    }

    //
    // Statement Nodes
    //

    public void visit(IfNode node) {
        super.visit(node);
        checkCondExpr(node.cond());
    }

    public void visit(WhileNode node) {
        super.visit(node);
        checkCondExpr(node.cond());
    }

    public void visit(ForNode node) {
        super.visit(node);
        checkCondExpr(node.cond());
    }

    protected void checkCondExpr(ExprNode cond) {
        Type t = cond.type();
        if (!t.isInteger() && !t.isPointer()) {
            notIntegerError(t);
        }
    }

    public void visit(SwitchNode node) {
        super.visit(node);
        mustBeScalar(node.cond());
    }

    public void visit(ReturnNode node) {
        super.visit(node);
        if (node.function().isVoid()) {
            if (node.expr() != null) {
                errorHandler.error("returning value from void function");
            }
        }
        else {  // non-void function
            if (node.expr() == null) {
                errorHandler.error("missing return value");
                return;
            }
            insertImplicitCast(node);
        }
    }

    protected void insertImplicitCast(ReturnNode node) {
        Type exprType = node.expr().type();
        Type retType = node.function().returnType();
        if (exprType.equals(retType)) {   // type matches
            return;
        }
        else if (exprType.isCompatible(retType)) {
            node.setExpr(newCastNode(retType, node.expr()));
        }
        else {
            errorHandler.error("returning incompatible value: "
                               + exprType.textize());
        }
    }

    //
    // Assignment Expression Nodes
    //

    public void visit(AssignNode node) {
        checkAssignment(node);
    }

    public void visit(PlusAssignNode node) {
        checkAssignment(node);
    }

    public void visit(MinusAssignNode node) {
        checkAssignment(node);
    }

    public void visit(MulAssignNode node) {
        checkAssignment(node);
    }

    public void visit(DivAssignNode node) {
        checkAssignment(node);
    }

    public void visit(ModAssignNode node) {
        checkAssignment(node);
    }

    public void visit(AndAssignNode node) {
        checkAssignment(node);
    }

    public void visit(OrAssignNode node) {
        checkAssignment(node);
    }

    public void visit(XorAssignNode node) {
        checkAssignment(node);
    }

    public void visit(LShiftAssignNode node) {
        checkAssignment(node);
    }

    public void visit(RShiftAssignNode node) {
        checkAssignment(node);
    }

    protected void checkAssignment(AbstractAssignNode node) {
        resolve(node.lhs());
        resolve(node.rhs());
        if (! node.lhs().isAssignable()) {
            errorHandler.error("invalid lhs expression");
            return;
        }
        if (isInvalidLHSType(node.lhs().type())) {
            errorHandler.error("invalid lhs type");
            return;
        }
        insertImplicitCast(node);
    }

    protected boolean isInvalidLHSType(Type type) {
        return type.isStruct() || type.isUnion() || type.isAllocatedArray();
    }

    protected void insertImplicitCast(AbstractAssignNode node) {
        Type l = node.lhs().type();
        Type r = node.rhs().type();
        if (l.equals(r)) {
            return;
        }
        else if (r.isCastableTo(l)) {   // insert cast on RHS
            if (! r.isCompatible(l)) {
                errorHandler.warn("implicit cast from " +
                        r.textize() + " to " + l.textize());
            }
            node.setRHS(newCastNode(l, node.rhs()));
        }
        else {
            incompatibleTypeError(l, r);
        }
    }

    //
    // Condition Expression Node
    //

    public void visit(CondExprNode node) {
        super.visit(node);
        checkCondExpr(node.cond());
        Type t = node.thenExpr().type();
        Type e = node.elseExpr().type();
        if (t.equals(e)) {
            return;
        }
        else if (t.isCompatible(e)) {   // insert cast on thenBody
            node.setThenExpr(newCastNode(e, node.thenExpr()));
        }
        else if (e.isCompatible(t)) {   // insert cast on elseBody
            node.setElseExpr(newCastNode(t, node.elseExpr()));
        }
        else {
            incompatibleTypeError(e, t);
        }
    }

    //
    // Binary Operator Nodes
    //

    public void visit(PlusNode node) {
        super.visit(node);
        expectsSameIntegerOrPointerDiff(node);
    }

    public void visit(MinusNode node) {
        super.visit(node);
        expectsSameIntegerOrPointerDiff(node);
    }

    public void visit(MulNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(DivNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(ModNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(BitwiseAndNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(BitwiseOrNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(BitwiseXorNode node) {
        super.visit(node);
        expectsSameInteger(node);
    }

    public void visit(LShiftNode node) {
        super.visit(node);
        expectsIntegers(node);
    }

    public void visit(RShiftNode node) {
        super.visit(node);
        expectsIntegers(node);
    }

    public void visit(EqNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(NotEqNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(LtNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(LtEqNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(GtNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(GtEqNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(LogicalAndNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    public void visit(LogicalOrNode node) {
        super.visit(node);
        expectsComparableScalars(node);
    }

    /**
     * For + and -, only following types of expression are valid:
     *
     *   * integer + integer
     *   * pointer + integer
     *   * integer + pointer
     */
    protected void expectsSameIntegerOrPointerDiff(BinaryOpNode node) {
        if (node.left().type().isPointer()) {
            mustBeInteger(node.right());
            node.setType(node.left().type());
        }
        else if (node.right().type().isPointer()) {
            mustBeInteger(node.left());
            node.setType(node.right().type());
        }
        else {
            expectsSameInteger(node);
        }
    }

    // *, /, %, &, |, ^
    protected void expectsSameInteger(BinaryOpNode node) {
        mustBeInteger(node.left());
        mustBeInteger(node.right());
        insertImplicitCast(node);
    }

    // <<, >>
    protected void expectsIntegers(BinaryOpNode node) {
        mustBeInteger(node.left());
        mustBeInteger(node.right());
    }

    // ==, !=, <, <=, >, >=, &&, ||
    protected void expectsComparableScalars(BinaryOpNode node) {
        mustBeScalar(node.left());
        mustBeScalar(node.right());
        insertImplicitCast(node);
    }

    protected void insertImplicitCast(BinaryOpNode node) {
        ExprNode r = node.right();
        ExprNode l = node.left();
        if (r.type().equals(l.type())) {
            return;
        }
        else if (r.type().isCompatible(l.type())) {
            // insert cast on right expr
            node.setRight(newCastNode(l.type(), r));
        }
        else if (l.type().isCompatible(r.type())) {
            // insert cast on left expr
            node.setLeft(newCastNode(r.type(), l));
        }
        else {
            incompatibleTypeError(l.type(), r.type());
        }
    }

    //
    // Unary Operator Nodes
    //

    public void visit(PrefixIncNode node) {
        expectsScalarOperand(node);
    }

    public void visit(SuffixIncNode node) {
        expectsScalarOperand(node);
    }

    public void visit(PrefixDecNode node) {
        expectsScalarOperand(node);
    }

    public void visit(SuffixDecNode node) {
        expectsScalarOperand(node);
    }

    /** We can increment/decrement an integer or a pointer. */
    protected void expectsScalarOperand(UnaryOpNode node) {
        resolve(node.expr());
        mustBeScalar(node.expr());
    }

    /**
     * For EXPR(ARG), checks:
     *
     *   * EXPR is callable (a pointer to a function).
     *   * The number of argument matches function prototype.
     *   * ARG matches function prototype.
     *   * ARG is neither a struct nor an union.
     */
    public void visit(FuncallNode node) {
        resolve(node.expr());
        if (! node.expr().isCallable()) {
            errorHandler.error("calling object is not a function");
            return;
        }
        FunctionType type = node.functionType();
        if (! type.acceptsArgc(node.numArgs())) {
            errorHandler.error("wrong number of argments: " + node.numArgs());
            return;
        }
        // Check type of only mandatory parameters.
        Iterator params = type.paramTypes();
        Iterator args = node.arguments();
        List newArgs = new ArrayList();
        while (params.hasNext()) {
            Type param = (Type)params.next();
            ExprNode arg = (ExprNode)args.next();
            resolve(arg);
            newArgs.add(checkArgType(arg, param));
        }
        while (args.hasNext()) {
            ExprNode arg = (ExprNode)args.next();
            resolve(arg);
            newArgs.add(arg);
        }
        node.replaceArgs(newArgs);
    }

    /** Checks argument type and insert implicit cast if needed. */
    protected ExprNode checkArgType(ExprNode arg, Type param) {
        if (isInvalidArgType(arg.type())) {
            errorHandler.error("invalid argument type: "
                               + arg.type().textize());
            return arg;
        }
        if (arg.type().equals(param)) {
            return arg;
        }
        else if (arg.type().isCompatible(param)) {
            return newCastNode(param, arg);
        }
        else {
            incompatibleTypeError(arg.type(), param);
            return arg;
        }
    }

    /**
     * Return true if the type t is invalid for function argument.
     */
    protected boolean isInvalidArgType(Type t) {
        return isInvalidLHSType(t);
    }

    /**
     * Checks if the type of base expression of EXPR[IDX] is valid.
     * EXPR must be an array or a pointer.  IDX must be an integer.
     */
    public void visit(ArefNode node) {
        resolve(node.expr());
        if (! node.expr().isDereferable()) {
            errorHandler.error("is not indexable: " +
                               node.expr().type().textize());
            return;
        }
        resolve(node.index());
        mustBeInteger(node.index());
    }

    public void visit(MemberNode node) {
        resolve(node.expr());
        checkMemberRef(node.expr().type(), node.name());
    }

    public void visit(PtrMemberNode node) {
        resolve(node.expr());
        if (! node.expr().type().isPointer()) {
            undereferableError(node.type());
            return;
        }
        checkMemberRef(node.dereferedType(), node.name());
    }

    protected void checkMemberRef(Type t, String memb) {
        if (! t.isComplexType()) {
            errorHandler.error("is not struct/union: " + t.textize());
            return;
        }
        ComplexType type = t.getComplexType();
        if (! type.hasMember(memb)) {
            errorHandler.error(type.textize() +
                               " does not have member " + memb);
            return;
        }
    }

    public void visit(DereferenceNode node) {
        super.visit(node);
        if (! node.expr().type().isDereferable()) {
            undereferableError(node.type());
            return;
        }
    }

    public void visit(AddressNode node) {
        super.visit(node);
        Type t = typeTable.pointerTo(node.expr().type());
        node.setType(t);
        if (! node.expr().isAssignable()) {
            errorHandler.error("invalid LHS expression for &");
        }
    }

    public void visit(CastNode node) {
        resolve(node.expr());
        if (! node.expr().type().isCastableTo(node.type())) {
            incompatibleTypeError(node.expr().type(), node.type());
        }
        else if (! node.expr().type().isCompatible(node.type())) {
            errorHandler.warn("incompatible cast from "
                              + node.expr().type().textize()
                              + " to " + node.type().textize());
        }
    }

    //
    // Utilities
    //

    protected CastNode newCastNode(Type t, ExprNode n) {
        return new CastNode(new TypeNode(t), n);
    }

    protected void mustBeInteger(ExprNode node) {
        if (node.type().isInteger()) return;
        notIntegerError(node.type());
    }

    protected void mustBeScalar(ExprNode node) {
        if (node.type().isInteger()) return;
        if (node.type().isPointer()) return;
        notIntegerError(node.type());
    }

    protected void incompatibleTypeError(Type l, Type r) {
        errorHandler.error("incompatible type: "
                           + l.textize() + " and " + r.textize());
    }

    protected void notIntegerError(Type type) {
        errorHandler.error("non-integer argument for unary op: "
                           + type.textize());
    }

    protected void undereferableError(Type type) {
        errorHandler.error("dereferencing non-pointer expression: "
                           + type.textize());
    }
}
